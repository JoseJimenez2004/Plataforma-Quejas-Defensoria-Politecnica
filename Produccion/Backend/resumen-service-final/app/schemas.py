from pydantic import BaseModel, Field

class ResumenRequest(BaseModel):
    descripcion: str = Field(min_length=1, max_length=20000)
    usar_fallback: bool = True  # Compatibilidad temporal con clientes anteriores.

class ResumenResponse(BaseModel):
    resumen: str
    requiere_revision: bool
    fuente_final: str
    captura_d9: str
    version_pipeline: str
