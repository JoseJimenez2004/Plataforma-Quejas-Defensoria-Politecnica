-- Usuarios de prueba para personal administrativo/staff
-- Contraseña en claro para todos: Prueba2026!
-- La contraseña cumple con la regla: mínimo 8 caracteres, al menos una mayúscula y un número.
-- Si los correos ya existen, actualiza sus datos y contraseña (UPSERT).

INSERT INTO personal_administrativo (
    nombre_completo,
    numero_empleado,
    correo_institucional,
    rol,
    password,
    cuenta_temporal,
    forzar_cambio_password,
    activo,
    fecha_creacion,
    ultimo_login
) VALUES (
    'Administrador de Prueba',
    'TEST-ADMIN-001',
    'admin.test@ipn.mx',
    'ADMIN_SISTEMAS',
    '$2a$10$rn5aaGoXkHAgCy3W2BStResAsYqZ.PhMW3iT6HVhfaDuSesCt4/4i',
    false,
    false,
    true,
    NOW(),
    NULL
), (
    'Recepcionista de Prueba',
    'TEST-RECEP-001',
    'recepcionista.test@ipn.mx',
    'RECEPCIONISTA',
    '$2a$10$rn5aaGoXkHAgCy3W2BStResAsYqZ.PhMW3iT6HVhfaDuSesCt4/4i',
    false,
    false,
    true,
    NOW(),
    NULL
), (
    'Analista Primer Contacto de Prueba',
    'TEST-PRIMER-001',
    'primer.contacto.test@ipn.mx',
    'ANALISTA_PRIMER_CONTACTO',
    '$2a$10$rn5aaGoXkHAgCy3W2BStResAsYqZ.PhMW3iT6HVhfaDuSesCt4/4i',
    false,
    false,
    true,
    NOW(),
    NULL
), (
    'Defensor de Prueba',
    'TEST-DEFEN-001',
    'defensor.test@ipn.mx',
    'DEFENSOR',
    '$2a$10$rn5aaGoXkHAgCy3W2BStResAsYqZ.PhMW3iT6HVhfaDuSesCt4/4i',
    false,
    false,
    true,
    NOW(),
    NULL
)
ON CONFLICT (correo_institucional)
DO UPDATE SET
    nombre_completo = EXCLUDED.nombre_completo,
    numero_empleado = EXCLUDED.numero_empleado,
    rol = EXCLUDED.rol,
    password = EXCLUDED.password,
    cuenta_temporal = EXCLUDED.cuenta_temporal,
    forzar_cambio_password = EXCLUDED.forzar_cambio_password,
    activo = EXCLUDED.activo,
    fecha_creacion = EXCLUDED.fecha_creacion;
