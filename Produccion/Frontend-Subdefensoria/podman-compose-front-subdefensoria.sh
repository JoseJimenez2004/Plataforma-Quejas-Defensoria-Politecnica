#!/bin/bash
BASE_DIR="/apps/aplicaciones/defensoria/front-subdefensoria"
CONTAINER_NAME="subdefensoria-web"
PORT=8095

case "$1" in
    up)
        cd $BASE_DIR
        podman stop "$CONTAINER_NAME" 2>/dev/null
        podman rm "$CONTAINER_NAME" 2>/dev/null
        podman build -t defensoria-subdefensoria-img .
        podman run -d --restart always --name "$CONTAINER_NAME" -p ${PORT}:80 localhost/defensoria-subdefensoria-img
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
