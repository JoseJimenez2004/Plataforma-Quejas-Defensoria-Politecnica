-- Cuentas de quejosos generadas por cargar-quejas.sh
-- Correr en el servidor de BD:
--   podman exec -i defensoria-db psql -U postgres -d defensoria_db < cuentas-quejosos.sql
-- La contrasena se cifra con BCrypt dentro de PostgreSQL (pgcrypto, $2a$), el mismo
-- formato que usa auth-service, asi que el quejoso entra con su correo y esa contrasena.
-- ON CONFLICT DO NOTHING: si el correo o la boleta ya tienen cuenta, se deja como esta.
CREATE EXTENSION IF NOT EXISTS pgcrypto;
BEGIN;
INSERT INTO usuarios (nombre, correo_institucional, boleta, password, activo, intentos_codigo) VALUES ('María José Hernández López', 'mjhernandez.prueba@alumno.ipn.mx', '2021630001', crypt('Prueba2026!', gen_salt('bf', 10)), true, 0) ON CONFLICT DO NOTHING;
INSERT INTO usuarios (nombre, correo_institucional, boleta, password, activo, intentos_codigo) VALUES ('Juan Pérez', 'jperez.prueba@ipn.mx', '12345678', crypt('Prueba2026!', gen_salt('bf', 10)), true, 0) ON CONFLICT DO NOTHING;
COMMIT;
