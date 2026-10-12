"""Cliente HTTP para el editor Qwen servido por llama-server.

Qwen NO vuelve a resumir la queja original: recibe la captura extractiva de D9
para producir un borrador más legible. La salida siempre requiere revisión humana.
"""
import json
import os
from urllib import request, error

MODEL_ID = os.getenv("QWEN_MODEL_ID", "Qwen/Qwen3-4B-Instruct-2507-Q4_K_M")
QWEN_SERVER_URL = os.getenv("QWEN_SERVER_URL", "http://10.89.0.2:8080").rstrip("/")
QWEN_TIMEOUT = float(os.getenv("QWEN_TIMEOUT", "120"))

SYSTEM_PROMPT = """Eres un editor conservador de texto para una Defensoría.
El texto que recibirás YA FUE RESUMIDO mediante selección extractiva. NO lo vuelvas a resumir.
Tu única tarea es mejorar cohesión y legibilidad.

Puedes:
- agregar conectores mínimos;
- corregir puntuación;
- hacer ajustes gramaticales mínimos.

Debes preservar estrictamente:
- todos los hechos presentes;
- los actores y quién hizo qué a quién;
- negaciones;
- cantidades y singular/plural;
- orden y relaciones temporales;
- relaciones causales;
- incertidumbre o ambigüedad del original;
- el significado central de cada acción.

No unas actores que podrían ser personas distintas. No conviertas proximidad entre frases en causalidad.
No sustituyas una acción por otra: por ejemplo, acosar no significa acusar y negar no significa rechazar.
No agregues hechos, diagnósticos, delitos, intenciones ni conclusiones.
Devuelve únicamente el texto editado, sin explicaciones ni encabezados."""

class QwenEditor:
    def editar(self, captura_d9: str):
        texto = str(captura_d9 or "").strip()
        if not texto:
            return "", {"modelo": MODEL_ID}

        payload = {
            "model": MODEL_ID,
            "messages": [
                {"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user", "content": texto},
            ],
            "temperature": 0,
            "max_tokens": 300,
            "repetition_penalty": 1.05,
        }
        data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        req = request.Request(
            f"{QWEN_SERVER_URL}/v1/chat/completions",
            data=data,
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        try:
            with request.urlopen(req, timeout=QWEN_TIMEOUT) as resp:
                body = json.loads(resp.read().decode("utf-8"))
        except (error.URLError, TimeoutError, json.JSONDecodeError) as exc:
            raise RuntimeError(f"No fue posible consultar llama-server: {exc}") from exc

        try:
            salida = body["choices"][0]["message"]["content"].strip()
        except (KeyError, IndexError, TypeError, AttributeError) as exc:
            raise RuntimeError("Respuesta inesperada de llama-server.") from exc
        if not salida:
            raise RuntimeError("llama-server devolvió una respuesta vacía.")
        return salida, {"modelo": MODEL_ID}

qwen = QwenEditor()
