-- =============================================================================
-- Migración CU-Q01 — Validaciones del formulario público de quejas
-- Fecha: 2026-09-08
-- Base:  defensoria_db (PostgreSQL 16)
--
-- Ejecutar UNA sola vez, ANTES de desplegar el nuevo quejas-service.jar:
--   psql -U postgres -d defensoria_db -f migracion_cu_q01_validaciones.sql
--
-- Es idempotente: se puede volver a correr sin efectos secundarios.
--
-- NOTA sobre los CHECK: se crean como NOT VALID a propósito. Eso hace que la
-- restricción aplique a TODO registro nuevo o actualizado, pero NO intente
-- validar las filas que ya existen (que sí tienen datos sucios de las pruebas
-- previas). Sin NOT VALID, el ALTER TABLE fallaría con esos registros.
-- Cuando la tabla esté limpia se pueden promover con:
--   ALTER TABLE public.quejas VALIDATE CONSTRAINT ck_quejas_nombre_quejoso;
-- =============================================================================

-- El script contiene acentos y ñ dentro de los regex de las restricciones CHECK.
-- Forzar UTF8 evita que se guarden mal si la sesión de psql abre con otra codificación.
\encoding UTF8
SET client_encoding TO 'UTF8';

BEGIN;

-- -----------------------------------------------------------------------------
-- 1. Columnas nuevas
-- -----------------------------------------------------------------------------

-- H-01.8: segundo apellido del denunciado (nullable: el quejoso puede no conocerlo)
ALTER TABLE public.quejas
    ADD COLUMN IF NOT EXISTS apellido_materno_denunciado character varying(255);

-- H-01.12: constancia de aceptación del aviso de privacidad
ALTER TABLE public.quejas
    ADD COLUMN IF NOT EXISTS aviso_privacidad_aceptado boolean;
ALTER TABLE public.quejas
    ADD COLUMN IF NOT EXISTS aviso_privacidad_fecha timestamp(6) without time zone;
ALTER TABLE public.quejas
    ADD COLUMN IF NOT EXISTS aviso_privacidad_version character varying(20);

-- H-01.5: distinguir la credencial oficial del resto de evidencias. Antes solo se
-- diferenciaban por el prefijo "IDENTIFICACION_" en el nombre del archivo.
-- Valores: 'IDENTIFICACION' | 'EVIDENCIA' (NULL en las filas anteriores a este cambio).
ALTER TABLE public.queja_evidencias
    ADD COLUMN IF NOT EXISTS tipo character varying(20);

-- Backfill del tipo para las evidencias ya cargadas, usando el prefijo del nombre.
UPDATE public.queja_evidencias
   SET tipo = CASE
                WHEN nombre_archivo LIKE 'IDENTIFICACION\_%' THEN 'IDENTIFICACION'
                ELSE 'EVIDENCIA'
              END
 WHERE tipo IS NULL;

-- -----------------------------------------------------------------------------
-- 2. Restricciones de formato en los datos del quejoso
-- -----------------------------------------------------------------------------
-- Regex de nombre: letras (con acentos y ñ), permitiendo espacio interno,
-- apóstrofe y guion entre palabras — "María José", "D'Angelo", "Pérez-Gómez".

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_quejas_nombre_quejoso') THEN
        ALTER TABLE public.quejas ADD CONSTRAINT ck_quejas_nombre_quejoso
            CHECK (nombre_quejoso IS NULL OR
                   nombre_quejoso ~ '^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+([ ''-][A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+)*$')
            NOT VALID;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_quejas_apellido_paterno_quejoso') THEN
        ALTER TABLE public.quejas ADD CONSTRAINT ck_quejas_apellido_paterno_quejoso
            CHECK (apellido_paterno_quejoso IS NULL OR
                   apellido_paterno_quejoso ~ '^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+([ ''-][A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+)*$')
            NOT VALID;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_quejas_apellido_materno_quejoso') THEN
        ALTER TABLE public.quejas ADD CONSTRAINT ck_quejas_apellido_materno_quejoso
            CHECK (apellido_materno_quejoso IS NULL OR
                   apellido_materno_quejoso ~ '^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+([ ''-][A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+)*$')
            NOT VALID;
    END IF;

    -- H-01.7 / H-01.8: mismos formatos para el denunciado
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_quejas_nombre_denunciado') THEN
        ALTER TABLE public.quejas ADD CONSTRAINT ck_quejas_nombre_denunciado
            CHECK (nombre_denunciado IS NULL OR
                   nombre_denunciado ~ '^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+([ ''-][A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+)*$')
            NOT VALID;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_quejas_apellido_denunciado') THEN
        ALTER TABLE public.quejas ADD CONSTRAINT ck_quejas_apellido_denunciado
            CHECK (apellido_denunciado IS NULL OR
                   apellido_denunciado ~ '^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+([ ''-][A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+)*$')
            NOT VALID;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_quejas_apellido_materno_denunciado') THEN
        ALTER TABLE public.quejas ADD CONSTRAINT ck_quejas_apellido_materno_denunciado
            CHECK (apellido_materno_denunciado IS NULL OR
                   apellido_materno_denunciado ~ '^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+([ ''-][A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+)*$')
            NOT VALID;
    END IF;

    -- H-01.4: boleta / número de empleado — solo dígitos, máximo 10
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_quejas_numero_identificacion') THEN
        ALTER TABLE public.quejas ADD CONSTRAINT ck_quejas_numero_identificacion
            CHECK (numero_identificacion_quejoso IS NULL OR
                   numero_identificacion_quejoso ~ '^[0-9]{1,10}$')
            NOT VALID;
    END IF;

    -- H-01.3: fecha de nacimiento no anterior a 1920.
    -- El límite superior (no puede ser del año en curso) NO se puede poner aquí:
    -- CURRENT_DATE no es inmutable y Postgres rechaza funciones volátiles en un CHECK.
    -- Ese tope lo aplica el backend (ValidadorFechas), que es la fuente de verdad.
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_quejas_fecha_nacimiento_min') THEN
        ALTER TABLE public.quejas ADD CONSTRAINT ck_quejas_fecha_nacimiento_min
            CHECK (fecha_nacimiento_quejoso IS NULL OR fecha_nacimiento_quejoso >= DATE '1920-01-01')
            NOT VALID;
    END IF;

    -- H-01.5: ningún archivo puede exceder 30 MB (tope global de multipart).
    -- El tope estricto de 3 MB para la credencial se aplica en el backend, porque
    -- depende del tipo de archivo.
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_evidencias_tamanio') THEN
        ALTER TABLE public.queja_evidencias ADD CONSTRAINT ck_evidencias_tamanio
            CHECK (tamanio_bytes IS NULL OR tamanio_bytes <= 31457280)
            NOT VALID;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_evidencias_tipo') THEN
        ALTER TABLE public.queja_evidencias ADD CONSTRAINT ck_evidencias_tipo
            CHECK (tipo IS NULL OR tipo IN ('IDENTIFICACION', 'EVIDENCIA'))
            NOT VALID;
    END IF;
END $$;

COMMIT;

-- -----------------------------------------------------------------------------
-- 3. Verificación
-- -----------------------------------------------------------------------------
\echo '--- Columnas nuevas en quejas ---'
SELECT column_name, data_type
  FROM information_schema.columns
 WHERE table_name = 'quejas'
   AND column_name IN ('apellido_materno_denunciado','aviso_privacidad_aceptado',
                       'aviso_privacidad_fecha','aviso_privacidad_version')
 ORDER BY column_name;

\echo '--- Columna nueva en queja_evidencias ---'
SELECT column_name, data_type
  FROM information_schema.columns
 WHERE table_name = 'queja_evidencias' AND column_name = 'tipo';

\echo '--- Restricciones creadas ---'
SELECT conname, convalidated
  FROM pg_constraint
 WHERE conname LIKE 'ck_quejas_%' OR conname LIKE 'ck_evidencias_%'
 ORDER BY conname;

\echo '--- Filas existentes que NO cumplirian las nuevas reglas (informativo) ---'
SELECT count(*) AS quejas_con_datos_invalidos
  FROM public.quejas
 WHERE (nombre_quejoso IS NOT NULL AND nombre_quejoso !~ '^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+([ ''-][A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+)*$')
    OR (numero_identificacion_quejoso IS NOT NULL AND numero_identificacion_quejoso !~ '^[0-9]{1,10}$');
