#!/bin/bash

# ==============================================================================
# Orquestador de Microservicios - Plataforma Defensoria
# ==============================================================================

BASE_DIR="/apps/aplicaciones/defensoria/back"

RESPALDOS_DIR="/apps/utiles/respaldos"
SERVICIOS=("auth-service" "quejas-service" "notificaciones-service" "catalogo-service" "admin-service" "revision-service" "chatbot-service" "primer-contacto-service" "subdefensoria-service" "historico-service")

# Mapa de puertos por microservicio
get_port() {
    case "$1" in
        "auth-service") echo 8083 ;;
        "quejas-service") echo 8084 ;;
        "notificaciones-service") echo 8085 ;;
        "catalogo-service") echo 8086 ;;
        "admin-service") echo 8087 ;;
        "revision-service") echo 8088 ;;
        "chatbot-service") echo 8089 ;;
        "primer-contacto-service") echo 8082 ;;
        "subdefensoria-service") echo 8091 ;;
        "historico-service") echo 8092 ;;
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
    echo "Servicios validos: auth-service, quejas-service, notificaciones-service, catalogo-service, admin-service, revision-service, chatbot-service, primer-contacto-service, subdefensoria-service, historico-service"
}

build_service() {
    SERVICE=$1
    PORT=$(get_port "$SERVICE")
    echo "Construyendo/Actualizando imagen defensoria-${SERVICE} (externo ${PORT} -> interno 8080)..."
    cd $BASE_DIR

    if [ ! -f "artifact/${SERVICE}.jar" ]; then
        echo "Error: No se encontro el archivo artifact/${SERVICE}.jar"
        exit 1
    fi

    DOCKERFILE="Dockerfile"
    if [ -f "${SERVICE}/Dockerfile" ]; then
        DOCKERFILE="${SERVICE}/Dockerfile"
    fi

    podman build \
      -f "$DOCKERFILE" \
      --build-arg JAR_FILE=artifact/${SERVICE}.jar \
      --build-arg SERVICE_PORT=8080 \
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

    echo "Levantando contenedor para $SERVICE ($PORT -> 8080 interno)..."

    VOLUMEN_EXTRA=""
    if [ "$SERVICE" = "admin-service" ]; then
        mkdir -p "$RESPALDOS_DIR"
        VOLUMEN_EXTRA="-v $RESPALDOS_DIR:/app/respaldos:Z"
    fi

    # === NUEVO BLOQUE PARA LIMITAR MEMORIA ===
    # Solo aplicará al servicio que elijas para la prueba
    MEM_ARGS=()
    if [ "$SERVICE" = "revision-service" ]; then
        echo "Aplicando limites de memoria (100MB) a $SERVICE..."
        MEM_ARGS=(--memory="100m" -e JAVA_TOOL_OPTIONS="-Xmx70m -Xms32m")
    fi
    # =========================================

    podman run -d \
      --name "$SERVICE" \
      -p $PORT:8080 \
      -v $BASE_DIR/config-files/$SERVICE/config:/app/config:Z \
      $VOLUMEN_EXTRA \
      "${MEM_ARGS[@]}" \
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