-- =============================================================================
-- Primer Contacto: alineación con el diagrama de estados (2026-09-27)
--
-- Acompaña los cambios de CU-PC-01..10. Se puede correr ANTES o DESPUÉS de subir el
-- jar nuevo: las columnas se crean con IF NOT EXISTS (Hibernate también las crea con
-- ddl-auto=update, pero así el script no depende del orden).
--
--   podman exec -i defensoria-db psql -U postgres -d defensoria_db \
--     < migracion-estados-primer-contacto-2026-09-27.sql
--
-- NO toca quejas.estatus: la sincronización con esa tabla viene apagada
-- (primer-contacto.sincronizar-estatus-queja=false) hasta que el panel del quejoso
-- reconozca los estados nuevos.
-- =============================================================================

BEGIN;

-- ── Columnas nuevas (todas nullable: las tablas ya tienen filas) ──────────────
ALTER TABLE expedientes_primer_contacto ADD COLUMN IF NOT EXISTS fecha_inicio_analisis    timestamp(6);
ALTER TABLE expedientes_primer_contacto ADD COLUMN IF NOT EXISTS analista_analisis_id     bigint;
ALTER TABLE expedientes_primer_contacto ADD COLUMN IF NOT EXISTS analista_analisis_nombre varchar(150);

ALTER TABLE remisiones_externas ADD COLUMN IF NOT EXISTS estatus       varchar(20);
ALTER TABLE remisiones_externas ADD COLUMN IF NOT EXISTS numero_oficio varchar(40);
ALTER TABLE remisiones_externas ADD COLUMN IF NOT EXISTS fecha_envio   timestamp(6);

ALTER TABLE citas_primer_contacto ADD COLUMN IF NOT EXISTS actualizado_por_id     bigint;
ALTER TABLE citas_primer_contacto ADD COLUMN IF NOT EXISTS actualizado_por_nombre varchar(150);
ALTER TABLE citas_primer_contacto ADD COLUMN IF NOT EXISTS fecha_actualizacion    timestamp(6);

-- ── Remisiones existentes: estatus propio ─────────────────────────────────────
-- "Redactada vs. enviada" ahora vive en la remisión. Las que ya estaban enviadas
-- (expediente REMITIDA) quedan ENVIADA; las demás, GENERADA.
UPDATE remisiones_externas r
   SET estatus = CASE WHEN e.estatus = 'REMITIDA' THEN 'ENVIADA' ELSE 'GENERADA' END,
       fecha_envio = CASE WHEN e.estatus = 'REMITIDA' THEN r.fecha_remision END
  FROM expedientes_primer_contacto e
 WHERE e.id = r.expediente_id
   AND r.estatus IS NULL;

UPDATE remisiones_externas
   SET numero_oficio = 'DDP/PC/REM/' || extract(year from fecha_remision)::int || '/' || lpad(id::text, 4, '0')
 WHERE numero_oficio IS NULL;

-- ── Expedientes: PENDIENTE_REMISION ya no es un estado del expediente ─────────
-- El expediente sigue IMPROCEDENTE hasta que su remisión se envía (-> REMITIDA).
UPDATE expedientes_primer_contacto SET estatus = 'IMPROCEDENTE'
 WHERE estatus = 'PENDIENTE_REMISION';

COMMIT;

-- ── Verificación ──────────────────────────────────────────────────────────────
SELECT 'expedientes' AS tabla, estatus, count(*) FROM expedientes_primer_contacto GROUP BY 1, 2
UNION ALL
SELECT 'remisiones', estatus, count(*) FROM remisiones_externas GROUP BY 1, 2
ORDER BY 1, 2;
