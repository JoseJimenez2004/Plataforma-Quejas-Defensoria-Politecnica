FROM docker.io/library/eclipse-temurin:21-jre-jammy
WORKDIR /app

# Definimos una variable para recibir la ruta del JAR al momento del build
ARG JAR_FILE

# Copiamos el JAR específico usando la variable
COPY ${JAR_FILE} app.jar

# El puerto se mapeará dinámicamente desde Podman, pero abrimos el estándar
EXPOSE 8080 

ENTRYPOINT ["java", "-jar", "app.jar"]
