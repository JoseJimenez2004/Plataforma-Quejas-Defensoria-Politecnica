import json
import os
from urllib import request

BASE_URL = os.getenv("RESUMEN_URL", "http://127.0.0.1:8080").rstrip("/")
payload = {
    "descripcion": "Una profesora comenzó a realizar comentarios inapropiados. Después acudí con las autoridades escolares para solicitar ayuda. Quiero saber qué puedo hacer ante esta situación.",
    "usar_fallback": True,
}
req = request.Request(
    f"{BASE_URL}/resumir",
    data=json.dumps(payload).encode("utf-8"),
    headers={"Content-Type": "application/json"},
    method="POST",
)
with request.urlopen(req, timeout=180) as resp:
    print(resp.read().decode("utf-8"))
