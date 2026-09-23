-- =============================================================================
-- defensoria_historico_db — quejas de años anteriores al lanzamiento
-- Fecha: 2026-09-17
--
-- Base SEPARADA de defensoria_db, en la misma instancia Postgres de la VPS
-- 2.25.78.22. Guarda las quejas que la Defensoría atendió antes del sistema
-- (Excel, llamadas, oficios físicos, sistemas anteriores) y que el recepcionista
-- captura a mano. Son de SOLO CONSULTA: no entran al flujo de validación,
-- turnado ni dictamen. Su único propósito es la búsqueda de antecedentes.
--
-- Se ejecuta en DOS pasos porque CREATE DATABASE no corre dentro de una
-- transacción ni desde otra base:
--
--   psql -h localhost -U postgres -c "CREATE DATABASE defensoria_historico_db;"
--   psql -h localhost -U postgres -d defensoria_historico_db -f crear_historico_db.sql
-- =============================================================================

BEGIN;

-- ---------------------------------------------------------------- quejas
CREATE TABLE IF NOT EXISTS quejas_historicas (
    id                          BIGSERIAL PRIMARY KEY,

    -- El folio ES el del expediente original (DDP/2019/0087, 2019-087, lo que
    -- traiga). No se normaliza: es el número con el que la Defensoría lo ha
    -- buscado siempre. Por eso 80 caracteres y no el formato FOL- de 12.
    folio                       VARCHAR(80)  NOT NULL,

    -- true cuando el expediente NO tenía folio y el sistema generó uno (SF-nnn).
    -- Le permite al recepcionista y al modelo distinguir un folio real de uno
    -- inventado para poder guardar el registro.
    folio_generado              BOOLEAN      NOT NULL DEFAULT false,

    fuente                      VARCHAR(30)  NOT NULL,

    -- Fecha en que la queja se presentó ANTE LA DEFENSORÍA en su momento.
    -- Es la que se expone como "fechaRegistro" en el contrato: si se expusiera
    -- la fecha de captura, todos los históricos parecerían de 2026 y el orden
    -- temporal de los antecedentes quedaría inservible.
    fecha_presentacion_original DATE,
    fecha_hechos                DATE,

    unidad_academica_clave      VARCHAR(20),
    motivo                      VARCHAR(255),
    descripcion                 TEXT,

    estatus                     VARCHAR(40)  NOT NULL DEFAULT 'CONCLUIDA',
    resultado                   VARCHAR(40)  NOT NULL DEFAULT 'SIN_DATO',

    -- quejoso
    quejoso_tipo_identificacion   VARCHAR(20),
    quejoso_numero_identificacion VARCHAR(50),
    quejoso_nombre                VARCHAR(150),
    quejoso_apellido1             VARCHAR(150),
    quejoso_apellido2             VARCHAR(150),
    quejoso_tipo_usuario          VARCHAR(20),

    -- denunciado
    denunciado_tipo_identificacion   VARCHAR(20),
    denunciado_numero_identificacion VARCHAR(50),
    denunciado_nombre                VARCHAR(150),
    denunciado_apellido1             VARCHAR(150),
    denunciado_apellido2             VARCHAR(150),
    denunciado_tipo_usuario          VARCHAR(20),

    -- trazabilidad de la captura (interna, NO va en el contrato del modelo)
    capturado_por               VARCHAR(150),
    fecha_captura               TIMESTAMP    NOT NULL DEFAULT now(),
    notas_captura               TEXT,

    CONSTRAINT uq_quejas_historicas_folio UNIQUE (folio),

    CONSTRAINT ck_qh_fuente CHECK (fuente IN
        ('EXCEL','LLAMADA','OFICIO_FISICO','SISTEMA_ANTERIOR','OTRO')),
    CONSTRAINT ck_qh_estatus CHECK (estatus IN
        ('RECHAZADA','IMPROCEDENTE','REMITIDA','CONCLUIDA')),
    CONSTRAINT ck_qh_resultado CHECK (resultado IN
        ('RECHAZADA_EN_RECEPCION','IMPROCEDENTE','REMITIDA_A_OTRA_AUTORIDAD',
         'CONCLUIDA_CON_ACUERDO','CONCLUIDA_SIN_ACUERDO','SIN_DATO')),
    CONSTRAINT ck_qh_tipo_id_quejoso CHECK (quejoso_tipo_identificacion IS NULL
        OR quejoso_tipo_identificacion IN ('BOLETA','EMPLEADO','CURP','INE','OTRO')),
    CONSTRAINT ck_qh_tipo_id_denunciado CHECK (denunciado_tipo_identificacion IS NULL
        OR denunciado_tipo_identificacion IN ('BOLETA','EMPLEADO','CURP','INE','OTRO')),
    -- Los 3 valores que realmente ofrece el sistema (selector de registro-manual).
    -- No hay DOCENTE ni ADMINISTRATIVO por separado: ambos son EMPLEADO.
    CONSTRAINT ck_qh_tipo_usr_quejoso CHECK (quejoso_tipo_usuario IS NULL
        OR quejoso_tipo_usuario IN ('ALUMNO','EMPLEADO','EXTERNO')),
    CONSTRAINT ck_qh_tipo_usr_denunciado CHECK (denunciado_tipo_usuario IS NULL
        OR denunciado_tipo_usuario IN ('ALUMNO','EMPLEADO','EXTERNO'))
);

-- ------------------------------------------------------------- evidencias
-- Mismo criterio que queja_evidencias en defensoria_db: el binario vive en
-- Postgres como bytea (no @Lob/oid), para que el respaldo se lleve todo.
CREATE TABLE IF NOT EXISTS quejas_historicas_evidencias (
    id                  BIGSERIAL PRIMARY KEY,
    queja_historica_id  BIGINT      NOT NULL,
    nombre_archivo      VARCHAR(255) NOT NULL,
    tipo_mime           VARCHAR(100),
    tipo                VARCHAR(20),
    tamanio_bytes       BIGINT,
    contenido           BYTEA       NOT NULL,
    fecha_subida        TIMESTAMP   NOT NULL DEFAULT now(),

    CONSTRAINT fk_qh_evidencia_queja FOREIGN KEY (queja_historica_id)
        REFERENCES quejas_historicas (id) ON DELETE CASCADE
);

-- ------------------------------------------------------------------ índices
-- El de antecedentes por identificación es EL índice: es la consulta que
-- justifica que esta base exista.
CREATE INDEX IF NOT EXISTS ix_qh_quejoso_identificacion
    ON quejas_historicas (quejoso_numero_identificacion);

CREATE INDEX IF NOT EXISTS ix_qh_denunciado_identificacion
    ON quejas_historicas (denunciado_numero_identificacion);

-- Búsqueda por nombre cuando no hay boleta/empleado (frecuente en registros
-- viejos). Funcional en minúsculas y sin acentos no — se normaliza en el
-- servicio antes de guardar y de consultar.
CREATE INDEX IF NOT EXISTS ix_qh_quejoso_apellido1
    ON quejas_historicas (lower(quejoso_apellido1));

CREATE INDEX IF NOT EXISTS ix_qh_unidad
    ON quejas_historicas (unidad_academica_clave);

CREATE INDEX IF NOT EXISTS ix_qh_fecha_presentacion
    ON quejas_historicas (fecha_presentacion_original DESC);

CREATE INDEX IF NOT EXISTS ix_qh_evidencia_queja
    ON quejas_historicas_evidencias (queja_historica_id);

COMMIT;

-- Verificación
SELECT table_name, count(*) AS columnas
FROM information_schema.columns
WHERE table_schema = 'public'
GROUP BY table_name
ORDER BY table_name;
