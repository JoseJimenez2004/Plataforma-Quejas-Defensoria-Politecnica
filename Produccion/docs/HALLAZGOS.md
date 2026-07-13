# Hallazgos y pendientes

Este documento solo **registra** lo encontrado al revisar el proyecto. No se modificó
código todavía (por decisión explícita: primero orden y documentación).

## 🔴 Bloqueante — por qué "no funciona" el frontend

`app.routes.ts` solo define dos rutas, ambas apuntando al login:

```ts
export const routes: Routes = [
  { path: '', component: LoginComponent },
  { path: 'login', component: LoginComponent }
];
```

Pero `LoginComponent.onLogin()` hace `this.router.navigate(['/dashboard'])` al autenticar
correctamente. Como no existe una ruta `dashboard`, Angular no tiene a dónde navegar — por
eso, tras un login exitoso, aparentemente "no pasa nada" (o el router marca error en consola).
El componente `Dashboard` sí existe (`dashboard.ts/html/scss`) pero nunca se registró en las rutas.

## 🟡 Alcance — falta casi todo el frontend

Lo único que existe es el scaffold por defecto de Angular 21 + un login funcional a medias +
un dashboard vacío. Comparado contra `cONTEXTOQUEJOSO/vistasquejoso.pdf`, faltan por construir:

- Página de inicio pública (búsqueda de folio, accesos a "Presentar queja" / "Iniciar sesión").
- Formulario de registro de queja (incluye modal de datos del tutor para menores de edad).
- Pantalla de folio generado + oferta de crear cuenta de seguimiento.
- Activación de cuenta con folio + configuración de contraseña.
- Login del portal de seguimiento (distinto al mock actual) + recuperar contraseña.
- Panel autenticado: Resumen, Mis Quejas (listado, detalle con línea de tiempo, edición),
  Nueva Queja, Acuerdos de Conciliación (aceptar/rechazar), Centro de Notificaciones, Mi Perfil.
- Servicios Angular para consumir `queja-service` y `notificaciones-service` (hoy solo existe
  `AuthService`, apuntando a `/api/auth`).
- Interceptor/guard para adjuntar el JWT a las peticiones y proteger rutas del panel.

## 🟢 Cosas que SÍ están bien resueltas en el backend

- Puertos: `server.port` de cada `config-files/*.yml` (8083/8084/8085) coincide exactamente
  con lo que espera `podman-compose.sh` (`get_port()`) y con lo que corre en `podman ps`.
- `jwt.secret` es idéntico en `auth-service` y `queja-service` en producción — necesario para
  que `queja-service` pueda validar tokens emitidos por `auth-service`. Confirmado.
- Nginx (`defensoria.conf`) ya enruta correctamente `/api/auth/`, `/api/quejoso/` y
  `/api/notificaciones/` a cada puerto, y sirve el Angular con `try_files ... /index.html`
  (necesario para el router de Angular).

## 🟡 Detalles menores a revisar (no bloquean, pero conviene limpiar)

1. `Backend/auth.service/src/main/resources/application.yaml` tiene un typo:
   `name: auth.serviceç` (carácter suelto). Es el nombre de la app para logs/metrics, no
   afecta el funcionamiento, pero se ve descuidado.
2. `front/` (local y lo que monta Nginx en el VPS) está vacío — es esperable si el `dist/`
   de Angular nunca se copió, pero hay que recordarlo en el primer despliegue real.
3. `nginx/config/defensoria.conf` apunta a `2.25.78.22` (IP pública) en los 3 `proxy_pass`,
   en vez de usar red interna de Podman o `localhost`. Funciona hoy porque todo está en el
   mismo host, pero es un patrón frágil (depende de hairpin NAT) — más relevante aún cuando
   se separen las VPS (ver `MIGRACION-2-VPS.md`).
4. ~~No hay HTTPS~~ **Resuelto (2026-07-12)**: `router-nginx` ya sirve HTTPS con certificado
   de Let's Encrypt (Certbot, método webroot, renovación automática) y redirige `:80` a
   `:443`. Ver detalle en `docs/CAMBIOS.md`.
5. ~~No se ha revisado el estado real de `ufw`/`firewalld`~~ **Resuelto**: `ufw` inactivo,
   `iptables` en accept en ambas VPS; el firewall administrado de hPanel es el que realmente
   controla el acceso y ya está confirmado/sincronizado en las dos.
6. `proxy.conf.json` (usado por `ng serve` en desarrollo local) no está incluido en lo que
   se compartió — no bloquea producción (Nginx reemplaza esa función), pero conviene tenerlo
   para que el equipo pueda desarrollar localmente sin CORS.

## 🔴 Frontend reconstruido — gaps de backend descubiertos al conectar las vistas reales

Se reconstruyó el frontend completo en `Frontend/` (proyecto Angular nuevo, el anterior era
solo el scaffold con login roto). Al conectar cada vista con los endpoints reales del backend
se encontraron varias funciones del diseño (`vistasquejoso.pdf`) que **el backend actual no
soporta todavía**:

1. **Registro público/anónimo de quejas**: el diseño permite presentar una queja sin tener
   cuenta (recolectando datos del quejoso en el mismo formulario) y luego crear la cuenta con
   el folio recibido. Pero `queja-service` solo expone `POST /api/quejoso/quejas/registrar`
   **protegido por JWT**, que además obtiene el correo del token de sesión (no de un campo del
   formulario) y solo acepta `motivo` + `descripcion` + `archivo` (no nombre/apellidos/fecha de
   nacimiento/datos del denunciado como pide el wireframe). Esto es una contradicción de
   arquitectura: no se puede registrar la primera queja sin iniciar sesión, pero tampoco se
   puede iniciar sesión sin una cuenta, y las cuentas solo se activan con un folio que ya debía
   existir. **Se necesita un endpoint público nuevo** (o hacer `/registrar` público y aceptar
   correo/nombre directamente en el body cuando no hay JWT).
2. **Listar "Mis Quejas" y ver detalle/editar una queja**: no existe ningún `GET` en
   `queja-service` para esto — solo `validar-folio` (devuelve `true/false`, no el detalle) y
   `registrar`. Faltan como mínimo: `GET /api/quejoso/quejas?correo=...` (listado) y
   `GET /api/quejoso/quejas/{folio}` (detalle), más lógica de estatus de trámite (hoy
   `Queja` no tiene ningún campo de estatus, solo se guarda y ya).
3. **Acuerdos de Conciliación**: no existe ningún backend para esto — ni modelo de datos, ni
   endpoints, ni lógica. Habría que diseñarlo desde cero.
4. **Centro de Notificaciones**: `notificaciones-service` solo tiene `POST /enviar` para mandar
   un correo — no hay forma de consultar un historial de avisos por usuario. Falta un modelo de
   "notificación" persistida y un endpoint de listado.
5. **Mi Perfil**: el login (`AuthResponseModel`) solo regresa `token` + `nombre`. No hay un
   endpoint tipo `GET /api/auth/me` para traer boleta/unidad académica/correo institucional, ni
   uno para actualizar correo personal/teléfono/datos de tutor.

El frontend ya está construido contra estas limitaciones de forma honesta: lo que sí conecta
(login, recuperar contraseña, activar cuenta, consultar folio, nueva queja dentro del panel)
funciona contra los endpoints reales; lo que no tiene backend está construido como UI con datos
de ejemplo, marcado con comentarios `TODO(backend)` en el código y un aviso visible en cada
pantalla afectada. Ver `Frontend/README.md` para el detalle de qué es real y qué es ejemplo.

**Además**: no se pudo correr `npm install` ni `ng build` en el entorno donde se escribió este
frontend (sin acceso a los registros de npm) — falta que alguien lo compile por primera vez y
reporte cualquier error de TypeScript/Angular que aparezca.
