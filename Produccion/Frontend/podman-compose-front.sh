#!/bin/bash
# Copiado del real en /apps/aplicaciones/defensoria/front en el servidor -- con UN fix: el
# original tenía PORT=22345 y publicaba "-p 22345:80", pero el Dockerfile expone 8090 y
# config/static.conf escucha en 8090 (confirmado con "podman ps -a": el contenedor
# defensoria-web real corre "0.0.0.0:8090->8090/tcp", red "bridge"). Con el PORT viejo, correr
# este script habría dejado el contenedor escuchando en un puerto que nadie más usa (22345
# hacia el 80 interno, que ni siquiera es el puerto real donde escucha nginx adentro).
BASE_DIR="/apps/aplicaciones/defensoria/front"
CONTAINER_NAME="defensoria-web"
PORT=8090

case "$1" in
    up)
        cd $BASE_DIR
        podman stop "$CONTAINER_NAME" 2>/dev/null
        podman rm "$CONTAINER_NAME" 2>/dev/null
        podman build -t defensoria-front-img .
        podman run -d --name "$CONTAINER_NAME" -p ${PORT}:${PORT} localhost/defensoria-front-img
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
