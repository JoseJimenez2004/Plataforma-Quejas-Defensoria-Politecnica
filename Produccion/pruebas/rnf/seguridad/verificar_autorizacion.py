#!/usr/bin/env python3
"""Prueba ESTÁTICA de autorización (caja blanca).

Compara la matriz rol x endpoint que sale del código (extraer_endpoints.py) contra las
reglas de reglas-autorizacion.csv, que vienen de los RNF y reglas de negocio.
Sale con código 1 si algún endpoint permite más (o menos) roles de los esperados.

Uso: python3 verificar_autorizacion.py matriz-endpoints.csv reglas-autorizacion.csv
"""
import csv
import re
import sys

matriz = list(csv.DictReader(open(sys.argv[1], encoding="utf-8")))
reglas = list(csv.DictReader(open(sys.argv[2], encoding="utf-8")))

fallos, revisados = [], 0
for regla in reglas:
    patron = re.compile(regla["ruta_regex"])
    esperados = set(regla["roles_esperados"].split("|"))
    coincidencias = [
        e for e in matriz
        if e["servicio"] == regla["servicio"]
        and (regla["metodo"] == "*" or e["metodo"] == regla["metodo"])
        and patron.search(e["ruta"])
    ]
    if not coincidencias:
        fallos.append(f"{regla['requisito']}: ningún endpoint coincide con {regla['ruta_regex']} "
                      f"en {regla['servicio']} (¿cambió la ruta?)")
    for e in coincidencias:
        revisados += 1
        reales = set(e["roles_permitidos"].split("|"))
        if reales != esperados:
            sobran = reales - esperados
            fallos.append(
                f"{regla['requisito']}: {e['metodo']} {e['ruta']} permite {sorted(reales)}; "
                f"se esperaba {sorted(esperados)}"
                + (f" -> roles de más: {sorted(sobran)}" if sobran else "")
                + f"  [{e['controller']}]"
            )

print(f"Endpoints revisados contra reglas: {revisados} (de {len(matriz)} en la matriz)")
if fallos:
    print(f"\nFALLAS ({len(fallos)}):")
    for f in fallos:
        print("  - " + f)
    sys.exit(1)
print("OK: todas las reglas de autorización se cumplen en el código.")
