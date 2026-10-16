-- =============================================================================
-- Datos para probar la BÚSQUEDA MANUAL de antecedentes (solo desarrollo).
--
-- Se puede correr sobre una base local que ya existe (todo es idempotente):
--   docker exec -i defensoria-db-local psql -U postgres -d defensoria_db < dev-local/init/04-busqueda-manual-prueba.sql
--
--   * Denunciado en las quejas del sistema: el profesor "Roberto Gómez Hernández" aparece
--     en FOL-HIST0020 y FOL-HIST0022.
--   * Base historico_db (copia mínima de la tabla de historico-service) con casos viejos:
--     dos contra el mismo profesor, uno presentado por Ana López y uno sin relación.
-- =============================================================================

-- Columnas que el esquema actual de queja-service ya tiene y el dump de agosto no.
ALTER TABLE quejas ADD COLUMN IF NOT EXISTS apellido1_quejoso varchar(255);
ALTER TABLE quejas ADD COLUMN IF NOT EXISTS apellido2_quejoso varchar(255);
ALTER TABLE quejas ADD COLUMN IF NOT EXISTS apellido1_denunciado varchar(255);
ALTER TABLE quejas ADD COLUMN IF NOT EXISTS apellido2_denunciado varchar(255);

UPDATE quejas SET nombre_denunciado = 'Roberto', apellido1_denunciado = 'Gómez',
                  apellido2_denunciado = 'Hernández'
 WHERE numero_folio IN ('FOL-HIST0020', 'FOL-HIST0022');

UPDATE quejas SET nombre_denunciado = 'Laura', apellido1_denunciado = 'Ortiz'
 WHERE numero_folio = 'FOL-HIST0023';

-- Base de casos históricos.
SELECT 'CREATE DATABASE historico_db'
 WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'historico_db') \gexec

\connect historico_db

CREATE TABLE IF NOT EXISTS quejas_historicas (
    id bigserial PRIMARY KEY,
    folio varchar(60) NOT NULL UNIQUE,
    folio_generado boolean NOT NULL DEFAULT false,
    fuente varchar(50),
    fecha_presentacion_original date,
    fecha_hechos date,
    unidad_academica_clave varchar(20),
    motivo varchar(500),
    descripcion text,
    estatus varchar(50) DEFAULT 'CONCLUIDA',
    resultado varchar(50) DEFAULT 'SIN_DATO',
    quejoso_tipo_identificacion varchar(20),
    quejoso_numero_identificacion varchar(50),
    quejoso_nombre varchar(150),
    quejoso_apellido1 varchar(150),
    quejoso_apellido2 varchar(150),
    quejoso_tipo_usuario varchar(20),
    denunciado_tipo_identificacion varchar(20),
    denunciado_numero_identificacion varchar(50),
    denunciado_nombre varchar(150),
    denunciado_apellido1 varchar(150),
    denunciado_apellido2 varchar(150),
    denunciado_tipo_usuario varchar(20),
    capturado_por varchar(150),
    fecha_captura timestamp NOT NULL DEFAULT now(),
    notas_captura text
);

INSERT INTO quejas_historicas
    (folio, fuente, fecha_presentacion_original, unidad_academica_clave, motivo, descripcion,
     estatus, resultado, quejoso_nombre, quejoso_apellido1, quejoso_apellido2,
     denunciado_nombre, denunciado_apellido1, denunciado_apellido2)
VALUES
    ('HIST-2019-0101', 'ARCHIVO', '2019-03-14', 'ESCOM', 'Humillación en clase',
     'El profesor Roberto Gómez humilló a varios alumnos frente al grupo al entregar calificaciones. Gritó que eran unos inútiles y amenazó con reprobarlos si se quejaban.',
     'CONCLUIDA', 'CONCILIADA', 'Mariana', 'Torres', 'Vega', 'Roberto', 'Gómez', 'Hernández'),
    ('HIST-2021-0457', 'ARCHIVO', '2021-09-02', 'ESCOM', 'Comentarios discriminatorios',
     'Durante la clase de Cálculo el docente hizo comentarios discriminatorios por el lugar de origen de un alumno. Varios compañeros fueron testigos.',
     'CONCLUIDA', 'RECOMENDACION', 'Luis', 'Pérez', NULL, 'Roberto', 'Gómez', 'Hernández'),
    ('HIST-2022-0033', 'ARCHIVO', '2022-02-20', 'ESCOM', 'Retraso en trámite',
     'Mi trámite de constancia tardó más de dos meses sin ninguna respuesta de la ventanilla.',
     'CONCLUIDA', 'SIN_DATO', 'Ana', 'López', 'Ruiz', NULL, NULL, NULL),
    ('HIST-2020-0210', 'ARCHIVO', '2020-11-05', 'UPIICSA', 'Cobro indebido',
     'Se cobró una cuota de laboratorio que no aparece en el reglamento.',
     'CONCLUIDA', 'IMPROCEDENTE', 'Pedro', 'Salas', NULL, NULL, NULL, NULL)
ON CONFLICT (folio) DO NOTHING;
