#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Agrega quejas sinteticas con redacciones variadas a Dataset/quejas_sinteticas.json.

Conserva las quejas FOL-HIS-0001..0050 y (re)genera FOL-HIS-0051 en adelante.
No usa parrafos fijos: cada queja combina frases de bancos grandes, cambia orden,
tono y nivel de faltas de ortografia. Algunos denunciados son reincidentes para que
la busqueda de antecedentes tenga casos relacionados. Personas y lugares son ficticios.
"""

import json
import os
import random
import re
import unicodedata
from datetime import date, timedelta

RUTA = os.path.join(os.path.dirname(os.path.abspath(__file__)), "Dataset", "quejas_sinteticas.json")
CONSERVAR_HASTA = 50
NUEVAS = 120
SEMILLA = 2026

# ---------------------------------------------------------------- catalogos

NOMBRES = {
    "f": ["Ximena", "Renata", "Itzel", "Abril", "Mariana", "Valeria", "Karen", "Daniela", "Fernanda", "Jimena",
          "Paola", "Arely", "Brenda", "Citlali", "Danna", "Estefania", "Frida", "Guadalupe", "Ingrid", "Janet",
          "Lizbeth", "Mayra", "Nayeli", "Odalys", "Pamela", "Rocio", "Sarahi", "Tania", "Vanessa", "Yaretzi"],
    "m": ["Emiliano", "Santiago", "Uriel", "Axel", "Diego", "Joel", "Ivan", "Rodrigo", "Saul", "Omar",
          "Brayan", "Cristian", "Damian", "Efrain", "Gael", "Hugo", "Isaac", "Jonathan", "Leonardo", "Marco",
          "Nestor", "Osvaldo", "Pablo", "Ramiro", "Sebastian", "Teodoro", "Ulises", "Victor", "Yahir", "Zahid"],
}
APELLIDOS = ["Aguilar", "Barrera", "Cabrera", "Carmona", "Cervantes", "Delgadillo", "Escobedo", "Figueroa", "Galindo",
             "Gallardo", "Huerta", "Ibarra", "Juarez", "Ledesma", "Maldonado", "Montiel", "Navarrete", "Ochoa",
             "Olvera", "Pacheco", "Quintero", "Rangel", "Salazar", "Santillan", "Tapia", "Trejo", "Urbina",
             "Valdez", "Villegas", "Zamora", "Arellano", "Bautista", "Campos", "Duran", "Esquivel", "Fajardo",
             "Gamboa", "Hinojosa", "Lara", "Meza", "Nieto", "Orozco", "Pineda", "Robles", "Sandoval", "Tovar"]

ESCUELAS = {
    "ESCOM": ["el laboratorio de sistemas digitales del edificio 2", "la sala de computo C", "el pasillo del tercer piso del edificio 1",
              "la cafeteria junto a la explanada", "los baños de la planta baja del edificio 2", "el salon 2105",
              "la biblioteca de la planta alta", "las mesas de afuera del auditorio"],
    "ESIME Zacatenco": ["el taller de maquinas herramienta del edificio 5", "el laboratorio de electronica L-204",
                        "los baños del segundo piso del edificio 4", "la cafeteria del edificio 3", "el estacionamiento de alumnos",
                        "el salon 4302", "las canchas de futbol rapido", "el pasillo de cubiculos de profesores"],
    "ESIME Culhuacan": ["el laboratorio de control", "el salon 112 del edificio B", "la explanada principal",
                        "los baños junto a servicios escolares", "la biblioteca", "el estacionamiento de profesores",
                        "el taller de soldadura", "la tiendita de la entrada"],
    "UPIICSA": ["los baños del tercer piso del edificio 2", "la cafeteria del edificio de ligeros", "el salon 1312",
                "la sala de computo del edificio de pesados", "el estacionamiento del edificio 2", "el gimnasio",
                "la biblioteca del segundo piso", "el paradero de camiones de enfrente"],
    "ESIA Zacatenco": ["el laboratorio de materiales", "el salon 105 del edificio 10", "la biblioteca de la ESIA",
                       "los vestidores de la alberca", "las mesas del jardin central", "el taller de topografia",
                       "el pasillo de la direccion", "los baños del edificio 12"],
    "ESFM": ["el salon 3 del edificio 9", "el cubiculo de asesorias", "la sala de lectura", "los baños del primer piso",
             "el auditorio pequeño", "la explanada de la ESFM", "el laboratorio de fisica moderna", "las escaleras de emergencia"],
    "ESIT": ["el laboratorio de redes del edificio 4", "el salon 410", "la cafeteria de la ESIT", "los baños del segundo piso",
             "la sala de profesores", "el estacionamiento del fondo", "la biblioteca", "el pasillo de servicios escolares"],
    "UPIBI": ["el laboratorio de bioquimica", "el salon 205", "el bioterio", "la cafeteria", "los vestidores del gimnasio",
              "la biblioteca", "el area de lockers", "el jardin de atras"],
    "ESIQIE": ["el laboratorio de operaciones unitarias", "el salon 6 del edificio 7", "la planta piloto",
               "los baños del edificio 8", "la cafeteria", "el estacionamiento de alumnos", "la biblioteca", "el auditorio"],
    "ESCA Tepepan": ["el salon 21", "la sala de computo 3", "la cafeteria", "los baños de la planta alta", "la biblioteca",
                     "la explanada", "el estacionamiento", "la oficina de control escolar"],
}
MATERIAS = ["Calculo Aplicado", "Estructuras de Datos", "Termodinamica", "Circuitos Electricos", "Probabilidad",
            "Fisica Clasica", "Bases de Datos", "Mecanica de Materiales", "Quimica Organica", "Contabilidad",
            "Redes de Computadoras", "Algebra Lineal", "Ecuaciones Diferenciales", "Ingles", "Metodologia"]
MEDIOS = ["WhatsApp", "Instagram", "Telegram", "Facebook", "TikTok", "el correo institucional", "Discord", "Snapchat"]
CLUBES = ["ajedrez", "robotica", "teatro", "fotografia", "danza", "programacion competitiva"]
DEPORTES = ["futbol", "basquet", "voleibol", "tochito"]
TURNOS = ["matutino", "vespertino"]
SEMESTRES = ["primer", "segundo", "tercer", "cuarto", "quinto", "sexto", "septimo", "octavo"]
AUTORIDADES = ["la coordinadora de la carrera", "el subdirector academico", "mi tutora", "el jefe de departamento",
               "la profesora de tutorias", "el prefecto del turno", "la licenciada de gestion escolar",
               "el encargado de la unidad de genero", "la psicologa de la escuela", "el director de la escuela"]
DIAS = ["lunes", "martes", "miercoles", "jueves", "viernes"]
MESES = ["enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"]
HORAS = ["como a las 7 de la mañana", "a eso de la 1 de la tarde", "como a las 3:30", "ya tarde, como a las 7 de la noche",
         "en la hora de la comida", "justo al salir de la ultima clase", "en el cambio de clase de las 11"]

AREAS = {
    "amenazas de muerte": "Unidad de Género", "intimidacion": "Coordinación de Seguridad",
    "sextorsion": "Unidad de Género", "difusion de contenido intimo": "Unidad de Género",
    "control y aislamiento": "Psicología Institucional", "humillacion publica": "Coordinación de Tutorías",
    "ridiculizar por orientacion sexual": "Unidad de Género", "acecho digital": "Coordinación de Seguridad",
    "comentarios sexuales": "Unidad de Género", "piropos soeces": "Unidad de Género",
    "empujones y jaloneos": "Coordinación de Seguridad", "golpes": "Coordinación de Seguridad",
    "tocamientos sin consentimiento": "Unidad de Género", "acoso por mensajes": "Coordinación de Seguridad",
    "hostigamiento laboral": "Dirección de Recursos Humanos", "discriminacion": "Coordinación de Tutorías",
    "chantaje academico": "Dirección de Carrera",
}

# ------------------------------------------------- hechos por tipo de violencia
# Placeholders: {dtit} denunciado (primera vez nombre completo con rol, despues forma corta),
# {dart} él/ella, {dlo} lo/la, {o} terminacion del quejoso, {lugar}, {lugar2}, {medio}, {testigo}, {materia}...

HECHOS = {
    "amenazas de muerte": [
        "{dtit} me dijo en {lugar} que si volvia a dejarle en visto me iba a ir muy mal, que sabe a que hora salgo y por donde camino.",
        "Me mando un audio por {medio} donde se escucha clarito que dice que me va a matar si hablo con alguien de lo que paso.",
        "En {lugar} se me acerco por la espalda y me dijo al oido que no sabia con quien me estaba metiendo, que tiene amigos afuera que me pueden levantar.",
        "Un dia encontre una nota en mi mochila que decia 'te vas a morir' con una cruz dibujada.",
        "Me enseño una navaja en {lugar2} y me dijo que la trae 'por si acaso' mientras me veia fijamente.",
        "Le dijo a {testigo} que si yo seguia en la escuela me iba a desaparecer.",
        "Me escribio desde una cuenta nueva: 'ya se donde vives, no salgas sol{o}'.",
        "Cuando le dije que ya no queria saber nada golpeo la pared junto a mi cara y grito que primero me mataba.",
    ],
    "intimidacion": [
        "{dtit} se para afuera de mi salon en {lugar} y se me queda viendo toda la clase sin decir nada.",
        "Cada que paso por {lugar} {dart} y sus amigos se ponen enfrente para no dejarme pasar y se rien.",
        "Me sigue hasta {lugar2} caminando a unos metros, y cuando volteo hace como que esta viendo su celular.",
        "En una revision de proyecto azoto mi libreta en la mesa y me grito que me iba a arrepentir de hablar de mas.",
        "Me rayaron el casillero con la palabra 'rata' y se que fue {dart} porque lo presumio en el grupo.",
        "Le pidio a otros compañeros que no me hablaran y que me hicieran el vacio.",
        "Se sienta justo detras de mi en {materia} y me patea la silla toda la clase.",
    ],
    "sextorsion": [
        "Cuando andabamos le mande unas fotos intimas y ahora {dtit} me dice que si no le paso dinero las va a subir a {medio}.",
        "Me pidio 2,000 pesos por transferencia para 'borrar todo' y ya le di una parte porque tenia mucho miedo.",
        "Me exige que le mande mas fotos o que {dobj} vea en {lugar}, si no, dice que le manda todo a mi familia.",
        "Me mando capturas donde ya tenia listo el mensaje para el grupo del salon con mis fotos adjuntas.",
        "Me puso fecha limite: el viernes antes de las 12 o publicaba todo.",
        "Hizo una cuenta falsa con mi nombre en {medio} y me amenaza con subir ahi el contenido.",
        "Me dijo que si lo denunciaba nadie me iba a creer porque yo le mande las fotos por voluntad propia.",
    ],
    "difusion de contenido intimo": [
        "Unos compañeros me avisaron que en un grupo de {medio} estaba circulando un video mio que yo solo le habia mandado a {dtit}.",
        "El video llego a gente de otros semestres y hasta a un grupo de la generacion.",
        "Cuando {dobj} confronte en {lugar} se rio y dijo que yo tenia la culpa por mandarlo.",
        "Me han llegado mensajes de desconocidos pidiendome 'mas contenido'.",
        "Alguien imprimio una captura y la pego en {lugar2}.",
        "Supe que {dart} lo estaba vendiendo en un canal de Telegram.",
        "Una maestra me pregunto si era yo la del video, frente a todo el grupo.",
    ],
    "control y aislamiento": [
        "{dtit} me revisa el celular todos los dias y si ve que hable con alguien me hace una escena en plena escuela.",
        "Me obligo a darle mis contraseñas de {medio} y del correo institucional.",
        "No me deja ir a asesorias ni a equipos de proyecto si hay alguien que no le cae bien.",
        "Me exige mandarle mi ubicacion en tiempo real desde que salgo de mi casa hasta que llego a la escuela.",
        "Me hizo salirme del club de {club} porque decia que ahi solo iba a coquetear.",
        "Si no le contesto en cinco minutos me llama veinte veces seguidas, aunque este en examen.",
        "Me espera afuera de cada clase en {lugar} para ver con quien salgo.",
    ],
    "humillacion publica": [
        "En plena clase de {materia} {dtit} leyo en voz alta mi calificacion y dijo que con gente como yo la escuela iba en picada.",
        "Me grabaron en {lugar} mientras me tropezaba y subieron el video a TikTok con apodos.",
        "En el grupo de {medio} del salon mandan stickers con mi cara editada.",
        "Frente a todos en {lugar2} me dijo que olia feo y que mejor me fuera a bañar.",
        "Me pusieron un apodo que ahora usan hasta los de otros grupos cuando paso por el pasillo.",
        "En una exposicion me interrumpio para decir que mi trabajo era una basura y todos se rieron.",
        "Me hizo pasar al pizarron solo para burlarse cuando me equivoque.",
    ],
    "ridiculizar por orientacion sexual": [
        "Desde que me vieron con mi pareja en {lugar}, un grupo encabezado por {dtit} hace comentarios cada que paso.",
        "En la clase de {materia} hizo un chiste sobre 'gente como yo' y me señalo.",
        "Escribieron en la puerta de {lugar2} mi nombre con insultos homofobicos.",
        "Me sacaron del equipo de proyecto diciendo que no querian 'contagiarse'.",
        "Me preguntan en voz alta delante de todos cosas intimas sobre mi relacion para reirse.",
        "Me gritan insultos por mi orientacion sexual desde el otro lado del pasillo.",
        "Subio a {medio} una foto mia con un texto burlandose de mi orientacion.",
    ],
    "acecho digital": [
        "Aunque lo bloquee en todos lados, {dtit} sigue creando cuentas nuevas en {medio} para ver mis historias.",
        "Me comenta en fotos de hace años para que vea que sigue revisando todo mi perfil.",
        "Sabia donde estaba un sabado porque lo vio en una historia de una amiga, y me escribio para decirmelo.",
        "Le escribe a mis amigas para preguntar con quien ando y a que hora salgo.",
        "Llego a {lugar2} justo cuando yo estaba ahi, y nunca le dije donde iba a estar.",
        "Me manda mensajes de madrugada con capturas de mis publicaciones viejas.",
        "Descubri que tenia acceso a mi cuenta porque me aparecian mensajes leidos que yo no habia abierto.",
    ],
    "comentarios sexuales": [
        "{dtit} me dice cosas sobre mi cuerpo, que que bien me queda la ropa y que deberia venir mas seguido asi.",
        "En {lugar} me pregunto si tenia pareja y si 'me portaba bien'.",
        "Me pidio que me sentara en primera fila porque asi 'la clase era mas bonita'.",
        "Hace comentarios con doble sentido cada que paso al pizarron y el salon se rie.",
        "Me mando un mensaje a las 11 de la noche diciendo que habia soñado conmigo.",
        "Delante de {testigo} dijo que con esa ropa yo 'andaba buscando'.",
        "Me pregunto de que color era mi ropa interior en medio de una asesoria.",
    ],
    "piropos soeces": [
        "Cuando paso frente a {lugar}, {dtit} me chifla y me grita groserias.",
        "Me grita 'mamacita' desde el otro lado del pasillo aunque haya profesores.",
        "Me dice cosas asquerosas sobre lo que me haria, en voz alta, sin importar que haya gente.",
        "Hace ruidos con la boca y me sigue unos pasos cuando salgo de {lugar2}.",
        "Una vez me grito algo tan vulgar que una señora de la cafeteria le llamo la atencion.",
        "Se junta con otros en la entrada y entre todos me dicen cosas cada mañana.",
    ],
    "empujones y jaloneos": [
        "En {lugar} {dtit} me agarro del brazo tan fuerte que me dejo moretones que todavia tengo.",
        "Me empujo contra los casilleros cuando le dije que no queria hablar.",
        "Me jalo de la mochila para que no me fuera y se rompio el tirante.",
        "Me arrincono en {lugar2} y no me dejaba salir hasta que le contestara.",
        "Dice que solo esta jugando, pero cada vez me avienta mas fuerte.",
        "Me jalo del cabello en la fila de la cafeteria.",
    ],
    "golpes": [
        "En {lugar} {dtit} me dio una cachetada delante de {testigo}.",
        "Me pateo en la pierna durante el partido de {deporte} y luego dijo que fue sin querer, pero me lo dijo sonriendo.",
        "Me pego con el puño en el estomago y me dijo que eso me pasaba por hablar de mas.",
        "Despues de la clase me espero en {lugar2} y me tiro al suelo.",
        "Tuve que ir al servicio medico de la escuela porque se me hincho el ojo.",
        "Me aventaron una botella llena de agua a la cabeza desde las escaleras.",
    ],
    "tocamientos sin consentimiento": [
        "{dtit} se pega por detras cuando me explica en la computadora y me toca la cintura.",
        "En {lugar} me toco las pompas y se fue como si nada.",
        "Me pone la mano en la pierna por debajo de la mesa durante la asesoria.",
        "Me abrazo a la fuerza y me intento dar un beso aunque le dije que no.",
        "En la fila de {lugar2} se me repegaba y cuando me voltee se hizo el desentendido.",
        "Me acaricio el cuello mientras revisaba mi practica y le dije que parara.",
    ],
    "acoso por mensajes": [
        "{dtit} me manda decenas de mensajes al dia por {medio} aunque no le conteste.",
        "Me mando fotos de sus partes intimas sin que yo se las pidiera.",
        "Me escribe que no puede dejar de pensar en mi y que algun dia vamos a estar juntos quiera o no.",
        "Cuando no le contesto me manda insultos y luego me pide perdon.",
        "Consiguio mi numero de la lista del grupo, nunca se lo di.",
        "Me hace videollamadas a las 2 de la mañana y si no contesto me deja audios llorando.",
    ],
    "hostigamiento laboral": [
        "{dtit} me grita delante de los demas compañeros del area y me dice inutil.",
        "Me carga trabajo que no me corresponde y me amenaza con levantarme actas si no lo termino ese mismo dia.",
        "Me cambio el horario sin avisar para que no coincida con mis compañeros.",
        "Me prohibe salir a comer y revisa a que hora llego y a que hora me voy.",
        "Les dijo a los demas que yo estaba robando material de {lugar}, lo cual es mentira.",
        "Me quito mi escritorio y me mando a trabajar a {lugar2}, donde no hay ni computadora.",
        "Me niega los permisos que a otros si les da, incluso para ir al medico.",
    ],
    "discriminacion": [
        "{dtit} dijo frente al grupo que las personas de mi pueblo no deberian estar en el Politecnico.",
        "Se burla de mi forma de hablar y me imita cuando participo.",
        "Me dijo que por mi discapacidad mejor me cambiara a una carrera 'mas facil'.",
        "Me bajo puntos en el proyecto diciendo que mi equipo era 'puro naco'.",
        "No me dejo entrar a {lugar} por mi forma de vestir, a los demas si los dejo.",
        "Hace comentarios sobre mi color de piel cada que me toca exponer.",
    ],
    "chantaje academico": [
        "{dtit} me dijo que si salia a tomar algo con {dlo} me pasaba la materia de {materia}.",
        "Me reprobo en el segundo parcial justo despues de que {dobj} rechace.",
        "Me cito en su cubiculo de {lugar} fuera de horario para 'revisar mi examen'.",
        "Me dijo que nadie me iba a creer porque lleva veinte años dando clases ahi.",
        "Me ofrecio subirme la calificacion a cambio de 'conocernos mejor'.",
        "Cuando le pedi revision de examen me dijo que eso se arreglaba 'en privado'.",
    ],
}

TIPOS_ALUMNO = [t for t in HECHOS if t != "hostigamiento laboral"]
TIPOS_EMPLEADO = ["hostigamiento laboral", "hostigamiento laboral", "comentarios sexuales", "tocamientos sin consentimiento",
                  "discriminacion", "intimidacion", "humillacion publica", "acoso por mensajes", "piropos soeces",
                  "amenazas de muerte"]

ROL_POR_TIPO = {
    "chantaje academico": ["docente"], "comentarios sexuales": ["docente", "docente", "companero", "personal"],
    "sextorsion": ["pareja", "expareja", "expareja"], "difusion de contenido intimo": ["expareja", "pareja", "companero"],
    "control y aislamiento": ["pareja", "pareja", "expareja"], "acecho digital": ["expareja", "expareja", "companero"],
    "amenazas de muerte": ["expareja", "companero", "pareja"], "golpes": ["companero", "pareja", "expareja"],
    "empujones y jaloneos": ["pareja", "companero", "expareja"], "piropos soeces": ["personal", "companero"],
    "hostigamiento laboral": ["jefe"], "tocamientos sin consentimiento": ["docente", "companero", "personal"],
}

# ------------------------------------------------------------ bancos generales

APERTURAS = [
    "Buenas tardes.", "Hola, no se bien como se llena esto pero aqui va.", "A quien corresponda:",
    "Neta no se ni por donde empezar.", "Por medio de la presente quiero levantar una queja formal.",
    "Escribo esto porque ya no aguanto mas.", "Me anime a escribir despues de mucho tiempo.",
    "Una amiga me paso el link de la defensoria y me dijo que aqui si hacen caso.",
    "Esto lo escribo desde mi celular, perdon si no se entiende.", "Llevo semanas pensando si mandar esto o no.",
    "Me cuesta mucho contar esto.", "Buen dia, quiero denunciar una situacion que me esta pasando en la escuela.",
    "La verdad tengo miedo de que esto se sepa, pero ya no puedo seguir callad{o}.", "Les escribo con mucho coraje.",
    "", "", "",
]
PRESENTACION = {
    "alumno": ["Soy {q}, {id}.", "Mi nombre es {q} y soy {id}.", "Me llamo {qn}, {id}, del grupo {grupo}.",
               "Para que me ubiquen: {q}, {id}, turno {turno}.", "Les escribe {q}, {id}, voy en {semestre} semestre.",
               "Soy {id} y mi nombre completo es {q}."],
    "empleado": ["Soy {q}, {id}.", "Mi nombre es {q}, {id}, y llevo {anios} años trabajando aqui.",
                 "Les escribe {q}, {id}, del turno {turno}.", "Soy {id}; mi nombre es {q}."],
}
CONTEXTO = {
    "companero": ["Conozco a {den} desde primer semestre, estuvimos en el mismo equipo de {materia}.",
                  "Al principio {dart} era buena onda conmigo, comiamos juntos en {lugar2}.",
                  "Nunca fuimos amigos, solo coincidimos en el grupo {grupo}.",
                  "Somos del mismo grupo y nos toca casi todas las materias juntos.",
                  "{dobj} conoci en el club de {club} y despues empezamos a coincidir en clases."],
    "pareja": ["Ando con {den} desde hace casi un año.", "Empezamos a salir el semestre pasado y al principio todo era normal.",
               "Nos conocimos en {lugar2} y empezamos a andar a las pocas semanas.",
               "Es mi pareja desde la prepa y ahora estudiamos en la misma escuela."],
    "expareja": ["Anduve con {den} casi un año.", "Terminamos hace como tres meses porque era muy celos{do}.",
                 "Fuimos novios un tiempo pero yo decidi terminar.", "Desde que cortamos no ha dejado de buscarme.",
                 "Nos conocimos en un evento de la escuela y salimos unos meses."],
    "docente": ["{den} me da la materia de {materia} este semestre.", "Es titular de {materia} en mi grupo {grupo}.",
                "Le tocaba darme asesorias de {materia} porque iba mal en los parciales.",
                "Todos en la carrera saben como es, pero nadie dice nada porque tiene muchos años aqui."],
    "jefe": ["{den} es mi jefe{fa} direct{do} desde hace dos años.", "Entre a trabajar aqui y desde el primer dia {dart} fue mi superior.",
             "Trabajo en su area desde que cambiaron de coordinador."],
    "personal": ["No conozco bien a esta persona, se que se llama {den} porque asi le dicen sus compañeros.",
                 "Trabaja en la escuela, casi siempre esta en {lugar2}.", "Es parte del personal de la escuela y {dobj} veo diario en la entrada."],
}
DETALLES_FECHA = [
    "Esto paso el {fecha}, {hora}.", "La primera vez fue el {fecha} en {lugar}.", "La ultima vez fue el {fecha}, {hora}.",
    "Todo empezo el {fecha}.", "Lo mas grave fue el {fecha}, {hora}, en {lugar}.",
]
DETALLES = [
    "Ese dia habia poca gente porque era hora de comida.", "Estaba con {testigo} cuando paso, vio todo.",
    "Fue en {lugar}, justo donde no hay camaras.", "Yo iba saliendo de mi clase de {materia} rumbo a {lugar2}.",
    "Era casi la hora de salida y ya estaba oscureciendo.", "Paso durante la semana de examenes, por eso no lo reporte antes.",
    "Me acuerdo bien porque ese dia entregabamos el proyecto final.", "En ese momento no habia ningun profesor cerca.",
    "Habia como diez personas alrededor y nadie dijo nada.",
]

# Episodios combinatorios: momento + situacion + desarrollo + reaccion. Evitan repetir frases fijas.
EP_MOMENTO = ["Un lunes temprano", "A mediados del semestre", "El dia de la feria de proyectos", "Antes de las vacaciones de semana santa",
              "En la semana de inscripciones", "Un viernes ya tarde", "Justo despues del primer parcial", "Un dia de lluvia",
              "El dia que suspendieron clases por la marcha", "En plena semana de entregas", "Una mañana que llegue antes de tiempo",
              "El dia del examen departamental", "Saliendo de una practica", "Cuando regresamos de vacaciones"]
EP_SITUACION = ["estaba repasando para el examen en {lugar2}", "iba con {testigo} a sacar copias", "esperaba a que abrieran {lugar}",
                "fui a entregar un tramite a control escolar", "estaba comiendo sol{o} en {lugar2}", "estaba terminando una practica en {lugar}",
                "iba camino al metro con unas compañeras", "estaba formad{o} para pagar en la cafeteria", "subia las escaleras hacia mi salon",
                "estaba conectad{o} a una clase en linea desde la biblioteca", "me quede despues de clase a preguntar una duda",
                "estaba guardando mis cosas en el locker", "iba a la papeleria de enfrente", "estaba en la fila del servicio medico"]
EP_DESARROLLO = ["cuando {denn} aparecio y volvio a hacer lo mismo", "y otra vez {denn} empezo con lo mismo",
                 "cuando senti que alguien me seguia y era {denn}", "y de la nada {denn} se puso enfrente de mi",
                 "cuando me llego una notificacion de {medio} y era otra vez {denn}", "y {denn} se acerco como si nada pasara",
                 "cuando escuche la voz de {denn} detras de mi", "y {denn} aprovecho que no habia nadie",
                 "y ahi estaba {denn}, esperandome", "cuando {denn} paso junto a mi y me dijo algo en voz baja"]
EP_REACCION = ["Me quede congelad{o} sin poder moverme.", "Intente irme lo mas rapido que pude.", "Senti que se me bajaba la presion.",
               "No dije nada porque no queria hacer mas grande el problema.", "Me fui directo al baño a llorar.",
               "Le marque a mi mama para que no colgara hasta que saliera de la escuela.", "Me temblaban las piernas.",
               "Fingi que estaba hablando por telefono para que me dejara.", "Me meti al primer salon que vi abierto.",
               "Le pedi a un compañero que me acompañara hasta la salida.", "Ese dia ya no entre a mis demas clases.",
               "Me puse a llorar ahi mismo y me dio mucha verguenza."]
EP_OTROS = ["{testigo} me dijo que ya lo reportara.", "Una compañera se dio cuenta y me pregunto si estaba bien.",
            "Nadie se metio aunque varias personas vieron.", "Un señor de intendencia se quedo viendo pero no hizo nada.",
            "Despues {testigo} me conto que a una amiga suya le paso algo parecido.", "Unos chavos se rieron como si fuera chistoso.",
            "Una maestra que iba pasando solo volteo y siguio su camino.", "{testigo} me presto su sudadera porque yo estaba temblando.",
            "El guardia de la entrada dijo que eso no le tocaba a el.", "Mis amigas me esperaron afuera hasta que me calme.",
            "Alguien grabo con su celular pero no se quien fue.", "Me ofrecieron agua en la cafeteria porque me vieron muy mal."]

EMO_SENTIR = ["miedo", "coraje", "verguenza", "tristeza", "ansiedad", "impotencia", "asco", "inseguridad", "culpa", "frustracion"]
EMO_MOMENTO = ["cada que llego a la escuela", "en las noches antes de dormir", "cuando veo una notificacion en el celular",
               "cuando paso por {lugar}", "cuando me toca {materia}", "los domingos en la noche", "cuando alguien camina detras de mi",
               "cuando escucho su nombre", "cuando tengo que exponer frente al grupo", "cuando salgo tarde de la escuela"]
EMO_PLANTILLAS = ["Siento mucha {e1} y {e2}, sobre todo {m}.", "Lo que mas me pega es la {e1} que me da {m}.",
                  "Ya no se si es {e1} o {e2}, pero me pasa {m}.", "Me gana la {e1} {m}, y luego viene la {e2}."]
CONECTORES = ["Luego", "Otro dia", "Ademas", "Para colmo", "Encima", "Despues", "Y no conforme con eso", "Tiempo despues", "La semana pasada"]
COLETILLAS = [", y la neta me quede en shock.", ", no supe ni que hacer.", ", todavia me tiemblan las manos cuando me acuerdo.",
              ", y yo nada mas me quede callad{o}.", ", y se fue riendo como si nada.", ", y nadie hizo nada."]
ESCALAMIENTO = [
    "Al principio pense que era broma, pero cada semana se puso peor.", "Despues de eso las cosas no pararon.",
    "Le pedi de buena forma que me dejara en paz y fue cuando empezo lo peor.", "Ya no es una vez, es casi diario.",
    "La ultima vez fue la gota que derramo el vaso.", "Con el tiempo empezo a involucrar a otras personas.",
    "Ahora ya no le importa si hay gente o no.", "Cada vez que creo que ya se calmo, vuelve a empezar.",
]
EVIDENCIAS = [
    "{testigo} puede confirmar lo que digo porque estaba ahi.", "En {lugar} hay una camara que apunta justo donde paso, ojala la revisen.",
    "No tengo pruebas porque fue en persona, pero varias personas lo vieron.", "Hice una lista con las fechas de cada vez que paso.",
]
EVIDENCIAS_DIGITALES = ["Tengo capturas de pantalla de los mensajes y las puedo mandar.", "Guarde los audios.",
                        "Guarde el link antes de que lo borraran.", "Tengo respaldados los chats completos."]
EVIDENCIAS_FISICAS = ["Tengo fotos de los moretones.", "Tengo la nota del servicio medico de la escuela."]
TIPOS_DIGITALES = {"sextorsion", "difusion de contenido intimo", "acecho digital", "acoso por mensajes", "control y aislamiento",
                   "amenazas de muerte", "humillacion publica"}
TIPOS_FISICOS = {"golpes", "empujones y jaloneos"}
REPORTES = [
    "Ya fui con {autoridad} pero me dijo que eran 'cosas de jovenes'.", "Le comente a {autoridad} y me dijo que sin pruebas no podia hacer nada.",
    "No lo habia reportado antes porque tenia miedo de las represalias.", "{autoridad_cap} me recomendo escribir aqui.",
    "Fui a la subdireccion pero nunca me dieron respuesta.", "Es la primera vez que lo cuento formalmente.",
    "Intente hablar con la coordinacion pero me trajeron de oficina en oficina.", "Lo platique con {autoridad} y solo le llamo la atencion de palabra.",
]
IMPACTO = [
    "Desde entonces me cuesta mucho dormir.", "He faltado a varias clases para no coincidir con {denn}.",
    "Baje mucho mis calificaciones este semestre.", "Tengo ataques de ansiedad cuando voy en camino a la escuela.",
    "Ya no como bien, he bajado como cinco kilos.", "Me da miedo salir sol{o} del plantel.", "Estoy pensando en darme de baja.",
    "Empece a ir con una psicologa porque ya no podia.", "Mi familia nota que algo me pasa pero no les he contado todo.",
    "Me siento culpable aunque se que no hice nada malo.", "Ya no participo en clase por miedo a que se burlen.",
    "Cambie mi ruta de regreso a casa y ahora hago casi dos horas.", "Deje de usar mis redes sociales.",
    "Ya no voy a la cafeteria ni a la biblioteca.", "Lloro casi todos los dias.", "Me duele la cabeza todo el tiempo por el estres.",
    "Reprobe un parcial porque no me pude concentrar.", "Ya no confio en nadie de la escuela.",
    "Me la paso revisando si {dart} esta cerca antes de salir del salon.", "Me siento sol{o} con todo esto.",
]
PETICIONES = [
    "Pido que se abra una investigacion.", "Quiero que me cambien de grupo para no coincidir con {denn}.",
    "Solicito medidas de proteccion para que no se me acerque.", "Quiero que bajen el contenido y que haya una sancion.",
    "Pido que se le suspenda mientras se investiga.", "Solo quiero poder terminar mi carrera tranquil{o}.",
    "Necesito que la escuela me garantice que no habra represalias.", "Pido que me den acompañamiento psicologico.",
    "Quiero que esto quede registrado por si le pasa a alguien mas.", "Pido que revisen las camaras de {lugar}.",
]
REFLEXIONES = [
    "Se que a otras personas les ha pasado algo parecido con {denn}.", "No es justo que yo tenga que cambiar mi vida por culpa de alguien mas.",
    "Me da coraje que en la escuela todos sepan y nadie haga nada.", "Al principio pense que yo estaba exagerando.",
    "Pense que si no le hacia caso se le iba a pasar.", "No quiero que esto se quede asi.",
    "Yo vengo a la escuela a estudiar, no a pasar por esto.", "Mucha gente me dice que lo ignore, pero ya no se puede.",
    "Me cuesta concentrarme incluso en mi casa.", "Tengo miedo de que se entere que escribi esto.",
    "Me arrepiento de no haber dicho nada antes.", "Varias veces quise contarlo pero me ganaba la pena.",
    "Siento que en cualquier momento {dart} puede volver a hacerlo.", "Mis amigos me acompañan a todas partes desde que paso.",
    "A veces pienso que si hubiera hecho algo diferente no estaria pasando esto.", "En la escuela hay muy poca vigilancia en esa zona.",
    "Hable con mis papas y me dijeron que lo denunciara.", "No le deseo esto a nadie.",
    "Me da miedo que tome represalias con mis calificaciones o con mis amigos.", "Me quede pensando todo el fin de semana en lo que paso.",
    "Ya ni siquiera disfruto venir a la escuela como antes.", "Siento que nadie me toma en serio.",
]
CIERRES = ["Gracias por leer.", "Espero su respuesta pronto.", "Ojala si hagan algo.", "Quedo atent{o}.",
           "Por favor no le digan que fui yo hasta que haya medidas.", "Gracias de antemano.", "Ya no se a quien mas acudir.", "", ""]

# ------------------------------------------------------------- utilidades

SUSTITUCIONES = {
    "que": ["q", "ke"], "porque": ["xq", "porq", "porke"], "por": ["x"], "tambien": ["tmb", "tambn"],
    "haber": ["aver"], "hacer": ["aser", "acer"], "iba": ["hiba"], "estaba": ["estava"], "vez": ["ves"],
    "veces": ["veses", "vezes"], "habia": ["abia", "avia"], "hasta": ["asta"], "mucho": ["muxo"], "nadie": ["nadien"],
    "haya": ["halla"], "voy": ["boy"], "bien": ["bn"], "mensajes": ["msjs"], "despues": ["dsps", "despues"],
    "para": ["pa"], "esta": ["sta"], "verdad": ["verda"], "ahi": ["ay"], "hay": ["ai"], "estoy": ["toy"],
}
MULETILLAS = ["osea", "neta", "la verdad", "wey", "o sea", "no manches", "bueno"]


def sin_acentos(texto):
    return "".join(c for c in unicodedata.normalize("NFD", texto) if unicodedata.category(c) != "Mn")


def ensuciar(texto, nivel, rng):
    """nivel 0: limpio, 1: algunos errores, 2: informal en minusculas, 3: muy informal."""
    if nivel == 0:
        return texto
    prob = {1: 0.06, 2: 0.18, 3: 0.32}[nivel]
    if nivel >= 2:
        texto = texto.lower()

    def reemplazar(m):
        palabra = m.group(0)
        clave = palabra.lower()
        if clave in SUSTITUCIONES and rng.random() < prob:
            nueva = rng.choice(SUSTITUCIONES[clave])
            return nueva.capitalize() if palabra[0].isupper() else nueva
        return palabra

    texto = re.sub(r"[A-Za-zÁÉÍÓÚáéíóúñÑ]+", reemplazar, texto)
    if nivel >= 2:
        oraciones = re.split(r"(?<=[.!?])\s+", texto)
        texto = " ".join(f"{rng.choice(MULETILLAS)} {o}" if rng.random() < 0.12 * nivel else o for o in oraciones)
    if nivel == 3:
        texto = re.sub(r",\s", lambda m: " " if rng.random() < 0.4 else m.group(0), texto)
        texto = re.sub(r"\bno\b", lambda m: "noo" if rng.random() < 0.2 else m.group(0), texto)
    return texto


def capitalizar(oracion):
    return oracion[:1].upper() + oracion[1:] if oracion else oracion


class Contexto(dict):
    """Devuelve la forma larga del denunciado la primera vez y la corta despues."""

    def __init__(self, base, larga, corta, den_larga, den_corta):
        super().__init__(base)
        self.formas = {"dtit": [larga, corta], "den": [den_larga, den_corta]}
        self.usadas = set()

    def __missing__(self, clave):
        if clave in self.formas:
            valor = self.formas[clave][1] if clave in self.usadas else self.formas[clave][0]
            self.usadas.add(clave)
            return valor
        raise KeyError(clave)


def rellenar(plantilla, ctx):
    return capitalizar(plantilla.format_map(ctx)) if plantilla else ""


def persona(rng, genero=None):
    genero = genero or rng.choice("fm")
    return genero, rng.choice(NOMBRES[genero]), rng.choice(APELLIDOS), rng.choice(APELLIDOS)


def forma_denunciado(rol, g, nombre, ap1, rng):
    f = g == "f"
    completo = f"{nombre} {ap1}"
    opciones = {
        "docente": [(f"la profesora {completo}" if f else f"el profesor {completo}", "la profe" if f else "el profe"),
                    (f"la ingeniera {completo}" if f else f"el ingeniero {completo}", "la ingeniera" if f else "el ingeniero"),
                    (f"la doctora {completo}" if f else f"el doctor {completo}", "la doctora" if f else "el doctor")],
        "companero": [(f"mi compañera {completo}" if f else f"mi compañero {completo}", nombre),
                      (f"una chava de mi grupo, {completo}," if f else f"un chavo de mi grupo, {completo},", nombre)],
        "pareja": [(f"mi novia {completo}" if f else f"mi novio {completo}", nombre)],
        "expareja": [(f"mi ex, {completo}," , nombre), (f"mi exnovia {completo}" if f else f"mi exnovio {completo}", "mi ex")],
        "jefe": [(f"mi jefa, la licenciada {completo}," if f else f"mi jefe, el licenciado {completo},", "mi jefa" if f else "mi jefe")],
        "personal": [(f"una trabajadora de mantenimiento, {completo}," if f else f"un trabajador de mantenimiento, {completo},", nombre),
                     (f"la guardia {completo}" if f else f"el guardia {completo}", "la guardia" if f else "el guardia"),
                     (f"la prefecta {completo}" if f else f"el prefecto {completo}", "la prefecta" if f else "el prefecto")],
    }
    return rng.choice(opciones[rol])


CARGO = {"docente": "profesor", "companero": "alumno", "pareja": "alumno", "expareja": "alumno",
         "jefe": "administrativo", "personal": "personal de apoyo"}
RELACION = {"docente": "profesor", "companero": "compañero", "pareja": "pareja", "expareja": "expareja",
            "jefe": "jefe directo", "personal": "desconocido"}


def crear_reincidentes(rng):
    reincidentes = []
    for rol, escuela in [("docente", "ESIME Zacatenco"), ("docente", "UPIICSA"), ("companero", "ESCOM"),
                         ("expareja", "ESIA Zacatenco"), ("personal", "ESIT"), ("jefe", "ESCA Tepepan")]:
        g, n, a1, a2 = persona(rng)
        reincidentes.append({"rol": rol, "escuela": escuela, "g": g, "nombre": n, "ap1": a1, "ap2": a2})
    return reincidentes


def fecha_aleatoria(rng):
    inicio = date(2023, 2, 1)
    dia = inicio + timedelta(days=rng.randint(0, (date(2025, 9, 30) - inicio).days))
    while dia.weekday() > 4:
        dia += timedelta(days=1)
    return dia, f"{DIAS[dia.weekday()]} {dia.day} de {MESES[dia.month - 1]}"


def tomar(rng, banco, k, usadas):
    disponibles = [s for s in banco if s not in usadas]
    elegidas = rng.sample(disponibles, min(k, len(disponibles)))
    usadas.update(elegidas)
    return elegidas


def generar_queja(indice, rng, reincidentes):
    tipo_q = "alumno" if rng.random() < 0.75 else "empleado"
    tipo = rng.choice(TIPOS_ALUMNO if tipo_q == "alumno" else TIPOS_EMPLEADO)

    reincidente = None
    if rng.random() < 0.25:
        candidatos = [r for r in reincidentes if (r["rol"] == "jefe") == (tipo_q == "empleado")]
        reincidente = rng.choice(candidatos) if candidatos else None
        if reincidente and reincidente["rol"] == "jefe":
            tipo = "hostigamiento laboral"
        elif reincidente and tipo == "hostigamiento laboral":
            tipo = rng.choice(TIPOS_ALUMNO)

    if reincidente:
        rol, escuela = reincidente["rol"], reincidente["escuela"]
        gd, dn, da1, da2 = reincidente["g"], reincidente["nombre"], reincidente["ap1"], reincidente["ap2"]
    else:
        rol = "jefe" if tipo == "hostigamiento laboral" else rng.choice(
            ROL_POR_TIPO.get(tipo, ["companero", "docente", "personal"]) if tipo_q == "alumno"
            else ["jefe", "personal", "companero"])
        escuela = rng.choice(list(ESCUELAS))
        gd, dn, da1, da2 = persona(rng)

    gq, qn, qa1, qa2 = persona(rng)
    num_id = str(rng.randint(2019000000, 2024999999)) if tipo_q == "alumno" else str(rng.randint(80000000, 89999999))
    ident = (f"alumn{'a' if gq == 'f' else 'o'} de {escuela} con boleta {num_id}" if tipo_q == "alumno"
             else f"trabajador{'a' if gq == 'f' else ''} de {escuela} con numero de empleado {num_id}")
    lugar, lugar2 = rng.sample(ESCUELAS[escuela], 2)
    dia, fecha_txt = fecha_aleatoria(rng)
    tg, tn, ta1, _ = persona(rng)
    autoridad = rng.choice(AUTORIDADES)
    larga, corta = forma_denunciado(rol, gd, dn, da1, rng)

    base = {
        "q": f"{qn} {qa1} {qa2}", "qn": qn, "id": ident, "o": "a" if gq == "f" else "o",
        "denn": dn, "dart": "ella" if gd == "f" else "él", "dlo": "ella" if gd == "f" else "él",
        "dobj": "la" if gd == "f" else "lo",
        "do": "a" if gd == "f" else "o", "fa": "a" if gd == "f" else "",
        "lugar": lugar, "lugar2": lugar2, "medio": rng.choice(MEDIOS), "testigo": f"{tn} {ta1}",
        "materia": rng.choice(MATERIAS), "club": rng.choice(CLUBES), "deporte": rng.choice(DEPORTES),
        "grupo": f"{rng.randint(1, 8)}{rng.choice(['CM', 'CV', 'IM', 'IV', 'MV'])}{rng.randint(1, 9)}",
        "turno": rng.choice(TURNOS), "semestre": rng.choice(SEMESTRES), "anios": rng.randint(2, 15),
        "fecha": fecha_txt, "hora": rng.choice(HORAS), "autoridad": autoridad, "autoridad_cap": capitalizar(autoridad),
    }
    ctx = Contexto(base, larga, corta, f"{dn} {da1} {da2}", dn)
    usadas = set()

    def bloque(banco, minimo, maximo):
        return [rellenar(s, ctx) for s in tomar(rng, banco, rng.randint(minimo, maximo), usadas)]

    contexto = bloque(CONTEXTO[rol], 1, 2)
    hechos = []
    for i, h in enumerate(tomar(rng, HECHOS[tipo], rng.randint(4, 6), usadas)):
        oracion = rellenar(h, ctx)
        if i > 0 and rng.random() < 0.45:
            oracion = f"{rng.choice(CONECTORES)}, {oracion[:1].lower()}{oracion[1:]}"
        if rng.random() < 0.25:
            oracion = oracion.rstrip(".") + rellenar(rng.choice(COLETILLAS), ctx)
        hechos.append(oracion)
    hechos.insert(1, rellenar(rng.choice(DETALLES_FECHA), ctx))
    hechos += bloque(DETALLES, 0, 2)

    evidencias = list(EVIDENCIAS)
    if tipo in TIPOS_DIGITALES:
        evidencias += EVIDENCIAS_DIGITALES
    if tipo in TIPOS_FISICOS:
        evidencias += EVIDENCIAS_FISICAS

    secciones = [
        [rellenar(rng.choice(APERTURAS), ctx)],
        [rellenar(rng.choice(PRESENTACION[tipo_q]), ctx)] + contexto,
        hechos + bloque(ESCALAMIENTO, 0, 2),
        bloque(evidencias, 0, 2) + bloque(REPORTES, 1, 2),
        bloque(IMPACTO, 2, 4) + bloque(REFLEXIONES, 1, 2),
        bloque(PETICIONES, 1, 3) + [rellenar(rng.choice(CIERRES), ctx)],
    ]
    if rng.random() < 0.3:
        secciones[1], secciones[2] = secciones[2][:2] + secciones[1], secciones[2][2:]

    vistos = set()

    def unico(generar):
        for _ in range(20):
            oracion = generar()
            if oracion not in vistos:
                vistos.add(oracion)
                return oracion
        return ""

    mazos = {}

    def sacar(banco):
        mazo = mazos.setdefault(id(banco), [])
        if not mazo:
            mazo.extend(rng.sample(banco, len(banco)))
        return mazo.pop()

    def episodio():
        def armar():
            reaccion = sacar(EP_REACCION)
            union = rng.choice(["; ", ", y ", ". Entonces "])
            reaccion = reaccion[:1].lower() + reaccion[1:]
            frase = f"{sacar(EP_MOMENTO)} {sacar(EP_SITUACION)} {sacar(EP_DESARROLLO)}{union}{reaccion}"
            if rng.random() < 0.3:
                frase += " " + sacar(EP_OTROS)
            return rellenar(frase.strip(), ctx)
        return unico(armar)

    def emocion():
        e1, e2 = rng.sample(EMO_SENTIR, 2)
        return unico(lambda: rellenar(rng.choice(EMO_PLANTILLAS).format(e1=e1, e2=e2, m=rng.choice(EMO_MOMENTO)), ctx))

    objetivo = rng.randint(500, 820)

    def contar():
        return sum(len(s.split()) for sec in secciones for s in sec)

    while contar() < objetivo:
        r = rng.random()
        if r < 0.55:
            secciones[2].insert(rng.randint(1, len(secciones[2])), episodio())
        elif r < 0.8:
            secciones[4].insert(rng.randint(0, len(secciones[4])), emocion())
        else:
            extra = tomar(rng, REFLEXIONES + IMPACTO, 1, usadas)
            secciones[4].append(rellenar(extra[0], ctx) if extra else episodio())

    separador = "\n\n" if rng.random() < 0.5 else " "
    texto = separador.join(" ".join(s for s in sec if s) for sec in secciones if any(sec)).strip()
    texto = re.sub(r"\b([Aa]) el\b", r"\1l", texto)
    texto = re.sub(r"\b([Dd])e el\b", r"\1el", texto)
    texto = ensuciar(texto, rng.choice([0, 1, 1, 2, 2, 3]), rng)

    return {
        "folio": f"FOL-HIS-{indice:04d}",
        "unidad_academica": escuela,
        "fecha_hechos": dia.isoformat(),
        "tipo_violencia": tipo,
        "lugar_hechos": lugar,
        "quejoso": {
            "nombre": qn.lower(), "apellido1": qa1.lower(), "apellido2": qa2.lower(),
            "correo": sin_acentos(f"{qn}.{qa1}{num_id[-3:]}@example.invalid").lower(),
            "tipo_identificacion": tipo_q, "numero_identificacion": num_id,
        },
        "denunciado": {
            "nombre": dn.lower(), "apellido1": da1.lower(), "apellido2": da2.lower(),
            "cargo": CARGO[rol], "relacion": RELACION[rol],
        },
        "texto_original": texto,
        "texto_preprocesado": sin_acentos(texto).lower(),
        "resumen_extractivo": "",
        "estatus": rng.choice(["FINALIZADA"] * 7 + ["EN_REVISION"] * 2 + ["RECIBIDA"]),
        "area_turnada": AREAS[tipo],
        "es_historico": True,
    }


def main():
    rng = random.Random(SEMILLA)
    with open(RUTA, encoding="utf-8") as f:
        datos = json.load(f)

    originales = [q for q in datos["quejas"] if int(q["folio"].split("-")[-1]) <= CONSERVAR_HASTA]
    reincidentes = crear_reincidentes(rng)
    nuevas = [generar_queja(CONSERVAR_HASTA + i + 1, rng, reincidentes) for i in range(NUEVAS)]

    datos["quejas"] = originales + nuevas
    datos["total"] = len(datos["quejas"])
    temporal = RUTA + ".tmp"
    with open(temporal, "w", encoding="utf-8") as f:
        json.dump(datos, f, ensure_ascii=False, indent=2)
    os.replace(temporal, RUTA)

    palabras = [len(q["texto_original"].split()) for q in nuevas]
    print(f"Dataset: {RUTA}")
    print(f"Originales conservadas: {len(originales)} | nuevas: {len(nuevas)} | total: {datos['total']}")
    print(f"Palabras (nuevas): min={min(palabras)} max={max(palabras)} promedio={sum(palabras) // len(palabras)}")
    print("Denunciados reincidentes:")
    for r in reincidentes:
        n = sum(1 for q in nuevas if q["denunciado"]["nombre"] == r["nombre"].lower() and q["denunciado"]["apellido1"] == r["ap1"].lower())
        print(f"  {r['nombre']} {r['ap1']} {r['ap2']} ({r['rol']}, {r['escuela']}): {n} quejas")


if __name__ == "__main__":
    main()
