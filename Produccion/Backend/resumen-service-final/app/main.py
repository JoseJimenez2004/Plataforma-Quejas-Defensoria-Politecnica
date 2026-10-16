from fastapi import FastAPI, HTTPException
from .pipeline import resumir
from .schemas import ResumenRequest, ResumenResponse

app = FastAPI(
    title="DDP Resumen Service",
    version="1.0.0",
    description="D9/MMR + editor Qwen. El resultado es un borrador y requiere revisión humana.",
)

@app.get("/health")
def health():
    return {
        "status": "ok",
        "pipeline": "D9/MMR + Qwen editor",
        "requiere_revision": True,
    }

@app.post("/resumir", response_model=ResumenResponse)
def crear_resumen(req: ResumenRequest):
    try:
        return resumir(req.descripcion, req.usar_fallback)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc))
    except Exception as exc:
        # El detalle se conserva en la cadena de excepción del servidor; no se expone al cliente.
        raise HTTPException(status_code=500, detail="No fue posible generar el resumen.") from exc
