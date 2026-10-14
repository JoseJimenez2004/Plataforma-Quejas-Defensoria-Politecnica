#!/bin/bash
# ==============================================================================
# Bases de datos - Plataforma Defensoría de los Derechos Politécnicos
# Servidor BD: 169.58.62.99  (correr como root: bash podman-compose-bd.sh <comando>)
# ==============================================================================
#  defensoria-db  -> puerto 5432 -> base defensoria_db           (todo el sistema)
#  historico-db   -> puerto 5433 -> base defensoria_historico_db (solo historico-service)
#
#  Orden de arranque desde cero:
#    1. bash podman-compose-bd.sh up         (crea las 2 bases vacías)
#    2. bash podman-compose-bd.sh firewall   (5432/5433 solo desde el backend)
#    3. levantar los microservicios en 156.67.26.73 (Hibernate crea las tablas)
#    4. bash podman-compose-bd.sh seed       (dependencias, chatbot, personal de prueba)
# ==============================================================================

APP_DIR="/apps/aplicaciones/defensoria"
SCRIPTS_DIR="$APP_DIR/database-scripts"
INIT_DIR="$SCRIPTS_DIR/init"      # se ejecuta solo al crear defensoria-db por primera vez
SEEDS_DIR="$SCRIPTS_DIR/seeds"    # se ejecuta a mano con "seed", cuando ya existen las tablas
DATA_DIR="/apps/data/postgresql"
BACKUP_DIR="/apps/respaldos/bd"

IMAGEN="docker.io/library/postgres:16-alpine"
POSTGRES_USER="postgres"
# Debe ser IDÉNTICA a spring.datasource.password / db.password de los config-files del backend.
POSTGRES_PASSWORD="Temporal2026@"

DB_PRINCIPAL="defensoria_db"
DB_HISTORICO="defensoria_historico_db"

# Única IP que puede llegar a 5432/5433 desde fuera (servidor backend).
IP_BACKEND="156.67.26.73"

DEFENSORIA_DB_MEMORY="1g"
HISTORICO_DB_MEMORY="512m"

mostrar_ayuda() {
    cat <<AYUDA
Uso: bash podman-compose-bd.sh <comando>

  up                  Crea/levanta los 2 contenedores (bases vacías la primera vez)
  up-defensoria       Solo defensoria-db
  up-historico        Solo historico-db
  down                Detiene y elimina los 2 contenedores (los DATOS se conservan)
  reset               Elimina contenedores Y DATOS y vuelve a crear todo vacío (pide confirmación)
  status              Estado de los contenedores
  logs <contenedor>   Logs en vivo (defensoria-db | historico-db)
  seed                Carga dependencias, chatbot y personal de prueba (después de levantar el backend)
  firewall            Bloquea 5432/5433 a todo el que no sea $IP_BACKEND
  firewall-boot       Instala un servicio systemd para reaplicar 'firewall' en cada reinicio
  backup              Respaldo de las 2 bases en $BACKUP_DIR
  restore <archivo>   Restaura un respaldo (.sql o .sql.gz)
  psql-defensoria     Abre psql en defensoria_db
  psql-historico      Abre psql en defensoria_historico_db
AYUDA
}

existe()   { podman ps -a --format '{{.Names}}' | grep -qx "$1"; }
corriendo(){ podman ps    --format '{{.Names}}' | grep -qx "$1"; }

esperar_listo() {
    local C=$1 DB=$2
    echo -n "Esperando a que $C acepte conexiones"
    for i in $(seq 1 60); do
        if podman exec "$C" pg_isready -U "$POSTGRES_USER" -d "$DB" >/dev/null 2>&1; then
            echo " ✅"; return 0
        fi
        echo -n "."; sleep 1
    done
    echo " ❌ (revisa: bash podman-compose-bd.sh logs $C)"; return 1
}

start_defensoria_db() {
    mkdir -p "$DATA_DIR/defensoria_db" "$INIT_DIR"
    existe defensoria-db && podman rm -f defensoria-db >/dev/null
    podman run -d \
      --name defensoria-db \
      --restart always \
      -p 5432:5432 \
      -m "$DEFENSORIA_DB_MEMORY" \
      -e TZ=America/Mexico_City \
      -e POSTGRES_USER="$POSTGRES_USER" \
      -e POSTGRES_PASSWORD="$POSTGRES_PASSWORD" \
      -e POSTGRES_DB="$DB_PRINCIPAL" \
      -v "$DATA_DIR/defensoria_db:/var/lib/postgresql/data:Z" \
      -v "$INIT_DIR:/docker-entrypoint-initdb.d:ro,Z" \
      "$IMAGEN" >/dev/null
    esperar_listo defensoria-db "$DB_PRINCIPAL" && echo "defensoria-db -> :5432 / $DB_PRINCIPAL"
}

start_historico_db() {
    mkdir -p "$DATA_DIR/historico_db"
    existe historico-db && podman rm -f historico-db >/dev/null
    podman run -d \
      --name historico-db \
      --restart always \
      -p 5433:5432 \
      -m "$HISTORICO_DB_MEMORY" \
      -e TZ=America/Mexico_City \
      -e POSTGRES_USER="$POSTGRES_USER" \
      -e POSTGRES_PASSWORD="$POSTGRES_PASSWORD" \
      -e POSTGRES_DB="$DB_HISTORICO" \
      -v "$DATA_DIR/historico_db:/var/lib/postgresql/data:Z" \
      "$IMAGEN" >/dev/null
    esperar_listo historico-db "$DB_HISTORICO" && \
      podman exec historico-db psql -q -U "$POSTGRES_USER" -d "$DB_HISTORICO" \
        -c "ALTER DATABASE $DB_HISTORICO SET timezone TO 'America/Mexico_City';" && \
      echo "historico-db  -> :5433 / $DB_HISTORICO"
}

# Arranque automático tras reiniciar el VPS (aplica a contenedores con --restart always).
habilitar_arranque() {
    systemctl enable podman-restart.service >/dev/null 2>&1 && \
      echo "podman-restart.service habilitado (los contenedores vuelven solos tras un reboot)"
}

eliminar() { existe "$1" && podman rm -f "$1" >/dev/null && echo "$1 eliminado"; }

# --- seeds --------------------------------------------------------------------
psql_q() { podman exec defensoria-db psql -U "$POSTGRES_USER" -d "$DB_PRINCIPAL" -tAc "$1"; }

cargar_seed() {
    local TABLA=$1 ARCHIVO=$2 SOLO_SI_VACIA=$3 SERVICIO=$4
    if [ ! -f "$SEEDS_DIR/$ARCHIVO" ]; then echo "⚠️  No existe $SEEDS_DIR/$ARCHIVO"; return; fi
    if [ "$(psql_q "SELECT to_regclass('public.$TABLA') IS NOT NULL")" != "t" ]; then
        echo "⏭️  $TABLA no existe todavía -> levanta primero $SERVICIO en el backend y repite 'seed'"
        return
    fi
    local N; N=$(psql_q "SELECT count(*) FROM $TABLA")
    if [ "$SOLO_SI_VACIA" = "si" ] && [ "$N" -gt 0 ]; then
        echo "⏭️  $TABLA ya tiene $N filas, no se vuelve a cargar"; return
    fi
    if podman exec -i defensoria-db psql -q -v ON_ERROR_STOP=1 -1 -U "$POSTGRES_USER" -d "$DB_PRINCIPAL" \
         < "$SEEDS_DIR/$ARCHIVO" >/dev/null; then
        echo "✅ $ARCHIVO -> $TABLA ahora tiene $(psql_q "SELECT count(*) FROM $TABLA") filas"
    else
        echo "❌ $ARCHIVO falló (no se cargó nada, la transacción se revirtió)"
    fi
}

seed() {
    corriendo defensoria-db || { echo "❌ defensoria-db no está corriendo"; exit 1; }
    cargar_seed dependencias            01-dependencias_seed.sql si catalogo-service
    cargar_seed preguntas_chatbot       02-chatbot_seed.sql      si chatbot-service
    cargar_seed personal_administrativo 03-personal_test.sql     no admin-service   # es UPSERT
}

# --- firewall -----------------------------------------------------------------
# Podman publica los puertos con reglas DNAT que se saltan ufw (igual que Docker), así que
# "ufw allow from ..." NO protege 5432/5433. La tabla raw se evalúa antes del DNAT.
firewall() {
    # Solo se filtra lo que entra por la interfaz pública: así el tráfico local y el de la
    # red de Podman nunca se ve afectado (ver Backend/firewall-backend.sh).
    local IFACE; IFACE=$(ip route show default | awk '{print $5; exit}')
    [ -n "$IFACE" ] || { echo "❌ No pude detectar la interfaz pública"; exit 1; }
    for P in 5432 5433; do
        while iptables -t raw -D PREROUTING -p tcp --dport $P ! -s "$IP_BACKEND" ! -i lo -j DROP 2>/dev/null; do :; done
        iptables -t raw -C PREROUTING -i "$IFACE" -p tcp --dport $P ! -s "$IP_BACKEND" -j DROP 2>/dev/null || \
        iptables -t raw -I PREROUTING -i "$IFACE" -p tcp --dport $P ! -s "$IP_BACKEND" -j DROP
    done
    echo "✅ 5432 y 5433 solo aceptan conexiones de $IP_BACKEND"
    iptables -t raw -S PREROUTING | grep -E 'dport (5432|5433)'
}

firewall_boot() {
    local RUTA; RUTA=$(readlink -f "$0")
    cat > /etc/systemd/system/defensoria-bd-firewall.service <<UNIT
[Unit]
Description=Defensoria - restringe 5432/5433 al servidor backend
After=network-online.target
Wants=network-online.target

[Service]
Type=oneshot
ExecStart=/bin/bash $RUTA firewall
RemainAfterExit=yes

[Install]
WantedBy=multi-user.target
UNIT
    systemctl daemon-reload && systemctl enable --now defensoria-bd-firewall.service && \
      echo "✅ La regla se reaplicará en cada reinicio"
}

# --- respaldos ----------------------------------------------------------------
backup() {
    mkdir -p "$BACKUP_DIR"
    local T; T=$(date +%Y%m%d_%H%M%S)
    corriendo defensoria-db && podman exec defensoria-db pg_dump -U "$POSTGRES_USER" "$DB_PRINCIPAL" \
        | gzip > "$BACKUP_DIR/${DB_PRINCIPAL}_$T.sql.gz" && echo "✅ $BACKUP_DIR/${DB_PRINCIPAL}_$T.sql.gz"
    corriendo historico-db && podman exec historico-db pg_dump -U "$POSTGRES_USER" "$DB_HISTORICO" \
        | gzip > "$BACKUP_DIR/${DB_HISTORICO}_$T.sql.gz" && echo "✅ $BACKUP_DIR/${DB_HISTORICO}_$T.sql.gz"
}

restore() {
    local A=$1 C DB
    [ -f "$A" ] || { echo "Uso: restore <archivo.sql[.gz]> (no existe '$A')"; exit 1; }
    case "$A" in
        *historico*) C=historico-db;  DB=$DB_HISTORICO ;;
        *)           C=defensoria-db; DB=$DB_PRINCIPAL ;;
    esac
    echo "Restaurando $A en $DB ($C)..."
    if [[ "$A" == *.gz ]]; then gunzip -c "$A"; else cat "$A"; fi \
      | podman exec -i "$C" psql -q -U "$POSTGRES_USER" -d "$DB" && echo "✅ Restaurado"
}

case "$1" in
    up)              start_defensoria_db; start_historico_db; habilitar_arranque; podman ps -a --filter name=-db ;;
    up-defensoria)   start_defensoria_db ;;
    up-historico)    start_historico_db ;;
    down)            eliminar defensoria-db; eliminar historico-db ;;
    reset)
        read -r -p "Esto BORRA todos los datos de las 2 bases. Escribe BORRAR para continuar: " R
        [ "$R" = "BORRAR" ] || { echo "Cancelado"; exit 1; }
        eliminar defensoria-db; eliminar historico-db
        rm -rf "$DATA_DIR/defensoria_db" "$DATA_DIR/historico_db"
        start_defensoria_db; start_historico_db; habilitar_arranque ;;
    status)          podman ps -a --filter name=-db --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}' ;;
    logs)            podman logs -f "$2" ;;
    seed)            seed ;;
    firewall)        firewall ;;
    firewall-boot)   firewall_boot ;;
    backup)          backup ;;
    restore)         restore "$2" ;;
    psql-defensoria) podman exec -it defensoria-db psql -U "$POSTGRES_USER" -d "$DB_PRINCIPAL" ;;
    psql-historico)  podman exec -it historico-db  psql -U "$POSTGRES_USER" -d "$DB_HISTORICO" ;;
    *)               mostrar_ayuda ;;
esac
