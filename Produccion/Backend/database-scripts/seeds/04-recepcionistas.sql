-- 4 recepcionistas más para simular el área de Recepción completa (2026-10-15).
-- Junto con bryanrecep@ipn.mx.com quedan 5. Todas con la misma contraseña de pruebas:
--   Temporal2026@
-- La contraseña se cifra aquí mismo con BCrypt (pgcrypto, formato $2a$ que acepta auth/admin).
-- Correr en la VPS de BD:
--   podman exec -i defensoria-db psql -U postgres -d defensoria_db < 04-recepcionistas.sql
-- Si un correo ya existe, actualiza su nombre, rol y contraseña (se puede correr varias veces).
CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO personal_administrativo (
    nombre_completo, numero_empleado, correo_institucional, rol, password,
    cuenta_temporal, forzar_cambio_password, activo, fecha_creacion, ultimo_login
) VALUES
    ('Ana Laura Torres Méndez',   'RECEP-002', 'ana.torres.recep@ipn.mx',    'RECEPCIONISTA', crypt('Temporal2026@', gen_salt('bf', 10)), false, false, true, NOW(), NULL),
    ('Luis Fernando Gómez Ruiz',  'RECEP-003', 'luis.gomez.recep@ipn.mx',    'RECEPCIONISTA', crypt('Temporal2026@', gen_salt('bf', 10)), false, false, true, NOW(), NULL),
    ('Mariana Castillo Peña',     'RECEP-004', 'mariana.castillo.recep@ipn.mx', 'RECEPCIONISTA', crypt('Temporal2026@', gen_salt('bf', 10)), false, false, true, NOW(), NULL),
    ('Jorge Alberto Ríos Salas',  'RECEP-005', 'jorge.rios.recep@ipn.mx',    'RECEPCIONISTA', crypt('Temporal2026@', gen_salt('bf', 10)), false, false, true, NOW(), NULL)
ON CONFLICT (correo_institucional)
DO UPDATE SET
    nombre_completo        = EXCLUDED.nombre_completo,
    rol                    = EXCLUDED.rol,
    password               = EXCLUDED.password,
    cuenta_temporal        = EXCLUDED.cuenta_temporal,
    forzar_cambio_password = EXCLUDED.forzar_cambio_password,
    activo                 = EXCLUDED.activo;

SELECT nombre_completo, correo_institucional, rol, activo
  FROM personal_administrativo
 WHERE rol = 'RECEPCIONISTA'
 ORDER BY nombre_completo;
