#!/bin/bash

# ==============================================================================
# Orquestador de Microservicios - Plataforma Defensoria
# ==============================================================================

BASE_DIR="/apps/aplicaciones/defensoria/back"
SERVICIOS=("auth-service" "quejas-service" "notificaciones-service" "catalogo-service" "admin-service" "revision-service" "chatbot-service" "primer-contacto-service" "subdefensoria-service")

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
        # primer-contacto-service trae 8082 como default en su propio application.properties
        # (módulo local "primercontacto") y no choca con nada más de esta lista, así que se
        # respeta el mismo puerto también en producción.
        "primer-contacto-service") echo 8082 ;;
        # subdefensoria-service trae 8083 como default en su propio application.properties
        # (módulo local "subdefensoria") -- CHOCA con auth-service (8083 ya asignado arriba).
        # 8090 tampoco sirve: ya lo usa defensoria-web en la VPS frontend (puerto distinto host,
        # sin conflicto real, pero se evita para no confundir). Se reasigna a 8091 vía
        # config-files/subdefensoria-service (no se tocó el application.properties del módulo,
        # solo se sobreescribe server.port en el yml de despliegue, igual que ya se hace con el
        # resto de la config de producción).
        "subdefensoria-service") echo 8091 ;;
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
    echo "Servicios validos: auth-service, quejas-service, notificaciones-service, catalogo-service, admin-service, revision-service, chatbot-service, primer-contacto-service, subdefensoria-service"
}

# Construir una imagen dedicada por microservicio (cada uno con su propio tag,
# ya no comparten "defensoria-base-img" para evitar confundir qué imagen es cuál
# y dejar de generar imagenes "dangling" cada vez que se reconstruye otro servicio)
build_service() {
    SERVICE=$1
    PORT=$(get_port "$SERVICE")
    echo "Construyendo/Actualizando imagen defensoria-${SERVICE} (externo ${PORT} -> interno 8080)..."
    cd $BASE_DIR

    if [ ! -f "artifact/${SERVICE}.jar" ]; then
        echo "Error: No se encontro el archivo artifact/${SERVICE}.jar"
        exit 1
    fi

    # admin-service tiene su propio Dockerfile (necesita postgresql-client instalado para
    # los respaldos, ver admin-service/Dockerfile) -- los demas usan el Dockerfile compartido
    # de la raiz.
    DOCKERFILE="Dockerfile"
    if [ -f "${SERVICE}/Dockerfile" ]; then
        DOCKERFILE="${SERVICE}/Dockerfile"
    fi

    # SERVICE_PORT solo documenta el EXPOSE de la imagen -- el puerto real en el que escucha
    # Spring Boot lo define server.port en config-files/$SERVICE/config/*.yml (8080 en los 9
    # microservicios, ver esos yml). Se deja fijo en 8080 para que EXPOSE refleje la realidad.
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

    # admin-service necesita un volumen para que los .sql de respaldo sobrevivan a que se
    # reconstruya el contenedor (si no, "up-container admin-service" los borraría cada vez).
    VOLUMEN_EXTRA=""
    if [ "$SERVICE" = "admin-service" ]; then
        mkdir -p "$BASE_DIR/respaldos"
        VOLUMEN_EXTRA="-v $BASE_DIR/respaldos:/app/respaldos:Z"
    fi

    # Puerto interno del contenedor unificado a 8080 en los 9 microservicios (mismo patrón que
    # el "template_gio" del trabajo) -- cada contenedor tiene su propio namespace de red, así
    # que no chocan entre sí aunque todos escuchen "por dentro" en el mismo puerto. El puerto
    # real de acceso externo/host ($PORT) no cambia.
    podman run -d \
      --name "$SERVICE" \
      -p $PORT:8080 \
      -v $BASE_DIR/config-files/$SERVICE/config:/app/config:Z \
      $VOLUMEN_EXTRA \
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
