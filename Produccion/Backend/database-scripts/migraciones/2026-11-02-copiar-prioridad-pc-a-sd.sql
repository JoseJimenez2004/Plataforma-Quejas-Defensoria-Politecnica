-- =====================================================================================
-- SD-6 (opcional): copia a Subdefensoría la prioridad que ya tenían en Primer Contacto los
-- expedientes que se turnaron ANTES de esta versión (llegaron a SD sin prioridad).
--
-- Une expedientes_investigacion.folio_origen (SD) con expedientes_primer_contacto.folio (PC).
-- Solo llena los que en SD siguen en NULL: no pisa lo que el subdefensor ya cambió.
-- Requiere que subdefensoria-service nuevo ya haya arrancado una vez (Hibernate crea la
-- columna expedientes_investigacion.prioridad con ddl-auto: update).
--
-- Uso (desde Backend/database-scripts):
--   podman exec -i defensoria-db psql -U postgres -d defensoria_db < migraciones/2026-11-02-copiar-prioridad-pc-a-sd.sql
-- =====================================================================================
BEGIN;

-- Vista previa: qué se va a copiar.
SELECT sd.folio AS folio_sd, pc.folio AS folio_pc, UPPER(pc.prioridad) AS prioridad
FROM expedientes_investigacion sd
JOIN expedientes_primer_contacto pc ON pc.folio = sd.folio_origen
WHERE sd.prioridad IS NULL
  AND UPPER(pc.prioridad) IN ('BAJA', 'MEDIA', 'ALTA', 'URGENTE');

UPDATE expedientes_investigacion sd
SET prioridad = UPPER(pc.prioridad)
FROM expedientes_primer_contacto pc
WHERE pc.folio = sd.folio_origen
  AND sd.prioridad IS NULL
  AND UPPER(pc.prioridad) IN ('BAJA', 'MEDIA', 'ALTA', 'URGENTE');

COMMIT;
