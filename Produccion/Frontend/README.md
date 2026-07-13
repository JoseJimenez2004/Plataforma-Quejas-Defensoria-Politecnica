# Frontend — Portal de Quejas DDP (Angular)

Reconstrucción del frontend a partir del scaffold roto que se tenía (solo login + dashboard
vacío sin ruta). Este proyecto no se pudo compilar ni probar en el entorno donde se escribió
(sin acceso a los registros de npm) — **primer paso obligatorio: instalar dependencias y
compilar en tu máquina o en la VPS para confirmar que no hay errores.**

## Instalación y verificación

```bash
cd Frontend
npm install
ng build
```

Si `ng build` marca algún error, cópialo tal cual para corregirlo — el código se escribió a
mano sin poder ejecutarlo, así que puede haber algún detalle de sintaxis o de tipos que solo
el compilador de TypeScript/Angular detecta.

Para desarrollo local contra el backend real (usa `proxy.conf.json`, ya apuntando a
`2.25.78.22:8083/8084/8085`):

```bash
ng serve
```

Para generar el build de producción que se copia a la VPS del frontend (`front/` en el
servidor, servido por Nginx):

```bash
ng build --configuration production
# copiar el contenido de dist/defensoria-front/browser/* a front/ en la VPS
```

## Qué se corrigió

- **Bug bloqueante original**: el login navegaba a `/dashboard`, ruta que nunca existió en
  `app.routes.ts`. Ahora el panel autenticado vive en `/panel` y las rutas están completas.
- Interceptor JWT: adjunta el token a toda petición `/api/**`, y si el backend responde
  401/403 (token vencido), cierra sesión y regresa al login en vez de dejar un error genérico.
- Guard de autenticación (`authGuard`) protegiendo todo `/panel/**`.

## Qué SÍ está conectado al backend real

- Login (`/api/auth/login`)
- Recuperar contraseña (`/api/auth/solicitar-codigo` + `/api/auth/reset-password`)
- Activar cuenta (`/api/auth/activar-cuenta`)
- Consultar folio (`/api/quejoso/quejas/validar-folio`)
- Nueva Queja dentro del panel (`/api/quejoso/quejas/registrar`)

## Qué es solo UI con datos de ejemplo (falta backend)

Ver `docs/HALLAZGOS.md` en la raíz del proyecto para el detalle completo, en corto:

- **Registro público de quejas** (`/queja/registro`): el backend actual exige JWT para
  registrar una queja, pero el flujo de "presentar queja sin cuenta" del diseño requiere que
  se pueda hacer de forma anónima. Hace falta un endpoint público nuevo en `queja-service`.
- **Mis Quejas / Detalle / Edición**: no existe un `GET` para listar o consultar el detalle de
  las quejas de un usuario en `queja-service`.
- **Acuerdos de Conciliación**: no existe ningún backend para esto todavía.
- **Centro de Notificaciones**: `notificaciones-service` solo envía correos, no expone un
  historial consultable por usuario.
- **Mi Perfil**: el login solo regresa `nombre` + `token`, no hay un endpoint `/api/auth/me`
  con boleta/unidad académica/etc., ni forma de actualizar correo personal/teléfono.

Estas vistas están construidas (para que se pueda revisar el diseño y flujo), pero con datos
de ejemplo claramente marcados en el código (`TODO(backend)`) y en la propia pantalla.
