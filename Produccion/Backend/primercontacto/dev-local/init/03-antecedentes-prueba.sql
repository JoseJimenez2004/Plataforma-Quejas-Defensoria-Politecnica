-- =============================================================================
-- Historial de quejas para probar la búsqueda de antecedentes (solo desarrollo).
--
--   * Ana López (quejoso1@alumno.ipn.mx) ya había presentado dos quejas antes: una
--     parecida (comentarios discriminatorios) y otra sin relación.
--   * Otras personas de ESCOM denunciaron hechos similares del mismo profesor.
--   * Una queja de otra escuela y otro tema, que NO debe salir como antecedente.
-- =============================================================================

INSERT INTO quejas
    (id, numero_folio, correo_institucional, motivo, descripcion, fecha_creacion,
     nombre_quejoso, apellido1_quejoso, unidad_academica_clave, origen_registro, estatus)
VALUES
    (20, 'FOL-HIST0020', 'quejoso1@alumno.ipn.mx', 'Trato discriminatorio',
     'El profesor de Cálculo hizo comentarios discriminatorios sobre mi origen frente al grupo durante la clase.',
     now() - interval '240 days', 'Ana', 'López', 'ESCOM', 'AUTENTICADO', 'TURNADA'),
    (21, 'FOL-HIST0021', 'quejoso1@alumno.ipn.mx', 'Problema con credencial',
     'No me entregaron la credencial de estudiante en el plazo indicado por servicios escolares.',
     now() - interval '400 days', 'Ana', 'López', 'ESCOM', 'AUTENTICADO', 'RECHAZADA'),
    (22, 'FOL-HIST0022', 'otro.alumno@alumno.ipn.mx', 'Comentarios ofensivos',
     'El profesor de Cálculo del grupo 2CM3 realiza comentarios discriminatorios y ofensivos a varios alumnos en clase.',
     now() - interval '120 days', 'Jorge', 'Ramírez', 'ESCOM', 'AUTENTICADO', 'TURNADA'),
    (23, 'FOL-HIST0023', 'tercera@alumno.ipn.mx', 'Mensajes ofensivos en chat',
     'Compañeros difunden mensajes ofensivos en el chat del grupo y en redes sociales.',
     now() - interval '60 days', 'Sofía', 'Méndez', 'ESCOM', 'AUTENTICADO', 'EN_VALIDACION'),
    (24, 'FOL-HIST0024', 'ajeno@ipn.mx', 'Cobro de estacionamiento',
     'Se cobra una cuota de estacionamiento que no aparece en el reglamento vigente.',
     now() - interval '30 days', 'Pedro', 'Salas', 'UPIICSA', 'AUTENTICADO', 'RECIBIDA');
