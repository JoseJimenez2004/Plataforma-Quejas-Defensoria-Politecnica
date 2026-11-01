-- =============================================================================
-- 06-quejas-panel-defensora.sql
-- Quejas de demostración para el PANEL DE LA DEFENSORA (admin-service).
--
-- Inserta 62 quejas repartidas en TODAS las etapas y estatus, con fechas en los
-- últimos 12 meses, para que se vean llenos:
--   · KPIs del tablero (recepción, sin tomar, con quejoso, primer contacto,
--     subdefensoría, cerradas, rechazadas)
--   · Gráfica mensual de recibidas / atendidas
--   · Tabla y gráfica por unidad académica (ESCOM, ESIME, UPIICSA, CECyT...)
--   · "Casos que requieren atención": al menos 2 quejas por cada una de las 8
--     reglas (sin tomar, corregida sin revisar, validación detenida, observaciones
--     pendientes, sin abogado, vencida primer contacto, sin turnar a
--     Subdefensoría, vencida seguimiento), con severidad MEDIA y ALTA
--   · Explorador de quejas y su línea de tiempo
--
-- Requisitos: correr DESPUÉS de los seeds 01 (dependencias), 04 (recepcionistas)
-- y 05 (abogados). Usa sus claves y nombres reales.
--
-- Cómo correrlo (VPS de base de datos):
--   podman exec -i defensoria-db psql -U postgres -d defensoria_db < 06-quejas-panel-defensora.sql
--
-- Todos los folios empiezan con "FOL-D" para distinguirlos. Para BORRARLAS:
--   DELETE FROM quejas WHERE numero_folio LIKE 'FOL-D%';
-- Es seguro correrlo varias veces: los folios repetidos se ignoran.
-- =============================================================================

BEGIN;

SELECT setseed(0.2026);  -- mismos datos "aleatorios" en cada corrida

WITH
-- -----------------------------------------------------------------------------
-- Escenarios: estatus + cuántas + días atrás de cada fecha (mín, máx).
--   d_turn   = días desde el turnado (NULL si no se ha turnado)
--   d_val    = días desde que Recepción la validó/regresó (NULL si no aplica)
--   d_rev    = días desde que una recepcionista la abrió (solo EN_VALIDACION)
--   extra    = días que pasaron entre la creación y la fecha más antigua de arriba
--   abogado  = si lleva defensor_asignado
-- -----------------------------------------------------------------------------
escenario (esc, estatus, n, d_turn_min, d_turn_max, d_val_min, d_val_max,
           d_rev_min, d_rev_max, crea_min, crea_max, extra_min, extra_max, abogado) AS (
  VALUES
  -- RECEPCIÓN ---------------------------------------------------------------
  ( 1, 'RECIBIDA',               6, NULL,NULL, NULL,NULL, NULL,NULL,   0,   2,  0,  0, false), -- nuevas, aún en plazo
  ( 2, 'RECIBIDA',               4, NULL,NULL, NULL,NULL, NULL,NULL,   4,  12,  0,  0, false), -- regla SIN_TOMAR (>3 d)
  ( 3, 'EN_VALIDACION',          2, NULL,NULL, NULL,NULL,    0,   0, NULL,NULL,  1,  3, false), -- la abrieron hoy
  ( 4, 'EN_VALIDACION',          3, NULL,NULL, NULL,NULL,    2,   5, NULL,NULL,  1,  4, false), -- regla VALIDACION_DETENIDA (>1 d)
  ( 5, 'CORREGIDA',              2, NULL,NULL,    5,   9, NULL,NULL, NULL,NULL,  4,  9, false), -- regla CORREGIDA_SIN_REVISAR
  -- CON EL QUEJOSO ----------------------------------------------------------
  ( 6, 'RECHAZADA',              2, NULL,NULL,    1,   3, NULL,NULL, NULL,NULL,  1,  4, false), -- regresada hace poco
  ( 7, 'RECHAZADA',              4, NULL,NULL,    7,  16, NULL,NULL, NULL,NULL,  2,  8, false), -- regla OBSERVACIONES_PENDIENTES (>5 d)
  -- PRIMER CONTACTO ---------------------------------------------------------
  ( 8, 'TURNADA',                5,    1,   6, NULL,NULL, NULL,NULL, NULL,NULL,  1,  4,  true),  -- en plazo, con abogado
  ( 9, 'TURNADA',                2,    1,   4, NULL,NULL, NULL,NULL, NULL,NULL,  1,  3,  false), -- regla SIN_ABOGADO
  (10, 'EN_ANALISIS',            3,   12,  24, NULL,NULL, NULL,NULL, NULL,NULL,  2,  6,  true),  -- regla VENCIDA_PRIMER_CONTACTO (>10 d)
  (11, 'EN_ANALISIS',            2,    3,   8, NULL,NULL, NULL,NULL, NULL,NULL,  2,  5,  true),  -- en análisis, en plazo
  (12, 'PROCEDENTE',             2,   18,  35, NULL,NULL, NULL,NULL, NULL,NULL,  2,  5,  true),  -- regla SIN_TURNAR_SUBDEFENSORIA (>15 d)
  -- SUBDEFENSORÍA -----------------------------------------------------------
  (13, 'RECIBIDA_SUBDEFENSORIA', 2,   16,  30, NULL,NULL, NULL,NULL, NULL,NULL,  2,  6,  true),
  (14, 'EN_INVESTIGACION',       3,   20,  50, NULL,NULL, NULL,NULL, NULL,NULL,  2,  6,  true),
  (15, 'EN_ESPERA_OFICIO',       2,   70, 130, NULL,NULL, NULL,NULL, NULL,NULL,  2,  8,  true),  -- regla VENCIDA_SEGUIMIENTO (>60 d)
  (16, 'ELABORO_ACUERDO',        1,   40,  55, NULL,NULL, NULL,NULL, NULL,NULL,  2,  6,  true),
  (17, 'PENDIENTE_CONCILIACION', 2,   65, 100, NULL,NULL, NULL,NULL, NULL,NULL,  2,  6,  true),  -- también VENCIDA_SEGUIMIENTO
  (18, 'PENDIENTE_CONCLUSION',   1,   45,  58, NULL,NULL, NULL,NULL, NULL,NULL,  2,  6,  true),
  -- CERRADAS (repartidas en el año para la gráfica mensual) ------------------
  (19, 'CONCLUIDA',              8,   30, 330, NULL,NULL, NULL,NULL, NULL,NULL,  3, 10,  true),
  (20, 'IMPROCEDENTE',           4,   20, 300, NULL,NULL, NULL,NULL, NULL,NULL,  3,  8,  true),
  (21, 'REMITIDA',               1,   90, 200, NULL,NULL, NULL,NULL, NULL,NULL,  3,  8,  true),
  (22, 'CANCELADA',              1, NULL,NULL, NULL,NULL, NULL,NULL,  40, 250,  0,  0, false)
),
filas AS (
  SELECT e.*, g.i,
         row_number() OVER (ORDER BY e.esc, g.i) AS k
    FROM escenario e
    CROSS JOIN LATERAL generate_series(1, e.n) AS g(i)
),
-- Catálogos de apoyo (claves y nombres reales de los seeds 01, 04 y 05)
unidades   AS (SELECT ARRAY['ESCOM','ESCOM','ESCOM','ESIME-ZAC','ESIME-CUL','UPIICSA','UPIICSA',
                            'UPIITA','UPIBI','CECYT16','CECYT19','ESIME-AZC'] AS a),
recepcion  AS (SELECT ARRAY['ana.torres.recep@ipn.mx','luis.gomez.recep@ipn.mx',
                            'mariana.castillo.recep@ipn.mx','jorge.rios.recep@ipn.mx'] AS correo,
                      ARRAY['Ana Laura Torres Méndez','Luis Fernando Gómez Ruiz',
                            'Mariana Castillo Peña','Jorge Alberto Ríos Salas'] AS nombre),
abogados   AS (SELECT ARRAY['Lic. Alejandra Vega Morales','Lic. Ricardo Núñez Paredes',
                            'Lic. Sofía Herrera Campos','Lic. Daniel Ortega Fuentes',
                            'Lic. Gabriela Luna Contreras'] AS a),
nombres    AS (SELECT ARRAY['María Fernanda','José Luis','Daniela','Carlos Eduardo','Valeria','Miguel Ángel',
                            'Andrea','Luis Antonio','Ximena','Jorge','Paola','Ricardo','Itzel','Emiliano',
                            'Regina','Diego','Camila','Alejandro','Montserrat','Fernando'] AS n,
                      ARRAY['Hernández','García','Martínez','López','González','Pérez','Rodríguez','Sánchez',
                            'Ramírez','Cruz','Flores','Gómez','Morales','Vázquez','Jiménez','Reyes'] AS ap),
denunciado AS (SELECT ARRAY['Roberto','Patricia','Javier','Leticia','Arturo','Silvia','Héctor','Claudia'] AS n,
                      ARRAY['Mendoza','Castro','Ortiz','Ruiz','Aguilar','Navarro','Domínguez','Salinas'] AS ap),
textos     AS (SELECT ARRAY[
  'El profesor de la materia me grita frente al grupo y amenaza con reprobarme si no entrego trabajos personales fuera de horario.',
  'No se me permitió presentar el examen extraordinario a pesar de haber pagado y tener el comprobante.',
  'Personal de ventanilla de Gestión Escolar se negó a entregarme mi constancia sin justificación.',
  'Un docente condiciona la calificación final a la compra de su libro.',
  'Recibí comentarios ofensivos sobre mi apariencia por parte de un trabajador del laboratorio.',
  'Se publicó mi calificación con mi nombre completo en un grupo de mensajería sin mi autorización.',
  'Me dieron de baja de la unidad de aprendizaje sin notificarme ni darme oportunidad de aclarar.',
  'El jefe de departamento me niega el acceso a la biblioteca argumentando un adeudo que ya pagué.',
  'Un profesor cancela clases constantemente y luego evalúa temas que nunca se vieron.',
  'Se me negó la beca institucional pese a cumplir con todos los requisitos de la convocatoria.'] AS t),
rechazos   AS (SELECT ARRAY[
  'No se exhibió una identificación oficial vigente. Favor de adjuntar credencial del IPN o INE.',
  'El número de boleta registrado no coincide con el de la credencial adjunta.',
  'La queja no fue promovida dentro del término de un año a partir de los hechos.',
  'La queja no aporta elementos suficientes para identificar a la persona señalada.',
  'El nombre asentado en la queja no coincide con la identificación presentada.'] AS t),
-- Fechas calculadas por fila
calc AS (
  SELECT f.*,
         CASE WHEN f.d_turn_min IS NOT NULL
              THEN now() - ((f.d_turn_min + floor(random() * (f.d_turn_max - f.d_turn_min + 1))) * interval '1 day')
                         - (floor(random() * 9) * interval '1 hour') END AS f_turn,
         CASE WHEN f.d_val_min IS NOT NULL
              THEN now() - ((f.d_val_min + floor(random() * (f.d_val_max - f.d_val_min + 1))) * interval '1 day')
                         - (floor(random() * 9) * interval '1 hour') END AS f_val,
         CASE WHEN f.d_rev_min IS NOT NULL
              THEN now() - ((f.d_rev_min + floor(random() * (f.d_rev_max - f.d_rev_min + 1))) * interval '1 day')
                         - ((1 + floor(random() * 5)) * interval '1 hour') END AS f_rev,
         (f.extra_min + floor(random() * (f.extra_max - f.extra_min + 1)))::int AS extra,
         CASE WHEN f.crea_min IS NOT NULL
              THEN now() - ((f.crea_min + floor(random() * (f.crea_max - f.crea_min + 1))) * interval '1 day')
                         - (floor(random() * 10) * interval '1 hour') END AS f_crea_fija,
         floor(random() * 1000)::int AS r1, floor(random() * 1000)::int AS r2,
         floor(random() * 1000)::int AS r3, floor(random() * 1000)::int AS r4
    FROM filas f
),
final AS (
  SELECT c.*,
         -- creación = la fecha fija del escenario, o antes del turnado/validación
         coalesce(c.f_crea_fija,
                  least(coalesce(c.f_turn, now()), coalesce(c.f_val, now()), coalesce(c.f_rev, now())) - (c.extra * interval '1 day')
                    - (interval '1 day')) AS f_crea,
         -- las turnadas también fueron validadas por Recepción (cuenta como "atendida")
         coalesce(c.f_val, c.f_turn - interval '2 hours') AS f_validacion
    FROM calc c
)
INSERT INTO quejas (
    numero_folio, correo_institucional, motivo, descripcion, fecha_creacion,
    nombre_quejoso, apellido1_quejoso, apellido2_quejoso, fecha_nacimiento_quejoso,
    tipo_identificacion_quejoso, numero_identificacion_quejoso, telefono_contacto,
    unidad_academica_clave, fecha_hechos,
    nombre_denunciado, apellido1_denunciado,
    aviso_privacidad_aceptado, aviso_privacidad_fecha, aviso_privacidad_version,
    origen_registro, estatus,
    motivo_rechazo, validado_por, fecha_validacion,
    revisando_por, revisando_por_nombre, fecha_inicio_revision,
    area_turnada, defensor_asignado, comentarios_recepcion, fecha_turnado,
    numero_oficio, fecha_recepcion_fisica, tipo_documento_fisico, ubicacion_fisica_expediente
)
SELECT
    'FOL-D' || lpad(k::text, 7, '0'),
    lower(translate(nm.n[1 + r1 % 20], 'áéíóúÁÉÍÓÚñ ', 'aeiouAEIOUn.')) || '.'
      || lower(translate(nm.ap[1 + r2 % 16], 'áéíóúÁÉÍÓÚñ', 'aeiouAEIOUn')) || k || '.demo@alumno.ipn.mx',
    'Queja en ' || u.a[1 + r3 % 12],
    tx.t[1 + r4 % 10],
    f_crea,
    nm.n[1 + r1 % 20], nm.ap[1 + r2 % 16], nm.ap[1 + (r2 + 5) % 16],
    date '2001-01-01' + (r3 * 4) % 2500,                       -- entre 2001 y 2007 (mayores de edad)
    CASE WHEN k % 7 = 0 THEN 'empleado' ELSE 'alumno' END,
    CASE WHEN k % 7 = 0 THEN (20000 + r4)::text ELSE (2021630000 + k * 13)::text END,
    '55' || lpad(((r1 * 7919 + r2) % 100000000)::text, 8, '0'),
    u.a[1 + r3 % 12],
    (f_crea - ((5 + r4 % 40) * interval '1 day'))::date,
    dn.n[1 + r2 % 8], dn.ap[1 + r4 % 8],
    true, f_crea, 'v1',
    CASE WHEN k % 9 = 0 THEN 'MANUAL' WHEN k % 4 = 0 THEN 'AUTENTICADO' ELSE 'PUBLICO' END,
    estatus,
    -- Rechazo: RECHAZADA y CORREGIDA (la corregida conserva el motivo original)
    CASE WHEN estatus IN ('RECHAZADA','CORREGIDA') THEN rz.t[1 + r1 % 5] END,
    CASE WHEN estatus IN ('RECHAZADA','CORREGIDA') OR f_turn IS NOT NULL
         THEN rc.correo[1 + r2 % 4] END,
    CASE WHEN estatus IN ('RECHAZADA','CORREGIDA') OR f_turn IS NOT NULL THEN f_validacion END,
    -- En validación: quién la tiene abierta
    CASE WHEN estatus = 'EN_VALIDACION' THEN rc.correo[1 + r3 % 4] END,
    CASE WHEN estatus = 'EN_VALIDACION' THEN rc.nombre[1 + r3 % 4] END,
    CASE WHEN estatus = 'EN_VALIDACION' THEN f_rev END,
    -- Turnado
    CASE WHEN f_turn IS NOT NULL THEN 'Primer Contacto' END,
    CASE WHEN abogado THEN ab.a[1 + r4 % 5] END,
    CASE WHEN f_turn IS NOT NULL THEN 'Documentación completa. Se turna para su atención.' END,
    f_turn,
    -- Las de ventanilla (MANUAL) llevan datos del documento físico
    CASE WHEN k % 9 = 0 THEN 'DDP/' || (100 + k) || '/2026' END,
    CASE WHEN k % 9 = 0 THEN f_crea::date END,
    CASE WHEN k % 9 = 0 THEN 'ESCRITO_LIBRE' END,
    CASE WHEN k % 9 = 0 THEN 'Archivero A, cajón ' || (1 + k % 4) END
FROM final
CROSS JOIN unidades u CROSS JOIN recepcion rc CROSS JOIN abogados ab
CROSS JOIN nombres nm CROSS JOIN denunciado dn CROSS JOIN textos tx CROSS JOIN rechazos rz
ON CONFLICT (numero_folio) DO NOTHING;

-- -----------------------------------------------------------------------------
-- Comprobación: lo que debería ver la Defensora
-- -----------------------------------------------------------------------------
SELECT coalesce(estatus, 'RECIBIDA') AS estatus, count(*) AS quejas_demo
  FROM quejas WHERE numero_folio LIKE 'FOL-D%'
 GROUP BY 1 ORDER BY 2 DESC;

SELECT unidad_academica_clave AS unidad, count(*) AS quejas_demo
  FROM quejas WHERE numero_folio LIKE 'FOL-D%'
 GROUP BY 1 ORDER BY 2 DESC;

COMMIT;
