-- =====================================================================================
-- Reparto ÚNICO de lo que ya existe (2026-11): cada analista de Primer Contacto y cada
-- abogado de Subdefensoría ve solo sus quejas.
--
-- CORRERLO DESPUÉS de desplegar los servicios nuevos (primer-contacto y subdefensoria).
-- Base: defensoria_db. Se corre con psql, en UNA sola sesión:
--
--     psql -h <host> -U postgres -d defensoria_db
--     \i reparto-asignaciones-2026-11.sql
--
-- Todo va dentro de una transacción. Al final se muestran los conteos por persona;
-- si se ven bien, escribe   COMMIT;   y si no,   ROLLBACK;   (no queda nada cambiado).
--
-- Reglas (acordadas con el equipo):
--   Primer Contacto
--     1. Expedientes CERRADOS (procedente, improcedente, remitido...) -> el analista que
--        los analizó (analista_analisis_id), si sigue activo como analista.
--     2. Todo lo demás sin analista (abiertos y cerrados sin analista conocido) -> por
--        turnos entre los analistas activos, del más antiguo al más nuevo. La diferencia
--        entre uno y otro queda en 1 o 2 expedientes.
--     3. quejas.defensor_asignado de las quejas ABIERTAS en PC = su analista (responsable
--        actual; es lo que usa la carga de trabajo de Recepción).
--   Subdefensoría
--     4. Expedientes con nombre de abogado pero sin id -> se busca su id por nombre.
--     5. Los que siguen sin abogado -> por turnos entre los SUBDEFENSORES activos
--        (el Defensor titular no recibe en el reparto; él ve todos).
--     6. quejas.defensor_asignado de las quejas no concluidas en SD = su abogado.
-- =====================================================================================

-- Por si el servicio nuevo de Primer Contacto todavía no creó las columnas.
ALTER TABLE expedientes_primer_contacto ADD COLUMN IF NOT EXISTS analista_asignado_id BIGINT;
ALTER TABLE expedientes_primer_contacto ADD COLUMN IF NOT EXISTS analista_asignado_nombre VARCHAR(150);

BEGIN;

-- ---------------------------------------------------------------- Vista previa: quién recibe
SELECT id, nombre_completo, rol
  FROM personal_administrativo
 WHERE activo AND rol IN ('ANALISTA_PRIMER_CONTACTO', 'SUBDEFENSOR', 'DEFENSOR')
 ORDER BY rol, id;

-- ---------------------------------------------------------------- 1. PC cerrados -> quien los analizó
UPDATE expedientes_primer_contacto e
   SET analista_asignado_id = p.id,
       analista_asignado_nombre = p.nombre_completo
  FROM personal_administrativo p
 WHERE e.analista_asignado_id IS NULL
   AND e.estatus NOT IN ('TURNADA', 'EN_ANALISIS')
   AND p.id = e.analista_analisis_id
   AND p.rol = 'ANALISTA_PRIMER_CONTACTO'
   AND p.activo;

-- ---------------------------------------------------------------- 2. PC resto -> por turnos
WITH analistas AS (
    SELECT id, nombre_completo,
           row_number() OVER (ORDER BY id) - 1 AS turno,
           count(*) OVER () AS total
      FROM personal_administrativo
     WHERE rol = 'ANALISTA_PRIMER_CONTACTO' AND activo
), pendientes AS (
    SELECT id, row_number() OVER (ORDER BY fecha_creacion, id) - 1 AS k
      FROM expedientes_primer_contacto
     WHERE analista_asignado_id IS NULL
)
UPDATE expedientes_primer_contacto e
   SET analista_asignado_id = a.id,
       analista_asignado_nombre = a.nombre_completo
  FROM pendientes x
  JOIN analistas a ON a.turno = x.k % a.total
 WHERE e.id = x.id;

-- ---------------------------------------------------------------- 3. Responsable en quejas (PC abiertos)
UPDATE quejas q
   SET defensor_asignado = e.analista_asignado_nombre
  FROM expedientes_primer_contacto e
 WHERE q.numero_folio = e.folio_origen
   AND e.estatus IN ('TURNADA', 'EN_ANALISIS')
   AND e.analista_asignado_nombre IS NOT NULL;

-- ---------------------------------------------------------------- 4. SD: id del abogado por nombre
UPDATE expedientes_investigacion s
   SET abogado_asesor_id = p.id,
       abogado_asesor_nombre = p.nombre_completo
  FROM personal_administrativo p
 WHERE s.abogado_asesor_id IS NULL
   AND s.abogado_asesor_nombre IS NOT NULL
   AND lower(trim(p.nombre_completo)) = lower(trim(s.abogado_asesor_nombre))
   AND p.rol IN ('SUBDEFENSOR', 'DEFENSOR')
   AND p.activo;

-- ---------------------------------------------------------------- 5. SD resto -> por turnos (subdefensores)
WITH abogados AS (
    SELECT id, nombre_completo,
           row_number() OVER (ORDER BY id) - 1 AS turno,
           count(*) OVER () AS total
      FROM personal_administrativo
     WHERE rol = 'SUBDEFENSOR' AND activo
), pendientes AS (
    SELECT id, row_number() OVER (ORDER BY fecha_admision, id) - 1 AS k
      FROM expedientes_investigacion
     WHERE abogado_asesor_id IS NULL
)
UPDATE expedientes_investigacion s
   SET abogado_asesor_id = a.id,
       abogado_asesor_nombre = a.nombre_completo
  FROM pendientes x
  JOIN abogados a ON a.turno = x.k % a.total
 WHERE s.id = x.id;

-- ---------------------------------------------------------------- 6. Responsable en quejas (SD abiertos)
UPDATE quejas q
   SET defensor_asignado = s.abogado_asesor_nombre
  FROM expedientes_investigacion s
 WHERE q.numero_folio = s.folio_queja
   AND s.estatus <> 'CONCLUIDO'
   AND s.abogado_asesor_nombre IS NOT NULL;

-- ---------------------------------------------------------------- Revisión (antes de COMMIT)
-- Primer Contacto: expedientes por analista (abiertos / total). Ninguno debe quedar sin analista.
SELECT coalesce(analista_asignado_nombre, '** SIN ANALISTA **') AS analista,
       count(*) FILTER (WHERE estatus IN ('TURNADA', 'EN_ANALISIS')) AS abiertos,
       count(*) AS total
  FROM expedientes_primer_contacto
 GROUP BY 1
 ORDER BY 1;

-- Subdefensoría: expedientes por abogado (no concluidos / total).
SELECT coalesce(abogado_asesor_nombre, '** SIN ABOGADO **') AS abogado,
       count(*) FILTER (WHERE estatus <> 'CONCLUIDO') AS en_tramite,
       count(*) AS total
  FROM expedientes_investigacion
 GROUP BY 1
 ORDER BY 1;

-- Si los conteos se ven bien:   COMMIT;
-- Si algo no cuadra:            ROLLBACK;
