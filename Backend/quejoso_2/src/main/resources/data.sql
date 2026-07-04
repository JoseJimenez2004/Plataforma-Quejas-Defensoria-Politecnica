-- ============================================================
-- data.sql  —  Se ejecuta UNA VEZ al arrancar (ddl-auto=update)
-- Hibernate ya creó las tablas; aquí solo insertamos datos de prueba.
-- ============================================================

-- Usuario de prueba
-- password = "Test1234" encriptado con BCrypt (puedes cambiarlo)
INSERT INTO usuarios (
    nombre,
    correo_institucional,
    boleta,
    password,
    activo,
    unidad_academica,
    correo_personal,
    telefono_celular
) VALUES (
    'Jair Estudiante',
    'jair100flo@ipn.mx',
    '2024000001',
    '$2a$10$7EqIkVLMX5mYpLQCHG9sbuJVHEpAFvAK9XVS1xKxHPPMWv6BnPW1G',
    true,
    'ESCOM',
    'jair_personal@gmail.com',
    '5512345678'
);

-- Queja de prueba ligada al usuario con id=1
INSERT INTO quejas (
    folio,
    asunto,
    descripcion,
    estatus,
    fecha_registro,
    unidaddonde_ocurrio,
    usuario_id,
    correo_quejoso,
    identificacion_institucional,
    tipo_identificacion
) VALUES (
    'DDP-2026-0001',
    'Falla en acceso a red',
    'No puedo conectar mi laptop en los laboratorios de ESCOM.',
    'RECIBIDA',
    CURRENT_TIMESTAMP,
    'ESCOM',
    1,
    'jair100flo@ipn.mx',
    '2024000001',
    'ALUMNO'
);
