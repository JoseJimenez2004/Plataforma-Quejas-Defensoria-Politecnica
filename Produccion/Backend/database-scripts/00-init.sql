-- Script de inicialización para defensoria_db
-- Se ejecuta automáticamente cuando el contenedor se levanta por primera vez
-- Este script se ejecuta ANTES de los seeds individuales

-- Extensiones necesarias
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Configuración de timezone
SET timezone = 'America/Mexico_City';

-- Nota importante:
-- Las tablas se crean automáticamente por Hibernate (ddl-auto=update)
-- cuando los microservicios se conectan por primera vez.
-- Este script es solo para configuraciones adicionales de Postgres.
-- 
-- Los seeds (dependencias_seed.sql, chatbot_seed.sql) se ejecutan
-- automáticamente después de este script porque están en el mismo
-- directorio /docker-entrypoint-initdb.d que se monta como volumen.
