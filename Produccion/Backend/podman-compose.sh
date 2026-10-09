#!/bin/bash

# ==============================================================================
# Orquestador de Microservicios - Plataforma Defensoria
# ==============================================================================

BASE_DIR="/apps/aplicaciones/defensoria/back"
RESPALDOS_DIR="/apps/utiles/respaldos"
# historico-service queda FUERA del "up" general (su base la está poblando otra persona). Se
# puede levantar a mano cuando esté lista: bash podman-compose.sh up-container historico-service
SERVICIOS=("auth-service" "quejas-service" "notificaciones-service" "catalogo-service" "admin-service" "revision-service" "chatbot-service" "primer-contacto-service" "subdefensoria-service" "denunciado-service")

# Mostrar menu de ayuda
mostrar_ayuda() {
    echo "Uso: sudo bash podman-compose.sh [COMANDO] [SERVICIO]"
    echo ""
    echo "Comandos disponibles:"
    echo "  up                      Construye y levanta TODOS los microservicios."
    echo "  up-container <srv>      Construye y levanta UN microservicio especifico."
    echo "  delete                  Detiene y elimina TODOS los microservicios."
    echo "  delete-container <srv>  Detiene y elimina UN microservicio especifico."
    echo ""
    echo "Servicios validos: auth-service, quejas-service, notificaciones-service, catalogo-service, admin-service, revision-service, chatbot-service, primer-contacto-service, subdefensoria-service, historico-service, denunciado-service"
}

# Construir una imagen dedicada por microservicio
build_service() {
    SERVICE=$1
    echo "Construyendo/Actualizando imagen defensoria-${SERVICE}..."
    cd $BASE_DIR

    if [ ! -f "artifact/${SERVICE}.jar" ]; then
        echo "Error: No se encontro el archivo artifact/${SERVICE}.jar"
        exit 1
    fi

    DOCKERFILE="Dockerfile"
    if [ -f "${SERVICE}/Dockerfile" ]; then
        DOCKERFILE="${SERVICE}/Dockerfile"
    fi

    sudo podman build -q \
      -f "$DOCKERFILE" \
      --build-arg JAR_FILE=artifact/${SERVICE}.jar \
      --build-arg SERVICE_PORT=8080 \
      -t "defensoria-${SERVICE}" .
    echo "Imagen defensoria-${SERVICE} actualizada exitosamente."
}

# Levantar contenedor con comandos explícitos por servicio
start_service() {
    SERVICE=$1
    echo "Levantando contenedor para $SERVICE..."

    case "$SERVICE" in
        "auth-service")
            sudo podman run -q -d \
              --restart always \
              --name auth-service \
              -p 8083:8080 \
              -m 512m \
              -e JAVA_TOOL_OPTIONS="-Xmx320m -Xms64m -XX:+ExitOnOutOfMemoryError" \
              -v $BASE_DIR/config-files/auth-service/config:/app/config:Z \
              -e SPRING_CONFIG_ADDITIONAL_LOCATION=optional:file:/app/config/ \
              -e SPRING_CONFIG_NAME=auth-service \
              -e QUEJAS_SERVICE_URL="http://156.67.26.73:8084" \
              localhost/defensoria-auth-service
            ;;
            
        "quejas-service")
            # Recibe las evidencias (hasta 60MB por petición) y las guarda como BYTEA: Java y el
            # driver de Postgres hacen varias copias en memoria. Con 250m el kernel mataba el
            # contenedor al primer envío con un PDF de ~12MB (502 en el navegador, log cortado
            # sin error). Por eso tiene más memoria que los demás: 1g aguanta el máximo de 100MB
            # por petición que permite quejas-service.yml.
            sudo podman run -q -d \
              --restart always \
              --name quejas-service \
              -p 8084:8080 \
              -m 1g \
              -e JAVA_TOOL_OPTIONS="-Xmx640m -Xms64m -XX:+ExitOnOutOfMemoryError" \
              -v $BASE_DIR/config-files/quejas-service/config:/app/config:Z \
              -e SPRING_CONFIG_ADDITIONAL_LOCATION=optional:file:/app/config/ \
              -e SPRING_CONFIG_NAME=quejas-service \
              -e QUEJAS_SERVICE_URL="http://156.67.26.73:8084" \
              localhost/defensoria-quejas-service
            ;;

        "notificaciones-service")
            sudo podman run -q -d \
              --restart always \
              --name notificaciones-service \
              -p 8085:8080 \
              -m 512m \
              -e JAVA_TOOL_OPTIONS="-Xmx320m -Xms64m -XX:+ExitOnOutOfMemoryError" \
              -v $BASE_DIR/config-files/notificaciones-service/config:/app/config:Z \
              -e SPRING_CONFIG_ADDITIONAL_LOCATION=optional:file:/app/config/ \
              -e SPRING_CONFIG_NAME=notificaciones-service \
              -e QUEJAS_SERVICE_URL="http://156.67.26.73:8084" \
              localhost/defensoria-notificaciones-service
            ;;

        "catalogo-service")
            sudo podman run -q -d \
              --restart always \
              --name catalogo-service \
              -p 8086:8080 \
              -m 512m \
              -e JAVA_TOOL_OPTIONS="-Xmx320m -Xms64m -XX:+ExitOnOutOfMemoryError" \
              -v $BASE_DIR/config-files/catalogo-service/config:/app/config:Z \
              -e SPRING_CONFIG_ADDITIONAL_LOCATION=optional:file:/app/config/ \
              -e SPRING_CONFIG_NAME=catalogo-service \
              -e QUEJAS_SERVICE_URL="http://156.67.26.73:8084" \
              localhost/defensoria-catalogo-service
            ;;

        "admin-service")
            mkdir -p "$RESPALDOS_DIR"
            sudo podman run -q -d \
              --restart always \
              --name admin-service \
              -p 8087:8080 \
              -m 512m \
              -e JAVA_TOOL_OPTIONS="-Xmx320m -Xms64m -XX:+ExitOnOutOfMemoryError" \
              -v $BASE_DIR/config-files/admin-service/config:/app/config:Z \
              -v $RESPALDOS_DIR:/app/respaldos:Z \
              -e SPRING_CONFIG_ADDITIONAL_LOCATION=optional:file:/app/config/ \
              -e SPRING_CONFIG_NAME=admin-service \
              -e QUEJAS_SERVICE_URL="http://156.67.26.73:8084" \
              localhost/defensoria-admin-service
            ;;

        "revision-service")
            # Antes 100m/-Xmx70m: con el jar del 2026-10-07 el contenedor moría al arrancar
            # (cgroup OOM, sin error de Java en el log).
            sudo podman run -q -d \
              --restart always \
              --name revision-service \
              -p 8088:8080 \
              -m 512m \
              -e JAVA_TOOL_OPTIONS="-Xmx320m -Xms64m -XX:+ExitOnOutOfMemoryError" \
              -v $BASE_DIR/config-files/revision-service/config:/app/config:Z \
              -e SPRING_CONFIG_ADDITIONAL_LOCATION=optional:file:/app/config/ \
              -e SPRING_CONFIG_NAME=revision-service \
              -e QUEJAS_SERVICE_URL="http://156.67.26.73:8084" \
              localhost/defensoria-revision-service
            ;;

        "chatbot-service")
            sudo podman run -q -d \
              --restart always \
              --name chatbot-service \
              -p 8089:8080 \
              -m 512m \
              -e JAVA_TOOL_OPTIONS="-Xmx320m -Xms64m -XX:+ExitOnOutOfMemoryError" \
              -v $BASE_DIR/config-files/chatbot-service/config:/app/config:Z \
              -e SPRING_CONFIG_ADDITIONAL_LOCATION=optional:file:/app/config/ \
              -e SPRING_CONFIG_NAME=chatbot-service \
              -e QUEJAS_SERVICE_URL="http://156.67.26.73:8084" \
              localhost/defensoria-chatbot-service
            ;;

        "primer-contacto-service")
            sudo podman run -q -d \
              --restart always \
              --name primer-contacto-service \
              -p 8082:8080 \
              -m 250m \
              -e JAVA_TOOL_OPTIONS="-Xmx150m -Xms64m" \
              -v $BASE_DIR/config-files/primer-contacto-service/config:/app/config:Z \
              -e SPRING_CONFIG_ADDITIONAL_LOCATION=optional:file:/app/config/ \
              -e SPRING_CONFIG_NAME=primer-contacto-service \
              -e QUEJAS_SERVICE_URL="http://156.67.26.73:8084" \
              localhost/defensoria-primer-contacto-service
            ;;

        "subdefensoria-service")
            sudo podman run -q -d \
              --restart always \
              --name subdefensoria-service \
              -p 8091:8080 \
              -m 250m \
              -e JAVA_TOOL_OPTIONS="-Xmx150m -Xms64m" \
              -v $BASE_DIR/config-files/subdefensoria-service/config:/app/config:Z \
              -e SPRING_CONFIG_ADDITIONAL_LOCATION=optional:file:/app/config/ \
              -e SPRING_CONFIG_NAME=subdefensoria-service \
              -e QUEJAS_SERVICE_URL="http://156.67.26.73:8084" \
              localhost/defensoria-subdefensoria-service
            ;;

        "historico-service")
            sudo podman run -q -d \
              --restart always \
              --name historico-service \
              -p 8092:8080 \
              -m 512m \
              -e JAVA_TOOL_OPTIONS="-Xmx320m -Xms64m -XX:+ExitOnOutOfMemoryError" \
              -v $BASE_DIR/config-files/historico-service/config:/app/config:Z \
              -e SPRING_CONFIG_ADDITIONAL_LOCATION=optional:file:/app/config/ \
              -e SPRING_CONFIG_NAME=historico-service \
              -e QUEJAS_SERVICE_URL="http://156.67.26.73:8084" \
              localhost/defensoria-historico-service
            ;;

        "denunciado-service")
            # Recibe credencial + evidencias (hasta 100MB por petición) y las guarda como BYTEA:
            # mismo perfil de memoria que quejas-service.
            sudo podman run -q -d \
              --restart always \
              --name denunciado-service \
              -p 8094:8080 \
              -m 1g \
              -e JAVA_TOOL_OPTIONS="-Xmx640m -Xms64m -XX:+ExitOnOutOfMemoryError" \
              -v $BASE_DIR/config-files/denunciado-service/config:/app/config:Z \
              -e SPRING_CONFIG_ADDITIONAL_LOCATION=optional:file:/app/config/ \
              -e SPRING_CONFIG_NAME=denunciado-service \
              localhost/defensoria-denunciado-service
            ;;

        *)
            echo "Error: Servicio desconocido o invalido: $SERVICE"
            exit 1
            ;;
    esac

    echo "Contenedor $SERVICE iniciado."
}

# Detener y eliminar contenedor
remove_service() {
    SERVICE=$1
    echo "Deteniendo y eliminando el contenedor $SERVICE..."
    sudo podman stop "$SERVICE" 2>/dev/null
    sudo podman rm "$SERVICE" 2>/dev/null
    echo "Contenedor $SERVICE eliminado."
}

# Logica principal del script
COMANDO=$1
SERVICIO=$2

case "$COMANDO" in
    up)
        for srv in "${SERVICIOS[@]}"; do
            remove_service "$srv"
            build_service "$srv"
            start_service "$srv"
        done
        ;;
    up-container)
        if [ -z "$SERVICIO" ]; then echo "Debes especificar un servicio."; exit 1; fi
        remove_service "$SERVICIO"
        build_service "$SERVICIO"
        start_service "$SERVICIO"
        ;;
    delete)
        for srv in "${SERVICIOS[@]}"; do
            remove_service "$srv"
        done
        ;;
    delete-container)
        if [ -z "$SERVICIO" ]; then echo "Debes especificar un servicio."; exit 1; fi
        remove_service "$SERVICIO"
        ;;
    *)
        mostrar_ayuda
        ;;
esac