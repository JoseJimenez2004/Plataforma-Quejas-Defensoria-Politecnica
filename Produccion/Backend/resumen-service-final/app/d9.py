"""
Motor D9 congelado del prototipo DDP.

IMPORTANTE:
- Esta versión conserva el comportamiento experimental validado.
- El Skip-gram de D9 se ajusta localmente sobre las unidades de cada queja,
  tal como en el notebook congelado. No usa las 15 quejas de evaluación.
- Para producción de alto tráfico conviene perfilar latencia antes de cambiar
  este comportamiento, porque cambiarlo alteraría el modelo evaluado.
"""

# 1. IMPORTS + FUNCIONES AUXILIARES AUTOCONTENIDAS
import re
import json
import unicodedata
import pandas as pd
from IPython.display import display

def limpiar_espacios(x):
    return re.sub(r"\s+", " ", str(x or "")).strip()

def _n(x):
    x=limpiar_espacios(x).lower()
    x="".join(c for c in unicodedata.normalize("NFD",x)
              if unicodedata.category(c)!="Mn")
    return x

def asegurar_punto(texto):
    t=limpiar_espacios(texto)
    if not t:
        return ""
    if t[-1] not in ".!?":
        t += "."
    return t

def segmentar_descripcion(texto):
    t=limpiar_espacios(texto)
    if not t:
        return []
    partes=re.split(r"(?<=[.!?])\s+(?=[A-ZÁÉÍÓÚÑ¿¡])",t)
    return [limpiar_espacios(x) for x in partes if limpiar_espacios(x)]

CATEGORIAS_PERMITIDAS={"HECHO","EVIDENCIA","GESTION","CONSECUENCIA","SOLICITUD"}


# 3. Clasificador V6 original (para comparación)
# 2. Reproducción exacta del extractor V6
import re, json, os, unicodedata
from pathlib import Path
import pandas as pd
import numpy as np

def limpiar_espacios(t):
    return re.sub(r"\s+", " ", str(t or "")).strip()

def normalizar(t):
    t=limpiar_espacios(t).lower()
    t="".join(c for c in unicodedata.normalize("NFD",t)
              if unicodedata.category(c)!="Mn")
    return t

def segmentar_descripcion(texto):
    texto=limpiar_espacios(texto)
    if not texto: return []
    partes=re.split(r'(?<=[.!?])\s+(?=[A-ZÁÉÍÓÚÑ¿¡])', texto)
    return [limpiar_espacios(p) for p in partes if len(limpiar_espacios(p))>=8]

MARCADORES_APERTURA = (
    "quiero contar", "quisiera dejar constancia", "solicito atentamente su intervención",
    "me permito solicitar orientación", "necesito reportar algo", "voy directo al punto",
    "por medio del presente", "buenas, les escribo", "hola,", "acudo porque",
)
PATRONES_SOLICITUD = [
    r"\bsolicito\b", r"\bpido que\b", r"\bnecesito que\b", r"\bespero que puedan\b",
    r"\bagradecer[ií]a que\b", r"\blo [uú]nico que busco\b", r"\bque revisen\b",
    r"\bme indiquen qu[eé] procede\b", r"\bse requiera\b", r"\bse adopten las medidas\b",
    r"\bque se investiguen\b", r"\bque se me comunique\b", r"\borient(?:en|aran|ación)\b"
]
PATRONES_EVIDENCIA = [
    r"\bprueba", r"\bevidencia", r"\bcapturas? de pantalla\b", r"\bmensajes? escritos?\b",
    r"\bacuse", r"\bcopia simple\b", r"\bconstancia", r"\bdocumentaci[oó]n\b",
    r"\btestig", r"\bpersonas? que presenci", r"\bcredencial\b", r"\bexpediente\b",
    r"\bconservo copia\b", r"\bpuedo proporcionar los nombres\b"
]
PATRONES_GESTION = [
    r"\bpresent[eé] un escrito\b", r"\bacud[ií] (?:previamente )?ante\b",
    r"\bintent[eé] resolver\b", r"\blo plante[oó] ante\b", r"\breport[eé] la situaci[oó]n\b",
    r"\bsolicit[eé] .* revisi[oó]n\b", r"\bal acudir con\b",
    r"\bno he realizado gesti[oó]n previa\b", r"\bunidad acad[eé]mica .* inform[oó]\b",
    r"\bme indicaron que\b", r"\bsin haber obtenido respuesta\b"
]
PATRONES_CONSECUENCIA = [
    r"\bme preocupa\b", r"\bincertidumbre\b", r"\bha repercutido\b", r"\bme ha afectado\b",
    r"\bafectado mi\b", r"\bhan alterado\b", r"\bhe considerado solicitar baja\b",
    r"\bmodificado mis horarios\b", r"\bafectado su participaci[oó]n\b",
    r"\bafectado mi participaci[oó]n\b"
]
PATRONES_CONTEXTO = [
    r"\bcuando ocurri[oó] esto, yo estaba inscrito\b", r"\bal momento de los hechos\b",
    r"\btrayectoria acad[eé]mica regular\b", r"\bno registrar antecedentes disciplinarios\b",
    r"\brelaci[oó]n con la persona se[nñ]alada se origina\b",
    r"\bdesarrollo ordinario de las actividades acad[eé]micas\b",
    r"\bdentro del horario y las instalaciones institucionales\b",
    r"\bse inform[oó] a .* sobre el tratamiento\b",
]
PATRONES_FACTUALES = [
    r"\bme\b", r"\bmi\b", r"\bmis\b", r"\bprofesor", r"\bdocente", r"\bcompa[ñn]er",
    r"\bautoridad\b", r"\bpersonal\b", r"\bunidad acad[eé]mica\b", r"\bservicio\b",
    r"\bsolicitud\b", r"\bbeca\b", r"\bcalific", r"\bexamen\b", r"\btr[aá]mite\b",
    r"\bdictamen\b", r"\bexpediente\b", r"\bdocument", r"\binsult", r"\bgrit",
    r"\bamenaz", r"\bagred", r"\bgolpe", r"\bacos", r"\bhostig", r"\bdiscrimin",
    r"\bneg", r"\brechaz", r"\bsuspend", r"\bexig", r"\bcobr", r"\bpag",
    r"\bmodific", r"\bperdi", r"\bextravi", r"\bretuv", r"\bimpidi", r"\bcondicion",
    r"\bcomentarios?\b", r"\bacercamientos?\b", r"\bconductas?\b", r"\bexpresiones?\b",
    r"\bno se\b", r"\bfue\b", r"\bhan\b", r"\bha sido\b", r"\bocurri"
]
def coincide(texto, patrones):
    t=normalizar(texto)
    return any(re.search(p,t,re.I) for p in patrones)

def clasificar_unidad_v6(oracion):
    t=normalizar(oracion)
    if coincide(t,PATRONES_SOLICITUD): return "SOLICITUD"
    if coincide(t,PATRONES_EVIDENCIA): return "EVIDENCIA"
    if coincide(t,PATRONES_GESTION): return "GESTION"
    if coincide(t,PATRONES_CONSECUENCIA): return "CONSECUENCIA"
    if coincide(t,PATRONES_CONTEXTO): return "CONTEXTO"
    if any(t.startswith(normalizar(x)) for x in MARCADORES_APERTURA) and len(t.split())<18:
        return "CONTEXTO"
    if re.search(r"\bagradezco de antemano\b|\bquedo pendiente\b",t):
        return "CONTEXTO"
    if len(t.split())>=6 and coincide(t,PATRONES_FACTUALES):
        return "HECHO"
    verbos=r"\b(?:fue|era|estaba|estuvo|dijo|indic[oó]|inform[oó]|rechaz[oó]|neg[oó]|modific[oó]|exigi[oó]|recib[ií]|ocurri[oó]|realiz[oó]|suspendi[oó]|perdi[oó]|extravi[oó]|impidi[oó]|amenaz[oó]|insult[oó]|grit[oó]|agredi[oó]|golpe[oó]|cobr[oó]|solicit[oó]|present[oó]|reporta|se reporta|se neg[oó]|se exigi[oó]|se indic[oó]|se brind[oó]|no se brind[oó]|no se aplic[oó]|no se emiti[oó])\b"
    if len(t.split())>=8 and re.search(verbos,t,re.I): return "HECHO"
    return "CONTEXTO"

# 3. V6.4 — rescate de alta precisión

def _n(t):
    return normalizar(t)

# Barreras: aunque contengan palabras fuertes, estas unidades NO deben rescatarse.
BARRERAS_RESCATE = [
    # solicitud actual
    r"^\s*(?:finalmente,?\s*)?(?:yo\s+)?solicito\b",
    r"\bpido que\b", r"\bagradecer[ií]a que\b", r"\bespero que\b",
    r"\bme indiquen qu[eé] procede\b", r"\blo [uú]nico que busco\b",
    r"\bque se requiera\b", r"\bque se investiguen\b",
    # evidencia / respaldo
    r"\bcomo (?:prueba|evidencia|respaldo)\b", r"\badjunto\b", r"\banexo\b",
    r"\btengo (?:capturas|mensajes|correos|documentos|constancias|copia)\b",
    r"\bcuento con (?:capturas|mensajes|correos|documentos|constancias|copia)\b",
    r"\bconservo (?:capturas|mensajes|correos|documentos|constancias|copia)\b",
    r"\bpuedo proporcionar\b", r"\bhay testigos\b", r"\bpersonas? que presenci",
    # consecuencia / preocupación
    r"^\s*me preocupa\b", r"\bhe considerado solicitar baja\b",
    r"\bcomo consecuencia\b", r"\bha repercutido\b", r"\bme ha afectado\b",
    # meta/contexto puro
    r"\bmi intenci[oó]n es\b", r"\bme cost[oó] decidirme a escribir\b",
    r"\bquiero contar\b", r"\bquisiera dejar constancia\b",
    r"\bpor medio del presente\b", r"\bagradezco de antemano\b",
    r"\bquedo pendiente\b", r"\bdesarrollo ordinario de las actividades\b",
    r"\bcuando ocurri[oó] esto, yo estaba inscrito\b",
    r"\bal momento de los hechos\b",
]

# Gestión previa explícita: no se rescata solo por contener "revisión", "solicitud", etc.
GESTION_PREVIA_RESCATE = [
    r"^\s*solicit[eé]\b", r"^\s*present[eé]\b", r"^\s*acud[ií]\b",
    r"^\s*report[eé]\b", r"^\s*inform[eé]\b", r"^\s*ped[ií]\b",
    r"^\s*intent[eé]\b", r"^\s*interpuse\b", r"^\s*entregu[eé]\b",
]

RESCATE_DEBIDO_PROCESO = [
    r"\b(?:me|le|nos)?\s*(?:impusieron|impuso|aplicaron|aplic[oó])\s+(?:una\s+)?sanci[oó]n\b",
    r"\bfui sancionad[oa]\b", r"\bfue sancionad[oa]\b",
    r"\bse (?:me|le) sancion[oó]\b",
    r"\bsanci[oó]n\b.*\b(?:sin|no)\b.*\b(?:notific|audiencia|defensa|defender|escuchar|procedimiento)\b",
    r"\b(?:sin|no)\b.*\b(?:notific|audiencia|defensa|defender|escuchar)\b.*\bsanci[oó]n\b",
    r"\bno (?:me|le) permitieron? (?:defender|explicar|ser escuchad|presentar)\b",
    r"\bno (?:me|le) notificaron?\b.*\b(?:sanci[oó]n|procedimiento|resoluci[oó]n)\b",
    r"\b(?:procedimiento|resoluci[oó]n)\b.*\b(?:sin notific|sin audiencia|sin defensa)\b",
]

RESCATE_COBRO = [
    r"\b(?:me|le|nos)?\s*(?:cobraron|cobr[oó]|exigieron|exigi[oó])\b.*\b(?:pago|cuota|dinero|dep[oó]sito|transferencia|aportaci[oó]n)\b",
    r"\b(?:pago|cuota|dep[oó]sito|transferencia|aportaci[oó]n)\b.*\b(?:obligatori|condici[oó]n|requisito|exig)\b",
    r"\bcondicion[oó]\b.*\b(?:pago|cuota|dinero|dep[oó]sito|transferencia)\b",
]

RESCATE_AMENAZA = [
    r"\b(?:me|le|nos)?\s*amenaz", r"\bintimid", r"\brepresalia",
    r"\badvirti[oó]\b.*\b(?:reprobar|sancion|expuls|perjudic|afectar|consecuencia)\b",
    r"\b(?:si|en caso de que)\b.*\b(?:queja|denuncia|acus)\b.*\b(?:reprobar|sancion|expuls|perjudic|afectar)\b",
]

RESCATE_NEGATIVA = [
    r"\b(?:se|me|le)\s+neg[oó]\b.*\b(?:acceso|servicio|tr[aá]mite|inscripci[oó]n|reinscripci[oó]n|documento|constancia|atenci[oó]n|solicitud)\b",
    r"\b(?:rechazaron|rechaz[oó]|fue rechazada|fue rechazado)\b.*\b(?:solicitud|tr[aá]mite|inscripci[oó]n|reinscripci[oó]n|beca|servicio)\b",
    r"\bno (?:me|le) permitieron?\b.*\b(?:acceder|ingresar|inscribir|realizar|continuar)\b",
    r"\bimpidieron?\b.*\b(?:acceso|ingreso|tr[aá]mite|inscripci[oó]n|servicio)\b",
]

RESCATE_INSTITUCIONAL = [
    r"\b(?:la|el) (?:unidad acad[eé]mica|jefatura|coordinaci[oó]n|gesti[oó]n escolar|servicios? escolares?|autoridad)\b.*\b(?:determin[oó]|resolvi[oó]|cancel[oó]|suspendi[oó]|retuvo|modific[oó])\b",
    r"\b(?:retuvieron|retuvo)\b.*\b(?:certificado|documento|constancia|credencial|expediente)\b",
    r"\b(?:modificaron|modific[oó])\b.*\b(?:calificaci[oó]n|evaluaci[oó]n|porcentaje|resultado)\b",
]

def barrera_rescate(oracion):
    t=_n(oracion)
    return any(re.search(p,t,re.I) for p in BARRERAS_RESCATE)

def gestion_previa_rescate(oracion):
    t=_n(oracion)
    return any(re.search(p,t,re.I) for p in GESTION_PREVIA_RESCATE)

def tipo_rescate_v64(oracion):
    """Devuelve el tipo de rescate o None."""
    t=_n(oracion)
    if len(t.split()) < 5:
        return None
    if barrera_rescate(oracion):
        return None

    grupos=[
        ("DEBIDO_PROCESO",RESCATE_DEBIDO_PROCESO),
        ("COBRO",RESCATE_COBRO),
        ("AMENAZA",RESCATE_AMENAZA),
        ("NEGATIVA",RESCATE_NEGATIVA),
        ("INSTITUCIONAL",RESCATE_INSTITUCIONAL),
    ]
    for nombre,pats in grupos:
        if any(re.search(p,t,re.I) for p in pats):
            # Si es una gestión previa, solo permitimos rescate cuando además describe
            # una acción adversa institucional explícita dentro de la misma unidad.
            if gestion_previa_rescate(oracion) and nombre not in {"NEGATIVA","INSTITUCIONAL"}:
                return None
            return nombre
    return None

def clasificar_unidad_v64(oracion):
    base = clasificar_unidad_v6(oracion)
    t = _n(oracion)

    # 1. Barreras de alta confianza.
    # Se aplican ANTES de conservar el HECHO de V6.

    # Meta-discurso / introducciones
    if re.search(
        r"\bmi intenci[oó]n es\b|"
        r"\bme cost[oó] decidirme a escribir\b|"
        r"\bquiero contar\b|"
        r"\bpor medio del presente\b|"
        r"\bagradezco de antemano\b|"
        r"\bquedo pendiente\b",
        t,
        re.I
    ):
        return "CONTEXTO"

    # Solicitud actual
    if re.search(
        r"^\s*(?:finalmente,?\s*)?(?:yo\s+)?solicito\b|"
        r"^\s*pido que\b|"
        r"\bagradecer[ií]a que\b|"
        r"\bme indiquen qu[eé] procede\b",
        t,
        re.I
    ):
        return "SOLICITUD"

    # Evidencia explícita
    if re.search(
        r"\bcomo (?:prueba|evidencia|respaldo)\b|"
        r"\badjunto\b|"
        r"\banexo\b|"
        r"\btengo (?:capturas|mensajes|correos|documentos|constancias|copia)\b|"
        r"\bcuento con (?:capturas|mensajes|correos|documentos|constancias|copia)\b|"
        r"\bconservo (?:capturas|mensajes|correos|documentos|constancias|copia)\b",
        t,
        re.I
    ):
        return "EVIDENCIA"

    # Consecuencia explícita
    if re.search(
        r"^\s*me preocupa\b|"
        r"\bhe considerado solicitar baja\b|"
        r"\bcomo consecuencia\b|"
        r"\bha repercutido\b|"
        r"\bme ha afectado\b",
        t,
        re.I
    ):
        return "CONSECUENCIA"

    # 2. Si pasó las barreras y V6 ya decía HECHO,
    # conservamos la clasificación.
    if base == "HECHO":
        return "HECHO"

    # 3. Si V6 NO encontró un hecho,
    # V6.4 intenta rescatar únicamente patrones de alta precisión.
    rescate = tipo_rescate_v64(oracion)

    if rescate:
        return "HECHO"

    # 4. Si no hay evidencia suficiente para cambiarlo,
    # mantenemos la clasificación original de V6.
    return base

# 4. Rescates adicionales V6.6
# IMPORTANTE: requieren relaciones completas; una palabra aislada no basta.

V66_DEBIDO_PROCESO = [
    # Notificación cuando la sanción ya estaba impuesta / resuelta
    r"\bno (?:fui|fue|me|le)?\s*notificad[oa]?\b.*\b(?:hasta|despu[eé]s)\b.*\b(?:sanci[oó]n|resoluci[oó]n|procedimiento)\b",
    r"\b(?:notificad[oa]|notificaron)\b.*\b(?:hasta|despu[eé]s)\b.*\b(?:sanci[oó]n|ya estaba impuesta|resoluci[oó]n)\b",
    r"\bsanci[oó]n\b.*\bya estaba impuesta\b.*\b(?:notific|defensa|pruebas|alegar)\b",
    # Imposibilidad concreta de defensa
    r"\b(?:impidi[oó]|impidieron|no permiti[oó]|no permitieron)\b.*\b(?:ofrecer|presentar)\b.*\bpruebas\b",
    r"\b(?:impidi[oó]|impidieron|no permiti[oó]|no permitieron)\b.*\b(?:alegar|defenderse|defenderme|defenderle)\b",
]

V66_DOCUMENTOS = [
    # Extravío/retención + consecuencia administrativa concreta
    r"\b(?:extrav[ií]o|extraviaron|se extravi[oó]|perdieron|retuvieron|retuvo)\b.*\b(?:expediente|documento|certificado|constancia)\b.*\b(?:impide|impidi[oó]|imposibilita|detiene|bloquea|no permite)\b.*\b(?:tr[aá]mite|titulaci[oó]n|inscripci[oó]n|servicio|continuar)\b",
    r"\b(?:expediente|documento|certificado|constancia)\b.*\b(?:extrav[ií]o|extraviad|perdido|retenid)\b.*\b(?:impide|imposibilita|detiene|bloquea)\b",
]

V66_AMENAZA_INDIRECTA = [
    # Advertencia explícita de consecuencias por ejercer una acción/derecho
    r"\b(?:me|le|nos)?\s*(?:indicaron|indic[oó]|dijeron|dijo|advirtieron|advirti[oó]|se[nñ]alaron|se[nñ]al[oó])\b.*\b(?:hacerlo|quejar|denunciar|reportar|inconformidad|insistir)\b.*\b(?:tendr[ií]a|habr[ií]a|traer[ií]a|generar[ií]a)\b.*\b(?:consecuencias|problemas|afectaciones|represalias)\b",
    r"\b(?:si|en caso de)\b.*\b(?:queja|denuncia|report|inconformidad|insist)\b.*\b(?:consecuencias|represalias|afectar[ií]a|perjudicar[ií]a)\b",
    r"\b(?:tendr[ií]a|habr[ií]a)\s+consecuencias\b.*\b(?:trayectoria|calificaci[oó]n|situaci[oó]n acad[eé]mica|permanencia)\b",
]

V66_EVALUACION = [
    # Alteración explícita de calificación/evaluación
    r"\b(?:cambiaron|cambi[oó]|modificaron|modific[oó]|alteraron|alter[oó])\b.*\b(?:calificaci[oó]n|nota|evaluaci[oó]n|resultado|porcentaje)\b",
    r"\b(?:calificaci[oó]n|nota|resultado)\b.*\b(?:cambiada|modificada|alterada)\b.*\b(?:sin|despu[eé]s|posteriormente)\b",
    # Negativa explícita a revisión/evaluación ya solicitada
    r"\b(?:se neg[oó]|negaron|rechazaron)\b.*\b(?:revisar|revisi[oó]n)\b.*\b(?:examen|calificaci[oó]n|evaluaci[oó]n|trabajo)\b",
]

def tipo_rescate_v66(oracion):
    t=_n(oracion)
    if len(t.split()) < 6:
        return None

    # Las barreras fuertes siguen teniendo prioridad.
    if barrera_rescate(oracion):
        return None

    grupos=[
        ("V66_DEBIDO_PROCESO",V66_DEBIDO_PROCESO),
        ("V66_DOCUMENTOS",V66_DOCUMENTOS),
        ("V66_AMENAZA_INDIRECTA",V66_AMENAZA_INDIRECTA),
        ("V66_EVALUACION",V66_EVALUACION),
    ]
    for nombre,pats in grupos:
        if any(re.search(p,t,re.I) for p in pats):
            return nombre
    return None

def clasificar_unidad_v66(oracion):
    # V6.4 sigue siendo la base.
    base=clasificar_unidad_v64(oracion)
    if base=="HECHO":
        return "HECHO"

    # Solo intentamos los cuatro rescates nuevos.
    return "HECHO" if tipo_rescate_v66(oracion) else base

# 5. Saneamiento V7.3.1
CATEGORIAS_PERMITIDAS={"HECHO","EVIDENCIA","GESTION","CONSECUENCIA","SOLICITUD"}

PATRONES_CORTE_V731=[
    # conectores iniciales observados
    r"^\s*(?:adem[aá]s|posteriormente|despu[eé]s(?:\s+de\s+(?:eso|esto))?|entonces|finalmente|"
    r"tambi[eé]n|incluso|ah[ií]\s+fue\s+cuando|en\s+ese\s+sentido|por\s+eso|"
    r"por\s+lo\s+anterior)\s*,?\s*",

    # aperturas metadiscursivas
    r"^\s*(?:la\s+verdad\s+)?(?:mi\s+problema\s+es\s+el\s+siguiente|lo\s+que\s+pas[oó]\s+fue\s+esto)\s*:\s*",
    r"^\s*la\s+cosa\s+es\s+que\s*,?\s*",
    r"^\s*(?:la\s+verdad\s+)?(?:necesito|quiero|quisiera)\s+ayuda\s+(?:con|para|por)\s+(?:una\s+)?situaci[oó]n(?:\s+que\s+estoy\s+viviendo)?\s*[,:.-]?\s*",
    r"^\s*(?:acudo|escribo|me\s+comunico)\s+porque\s+no\s+he\s+podido\s+resolver\s+(?:esta|la)\s+situaci[oó]n\s*[,:.-]?\s*",
    r"^\s*(?:acudo|escribo|me\s+comunico)\s+(?:para|con\s+el\s+fin\s+de)\s+(?:pedir|solicitar|explicar|contar)\b[^.:;!?]*[.:;]\s*",
    r"^\s*(?:ya\s+)?no\s+s[eé]\s+qu[eé]\s+hacer\s*(?:y\s+por\s+eso\s+)?(?:decid[ií]\s+(?:contar|escribir|acudir)[^.:;!?]*)?\s*[,:.-]?\s*",
    r"^\s*mi\s+intenci[oó]n\s+es\s+que\s+(?:los\s+)?hechos\s+sean\s+revisados\s*[,:.-]?\s*",
]

# Fragmentos que, aislados al inicio tras quitar una apertura, no expresan un hecho autónomo.
# Se mantienen deliberadamente específicos.
HUERFANOS_INICIALES_V731=[
    r"^\s*que\s+se\s+me\s+sali[oó]\s+de\s+las\s+manos\s*[.,;:-]?\s*",
    r"^\s*que\s+ya\s+no\s+s[eé]\s+c[oó]mo\s+resolver\s*[.,;:-]?\s*",
]

META_SOLO_V731=[
    r"^\s*(?:acudo|escribo|me\s+comunico)\s+porque\b",
    r"^\s*(?:quiero|quisiera|deseo)\s+(?:explicar|contar|comentar)\b",
    r"^\s*(?:necesito|quiero|quisiera)\s+ayuda\b",
    r"^\s*(?:mi\s+problema\s+es\s+el\s+siguiente|lo\s+que\s+pas[oó]\s+fue\s+esto)\s*$",
    r"^\s*(?:ya\s+)?no\s+s[eé]\s+qu[eé]\s+hacer\b",
    r"^\s*la\s+cosa\s+es\s+que\b",
]

def sanear_unidad_v731(texto):
    t=limpiar_espacios(texto)
    anterior=None

    # Varias capas pueden estar encadenadas:
    # "Por eso, la cosa es que, Acudo porque..."
    for _ in range(10):
        if t==anterior:
            break
        anterior=t
        for p in PATRONES_CORTE_V731:
            t=re.sub(p,"",t,flags=re.I).strip(" ,;:-")
        for p in HUERFANOS_INICIALES_V731:
            t=re.sub(p,"",t,flags=re.I).strip(" ,;:-")

    if not t:
        return ""

    n=_n(t)
    if any(re.search(p,n,re.I) for p in META_SOLO_V731):
        return ""
    if len(re.findall(r"\w+",t)) < 4:
        return ""
    return limpiar_espacios(t)

def extraer_plan_v731(descripcion):
    plan=[]
    for i,u_original in enumerate(segmentar_descripcion(descripcion),1):
        u=sanear_unidad_v731(u_original)
        if not u:
            continue
        cat=clasificar_unidad_v66(u)
        if cat not in CATEGORIAS_PERMITIDAS:
            continue
        plan.append({"id_fuente":f"S{i}","categoria":cat,"texto":u,"texto_original":u_original})
    return plan

def plan_suficiente_v731(plan):
    return any(x["categoria"]=="HECHO" for x in plan)

def seleccionar_plan_v731(plan):
    limites={"HECHO":8,"EVIDENCIA":2,"GESTION":2,"CONSECUENCIA":2,"SOLICITUD":2}
    usados={k:0 for k in limites}; salida=[]
    for x in plan:
        c=x["categoria"]
        if usados[c] < limites[c]:
            salida.append(x); usados[c]+=1
    return salida

# 7. BASE SEGURA V7.3.1 — AUTOCONTENIDA

def base_segura_v731(plan):
    partes=[]
    for item in plan:
        texto=limpiar_espacios(item.get("texto",""))
        if not texto:
            continue
        partes.append(asegurar_punto(texto))
    return limpiar_espacios(" ".join(partes))

# Smoke test de funciones críticas
_dependencias = [
    "segmentar_descripcion","clasificar_unidad_v6","clasificar_unidad_v64",
    "clasificar_unidad_v66","sanear_unidad_v731","extraer_plan_v731",
    "plan_suficiente_v731","base_segura_v731","limpiar_espacios",
    "asegurar_punto","_n"
]
faltantes=[x for x in _dependencias if x not in globals()]
assert not faltantes, f"Dependencias faltantes: {faltantes}"

# 8. V7.6 — LIMPIEZA SUPERFICIAL DE SALIDA
#
# Regla central:
# V7.6 recibe la base YA construida por V7.3.1.
# NO toca el plan y NO llama a V6.6 para decidir qué HECHO conservar.

META_ORACION_COMPLETA_V76 = [
    r"^(?:[^.!?]*,\s*)?acudo\s+porque\s+no\s+he\s+podido\s+resolver\b.*$",
    r"^(?:[^.!?]*,\s*)?escribo\s+porque\b.*(?:situaci[oó]n|problema).*$",
    r"^(?:[^.!?]*,\s*)?me\s+comunico\s+porque\b.*(?:situaci[oó]n|problema).*$",
    r"^(?:[^.!?]*,\s*)?(?:necesito|requiero)\s+(?:ayuda|apoyo|orientaci[oó]n)\b.*$",
    r"^(?:[^.!?]*,\s*)?no\s+s[eé]\s+(?:qu[eé]\s+hacer|c[oó]mo\s+resolver)\b.*$",
    r"^(?:[^.!?]*,\s*)?mi\s+intenci[oó]n\s+es\s+que\s+los\s+hechos\s+sean\s+revisados\b.*$",
]

def dividir_oraciones_salida_v76(texto):
    t=limpiar_espacios(texto)
    if not t:
        return []
    return [limpiar_espacios(x) for x in re.split(r"(?<=[.!?])\s+",t)
            if limpiar_espacios(x)]

def es_meta_salida_v76(oracion):
    n=_n(oracion).strip(" .,:;-")
    return any(re.fullmatch(p,n,re.I) for p in META_ORACION_COMPLETA_V76)

def limpiar_base_v76(base):
    conservadas=[]
    for o in dividir_oraciones_salida_v76(base):
        if es_meta_salida_v76(o):
            continue
        conservadas.append(o)
    return limpiar_espacios(" ".join(conservadas))

def construir_v76(descripcion):
    plan=extraer_plan_v731(descripcion)

    if not plan_suficiente_v731(plan):
        return {
            "estado":"BLOQUEADO_SIN_HECHOS",
            "plan":plan,
            "base_v731":"",
            "base_v76":""
        }

    base731=base_segura_v731(plan)
    base76=limpiar_base_v76(base731)

    return {
        "estado":"GENERADO",
        "plan":plan,
        "base_v731":base731,
        "base_v76":base76
    }


# 9. V7.6.1 — ÚNICO CAMBIO: cierre de la construcción residual
#
# Se mantiene la estrategia superficial:
# - no toca plan
# - no clasifica
# - no elimina HECHOs
# - opera únicamente sobre oraciones de la base final

META_ORACION_COMPLETA_V761 = META_ORACION_COMPLETA_V76 + [
    # Residuo observado tras V7.6:
    # "Quiero reportar la siguiente situación: Acudo porque ..."
    r"^(?:quiero\s+reportar\s+la\s+siguiente\s+situaci[oó]n\s*:\s*)?"
    r"(?:[^.!?]*,\s*)?acudo\s+porque\b.*$",
]

def es_meta_salida_v761(oracion):
    n=_n(oracion).strip(" .,:;-")
    return any(re.fullmatch(p,n,re.I) for p in META_ORACION_COMPLETA_V761)

def limpiar_base_v761(base):
    conservadas=[]
    for o in dividir_oraciones_salida_v76(base):
        if es_meta_salida_v761(o):
            continue
        conservadas.append(o)
    return limpiar_espacios(" ".join(conservadas))

def construir_v761(descripcion):
    # El plan es EXACTAMENTE V7.3.1.
    plan=extraer_plan_v731(descripcion)

    if not plan_suficiente_v731(plan):
        return {
            "estado":"BLOQUEADO_SIN_HECHOS",
            "plan":plan,
            "base_v731":"",
            "base_v76":"",
            "base_v761":""
        }

    base731=base_segura_v731(plan)
    base76=limpiar_base_v76(base731)
    base761=limpiar_base_v761(base731)

    return {
        "estado":"GENERADO",
        "plan":plan,
        "base_v731":base731,
        "base_v76":base76,
        "base_v761":base761
    }


# 11. MOTOR B — MMR + SKIP-GRAM (AUTOCONTENIDO)
import math
import re
import numpy as np

try:
    import tensorflow as tf
except ImportError:
    import tensorflow as tf

tf.keras.utils.set_random_seed(42)
np.random.seed(42)

# Pedimos operaciones deterministas cuando el runtime de TensorFlow las soporta.
try:
    tf.config.experimental.enable_op_determinism()
except Exception:
    pass

STOPWORDS_ES = {
    "a","al","algo","algunas","algunos","ante","antes","como","con","contra","cual",
    "cuando","de","del","desde","donde","durante","e","el","ella","ellas","ellos","en",
    "entre","era","erais","eran","eras","eres","es","esa","esas","ese","eso","esos",
    "esta","estaba","estaban","estado","estamos","estan","estar","estas","este","esto",
    "estos","fue","fueron","ha","han","hasta","hay","la","las","le","les","lo","los",
    "más","me","mi","mis","muy","no","nos","o","para","pero","por","porque","que",
    "se","si","sin","sobre","su","sus","te","tiene","tienen","todo","un","una","uno",
    "unos","unas","y","ya","yo"
}

CONECTORES_PROPIOS = re.compile(
    r"^(sin embargo|además|por ejemplo|aunque|pero|así|asimismo|de hecho|en cambio|"
    r"por tanto|por ello|en consecuencia|no obstante|finalmente|primero|segundo|"
    r"por otro lado|en efecto)", re.I
)

PATRONES_NEXO = [
    (re.compile(r"\b(sin embargo|pero|aunque|desafío|amenaza|riesgo|problema|sesgo|limitación|obstáculo|dificultad|critica)\b", re.I), "Sin embargo,"),
    (re.compile(r"\b(contraste|diferencia|distinto|opuesto|contradicción)\b", re.I), "En contraste,"),
    (re.compile(r"\b(impacto|consecuencia|resultado|efecto|deriva|ocasiona|provoca)\b", re.I), "Como consecuencia,"),
    (re.compile(r"\b(por ello|por lo tanto|por ende|razón|causa|motivo|origen|permite|genera)\b", re.I), "Por lo tanto,"),
    (re.compile(r"\b(también|asimismo|igualmente|además|sumado|adicionalmente)\b", re.I), "Asimismo,"),
    (re.compile(r"\b(después|posteriormente|luego|más tarde|a continuación|subsecuente)\b", re.I), "Posteriormente,"),
    (re.compile(r"\b(regulación|gobernanza|norma|ley|legislación|política|marco legal|decreto)\b", re.I), "En materia regulatoria,"),
    (re.compile(r"\b(ejemplo|ilustra|muestra|evidencia|caso|instancia|demostrar|prueba)\b", re.I), "Por ejemplo,"),
    (re.compile(r"\b(es decir|o sea|en otras palabras|esto es|significa|implica|define|concepto|definición)\b", re.I), "Es decir,"),
]

def separar_oraciones_mmr(texto):
    texto = re.sub(r"\s+", " ", str(texto or "")).strip()
    if not texto:
        return []
    # \u2063 = marcador invisible de frontera que inserta D5 entre unidades
    # informativas (que pueden empezar en minúscula, p. ej. "cuando vi...").
    partes = re.split(r"(?<=[.!?])\s+(?=[A-ZÁÉÍÓÚÑ¿¡0-9])|\s*\u2063\s*", texto)
    return [p.strip() for p in partes if p.strip()]

def tokenizar_mmr(texto):
    toks = re.findall(r"[a-záéíóúüñ0-9]+", str(texto).lower())
    return [t for t in toks if len(t) > 1 and t not in STOPWORDS_ES]

def distancia_coseno(v1, v2):
    v1 = np.asarray(v1, dtype=np.float32)
    v2 = np.asarray(v2, dtype=np.float32)
    n1, n2 = np.linalg.norm(v1), np.linalg.norm(v2)
    if n1 == 0 or n2 == 0:
        return 1.0
    sim = float(np.dot(v1, v2) / (n1 * n2))
    return 1.0 - max(-1.0, min(1.0, sim))

def similitud_coseno(v1, v2):
    return 1.0 - distancia_coseno(v1, v2)

def detectar_nexo(oracion, es_primera=False):
    if es_primera or CONECTORES_PROPIOS.match(oracion.strip()):
        return ""
    for patron, nexo in PATRONES_NEXO:
        if patron.search(oracion):
            return nexo
    return ""

def mmr(emb_oraciones, centroide, indices_validos, n=6, lam=0.65):
    candidatos = list(range(len(emb_oraciones)))
    seleccionados = []
    while len(seleccionados) < n and candidatos:
        scores = {}
        for idx in candidatos:
            rel = similitud_coseno(emb_oraciones[idx], centroide)
            redundancia = max(
                (similitud_coseno(emb_oraciones[idx], emb_oraciones[s]) for s in seleccionados),
                default=0.0
            )
            scores[idx] = lam * rel - (1-lam) * redundancia
        mejor = max(scores, key=scores.get)
        seleccionados.append(mejor)
        candidatos.remove(mejor)
    return sorted(indices_validos[i] for i in seleccionados)

def entrenar_embeddings(tokens_planos, vocab_size, word2idx, epochs=500, embedding_dim=20):
    """
    Skip-gram determinista.

    La versión anterior utilizaba tf.random.normal(). Aunque existiera una semilla
    global, el estado interno del generador podía avanzar entre ejecuciones.
    Aquí usamos tf.random.stateless_normal(), por lo que la inicialización depende
    únicamente de semillas explícitas y no del historial del runtime.
    """
    if vocab_size == 0:
        return np.empty((0, embedding_dim), dtype=np.float32)

    window_size = 2
    X_idx, y_idx = [], []

    for i, tok in enumerate(tokens_planos):
        central = word2idx[tok]
        for j in range(max(0, i-window_size), min(len(tokens_planos), i+window_size+1)):
            if i != j:
                X_idx.append(central)
                y_idx.append(word2idx[tokens_planos[j]])

    if not X_idx:
        # También determinista para textos extremadamente pequeños.
        rng = np.random.default_rng(42)
        return rng.normal(size=(vocab_size, embedding_dim)).astype(np.float32)

    X = tf.one_hot(X_idx, depth=vocab_size, dtype=tf.float32)
    y = tf.one_hot(y_idx, depth=vocab_size, dtype=tf.float32)

    # Inicialización SIN estado global aleatorio.
    W1 = tf.Variable(
        tf.random.stateless_normal(
            [vocab_size, embedding_dim],
            seed=[42, 101],
            dtype=tf.float32
        )
    )
    W2 = tf.Variable(
        tf.random.stateless_normal(
            [embedding_dim, vocab_size],
            seed=[42, 202],
            dtype=tf.float32
        )
    )

    optimizer = tf.optimizers.Adam(0.01)

    for _ in range(int(epochs)):
        with tf.GradientTape() as tape:
            hidden = tf.matmul(X, W1)
            logits = tf.matmul(hidden, W2)
            loss = tf.reduce_mean(
                tf.nn.softmax_cross_entropy_with_logits(
                    labels=y,
                    logits=logits
                )
            )

        grads = tape.gradient(loss, [W1, W2])
        optimizer.apply_gradients(zip(grads, [W1, W2]))

    return W1.numpy()
def generar_resumen_mmr_tf(texto, nivel=2, epochs=500, usar_nexos=False, lam=0.65):
    todas = separar_oraciones_mmr(texto)
    oraciones = [o for o in todas if len(o.strip()) > 20] or todas

    if len(oraciones) <= 2:
        return texto.strip(), {"n_oraciones":len(oraciones), "seleccionadas":list(range(len(oraciones)))}

    corpus_tokens = [tokenizar_mmr(o) for o in oraciones]
    tokens_planos = [t for toks in corpus_tokens for t in toks]
    if not tokens_planos:
        return texto.strip(), {"n_oraciones":len(oraciones), "seleccionadas":list(range(len(oraciones)))}

    vocab = sorted(set(tokens_planos))
    word2idx = {p:i for i,p in enumerate(vocab)}
    E = entrenar_embeddings(tokens_planos, len(vocab), word2idx, epochs=epochs)

    emb_oraciones, indices_validos = [], []
    for i, toks in enumerate(corpus_tokens):
        vecs = [E[word2idx[t]] for t in toks if t in word2idx]
        if vecs:
            emb_oraciones.append(np.mean(vecs, axis=0))
            indices_validos.append(i)

    if not emb_oraciones:
        return texto.strip(), {"n_oraciones":len(oraciones), "seleccionadas":[]}

    centroide = np.mean(emb_oraciones, axis=0)
    porcentaje = {3:0.20, 2:0.40}.get(nivel, 0.60)
    n = max(1, min(math.ceil(len(oraciones)*porcentaje), len(emb_oraciones)))
    top = mmr(emb_oraciones, centroide, indices_validos, n=n, lam=lam)

    partes = []
    for i, idx in enumerate(top):
        o = oraciones[idx].strip()
        nexo = detectar_nexo(o, es_primera=(i==0)) if usar_nexos else ""
        if nexo and o:
            o = nexo + " " + o[0].lower() + o[1:]
        partes.append(o)

    return " ".join(partes).strip(), {
        "n_oraciones":len(oraciones),
        "n_seleccionadas":len(top),
        "seleccionadas":top,
        "vocab_size":len(vocab),
        "epochs":int(epochs)
    }


# ============================================================
# D5 — SEGMENTACIÓN EN UNIDADES INFORMATIVAS
# ============================================================
#
# Objetivo:
# Las quejas reales suelen tener pocas oraciones gramaticales,
# pero contienen varios hechos unidos con conectores.
#
# D5 divide SOLO cuando:
#   1) la unidad es suficientemente larga, y
#   2) aparece un conector discursivo razonablemente seguro.
#
# No reescribe el contenido: únicamente crea candidatos más
# pequeños para que MMR pueda seleccionar información.
#
# REGLA: D5 puede CORTAR el texto, pero NO puede MODIFICARLO.
# Se valida automáticamente (ver validar_segmentacion_d5).
#
# BUG CORREGIDO: el patrón de conectores tenía un grupo de captura
# dentro del lookahead; re.split() devuelve los grupos capturados como
# elementos extra, lo que duplicaba "aunque", "cuando", etc.
# Ahora el grupo es no capturante (?:...).
# ============================================================

def separar_oraciones_d5(texto):
    """
    Separador local y autocontenido para D5.
    Conserva ?, ! y . como fronteras principales.
    No depende de Librerias_PLN_pagerank.
    """
    texto = limpiar_espacios(texto)
    if not texto:
        return []

    partes = re.split(r'(?<=[.!?])\s+', texto)
    return [limpiar_espacios(p) for p in partes if limpiar_espacios(p)]

CONECTORES_SEGMENTACION_D5 = re.compile(
    r"\s+(?=(?:"
    r"pero|sin embargo|no obstante|"
    r"entonces|después|posteriormente|luego|"
    r"por lo cual|por eso|por ello|por tanto|"
    r"cuando|aunque|"
    r"además|asimismo"
    r")\b)",
    flags=re.I
)

def segmentar_unidades_informativas_d5(texto, min_palabras=10, max_palabras=38):
    """
    Segmenta una queja en unidades informativas conservando literalmente
    el texto original.

    Estrategia:
    1. Usa separar_oraciones() como primera frontera.
    2. Solo intenta subdividir oraciones largas.
    3. Divide en conectores discursivos.
    4. Evita fragmentos demasiado pequeños uniéndolos con el anterior.
    """
    texto = limpiar_espacios(texto)
    if not texto:
        return []

    oraciones = [limpiar_espacios(x) for x in separar_oraciones_d5(texto) if limpiar_espacios(x)]
    unidades = []

    for oracion in oraciones:
        n_pal = len(re.findall(r"\b\w+\b", oracion, flags=re.UNICODE))

        # Las oraciones normales permanecen intactas.
        if n_pal <= max_palabras:
            unidades.append(oracion)
            continue

        partes = [
            limpiar_espacios(p)
            for p in CONECTORES_SEGMENTACION_D5.split(oracion)
            if limpiar_espacios(p)
        ]

        # El split con lookahead puede devolver únicamente la oración original.
        if len(partes) <= 1:
            unidades.append(oracion)
            continue

        # Reconstrucción conservadora de fragmentos pequeños.
        buffer = ""
        for parte in partes:
            candidato = limpiar_espacios((buffer + " " + parte).strip()) if buffer else parte
            palabras = len(re.findall(r"\b\w+\b", candidato, flags=re.UNICODE))

            if palabras < min_palabras:
                buffer = candidato
                continue

            unidades.append(candidato)
            buffer = ""

        if buffer:
            if unidades:
                # Si el remanente es corto, se anexa al último fragmento.
                unidades[-1] = limpiar_espacios(unidades[-1] + " " + buffer)
            else:
                unidades.append(buffer)

    # Segunda pasada: eliminar duplicados vacíos y preservar orden.
    salida = []
    for u in unidades:
        u = limpiar_espacios(u)
        if u and (not salida or u != salida[-1]):
            salida.append(u)

    return salida


def _palabras_d5(texto):
    return re.findall(r"\b\w+\b", texto or "", flags=re.UNICODE)


def validar_segmentacion_d5(texto_original, unidades, verbose=True):
    """
    Comprueba que la segmentación solo cortó el texto:
    la secuencia de palabras debe ser IDÉNTICA a la original
    (misma cantidad, mismo orden, sin duplicar ni perder ninguna).
    """
    orig = _palabras_d5(texto_original)
    seg = _palabras_d5(" ".join(unidades))

    ok = (orig == seg)

    if verbose:
        print(f"Palabras originales:           {len(orig)}")
        print(f"Palabras después de segmentar: {len(seg)}")

    if ok:
        if verbose:
            pass
        return True

    # Diagnóstico: primera posición donde divergen.
    pos = next((i for i, (a, b) in enumerate(zip(orig, seg)) if a != b),
               min(len(orig), len(seg)))
    ctx_o = " ".join(orig[max(0, pos - 3):pos + 4])
    ctx_s = " ".join(seg[max(0, pos - 3):pos + 4])
    msg = (
        "❌ ERROR: la segmentación modificó el contenido.\n"
        f"   Original: {len(orig)} palabras | Segmentado: {len(seg)} palabras\n"
        f"   Primera diferencia en la palabra {pos + 1}:\n"
        f"     original:  ...{ctx_o}...\n"
        f"     segmentado: ...{ctx_s}..."
    )
    if verbose:
        print(msg)
    raise AssertionError(msg)


def preparar_texto_segmentado_d5(texto):
    """
    Convierte las unidades en pseudo-oraciones separadas por punto.
    Así reutilizamos el motor D4 sin modificar su MMR, pesos ni embeddings.
    """
    unidades = segmentar_unidades_informativas_d5(texto)

    # Validación ANTES de MMR: si la segmentación alteró algo, se detiene.
    validar_segmentacion_d5(texto, unidades, verbose=False)

    # D5 NO altera la puntuación original: la frontera entre unidades se marca
    # con un carácter invisible (\u2063) que separar_oraciones_mmr() reconoce.
    # Así "calificaciones," no se convierte en "calificaciones,." y las unidades
    # que empiezan en minúscula ("cuando vi...") sí cuentan como oraciones.
    return "\u2063".join(u.strip() for u in unidades), unidades

# 16. MOTOR D — MMR PONDERADO POR IMPORTANCIA DEL HECHO
#
# A diferencia de C, aquí ninguna oración queda "protegida".
# Todas siguen compitiendo en MMR, pero reciben una bonificación si contienen
# señales importantes para una queja de la Defensoría.

PATRONES_IMPORTANCIA_D = {
    # ── Dimensiones GENERALES de una queja ───────────────────────────────
    "CONTEXTO_ANTECEDENTE": [
        r"\bsoy\b", r"\bestudiante\b", r"\balumn[oa]\b",
        r"\buniversidad\b", r"\bescuela\b", r"\bcarrera\b",
        r"\bsemestre\b", r"\bdesde hace\b", r"\bhace \d+ años\b",
        r"\bservicio social\b", r"\bprácticas?\b", r"\bpreparatoria\b"
    ],

    "HECHO_PRINCIPAL": [
        r"\bproblema\b", r"\bsituaci[oó]n\b", r"\bocurri[oó]\b",
        r"\bsucedi[oó]\b", r"\bpas[oó]\b", r"\bresulta que\b",
        r"\bfalt", r"\bno aparece\b", r"\bap[oó]crif",
        r"\bno liberad", r"\bno valid", r"\bno reconoc",
        r"\bno registr", r"\birregular", r"\berror\b"
    ],

    "ACCION_OMISION": [
        r"\bno me\b", r"\bno nos\b", r"\bno dieron\b",
        r"\bno entreg", r"\bno avis", r"\bno inform",
        r"\bno notific", r"\bno record", r"\bno liber",
        r"\bme piden\b", r"\bme pide\b", r"\bme exigen\b",
        r"\bme oblig", r"\bme nieg", r"\bnegaron\b",
        r"\brechaz", r"\bcancel", r"\bdieron de baja\b"
    ],

    "GESTION_TRAMITE": [
        r"\bacud", r"\breport", r"\bdenunci", r"\binform[eé]",
        r"\brevis[eé]\b", r"\bconsult", r"\bsolicit",
        r"\btr[aá]mite\b", r"\bgesti[oó]n\b", r"\blevant.*acta",
        r"\bsistemas? escolares?", r"\bservicio social\b",
        r"\bcomit[eé]\b", r"\bcoordinaci[oó]n\b", r"\bdirecci[oó]n\b",
        r"\bSEP\b", r"\bdefensor[ií]a\b"
    ],

    "EVIDENCIA_DOCUMENTO": [
        r"\bacta\b", r"\bconstancia\b", r"\bcertificado\b",
        r"\bhoja\b", r"\bdocument", r"\bcomprobante\b",
        r"\bcorreo\b", r"\bmensaje\b", r"\bcaptur", r"\bfoto",
        r"\bvideo\b", r"\bgrabaci[oó]n\b", r"\bexpediente\b",
        r"\bfolio\b", r"\bp[aá]gina\b", r"\bregistro\b",
        r"\bevidencia\b", r"\bprueba\b"
    ],

    "RESPUESTA_INSTITUCIONAL": [
        r"\bme dicen\b", r"\bme dijeron\b", r"\brespond",
        r"\bnegaron\b", r"\bneg[oó]\b", r"\brechaz",
        r"\bno procede\b", r"\bsin respuesta\b", r"\barchiv",
        r"\bno (?:me )?(?:dieron|brindaron|atendieron|permitieron)",
        r"\bme piden\b", r"\bme pide\b", r"\bme exigen\b"
    ],

    "CONSECUENCIA_AFECTACION": [
        r"\bno puedo\b", r"\bno podr[eé]\b", r"\bimpide\b",
        r"\bperder\b", r"\bp[eé]rdida\b", r"\bafect",
        r"\bperjudic", r"\bdaño\b", r"\bretras", r"\bdeuda\b",
        r"\bno puedo titular\b", r"\bno puedo ser\b",
        r"\bdarme de baja\b", r"\bbaja de la carrera\b",
        r"\brepetir\b", r"\bvolver a hacer\b",
        r"\bmiedo\b", r"\btemor\b", r"\bansiedad\b",
        r"\bmoret", r"\blesi[oó]n", r"\bherid", r"\bdolor"
    ],

    "RIESGO_AMENAZA": [
        r"\bamenaz", r"\briesgo\b", r"\bmiedo de perder\b",
        r"\bdarme de baja\b", r"\bbaja de la carrera\b",
        r"\bexpuls", r"\bsancion", r"\brepresalia\b"
    ],

    "SOLICITUD_ORIENTACION": [
        r"\bqu[eé] puedo hacer\b", r"\bqu[eé] procede\b",
        r"\bsolicito\b", r"\bpido que\b", r"\bnecesito\b",
        r"\bquiero que\b", r"\bme orient", r"\borientaci[oó]n\b",
        r"\bayuda\b", r"\bque me ayuden\b", r"\bdejen de\b",
        r"\bno s[eé] en donde\b", r"\bno s[eé] qu[eé] hacer\b"
    ],

    # ── Dimensiones ESPECÍFICAS complementarias ──────────────────────────
    "AGRESION_ACOSO": [
        r"\bacos", r"\bhostig", r"\binsult", r"\bamenaz",
        r"\bintimid", r"\bgolpe", r"\bagredi", r"\bempuj",
        r"\bjal[oó]", r"\bsujet", r"\btoc[oó]", r"\btocamiento",
        r"\bnalgad", r"\bpatad", r"\bpuñet", r"\bburl",
        r"\bhumill", r"\bviolencia"
    ],

    "ACADEMICO_ADMINISTRATIVO": [
        r"\btitul", r"\bsemestre\b", r"\bcarrera\b",
        r"\bservicio social\b", r"\bcomunitari",
        r"\bcertificado\b", r"\bconstancia\b", r"\bSEP\b",
        r"\bbaja\b", r"\binscrip", r"\bcomit[eé]\b",
        r"\bescolares\b", r"\bliberad"
    ]
}

# Los pesos reflejan valor informativo, NO una decisión obligatoria.
# Las dimensiones generales dominan; las específicas complementan.
PESO_ETIQUETA_D = {
    "CONTEXTO_ANTECEDENTE": 0.45,
    "HECHO_PRINCIPAL": 0.95,
    "ACCION_OMISION": 0.90,
    "GESTION_TRAMITE": 0.85,
    "EVIDENCIA_DOCUMENTO": 0.90,
    "RESPUESTA_INSTITUCIONAL": 1.00,
    "CONSECUENCIA_AFECTACION": 1.00,
    "RIESGO_AMENAZA": 1.00,
    "SOLICITUD_ORIENTACION": 0.80,
    "AGRESION_ACOSO": 0.90,
    "ACADEMICO_ADMINISTRATIVO": 0.55,
}

def etiquetas_importancia_d(oracion):
    n = normalizar(oracion)
    return [
        etiqueta for etiqueta, patrones in PATRONES_IMPORTANCIA_D.items()
        if any(re.search(p, n, re.I) for p in patrones)
    ]

def score_importancia_d(oracion):
    """
    Importancia acumulativa GENERAL para quejas DDP.

    Cada dimensión informativa distinta aporta al score.
    Las categorías específicas complementan a las generales.

    No existe saturación en 1.0 y ninguna oración queda protegida.
    """
    etiquetas = etiquetas_importancia_d(oracion)

    if not etiquetas:
        return 0.0, []

    importancia = sum(PESO_ETIQUETA_D[e] for e in etiquetas)

    # Bonus pequeño por reunir dimensiones diferentes en la misma oración.
    if len(etiquetas) >= 2:
        importancia += 0.05 * (len(etiquetas) - 1)

    return float(importancia), etiquetas
def mmr_ponderado_d(
    emb_oraciones,
    centroide,
    indices_validos,
    oraciones_raw,
    n=6,
    peso_relevancia=0.50,
    peso_redundancia=0.20,
    peso_importancia=0.30
):
    candidatos = list(range(len(emb_oraciones)))
    seleccionados = []
    auditoria_iteraciones = []

    while len(seleccionados) < n and candidatos:
        scores = {}

        for idx_local in candidatos:
            idx_original = indices_validos[idx_local]
            oracion = oraciones_raw[idx_original]

            relevancia = similitud_coseno(
                emb_oraciones[idx_local], centroide
            )

            redundancia = max(
                (
                    similitud_coseno(
                        emb_oraciones[idx_local],
                        emb_oraciones[s]
                    )
                    for s in seleccionados
                ),
                default=0.0
            )

            importancia, etiquetas = score_importancia_d(oracion)

            score = (
                peso_relevancia * relevancia
                - peso_redundancia * redundancia
                + peso_importancia * importancia
            )

            scores[idx_local] = {
                "score": float(score),
                "relevancia": float(relevancia),
                "redundancia": float(redundancia),
                "importancia": float(importancia),
                "etiquetas": etiquetas
            }

        mejor = max(scores, key=lambda i: scores[i]["score"])
        auditoria_iteraciones.append({
            "paso": len(seleccionados) + 1,
            "indice_original": indices_validos[mejor],
            **scores[mejor]
        })
        seleccionados.append(mejor)
        candidatos.remove(mejor)

    originales = sorted(indices_validos[i] for i in seleccionados)
    return originales, auditoria_iteraciones

def generar_resumen_mmr_ponderado_d(
    texto,
    nivel=1,
    epochs=500,
    peso_relevancia=0.50,
    peso_redundancia=0.20,
    peso_importancia=0.30
):
    todas = separar_oraciones_mmr(texto)
    oraciones = [o for o in todas if len(o.strip()) > 20] or todas

    if len(oraciones) <= 2:
        return texto.strip(), {
            "n_oraciones": len(oraciones),
            "seleccionadas": list(range(len(oraciones))),
            "auditoria_oraciones": []
        }

    corpus_tokens = [tokenizar_mmr(o) for o in oraciones]
    tokens_planos = [t for toks in corpus_tokens for t in toks]

    if not tokens_planos:
        return texto.strip(), {
            "n_oraciones": len(oraciones),
            "seleccionadas": list(range(len(oraciones))),
            "auditoria_oraciones": []
        }

    vocab = sorted(set(tokens_planos))
    word2idx = {p:i for i,p in enumerate(vocab)}
    E = entrenar_embeddings(
        tokens_planos, len(vocab), word2idx, epochs=epochs
    )

    emb_oraciones = []
    indices_validos = []

    for i, toks in enumerate(corpus_tokens):
        vecs = [E[word2idx[t]] for t in toks if t in word2idx]
        if vecs:
            emb_oraciones.append(np.mean(vecs, axis=0))
            indices_validos.append(i)

    if not emb_oraciones:
        return texto.strip(), {
            "n_oraciones": len(oraciones),
            "seleccionadas": [],
            "auditoria_oraciones": []
        }

    centroide = np.mean(emb_oraciones, axis=0)

    porcentaje = {1:0.60, 2:0.40, 3:0.20}.get(nivel, 0.60)
    n = max(
        1,
        min(math.ceil(len(oraciones) * porcentaje), len(emb_oraciones))
    )

    top, auditoria_iteraciones = mmr_ponderado_d(
        emb_oraciones,
        centroide,
        indices_validos,
        oraciones,
        n=n,
        peso_relevancia=peso_relevancia,
        peso_redundancia=peso_redundancia,
        peso_importancia=peso_importancia
    )

    resumen = " ".join(oraciones[i].strip() for i in top).strip()

    auditoria_oraciones = []
    seleccionadas_set = set(top)
    for i, o in enumerate(oraciones):
        imp, etiquetas = score_importancia_d(o)
        auditoria_oraciones.append({
            "oracion": i + 1,
            "seleccionada": i in seleccionadas_set,
            "importancia": round(imp, 3),
            "etiquetas": ", ".join(etiquetas) if etiquetas else "—",
            "texto": o
        })

    return resumen, {
        "n_oraciones": len(oraciones),
        "n_seleccionadas": len(top),
        "seleccionadas": top,
        "porcentaje_objetivo": porcentaje,
        "pesos": {
            "relevancia": peso_relevancia,
            "redundancia": peso_redundancia,
            "importancia": peso_importancia
        },
        "auditoria_seleccion": auditoria_iteraciones,
        "auditoria_oraciones": auditoria_oraciones
    }

print("  Score = 0.50*relevancia - 0.20*redundancia + 0.30*importancia")
print("  Ninguna oración se protege obligatoriamente.")

# ============================================================
# D5 — D4 + SEGMENTACIÓN INFORMATIVA
# ============================================================

def generar_resumen_d5(
    texto,
    nivel=1,
    epochs=500,
    peso_relevancia=0.50,
    peso_redundancia=0.20,
    peso_importancia=0.30
):
    # Valida internamente que la segmentación no modifique el contenido.
    texto_segmentado, unidades = preparar_texto_segmentado_d5(texto)

    resumen, meta = generar_resumen_mmr_ponderado_d(
        texto_segmentado,
        nivel=nivel,
        epochs=epochs,
        peso_relevancia=peso_relevancia,
        peso_redundancia=peso_redundancia,
        peso_importancia=peso_importancia
    )

    # Si la última unidad seleccionada termina en coma/;/: se cierra con punto.
    resumen = re.sub(r"[,;:]\s*$", ".", resumen.strip())

    n_orig = len(_palabras_d5(texto))
    n_seg = len(_palabras_d5(" ".join(unidades)))

    meta["texto_segmentado_d5"] = texto_segmentado.replace("\u2063", " ")
    meta["palabras_originales_d5"] = n_orig
    meta["palabras_segmentadas_d5"] = n_seg
    meta["segmentacion_integra_d5"] = (n_orig == n_seg)
    meta["unidades_d5"] = unidades
    meta["n_unidades_d5"] = len(unidades)

    return resumen, meta

# ============================================================
# D6 — D5 + COBERTURA CONTEXTUAL
# ============================================================

PATRONES_DEPENDENCIA_D6 = re.compile(
    r"^(cuando|aunque|pero|sin embargo|no obstante|entonces|después|posteriormente|"
    r"luego|por lo cual|por eso|por ello|por tanto|además|asimismo|a lo cual|"
    r"por esta razón|por estas cosas)\b",
    flags=re.I
)

# Señales generales adicionales. No sustituyen la taxonomía D5:
# solo ayudan a reconocer hechos que D5 podía dejar sin etiqueta.
PATRONES_HECHO_D6 = [
    r"\bno (?:dio|dieron|proporcion[oó]|proporcionaron|estableci[oó]|establecieron)\b",
    r"\bsin (?:criterios?|explicaci[oó]n|respuesta|aviso|notificaci[oó]n)\b",
    r"\bcriterios? de evaluaci[oó]n\b",
    r"\bcalificaci[oó]n(?:es)?\b",
    r"\breprob", r"\bresultado\b",
    r"\bno me hac[ií]a sentido\b",
]

CIERRE_D6 = {
    "RESPUESTA_INSTITUCIONAL",
    "CONSECUENCIA_AFECTACION",
    "RIESGO_AMENAZA",
    "SOLICITUD_ORIENTACION",
    "GESTION_TRAMITE",
    "EVIDENCIA_DOCUMENTO",
}

NUCLEO_D6 = {
    "HECHO_PRINCIPAL",
    "ACCION_OMISION",
    "AGRESION_ACOSO",
    "ACADEMICO_ADMINISTRATIVO",
}

def etiquetas_d6(unidad):
    etiquetas = set(etiquetas_importancia_d(unidad))
    n = normalizar(unidad)
    if any(re.search(p, n, re.I) for p in PATRONES_HECHO_D6):
        etiquetas.add("HECHO_D6")
    return etiquetas

def depende_de_anterior_d6(unidad):
    return bool(PATRONES_DEPENDENCIA_D6.search(limpiar_espacios(unidad)))

def _score_unidad_d6(unidad):
    imp, _ = score_importancia_d(unidad)
    etiquetas = etiquetas_d6(unidad)
    extra = 0.0
    if "HECHO_D6" in etiquetas:
        extra += 1.10
    if etiquetas & NUCLEO_D6:
        extra += 0.45
    if etiquetas & CIERRE_D6:
        extra += 0.40
    return imp + extra

def ajustar_cobertura_contextual_d6(unidades, seleccionadas, n_objetivo):
    """
    Ajuste posterior conservador:
    1) si una unidad seleccionada empieza con un conector dependiente,
       intenta incluir su antecedente inmediato;
    2) intenta cubrir núcleo del problema + cierre/afectación;
    3) mantiene el mismo presupuesto de unidades de D5;
    4) nunca reescribe texto.
    """
    if not unidades:
        return [], []

    sel = set(int(i) for i in seleccionadas if 0 <= int(i) < len(unidades))
    eventos = []

    # --- A. Resolver dependencias explícitas ---
    requeridos = set()
    for i in sorted(sel):
        if i > 0 and depende_de_anterior_d6(unidades[i]) and (i - 1) not in sel:
            requeridos.add(i - 1)
            eventos.append({
                "tipo":"antecedente",
                "unidad_dependiente": i + 1,
                "unidad_agregada": i,
                "motivo":"La unidad seleccionada inicia con un conector que depende del contexto anterior."
            })

    sel |= requeridos

    # --- B. Cobertura del núcleo del problema ---
    def tiene_nucleo(indices):
        for i in indices:
            e = etiquetas_d6(unidades[i])
            if ("HECHO_D6" in e) or bool(e & NUCLEO_D6):
                return True
        return False

    if not tiene_nucleo(sel):
        candidatos = [
            i for i,u in enumerate(unidades)
            if ("HECHO_D6" in etiquetas_d6(u)) or bool(etiquetas_d6(u) & NUCLEO_D6)
        ]
        if candidatos:
            mejor = max(candidatos, key=lambda i: _score_unidad_d6(unidades[i]))
            sel.add(mejor)
            requeridos.add(mejor)
            eventos.append({
                "tipo":"cobertura_nucleo",
                "unidad_agregada": mejor + 1,
                "motivo":"Se agregó una unidad que explica el problema o hecho principal."
            })

    # --- C. Cobertura de respuesta/consecuencia/gestión/solicitud ---
    def tiene_cierre(indices):
        return any(bool(etiquetas_d6(unidades[i]) & CIERRE_D6) for i in indices)

    if not tiene_cierre(sel):
        candidatos = [
            i for i,u in enumerate(unidades)
            if etiquetas_d6(u) & CIERRE_D6
        ]
        if candidatos:
            mejor = max(candidatos, key=lambda i: _score_unidad_d6(unidades[i]))
            sel.add(mejor)
            requeridos.add(mejor)
            eventos.append({
                "tipo":"cobertura_cierre",
                "unidad_agregada": mejor + 1,
                "motivo":"Se agregó una unidad con respuesta, consecuencia, gestión, evidencia o solicitud."
            })

    # --- D. Respetar el presupuesto de D5 ---
    # Nunca eliminamos las unidades requeridas por coherencia/cobertura.
    # Entre las demás, conservamos las de mayor valor informativo.
    if len(sel) > n_objetivo:
        protegidas = set(requeridos)

        # Si una unidad dependiente sigue seleccionada, su antecedente queda protegido.
        for i in list(sel):
            if i > 0 and depende_de_anterior_d6(unidades[i]) and (i - 1) in sel:
                protegidas.add(i - 1)

        removibles = [i for i in sel if i not in protegidas]
        removibles = sorted(removibles, key=lambda i: _score_unidad_d6(unidades[i]))

        while len(sel) > n_objetivo and removibles:
            quitar = removibles.pop(0)
            sel.remove(quitar)
            eventos.append({
                "tipo":"ajuste_presupuesto",
                "unidad_eliminada": quitar + 1,
                "motivo":"Se retiró una unidad menos informativa para conservar el tamaño objetivo."
            })

    # Si las restricciones esenciales requieren más unidades que el presupuesto,
    # preferimos coherencia antes que cortar el antecedente.
    return sorted(sel), eventos


def generar_resumen_d6(
    texto,
    nivel=1,
    epochs=500,
    peso_relevancia=0.50,
    peso_redundancia=0.20,
    peso_importancia=0.30
):
    # D5 hace segmentación íntegra + MMR ponderado.
    resumen_d5, meta = generar_resumen_d5(
        texto,
        nivel=nivel,
        epochs=epochs,
        peso_relevancia=peso_relevancia,
        peso_redundancia=peso_redundancia,
        peso_importancia=peso_importancia
    )

    unidades = meta.get("unidades_d5", [])
    seleccionadas_d5 = meta.get("seleccionadas", [])
    n_objetivo = max(1, int(meta.get("n_seleccionadas", len(seleccionadas_d5) or 1)))

    seleccionadas_d6, eventos = ajustar_cobertura_contextual_d6(
        unidades,
        seleccionadas_d5,
        n_objetivo
    )

    resumen = " ".join(unidades[i].strip() for i in seleccionadas_d6).strip()
    resumen = re.sub(r"[,;:]\s*$", ".", resumen)

    auditoria_d6 = []
    for i, u in enumerate(unidades):
        auditoria_d6.append({
            "unidad": i + 1,
            "seleccionada_D5": i in set(seleccionadas_d5),
            "seleccionada_D6": i in set(seleccionadas_d6),
            "dependiente": depende_de_anterior_d6(u),
            "score_contextual": round(_score_unidad_d6(u), 3),
            "etiquetas_D6": ", ".join(sorted(etiquetas_d6(u))) or "—",
            "texto": u
        })

    meta = dict(meta)
    meta["seleccionadas_d5"] = list(seleccionadas_d5)
    meta["seleccionadas_d6"] = seleccionadas_d6
    meta["eventos_contextuales_d6"] = eventos
    meta["auditoria_d6"] = auditoria_d6
    meta["n_seleccionadas_d6"] = len(seleccionadas_d6)

    return resumen, meta

print("  - conserva texto extractivo")
print("  - resuelve conectores dependientes")
print("  - busca núcleo + cierre")
print("  - intenta mantener el mismo presupuesto de unidades")

# ============================================================
# D7 — D6 + CIERRE INFORMATIVO FLEXIBLE
# ============================================================

# Señales que indican que una unidad puede cerrar o completar el relato.
PATRONES_CIERRE_D7 = [
    r"\b(?:solo|solamente)\s+me\b",
    r"\bno me (?:dio|dieron|respondio|respondió|contestaron|contestó|explicaron|explicó)\b",
    r"\bme (?:dijo|dijeron|respondio|respondió|contestó|contestaron|subio|subió|negaron|rechazaron)\b",
    r"\b(?:al final|finalmente|termin[oó]|acab[oó])\b",
    r"\b(?:por eso|por ello|por lo cual|entonces|como consecuencia)\b",
    r"\b(?:perd[ií]|perder|baja|expuls|reprob|afect|miedo|amenaz)\b",
    r"\b(?:que puedo hacer|qué puedo hacer|solicito|pido|necesito|quiero saber)\b",
]

def es_cierre_d7(unidad):
    n = normalizar(unidad)
    etiquetas = etiquetas_d6(unidad)

    if etiquetas & CIERRE_D6:
        return True

    return any(re.search(p, n, re.I) for p in PATRONES_CIERRE_D7)


def score_cierre_d7(unidad):
    """
    Puntaje independiente para decidir si una unidad omitida
    contiene un desenlace/cierre que conviene conservar.
    """
    score = 0.0
    n = normalizar(unidad)
    etiquetas = etiquetas_d6(unidad)

    if etiquetas & CIERRE_D6:
        score += 1.50

    for p in PATRONES_CIERRE_D7:
        if re.search(p, n, re.I):
            score += 0.65

    # Las últimas unidades tienen una pequeña prioridad porque
    # frecuentemente contienen respuesta, consecuencia o solicitud.
    return score


def completar_cierre_d7(unidades, seleccionadas_d6):
    """
    D7 NO reemplaza una unidad de D6 para cumplir rígidamente el presupuesto.
    Si falta un cierre relevante, puede agregar UNA unidad extra.
    """
    sel = set(seleccionadas_d6)
    eventos = []

    if not unidades:
        return [], eventos

    # ¿La selección actual ya contiene un cierre?
    cierres_sel = [i for i in sel if es_cierre_d7(unidades[i])]

    # Cierres omitidos.
    candidatos = [
        i for i, u in enumerate(unidades)
        if i not in sel and es_cierre_d7(u)
    ]

    if candidatos:
        # Favorecer cierre fuerte y, en empate, el más tardío.
        mejor = max(
            candidatos,
            key=lambda i: (score_cierre_d7(unidades[i]), i)
        )

        score_mejor = score_cierre_d7(unidades[mejor])

        # Agregar si:
        # 1) no hay ningún cierre en la selección, o
        # 2) el candidato es un cierre fuerte (>= 0.65) y aparece después
        #    de la última unidad seleccionada, completando el desenlace.
        agregar = False
        motivo = ""

        if not cierres_sel:
            agregar = True
            motivo = "La selección no contenía respuesta, consecuencia, solicitud o desenlace."
        elif mejor > max(sel) and score_mejor >= 0.65:
            agregar = True
            motivo = "Se conservó una unidad posterior que completa el desenlace de la queja."

        if agregar:
            sel.add(mejor)
            eventos.append({
                "tipo": "cierre_flexible",
                "unidad_agregada": mejor + 1,
                "score_cierre": round(score_mejor, 3),
                "motivo": motivo
            })

    return sorted(sel), eventos


def generar_resumen_d7(
    texto,
    nivel=1,
    epochs=500,
    peso_relevancia=0.50,
    peso_redundancia=0.20,
    peso_importancia=0.30
):
    # Primero ejecutamos D6 completo.
    resumen_d6, meta = generar_resumen_d6(
        texto,
        nivel=nivel,
        epochs=epochs,
        peso_relevancia=peso_relevancia,
        peso_redundancia=peso_redundancia,
        peso_importancia=peso_importancia
    )

    unidades = meta.get("unidades_d5", [])
    seleccionadas_d6 = meta.get("seleccionadas_d6", meta.get("seleccionadas", []))

    seleccionadas_d7, eventos_d7 = completar_cierre_d7(
        unidades,
        seleccionadas_d6
    )

    resumen = " ".join(
        unidades[i].strip()
        for i in seleccionadas_d7
    ).strip()

    resumen = re.sub(r"[,;:]\s*$", ".", resumen)

    auditoria = []
    for i, u in enumerate(unidades):
        auditoria.append({
            "unidad": i + 1,
            "seleccionada_D6": i in set(seleccionadas_d6),
            "seleccionada_D7": i in set(seleccionadas_d7),
            "dependiente": depende_de_anterior_d6(u),
            "es_cierre_D7": es_cierre_d7(u),
            "score_cierre": round(score_cierre_d7(u), 3),
            "etiquetas": ", ".join(sorted(etiquetas_d6(u))) or "—",
            "texto": u
        })

    meta = dict(meta)
    meta["seleccionadas_d7"] = seleccionadas_d7
    meta["eventos_d7"] = eventos_d7
    meta["auditoria_d7"] = auditoria
    meta["n_seleccionadas_d7"] = len(seleccionadas_d7)
    meta["excede_presupuesto_d6"] = len(seleccionadas_d7) > len(seleccionadas_d6)

    return resumen, meta

print("  D6 = coherencia contextual")
print("  D7 = coherencia contextual + cierre informativo flexible")
print("  El porcentaje es un objetivo, no una obligación rígida.")

# ============================================================
# D8 — D7 + PROTECCIÓN DE CONSECUENCIAS RELEVANTES
# ============================================================

# Consecuencias concretas. Se buscan relaciones/frases, no palabras emocionales aisladas.
PATRONES_CONSECUENCIA_D8 = {
    "LABORAL": [
        r"\bperd[ií]\s+(?:mi|el|un)\s+(?:trabajo|empleo|puesto)\b",
        r"\bme\s+(?:corrieron|despidieron|despidió|despidio)\b",
        r"\b(?:no pude|no puedo|me impide|me impidió|me impidio)\s+(?:trabajar|conseguir trabajo|conseguir empleo)\b",
        r"\bperd[ií]\s+(?:una|la)\s+(?:oportunidad|oferta)\s+(?:laboral|de trabajo|de empleo)\b",
    ],
    "ECONOMICA": [
        r"\bperd[ií]\s+(?:dinero|ingresos|apoyo económico|apoyo economico)\b",
        r"\bme qued[eé]\s+sin\s+(?:dinero|ingresos|apoyo)\b",
        r"\b(?:deuda|deudas|gastos|pagar|pago)\b.{0,45}\b(?:por|debido|a causa)\b",
    ],
    "ACADEMICA": [
        r"\b(?:perd[ií]|perder|reprob[eé]|reprobar)\s+(?:el|un|mi)?\s*(?:semestre|materia|curso|año|anio)\b",
        r"\b(?:me dieron|me darán|me daran|darme|me amenazan con)\s+(?:de )?baja\b",
        r"\b(?:no puedo|no podr[eé]|me impide|me impidió|me impidio)\s+(?:titularme|graduarme|inscribirme|continuar)\b",
        r"\b(?:expulsaron|expulsión|expulsion|suspendieron|suspensión|suspension)\b",
        r"\bperd[ií]\s+(?:mi|la|una)\s+beca\b",
    ],
    "FISICA_SALUD": [
        r"\b(?:me lesion[eé]|me lastim[eé]|me fractur[eé]|me golpearon|me empujaron)\b",
        r"\b(?:moret[oó]n|moretones|fractura|lesi[oó]n|herida|sangrado)\b",
        r"\b(?:termin[eé]|acab[eé])\s+(?:en|con)\s+(?:urgencias|hospital|el hospital|una lesión|una lesion)\b",
    ],
    "ADMINISTRATIVA": [
        r"\b(?:no puedo|no podr[eé]|me impide|me impidió|me impidio)\s+(?:hacer|iniciar|continuar|terminar)\s+(?:el|mi|un)\s+tr[aá]mite\b",
        r"\b(?:bloquearon|retuvieron|cancelaron|rechazaron|negaron)\s+(?:mi|el|la)\s+(?:trámite|tramite|documento|certificado|constancia|solicitud)\b",
        r"\b(?:no me liberaron|no me han liberado|no liberaron)\s+(?:el|mi)\s+(?:servicio social|trámite|tramite)\b",
    ],
}

# Expresiones que por sí solas NO deben convertir una unidad en consecuencia.
PATRONES_SOLO_EMOCION_D8 = [
    r"\b(?:tengo miedo|me da miedo|estoy triste|estoy enojad[oa]|me siento mal|estoy preocupad[oa])\b",
    r"\b(?:jaja+|jeje+|xd)\b",
    r"\b(?:me muero de hambre|me quiero morir)\b",
]

def detectar_consecuencias_d8(unidad):
    n = normalizar(unidad)
    tipos = []
    coincidencias = []

    for tipo, patrones in PATRONES_CONSECUENCIA_D8.items():
        for patron in patrones:
            m = re.search(patron, n, re.I)
            if m:
                tipos.append(tipo)
                coincidencias.append(m.group(0))
                break

    return sorted(set(tipos)), coincidencias


def score_consecuencia_d8(unidad):
    """
    Puntaje de protección:
    - una consecuencia concreta ya es relevante;
    - varias categorías o varias señales aumentan la prioridad;
    - emoción aislada no suma.
    """
    tipos, coincidencias = detectar_consecuencias_d8(unidad)

    if not tipos:
        return 0.0

    score = 1.50
    score += 0.45 * max(0, len(tipos) - 1)
    score += 0.20 * max(0, len(coincidencias) - 1)

    # Refuerzo para pérdidas/impedimentos explícitos.
    n = normalizar(unidad)
    if re.search(r"\b(?:perd[ií]|perder|no puedo|no podr[eé]|me impide|me impidió|me impidio|baja|despid)\b", n, re.I):
        score += 0.50

    return round(score, 3)


def completar_consecuencias_d8(unidades, seleccionadas_d7, umbral=1.50, max_recuperaciones=2):
    """
    Parte de D7 y recupera únicamente consecuencias concretas omitidas.
    No reemplaza unidades ya seleccionadas y no fuerza una compresión rígida.
    """
    sel = set(seleccionadas_d7)
    eventos = []

    candidatos = []
    for i, u in enumerate(unidades):
        if i in sel:
            continue
        score = score_consecuencia_d8(u)
        tipos, coincidencias = detectar_consecuencias_d8(u)
        if score >= umbral and tipos:
            candidatos.append((i, score, tipos, coincidencias))

    # Prioridad: mayor score; en empate, conservar orden narrativo.
    candidatos.sort(key=lambda x: (-x[1], x[0]))

    for i, score, tipos, coincidencias in candidatos[:max_recuperaciones]:
        sel.add(i)
        eventos.append({
            "tipo": "proteccion_consecuencia",
            "unidad_agregada": i + 1,
            "score_consecuencia": score,
            "tipos_consecuencia": ", ".join(tipos),
            "evidencia_detectada": " | ".join(coincidencias),
            "motivo": "Se recuperó una consecuencia concreta que D7 había omitido."
        })

    return sorted(sel), eventos


def generar_resumen_d8(
    texto,
    nivel=1,
    epochs=500,
    peso_relevancia=0.50,
    peso_redundancia=0.20,
    peso_importancia=0.30,
    umbral_consecuencia=1.50,
    max_recuperaciones=2
):
    # D8 hereda TODO lo decidido por D7.
    resumen_d7, meta = generar_resumen_d7(
        texto,
        nivel=nivel,
        epochs=epochs,
        peso_relevancia=peso_relevancia,
        peso_redundancia=peso_redundancia,
        peso_importancia=peso_importancia
    )

    unidades = meta.get("unidades_d5", [])
    seleccionadas_d7 = meta.get("seleccionadas_d7", meta.get("seleccionadas_d6", []))

    seleccionadas_d8, eventos_d8 = completar_consecuencias_d8(
        unidades,
        seleccionadas_d7,
        umbral=umbral_consecuencia,
        max_recuperaciones=max_recuperaciones
    )

    resumen = " ".join(unidades[i].strip() for i in seleccionadas_d8).strip()
    resumen = re.sub(r"[,;:]\s*$", ".", resumen)

    set_d7 = set(seleccionadas_d7)
    set_d8 = set(seleccionadas_d8)

    auditoria = []
    for i, u in enumerate(unidades):
        tipos, coincidencias = detectar_consecuencias_d8(u)
        auditoria.append({
            "unidad": i + 1,
            "seleccionada_D7": i in set_d7,
            "seleccionada_D8": i in set_d8,
            "recuperada_D8": (i not in set_d7 and i in set_d8),
            "score_consecuencia": score_consecuencia_d8(u),
            "tipos_consecuencia": ", ".join(tipos) or "—",
            "evidencia_detectada": " | ".join(coincidencias) or "—",
            "texto": u
        })

    meta = dict(meta)
    meta["seleccionadas_d8"] = seleccionadas_d8
    meta["eventos_d8"] = eventos_d8
    meta["auditoria_d8"] = auditoria
    meta["n_seleccionadas_d8"] = len(seleccionadas_d8)
    meta["recuperaciones_d8"] = len(eventos_d8)
    meta["excede_presupuesto_d7"] = len(seleccionadas_d8) > len(seleccionadas_d7)

    return resumen, meta

print("  D7 = cobertura contextual + cierre flexible")
print("  D8 = D7 + protección de consecuencias concretas")
print("  Una emoción aislada no basta para recuperar una unidad.")

# D9.1 — MICROSEGMENTACIÓN CONTROLADA

def contar_palabras_eval(texto):
    return len(re.findall(r"\b\w+\b", str(texto or ""), flags=re.UNICODE))

CONECTORES_D9 = [
    "sin embargo", "no obstante", "por otra parte", "por otro lado",
    "por lo cual", "por ello", "por eso", "por tanto", "así que",
    "además", "asimismo", "posteriormente", "después", "entonces",
    "luego", "aunque", "pero"
]

def _tokens_d9(texto):
    return re.findall(r"\b\w+\b", limpiar_espacios(str(texto)).lower(), flags=re.UNICODE)

def _fusionar_cortos_d9(partes, minimo=6):
    partes = [limpiar_espacios(p) for p in partes if limpiar_espacios(p)]
    salida, buffer = [], ""
    for p in partes:
        cand = limpiar_espacios((buffer + " " + p).strip()) if buffer else p
        if contar_palabras_eval(cand) < minimo:
            buffer = cand
        else:
            salida.append(cand); buffer = ""
    if buffer:
        if salida: salida[-1] = limpiar_espacios(salida[-1] + " " + buffer)
        else: salida = [buffer]
    return salida

def microsegmentar_unidad_d9(unidad, min_palabras_unidad=24, min_fragmento=6):
    unidad = limpiar_espacios(unidad)
    if contar_palabras_eval(unidad) < min_palabras_unidad:
        return [unidad]
    conectores = "|".join(sorted((re.escape(x) for x in CONECTORES_D9), key=len, reverse=True))
    partes = re.split(rf"\s+(?=(?:{conectores})\b)", unidad, flags=re.I)
    partes = _fusionar_cortos_d9(partes, min_fragmento)
    if len(partes) <= 1:
        return [unidad]
    if _tokens_d9(" ".join(partes)) != _tokens_d9(unidad):
        raise AssertionError("La microsegmentación D9 modificó el contenido.")
    return partes

def segmentar_unidades_d9(texto, min_palabras_unidad=24, min_fragmento=6):
    unidades_d5 = segmentar_unidades_informativas_d5(texto)
    validar_segmentacion_d5(texto, unidades_d5, verbose=False)
    micros, mapa = [], []
    for i,u in enumerate(unidades_d5):
        partes = microsegmentar_unidad_d9(u, min_palabras_unidad, min_fragmento)
        for j,p in enumerate(partes):
            micros.append(p)
            mapa.append({
                "micro_unidad": len(micros), "unidad_d5_origen": i+1,
                "fragmento": j+1, "n_fragmentos_origen": len(partes),
                "palabras": contar_palabras_eval(p), "texto": p
            })
    if _tokens_d9(" ".join(micros)) != _tokens_d9(texto):
        raise AssertionError("D9 perdió, agregó o reordenó palabras.")
    return micros, mapa

# D9.2 — CONSECUENCIAS: D8 + CORRECCIÓN MORFOLÓGICA MÍNIMA
PATRONES_CONSECUENCIA_D9 = {k:list(v) for k,v in PATRONES_CONSECUENCIA_D8.items()}
PATRONES_CONSECUENCIA_D9["FISICA_SALUD"] = [
    r"\b(?:me lesion[eé]|me lastim[eé]|me fractur[eé]|me golpearon|me empujaron|me empuj[oó]|me empuja|me empujaban)\b",
    r"\b(?:moret[oó]n|moretones|fractura|lesi[oó]n|herida|sangrado)\b",
    r"\b(?:termin[eé]|acab[eé])\s+(?:en|con)\s+(?:urgencias|hospital|el hospital|una lesión|una lesion)\b",
]

def detectar_consecuencias_d9(unidad):
    n=normalizar(unidad); tipos=[]; evidencias=[]
    for tipo,patrones in PATRONES_CONSECUENCIA_D9.items():
        for patron in patrones:
            m=re.search(patron,n,re.I)
            if m:
                tipos.append(tipo); evidencias.append(m.group(0)); break
    return sorted(set(tipos)), evidencias

def score_consecuencia_d9(unidad):
    tipos,evidencias=detectar_consecuencias_d9(unidad)
    if not tipos: return 0.0
    score=1.50 + 0.45*max(0,len(tipos)-1) + 0.20*max(0,len(evidencias)-1)
    if re.search(r"\b(?:perd[ií]|perder|no puedo|no podr[eé]|me impide|me impidió|me impidio|baja|despid)\b", normalizar(unidad), re.I):
        score += 0.50
    return round(score,3)

def completar_consecuencias_d9(unidades, seleccionadas, umbral=1.50, max_recuperaciones=2):
    sel=set(seleccionadas); eventos=[]; candidatos=[]
    for i,u in enumerate(unidades):
        if i in sel: continue
        score=score_consecuencia_d9(u); tipos,evidencias=detectar_consecuencias_d9(u)
        if score>=umbral and tipos: candidatos.append((i,score,tipos,evidencias))
    candidatos.sort(key=lambda x:(-x[1],x[0]))
    for i,score,tipos,evidencias in candidatos[:max_recuperaciones]:
        sel.add(i); eventos.append({"unidad_agregada":i+1,"score_consecuencia":score,"tipos_consecuencia":", ".join(tipos),"evidencia_detectada":" | ".join(evidencias)})
    return sorted(sel), eventos

# D9.3 — GENERADOR COMPLETO

def generar_resumen_d9(texto, nivel=1, epochs=500, peso_relevancia=0.50,
                       peso_redundancia=0.20, peso_importancia=0.30,
                       umbral_consecuencia=1.50, max_recuperaciones=2,
                       min_palabras_unidad=24, min_fragmento=6):
    texto=limpiar_espacios(texto)
    unidades,mapa=segmentar_unidades_d9(texto,min_palabras_unidad,min_fragmento)
    texto_micro="\u2063".join(u.strip() for u in unidades)
    _,meta=generar_resumen_mmr_ponderado_d(texto_micro,nivel=nivel,epochs=epochs,
        peso_relevancia=peso_relevancia,peso_redundancia=peso_redundancia,peso_importancia=peso_importancia)
    sel_mmr=meta.get("seleccionadas",[])
    n_obj=max(1,int(meta.get("n_seleccionadas",len(sel_mmr) or 1)))
    sel_d6,eventos_d6=ajustar_cobertura_contextual_d6(unidades,sel_mmr,n_obj)
    sel_d7,eventos_d7=completar_cierre_d7(unidades,sel_d6)
    sel_d9,eventos_d9=completar_consecuencias_d9(unidades,sel_d7,umbral_consecuencia,max_recuperaciones)
    resumen=" ".join(unidades[i].strip() for i in sel_d9).strip()
    resumen=re.sub(r"[,;:]\s*$",".",resumen)
    auditoria=[]
    for i,u in enumerate(unidades):
        tipos,evid=detectar_consecuencias_d9(u)
        auditoria.append({
            "micro_unidad":i+1,"unidad_d5_origen":mapa[i]["unidad_d5_origen"],
            "fragmento":mapa[i]["fragmento"],"palabras":contar_palabras_eval(u),
            "seleccionada_D9":i in set(sel_d9),"recuperada_D9":i not in set(sel_d7) and i in set(sel_d9),
            "score_consecuencia":score_consecuencia_d9(u),"tipos_consecuencia":", ".join(tipos) or "—",
            "evidencia_detectada":" | ".join(evid) or "—","texto":u
        })
    meta=dict(meta); meta.update({
        "unidades_d9":unidades,"mapa_microsegmentacion_d9":mapa,
        "segmentacion_integra_d9":_tokens_d9(" ".join(unidades))==_tokens_d9(texto),
        "seleccionadas_d9":sel_d9,"eventos_d9":eventos_d9,
        "recuperaciones_d9":len(eventos_d9),"auditoria_d9":auditoria
    })
    return resumen,meta

