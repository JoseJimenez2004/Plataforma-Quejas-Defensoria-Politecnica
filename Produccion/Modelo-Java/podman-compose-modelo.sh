#!/bin/bash

# ==============================================================================
# Orquestador del Modelo de IA - Plataforma Defensoria
# Microservicio: antecedentes-service (TF-IDF + coseno / resumen TextRank)
# ==============================================================================

BASE_DIR="/apps/aplicaciones/defensoria/back"
SERVICIO="antecedentes-service"
IMAGEN="defensoria-${SERVICIO}"
PUERTO_HOST=8093
JAR="artifact/${SERVICIO}.jar"
CONFIG_DIR="$BASE_DIR/config-files/${SERVICIO}/config"
DATASET_DIR="$BASE_DIR/modelo/Dataset"

mostrar_ayuda() {
    echo "Uso: sudo bash podman-compose-modelo.sh [COMANDO]"
    echo ""
    echo "Comandos disponibles:"
    echo "  up        Construye la imagen y levanta el contenedor (recrea si ya existe)."
    echo "  restart   Reinicia el contenedor sin reconstruir la imagen."
    echo "  delete    Detiene y elimina el contenedor."
    echo "  logs      Muestra los logs en vivo."
    echo "  status    Muestra el estado del contenedor y el health check."
    echo ""
    echo "Rutas esperadas en el servidor:"
    echo "  JAR:      $BASE_DIR/$JAR"
    echo "  Config:   $CONFIG_DIR/${SERVICIO}.yml"
    echo "  Datasets: $DATASET_DIR/quejas_sinteticas.json, quejas_prueba_30.json"
}

build_service() {
    echo "Construyendo/Actualizando imagen ${IMAGEN}..."
    cd "$BASE_DIR" || exit 1

    if [ ! -f "$JAR" ]; then
        echo "Error: No se encontro el archivo $BASE_DIR/$JAR"
        exit 1
    fi
    if [ ! -f "$CONFIG_DIR/${SERVICIO}.yml" ]; then
        echo "Error: No se encontro la configuracion $CONFIG_DIR/${SERVICIO}.yml"
        exit 1
    fi

    DOCKERFILE="Dockerfile"
    if [ -f "${SERVICIO}/Dockerfile" ]; then
        DOCKERFILE="${SERVICIO}/Dockerfile"
    fi

    sudo podman build -q \
      -f "$DOCKERFILE" \
      --build-arg JAR_FILE="$JAR" \
      --build-arg SERVICE_PORT=8080 \
      -t "$IMAGEN" .
    echo "Imagen ${IMAGEN} actualizada exitosamente."
}

start_service() {
    echo "Levantando contenedor para ${SERVICIO}..."
    mkdir -p "$DATASET_DIR"

    if [ ! -f "$DATASET_DIR/quejas_sinteticas.json" ]; then
        echo "Aviso: no existe $DATASET_DIR/quejas_sinteticas.json; el modelo arrancara solo con las quejas aprendidas."
    fi

    # -XX:+UseSerialGC reduce la huella de memoria en contenedores pequeños.
    sudo podman run -q -d \
      --name "$SERVICIO" \
      -p ${PUERTO_HOST}:8080 \
      -m 250m \
      -e JAVA_TOOL_OPTIONS="-Xmx150m -Xms64m -XX:+UseSerialGC" \
      -v "$CONFIG_DIR":/app/config:Z \
      -v "$DATASET_DIR":/app/data:Z \
      -e SPRING_CONFIG_ADDITIONAL_LOCATION=optional:file:/app/config/ \
      -e SPRING_CONFIG_NAME="$SERVICIO" \
      "localhost/$IMAGEN"

    echo "Contenedor ${SERVICIO} iniciado en el puerto ${PUERTO_HOST}."
}

remove_service() {
    echo "Deteniendo y eliminando el contenedor ${SERVICIO}..."
    sudo podman stop "$SERVICIO" 2>/dev/null
    sudo podman rm "$SERVICIO" 2>/dev/null
    echo "Contenedor ${SERVICIO} eliminado."
}

status_service() {
    sudo podman ps -a --filter "name=^${SERVICIO}$"
    echo ""
    curl -s --max-time 5 "http://127.0.0.1:${PUERTO_HOST}/api/antecedentes/health" || echo "Health check sin respuesta."
    echo ""
}

case "$1" in
    up)
        remove_service
        build_service
        start_service
        ;;
    restart)
        sudo podman restart "$SERVICIO"
        ;;
    delete)
        remove_service
        ;;
    logs)
        sudo podman logs -f --tail 100 "$SERVICIO"
        ;;
    status)
        status_service
        ;;
    *)
        mostrar_ayuda
        ;;
esac
