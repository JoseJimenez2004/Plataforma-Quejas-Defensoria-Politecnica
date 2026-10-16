#!/usr/bin/env python3
"""
Genera un quejas.csv listo para cargar-quejas.sh a partir del dataset sintetico del modelo
de IA (Modelo-Java/Dataset/quejas_sinteticas.json, 170 quejas con quejoso, denunciado,
unidad, fecha y redaccion completa).

Uso:
    python3 generar-csv.py ../../Modelo-Java/Dataset/quejas_sinteticas.json            # todas
    python3 generar-csv.py quejas_sinteticas.json --cantidad 50 --password Prueba2026!
    python3 generar-csv.py quejas_sinteticas.json --salida otra-carpeta

Crea en la carpeta de salida (por defecto ./lote):
    quejas.csv                el archivo para cargar-quejas.sh
    descripciones/NNN.txt     la redaccion de cada queja (va como @descripciones/NNN.txt
                              porque son textos largos, de varios parrafos)

Notas:
- Los nombres vienen en minusculas y sin acentos en el dataset; aqui se capitalizan.
- El dataset no trae fecha de nacimiento: se genera una de adulto (1985-2005) anterior a la
  fecha de los hechos, para que el formulario no pida datos de tutor.
- Los correos del dataset son inventados (@escom.ipn.mx, etc.). Con CUENTAS=sql no se manda
  ningun correo, asi que no hay rebotes.
- Las unidades se traducen a la clave del catalogo de dependencias. Las que no existen en el
  catalogo (ESIQIE, UPIIC) se mandan como --unidad-default (ESCOM).
"""
import argparse
import datetime
import json
import os
import random
import re
import sys

UNIDADES = {
    "ESCOM": "ESCOM", "ESCOM-IPN": "ESCOM",
    "ESCA TEPEPAN": "ESCA-TEP", "ESCA SANTO TOMAS": "ESCA-ST",
    "ESIA ZACATENCO": "ESIA-ZAC", "ESIA": "ESIA-ZAC", "ESIA-TECAMACHALCO": "ESIA-TEC",
    "ESIA TECAMACHALCO": "ESIA-TEC", "ESIA TICOMAN": "ESIA-TIC",
    "ESIME ZACATENCO": "ESIME-ZAC", "ESIME": "ESIME-ZAC",
    "ESIME-CULHUACAN": "ESIME-CUL", "ESIME CULHUACAN": "ESIME-CUL",
    "UPIICSA": "UPIICSA", "UPIBI": "UPIBI", "ESFM": "ESFM", "ESIT": "ESIT",
}
NOMBRE_VALIDO = re.compile(r"^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+(?:[ '\-][A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+)*$")
CAMPOS = ["nombre", "apellido1", "apellido2", "correo", "fecha_nacimiento", "tipo", "numero",
          "unidad", "fecha_hechos", "den_nombre", "den_apellido1", "den_apellido2",
          "descripcion", "password", "credenciales", "evidencias"]


def nombre(valor):
    """Capitaliza y limpia; si no pasa la regla de nombres del backend se deja vacio."""
    valor = " ".join((valor or "").replace("|", " ").split()).title()
    return valor if NOMBRE_VALIDO.match(valor) and 2 <= len(valor) <= 50 else ""


def limpio(valor):
    return " ".join(str(valor or "").replace("|", " ").split())


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("dataset")
    p.add_argument("--salida", default="lote")
    p.add_argument("--cantidad", type=int, default=0, help="0 = todas")
    p.add_argument("--password", default="", help="contrasena para todas las cuentas (vacio = sin cuenta)")
    p.add_argument("--evidencias", default="EJEMPLO", help="EJEMPLO, vacio o rutas separadas por ;")
    p.add_argument("--unidad-default", default="ESCOM")
    p.add_argument("--semilla", type=int, default=2026)
    a = p.parse_args()

    random.seed(a.semilla)
    with open(a.dataset, encoding="utf-8") as f:
        datos = json.load(f)
    quejas = datos["quejas"] if isinstance(datos, dict) else datos
    if a.cantidad:
        quejas = quejas[: a.cantidad]

    os.makedirs(os.path.join(a.salida, "descripciones"), exist_ok=True)
    hoy = datetime.date.today()
    filas, omitidas, boletas = [], 0, set()

    for i, q in enumerate(quejas, start=1):
        qj, dn = q.get("quejoso", {}), q.get("denunciado", {})
        nom, ap1 = nombre(qj.get("nombre")), nombre(qj.get("apellido1"))
        texto = (q.get("texto_original") or "").strip()
        correo = limpio(qj.get("correo")).lower()
        numero = re.sub(r"\D", "", str(qj.get("numero_identificacion", "")))[:10]
        if not (nom and ap1 and correo and len(texto) >= 20):
            omitidas += 1
            continue
        while not numero or numero in boletas:          # boleta unica por cuenta
            numero = str(random.randint(2019000000, 2025999999))
        boletas.add(numero)

        try:
            hechos = datetime.date.fromisoformat(q.get("fecha_hechos", ""))
        except ValueError:
            hechos = hoy - datetime.timedelta(days=random.randint(10, 600))
        hechos = min(hechos, hoy)
        nacimiento = datetime.date(random.randint(1985, 2005), random.randint(1, 12), random.randint(1, 28))
        if nacimiento >= hechos:
            nacimiento = hechos.replace(year=hechos.year - 20)

        archivo = f"descripciones/{i:03d}.txt"
        with open(os.path.join(a.salida, archivo), "w", encoding="utf-8") as f:
            f.write(texto + "\n")

        unidad = UNIDADES.get(limpio(q.get("unidad_academica")).upper(), a.unidad_default)
        tipo = "empleado" if qj.get("tipo_identificacion") == "empleado" else "alumno"
        filas.append([nom, ap1, nombre(qj.get("apellido2")), correo, nacimiento.isoformat(), tipo,
                      numero, unidad, hechos.isoformat(), nombre(dn.get("nombre")),
                      nombre(dn.get("apellido1")), nombre(dn.get("apellido2")), "@" + archivo,
                      a.password, "", a.evidencias])

    with open(os.path.join(a.salida, "quejas.csv"), "w", encoding="utf-8") as f:
        f.write("|".join(CAMPOS) + "\n")
        for fila in filas:
            f.write("|".join(fila) + "\n")

    print(f"{len(filas)} quejas en {os.path.join(a.salida, 'quejas.csv')} ({omitidas} omitidas por datos incompletos)")


if __name__ == "__main__":
    sys.exit(main())
