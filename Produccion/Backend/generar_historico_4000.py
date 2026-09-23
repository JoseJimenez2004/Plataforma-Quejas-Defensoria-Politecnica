# -*- coding: utf-8 -*-
"""
4000 quejas históricas sintéticas para defensoria_historico_db.

Incluye deliberadamente asuntos graves (hostigamiento y acoso sexual, violencia de
género, agresión sexual, violencia física, amenazas, represalias) porque el modelo de
antecedentes se va a topar con ellos en el archivo real de una Defensoría. Están
redactados en el registro institucional de un expediente: describen la conducta
reportada, las gestiones previas y la petición, sin detalle gráfico. Así es como se
lee un acta real y así es como el modelo debe aprender a leerlos.

NADA de esto son datos reales: nombres, boletas y números de empleado son inventados.
"""
import random, textwrap
from collections import Counter
from datetime import date, timedelta

random.seed(20260917)
TOTAL = 4000
SALIDA = "/mnt/user-data/outputs/seed_historico_4000.sql"

# ═══════════════════════════════════════════════════════ catálogos reales del sistema
UNIDADES = [
 "CECYT1","CECYT2","CECYT3","CECYT4","CECYT5","CECYT6","CECYT7","CECYT8","CECYT9",
 "CECYT10","CECYT11","CECYT12","CECYT13","CECYT14","CECYT15","CECYT16","CECYT17",
 "CECYT18","CECYT19","CET1","ESIME-ZAC","ESIME-CUL","ESIME-AZC","ESIME-TIC",
 "ESIA-ZAC","ESIA-TEC","ESIA-TIC","ESIT","ESFM","ESCOM","UPIICSA","UPIITA","UPIBI",
 "UPIG","UPIZ","ENCB","ESM","ENMH","ESEO","CICS-MA","CICS-ST","ESCA-ST","ESCA-TEP",
 "ESE","EST",
]
PESO_UNIDAD = [2]*20 + [5]*25
# TIPOS DE USUARIO: solo los 3 que ofrece el sistema (selector de registro-manual).
# No hay DOCENTE ni ADMINISTRATIVO por separado; ambos son EMPLEADO.

NOMBRES_M = """José Luis Juan Carlos Miguel Jorge Ricardo Fernando Alejandro Eduardo Roberto
Javier Daniel Sergio Arturo Raúl Óscar Héctor Pablo Andrés Emiliano Diego Iván Rodrigo
Gerardo Alan Bryan Omar César Ángel Antonio Francisco Manuel Alberto Enrique Rafael
Gustavo Armando Ernesto Salvador Ramón Felipe Mauricio Rubén Marco Julio Abraham Ismael
Leonardo Santiago Mateo Sebastián Maximiliano Axel Kevin Erick Uriel Isaac Noé Efraín
Gilberto Hugo Israel Joaquín Lorenzo Martín Nicolás Octavio Patricio""".split()

NOMBRES_F = """María Guadalupe Ana Laura Patricia Verónica Mónica Alejandra Claudia Gabriela
Rosa Martha Silvia Adriana Fernanda Karla Diana Paola Andrea Lucía Valeria Itzel Citlali
Xóchitl Brenda Nayeli Regina Camila Alondra Jimena Renata Ximena Mariana Daniela Carolina
Beatriz Elena Teresa Josefina Margarita Leticia Norma Elizabeth Susana Yolanda Araceli
Cecilia Dulce Estefanía Fabiola Gloria Ivonne Jacqueline Lorena Marisol Mayra Nancy
Olivia Perla Rocío Sandra Tania Úrsula Viridiana Wendy Yazmín Zaira Abril Berenice""".split()

APELLIDOS = """Hernández García Martínez López González Pérez Rodríguez Sánchez Ramírez Cruz
Flores Gómez Morales Vázquez Reyes Jiménez Torres Díaz Gutiérrez Ruiz Mendoza Aguilar
Ortiz Moreno Castillo Romero Álvarez Chávez Ramos Domínguez Herrera Medina Castro Vargas
Guerrero Rojas Contreras Salazar Luna Espinoza Juárez Cortés Ibarra Padilla Mejía Navarro
Rivas Cabrera Zamora Acosta Alvarado Arellano Ávila Bautista Becerra Bernal Camacho
Cárdenas Carrillo Cervantes Delgado Duarte Escobar Estrada Fuentes Galván Gallardo Godínez
Guzmán Lara Lozano Maldonado Márquez Meza Miranda Montes Muñoz Nájera Núñez Ochoa Olvera
Orozco Ortega Palacios Peña Ponce Quintero Rangel Rendón Rivera Robles Rosales Saldaña
Sandoval Santillán Serrano Solís Soto Tapia Tovar Trejo Valencia Valdez Vega Velázquez
Villanueva Zapata Zúñiga Alcántara Barrera Beltrán Bravo Calderón""".split()
APELLIDOS_COMPUESTOS = ["Del Valle","De la Cruz","De la Rosa","San Juan","De León","Del Río",
                        "De la Torre","Del Ángel","De los Santos","De Anda"]

# ═════════════════════════════════════════════════════════════════════ motivos
# (nombre, peso, familia, [párrafos de hechos])
# familia: ADMINISTRATIVA | ACADEMICA | VIOLENCIA
M = []

def mot(nombre, peso, familia, hechos):
    M.append((nombre, peso, familia, hechos))

# ─────────────────────────────────────────────── administrativas y académicas
mot("Irregularidades en la evaluación", 90, "ACADEMICA", [
 "La persona quejosa expone que la calificación asentada en el acta final no corresponde con las evaluaciones parciales, trabajos y prácticas que entregó durante el periodo escolar. Señala que conservó copia de cada entrega con acuse de recibo y que, al sacar el promedio conforme a los porcentajes que el propio docente publicó al inicio del curso, el resultado difiere de manera significativa del que quedó registrado.",
 "Refiere que solicitó al profesor la revisión de su examen final, en los términos que prevé el reglamento, y que le fue negada verbalmente sin entregarle respuesta por escrito ni explicarle el criterio con el que se calificó. Al acudir con el jefe de la academia, le indicaron que ese asunto correspondía exclusivamente al docente titular y que no podían intervenir.",
 "Manifiesta que el docente modificó los porcentajes de evaluación a mitad del periodo, sin notificarlo al grupo y sin dejar constancia del cambio, lo que alteró el resultado de quienes habían orientado su esfuerzo conforme a los criterios originalmente publicados.",
 "Expone que no se aplicó el examen de recuperación previsto en el programa de la unidad de aprendizaje, pese a que varios integrantes del grupo lo solicitaron por escrito con la debida anticipación, y que la solicitud nunca recibió respuesta.",
])
mot("Negativa de trámite escolar", 72, "ADMINISTRATIVA", [
 "La persona quejosa señala que se le negó la reinscripción bajo el argumento de un adeudo administrativo que afirma haber cubierto en tiempo y forma, y del cual conserva el comprobante de pago correspondiente. Refiere haber presentado dicho comprobante en la ventanilla sin que se le recibiera ni se le extendiera acuse.",
 "Expone que su solicitud de baja temporal fue rechazada sin que se le entregara respuesta por escrito ni se le fundamentara la negativa, lo que le impide regularizar su situación escolar y planear su reincorporación.",
 "Refiere que el área de gestión escolar se ha negado reiteradamente a recibir su solicitud de cambio de turno, remitiéndola de una ventanilla a otra sin que ninguna asuma la competencia del trámite.",
 "Manifiesta que su trámite de equivalencia de estudios lleva varios meses sin resolución, que en cada visita le informan que el expediente está en revisión y que no ha podido obtener un número de folio ni una fecha estimada de respuesta.",
])
mot("Trato indebido por parte del personal", 78, "ADMINISTRATIVA", [
 "La persona quejosa refiere haber recibido expresiones despectivas por parte del personal docente frente al grupo, en términos que considera humillantes y que, señala, se han repetido en distintas sesiones a lo largo del periodo.",
 "Reporta trato descortés y negativa de atención por parte del personal de la ventanilla de servicios escolares, quien le habría indicado en tono elevado que dejara de insistir con su trámite.",
 "Señala que fue increpada en público por personal administrativo al solicitar información sobre el estado de su solicitud, y que dicha situación ocurrió frente a otras personas que esperaban ser atendidas.",
])
mot("Retención indebida de documentos", 46, "ADMINISTRATIVA", [
 "La persona quejosa expone que la unidad académica retiene su certificado de estudios pese a que concluyó el trámite correspondiente y cubrió los requisitos que le fueron señalados, lo que le impide continuar con un proceso de admisión en otra institución.",
 "Refiere que no se le ha entregado su documento de acreditación argumentando un adeudo de biblioteca que asegura haber liquidado, y del cual presentó el comprobante sin que se le recibiera.",
])
mot("Irregularidades en el proceso de titulación", 52, "ACADEMICA", [
 "La persona egresada señala demoras injustificadas en la asignación de sinodales para su acto de titulación, situación que se ha prolongado por varios periodos sin que se le informe una fecha probable.",
 "Expone que su director de tesis dejó de dar seguimiento al trabajo y que la academia no designó a una persona sustituta, dejando el proceso detenido por tiempo indefinido.",
 "Refiere que se le exigieron requisitos que no están previstos en el reglamento de titulación vigente y que, al solicitar el fundamento normativo de dichos requisitos, no obtuvo respuesta.",
])
mot("Problemas con beca o apoyo económico", 58, "ADMINISTRATIVA", [
 "La persona quejosa reporta la suspensión de su beca sin notificación previa ni motivo informado, y señala que se enteró cuando el depósito correspondiente dejó de realizarse.",
 "Expone que su solicitud de beca fue rechazada sin que se le entregara el dictamen correspondiente ni se le explicara qué requisito no cubrió, lo que le impide subsanarlo o inconformarse.",
])
mot("Incumplimiento del programa académico", 52, "ACADEMICA", [
 "Se reporta inasistencia reiterada del docente titular sin que la academia designara persona suplente, de modo que el grupo perdió una parte considerable de las sesiones programadas en el periodo.",
 "El grupo señala que no se cubrieron las unidades temáticas comprometidas en el programa de estudios y que, pese a ello, la evaluación incluyó contenidos que nunca fueron impartidos.",
 "Se expone que las prácticas de laboratorio no se realizaron durante todo el semestre por falta de material y reactivos, situación que fue reportada en su momento a la jefatura sin obtener solución.",
])
mot("Negativa de acceso a servicios", 38, "ADMINISTRATIVA", [
 "La persona quejosa expone que se le negó el acceso al laboratorio de cómputo pese a contar con credencial vigente y estar inscrita en la unidad de aprendizaje que requiere su uso.",
 "Reporta que no se le permitió el uso de las instalaciones deportivas sin que se le diera explicación alguna ni se le mostrara disposición normativa que sustentara la negativa.",
])
mot("Cobros indebidos", 40, "ADMINISTRATIVA", [
 "Se reporta el cobro de una cuota no prevista en la normatividad para poder realizar un trámite escolar ordinario, y que dicho cobro se solicitó en efectivo y sin expedir comprobante.",
 "La persona quejosa señala que se le condicionó la entrega de documentos al pago de una aportación presentada como voluntaria, pero que en los hechos se exigió como requisito.",
])
mot("Violación al debido proceso en sanción", 46, "ADMINISTRATIVA", [
 "Se aplicó una sanción disciplinaria a la persona quejosa sin darle oportunidad de manifestar lo que a su derecho conviniera, y sin que se le corriera traslado de los hechos que se le imputaban.",
 "La persona quejosa refiere que no fue notificada del procedimiento seguido en su contra sino hasta que la sanción ya estaba impuesta, lo que le impidió ofrecer pruebas y alegar en su defensa.",
])
mot("Irregularidades en servicio social o prácticas", 34, "ADMINISTRATIVA", [
 "No se ha liberado el servicio social de la persona quejosa pese a que cubrió la totalidad de las horas requeridas y entregó los reportes en las fechas establecidas.",
 "Se reporta que la unidad receptora asignada no corresponde al perfil profesional del programa académico y que las actividades encomendadas no guardan relación con la formación recibida.",
])
mot("Discriminación", 34, "VIOLENCIA", [
 "La persona quejosa refiere haber recibido un trato diferenciado respecto del resto del grupo, sin que exista causa académica que lo justifique, y señala que dicho trato se intensificó después de que hizo pública una característica personal.",
 "Reporta que no se otorgaron los ajustes razonables que solicitó para la presentación de sus evaluaciones, pese a haber entregado la documentación de respaldo que le fue requerida.",
 "Expone que se le excluyó de actividades académicas y de equipos de trabajo por instrucción del propio docente, quien habría manifestado ante el grupo razones ajenas al desempeño académico.",
])
mot("Extravío de documentación oficial", 22, "ADMINISTRATIVA", [
 "La unidad académica reporta el extravío del expediente de la persona quejosa, lo que impide continuar con su trámite y le exige reponer documentación que ya había entregado.",
 "Se denuncia la pérdida del acta de evaluación correspondiente al periodo cursado, situación que dejó al grupo sin calificación registrada.",
])
mot("Negativa de servicios de salud escolar", 20, "ADMINISTRATIVA", [
 "Se reporta que no se brindó atención en el servicio médico de la unidad pese a tratarse de una situación que requería atención inmediata, y que se indicó a la persona acudir a una institución externa.",
 "La persona quejosa señala que no se emitió el dictamen necesario para justificar sus inasistencias, pese a haberlo solicitado por escrito en dos ocasiones.",
])

# ────────────────────────────────────────────────────────── asuntos graves
# Redacción en registro institucional: qué se reportó, cuándo, qué gestiones hubo y qué
# se pide. Sin detalle gráfico -- es como se asienta en un expediente real.
mot("Hostigamiento sexual", 44, "VIOLENCIA", [
 "La persona quejosa expone que una persona con jerarquía superior dentro de la unidad académica ha dirigido hacia ella, de manera reiterada, comentarios y proposiciones de naturaleza sexual no deseados, y que dichas conductas se han presentado en el contexto de la relación académica que las vincula. Señala que las insinuaciones se acompañaron de referencias a su situación académica, lo que interpreta como un condicionamiento.",
 "Refiere que, tras rechazar las proposiciones, comenzó a recibir un trato notoriamente distinto: le fueron negadas asesorías a las que antes tenía acceso, se le retiró de un proyecto en el que participaba y se elevó de manera injustificada el nivel de exigencia en sus evaluaciones respecto del resto del grupo.",
 "Manifiesta que la conducta se ha presentado tanto de manera presencial como a través de mensajes enviados fuera del horario escolar a su teléfono personal, y que conserva las capturas de dichos mensajes, las cuales ofrece como medio de prueba.",
 "Expone que existen otras personas del mismo grupo que han presenciado los hechos o que refieren haber vivido situaciones similares con la misma persona, y que están dispuestas a rendir testimonio ante esta Defensoría.",
])
mot("Acoso sexual", 40, "VIOLENCIA", [
 "La persona quejosa expone que ha sido objeto de conductas de naturaleza sexual no consentidas por parte de una persona con la que no guarda relación de subordinación, y que dichas conductas se han presentado de manera reiterada dentro de las instalaciones de la unidad académica.",
 "Refiere que las conductas incluyeron acercamientos físicos no consentidos y comentarios sobre su cuerpo, formulados en presencia de terceros, y que al pedir expresamente que cesaran, la conducta continuó.",
 "Señala que ha modificado sus horarios y sus rutas dentro del plantel para evitar coincidir con la persona señalada, lo que ha afectado su participación en actividades académicas y su asistencia a determinadas sesiones.",
 "Manifiesta que reportó la situación ante una autoridad de la unidad académica, donde se le sugirió resolverlo de manera personal y evitar formalizar la queja, razón por la cual acude ante esta Defensoría.",
])
mot("Agresión sexual", 22, "VIOLENCIA", [
 "La persona quejosa comparece para denunciar una agresión sexual ocurrida en las circunstancias de tiempo y lugar que precisa en su declaración, atribuida a una persona integrante de la comunidad politécnica. Se asienta la manifestación en los términos en que fue expuesta, sin recabar más detalle del estrictamente necesario.",
 "Se le informó de su derecho a presentar denuncia ante la autoridad ministerial competente, así como de los servicios de acompañamiento disponibles, y se dejó constancia de que la vía institucional no sustituye ni condiciona la vía penal.",
 "La persona quejosa solicita medidas de protección dentro del ámbito escolar mientras se desahoga el procedimiento, en particular que se evite cualquier coincidencia con la persona señalada en grupos, laboratorios y espacios comunes.",
 "Se hace constar que la persona quejosa manifestó su voluntad de que el asunto sea tratado con la reserva que corresponde y que su identidad no sea difundida dentro de la unidad académica.",
])
mot("Violencia de género", 38, "VIOLENCIA", [
 "La persona quejosa expone haber sido objeto de conductas discriminatorias y de menosprecio basadas en su género, de manera sostenida y en el contexto de las actividades académicas, incluyendo comentarios sobre su capacidad para cursar el programa por razón de ser mujer.",
 "Refiere que dichas expresiones se formularon frente al grupo y que se acompañaron de la asignación sistemática de tareas secundarias dentro de los equipos de trabajo, con independencia de su desempeño.",
 "Señala que al manifestar su inconformidad se le respondió que se trataba de bromas y que debía desarrollar mayor tolerancia, sin que la conducta cesara.",
])
mot("Violencia física", 24, "VIOLENCIA", [
 "La persona quejosa expone haber sido objeto de una agresión física dentro de las instalaciones de la unidad académica, y refiere haber recibido atención en el servicio médico del plantel, del cual solicita se recabe la constancia correspondiente.",
 "Refiere que el hecho ocurrió a la vista de otras personas integrantes de la comunidad, cuyos nombres proporciona, y que el personal presente no intervino para detener la agresión.",
 "Manifiesta que con anterioridad al hecho existieron episodios de confrontación verbal que fueron reportados ante la autoridad de la unidad sin que se tomara medida alguna.",
])
mot("Amenazas", 26, "VIOLENCIA", [
 "La persona quejosa expone haber recibido expresiones intimidatorias dirigidas a su persona y a su permanencia en la institución, formuladas por quien tiene atribuciones sobre su evaluación académica.",
 "Refiere que las expresiones se produjeron después de que manifestó su intención de inconformarse por una calificación, y que se le indicó que hacerlo tendría consecuencias en su trayectoria escolar.",
 "Señala que conserva mensajes escritos con el contenido referido, los cuales ofrece como medio de prueba, y solicita que se resguarde su identidad.",
])
mot("Represalias por presentar una queja", 26, "VIOLENCIA", [
 "La persona quejosa expone que, con posterioridad a la presentación de una inconformidad ante la unidad académica, comenzó a recibir un trato desfavorable que atribuye directamente a dicha presentación.",
 "Refiere que le fueron retiradas responsabilidades que tenía asignadas, se le excluyó de actividades en las que participaba habitualmente y se incrementó el nivel de escrutinio sobre su desempeño.",
 "Manifiesta que otras personas de su entorno académico le han comentado que se les sugirió mantener distancia respecto de su caso.",
])
mot("Abuso de autoridad", 34, "VIOLENCIA", [
 "La persona quejosa expone que una autoridad de la unidad académica ejerció atribuciones que no le corresponden para imponerle condiciones ajenas a la normatividad, aprovechando la posición jerárquica que ocupa.",
 "Refiere que se le exigió realizar actividades ajenas a su condición académica bajo advertencia de afectar su situación escolar en caso de negarse.",
 "Señala que las instrucciones se dieron de manera verbal y que, al solicitar que se le entregaran por escrito, le fue negado.",
])
mot("Otra situación no clasificada", 16, "ADMINISTRATIVA", [
 "El expediente original no especifica con claridad el motivo de la queja. Se transcribe lo asentado en el archivo del que proviene el registro, conservando la redacción de origen en lo que resulta legible.",
 "Asunto registrado en el archivo histórico sin clasificación de motivo, recuperado durante la digitalización del acervo documental del área.",
])

# ═════════════════════════════════════════════════ bloques de redacción del expediente
APERTURA = [
 "Comparece ante la Defensoría de los Derechos Politécnicos la persona quejosa, quien expone los hechos que a continuación se asientan y solicita la intervención de este órgano.",
 "Se recibe en esta Defensoría el escrito de queja presentado por la persona interesada, cuyo contenido se resume a continuación para efectos del expediente.",
 "Por comparecencia personal ante el área de recepción de esta Defensoría, se asienta la manifestación de hechos en los términos siguientes.",
 "Se registra la presente queja a partir del escrito recibido por oficialía de partes, del cual se desprenden los hechos que enseguida se relatan.",
 "La persona quejosa acude a esta Defensoría para exponer una situación que, refiere, no ha podido resolver ante las instancias de su unidad académica.",
]
CONTEXTO = [
 "Al momento de los hechos, la persona quejosa se encontraba inscrita en la unidad académica señalada, cursando el periodo escolar correspondiente de manera regular.",
 "La persona quejosa manifiesta tener trayectoria académica regular en la unidad y no registrar antecedentes disciplinarios previos a los hechos que expone.",
 "Los hechos se enmarcan en el desarrollo ordinario de las actividades académicas de la unidad, dentro del horario y las instalaciones institucionales.",
 "La relación entre la persona quejosa y la persona señalada se origina en el desarrollo de una unidad de aprendizaje del plan de estudios vigente.",
]
GESTIONES = [
 "Refiere haber acudido previamente ante la jefatura del departamento correspondiente, donde se le indicó que el asunto no era de su competencia y se le remitió a otra instancia, sin que ninguna asumiera el seguimiento.",
 "Manifiesta haber presentado un escrito ante la subdirección de la unidad académica, del cual conserva acuse de recibido, sin haber obtenido respuesta dentro del plazo que le fue informado.",
 "Expone que intentó resolver la situación por la vía del diálogo directo con la persona señalada, sin obtener resultado, y que posteriormente lo planteó ante la coordinación de carrera.",
 "Señala que la unidad académica le informó verbalmente que el asunto había sido atendido, sin entregarle constancia alguna ni comunicarle el sentido de la determinación.",
 "Indica que no ha realizado gestión previa ante su unidad académica por temor a que la situación se agrave, razón por la cual acude directamente ante esta Defensoría.",
]
PRUEBAS = [
 "Ofrece como medios de prueba los documentos que acompaña a su escrito, consistentes en copias de los acuses de recibido y de la documentación entregada a la unidad académica.",
 "Acompaña capturas de pantalla de la comunicación sostenida con la persona señalada, así como copia de su credencial vigente.",
 "Proporciona los nombres de las personas que presenciaron los hechos y manifiesta que se encuentran dispuestas a rendir testimonio ante esta Defensoría.",
 "Manifiesta no contar con documentación de respaldo, toda vez que las conductas se produjeron de manera verbal y sin la presencia de terceros que pudieran dar fe de ellas.",
 "Anexa copia simple del expediente que integró ante su unidad académica, así como constancia de calificaciones del periodo correspondiente.",
]
AFECTACION = [
 "Señala que la situación ha repercutido en su desempeño escolar y en su asistencia regular a las sesiones correspondientes.",
 "Manifiesta que los hechos han alterado su tránsito ordinario por las instalaciones y su participación en actividades del plantel.",
 "Refiere que la situación le ha generado incertidumbre respecto de la continuidad de su trayectoria académica.",
 "Expone que ha considerado solicitar baja temporal como consecuencia de los hechos que relata.",
]
PETICION = [
 "Solicita la intervención de esta Defensoría a efecto de que se investiguen los hechos, se restituya el derecho que estima vulnerado y se determine lo que en derecho corresponda.",
 "Pide que se requiera a la unidad académica un informe sobre los hechos y que se le comunique por escrito el resultado del procedimiento.",
 "Solicita que se adopten las medidas necesarias para que la situación no se repita y que se garantice que su queja no derive en represalias en su contra.",
 "Requiere que se le oriente sobre las vías institucionales aplicables y que se dé vista a las instancias competentes en caso de que así proceda.",
]
RESERVA = [
 "Se hace constar que la persona quejosa solicitó expresamente el tratamiento reservado de su identidad y de la información contenida en el expediente.",
 "Se informó a la persona quejosa sobre el tratamiento que se dará a sus datos personales y sobre las vías institucionales y externas que tiene disponibles.",
 "Se le hizo saber que puede acudir acompañada durante el desahogo del procedimiento y que puede aportar elementos adicionales en cualquier momento.",
]
CIERRE_AREA = [
 "El área de recepción hace constar que la manifestación se asentó en los términos expuestos por la persona quejosa, sin alterar su contenido.",
 "Se deja constancia de que el presente registro proviene del archivo histórico del área y fue capturado en el sistema durante el proceso de digitalización del acervo.",
 "Se anexa al expediente la documentación recibida y se turna para su análisis conforme al procedimiento aplicable.",
]

FUENTES = ["EXCEL"]*55 + ["OFICIO_FISICO"]*25 + ["LLAMADA"]*12 + ["SISTEMA_ANTERIOR"]*6 + ["OTRO"]*2

# ═══════════════════════════════════════════════════════════════ padrón de personas
def nombre_persona():
    if random.random() < 0.5:
        nom = random.choice(NOMBRES_M)
    else:
        nom = random.choice(NOMBRES_F)
    if random.random() < 0.18:
        nom += " " + random.choice(NOMBRES_M + NOMBRES_F)     # nombres compuestos
    pool = APELLIDOS + APELLIDOS_COMPUESTOS * 2
    a1 = random.choice(pool)
    a2 = random.choice(pool) if random.random() > 0.11 else None
    return nom, a1, a2

def boleta(anio):
    return f"{anio}{random.randint(600000, 699999)}"

def nuevo_quejoso():
    nom, a1, a2 = nombre_persona()
    r = random.random()
    if r < 0.82:
        tipo_usr = "ALUMNO"
        tipo_id, num_id = "BOLETA", boleta(random.randint(2010, 2024))
        if random.random() < 0.13:                 # registros viejos sin boleta anotada
            tipo_id = num_id = None
    elif r < 0.92:
        tipo_usr, tipo_id = "EMPLEADO", "EMPLEADO"
        num_id = f"EMP-{random.randint(10000, 99999)}"
    else:
        tipo_usr = "EXTERNO"                        # madres, padres, tutores, egresados
        if random.random() < 0.55:
            tipo_id = num_id = None
        else:
            tipo_id = random.choice(["INE", "CURP"])
            num_id = f"{random.randint(10**9, 10**10-1)}"
    return dict(nombre=nom, a1=a1, a2=a2, tipo_id=tipo_id, num_id=num_id,
                tipo_usr=tipo_usr, unidad=random.choices(UNIDADES, weights=PESO_UNIDAD)[0])

def nuevo_denunciado():
    nom, a1, a2 = nombre_persona()
    r = random.random()
    tipo_usr = "EMPLEADO" if r < 0.88 else "ALUMNO"
    if random.random() < 0.32:
        tipo_id = num_id = None
    elif tipo_usr == "ALUMNO":
        tipo_id, num_id = "BOLETA", boleta(random.randint(2010, 2023))
    else:
        tipo_id, num_id = "EMPLEADO", f"EMP-{random.randint(10000, 99999)}"
    return dict(nombre=nom, a1=a1, a2=a2, tipo_id=tipo_id, num_id=num_id, tipo_usr=tipo_usr)

QUEJOSOS = [nuevo_quejoso() for _ in range(3100)]
PESO_QUEJOSO = [1]*2950 + [5]*110 + [11]*40

DENUNCIADOS = [nuevo_denunciado() for _ in range(640)]
PESO_DENUNCIADO = [1]*540 + [3]*70 + [7]*30

# ═════════════════════════════════════════════════════════ armado de la descripción
motivos_exp = []
for nombre, peso, familia, hechos in M:
    motivos_exp += [(nombre, familia, hechos)] * peso

def redactar(hechos, objetivo):
    """Arma una descripción de expediente hasta acercarse a `objetivo` caracteres."""
    partes = [random.choice(APERTURA), random.choice(CONTEXTO)]
    orden = hechos[:]
    random.shuffle(orden)
    partes += orden
    extras = [random.choice(GESTIONES), random.choice(PRUEBAS),
              random.choice(AFECTACION), random.choice(PETICION)]
    partes += extras
    # Si aún falta longitud, se agregan bloques adicionales sin repetir el anterior.
    reserva = [random.choice(RESERVA), random.choice(CIERRE_AREA)]
    bolsa = GESTIONES + PRUEBAS + AFECTACION + CONTEXTO
    usados = set(partes)
    while len(" ".join(partes)) < objetivo:
        libres = [b for b in bolsa if b not in usados]
        if not libres:
            partes += reserva
            break
        elegido = random.choice(libres)
        usados.add(elegido)
        partes.insert(len(partes) - 1, elegido)
    if random.random() < 0.5:
        partes += reserva
    texto = " ".join(partes)
    return texto[:objetivo + 400]   # tope suave, nunca corta a media palabra por debajo

def objetivo_longitud():
    r = random.random()
    if r < 0.28:  return random.randint(900, 1300)      # cortas
    if r < 0.66:  return random.randint(1800, 2800)     # medias
    if r < 0.90:  return random.randint(3600, 4400)     # largas
    return random.randint(4700, 5400)                   # muy largas

# ═════════════════════════════════════════════════════════════════════ generación
def fecha_aleatoria(ini, fin):
    return ini + timedelta(days=random.randint(0, (fin - ini).days))

def esc(v):
    if v is None:            return "NULL"
    if isinstance(v, bool):  return "TRUE" if v else "FALSE"
    if isinstance(v, date):  return f"'{v.isoformat()}'"
    return "'" + str(v).replace("'", "''") + "'"

filas, folios, sin_folio = [], set(), 0
INI, FIN = date(2015, 1, 15), date(2025, 11, 30)

for _ in range(TOTAL):
    fuente = random.choice(FUENTES)
    f_pres = fecha_aleatoria(INI, FIN)
    anio = f_pres.year

    if fuente == "LLAMADA" and random.random() < 0.80:
        sin_folio += 1
        folio, generado = f"SF-{sin_folio:06d}", True
    else:
        for _ in range(80):
            if   fuente == "EXCEL":            cand = f"DDP/{anio}/{random.randint(1,9999):04d}"
            elif fuente == "OFICIO_FISICO":    cand = f"OF-DDP-{random.randint(1,999):03d}/{anio}"
            elif fuente == "SISTEMA_ANTERIOR": cand = f"{anio}-{random.randint(1,9999):04d}"
            else:                              cand = f"EXP{anio}{random.randint(1,9999):04d}"
            if cand not in folios: break
        else:
            sin_folio += 1
            cand = f"SF-{sin_folio:06d}"
        folio, generado = cand, False
    folios.add(folio)

    q = random.choices(QUEJOSOS, weights=PESO_QUEJOSO)[0]
    motivo, familia, hechos = random.choice(motivos_exp)

    # En los asuntos graves casi siempre hay una persona señalada.
    sin_den = 0.02 if familia == "VIOLENCIA" else 0.11
    d = None if random.random() < sin_den else random.choices(DENUNCIADOS, weights=PESO_DENUNCIADO)[0]

    descripcion = redactar(hechos, objetivo_longitud())

    f_hechos = None
    if random.random() > 0.09:
        f_hechos = f_pres - timedelta(days=random.randint(3, 300))

    r = random.random()
    if familia == "VIOLENCIA":
        # Los asuntos graves se rechazan menos y concluyen más con acuerdo o se remiten
        # a la autoridad competente.
        if   r < 0.58: estatus, resultado = "CONCLUIDA", random.choices(
                ["CONCLUIDA_CON_ACUERDO","CONCLUIDA_SIN_ACUERDO","SIN_DATO"], weights=[50,22,28])[0]
        elif r < 0.80: estatus, resultado = "REMITIDA", random.choices(
                ["REMITIDA_A_OTRA_AUTORIDAD","SIN_DATO"], weights=[85,15])[0]
        elif r < 0.92: estatus, resultado = "IMPROCEDENTE", random.choices(
                ["IMPROCEDENTE","SIN_DATO"], weights=[80,20])[0]
        else:          estatus, resultado = "RECHAZADA", "RECHAZADA_EN_RECEPCION"
    else:
        if   r < 0.63: estatus, resultado = "CONCLUIDA", random.choices(
                ["CONCLUIDA_CON_ACUERDO","CONCLUIDA_SIN_ACUERDO","SIN_DATO"], weights=[45,25,30])[0]
        elif r < 0.79: estatus, resultado = "RECHAZADA", random.choices(
                ["RECHAZADA_EN_RECEPCION","SIN_DATO"], weights=[80,20])[0]
        elif r < 0.93: estatus, resultado = "IMPROCEDENTE", random.choices(
                ["IMPROCEDENTE","SIN_DATO"], weights=[80,20])[0]
        else:          estatus, resultado = "REMITIDA", random.choices(
                ["REMITIDA_A_OTRA_AUTORIDAD","SIN_DATO"], weights=[75,25])[0]

    unidad = q["unidad"] if random.random() > 0.04 else None

    filas.append((folio, generado, fuente, f_pres, f_hechos, unidad, motivo, descripcion,
                  estatus, resultado,
                  q["tipo_id"], q["num_id"], q["nombre"], q["a1"], q["a2"], q["tipo_usr"],
                  (d or {}).get("tipo_id"), (d or {}).get("num_id"), (d or {}).get("nombre"),
                  (d or {}).get("a1"), (d or {}).get("a2"), (d or {}).get("tipo_usr"),
                  familia))

# ═══════════════════════════════════════════════════════════════════════════ SQL
COLS = ("folio, folio_generado, fuente, fecha_presentacion_original, fecha_hechos, "
        "unidad_academica_clave, motivo, descripcion, estatus, resultado, "
        "quejoso_tipo_identificacion, quejoso_numero_identificacion, quejoso_nombre, "
        "quejoso_apellido1, quejoso_apellido2, quejoso_tipo_usuario, "
        "denunciado_tipo_identificacion, denunciado_numero_identificacion, "
        "denunciado_nombre, denunciado_apellido1, denunciado_apellido2, "
        "denunciado_tipo_usuario, capturado_por, fecha_captura, notas_captura")

out = ["""-- =============================================================================
-- Datos de prueba: 4000 quejas históricas para defensoria_historico_db
-- Generado el 2026-09-17 · semilla fija (20260917), reproducible.
--
-- NO SON DATOS REALES. Nombres, boletas y números de empleado son inventados;
-- cualquier parecido con una persona real es coincidencia. Las claves de unidad
-- académica SÍ son las reales del catálogo, para que los filtros y los joins con
-- dependencias funcionen igual que en producción.
--
-- CONTIENE ASUNTOS GRAVES a propósito: hostigamiento y acoso sexual, agresión
-- sexual, violencia de género, violencia física, amenazas, represalias y abuso de
-- autoridad. El archivo real de una Defensoría los tiene y el modelo de
-- antecedentes tiene que aprender a leerlos. Están redactados en el registro
-- institucional de un expediente -- conducta reportada, gestiones previas y
-- petición -- sin detalle gráfico.
--
-- Reincidencia deliberada: 4000 quejas repartidas entre ~3100 quejosos y 640
-- personas señaladas, con algunas concentrando varias quejas. Si cada queja fuera
-- de una persona distinta, la búsqueda de antecedentes no encontraría nada.
--
--   podman exec -i defensoria-db psql -U postgres -d defensoria_historico_db \\
--     < seed_historico_4000.sql
-- =============================================================================

BEGIN;
"""]

LOTE = 50
for ini in range(0, len(filas), LOTE):
    out.append(f"INSERT INTO quejas_historicas ({COLS}) VALUES")
    vals = []
    for f in filas[ini:ini+LOTE]:
        campos = ", ".join(esc(x) for x in f[:22])
        vals.append(f"  ({campos}, 'carga.inicial@defensoria.ipn.mx', now(), "
                    f"'Carga inicial del archivo histórico (datos de prueba).')")
    out.append(",\n".join(vals) + ";\n")

out.append("""COMMIT;

-- =============================== VERIFICACIÓN ===============================
SELECT count(*) AS total FROM quejas_historicas;

SELECT motivo, count(*) AS quejas,
       round(avg(length(descripcion))) AS long_promedio
FROM quejas_historicas GROUP BY 1 ORDER BY 2 DESC;

SELECT estatus, resultado, count(*) FROM quejas_historicas GROUP BY 1,2 ORDER BY 3 DESC;

-- Reincidentes: lo que la búsqueda de antecedentes debe encontrar.
SELECT quejoso_numero_identificacion, quejoso_nombre, quejoso_apellido1, count(*) AS quejas
FROM quejas_historicas WHERE quejoso_numero_identificacion IS NOT NULL
GROUP BY 1,2,3 HAVING count(*) > 3 ORDER BY 4 DESC LIMIT 20;

-- Personas con más quejas EN SU CONTRA.
SELECT denunciado_numero_identificacion, denunciado_nombre, denunciado_apellido1,
       count(*) AS quejas_recibidas
FROM quejas_historicas WHERE denunciado_numero_identificacion IS NOT NULL
GROUP BY 1,2,3 ORDER BY 4 DESC LIMIT 20;

SELECT fuente, count(*) FROM quejas_historicas GROUP BY 1 ORDER BY 2 DESC;
SELECT extract(year FROM fecha_presentacion_original) AS anio, count(*)
FROM quejas_historicas GROUP BY 1 ORDER BY 1;
""")

with open(SALIDA, "w", encoding="utf-8") as fh:
    fh.write("\n".join(out))

# ═════════════════════════════════════════════════════════════════════ resumen
longs = [len(f[7]) for f in filas]
cq = Counter(f[11] for f in filas if f[11])
cd = Counter(f[17] for f in filas if f[17])
por_motivo = Counter(f[6] for f in filas)
fam = {n: fa for n, _, fa, _ in M}

resumen = []
resumen.append(("TOTAL", "", len(filas), "", ""))
for nombre, peso, familia, _ in sorted(M, key=lambda x: -por_motivo[x[0]]):
    n = por_motivo[nombre]
    ls = [len(f[7]) for f in filas if f[6] == nombre]
    resumen.append((nombre, familia, n, f"{100*n/len(filas):.1f}%",
                    f"{min(ls)}–{max(ls)} (prom {sum(ls)//len(ls)})"))

print(f"Filas: {len(filas)} · folios únicos: {len(folios)} · SF- generados: {sin_folio}")
print(f"Descripciones: min {min(longs)} · prom {sum(longs)//len(longs)} · max {max(longs)} caracteres")
print(f"  <1500: {sum(1 for l in longs if l<1500)} · 1500-3000: {sum(1 for l in longs if 1500<=l<3000)}"
      f" · 3000-4500: {sum(1 for l in longs if 3000<=l<4500)} · >=4500: {sum(1 for l in longs if l>=4500)}")
print(f"Quejosos identificados: {len(cq)} · con 4+ quejas: {sum(1 for v in cq.values() if v>=4)}")
print(f"Denunciados identificados: {len(cd)} · máximo en contra de uno: {max(cd.values())}")
print(f"Quejas de familia VIOLENCIA: {sum(1 for f in filas if f[22]=='VIOLENCIA')}")
print(f"Tipos de usuario quejoso: {Counter(f[15] for f in filas).most_common()}")

import json
with open("/mnt/user-data/outputs/resumen.json","w",encoding="utf-8") as fh:
    json.dump(resumen, fh, ensure_ascii=False)
