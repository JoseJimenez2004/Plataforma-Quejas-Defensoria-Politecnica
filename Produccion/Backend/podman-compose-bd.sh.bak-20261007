#!/bin/bash

# ==============================================================================
# Podman Compose - Bases de Datos
# Plataforma Defensoria de los Derechos Politécnicos
# ==============================================================================
# 
# Este script crea y levanta automáticamente 2 contenedores de Postgres:
# - defensoria_db (puerto 5432) - Base principal del sistema
# - historico_db (puerto 5433) - Base de histórico/archivo
#
# Los scripts SQL en database-scripts/ se ejecutan automáticamente cuando el
# contenedor se levanta por primera vez.
#
# ==============================================================================

# Configuración
BASE_DIR="/apps/aplicaciones/defensoria/database"
DATA_DIR="/apps/data/postgresql"
SCRIPTS_DIR="/apps/aplicaciones/defensoria/database-scripts"
BACKUP_DIR="/apps/respaldos/bd"

# Credenciales de base de datos (CAMBIAR EN PRODUCCIÓN)
POSTGRES_USER="postgres"
POSTGRES_PASSWORD="Temporal2026@"

# Configuración de memoria
DEFENSORIA_DB_MEMORY="1g"
HISTORICO_DB_MEMORY="512m"

# Mostrar menu de ayuda
mostrar_ayuda() {
    echo "Uso: sudo bash podman-compose-bd.sh [COMANDO]"
    echo ""
    echo "Comandos disponibles:"
    echo "  up                    Levanta AMBOS contenedores de BD"
    echo "  up-defensoria        Levanta solo defensoria_db"
    echo "  up-historico          Levanta solo historico_db"
    echo "  down                  Detiene AMBOS contenedores"
    echo "  down-defensoria       Detiene solo defensoria_db"
    echo "  down-historico       Detiene solo historico_db"
    echo "  restart              Reinicia AMBOS contenedores"
    echo "  status               Muestra estado de los contenedores"
    echo "  logs <contenedor>    Muestra logs de un contenedor"
    echo "  backup               Hace respaldo de todas las BDs"
    echo "  restore <archivo>     Restaura un respaldo"
    echo "  connect-defensoria   Conecta a defensoria_db con psql"
    echo "  connect-historico    Conecta a historico_db con psql"
    echo ""
    echo "Bases de datos:"
    echo "  - defensoria_db       (puerto 5432) - Base principal del sistema"
    echo "  - historico_db        (puerto 5433) - Base de histórico/archivo"
    echo ""
    echo "Scripts SQL disponibles en $SCRIPTS_DIR:"
    ls -1 "$SCRIPTS_DIR"/*.sql 2>/dev/null || echo "  (No hay scripts SQL)"
}

# Crear directorios necesarios
crear_directorios() {
    echo "Creando directorios necesarios..."
    mkdir -p "$DATA_DIR/defensoria_db"
    mkdir -p "$DATA_DIR/historico_db"
    mkdir -p "$BACKUP_DIR"
    mkdir -p "$SCRIPTS_DIR"
    echo "✅ Directorios creados"
}

# Levantar contenedor de defensoria_db
start_defensoria_db() {
    echo "Levantando contenedor defensoria_db..."
    
    # Verificar si ya existe
    if sudo podman ps -a --format "{{.Names}}" | grep -q "^defensoria-db$"; then
        echo "El contenedor defensoria-db ya existe. Eliminándolo..."
        sudo podman rm -f defensoria-db
    fi
    
    sudo podman run -d \
      --name defensoria-db \
      --restart unless-stopped \
      -p 5432:5432 \
      -e POSTGRES_USER="$POSTGRES_USER" \
      -e POSTGRES_PASSWORD="$POSTGRES_PASSWORD" \
      -e POSTGRES_DB=defensoria_db \
      -v "$DATA_DIR/defensoria_db:/var/lib/postgresql/data:Z" \
      -v "$SCRIPTS_DIR:/docker-entrypoint-initdb.d:Z" \
      -m "$DEFENSORIA_DB_MEMORY" \
      docker.io/library/postgres:16-alpine
    
    echo "✅ Contenedor defensoria_db iniciado en puerto 5432"
    echo "   Base de datos: defensoria_db"
    echo "   Usuario: $POSTGRES_USER"
    echo "   Scripts SQL se ejecutarán automáticamente en el primer arranque"
}

# Levantar contenedor de historico_db
start_historico_db() {
    echo "Levantando contenedor historico_db..."
    
    # Verificar si ya existe
    if sudo podman ps -a --format "{{.Names}}" | grep -q "^historico-db$"; then
        echo "El contenedor historico-db ya existe. Eliminándolo..."
        sudo podman rm -f historico-db
    fi
    
    sudo podman run -d \
      --name historico-db \
      --restart unless-stopped \
      -p 5433:5432 \
      -e POSTGRES_USER="$POSTGRES_USER" \
      -e POSTGRES_PASSWORD="$POSTGRES_PASSWORD" \
      -e POSTGRES_DB=historico_db \
      -v "$DATA_DIR/historico_db:/var/lib/postgresql/data:Z" \
      -m "$HISTORICO_DB_MEMORY" \
      docker.io/library/postgres:16-alpine
    
    echo "✅ Contenedor historico_db iniciado en puerto 5433"
    echo "   Base de datos: historico_db"
    echo "   Usuario: $POSTGRES_USER"
}

# Detener contenedor
stop_container() {
    CONTAINER=$1
    echo "Deteniendo contenedor $CONTAINER..."
    
    if sudo podman ps --format "{{.Names}}" | grep -q "^$CONTAINER$"; then
        sudo podman stop "$CONTAINER"
        echo "✅ Contenedor $CONTAINER detenido"
    else
        echo "⚠️  El contenedor $CONTAINER no está corriendo"
    fi
}

# Eliminar contenedor
remove_container() {
    CONTAINER=$1
    echo "Eliminando contenedor $CONTAINER..."
    
    if sudo podman ps -a --format "{{.Names}}" | grep -q "^$CONTAINER$"; then
        sudo podman rm -f "$CONTAINER"
        echo "✅ Contenedor $CONTAINER eliminado"
    else
        echo "⚠️  El contenedor $CONTAINER no existe"
    fi
}

# Hacer respaldo de todas las BDs
backup_all() {
    TIMESTAMP=$(date +%Y%m%d_%H%M%S)
    
    echo "Iniciando respaldo de todas las bases de datos..."
    
    # Verificar que los contenedores estén corriendo
    if ! sudo podman ps --format "{{.Names}}" | grep -q "^defensoria-db$"; then
        echo "❌ Error: defensoria-db no está corriendo"
        exit 1
    fi
    
    if ! sudo podman ps --format "{{.Names}}" | grep -q "^historico-db$"; then
        echo "❌ Error: historico-db no está corriendo"
        exit 1
    fi
    
    # Respaldo defensoria_db
    echo "Respaldo de defensoria_db..."
    sudo podman exec defensoria-db pg_dump -U "$POSTGRES_USER" defensoria_db \
      > "$BACKUP_DIR/defensoria_db_$TIMESTAMP.sql"
    
    # Respaldo historico_db
    echo "Respaldo de historico_db..."
    sudo podman exec historico-db pg_dump -U "$POSTGRES_USER" historico_db \
      > "$BACKUP_DIR/historico_db_$TIMESTAMP.sql"
    
    # Comprimir
    gzip "$BACKUP_DIR/defensoria_db_$TIMESTAMP.sql"
    gzip "$BACKUP_DIR/historico_db_$TIMESTAMP.sql"
    
    echo "✅ Respaldo completado:"
    echo "   $BACKUP_DIR/defensoria_db_$TIMESTAMP.sql.gz"
    echo "   $BACKUP_DIR/historico_db_$TIMESTAMP.sql.gz"
}

# Restaurar respaldo
restore_backup() {
    ARCHIVO=$1
    
    if [ -z "$ARCHIVO" ]; then
        echo "❌ Error: Debes especificar el archivo de respaldo"
        echo "   Uso: $0 restore <archivo.sql o archivo.sql.gz>"
        exit 1
    fi
    
    if [ ! -f "$ARCHIVO" ]; then
        echo "❌ Error: El archivo $ARCHIVO no existe"
        exit 1
    fi
    
    # Determinar cual BD restaurar por el nombre del archivo
    if [[ "$ARCHIVO" == *"defensoria_db"* ]]; then
        CONTENEDOR="defensoria-db"
        BD="defensoria_db"
    elif [[ "$ARCHIVO" == *"historico_db"* ]]; then
        CONTENEDOR="historico-db"
        BD="historico_db"
    else
        echo "❌ Error: No se puede determinar la BD del archivo $ARCHIVO"
        echo "   El nombre debe contener 'defensoria_db' o 'historico_db'"
        exit 1
    fi
    
    # Verificar que el contenedor esté corriendo
    if ! sudo podman ps --format "{{.Names}}" | grep -q "^$CONTENEDOR$"; then
        echo "❌ Error: El contenedor $CONTENEDOR no está corriendo"
        exit 1
    fi
    
    echo "Restaurando $ARCHIVO en $BD..."
    
    # Descomprimir si es .gz
    if [[ "$ARCHIVO" == *.gz ]]; then
        gunzip -c "$ARCHIVO" | sudo podman exec -i "$CONTENEDOR" psql -U "$POSTGRES_USER" "$BD"
    else
        cat "$ARCHIVO" | sudo podman exec -i "$CONTENEDOR" psql -U "$POSTGRES_USER" "$BD"
    fi
    
    echo "✅ Restauración completada"
}

# Mostrar estado
show_status() {
    echo "Estado de los contenedores de base de datos:"
    echo ""
    
    echo "defensoria-db:"
    if sudo podman ps -a --format "{{.Names}}\t{{.Status}}\t{{.Ports}}" | grep -q "^defensoria-db"; then
        sudo podman ps -a --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep "defensoria-db"
    else
        echo "  ❌ No existe"
    fi
    
    echo ""
    echo "historico-db:"
    if sudo podman ps -a --format "{{.Names}}\t{{.Status}}\t{{.Ports}}" | grep -q "^historico-db"; then
        sudo podman ps -a --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep "historico-db"
    else
        echo "  ❌ No existe"
    fi
}

# Mostrar logs
show_logs() {
    CONTENEDOR=$1
    
    if [ -z "$CONTENEDOR" ]; then
        echo "❌ Error: Debes especificar el contenedor"
        echo "   Uso: $0 logs <defensoria-db|historico-db>"
        exit 1
    fi
    
    if [[ "$CONTENEDOR" != "defensoria-db" && "$CONTENEDOR" != "historico-db" ]]; then
        echo "❌ Error: Contenedor no válido. Debe ser 'defensoria-db' o 'historico-db'"
        exit 1
    fi
    
    echo "Mostrando logs de $CONTENEDOR (Ctrl+C para salir)..."
    sudo podman logs -f "$CONTENEDOR"
}

# Conectar a BD con psql
connect_db() {
    CONTENEDOR=$1
    BD=$2
    
    if ! sudo podman ps --format "{{.Names}}" | grep -q "^$CONTENEDOR$"; then
        echo "❌ Error: El contenedor $CONTENEDOR no está corriendo"
        exit 1
    fi
    
    echo "Conectando a $BD en $CONTENEDOR..."
    sudo podman exec -it "$CONTENEDOR" psql -U "$POSTGRES_USER" "$BD"
}

# Lógica principal
COMANDO=$1
PARAMETRO=$2

case "$COMANDO" in
    up)
        crear_directorios
        start_defensoria_db
        start_historico_db
        echo ""
        echo "✅ Todos los contenedores de BD iniciados"
        echo ""
        echo "Esperando 10 segundos para que Postgres se inicialice..."
        sleep 10
        show_status
        ;;
    up-defensoria)
        crear_directorios
        start_defensoria_db
        ;;
    up-historico)
        crear_directorios
        start_historico_db
        ;;
    down)
        stop_container "defensoria-db"
        stop_container "historico-db"
        echo ""
        echo "✅ Todos los contenedores de BD detenidos"
        ;;
    down-defensoria)
        stop_container "defensoria-db"
        ;;
    down-historico)
        stop_container "historico-db"
        ;;
    restart)
        stop_container "defensoria-db"
        stop_container "historico-db"
        sleep 2
        start_defensoria_db
        start_historico_db
        echo ""
        echo "✅ Todos los contenedores de BD reiniciados"
        ;;
    status)
        show_status
        ;;
    logs)
        show_logs "$PARAMETRO"
        ;;
    backup)
        backup_all
        ;;
    restore)
        restore_backup "$PARAMETRO"
        ;;
    connect-defensoria)
        connect_db "defensoria-db" "defensoria_db"
        ;;
    connect-historico)
        connect_db "historico-db" "historico_db"
        ;;
    *)
        mostrar_ayuda
        ;;
esac
