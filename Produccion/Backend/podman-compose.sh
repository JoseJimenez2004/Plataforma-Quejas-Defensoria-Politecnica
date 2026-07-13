#!/bin/bash

# ==============================================================================
# Orquestador de Microservicios - Plataforma Defensoria
# ==============================================================================

BASE_DIR="/apps/aplicaciones/defensoria/back"
SERVICIOS=("auth-service" "quejas-service" "notificaciones-service")

# Mapa de puertos por microservicio
get_port() {
    case "$1" in
        "auth-service") echo 8083 ;;
        "quejas-service") echo 8084 ;;
        "notificaciones-service") echo 8085 ;;
        *) echo 0 ;;
    esac
}

# Mostrar menu de ayuda
mostrar_ayuda() {
    echo "Uso: sh podman-compose.sh [COMANDO] [SERVICIO]"
    echo ""
    echo "Comandos disponibles:"
    echo "  up                      Construye y levanta TODOS los microservicios."
    echo "  up-container <srv>      Construye y levanta UN microservicio especifico."
    echo "  delete                  Detiene y elimina TODOS los microservicios."
    echo "  delete-container <srv>  Detiene y elimina UN microservicio especifico."
    echo ""
    echo "Servicios validos: auth-service, quejas-service, notificaciones-service"
}

# Construir una imagen dedicada por microservicio (cada uno con su propio tag,
# ya no comparten "defensoria-base-img" para evitar confundir qué imagen es cuál
# y dejar de generar imagenes "dangling" cada vez que se reconstruye otro servicio)
build_service() {
    SERVICE=$1
    PORT=$(get_port "$SERVICE")
    echo "Construyendo/Actualizando imagen defensoria-${SERVICE} (puerto ${PORT})..."
    cd $BASE_DIR

    if [ ! -f "artifact/${SERVICE}.jar" ]; then
        echo "Error: No se encontro el archivo artifact/${SERVICE}.jar"
        exit 1
    fi

    podman build \
      --build-arg JAR_FILE=artifact/${SERVICE}.jar \
      --build-arg SERVICE_PORT=${PORT} \
      -t "defensoria-${SERVICE}" .
    echo "Imagen defensoria-${SERVICE} actualizada exitosamente."
}

# Levantar contenedor utilizando su propia imagen dedicada
start_service() {
    SERVICE=$1
    PORT=$(get_port "$SERVICE")

    if [ "$PORT" -eq 0 ]; then
        echo "Error: Servicio desconocido o invalido: $SERVICE"
        exit 1
    fi

    echo "Levantando contenedor para $SERVICE en el puerto $PORT..."

    podman run -d \
      --name "$SERVICE" \
      -p $PORT:$PORT \
      -v $BASE_DIR/config-files/$SERVICE/config:/app/config:Z \
      -e SPRING_CONFIG_ADDITIONAL_LOCATION=optional:file:/app/config/ \
      -e SPRING_CONFIG_NAME="$SERVICE" \
      -e QUEJAS_SERVICE_URL="http://2.25.78.22:8084" \
      "localhost/defensoria-${SERVICE}"

    echo "Contenedor $SERVICE iniciado."
}

# Detener y eliminar contenedor
remove_service() {
    SERVICE=$1
    echo "Deteniendo y eliminando el contenedor $SERVICE..."
    podman stop "$SERVICE" 2>/dev/null
    podman rm "$SERVICE" 2>/dev/null
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
