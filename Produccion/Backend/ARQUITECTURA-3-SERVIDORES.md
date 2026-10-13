> ⚠️ **Obsoleto.** Ver `docs/VERSION-FINAL-TT.md` (contraseña, nombre de la base histórica y manejo de seeds cambiaron).

# Arquitectura de 3 Servidores - Configuración Completa

## 📊 Resumen de IPs

| Servidor | IP | Propósito | Puertos Principales |
|----------|-----|-----------|-------------------|
| **Servidor BD** | 169.58.62.99 | Bases de datos Postgres | 5432 (defensoria_db), 5433 (historico_db) |
| **Servidor Backend** | 156.67.26.73 | Microservicios Spring Boot | 8082-8092 (9 microservicios) |
| **Servidor Frontend** | 169.58.62.111 | Frontends Angular + Nginx | 80 (HTTP), 443 (HTTPS), 8090, 22346, 22347 |

---

## 🏗️ Arquitectura Completa

```
┌─────────────────────────────────────────────────────────────────┐
│                  Servidor BD (169.58.62.99)                    │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  defensoria-db (Postgres 16) - Puerto 5432           │  │
│  │  - defensoria_db (base principal)                     │  │
│  │  - Memoria: 1GB                                        │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  historico-db (Postgres 16) - Puerto 5433            │  │
│  │  - historico_db (base de histórico)                   │  │
│  │  - Memoria: 512MB                                      │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                            ▲
                            │ Firewall: solo desde 156.67.26.73
                            │
┌─────────────────────────────────────────────────────────────────┐
│              Servidor Backend (156.67.26.73)                   │
│                                                                  │
│  auth-service:8083         quejas-service:8084                │
│  notificaciones:8085       catalogo-service:8086               │
│  admin-service:8087         revision-service:8088               │
│  chatbot-service:8089        primer-contacto:8082               │
│  subdefensoria:8091         historico-service:8092              │
│                                                                  │
│  Memoria total optimizada: ~1.5GB                              │
└─────────────────────────────────────────────────────────────────┘
                            ▲
                            │ Firewall: solo desde 169.58.62.111
                            │
┌─────────────────────────────────────────────────────────────────┐
│              Servidor Frontend (169.58.62.111)                  │
│                                                                  │
│  router-nginx (HTTPS 443, HTTP 80)                             │
│    ├── / → defensoria-web:8090 (quejoso)                      │
│    ├── /admin/ → admin-web:8091                                │
│    └── /revision/ → revision-web:8092                          │
│                                                                  │
│  Proxy a backend: /api/* → 156.67.26.73:8083-8092             │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔧 Configuración por Servidor

### Servidor BD (169.58.62.99)

#### Archivos a copiar:
```bash
scp Backend/podman-compose-bd.sh root@169.58.62.99:/apps/aplicaciones/defensoria/database/
scp -r Backend/database-scripts root@169.58.62.99:/apps/aplicaciones/defensoria/database-scripts/
```

#### Configuración de firewall:
```bash
# Permitir solo desde el servidor backend
sudo ufw allow from 156.67.26.73 to any port 5432
sudo ufw allow from 156.67.26.73 to any port 5433
sudo ufw enable
```

#### Levantar contenedores:
```bash
ssh root@169.58.62.99
cd /apps/aplicaciones/defensoria/database
sudo bash podman-compose-bd.sh up
```

#### Credenciales:
- Usuario: `postgres`
- Password: `Temporaloct2026`
- BD principal: `defensoria_db` (puerto 5432)
- BD histórico: `historico_db` (puerto 5433)

---

### Servidor Backend (156.67.26.73)

#### Archivos a copiar:
```bash
# Config-files ya actualizados con IP 169.58.62.99
scp -r Backend/config-files root@156.67.26.73:/apps/aplicaciones/defensoria/back/
# JARs
scp -r Backend/artifact root@156.67.26.73:/apps/aplicaciones/defensoria/back/
# Scripts
scp Backend/podman-compose.sh root@156.67.26.73:/apps/aplicaciones/defensoria/back/
scp Backend/Dockerfile root@156.67.26.73:/apps/aplicaciones/defensoria/back/
```

#### Configuración de firewall:
```bash
# Permitir solo desde el servidor frontend
sudo ufw allow from 169.58.62.111 to any port 8082:8092
sudo ufw enable
```

#### Cadena de conexión en config-files:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://169.58.62.99:5432/defensoria_db
    username: postgres
    password: Temporaloct2026
```

#### Levantar microservicios:
```bash
ssh root@156.67.26.73
cd /apps/aplicaciones/defensoria/back
sudo bash podman-compose.sh up
```

---

### Servidor Frontend (169.58.62.111)

#### Configuración de Nginx:

```nginx
upstream backend {
    server 156.67.26.73:8083;   # auth-service
    server 156.67.26.73:8084;   # quejas-service
    server 156.67.26.73:8085;   # notificaciones-service
    server 156.67.26.73:8086;   # catalogo-service
    server 156.67.26.73:8087;   # admin-service
    server 156.67.26.73:8088;   # revision-service
    server 156.67.26.73:8089;   # chatbot-service
    server 156.67.26.73:8082;   # primer-contacto-service
    server 156.67.26.73:8091;   # subdefensoria-service
    server 156.67.26.73:8092;   # historico-service
}

server {
    listen 80;
    server_name defensoria-escom.ddns.net;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl;
    server_name defensoria-escom.ddns.net;
    
    ssl_certificate /etc/letsencrypt/live/defensoria-escom.ddns.net/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/defensoria-escom.ddns.net/privkey.pem;
    
    # Frontend del quejoso
    location / {
        proxy_pass http://localhost:8090;
    }
    
    # Frontend de administración
    location /admin/ {
        proxy_pass http://localhost:8091/;
    }

    # Frontend de revisión
    location /revision/ {
        proxy_pass http://localhost:8092/;
    }
    
    # Proxy a microservicios backend
    location /api/auth/ {
        proxy_pass http://156.67.26.73:8083/api/auth/;
    }
    
    location /api/quejoso/ {
        proxy_pass http://156.67.26.73:8084/api/quejoso/;
    }
    
    location /api/notificaciones/ {
        proxy_pass http://156.67.26.73:8085/api/notificaciones/;
    }
    
    location /api/catalogos/ {
        proxy_pass http://156.67.26.73:8086/api/catalogos/;
    }
    
    location /api/admin/ {
        proxy_pass http://156.67.26.73:8087/api/admin/;
    }
    
    location /api/revision/ {
        proxy_pass http://156.67.26.73:8088/api/revision/;
    }
    
    location /api/chatbot/ {
        proxy_pass http://156.67.26.73:8089/api/chatbot/;
    }
    
    client_max_body_size 100M;
}
```

#### Configuración de firewall:
```bash
# Permitir tráfico público en 80/443
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
```

---

## ✅ Checklist de Despliegue

### Servidor BD (169.58.62.99)
- [ ] Copiar podman-compose-bd.sh
- [ ] Copiar database-scripts/
- [ ] Configurar firewall (permitir desde 156.67.26.73)
- [ ] Levantar contenedores: `sudo bash podman-compose-bd.sh up`
- [ ] Verificar seeds: `SELECT COUNT(*) FROM dependencias;` (debe ser 208)
- [ ] Verificar seeds: `SELECT COUNT(*) FROM preguntas_chatbot;`

### Servidor Backend (156.67.26.73)
- [ ] Copiar config-files (ya actualizados con IP 169.58.62.99)
- [ ] Copiar JARs (artifact/)
- [ ] Copiar podman-compose.sh y Dockerfile
- [ ] Configurar firewall (permitir desde 169.58.62.111)
- [ ] Levantar microservicios: `sudo bash podman-compose.sh up`
- [ ] Verificar conexión a BD: probar login

### Servidor Frontend (169.58.62.111)
- [ ] Configurar Nginx con IPs correctas
- [ ] Desplegar frontends Angular
- [ ] Configurar HTTPS con Certbot
- [ ] Configurar firewall (permitir 80/443)
- [ ] Verificar acceso público

---

## 🔍 Verificación Final

### 1. Verificar BD desde Backend
```bash
# En servidor backend (156.67.26.73)
psql -h 169.58.62.99 -p 5432 -U postgres -d defensoria_db
```

### 2. Verificar Backend desde Frontend
```bash
# En servidor frontend (169.58.62.111)
curl http://156.67.26.73:8083/api/auth/me
```

### 3. Verificar Frontend público
```bash
# Desde cualquier lugar
curl -I https://defensoria-escom.ddns.net
curl -I https://defensoria-escom.ddns.net/api/catalogos/dependencias
```

---

## 📝 Resumen de Configuración

| Componente | IP | Puerto | Usuario | Password |
|------------|-----|--------|---------|----------|
| defensoria_db | 169.58.62.99 | 5432 | postgres | Temporaloct2026 |
| historico_db | 169.58.62.99 | 5433 | postgres | Temporaloct2026 |
| auth-service | 156.67.26.73 | 8083 | - | - |
| quejas-service | 156.67.26.73 | 8084 | - | - |
| notificaciones-service | 156.67.26.73 | 8085 | - | - |
| catalogo-service | 156.67.26.73 | 8086 | - | - |
| admin-service | 156.67.26.73 | 8087 | - | - |
| revision-service | 156.67.26.73 | 8088 | - | - |
| chatbot-service | 156.67.26.73 | 8089 | - | - |
| primer-contacto-service | 156.67.26.73 | 8082 | - | - |
| subdefensoria-service | 156.67.26.73 | 8091 | - | - |
| historico-service | 156.67.26.73 | 8092 | - | - |
| router-nginx | 169.58.62.111 | 80/443 | - | - |
