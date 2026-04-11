-- ==========================================
-- DATOS DE PRUEBA - Microservicio Recepcionista
-- Ejecutar DESPUÉS de init.sql
-- ==========================================

-- Catálogos faltantes
INSERT INTO cat_tema (nombre) VALUES
('Acoso escolar'),
('Discriminación'),
('Abuso de autoridad'),
('Irregularidades académicas'),
('Violencia');

INSERT INTO cat_tipo_documento (nombre) VALUES
('Queja formal'),
('Denuncia'),
('Solicitud de orientación');

INSERT INTO cat_calidad_persona (nombre) VALUES
('Estudiante'),
('Docente'),
('Administrativo');

INSERT INTO cat_tipo_contacto (nombre) VALUES
('Correo electrónico'),
('Teléfono');

INSERT INTO cat_dependencias (nombre, acronimo) VALUES
('Escuela Superior de Cómputo', 'ESCOM'),
('Escuela Nacional de Medicina', 'ENM'),
('Unidad Profesional Interdisciplinaria de Ingeniería', 'UPIITA');

INSERT INTO cat_subdefensoria (nombre) VALUES
('Subdefensoría de Derechos Estudiantiles'),
('Subdefensoría de Derechos del Personal');

INSERT INTO cat_calificacion (nombre) VALUES
('Procedente'),
('Improcedente'),
('Orientación');

-- Usuario recepcionista (id=1, usado como usuario_captura en quejas)
INSERT INTO usuarios (username, password_hash, email) VALUES
('recepcionista01', '$2a$10$dummy_hash_recepcionista', 'recepcionista@ipn.mx');

-- Usuarios quejosos
INSERT INTO usuarios (username, password_hash, email) VALUES
('juan.perez',    '$2a$10$dummy_hash_quejoso1', 'juan.perez@alumno.ipn.mx'),
('maria.garcia',  '$2a$10$dummy_hash_quejoso2', 'maria.garcia@alumno.ipn.mx'),
('carlos.lopez',  '$2a$10$dummy_hash_quejoso3', 'carlos.lopez@alumno.ipn.mx');

-- Perfiles quejosos (usuario_id 2, 3, 4)
INSERT INTO perfiles_quejoso (usuario_id, nombre, primer_apellido, segundo_apellido, es_mayor_edad, boleta_empleado, dependencia_id) VALUES
(2, 'Juan',   'Pérez',   'Hernández', true, '2020630001', 1),
(3, 'María',  'García',  'López',     true, '2021630045', 1),
(4, 'Carlos', 'López',   'Martínez',  true, '2019630089', 2);

-- Contactos de quejosos
INSERT INTO contactos_quejoso (quejoso_id, tipo_contacto_id, valor) VALUES
(1, 1, 'juan.perez@alumno.ipn.mx'),
(2, 1, 'maria.garcia@alumno.ipn.mx'),
(3, 1, 'carlos.lopez@alumno.ipn.mx');

-- Quejas con estatus RECIBIDA (id=1) → aparecen en el dashboard
INSERT INTO quejas (no_folio, fecha_recepcion, descripcion_hechos, tipo_documento_id, tema_id, estatus_id, usuario_captura_id, quejoso_id) VALUES
('TEMP-001', '2023-10-26', 'El docente de la materia de Cálculo reprobó mi examen sin justificación y se negó a mostrarme mi evaluación. Solicito revisión del caso.', 1, 4, 1, 1, 1),
('TEMP-002', '2023-10-25', 'Fui víctima de discriminación por parte de un administrativo al negarme un trámite escolar sin motivo aparente.', 2, 2, 1, 1, 2),
('TEMP-003', '2023-10-24', 'Compañeros de clase llevan semanas hostigándome verbalmente. Ya reporté a prefectura pero no tomaron acción.', 1, 1, 1, 1, 3);

-- Documentos adjuntos (para pantalla de Validación de Requisitos)
INSERT INTO documentos (queja_id, nombre_original, tipo_archivo, peso_bytes, hash_sha256, url_almacenamiento, usuario_subio_id) VALUES
(1, 'Documento_Identidad.pdf',   'application/pdf',  204800, 'abc123def456abc123def456abc123def456abc123def456abc123def456abc1', '/docs/queja1/identidad.pdf',    1),
(1, 'Evidencia_Fotografica.jpg', 'image/jpeg',        512000, 'bcd234efg567bcd234efg567bcd234efg567bcd234efg567bcd234efg567bcd2', '/docs/queja1/evidencia.jpg',    1),
(1, 'Declaracion_Escrita.docx',  'application/docx',   98304, 'cde345fgh678cde345fgh678cde345fgh678cde345fgh678cde345fgh678cde3', '/docs/queja1/declaracion.docx', 1),
(2, 'Solicitud_Negada.pdf',      'application/pdf',   153600, 'def456ghi789def456ghi789def456ghi789def456ghi789def456ghi789def4', '/docs/queja2/solicitud.pdf',    1),
(3, 'Captura_Mensajes.png',      'image/png',          307200, 'efg567hij890efg567hij890efg567hij890efg567hij890efg567hij890efg5', '/docs/queja3/captura.png',      1);
