#!/bin/bash
BASE_DIR="/apps/aplicaciones/defensoria/front-admin"
CONTAINER_NAME="admin-web"
PORT=22346

case "$1" in
    up)
        cd $BASE_DIR
        podman stop "$CONTAINER_NAME" 2>/dev/null
        podman rm "$CONTAINER_NAME" 2>/dev/null
        podman build -t defensoria-admin-img .
        podman run -d --name "$CONTAINER_NAME" -p ${PORT}:80 localhost/defensoria-admin-img
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
