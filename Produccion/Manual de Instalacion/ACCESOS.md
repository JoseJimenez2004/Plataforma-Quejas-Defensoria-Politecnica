# Accesos — Plataforma de Quejas DDP

Referencia rápida de servidores, cuentas y URLs de la instalación de producción.

> ⚠️ **Contraseñas:** se dejan como `<…>` para que las llenes tú. Si escribes las reales en este archivo, no lo compartas ni lo subas a repositorios ni al Project; lo más seguro es dejarlas solo en la bóveda de Termius o en un gestor de contraseñas.

## 1. Servidores (VPS Contabo, Ubuntu 24.04.5 LTS)

| Host en Termius | Rol | IP pública | Hostname | Usuario | Puerto SSH | Contraseña |
|---|---|---|---|---|---|---|
| FRONTEND DEFENSORIA | Frontend (router-nginx + 5 frontends) | 169.58.62.111 | vmi3633026 | root | 22 | `<CONTRASEÑA_ROOT_FRONTEND>` |
| BACKEND DEFENSORIA | Backend (10 microservicios + modelo IA) | 156.67.26.73 | vmi3633021 | root | 22 | `<CONTRASEÑA_ROOT_BACKEND>` |
| BASE DEFENSORIA | Base de datos (PostgreSQL 16) | 169.58.62.99 | vmi3633025 | root | 22 | `<CONTRASEÑA_ROOT_BD>` |

Cliente SSH: **Termius** (vault *Personal*). Los tres hosts usan autenticación por contraseña (sin llaves SSH).

## 2. Dominio y certificado

| Recurso | Valor |
|---|---|
| Dominio | https://defensoria-escom.ddns.net |
| Proveedor DNS | No-IP (host gratuito, confirmar cada 30 días) |
| Apunta a | 169.58.62.111 (VPS Frontend) |
| Certificado | Let's Encrypt (Certbot, método webroot) |
| Correo de Let's Encrypt | `<CORREO_CONTACTO>` |
| Cuenta No-IP | `<USUARIO_NOIP>` / `<CONTRASEÑA_NOIP>` |

## 3. Base de datos

| Dato | Valor |
|---|---|
| Contenedor | `defensoria-db` (VPS 169.58.62.99) |
| Puerto | 5432 (solo accesible desde 156.67.26.73) |
| Base | `defensoria_db` |
| Usuario | `postgres` |
| Contraseña | `<CONTRASEÑA_BD>` — debe ser igual en `podman-compose-bd.sh` (`POSTGRES_PASSWORD`) y en `spring.datasource.password` de los config-files |
| Consola SQL | en el VPS BD: `bash podman-compose-bd.sh psql-defensoria` |

Acceso desde tu equipo sin abrir el puerto (túnel SSH, luego DBeaver a `localhost:5434`):
```bash
ssh -L 5434:localhost:5432 root@169.58.62.99
```

## 4. Correo (SMTP)

| Dato | Valor |
|---|---|
| Servidor | smtp.gmail.com:587 |
| Cuenta | josebryanomar2004@gmail.com |
| Contraseña de aplicación | `<CONTRASEÑA_APP_GMAIL>` |
| Usado por | auth-service, notificaciones-service |

## 5. URLs de la plataforma

| Módulo | URL |
|---|---|
| Portal del quejoso / denunciado | https://defensoria-escom.ddns.net/ |
| Login del personal (revisión) | https://defensoria-escom.ddns.net/revision/ |
| Panel de la Defensora | https://defensoria-escom.ddns.net/admin/ |
| Primer Contacto | https://defensoria-escom.ddns.net/primer-contacto/ |
| Subdefensoría | https://defensoria-escom.ddns.net/subdefensoria/ |

Swagger de cada microservicio (solo desde el VPS Backend o por túnel SSH): `http://127.0.0.1:<puerto>/swagger-ui.html`.

## 6. Cuentas de prueba del personal

Se crean con los scripts semilla (`Backend/database-scripts/seeds/`). La contraseña de cada grupo está en el comentario del propio archivo `.sql`. **Desactivarlas antes de la entrega.**

| Correo | Rol | Seed |
|---|---|---|
| admin.test@ipn.mx | ADMIN_SISTEMAS | 03-personal_test.sql |
| recepcionista.test@ipn.mx | RECEPCIONISTA | 03-personal_test.sql |
| primer.contacto.test@ipn.mx | ANALISTA_PRIMER_CONTACTO | 03-personal_test.sql |
| defensor.test@ipn.mx | DEFENSOR | 03-personal_test.sql |
| ana.torres.recep@ipn.mx, luis.gomez.recep@ipn.mx, mariana.castillo.recep@ipn.mx, jorge.rios.recep@ipn.mx | RECEPCIONISTA | 04-recepcionistas.sql |
| alejandra.vega.abogada@ipn.mx, ricardo.nunez.abogado@ipn.mx, sofia.herrera.abogada@ipn.mx, daniel.ortega.abogado@ipn.mx, gabriela.luna.abogada@ipn.mx | SUBDEFENSOR | 05-abogados.sql |

## 7. Puertos por servidor

| VPS | Puertos | Quién puede entrar |
|---|---|---|
| Frontend | 80, 443 | Internet |
| Frontend | 8090–8095 (frontends) | solo local (router-nginx) |
| Backend | 8082–8094 (microservicios) | solo 169.58.62.111 |
| BD | 5432 | solo 156.67.26.73 |
| Los 3 | 22 (SSH) | administración |
