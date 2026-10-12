import os
from .d9 import limpiar_espacios, generar_resumen_d9
from .qwen import qwen

D9_EPOCHS = int(os.getenv("D9_EPOCHS", "500"))

def resumir(texto: str, usar_fallback: bool = True):
    original = limpiar_espacios(texto)
    if not original:
        raise ValueError("La descripción no puede estar vacía.")

    captura_d9, _ = generar_resumen_d9(original, nivel=1, epochs=D9_EPOCHS)
    resumen_editado, _ = qwen.editar(captura_d9)

    return {
        "resumen": resumen_editado,
        "requiere_revision": True,
        "fuente_final": "D9_QWEN_EDITOR",
        "captura_d9": captura_d9,
        "version_pipeline": "d9-qwen-editor-1.0",
    }
