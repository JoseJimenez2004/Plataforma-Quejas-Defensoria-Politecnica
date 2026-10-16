-- =============================================================================
-- Datos de prueba para la base LOCAL de Primer Contacto (solo desarrollo).
--
-- 01-estructura.sql es el esquema de defensoria_db tal como estaba en el servidor
-- (dump 2026-08-22), sin datos. Aquí se agrega lo mínimo para recorrer los CU-PC:
--
--   * un analista de Primer Contacto (el correo es el "sub" del JWT de prueba),
--   * un segundo analista, para probar que nadie edita/borra notas ajenas,
--   * tres quejas ya TURNADAS con evidencias reales (BYTEA), listas para que
--     revision-service -- o el script de ingesta -- las mande a Primer Contacto.
--
-- NUNCA correr esto contra el servidor.
-- =============================================================================

-- Columnas que el código actual ya usa y el dump de agosto todavía no tenía.
ALTER TABLE quejas ADD COLUMN IF NOT EXISTS apellido1_quejoso varchar(255);
ALTER TABLE quejas ADD COLUMN IF NOT EXISTS apellido2_quejoso varchar(255);

INSERT INTO personal_administrativo
    (id, activo, correo_institucional, cuenta_temporal, fecha_creacion,
     forzar_cambio_password, nombre_completo, numero_empleado, password, rol)
VALUES
    (1, true, 'analista.pc@ipn.mx', false, now(), false,
     'Laura Méndez Ortiz', 'EMP-PC-001', '{noop}no-se-usa-en-local', 'ANALISTA_PRIMER_CONTACTO'),
    (2, true, 'analista2.pc@ipn.mx', false, now(), false,
     'Carlos Ruiz Soto', 'EMP-PC-002', '{noop}no-se-usa-en-local', 'ANALISTA_PRIMER_CONTACTO');

INSERT INTO quejas
    (id, numero_folio, correo_institucional, motivo, descripcion, fecha_creacion,
     nombre_quejoso, apellido1_quejoso, apellido2_quejoso, tipo_identificacion_quejoso,
     unidad_academica_clave, origen_registro, estatus, fecha_turnado, area_turnada)
VALUES
    (1, 'FOL-LOCAL001', 'quejoso1@alumno.ipn.mx', 'Trato discriminatorio',
     'El profesor de la materia de Cálculo realizó comentarios discriminatorios frente al grupo.',
     now() - interval '5 days', 'Ana', 'López', 'García', 'alumno', 'ESCOM', 'AUTENTICADO',
     'TURNADA', now() - interval '1 day', 'Primer Contacto'),
    (2, 'FOL-LOCAL002', 'quejoso2@alumno.ipn.mx', 'Cobro indebido',
     'Se me solicitó un pago no previsto en el reglamento para presentar un examen extraordinario.',
     now() - interval '3 days', 'Luis', 'Hernández', 'Pérez', 'alumno', 'UPIICSA', 'AUTENTICADO',
     'TURNADA', now() - interval '1 day', 'Primer Contacto'),
    (3, 'FOL-LOCAL003', 'quejoso3@ipn.mx', 'Conflicto laboral',
     'Conflicto con un proveedor externo de la cafetería, ajeno a la comunidad politécnica.',
     now() - interval '2 days', 'María', 'Torres', 'Vega', 'empleado', 'ESIME Zacatenco', 'AUTENTICADO',
     'TURNADA', now() - interval '1 day', 'Primer Contacto');

-- Evidencias con contenido real para probar "abrir evidencia" desde Primer Contacto.
INSERT INTO queja_evidencias (id, queja_id, nombre_archivo, tipo_mime, tamanio_bytes, contenido, fecha_subida)
VALUES
    (1, 1, 'captura-mensaje.txt', 'text/plain', 58,
     convert_to('Captura de prueba: mensaje enviado por el profesor al grupo.', 'UTF8'), now() - interval '5 days'),
    (2, 2, 'recibo-pago.txt', 'text/plain', 44,
     convert_to('Recibo de prueba: pago de $850 por extraordinario.', 'UTF8'), now() - interval '3 days');

SELECT setval('personal_administrativo_id_seq', 10, false)
 WHERE EXISTS (SELECT 1 FROM pg_class WHERE relname = 'personal_administrativo_id_seq');
SELECT setval('quejas_id_seq', 100, false)
 WHERE EXISTS (SELECT 1 FROM pg_class WHERE relname = 'quejas_id_seq');
SELECT setval('queja_evidencias_id_seq', 100, false)
 WHERE EXISTS (SELECT 1 FROM pg_class WHERE relname = 'queja_evidencias_id_seq');
