#!/usr/bin/env python3
"""Compuerta de cobertura (criterio del plan §5.1: >= 70 % de líneas en reglas y servicios).

Lee target/site/jacoco/jacoco.csv de cada servicio y calcula la cobertura de líneas SOLO
de los paquetes que toman decisiones (service, validacion, util, reglas). Los paquetes de
config/dto/entity/model ya los excluye el pom.

Uso: python3 cobertura.py <umbral> Backend/queja-service Backend/revision-service ...
"""
import csv
import os
import sys

PAQUETES = ("service", "validacion", "util", "reglas", "motor")
umbral = float(sys.argv[1])
fallas = []
print(f"{'Servicio':28} {'Líneas':>8} {'Cubiertas':>10} {'%':>7}")
for d in sys.argv[2:]:
    ruta = os.path.join(d, "target", "site", "jacoco", "jacoco.csv")
    if not os.path.exists(ruta):
        print(f"{os.path.basename(d):28} {'sin reporte JaCoCo':>27}")
        fallas.append(f"{d}: no hay jacoco.csv (¿corrió mvn verify?)")
        continue
    cub = fal = 0
    for fila in csv.DictReader(open(ruta, encoding="utf-8")):
        if any(p in fila["PACKAGE"].split(".") for p in PAQUETES):
            cub += int(fila["LINE_COVERED"])
            fal += int(fila["LINE_MISSED"])
    total = cub + fal
    pct = 100 * cub / total if total else 0.0
    print(f"{os.path.basename(d):28} {total:8} {cub:10} {pct:6.1f}%")
    if total and pct < umbral:
        fallas.append(f"{os.path.basename(d)}: {pct:.1f} % < {umbral} %")

if fallas:
    print("\nDebajo del umbral:\n  - " + "\n  - ".join(fallas))
    sys.exit(1)
print(f"\nOK: todos los servicios >= {umbral} %")
