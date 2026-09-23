-- =============================================================================
-- Migración de estados al diagrama de la queja (versión 2026-09-18)
--
-- Los renombres en el código NO mueven los datos: si solo se sube el jar, los
-- expedientes que ya existen se quedan con el texto viejo y desaparecen de las
-- bandejas, porque ninguna consulta los reconoce. Este script los alinea.
--
-- CORRER ANTES de levantar los contenedores con el código nuevo.
--
--   podman exec -i defensoria-db psql -U postgres -d defensoria_db \
--     < migracion_estados_diagrama.sql
-- =============================================================================

BEGIN;

-- ─────────────────────────────── Primer Contacto ───────────────────────────
-- PENDIENTE_ANALISIS -> EN_ANALISIS
-- TURNADO_SUBDEFENSORIA -> PROCEDENTE
-- El par de remisión estaba INVERTIDO: el expediente pasaba a "REMITIDA" al
-- REDACTAR la remisión y a "REMISION_ENVIADA" al enviarla. Se corrige para que
-- REMITIDA signifique lo que dice.
UPDATE expedientes_primer_contacto SET estatus = 'EN_ANALISIS'
 WHERE estatus = 'PENDIENTE_ANALISIS';

UPDATE expedientes_primer_contacto SET estatus = 'PROCEDENTE'
 WHERE estatus = 'TURNADO_SUBDEFENSORIA';

-- OJO con el orden: primero la que ya está enviada, luego la redactada. Al revés,
-- las redactadas se convertirían en enviadas.
UPDATE expedientes_primer_contacto SET estatus = '__TMP_ENVIADA__'
 WHERE estatus = 'REMISION_ENVIADA';

UPDATE expedientes_primer_contacto SET estatus = 'PENDIENTE_REMISION'
 WHERE estatus = 'REMITIDA';

UPDATE expedientes_primer_contacto SET estatus = 'REMITIDA'
 WHERE estatus = '__TMP_ENVIADA__';

-- ──────────────────────────────── Subdefensoría ────────────────────────────
UPDATE expedientes_investigacion SET estatus = 'EN_ESPERA_OFICIO'
 WHERE estatus = 'EN_GESTION_DIRECTOR';

UPDATE expedientes_investigacion SET estatus = 'ELABORO_ACUERDO'
 WHERE estatus = 'LISTO_A_DICTAMINAR';

-- Los expedientes ya CONCLUIDOS se quedan como están: se cerraron bajo la regla
-- anterior y reabrirlos para pedirle respuesta al quejoso sería reescribir
-- historia. PENDIENTE_CONCLUSION aplica de aquí en adelante.

COMMIT;

-- ── Verificación: no debe quedar ningún estado viejo ───────────────────────
SELECT 'primer_contacto' AS tabla, estatus, count(*)
  FROM expedientes_primer_contacto GROUP BY 1,2
UNION ALL
SELECT 'investigacion', estatus, count(*)
  FROM expedientes_investigacion GROUP BY 1,2
UNION ALL
SELECT 'quejas', estatus, count(*)
  FROM quejas GROUP BY 1,2
ORDER BY 1,2;
