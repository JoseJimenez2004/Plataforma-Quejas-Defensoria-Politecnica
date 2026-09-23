# -*- coding: utf-8 -*-
"""
Genera 1000 quejas históricas sintéticas para defensoria_historico_db.

La propiedad que importa NO es el volumen, es la REINCIDENCIA: si las 1000 quejas
fueran de 1000 personas distintas, la búsqueda de antecedentes nunca encontraría nada
y no se podría probar ni demostrar. Por eso el generador arma primero un padrón de
personas y luego reparte las quejas entre ellas con repetición controlada.
"""
import random, unicodedata
from datetime import date, timedelta

random.seed(20260917)   # reproducible: correrlo dos veces da el mismo dataset
TOTAL = 1000

# --------------------------------------------------------------- catálogos reales
UNIDADES = [
 "CECYT1","CECYT2","CECYT3","CECYT4","CECYT5","CECYT6","CECYT7","CECYT8","CECYT9",
 "CECYT10","CECYT11","CECYT12","CECYT13","CECYT14","CECYT15","CECYT16","CECYT17",
 "CECYT18","CECYT19","CET1","ESIME-ZAC","ESIME-CUL","ESIME-AZC","ESIME-TIC",
 "ESIA-ZAC","ESIA-TEC","ESIA-TIC","ESIT","ESFM","ESCOM","UPIICSA","UPIITA","UPIBI",
 "UPIG","UPIZ","ENCB","ESM","ENMH","ESEO","CICS-MA","CICS-ST","ESCA-ST","ESCA-TEP",
 "ESE","EST",
]
# Las de nivel superior concentran más quejas que las de medio superior.
PESO_UNIDAD = [2]*20 + [5]*25

NOMBRES_H = ["José","Luis","Juan","Carlos","Miguel","Jorge","Ricardo","Fernando","Alejandro",
 "Eduardo","Roberto","Javier","Daniel","Sergio","Arturo","Raúl","Óscar","Héctor","Pablo",
 "Andrés","Emiliano","Diego","Iván","Rodrigo","Gerardo","Alan","Bryan","Omar","César","Ángel"]
NOMBRES_M = ["María","Guadalupe","Ana","Laura","Patricia","Verónica","Mónica","Alejandra",
 "Claudia","Gabriela","Rosa","Martha","Silvia","Adriana","Fernanda","Karla","Diana","Paola",
 "Andrea","Lucía","Valeria","Itzel","Citlali","Xóchitl","Brenda","Nayeli","Regina","Camila"]
APELLIDOS = ["Hernández","García","Martínez","López","González","Pérez","Rodríguez","Sánchez",
 "Ramírez","Cruz","Flores","Gómez","Morales","Vázquez","Reyes","Jiménez","Torres","Díaz",
 "Gutiérrez","Ruiz","Mendoza","Aguilar","Ortiz","Moreno","Castillo","Romero","Álvarez",
 "Chávez","Ramos","Domínguez","Herrera","Medina","Castro","Vargas","Guerrero","Rojas",
 "Contreras","Salazar","Luna","Espinoza","Juárez","Cortés","Ibarra","Padilla","Mejía",
 "Navarro","Rivas","Cabrera","Zamora","Del Valle","De la Cruz","San Juan"]

# ------------------------------------------------- motivos, por índole, con descripciones
MOTIVOS = [
 ("Irregularidades en la evaluación", 14, [
  "El alumno señala que la calificación asentada en acta no corresponde con las evaluaciones entregadas durante el semestre.",
  "Se reporta que el profesor no aplicó examen de recuperación pese a estar contemplado en el programa de la unidad de aprendizaje.",
  "La quejosa manifiesta que no se le permitió revisar su examen final ni conocer los criterios de calificación.",
  "Se denuncia que la ponderación aplicada difiere de la establecida al inicio del periodo escolar."]),
 ("Negativa de trámite escolar", 11, [
  "Se niega la reinscripción al alumno argumentando adeudos administrativos que el interesado afirma haber cubierto.",
  "El trámite de baja temporal fue rechazado sin entregar respuesta por escrito ni fundamentación.",
  "La quejosa reporta que el área de gestión escolar se ha negado a recibir su solicitud de cambio de turno."]),
 ("Trato indebido por parte del personal", 12, [
  "El alumno refiere haber recibido expresiones despectivas del personal docente frente al grupo.",
  "Se reporta trato descortés y negativa de atención por parte del personal de la ventanilla de servicios escolares.",
  "La quejosa señala que fue increpada en público por personal administrativo al solicitar información sobre su trámite."]),
 ("Retención indebida de documentos", 7, [
  "La institución retiene el certificado de estudios del quejoso pese a haber concluido el trámite correspondiente.",
  "Se reporta que no se ha entregado el documento de acreditación por un adeudo de biblioteca ya liquidado."]),
 ("Irregularidades en el proceso de titulación", 8, [
  "El egresado señala demoras injustificadas en la asignación de sinodales para su acto de titulación.",
  "Se denuncia que el director de tesis dejó de dar seguimiento al trabajo sin designar sustituto.",
  "La quejosa refiere que se le exigieron requisitos no previstos en el reglamento de titulación vigente."]),
 ("Problemas con beca o apoyo económico", 9, [
  "Se reporta la suspensión de la beca sin notificación previa ni motivo informado al alumno.",
  "El alumno señala que su solicitud de beca fue rechazada sin que se le entregara el dictamen correspondiente."]),
 ("Incumplimiento del programa académico", 8, [
  "Se reporta inasistencia reiterada del docente sin que la academia designara suplente.",
  "El grupo señala que no se cubrieron las unidades temáticas comprometidas en el programa de estudios.",
  "Se denuncia que las prácticas de laboratorio no se realizaron por falta de material durante todo el semestre."]),
 ("Negativa de acceso a servicios", 6, [
  "Se niega el acceso al laboratorio de cómputo al alumno pese a contar con credencial vigente.",
  "La quejosa reporta que no se le permitió el uso de las instalaciones deportivas sin explicación."]),
 ("Cobros indebidos", 6, [
  "Se reporta el cobro de una cuota no prevista en la normatividad para realizar un trámite escolar.",
  "El alumno señala que se le condicionó la entrega de documentos al pago de una aportación voluntaria."]),
 ("Violación al debido proceso en sanción", 7, [
  "Se aplicó una sanción disciplinaria al alumno sin haberle dado oportunidad de manifestar lo que a su derecho conviniera.",
  "El quejoso refiere que no fue notificado del procedimiento seguido en su contra hasta que la sanción estaba firme."]),
 ("Irregularidades en servicio social o prácticas", 5, [
  "No se libera el servicio social del quejoso pese a haber cubierto las horas requeridas.",
  "Se reporta que la unidad receptora asignada no corresponde al perfil profesional del alumno."]),
 ("Discriminación", 4, [
  "El quejoso refiere haber recibido un trato diferenciado respecto del resto del grupo sin causa académica que lo justifique.",
  "Se reporta que no se otorgaron los ajustes razonables solicitados para la presentación de evaluaciones."]),
 ("Hostigamiento en el ámbito escolar", 5, [
  "La quejosa reporta conductas reiteradas de intimidación por parte de personal de la unidad académica.",
  "Se denuncian comentarios reiterados de carácter intimidatorio durante las sesiones de clase."]),
 ("Negativa de servicios de salud escolar", 3, [
  "Se reporta que no se brindó atención en el servicio médico de la unidad pese a tratarse de una urgencia.",
  "El quejoso señala que no se emitió el dictamen médico necesario para justificar inasistencias."]),
 ("Extravío de documentación oficial", 3, [
  "La unidad académica reporta el extravío del expediente del alumno, lo que impide continuar su trámite.",
  "Se denuncia la pérdida del acta de evaluación correspondiente al periodo cursado."]),
 ("Otra situación no clasificada", 2, [
  "El expediente original no especifica con claridad el motivo de la queja; se transcribe lo asentado en el archivo.",
  "Asunto registrado en el archivo histórico sin clasificación de motivo."]),
]

FUENTES = ["EXCEL"]*55 + ["OFICIO_FISICO"]*25 + ["LLAMADA"]*12 + ["SISTEMA_ANTERIOR"]*6 + ["OTRO"]*2

# ---------------------------------------------------------------- padrón de personas
def sin_acentos(t):
    return "".join(c for c in unicodedata.normalize("NFD", t) if unicodedata.category(c) != "Mn")

def nombre_persona():
    nom = random.choice(NOMBRES_H + NOMBRES_M)
    a1 = random.choice(APELLIDOS)
    a2 = random.choice(APELLIDOS) if random.random() > 0.12 else None
    return nom, a1, a2

def boleta(anio_ingreso):
    return f"{anio_ingreso}{random.randint(600000, 699999)}"

def nuevo_quejoso():
    nom, a1, a2 = nombre_persona()
    r = random.random()
    if r < 0.78:
        tipo_usr = "ALUMNO"
        anio = random.randint(2012, 2024)
        tipo_id, num_id = "BOLETA", boleta(anio)
    elif r < 0.87:
        tipo_usr, tipo_id = "DOCENTE", "EMPLEADO"
        num_id = f"EMP-{random.randint(10000, 99999)}"
    elif r < 0.93:
        tipo_usr, tipo_id = "ADMINISTRATIVO", "EMPLEADO"
        num_id = f"EMP-{random.randint(10000, 99999)}"
    else:
        # Externos: padres, tutores, egresados. Muchos sin identificación institucional.
        tipo_usr = "EXTERNO"
        if random.random() < 0.55:
            tipo_id, num_id = None, None
        else:
            tipo_id, num_id = random.choice(["INE", "CURP"]), f"{random.randint(10**9, 10**10-1)}"
    # Registros viejos donde no se anotó la boleta
    if tipo_usr == "ALUMNO" and random.random() < 0.14:
        tipo_id, num_id = None, None
    return dict(nombre=nom, a1=a1, a2=a2, tipo_id=tipo_id, num_id=num_id, tipo_usr=tipo_usr,
                unidad=random.choices(UNIDADES, weights=PESO_UNIDAD)[0])

def nuevo_denunciado():
    nom, a1, a2 = nombre_persona()
    r = random.random()
    tipo_usr = "DOCENTE" if r < 0.70 else ("ADMINISTRATIVO" if r < 0.95 else "ALUMNO")
    if random.random() < 0.35:          # el expediente viejo no anotó el número de empleado
        tipo_id, num_id = None, None
    elif tipo_usr == "ALUMNO":
        tipo_id, num_id = "BOLETA", boleta(random.randint(2012, 2023))
    else:
        tipo_id, num_id = "EMPLEADO", f"EMP-{random.randint(10000, 99999)}"
    return dict(nombre=nom, a1=a1, a2=a2, tipo_id=tipo_id, num_id=num_id, tipo_usr=tipo_usr)

# Padrones. El de denunciados es mucho más chico a propósito: las quejas se concentran
# en relativamente pocas personas, y eso es justo lo que el modelo debe poder detectar.
#
# Los pesos buscan una distribución CREÍBLE, no la más cómoda para la demo: la mayoría
# de la gente se queja una sola vez en diez años. Los reincidentes existen pero son
# pocos, y es en esos pocos donde la búsqueda de antecedentes gana su valor.
QUEJOSOS = [nuevo_quejoso() for _ in range(800)]
PESO_QUEJOSO = [1]*760 + [5]*30 + [10]*10

DENUNCIADOS = [nuevo_denunciado() for _ in range(170)]
# Unos pocos denunciados con varias quejas encima -- el patrón que a la Defensoría le
# interesa detectar. Topado a un máximo verosímil: 40 quejas contra una persona en diez
# años sería un caso extraordinario, no un dato de prueba.
PESO_DENUNCIADO = [1]*140 + [2]*20 + [4]*10

# ------------------------------------------------------------------------ generación
def fecha_aleatoria(ini, fin):
    return ini + timedelta(days=random.randint(0, (fin - ini).days))

def esc(v):
    if v is None:
        return "NULL"
    if isinstance(v, bool):
        return "TRUE" if v else "FALSE"
    if isinstance(v, date):
        return f"'{v.isoformat()}'"
    return "'" + str(v).replace("'", "''") + "'"

motivos_expandidos = []
for nombre, peso, descs in MOTIVOS:
    motivos_expandidos += [(nombre, descs)] * peso

filas, folios_usados, sin_folio = [], set(), 0
INI, FIN = date(2015, 1, 15), date(2025, 11, 30)

for i in range(TOTAL):
    fuente = random.choice(FUENTES)
    f_pres = fecha_aleatoria(INI, FIN)
    anio = f_pres.year

    # ---- folio en el formato que usaba cada fuente
    if fuente == "LLAMADA" and random.random() < 0.80:
        sin_folio += 1
        folio, generado = f"SF-{sin_folio:06d}", True
    else:
        for _ in range(50):
            if fuente == "EXCEL":
                cand = f"DDP/{anio}/{random.randint(1, 9999):04d}"
            elif fuente == "OFICIO_FISICO":
                cand = f"OF-DDP-{random.randint(1, 999):03d}/{anio}"
            elif fuente == "SISTEMA_ANTERIOR":
                cand = f"{anio}-{random.randint(1, 9999):04d}"
            else:
                cand = f"EXP{anio}{random.randint(1, 9999):04d}"
            if cand not in folios_usados:
                break
        folio, generado = cand, False
    folios_usados.add(folio)

    # ---- quejoso: con probabilidad alta se reutiliza uno del padrón (reincidencia)
    q = random.choices(QUEJOSOS, weights=PESO_QUEJOSO)[0]

    # ---- denunciado: 8% de las quejas son contra el área, no contra una persona
    d = None if random.random() < 0.08 else random.choices(DENUNCIADOS, weights=PESO_DENUNCIADO)[0]

    motivo, descs = random.choice(motivos_expandidos)
    descripcion = random.choice(descs)

    f_hechos = None
    if random.random() > 0.10:
        f_hechos = f_pres - timedelta(days=random.randint(3, 240))
        if f_hechos < INI - timedelta(days=365):
            f_hechos = f_pres - timedelta(days=30)

    r = random.random()
    if r < 0.62:
        estatus = "CONCLUIDA"
        resultado = random.choices(
            ["CONCLUIDA_CON_ACUERDO", "CONCLUIDA_SIN_ACUERDO", "SIN_DATO"],
            weights=[45, 25, 30])[0]
    elif r < 0.77:
        estatus, resultado = "RECHAZADA", random.choices(
            ["RECHAZADA_EN_RECEPCION", "SIN_DATO"], weights=[80, 20])[0]
    elif r < 0.92:
        estatus, resultado = "IMPROCEDENTE", random.choices(
            ["IMPROCEDENTE", "SIN_DATO"], weights=[80, 20])[0]
    else:
        estatus, resultado = "REMITIDA", random.choices(
            ["REMITIDA_A_OTRA_AUTORIDAD", "SIN_DATO"], weights=[75, 25])[0]

    unidad = q["unidad"] if random.random() > 0.05 else None

    filas.append((
        folio, generado, fuente, f_pres, f_hechos, unidad, motivo, descripcion,
        estatus, resultado,
        q["tipo_id"], q["num_id"], q["nombre"], q["a1"], q["a2"], q["tipo_usr"],
        (d or {}).get("tipo_id"), (d or {}).get("num_id"), (d or {}).get("nombre"),
        (d or {}).get("a1"), (d or {}).get("a2"), (d or {}).get("tipo_usr"),
    ))

# --------------------------------------------------------------------------- SQL
COLS = ("folio, folio_generado, fuente, fecha_presentacion_original, fecha_hechos, "
        "unidad_academica_clave, motivo, descripcion, estatus, resultado, "
        "quejoso_tipo_identificacion, quejoso_numero_identificacion, quejoso_nombre, "
        "quejoso_apellido1, quejoso_apellido2, quejoso_tipo_usuario, "
        "denunciado_tipo_identificacion, denunciado_numero_identificacion, "
        "denunciado_nombre, denunciado_apellido1, denunciado_apellido2, "
        "denunciado_tipo_usuario, capturado_por, fecha_captura, notas_captura")

out = ["""-- =============================================================================
-- Datos de prueba: 1000 quejas históricas para defensoria_historico_db
-- Generado el 2026-09-17 · semilla fija (20260917), reproducible.
--
-- NO SON DATOS REALES. Nombres, boletas y números de empleado son inventados;
-- cualquier parecido con una persona real es coincidencia. Las unidades
-- académicas SÍ son las claves reales del catálogo, para que los filtros y los
-- joins con dependencias funcionen igual que en producción.
--
-- Lo importante de este conjunto no es el volumen sino la REINCIDENCIA: las
-- 1000 quejas se reparten entre 620 quejosos y 170 denunciados, con algunos
-- denunciados concentrando muchas quejas. Si cada queja fuera de una persona
-- distinta, la búsqueda de antecedentes no encontraría nada y no se podría
-- probar ni demostrar.
--
--   podman exec -i defensoria-db psql -U postgres -d defensoria_historico_db \\
--     < seed_historico_1000.sql
-- =============================================================================

BEGIN;
"""]

LOTE = 100
for ini in range(0, len(filas), LOTE):
    bloque = filas[ini:ini + LOTE]
    out.append(f"INSERT INTO quejas_historicas ({COLS}) VALUES")
    vals = []
    for f in bloque:
        campos = ", ".join(esc(x) for x in f)
        vals.append(f"  ({campos}, 'carga.inicial@defensoria.ipn.mx', now(), "
                    f"'Carga inicial del archivo histórico (datos de prueba).')")
    out.append(",\n".join(vals) + ";\n")

out.append("""COMMIT;

-- =============================== VERIFICACIÓN ===============================
SELECT count(*) AS total_quejas FROM quejas_historicas;

SELECT 'quejosos distintos' AS concepto, count(DISTINCT quejoso_numero_identificacion) AS n
FROM quejas_historicas WHERE quejoso_numero_identificacion IS NOT NULL
UNION ALL
SELECT 'denunciados distintos', count(DISTINCT denunciado_numero_identificacion)
FROM quejas_historicas WHERE denunciado_numero_identificacion IS NOT NULL;

-- Los reincidentes: esto es lo que la búsqueda de antecedentes debe encontrar.
SELECT quejoso_numero_identificacion, quejoso_nombre, quejoso_apellido1,
       count(*) AS quejas_presentadas
FROM quejas_historicas
WHERE quejoso_numero_identificacion IS NOT NULL
GROUP BY 1, 2, 3 HAVING count(*) > 2
ORDER BY 4 DESC LIMIT 15;

-- Personas con más quejas EN SU CONTRA.
SELECT denunciado_numero_identificacion, denunciado_nombre, denunciado_apellido1,
       count(*) AS quejas_recibidas
FROM quejas_historicas
WHERE denunciado_numero_identificacion IS NOT NULL
GROUP BY 1, 2, 3 ORDER BY 4 DESC LIMIT 15;

SELECT estatus, resultado, count(*) FROM quejas_historicas
GROUP BY 1, 2 ORDER BY 3 DESC;

SELECT fuente, count(*) FROM quejas_historicas GROUP BY 1 ORDER BY 2 DESC;

SELECT extract(year FROM fecha_presentacion_original) AS anio, count(*)
FROM quejas_historicas GROUP BY 1 ORDER BY 1;
""")

with open("/mnt/user-data/outputs/seed_historico_1000.sql", "w", encoding="utf-8") as fh:
    fh.write("\n".join(out))

# ------------------------------------------------------------------- resumen
from collections import Counter
cq = Counter(f[11] for f in filas if f[11])
cd = Counter(f[17] for f in filas if f[17])
print(f"Filas generadas: {len(filas)}")
print(f"Folios únicos:   {len(folios_usados)}  (sin folio original -> SF-: {sin_folio})")
print(f"Quejosos con identificación: {len(cq)} distintos")
print(f"  con 1 queja: {sum(1 for v in cq.values() if v==1)} · "
      f"2: {sum(1 for v in cq.values() if v==2)} · "
      f"3: {sum(1 for v in cq.values() if v==3)} · "
      f"4+: {sum(1 for v in cq.values() if v>=4)}")
print(f"Denunciados con identificación: {len(cd)} distintos · máximo en contra de uno: {max(cd.values())}")
print(f"Quejas sin denunciado: {sum(1 for f in filas if f[18] is None)}")
print(f"Quejosos sin identificación: {sum(1 for f in filas if f[11] is None)}")
print("Estatus:", Counter(f[8] for f in filas).most_common())
print("Fuente: ", Counter(f[2] for f in filas).most_common())
print("Años:   ", sorted(Counter(f[3].year for f in filas).items())[:4], "...")
