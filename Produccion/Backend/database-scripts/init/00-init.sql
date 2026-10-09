-- Se ejecuta UNA sola vez, cuando el contenedor se crea con la carpeta de datos vacía.
-- Solo configuración de Postgres: las tablas las crea Hibernate (ddl-auto: update) cuando
-- arrancan los microservicios. Los seeds van en database-scripts/seeds/ y se corren después
-- con:  bash podman-compose-bd.sh seed
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
ALTER DATABASE defensoria_db SET timezone TO 'America/Mexico_City';
