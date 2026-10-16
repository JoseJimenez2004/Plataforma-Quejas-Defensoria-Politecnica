"""Java-compatible endpoint plus structured V5 endpoint."""
from typing import Any
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from .engine import search, refresh_index, index_status

app = FastAPI(title='DDP Antecedentes V5', version='0.1.0')

class LegacyRequest(BaseModel):
    texto: str = Field(min_length=1)
    umbral: float = Field(default=0, ge=0, le=1)
    topK: int = Field(default=500, ge=1, le=5000)
    # Optional: older clients can continue sending only texto/umbral/topK.
    quejoso: dict[str, Any] | None = None
    denunciado: dict[str, Any] | None = None
    folio: str | None = None
    fechaHechos: str | None = None

class StructuredRequest(BaseModel):
    descripcion: str = Field(min_length=1)
    folio: str | None = None
    origen: str = 'SISTEMA'
    fechaHechos: str | None = None
    unidadAcademicaClave: str | None = None
    quejoso: dict[str, Any] = Field(default_factory=dict)
    denunciado: dict[str, Any] = Field(default_factory=dict)
    topK: int = Field(default=10, ge=1, le=5000)


def safe_search(query, top_k):
    try:
        return search(query, top_k=top_k)
    except Exception as exc:
        # Do not leak DB credentials, personal data or connection strings in HTTP errors.
        import logging
        logging.getLogger(__name__).error('Búsqueda fallida: %s', type(exc).__name__)
        raise HTTPException(status_code=503, detail='Búsqueda no disponible: verificar bases de datos o modelo') from None


def legacy_person(p):
    return {'nombre': p.get('nombre'), 'apellido1': p.get('apellido1'),
            'apellido2': p.get('apellido2'), 'numero_identificacion': p.get('numeroIdentificacion'),
            'correo': p.get('correo')}


def legacy_item(item):
    return {'folio': item['folio'], 'similitud': item['similitud_hechos'],
            'historico': item['origen'] == 'HISTORICO',
            'quejoso': legacy_person(item.get('quejoso') or {}),
            'denunciado': legacy_person(item.get('denunciado') or {}),
            'texto_original': item.get('descripcion'), 'fragmento': item.get('descripcion'),
            'fecha_hechos': item.get('fechaHechos'), 'tipo_violencia': item.get('motivo'),
            'unidad_academica': item.get('unidadAcademicaClave'), 'estatus': item.get('estatus'),
            'resultado': item.get('resultado')}

@app.get('/health')
def health():
    return {'status': 'ok'}  # Process liveness only; not a DB/model readiness check.

@app.get('/ready')
def ready():
    try:
        refresh_index()
        return {'status': 'ready', **index_status()}
    except Exception:
        raise HTTPException(status_code=503, detail='Índice no disponible') from None

@app.post('/api/antecedentes/buscar')
def buscar_legacy(request: LegacyRequest):
    """Exact request/response shape expected by MotorAntecedentesModelo.java.

    Existing clients may send only text; optional person fields support richer
    queries without changing the legacy response shape.
    """
    query = {
        'descripcion': request.texto,
        'quejoso': request.quejoso or {},
        'denunciado': request.denunciado or {},
        'folio': request.folio,
        'fechaHechos': request.fechaHechos,
    }
    result = safe_search(query, request.topK)
    return {'antecedentes': [legacy_item(i) for i in result['por_hechos']
                            if i['similitud_hechos'] >= request.umbral]}

@app.post('/api/v5/antecedentes/buscar')
def buscar_v5(request: StructuredRequest):
    return safe_search(request.model_dump(), request.topK)
