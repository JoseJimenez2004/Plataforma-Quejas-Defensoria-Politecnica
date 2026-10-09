#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Generador de datasets sinteticos de quejas para el modelo de IA en Java.
Este script es solo para generar datos; el procesamiento real sera en Java."""

import json
import random
import os
from datetime import datetime, timedelta

# Listas de datos ficticios
nombres_femeninos = [
    "ana", "maria", "gabriela", "fernanda", "daniela", "laura", "patricia",
    "carmen", "gloria", "esperanza", "lucia", "sofia", "valentina", "camila",
    "paula", "andrea", "jessica", "monica", "roberta", "alejandra"
]
nombres_masculinos = [
    "javier", "pedro", "andres", "mauricio", "ricardo", "sergio", "mario",
    "carlos", "fernando", "hector", "roberto", "jorge", "daniel", "brandon",
    "kevin", "raul", "luis", "francisco", "alejandro", "eduardo"
]
apellidos = [
    "garcia", "lopez", "martinez", "hernandez", "gonzalez", "perez", "sanchez",
    "ramirez", "ortiz", "castillo", "fuentes", "vidal", "morales", "soliz",
    "lara", "vazquez", "espinoza", "mendez", "diaz", "jimenez", "soto",
    "paredes", "estrada", "ruiz", "beltran", "cruz", "delgado", "nunez"
]
escuelas = [
    "ESCOM", "ESIME", "UPIICSA", "ESIA", "ESFM", "ESIT", "UPIBI", "UPIIC",
    "ESIA-Tecamachalco", "ESIME-Culhuacan", "ESCOM-IPN"
]
lugares = [
    "salon 105 del edificio central",
    "salon 304 del edificio A",
    "salon 410 del edificio 4",
    "los baños del primer piso del edificio 3",
    "los baños del segundo piso del edificio 2",
    "los baños del edificio B",
    "la cafeteria del edificio A",
    "la cafeteria del plantel bajo",
    "la biblioteca",
    "el estacionamiento de alumnos",
    "el estacionamiento del edificio 2",
    "el taller de mecanica",
    "el laboratorio de redes",
    "el gimnasio",
    "los vestidores del gimnasio",
    "las canchas deportivas",
    "la entrada principal del plantel",
    "la explanada central",
    "la libreria cercana al plantel",
    "el metro cercano al plantel"
]

tipos_violencia = [
    "amenazar de muerte",
    "intimidacion",
    "sextorsion",
    "difusion de contenido intimo",
    "controlar y prohibir",
    "aislar",
    "humillacion publica",
    "ridiculizar por orientacion sexual",
    "stalkear redes sociales",
    "acecho digital",
    "comentarios sexuales",
    "piropos soeces",
    "empujar y jalonear",
    "golpear jugando",
    "cachetear",
    "manosear",
    "tocar sin consentimiento",
    "intimidar y amenazar",
    "humillar en publico",
    "acosar por mensajes"
]

cargos_denunciado = ["alumno", "profesor", "subdirector", "director", "empleado", "exnovio", "compañero"]
relaciones = ["novio", "exnovio", "compañero", "profesor", "desconocido", "amigo", "expareja"]
areas_turno = {
    "amenazar de muerte": "Unidad de Género",
    "intimidacion": "Coordinación de Seguridad",
    "sextorsion": "Coordinación de Tutorías",
    "difusion de contenido intimo": "Coordinación de Tutorías",
    "controlar y prohibir": "Psicología Institucional",
    "aislar": "Psicología Institucional",
    "humillacion publica": "Unidad de Género",
    "ridiculizar por orientacion sexual": "Unidad de Género",
    "stalkear redes sociales": "Coordinación de Seguridad",
    "acecho digital": "Coordinación de Seguridad",
    "comentarios sexuales": "Dirección de Carrera",
    "piropos soeces": "Dirección de Carrera",
    "empujar y jalonear": "Unidad de Género",
    "golpear jugando": "Unidad de Género",
    "cachetear": "Unidad de Género",
    "manosear": "Unidad de Género",
    "tocar sin consentimiento": "Unidad de Género",
    "intimidar y amenazar": "Coordinación de Seguridad",
    "humillar en publico": "Unidad de Género",
    "acosar por mensajes": "Coordinación de Seguridad"
}

nombres_autoridad = [
    "dr hector ramirez", "ing laura vidal", "lic roberto sanchez", "profe sergio mendez",
    "dr francisco diaz", "mtra gloria paredes", "dr carlos jimenez", "mtro daniel rivera",
    "lic anabel torres", "ing marco antonio robles"
]


def generar_nombre(genero="aleatorio"):
    if genero == "aleatorio":
        genero = random.choice(["femenino", "masculino"])
    nombres = nombres_femeninos if genero == "femenino" else nombres_masculinos
    return random.choice(nombres)


def generar_apellidos():
    return random.choice(apellidos), random.choice(apellidos)


def generar_correo(nombre, apellido1, apellido2, escuela):
    dominios = {
        "ESCOM": "escom.ipn.mx",
        "ESIME": "esime.ipn.mx",
        "UPIICSA": "upiicsa.ipn.mx",
        "ESIA": "esia.ipn.mx",
        "ESFM": "esfm.ipn.mx",
        "ESIT": "esit.ipn.mx",
        "UPIBI": "upibi.ipn.mx",
        "UPIIC": "upiic.ipn.mx",
        "ESIA-Tecamachalco": "esiatec.ipn.mx",
        "ESIME-Culhuacan": "esimecul.ipn.mx",
        "ESCOM-IPN": "escom.ipn.mx"
    }
    dominio = dominios.get(escuela, "ipn.mx")
    iniciales = f"{nombre[0]}{apellido1[:3]}{apellido2[0]}"
    numero = random.randint(100000, 999999)
    return f"{iniciales}{numero}@{dominio}"


def generar_identificacion(tipo):
    if tipo == "alumno":
        return str(random.randint(2020000000, 2024999999))
    return str(random.randint(80000000, 89999999))


def generar_fecha():
    inicio = datetime(2024, 1, 1)
    fin = datetime(2025, 9, 30)
    delta = fin - inicio
    dias = random.randint(0, delta.days)
    return (inicio + timedelta(days=dias)).strftime("%Y-%m-%d")


def normalizar(texto):
    """Minusculas y quita tildes basicas."""
    texto = texto.lower()
    tildes = str.maketrans("áéíóúüñ¿¡", "aeiouun  ")
    return texto.translate(tildes)


def generar_contexto(q_full, escuela, id_texto):
    return (
        f"antes de contar lo que paso, quiero que sepan que soy {q_full}, {id_texto}, y siempre he tratado de llevarme bien con todos. "
        f"estudio en la {escuela} y nunca imagine que terminaria en una situacion asi dentro del plantel. "
        f"tengo amigos en la escuela pero despues de lo que paso me he distanciado de varios porque no se como explicarles. "
        f"mi familia no sabe todo todavia porque me da mucha verguenza contarles, pero ya no puedo callarme mas. "
        f"necesito que alguien me escuche, me crea y me ayude a salir de esto sin que las cosas empeoren. "
        f"vengo a la defensoria porque necesito que se haga justicia y porque no quiero que otras personas pasen por lo mismo que yo."
    )


def generar_impacto(d_full, autoridad):
    return (
        f"ademas de todo esto, la situacion me tiene muy afectada emocionalmente. "
        f"ya no me concentro en las clases, me cuesta dormir y he perdido el apetito. "
        f"mis amistades notan que ya no soy la misma, me preguntan que me pasa pero me da verguenza contarles. "
        f"me he ido tarde de la escuela para evitar encontrarme con {d_full}. "
        f"he dejado de participar en clase porque no quiero llamar la atencion. "
        f"fui con {autoridad}, pero no senti que me tomaran en serio, como si estuviera exagerando. "
        f"necesito que alguien intervenga y me proteja, no quiero que esto siga pasando. "
        f"mi familia esta preocupada y ya no sabe como ayudarme, se dan cuenta de que algo anda mal pero no les digo todo. "
        f"he tenido crisis de ansiedad y me cuesta concentrarme en los examenes. "
        f"he estado pensando seriamente en pedir una baja temporal porque no aguanto la presion, pero no quiero que mi esfuerzo de años se vaya a la basura por culpa de alguien mas. "
        f"pido una investigacion formal y medidas para que {d_full} no se me acerque ni me contacte por ningun medio."
    )


def generar_consecuencias(d_full):
    return (
        f"en la escuela ya no me siento segura. he faltado varias veces a clase para no toparme con {d_full}. "
        f"mis calificaciones estan bajando y mis maestros me preguntan que me pasa. "
        f"tengo miedo de que esto afecte mi trayectoria escolar y mis proyectos personales. "
        f"no es justo que por culpa de una persona asi yo tenga que dejar de estudiar o cambiar de carrera. "
        f"quiero que se tomen medidas reales y que mi denuncia no quede solo en palabras. "
        f"quiero que quede claro que no busco venganza, solo quiero que se respete mi dignidad y que el plantel sea un lugar seguro para todas las personas."
    )


def identificacion_texto(q_tipo_id, q_num_id):
    if q_tipo_id == "alumno":
        return f"alumno de la escuela con boleta {q_num_id}"
    return f"empleado del plantel con numero de empleado {q_num_id}"


def plantilla_texto(tipo, escuela, lugar, q_nombre, q_ap1, q_ap2, d_nombre, d_ap1, d_ap2, d_relacion, autoridad, id_texto):
    q_full = f"{q_nombre} {q_ap1} {q_ap2}"
    d_full = f"{d_nombre} {d_ap1} {d_ap2}"
    contexto = generar_contexto(q_full, escuela, id_texto)
    impacto = generar_impacto(d_full, autoridad)
    consecuencias = generar_consecuencias(d_full)

    if tipo == "amenazar de muerte":
        base = (
            f"pues mira, yo soy {q_full}, {id_texto}, y la neta ya no aguanto. "
            f"estudio en la {escuela} y ahi conoci a {d_full} en {lugar}, todo bien al principio, re platicadores y eso, "
            f"pero de repente el vato empezo a cambiar. primero era celos tontos, luego ya me decia que no podia salir con mis amigas. "
            f"un dia en {lugar} me dijo textual 'si me dejas te juro que te mato y luego me mato yo, no te voy a dejar en paz ni viva'. "
            f"me quede helada wey, no sabia ni que contestar, me temblaban las manos. "
            f"despues me mando mensajes por whatsapp tipo 'ya te vi salir de tu casa', 'se donde vives', 'no vas a escapar'. "
            f"una vez me espero afuera del plantel, cerca de {lugar}, hasta las diez de la noche. "
            f"fui a hablar con {autoridad}, pero me hizo poco caso. "
        )
    elif tipo == "sextorsion":
        base = (
            f"osea, esto me tiene super mal, no puedo ni ir a clase. soy {q_full}, {id_texto}. "
            f"andaba saliendo con {d_full} de la misma escuela, todo cool, nos mandabamos fotos intimas. "
            f"nunca pense que fuera a usar eso en mi contra. el problema empezo cuando termine con el. "
            f"primero me mando mensajes diciendo que si no regresaba me iba a mandar las fotos a mis compas. "
            f"una amiga me aviso que ya andaban circulando las fotos en grupos de whatsapp y discord. "
            f"me queria morir de la verguenza, no queria salir de mi cuarto. "
            f"luego me pidio dinero por transferencia para 'borrar todo', pero yo se que es mentira. "
            f"fui con {autoridad}, pero me dijeron que primero tenia que poner la denuncia formal. "
        )
    elif tipo == "controlar y prohibir":
        base = (
            f"buenas tardes, soy {q_full}, {id_texto}. vengo a exponer mi situacion porque ya no puedo mas. "
            f"tengo un novio que se llama {d_full}, de la misma escuela, llevamos año y medio, pero en los ultimos meses se volvio muy controlador. "
            f"no me deja salir con mis amigas, dice que son malas influencias. si llego tarde diez minutos me hace un interrogatorio. "
            f"me pide capturas de pantalla de mis chats y me obligo a bloquear a varias amistades. "
            f"me revisa el celular a escondidas. una vez en {lugar} me grito que si lo amaba le haria caso. "
            f"se pone a romper cosas, tiro mi laptop contra la pared y rompio mi celular. "
            f"me siento sola, me alejo de todos. fui con {autoridad}, pero no me hizo caso. "
        )
    elif tipo == "humillacion publica":
        base = (
            f"hola, me presento, soy {q_full}, {id_texto}. ya no aguanto la burla. "
            f"desde hace un semestre {d_full} y un grupo de compañeros se la pasan haciendome la vida imposible. "
            f"todo empezo en {lugar} donde comente algo personal. ahora me gritan cosas en los pasillos del poli. "
            f"una vez en el salon de clases pusieron un video ridiculo en la proyectora con mi cara editada y todo el salon se rio. "
            f"otros dias me esconden la mochila, me rayan los cuadernos, y en los grupos me mandan memes ofensivos. "
            f"fui con {autoridad}, pero solo les dijo 'ya no sean gachos'. me siento humillado todos los dias. "
        )
    elif tipo == "ridiculizar por orientacion sexual":
        base = (
            f"soy {q_full}, {id_texto}. vengo a contar lo que me ha estado pasando porque ya no aguanto. "
            f"un grupo de compañeros, encabezado por {d_full}, se la pasa burlandose de mi orientacion sexual. "
            f"todo empezo en {lugar} cuando comente que me gustaba alguien de mi mismo genero. ahora me gritan 'maricon' y 'putito' en los pasillos. "
            f"pusieron un video con mi cara editada en la proyectora del salon y todo se rio. me esconden la mochila y me rayan los cuadernos. "
            f"fui con {autoridad}, pero me dijo que solo era una broma. me siento humillado y no quiero ir a la escuela. "
        )
    elif tipo == "stalkear redes sociales":
        base = (
            f"quien me pueda ayudar, soy {q_full}, {id_texto}, ya no se que hacer. "
            f"hace cuatro meses termine con mi ex {d_full}, pero el no acepto que ya no estamos juntos. "
            f"me mandaba mensajes suplicando regresar, como cien veces al dia. cuando lo bloquee de todas partes empezo a buscarme. "
            f"creo se hizo cuentas falsas de instagram para ver mis historias. me envio un mensaje anonimo diciendo 'te sigo viendo'. "
            f"una noche sali de {lugar} y lo vi parado a media cuadra viendome. otra vez me encontro en el metro. "
            f"fui con {autoridad}, pero solo me dijo que le mandara un correo formal. "
        )
    elif tipo == "comentarios sexuales":
        base = (
            f"tengo que denunciar a un maestro de la {escuela}. soy {q_full}, {id_texto}. "
            f"el profesor {d_full} da clase en {lugar} y su comportamiento ya es insoportable. "
            f"siempre que paso cerca de su escritorio me dice cosas como 'hoy te ves muy sabrosa', 'esa falda te queda apretadita'. "
            f"una vez me toco el hombro y me dijo al oido 'tu eres mi favorita de la clase'. me dio mucho asco. "
            f"tambien le dice a otras compañeras 'mamacita, ven a ver tu calificacion'. en clase pone videos de bailes provocativos. "
            f"fui con {autoridad}, pero me pidio pruebas. yo ya no quiero seguir en ese salon, me siento expuesta. "
        )
    elif tipo == "empujar y jalonear":
        base = (
            f"soy {q_full}, {id_texto}. vengo a reportar a {d_full} porque me esta agrediendo fisicamente. "
            f"todo empezo con discusiones tontas, pero luego empezo a empujarme y jalonearme cuando no le daba la razon. "
            f"una vez en {lugar} me agarro del brazo fuerte y me dejo morada. otro dia me empujo contra la pared del pasillo. "
            f"me dice que solo esta jugando, pero me duele y me da miedo. fui con {autoridad}, pero me dijo que no podia hacer nada sin testigos. "
        )
    elif tipo == "cachetear":
        base = (
            f"buenas noches, soy {q_full}, {id_texto}. la verdad me da pena contar esto, pero ya no aguanto. "
            f"mi {d_relacion}, {d_full}, me ha estado cacheteando cada vez que discutimos. "
            f"la primera vez fue en {lugar}, me dio una cachetada y me dijo que me lo merecia. despues empezo a ser mas frecuente. "
            f"una vez delante de mis compañeros me cacheteo y me humillo. me da verguenza y miedo contarle a alguien. "
            f"fui con {autoridad}, pero me dijo que era un problema de pareja. "
        )
    elif tipo == "manosear":
        base = (
            f"soy {q_full}, {id_texto}. vengo a denunciar a {d_full} porque me ha estado manoseando. "
            f"todo empezo en {lugar} cuando se me acerco y me puso la mano en la cintura sin permiso. "
            f"despues en otras ocasiones me toco el muslo y la espalda, diciendo que solo era amistoso. "
            f"me siento muy incomoda y asqueada. le pedi que parara y se rio. fui con {autoridad}, pero no me creyeron. "
        )
    else:
        # Plantilla generica para el resto de tipos
        base = (
            f"soy {q_full}, {id_texto}. vengo a reportar a {d_full} por {tipo}. "
            f"todo empezo hace unos meses en {lugar}. al principio pense que no era grave, pero la situacion empeoro. "
            f"cada vez es mas frecuente y me siento muy incomoda, tengo miedo de encontrarme con el en los pasillos. "
            f"varias personas lo han visto pero nadie dice nada. yo ya no se como actuar ni a quien recurrir. "
            f"fui con {autoridad}, pero no resolvieron nada y me senti como si exagerara. "
        )

    return f"{contexto} {base} {impacto} {consecuencias}"


def generar_queja(indice, historico=True):
    escuela = random.choice(escuelas)
    lugar = random.choice(lugares)
    tipo = random.choice(tipos_violencia)

    genero_quejoso = random.choice(["femenino", "masculino"])
    q_nombre = generar_nombre(genero_quejoso)
    q_ap1, q_ap2 = generar_apellidos()
    q_tipo_id = random.choice(["alumno", "alumno", "empleado"])
    q_num_id = generar_identificacion(q_tipo_id)
    q_correo = generar_correo(q_nombre, q_ap1, q_ap2, escuela)

    d_relacion = random.choice(relaciones)
    # Coherencia de genero del denunciado segun la relacion
    if d_relacion in ["novio", "exnovio", "amigo"]:
        d_genero = "masculino"
    elif d_relacion in ["novia", "exnovia", "amiga"]:
        d_genero = "femenino"
    else:
        d_genero = random.choice(["femenino", "masculino"])
    d_nombre = generar_nombre(d_genero)
    d_ap1, d_ap2 = generar_apellidos()
    d_cargo = random.choice(cargos_denunciado)

    autoridad = random.choice(nombres_autoridad)
    id_texto = identificacion_texto(q_tipo_id, q_num_id)

    texto = plantilla_texto(
        tipo, escuela, lugar, q_nombre, q_ap1, q_ap2,
        d_nombre, d_ap1, d_ap2, d_relacion, autoridad, id_texto
    )

    return {
        "folio": f"FOL-HIS-{indice:04d}" if historico else f"FOL-PRU-{indice:04d}",
        "unidad_academica": escuela,
        "fecha_hechos": generar_fecha(),
        "tipo_violencia": tipo,
        "lugar_hechos": lugar,
        "quejoso": {
            "nombre": q_nombre,
            "apellido1": q_ap1,
            "apellido2": q_ap2,
            "correo": q_correo,
            "tipo_identificacion": q_tipo_id,
            "numero_identificacion": q_num_id
        },
        "denunciado": {
            "nombre": d_nombre,
            "apellido1": d_ap1,
            "apellido2": d_ap2,
            "cargo": d_cargo,
            "relacion": d_relacion
        },
        "texto_original": texto,
        "texto_preprocesado": normalizar(texto),
        "resumen_extractivo": "",
        "estatus": "FINALIZADA",
        "area_turnada": areas_turno.get(tipo, "Unidad de Género"),
        "es_historico": historico
    }


def generar_dataset(cantidad, historico=True):
    return {
        "dataset": "quejas_historicas" if historico else "quejas_prueba_30",
        "formato": "json",
        "normalizacion": "texto_preprocesado en minusculas, sin tildes",
        "total": cantidad,
        "fuente": "generado_sintetico_basado_en_violentometro_y_acosometro",
        "quejas": [generar_queja(i + 1, historico) for i in range(cantidad)]
    }


def guardar_json(datos, nombre):
    ruta = os.path.join("/home/bicho/Documents/Plataforma-Quejas-Defensoria-Politecnica/Produccion/Modelo-Java/Dataset", nombre)
    with open(ruta, "w", encoding="utf-8") as f:
        json.dump(datos, f, ensure_ascii=False, indent=2)
    print(f"Generado: {ruta} ({datos['total']} quejas)")


if __name__ == "__main__":
    random.seed(42)
    dataset_50 = generar_dataset(50, historico=True)
    dataset_30 = generar_dataset(30, historico=False)
    guardar_json(dataset_50, "quejas_sinteticas.json")
    guardar_json(dataset_30, "quejas_prueba_30.json")
