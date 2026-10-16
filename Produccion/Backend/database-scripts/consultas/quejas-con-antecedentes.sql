-- Quejas que siguen en Recepción (por validar o turnar) y que SÍ tienen antecedentes:
-- otra queja de la misma persona (boleta/número o correo) o contra la misma persona señalada.
-- Sirve para probar la pantalla "Búsqueda de Antecedentes y Turnado".
--   podman exec -i defensoria-db psql -U postgres -d defensoria_db < quejas-con-antecedentes.sql
SELECT q.numero_folio,
       q.estatus,
       concat_ws(' ', q.nombre_quejoso, q.apellido1_quejoso)            AS quejoso,
       concat_ws(' ', q.nombre_denunciado, q.apellido1_denunciado)      AS denunciado,
       count(DISTINCT a.id)                                             AS antecedentes,
       string_agg(DISTINCT a.numero_folio || coalesce(' (' || a.defensor_asignado || ')', ''), ', ') AS cuales
  FROM quejas q
  JOIN quejas a
    ON a.id <> q.id
   AND (   (q.numero_identificacion_quejoso <> '' AND a.numero_identificacion_quejoso = q.numero_identificacion_quejoso)
        OR lower(a.correo_institucional) = lower(q.correo_institucional)
        OR (q.nombre_denunciado <> '' AND q.apellido1_denunciado <> ''
            AND lower(a.nombre_denunciado) = lower(q.nombre_denunciado)
            AND lower(a.apellido1_denunciado) = lower(q.apellido1_denunciado)))
 WHERE q.estatus IN ('RECIBIDA', 'CORREGIDA', 'EN_VALIDACION')
 GROUP BY q.numero_folio, q.estatus, quejoso, denunciado
 ORDER BY antecedentes DESC, q.numero_folio
 LIMIT 20;
