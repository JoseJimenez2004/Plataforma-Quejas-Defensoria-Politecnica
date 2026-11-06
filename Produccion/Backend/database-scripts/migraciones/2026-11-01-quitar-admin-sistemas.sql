-- =====================================================================================
-- Quita el rol ADMIN_SISTEMAS: por convención con la Defensoría, la administración de la
-- plataforma corresponde a la Defensora (rol DEFENSOR).
--
-- Correr ANTES de levantar los jars nuevos (admin-service, revision-service, primercontacto
-- ya no conocen ese valor del enum: una fila con rol ADMIN_SISTEMAS haría fallar la carga
-- del personal). Una sola transacción: o entra completo o no entra nada.
--   podman exec -i defensoria-db psql -U postgres -d defensoria_db < 2026-11-01-quitar-admin-sistemas.sql
-- =====================================================================================
BEGIN;

-- 1. Las cuentas con ADMIN_SISTEMAS pasan a DEFENSOR. Si ya existe otra cuenta DEFENSOR
--    activa, se desactivan (baja lógica: conservan su historial en la bitácora).
UPDATE personal_administrativo pa
SET rol = 'DEFENSOR',
    activo = CASE WHEN EXISTS (SELECT 1 FROM personal_administrativo o
                               WHERE o.rol = 'DEFENSOR' AND o.activo AND o.id <> pa.id)
                  THEN false ELSE pa.activo END
WHERE pa.rol = 'ADMIN_SISTEMAS';

-- 2. La restricción CHECK que creó Hibernate deja de admitir ADMIN_SISTEMAS.
ALTER TABLE personal_administrativo DROP CONSTRAINT IF EXISTS personal_administrativo_rol_check;
ALTER TABLE personal_administrativo ADD CONSTRAINT personal_administrativo_rol_check
    CHECK (rol IN ('RECEPCIONISTA', 'ANALISTA_PRIMER_CONTACTO', 'SUBDEFENSOR', 'DEFENSOR'));

-- Verificación
SELECT rol, activo, count(*) FROM personal_administrativo GROUP BY rol, activo ORDER BY rol;

COMMIT;
