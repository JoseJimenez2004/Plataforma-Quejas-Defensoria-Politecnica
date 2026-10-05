# Configuración de Bases de Datos - Servidor Dedicado

## Arquitectura

Este servidor dedicado aloja 2 contenedores de Postgres en Podman:

**IP del servidor BD: 169.58.62.99**

```
┌─────────────────────────────────────────┐
│  Servidor de Bases de Datos            │
│  IP: 169.58.62.99                     │
│                                         │
│  ┌─────────────────────────────────┐   │
│  │  defensoria-db (Postgres 16)   │   │
│  │  Puerto: 5432                   │   │
│  │  Memoria: 1GB                   │   │
│  │  BD: defensoria_db              │   │
│  └─────────────────────────────────┘   │
│                                         │
│  ┌─────────────────────────────────┐   │
│  │  historico-db (Postgres 16)     │   │
│  │  Puerto: 5433                   │   │
│  │  Memoria: 512MB                 │   │
│  │  BD: historico_db               │   │
│  └─────────────────────────────────┘   │
└─────────────────────────────────────────┘
```

## Pasos de Instalación

### 1. Copiar archivos al servidor

```bash
# Desde tu máquina local
scp Backend/podman-compose-bd.sh root@169.58.62.99:/apps/aplicaciones/defensoria/database/
scp -r Backend/database-scripts root@169.58.62.99:/apps/aplicaciones/defensoria/database-scripts/
```

### 2. Configurar credenciales

En el servidor BD, editar el script:

```bash
nano /apps/aplicaciones/defensoria/database/podman-compose-bd.sh
```

Cambiar estas líneas:
```bash
POSTGRES_PASSWORD="CambiarEstaPasswordEnProduccion2026!"  # ← CAMBIAR ESTO
```

### 3. Dar permisos de ejecución

```bash
chmod +x /apps/aplicaciones/defensoria/database/podman-compose-bd.sh
```

### 4. Levantar contenedores

```bash
cd /apps/aplicaciones/defensoria/database
sudo bash podman-compose-bd.sh up
```

Esto creará:
- Directorio de datos: `/apps/data/postgresql/defensoria_db`
- Directorio de datos: `/apps/data/postgresql/historico_db`
- Directorio de respaldos: `/apps/respaldos/bd`

### 5. Verificar que todo esté funcionando

```bash
# Ver estado
sudo bash podman-compose-bd.sh status

# Ver logs
sudo bash podman-compose-bd.sh logs defensoria-db

# Conectar a la BD
sudo bash podman-compose-bd.sh connect-defensoria
# Dentro de psql:
\l  # Listar BDs
\q  # Salir
```

## Scripts SQL Disponibles

Los scripts en `database-scripts/` se ejecutan automáticamente cuando el contenedor se levanta por primera vez:

- `00-init.sql` - Configuración inicial (timezone, extensiones)
- `dependencias_seed.sql` - Catálogo de 208 dependencias del IPN
- `chatbot_seed.sql` - Preguntas frecuentes del chatbot

## Cómo Agregar el Script de Estructura

Si tienes un script con la estructura de la base vieja/nueva:

1. Colócalo en `Backend/database-scripts/`
2. Nómbralo con un prefijo numérico para controlar el orden (ej: `01-estructura-base.sql`)
3. Vuelve a levantar el contenedor (o ejecútalo manualmente)

```bash
# Opción A: Volver a crear el contenedor (ejecuta todos los scripts)
sudo bash podman-compose-bd.sh down-defensoria
sudo bash podman-compose-bd.sh up-defensoria

# Opción B: Ejecutar solo el script específico
sudo podman exec -i defensoria-db psql -U postgres -d defensoria_db < /ruta/al/script.sql
```

## Comandos Útiles

```bash
# Levantar ambos contenedores
sudo bash podman-compose-bd.sh up

# Levantar solo uno
sudo bash podman-compose-bd.sh up-defensoria
sudo bash podman-compose-bd.sh up-historico

# Detener ambos
sudo bash podman-compose-bd.sh down

# Ver estado
sudo bash podman-compose-bd.sh status

# Ver logs
sudo bash podman-compose-bd.sh logs defensoria-db
sudo bash podman-compose-bd.sh logs historico-db

# Hacer respaldo
sudo bash podman-compose-bd.sh backup

# Restaurar respaldo
sudo bash podman-compose-bd.sh restore defensoria_db_20261004_100000.sql.gz

# Conectar a BD
sudo bash podman-compose-bd.sh connect-defensoria
sudo bash podman-compose-bd.sh connect-historico
```

## Configuración de Firewall

En el servidor BD (169.58.62.99), permitir solo conexiones desde el servidor backend (156.67.26.73):

```bash
# Ejemplo con ufw
sudo ufw allow from 156.67.26.73 to any port 5432
sudo ufw allow from 156.67.26.73 to any port 5433
sudo ufw enable
```

## Configuración en los Microservicios

Los microservicios en el servidor backend (156.67.26.73) deben usar estas cadenas de conexión:

```yaml
# Para defensoria_db (todos excepto historico-service)
spring:
  datasource:
    url: jdbc:postgresql://169.58.62.99:5432/defensoria_db
    username: postgres
    password: Temporaloct2026

# Para historico_db (solo historico-service)
spring:
  datasource:
    url: jdbc:postgresql://169.58.62.99:5433/historico_db
    username: postgres
    password: Temporaloct2026
```

### Actualizar Config-files Automáticamente

Para actualizar todos los config-files con la nueva IP, ejecuta este script en el servidor backend:

```bash
# Crear script de actualización
cat > /tmp/actualizar-bd-ip.sh << 'EOF'
#!/bin/bash
IP_NUEVA="169.58.62.99"
CONFIG_DIR="/apps/aplicaciones/defensoria/back/config-files"

# Buscar y reemplazar la IP vieja en todos los archivos YAML
find "$CONFIG_DIR" -name "*.yml" -exec sed -i "s|2.25.78.22:5432|$IP_NUEVA:5432|g" {} \;
find "$CONFIG_DIR" -name "*.yaml" -exec sed -i "s|2.25.78.22:5432|$IP_NUEVA:5432|g" {} \;

echo "✅ IPs actualizadas a $IP_NUEVA en todos los config-files"
EOF

chmod +x /tmp/actualizar-bd-ip.sh
sudo /tmp/actualizar-bd-ip.sh
```

## Verificación de Seeds

Después de levantar los contenedores, verificar que los seeds se ejecutaron:

```bash
# Conectar a defensoria_db
sudo bash podman-compose-bd.sh connect-defensoria

# Dentro de psql:
SELECT COUNT(*) FROM dependencias;  -- Debe ser 208
SELECT COUNT(*) FROM preguntas_chatbot;  -- Debe ser el número de preguntas
\q
```

## Respaldo Automático

Para configurar respaldo automático diario (opcional):

```bash
# Editar crontab
sudo crontab -e

# Agregar: respaldo diario a las 4 AM
0 4 * * * /apps/aplicaciones/defensoria/database/podman-compose-bd.sh backup
```

## Troubleshooting

### Los seeds no se ejecutan

Los seeds solo se ejecutan la PRIMERA vez. Si necesitas volver a ejecutarlos:

```bash
# Eliminar contenedor y datos
sudo bash podman-compose-bd.sh down-defensoria
sudo rm -rf /apps/data/postgresql/defensoria_db
sudo bash podman-compose-bd.sh up-defensoria
```

### Un contenedor no inicia

Ver logs:

```bash
sudo bash podman-compose-bd.sh logs defensoria-db
```

Posibles causas:
- Puerto ya en uso
- Permisos incorrectos en directorios
- Memoria insuficiente

### No puedo conectar desde el backend

Verificar:
1. Que el contenedor esté corriendo: `sudo bash podman-compose-bd.sh status`
2. Que el firewall permita conexiones desde IP_BACKEND
3. Que la contraseña coincida
