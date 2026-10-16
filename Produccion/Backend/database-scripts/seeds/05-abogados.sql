-- 5 abogados para recibir quejas turnadas (2026-10-16).
-- Se dan de alta con rol SUBDEFENSOR: aparecen en "Defensor / Abogado Responsable" al turnar
-- y en el panel "Carga de trabajo de los abogados". Al iniciar sesión en /revision/login el
-- sistema los manda a su panel (/subdefensoria/).
-- (No se usa el rol DEFENSOR porque ese es de la persona titular y entra al Panel de la Defensora.)
--
-- La cuenta queda creada con la contraseña de pruebas Temporal2026@ (cifrada con BCrypt aquí mismo).
-- Correr en la VPS de BD:
--   podman exec -i defensoria-db psql -U postgres -d defensoria_db < 05-abogados.sql
-- Se puede correr varias veces: si el correo ya existe, actualiza nombre, rol y contraseña.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO personal_administrativo (
    nombre_completo, numero_empleado, correo_institucional, rol, password,
    cuenta_temporal, forzar_cambio_password, activo, fecha_creacion, ultimo_login
) VALUES
    ('Lic. Alejandra Vega Morales',    'ABOG-001', 'alejandra.vega.abogada@ipn.mx',  'SUBDEFENSOR', crypt('Temporal2026@', gen_salt('bf', 10)), false, false, true, NOW(), NULL),
    ('Lic. Ricardo Núñez Paredes',     'ABOG-002', 'ricardo.nunez.abogado@ipn.mx',   'SUBDEFENSOR', crypt('Temporal2026@', gen_salt('bf', 10)), false, false, true, NOW(), NULL),
    ('Lic. Sofía Herrera Campos',      'ABOG-003', 'sofia.herrera.abogada@ipn.mx',   'SUBDEFENSOR', crypt('Temporal2026@', gen_salt('bf', 10)), false, false, true, NOW(), NULL),
    ('Lic. Daniel Ortega Fuentes',     'ABOG-004', 'daniel.ortega.abogado@ipn.mx',   'SUBDEFENSOR', crypt('Temporal2026@', gen_salt('bf', 10)), false, false, true, NOW(), NULL),
    ('Lic. Gabriela Luna Contreras',   'ABOG-005', 'gabriela.luna.abogada@ipn.mx',   'SUBDEFENSOR', crypt('Temporal2026@', gen_salt('bf', 10)), false, false, true, NOW(), NULL)
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
 WHERE rol IN ('SUBDEFENSOR', 'DEFENSOR')
 ORDER BY rol, nombre_completo;
