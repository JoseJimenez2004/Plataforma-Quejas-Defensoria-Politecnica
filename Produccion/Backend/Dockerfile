FROM docker.io/library/eclipse-temurin:21-jre-jammy
WORKDIR /app

# Definimos una variable para recibir la ruta del JAR al momento del build
ARG JAR_FILE

# Puerto real de este microservicio (8083 auth / 8084 quejas / 8085 notificaciones).
# Spring Boot decide el puerto real leyendo server.port de su config de producción
# (config-files/<servicio>/config/*.yml); este ARG solo documenta correctamente el
# puerto en la metadata de la imagen (antes decía "8080" fijo, que no reflejaba la realidad).
ARG SERVICE_PORT=8080

# Copiamos el JAR específico usando la variable
COPY ${JAR_FILE} app.jar

EXPOSE ${SERVICE_PORT}

ENTRYPOINT ["java", "-jar", "app.jar"]
