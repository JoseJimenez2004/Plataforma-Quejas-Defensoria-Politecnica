# Scripts de base de datos

- `init/` → se monta en `/docker-entrypoint-initdb.d` de `defensoria-db` y corre **una sola vez**,
  cuando el contenedor se crea con la carpeta de datos vacía. Solo configuración (sin INSERT:
  las tablas todavía no existen en ese momento).
- `seeds/` → datos iniciales. Se corren con `bash podman-compose-bd.sh seed` **después** de
  levantar los microservicios (Hibernate crea las tablas). El orden lo da el prefijo numérico.

Guía completa: `docs/VERSION-FINAL-TT.md`.
