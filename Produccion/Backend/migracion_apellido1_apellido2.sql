-- =============================================================================
-- Migración: apellido_paterno / apellido_materno  ->  apellido1 / apellido2
-- Fecha: 2026-09-16
--
-- Motivo: el formulario ya le pide al quejoso "Primer Apellido" y "Segundo
-- Apellido"; el modelo de datos decía paterno/materno. Además el denunciado
-- tenía nombres asimétricos (apellido_denunciado sin "paterno"). Con el
-- histórico entrando al sistema hay registros de un solo apellido, donde
-- "materno vacío" se lee como dato faltante y "apellido2 vacío" no.
--
-- RENAME COLUMN en Postgres es solo metadatos: instantáneo, no reescribe la
-- tabla y NO pierde datos. Aun así, respalda antes.
--
-- IMPORTANTE: correr ESTE script ANTES de levantar los contenedores con el
-- código nuevo. Los servicios usan ddl-auto=update, que NO renombra: si
-- levantas primero, Hibernate crea las columnas nuevas VACÍAS y deja los datos
-- atrapados en las viejas, sin avisar.
--
--   psql -h localhost -U postgres -d defensoria_db -f migracion_apellido1_apellido2.sql
-- =============================================================================

BEGIN;

DO $$
DECLARE
    r RECORD;
    renombradas INT := 0;
    cambios CONSTANT TEXT[][] := ARRAY[
        ['quejas',        'apellido_paterno_quejoso',    'apellido1_quejoso'],
        ['quejas',        'apellido_materno_quejoso',    'apellido2_quejoso'],
        ['quejas',        'apellido_denunciado',         'apellido1_denunciado'],
        ['quejas',        'apellido_materno_denunciado', 'apellido2_denunciado'],
        ['queja_tutores', 'apellido_paterno',            'apellido1'],
        ['queja_tutores', 'apellido_materno',            'apellido2']
    ];
    i INT;
BEGIN
    FOR i IN 1 .. array_length(cambios, 1) LOOP

        -- ¿ya está renombrada? (script idempotente: se puede correr dos veces)
        IF EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = 'public'
                     AND table_name  = cambios[i][1]
                     AND column_name = cambios[i][3]) THEN
            RAISE NOTICE 'OMITIDA  %.% -> % (ya existe el nombre nuevo)',
                cambios[i][1], cambios[i][2], cambios[i][3];

        -- ¿existe la columna vieja?
        ELSIF EXISTS (SELECT 1 FROM information_schema.columns
                      WHERE table_schema = 'public'
                        AND table_name  = cambios[i][1]
                        AND column_name = cambios[i][2]) THEN
            EXECUTE format('ALTER TABLE public.%I RENAME COLUMN %I TO %I',
                           cambios[i][1], cambios[i][2], cambios[i][3]);
            renombradas := renombradas + 1;
            RAISE NOTICE 'RENOMBRADA  %.% -> %',
                cambios[i][1], cambios[i][2], cambios[i][3];

        -- no existe ninguna de las dos: normal para apellido_materno_denunciado,
        -- que Hibernate pudo no haber creado todavía. Se crea para que el código
        -- nuevo tenga dónde escribir.
        ELSE
            EXECUTE format('ALTER TABLE public.%I ADD COLUMN %I varchar(255)',
                           cambios[i][1], cambios[i][3]);
            RAISE NOTICE 'CREADA      %.% (no existía la columna vieja %)',
                cambios[i][1], cambios[i][3], cambios[i][2];
        END IF;

    END LOOP;

    RAISE NOTICE '--- Columnas renombradas: % ---', renombradas;
END $$;

COMMIT;

-- Verificación: debe devolver exactamente 6 renglones, todos con apellido1/apellido2.
SELECT table_name, column_name, is_nullable
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name IN ('quejas', 'queja_tutores')
  AND column_name LIKE 'apellido%'
ORDER BY table_name, column_name;
