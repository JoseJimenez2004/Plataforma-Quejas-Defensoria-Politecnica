FROM docker.io/library/eclipse-temurin:21-jre-jammy
WORKDIR /app

# A diferencia del Dockerfile compartido de los demás microservicios, este SÍ necesita
# pg_dump/psql instalados en la imagen -- los usa RespaldoService para los respaldos
# manuales/automáticos y para restaurar (ver "Seguridad y Respaldos" del panel).
#
# Cliente 16 desde el repositorio oficial de PostgreSQL (PGDG): el de Ubuntu jammy es la
# versión 14, y pg_dump se niega a respaldar un servidor más nuevo ("server version mismatch").
# El servidor de BD corre postgres:16, así que el cliente tiene que ser 16 o mayor.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates \
    && install -d /usr/share/postgresql-common/pgdg \
    && curl -fsSL -o /usr/share/postgresql-common/pgdg/apt.postgresql.org.asc https://www.postgresql.org/media/keys/ACCC4CF8.asc \
    && echo "deb [signed-by=/usr/share/postgresql-common/pgdg/apt.postgresql.org.asc] https://apt.postgresql.org/pub/repos/apt jammy-pgdg main" \
       > /etc/apt/sources.list.d/pgdg.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends postgresql-client-16 \
    && rm -rf /var/lib/apt/lists/* \
    && pg_dump --version

ARG JAR_FILE
ARG SERVICE_PORT=8087

COPY ${JAR_FILE} app.jar

# Directorio donde se guardan los .sql de respaldo -- se monta como volumen para que
# sobrevivan a la reconstrucción del contenedor (ver podman-compose.sh).
RUN mkdir -p /app/respaldos

EXPOSE ${SERVICE_PORT}

ENTRYPOINT ["java", "-jar", "app.jar"]
