#!/usr/bin/env python3
"""
Genera N quejas sinteticas NUEVAS (redacciones distintas a las del corpus del modelo) y las
deja listas para cargar-quejas.sh, reutilizando:
  - Modelo-Java/generar_quejas_variadas.py  (redaccion variada, denunciados reincidentes)
  - generar-csv.py                          (formato | para cargar-quejas.sh)
No modifica Modelo-Java/Dataset/quejas_sinteticas.json.

Uso (desde Backend/carga-masiva):
    python3 generar-lote.py --cantidad 1012 --salida lote-1012 --password 'Prueba2026!'
"""
import argparse, importlib.util, json, os, random, subprocess, sys

AQUI = os.path.dirname(os.path.abspath(__file__))
GENERADOR = os.path.join(AQUI, "..", "..", "Modelo-Java", "generar_quejas_variadas.py")

p = argparse.ArgumentParser()
p.add_argument("--cantidad", type=int, required=True)
p.add_argument("--salida", default="lote-nuevo")
p.add_argument("--password", default="")
p.add_argument("--semilla", type=int, default=20261027)
p.add_argument("--evidencias", default="EJEMPLO")
a = p.parse_args()

spec = importlib.util.spec_from_file_location("gqv", GENERADOR)
gqv = importlib.util.module_from_spec(spec)
spec.loader.exec_module(gqv)

rng = random.Random(a.semilla)
reincidentes = gqv.crear_reincidentes(rng)
quejas, textos, correos = [], set(), set()
i = 0
while len(quejas) < a.cantidad:
    i += 1
    q = gqv.generar_queja(5000 + i, rng, reincidentes)
    c = q["quejoso"]["correo"]
    if q["unidad_academica"] == "ESIQIE":                   # no esta en el catalogo de dependencias
        continue
    if q["texto_original"] in textos or c in correos:      # sin textos ni correos repetidos
        continue
    textos.add(q["texto_original"]); correos.add(c)
    q["folio"] = f"FOL-LOTE-{len(quejas) + 1:04d}"
    quejas.append(q)

os.makedirs(a.salida, exist_ok=True)
ruta_json = os.path.join(a.salida, "quejas_lote.json")
with open(ruta_json, "w", encoding="utf-8") as f:
    json.dump({"total": len(quejas), "quejas": quejas}, f, ensure_ascii=False, indent=1)

cmd = [sys.executable, os.path.join(AQUI, "generar-csv.py"), ruta_json, "--salida", a.salida,
       "--evidencias", a.evidencias, "--semilla", str(a.semilla)]
if a.password:
    cmd += ["--password", a.password]
subprocess.run(cmd, check=True)

palabras = [len(q["texto_original"].split()) for q in quejas]
tipos = {}
for q in quejas:
    tipos[q["tipo_violencia"]] = tipos.get(q["tipo_violencia"], 0) + 1
print(f"Palabras por queja: min={min(palabras)} max={max(palabras)} promedio={sum(palabras)//len(palabras)}")
print("Por tipo:", ", ".join(f"{k}={v}" for k, v in sorted(tipos.items(), key=lambda x: -x[1])))
