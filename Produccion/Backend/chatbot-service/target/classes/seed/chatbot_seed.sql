-- Seed de contenido del chatbot (mini-chat/tutorial del portal del quejoso) para
-- chatbot-service -- correr una sola vez tras el primer arranque del servicio (Hibernate
-- ddl-auto=update ya habrá creado la tabla 'preguntas_chatbot' vacía).
--
-- Contenido redactado a partir de fuentes oficiales de la Defensoría de los Derechos
-- Politécnicos:
--   - Acuerdo por el que se expide la Declaración de los Derechos Politécnicos y se
--     establece la Defensoría de los Derechos Politécnicos del IPN
--     (Gaceta Politécnica, Número Extraordinario 622, 31 de enero de 2006).
--   - Manual de Procedimientos de la DDP (DDP-MP-00 / DDP-PO-02, versión 01, 2024-10-25).
--   - Información pública del sitio de la Defensoría (servicio de orientación, contacto).
--
-- "orden" es GLOBAL (no reinicia por categoría) y se deja en bloques de 10 para poder
-- insertar preguntas nuevas entre dos existentes sin tener que renumerar todo.

INSERT INTO preguntas_chatbot (categoria, pregunta, respuesta, orden, activo, creado_en, actualizado_en) VALUES

-- ===== Sobre la Defensoría =====
('Sobre la Defensoría',
 '¿Qué es la Defensoría de los Derechos Politécnicos?',
 'La Defensoría de los Derechos Politécnicos (DDP) es un órgano autónomo del Instituto Politécnico Nacional que actúa con independencia de las autoridades del Instituto. Su objetivo es la promoción, protección, defensa, estudio y divulgación de los derechos de la comunidad politécnica (alumnado, personal académico, administrativo, directivo y egresados), bajo los principios de legalidad, imparcialidad, eficiencia y oportunidad.',
 10, TRUE, now(), now()),

('Sobre la Defensoría',
 '¿Qué es una queja?',
 'Es la manifestación por escrito de cualquier miembro de la comunidad politécnica respecto de presuntos actos u omisiones cometidos en su agravio por parte de autoridades del Instituto, ya sean administrativas o académicas.',
 20, TRUE, now(), now()),

('Sobre la Defensoría',
 '¿Quién puede presentar una queja u orientación?',
 'Cualquier integrante de la comunidad politécnica: alumnado, personal académico, administrativo, directivo y de mando, así como egresadas y egresados del IPN.',
 30, TRUE, now(), now()),

-- ===== Requisitos y Plazos =====
('Requisitos y Plazos',
 '¿Qué necesito para presentar mi queja?',
 'Tu queja debe incluir: nombre completo, número de cuenta (si eres alumno) o número de empleado, la autoridad señalada precisando escuela, centro o unidad, un domicilio o correo para recibir notificaciones, una descripción detallada de los hechos, copia de los documentos o pruebas que tengas relacionados, y tu firma. En este portal, estos datos corresponden a los campos del formulario y a la sección de evidencias que puedes adjuntar.',
 100, TRUE, now(), now()),

('Requisitos y Plazos',
 '¿Cuánto tiempo tengo para presentar mi queja?',
 '90 días naturales contados a partir de los hechos que la motivan. Si ya interpusiste otro recurso relacionado con los mismos hechos, el plazo se pausa mientras ese recurso se resuelve.',
 110, TRUE, now(), now()),

('Requisitos y Plazos',
 '¿Por qué podrían rechazar mi queja?',
 'La Defensoría puede rechazarla si es anónima, si de los hechos narrados se advierte mala fe o no existe una pretensión real, si se refiere a hechos con más de 90 días de anterioridad, o si el asunto no es de su competencia. En ese caso se te notificará la causa fundada del rechazo.',
 120, TRUE, now(), now()),

('Requisitos y Plazos',
 '¿Qué asuntos NO atiende la Defensoría?',
 'No es competente para conocer de: afectaciones de carácter colectivo, resoluciones disciplinarias, derechos de naturaleza laboral, evaluaciones académicas (salvo que en ellas se violen notoriamente tus derechos), y procedimientos o resoluciones del Órgano Interno de Control.',
 130, TRUE, now(), now()),

-- ===== Cómo Presentar tu Queja en este Portal =====
('Cómo Presentar tu Queja',
 '¿Cómo presento mi queja paso a paso en este portal?',
 E'1) Entra a la opción "Presentar una queja".\n2) Llena tus datos personales y de contacto.\n3) Describe con el mayor detalle posible los hechos que consideras que vulneran tus derechos: qué pasó, cuándo y qué autoridad estuvo involucrada.\n4) Adjunta la evidencia que tengas (documentos, capturas, oficios, etc.). Mientras más completa esté tu queja, más rápida será su validación.\n5) Envía tu queja: recibirás un número de folio (por ejemplo DDP-0001). Guárdalo junto con el correo que usaste para registrarla.\n6) Con ese folio y tu correo podrás consultar el estatus de tu trámite en cualquier momento desde la página principal.',
 200, TRUE, now(), now()),

('Cómo Presentar tu Queja',
 '¿Qué pasa después de que envío mi queja?',
 'Personal de la Defensoría revisa que tu queja venga completa. Si falta información, te lo notificaremos para que la completes desde tu portal con tus observaciones. Si está completa, se admite y se turna al área correspondiente para su investigación; la autoridad señalada cuenta con un plazo para responder.',
 210, TRUE, now(), now()),

('Cómo Presentar tu Queja',
 '¿Cómo consulto el estatus de mi queja?',
 'En la página principal, en la opción de consulta de estatus, ingresa tu número de folio y el correo electrónico con el que registraste tu queja.',
 220, TRUE, now(), now()),

-- ===== Servicio de Orientación =====
('Servicio de Orientación',
 '¿Qué es el servicio de orientación y en qué se diferencia de una queja?',
 'La orientación es cuando personal de la Defensoría te informa sobre la naturaleza de tu problema, las posibles formas de solución y las acciones que puedes realizar, sin necesidad de abrir una queja formal. Puedes solicitarla de forma personal, telefónica, por correo electrónico o por escrito. Si tu solicitud no es competencia de la Defensoría, se te informará qué autoridad puede atenderla.',
 300, TRUE, now(), now()),

('Servicio de Orientación',
 '¿El servicio tiene costo?',
 'No. Todos los servicios que brinda la Defensoría son gratuitos.',
 310, TRUE, now(), now()),

('Servicio de Orientación',
 '¿Mis datos personales están protegidos?',
 'Sí. Tus datos personales se manejan de forma confidencial y reservada, conforme a la normatividad de transparencia y acceso a la información aplicable.',
 320, TRUE, now(), now()),

-- ===== Contacto y Horarios =====
('Contacto y Horarios',
 '¿Cuál es el horario de atención?',
 'Lunes a viernes de 9:00 a 14:00 y de 16:00 a 19:00 horas (días hábiles) para orientación personal y telefónica. Los medios electrónicos (este portal y correo) están disponibles todos los días.',
 400, TRUE, now(), now()),

('Contacto y Horarios',
 '¿Cómo contacto directamente a la Defensoría?',
 'Teléfono: 55 5729 6000, extensiones 57277 y 57278. Correo electrónico: quejasddp@ipn.mx. Sitio web: www.ipn.mx/defensoria/.',
 410, TRUE, now(), now());
