# Plataforma de Quejas — Defensoría Politécnica

Proyecto de la Defensoría de los Derechos Politécnicos (DDP): portal público para presentar
y dar seguimiento a quejas, backend en microservicios (Java/Spring Boot) y proxy Nginx.

Este README es el punto de entrada. El detalle está en `docs/`.

## Estructura del proyecto

```
Produccion/
├── Backend/                  # Los 4 microservicios (Spring Boot / Maven) + su config de producción
│   ├── auth.service/         # Login, JWT, activación de cuenta, recuperación de contraseña
│   ├── queja-service/        # Registro y consulta de quejas, folios, evidencias
│   ├── notificaciones-service/ # Envío de correos (activación, códigos, avisos)
│   ├── catalogo-service/     # Catálogos institucionales (dependencias del IPN, puerto 8086)
│   ├── config-files/         # application.yml "de producción" por servicio (montados como volumen en el VPS)
│   └── podman-compose.sh     # Build + despliegue de los 4 microservicios en Podman
├── Frontend/                  # Proyecto Angular (código fuente) — reconstruido de cero
│   └── src/app/...            # ver Frontend/README.md para instalar/compilar/desplegar
├── front/                    # Destino del BUILD estático (lo que sirve Nginx), aún vacío
│   ├── artifact/
│   └── config-files/
├── nginx/                    # Proxy inverso: sirve el front estático y enruta /api/* al backend
│   ├── config/defensoria.conf
│   └── podman-ngnix.sh
├── cONTEXTOQUEJOSO/          # Material de referencia original (wireframes, specs, accesos)
└── docs/                     # Documentación de arquitectura, despliegue, hallazgos y migración
    ├── ARQUITECTURA.md
    ├── DESPLIEGUE.md
    ├── HALLAZGOS.md
    └── MIGRACION-2-VPS.md
```

## Estado actual (2026-07-12)

- **Backend**: funcional. Ahora son **4 microservicios** corriendo en Podman sobre la VPS
  2.25.78.22 (Hostinger), con Postgres 16 en el mismo servidor, cada uno con su propia imagen
  Docker: `auth-service` (8083), `quejas-service` (8084), `notificaciones-service` (8085) y el
  nuevo `catalogo-service` (8086, catálogo de dependencias del IPN). Puertos 8083-8086
  restringidos por IP de origen (solo accesibles desde la VPS frontend). Los 4 exponen
  Swagger/OpenAPI 3 en `/swagger-ui.html`.
- **Frontend**: reconstruido en `Frontend/` — todas las pantallas del PDF están construidas,
  compiladas y desplegadas. Login/recuperar/activar/consultar-folio/nueva-queja conectados al
  backend real. Rediseño visual institucional (inspirado en ipn.mx/defensoria) aplicado al
  header, footer e Inicio — pendiente de confirmación final tras revisión del usuario. Las
  vistas sin backend (Mis Quejas, Conciliación, Notificaciones, parte de Mi Perfil) siguen
  usando datos de ejemplo, marcados en el código. Ver `Frontend/README.md` y `docs/HALLAZGOS.md`.
- **Infraestructura**: migración a 2 VPS **completada**. Arquitectura de 2 contenedores en el
  frontend (`defensoria-web` sirviendo el Angular compilado + `router-nginx` como proxy
  inverso), HTTPS con Certbot (renovación automática), dominio apuntando correctamente. Ver la
  entrada "Puerto 80 desbloqueado y HTTPS con Certbot" en `docs/CAMBIOS.md` para el detalle.
- **Pendiente real**: 5 endpoints de backend documentados en `docs/HALLAZGOS.md` (registro
  público de quejas, listado/detalle de quejas, conciliación, historial de notificaciones,
  `/api/auth/me`) — no iniciados.

## Accesos rápidos (ver `cONTEXTOQUEJOSO/` para el detalle completo)

| Recurso | Valor |
|---|---|
| Dominio | https://defensoria-escom.ddns.net (No-IP + Certbot, HTTP redirige a HTTPS) |
| IP pública VPS backend + BD | 2.25.78.22 |
| IP pública VPS frontend | 2.25.64.47 |
| Puertos backend | auth 8083 · quejas 8084 · notificaciones 8085 · catálogo 8086 (solo accesibles desde 2.25.64.47) |
| Base de datos | Postgres 16, puerto 5432, `defensoria_db` |
| Frontend | `defensoria-web` (Nginx interno :8090) + `router-nginx` (proxy público :80/:443) |

## Migración a 2 VPS — completada

Backend y frontend separados en 2 VPS, con HTTPS funcionando. Detalle paso a paso de cómo se
llegó ahí en `docs/CONFIGURAR-VPS-FRONTEND.md` y en el historial de `docs/CAMBIOS.md`.
