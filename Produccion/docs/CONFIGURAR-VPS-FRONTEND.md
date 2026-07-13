# Configurar la VPS del Frontend (segunda VPS)

VPS nueva: `srv1824254.hstgr.cloud` (KVM 1) — **IP pública: `2.25.64.47`**.
VPS backend (sin cambios de IP): `2.25.78.22`.

Idea general: esta VPS pasa a ser la que da la cara a Internet (dominio + HTTPS + estáticos de
Angular) y reenvía `/api/*` al backend en `2.25.78.22`. El backend deja de exponer Nginx al
público — solo lo consume esta VPS nueva, y solo por IP autorizada.

> Nota: no hay confirmación de que Hostinger ofrezca red privada/VPC entre VPS en el plan
> KVM 1, así que este runbook usa IP pública + firewall por origen, que siempre funciona.

## Estado: ✅ comandos listos para ejecutar (pendiente de correrlos en las VPS)

## 1. Backend (2.25.78.22) — quitar el nginx/front viejo

```bash
ssh root@2.25.78.22
podman stop defensoria-nginx
podman rm defensoria-nginx
rm -rf /apps/aplicaciones/defensoria/front/*
ufw status numbered   # revisa antes de tocar reglas
```

Borra con `ufw delete <número>` cualquier regla `8083/8084/8085/5432/tcp ALLOW Anywhere`, y deja:

```bash
ufw allow from 2.25.64.47 to any port 8083 proto tcp
ufw allow from 2.25.64.47 to any port 8084 proto tcp
ufw allow from 2.25.64.47 to any port 8085 proto tcp
ufw allow from 2.25.64.47 to any port 5432 proto tcp
ufw allow 22/tcp
ufw delete allow 80/tcp   # si existía
ufw enable
ufw status verbose
```

⚠️ **Podman/Docker publican puertos vía reglas de `iptables`/`nftables` que en muchas distros
se evalúan ANTES que las de `ufw`** — es un problema conocido de estas herramientas, no un
error tuyo. Eso significa que `ufw` por sí solo puede no bastar para bloquear el tráfico a
los contenedores. Réplica la misma restricción (origen `2.25.64.47`, puertos 8083-8085, 5432)
en **hPanel → VPS backend → Security → Firewall** — ese filtra el tráfico antes de llegar a
la VM y es la capa en la que realmente confiar.

## 2. VPS nueva (2.25.64.47) — instalar Podman + Nginx

```bash
ssh root@2.25.64.47
apt update && apt upgrade -y
apt install -y podman
mkdir -p /apps/aplicaciones/defensoria/front
mkdir -p /apps/aplicaciones/defensoria/nginx/config
```

Config de Nginx (igual que la que ya tenías — sigue apuntando al backend, eso no cambia):

```bash
cat > /apps/aplicaciones/defensoria/nginx/config/defensoria.conf << 'EOF'
server {
    listen 80;
    server_name defensoria-escom.ddns.net;

    location / {
        root /usr/share/nginx/html;
        index index.html index.htm;
        try_files $uri $uri/ /index.html;
    }

    location /api/auth/ {
        proxy_pass http://2.25.78.22:8083;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location /api/quejoso/ {
        proxy_pass http://2.25.78.22:8084;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location /api/notificaciones/ {
        proxy_pass http://2.25.78.22:8085;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
EOF
```

Script de despliegue (mismo patrón que ya usabas en el backend):

```bash
cat > /apps/aplicaciones/defensoria/nginx/podman-ngnix.sh << 'EOF'
#!/bin/bash
BASE_DIR="/apps/aplicaciones/defensoria"
CONTAINER_NAME="defensoria-nginx"
PORT=80
case "$1" in
    up)
        if [ "$(podman ps -aq -f name=^${CONTAINER_NAME}$)" ]; then
            podman stop "$CONTAINER_NAME" 2>/dev/null
            podman rm "$CONTAINER_NAME" 2>/dev/null
        fi
        podman run -d \
          --name "$CONTAINER_NAME" \
          -p ${PORT}:80 \
          -v "$BASE_DIR/nginx/config/defensoria.conf:/etc/nginx/conf.d/default.conf:Z" \
          -v "$BASE_DIR/front:/usr/share/nginx/html:Z" \
          docker.io/library/nginx:alpine
        podman ps -f name="$CONTAINER_NAME"
        ;;
    down)
        podman stop "$CONTAINER_NAME" 2>/dev/null
        podman rm "$CONTAINER_NAME" 2>/dev/null
        ;;
    *)
        echo "Uso: $0 {up|down}"
        exit 1
        ;;
esac
EOF
chmod +x /apps/aplicaciones/defensoria/nginx/podman-ngnix.sh
```

Placeholder mientras no exista el build real de Angular:

```bash
cat > /apps/aplicaciones/defensoria/front/index.html << 'EOF'
<h1>Defensoria - Frontend en construcción</h1>
EOF

cd /apps/aplicaciones/defensoria/nginx
sh podman-ngnix.sh up
podman ps -f name=defensoria-nginx
```

Firewall de esta VPS (aquí 80/443 sí deben ser públicos):

```bash
ufw allow 22/tcp
ufw allow 80/tcp
ufw allow 443/tcp
ufw enable
ufw status verbose
```

## 3. Verificación (antes de tocar el DNS)

```bash
curl -I http://2.25.64.47/                 # 200, sirve el placeholder
curl -I http://2.25.64.47/api/auth/login   # respuesta del backend (405/400), NO timeout
```

Si el segundo falla con timeout: el firewall del backend (paso 1) está bloqueando —
revísalo antes de seguir.

## 4. DNS

En No-IP, apunta `defensoria-escom.ddns.net` → `2.25.64.47` (antes apuntaba a `2.25.78.22`).
Espera unos minutos de propagación.

## 5. Build y copia del frontend Angular (cuando exista)

```bash
ng build --configuration production
scp -r dist/<nombre-app>/browser/* root@2.25.64.47:/apps/aplicaciones/defensoria/front/
```
No hace falta recrear el contenedor de Nginx para que tome los archivos nuevos (es un volumen
montado) — solo si cambias `defensoria.conf`, ahí sí corre de nuevo `sh podman-ngnix.sh up`.

## 6. HTTPS (Certbot) — después de que el DNS ya resuelva a esta VPS

```bash
apt install -y certbot
certbot certonly --webroot -w /apps/aplicaciones/defensoria/front -d defensoria-escom.ddns.net
```

Luego actualiza `defensoria.conf` para escuchar en 443 con los certificados generados, y monta
`/etc/letsencrypt` como volumen de solo lectura en el contenedor de Nginx (el `podman-ngnix.sh`
de arriba habría que extenderlo con ese volumen adicional cuando lleguemos a este paso).
