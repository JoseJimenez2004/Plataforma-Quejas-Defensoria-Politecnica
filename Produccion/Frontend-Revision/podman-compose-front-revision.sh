#!/bin/bash
BASE_DIR="/apps/aplicaciones/defensoria/front-revision"
CONTAINER_NAME="revision-web"
PORT=22347

case "$1" in
    up)
        cd $BASE_DIR
        podman stop "$CONTAINER_NAME" 2>/dev/null
        podman rm "$CONTAINER_NAME" 2>/dev/null
        podman build -t defensoria-revision-img .
        podman run -d --name "$CONTAINER_NAME" -p ${PORT}:80 localhost/defensoria-revision-img
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
