#!/bin/bash
# Reconstruye los .jar de los microservicios ya actualizados (constructor injection,
# nombre de servicio en logs, fix de PasswordEncoder/primer-contacto.base-url).
# Ejecutar DESDE la carpeta Backend/ (donde están las carpetas de cada microservicio):
#
#   cd Backend
#   bash rebuild-jars.sh
#
# Al final deja los jars listos para subir en Backend/_jars-listos/

set -uo pipefail

# "carpeta:nombre-del-jar" -- el nombre del jar no siempre coincide con la carpeta
# (ej. queja-service/ genera quejas-service.jar, definido en su pom.xml finalName).
SERVICIOS=(
  "admin-service:admin-service"
  "auth-service:auth-service"
  "catalogo-service:catalogo-service"
  "chatbot-service:chatbot-service"
  "notificaciones-service:notificaciones-service"
  "queja-service:quejas-service"
  "revision-service:revision-service"
)

DESTINO="_jars-listos"
mkdir -p "$DESTINO"

OK=()
FAIL=()

for entry in "${SERVICIOS[@]}"; do
  CARPETA="${entry%%:*}"
  JAR="${entry##*:}"

  echo ""
  echo "=============================================="
  echo " Construyendo $CARPETA..."
  echo "=============================================="

  if [ ! -d "$CARPETA" ]; then
    echo "ERROR: no existe la carpeta $CARPETA"
    FAIL+=("$CARPETA (carpeta no encontrada)")
    continue
  fi

  (cd "$CARPETA" && mvn clean package -DskipTests)

  if [ $? -eq 0 ] && [ -f "$CARPETA/target/$JAR.jar" ]; then
    cp "$CARPETA/target/$JAR.jar" "$DESTINO/"
    OK+=("$CARPETA -> $JAR.jar")
  else
    FAIL+=("$CARPETA (build fallo o no se encontro el jar esperado: target/$JAR.jar)")
  fi
done

echo ""
echo "================ RESUMEN ================"
echo "Exitosos:"
for s in "${OK[@]:-}"; do [ -n "$s" ] && echo "  OK   $s"; done
echo ""
echo "Fallidos:"
for s in "${FAIL[@]:-}"; do [ -n "$s" ] && echo "  FAIL $s"; done
echo ""
echo "Jars listos para subir en: $(pwd)/$DESTINO/"
echo "Súbelos a \$BASE_DIR/artifact/ en el servidor y luego corre, por cada uno:"
echo "  bash podman-compose.sh up-container <servicio>"
