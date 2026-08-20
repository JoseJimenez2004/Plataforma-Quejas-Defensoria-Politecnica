# Arquitectura

> ⚠️ **Nota de actualización**: el diagrama de abajo describe el estado original de 1 sola
> VPS. Desde entonces el proyecto migró a **2 VPS** (backend+BD en `2.25.78.22`, frontend en
> `2.25.64.47` con HTTPS vía Certbot) y se agregó un **4to microservicio, `catalogo-service`
> (puerto 8086)** — ver el detalle real y actualizado en `docs/CAMBIOS.md` (es la bitácora
> que sí se mantiene al día). Este archivo se deja como referencia histórica del diseño
> original de los 3 microservicios + Nginx de 1 sola VPS.

## catalogo-service (puerto 8086) — agregado después de la migración a 2 VPS

- Catálogo de dependencias del IPN (`GET /api/catalogos/dependencias`, `GET
  /api/catalogos/dependencias/{clave}`) — ambos públicos, sin JWT, porque el formulario de
  "Presentar una queja" los necesita incluso antes de que el quejoso tenga cuenta.
- Se decidió como microservicio propio (en vez de vivir dentro de queja-service) pensando en
  que el catálogo pueda crecer y ser consumido por más de un servicio a futuro — ver el
  razonamiento completo en `docs/CAMBIOS.md`.
- Comparte `defensoria_db` (misma instancia de Postgres que los otros 3 servicios) y el mismo
  `jwt.secret`, siguiendo el patrón ya establecido.
- Los 4 microservicios (auth, quejas, notificaciones, catálogo) ahora exponen Swagger/OpenAPI 3
  en `/swagger-ui.html` y `/v3/api-docs` (rutas públicas, sin JWT).

## Diagrama lógico (estado original — 1 VPS, ya no vigente)

```
                        Internet
                           │
                           ▼
        defensoria-escom.ddns.net  (2.25.78.22:80)
                           │
                 ┌─────────┴─────────┐
                 │   Nginx (Podman)  │
                 │  defensoria.conf  │
                 └─────────┬─────────┘
       ┌───────────────────┼────────────────────┐
       │                   │                    │
   / (estático)      /api/auth/*          /api/quejoso/*        /api/notificaciones/*
       │                   │                    │                        │
       ▼                   ▼                    ▼                        ▼
  front/ (Angular)   auth-service:8083     quejas-service:8084     notificaciones-service:8085
                           │                    │                        │
                           └─────────┬──────────┘                        │
                                     ▼                                   │
                            Postgres 16 :5432 (defensoria_db)            │
                                                                          ▼
                                                                  SMTP Gmail (correo)
```

Todo corre hoy en un único VPS Hostinger (contenedores Podman): `defensoria-db`,
`auth-service`, `quejas-service`, `notificaciones-service`, `defensoria-nginx`.

## Servicios

### auth-service (puerto 8083)
- Login (`POST /api/auth/login`) → devuelve JWT (HS256, expira en 1h).
- Recuperación de contraseña por código de 6 dígitos enviado por correo.
- Activación de cuenta "Just-in-Time": el usuario existe formalmente hasta que activa con
  folio + correo (valida contra `queja-service` vía Feign/HTTP:
  `QuejasClient → POST http://quejas-service:8084/api/quejoso/quejas/validar-folio`).
- Entidad `Usuario`: boleta, correo institucional, unidad académica, datos de tutor (para menores).

### queja-service (puerto 8084)
- Registro de quejas (`POST /api/quejoso/quejas/registrar`, protegido por JWT) — genera folio
  `FOL-XXXXXXXX` y guarda evidencia en disco (`storage.location`).
- Validación de folio+correo (`POST /api/quejoso/quejas/validar-folio`, público — lo consume auth-service).
- Filtro `JwtAuthenticationFilter` propio: valida el mismo JWT que emite auth-service
  (comparten el mismo `jwt.secret`, confirmado en las configs de producción).

### notificaciones-service (puerto 8085)
- Único endpoint: `POST /api/notificaciones/enviar` — envío de correo simple vía Gmail SMTP.
- Sin base de datos (autoconfiguración de datasource/JPA deshabilitada a propósito).

### Frontend (Angular) — pendiente
Según `cONTEXTOQUEJOSO/vistasquejoso.pdf`, el portal público debe tener:
inicio con búsqueda de folio + accesos a "Presentar queja" e "Iniciar sesión", formulario de
registro de queja (con modal de tutor para menores), pantalla de folio generado + creación de
cuenta, activación de cuenta con folio, login del portal, recuperar contraseña, y un panel
autenticado con: Resumen, Mis Quejas (listado/edición/detalle con línea de tiempo), Nueva Queja,
Acuerdos de Conciliación (aceptar/rechazar propuesta), Centro de Notificaciones, y Mi Perfil.
Ver `docs/HALLAZGOS.md` para el estado real de lo que existe vs. lo que falta.

## Base de datos

Postgres 16, contenedor `defensoria-db`, BD `defensoria_db`, usuario `postgres`.
Tablas gestionadas por Hibernate (`ddl-auto: update`) desde auth-service (`usuarios`) y
queja-service (`quejas`). `notificaciones-service` no toca la BD.

## Seguridad / CORS

Ambos servicios protegidos (auth y quejas) usan `allowedOriginPatterns: *` +
`allowCredentials(true)` — válido porque la autenticación es por header `Authorization: Bearer`,
no por cookies. Como hoy el front se sirve desde el mismo dominio que el proxy Nginx, el
navegador ni siquiera dispara CORS (mismo origen). Esto cambia si el frontend se muda a otra
VPS/dominio — ver `docs/MIGRACION-2-VPS.md`.
