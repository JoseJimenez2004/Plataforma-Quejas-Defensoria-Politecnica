#!/bin/bash

# Configuración de rutas base
BASE_DIR="/apps/aplicaciones/defensoria"
CONTAINER_NAME="defensoria-nginx"
PORT=80

case "$1" in
    up)
        echo "=== Iniciando despliegue de Nginx ==="
        
        # 1. Validar si el contenedor ya existe para borrarlo
        if [ "$(podman ps -aq -f name=^${CONTAINER_NAME}$)" ]; then
            echo "Deteniendo y eliminando contenedor viejo '$CONTAINER_NAME'..."
            podman stop "$CONTAINER_NAME" 2>/dev/null
            podman rm "$CONTAINER_NAME" 2>/dev/null
        fi

        echo "Levantando nuevo contenedor Nginx en el puerto $PORT..."
        
        # 2. Ejecutar el contenedor mapeando volúmenes de config y front
        podman run -d \
          --name "$CONTAINER_NAME" \
          -p ${PORT}:80 \
          -v "$BASE_DIR/nginx/config/defensoria.conf:/etc/nginx/conf.d/default.conf:Z" \
          -v "$BASE_DIR/front:/usr/share/nginx/html:Z" \
          docker.io/library/nginx:alpine

        echo "=== Nginx desplegado exitosamente ==="
        podman ps -f name="$CONTAINER_NAME"
        ;;
        
    down)
        echo "=== Deteniendo entorno Nginx ==="
        if [ "$(podman ps -aq -f name=^${CONTAINER_NAME}$)" ]; then
            echo "Deteniendo contenedor '$CONTAINER_NAME'..."
            podman stop "$CONTAINER_NAME"
            echo "Eliminando contenedor '$CONTAINER_NAME'..."
            podman rm "$CONTAINER_NAME"
            echo "=== Entorno Nginx limpio y removido ==="
        else
            echo "El contenedor '$CONTAINER_NAME' no está activo o no existe."
        fi
        ;;
        
    *)
        echo "Uso incorrecto del script."
        echo "Modo de empleo: $0 {up|down}"
        exit 1
        ;;
esac
