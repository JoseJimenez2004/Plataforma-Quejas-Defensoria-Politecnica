FROM docker.io/library/eclipse-temurin:21-jre-jammy
WORKDIR /app

# A diferencia del Dockerfile compartido de los demás microservicios, este SÍ necesita
# pg_dump/psql instalados en la imagen -- los usa RespaldoService para los respaldos
# manuales/automáticos y para restaurar (ver "Seguridad y Respaldos" del panel).
RUN apt-get update \
    && apt-get install -y --no-install-recommends postgresql-client \
    && rm -rf /var/lib/apt/lists/*

ARG JAR_FILE
ARG SERVICE_PORT=8087

COPY ${JAR_FILE} app.jar

# Directorio donde se guardan los .sql de respaldo -- se monta como volumen para que
# sobrevivan a la reconstrucción del contenedor (ver podman-compose.sh).
RUN mkdir -p /app/respaldos

EXPOSE ${SERVICE_PORT}

ENTRYPOINT ["java", "-jar", "app.jar"]
