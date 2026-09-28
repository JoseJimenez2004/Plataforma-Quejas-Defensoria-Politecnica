#!/bin/bash
BASE_DIR="/apps/aplicaciones/defensoria/front-primer-contacto"
CONTAINER_NAME="primer-contacto-web"
PORT=22348

case "$1" in
    up)
        cd $BASE_DIR
        podman stop "$CONTAINER_NAME" 2>/dev/null
        podman rm "$CONTAINER_NAME" 2>/dev/null
        podman build -t defensoria-primer-contacto-img .
        podman run -d --name "$CONTAINER_NAME" -p ${PORT}:80 localhost/defensoria-primer-contacto-img
        podman ps -f name="$CONTAINER_NAME"
        ;;
    down)
        podman stop "$CONTAINER_NAME" 2>/dev/null
        podman rm "$CONTAINER_NAME" 2>/dev/null
        ;;
    *)
        echo "Uso: $0 {up|down}"
        ;;
esac
