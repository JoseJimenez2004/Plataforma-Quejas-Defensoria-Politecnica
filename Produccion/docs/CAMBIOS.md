# Registro de cambios

Bitácora cronológica de todo lo que se va documentando, encontrando y ejecutando en el
proyecto. Se actualiza en cada sesión de trabajo.

---

## 2026-07-12

### Documentación inicial del proyecto
- Se conectó la carpeta `Produccion/` completa (antes solo se tenía acceso a `Backend/config-files`).
- Se reveló la estructura real: `Backend/` (3 microservicios Maven + config-files + script de
  despliegue), `front/` (vacío, reservado), `nginx/` (config + script), `cONTEXTOQUEJOSO/`
  (material de referencia original).
- Se crearon `README.md`, `docs/ARQUITECTURA.md`, `docs/DESPLIEGUE.md`, `docs/HALLAZGOS.md`,
  `docs/MIGRACION-2-VPS.md`. Nada existente fue movido, renombrado ni borrado.

### Hallazgo bloqueante (sin corregir todavía)
- `app.routes.ts` del frontend solo define `''` y `'login'` (ambas a `LoginComponent`). El
  login navega a `/dashboard`, ruta que no existe → por eso "no funciona" tras iniciar sesión.
  Pendiente de corregir cuando se retome el código del frontend.

### Compra de la segunda VPS (frontend)
- VPS nueva: `srv1824254.hstgr.cloud` (Hostinger, KVM 1) — **IP pública asignada: `2.25.64.47`**.
- VPS backend sin cambios: `2.25.78.22` (queda solo con los 3 microservicios + Postgres).
- Se creó `docs/CONFIGURAR-VPS-FRONTEND.md` con el runbook completo: quitar Nginx del backend,
  instalar Podman + Nginx en la VPS nueva, firewall en ambas VPS, DNS, HTTPS con Certbot.
- README y este changelog actualizados con la IP real de la VPS frontend.

### Auditoría de firewall en la VPS backend (2.25.78.22) — resuelta
- `ufw status numbered` → **`Status: inactive`**. Ninguna regla de ufw está activa.
- `systemctl status firewalld` → **`Unit firewalld.service could not be found`** (ni siquiera
  está instalado).
- **hPanel → Security → Firewall SÍ tiene un firewall asignado**, con estas reglas:
  1. Accept TCP 80, source: any
  2. Accept TCP 443, source: any
  3. Drop any/any, source: any (catch-all)
- Conclusión: el filtrado real ya lo hace hPanel (a nivel de red, antes de llegar a la VM),
  no ufw. Con el catch-all de "drop everything", los puertos 8083-8085 y 5432 **ya están
  bloqueados a cualquier IP externa** por default — no había ninguna exposición real.
  ufw queda como capa secundaria opcional, no urgente.
- No hace falta abrir 5432 para la VPS del frontend: los microservicios llegan a Postgres
  por la IP pública en "hairpin" (mismo host), tráfico que no pasa por este filtro de
  perímetro, así que ya funciona sin regla adicional.
- Acción tomada: se agregaron 3 reglas Accept TCP (8083, 8084, 8085) con source custom
  `2.25.64.47/32` para permitir que la VPS del frontend llegue a los 3 microservicios.
- **Confirmado**: orden final de reglas = 80 (any) → 443 (any) → 8083/8084/8085
  (`2.25.64.47/32`) → drop any/any (catch-all). Correcto: las reglas nuevas quedan antes
  del catch-all. Sigue sin existir una regla explícita para el 22 (SSH), pero el acceso SSH
  sigue funcionando con normalidad, así que Hostinger evidentemente no filtra su propio canal
  de administración con este firewall — sin acción pendiente aquí.
- ✅ Firewall del backend: **listo**.

### Firewall de hPanel — hallazgo: grupo compartido entre las 2 VPS
- Al revisar el firewall de la VPS frontend (2.25.64.47), aparecen las mismas reglas que se
  configuraron para el backend (incluidas 8083/8084/8085 restringidas a `2.25.64.47/32`) —
  es decir, ambas VPS usan el **mismo grupo de firewall** de hPanel en vez de uno por máquina.
- No es urgente: no hay ningún proceso escuchando en los puertos "de más" en cada máquina
  (el frontend no corre microservicios, el backend ya no correrá Nginx), así que no hay
  exposición real hoy. Sí es una mejora pendiente para más adelante.
- Pendiente (no bloqueante): separar en dos firewalls independientes — Backend: 22 (any) +
  8083/8084/8085 (solo `2.25.64.47/32`) + drop-all, sin 80/443. Frontend: 22/80/443 (any) +
  drop-all, sin 8083-8085.

### Frontend VPS (2.25.64.47) — Nginx desplegado y probado
- Podman instalado, `defensoria.conf` y `podman-ngnix.sh` creados, contenedor
  `defensoria-nginx` corriendo (`0.0.0.0:80->80/tcp`).
- `curl -I http://2.25.64.47/` → `200 OK` (sirve el placeholder).
- `curl -I http://2.25.64.47/api/auth/login` → `500` con headers de Spring — confirma que la
  petición llegó completa hasta `auth-service` (el 500 es el `GlobalExceptionHandler`
  atrapando el `405` de un `HEAD` en un endpoint `POST`, no un fallo de red).
- **Proxy frontend → backend: confirmado funcionando end-to-end.** ✅
- Nginx viejo del backend (2.25.78.22): detenido y eliminado, `front/` limpiado. ✅

### Limpieza de imágenes por servicio (Dockerfile + podman-compose.sh)
- Hallazgo: los 3 microservicios compartían el mismo tag de imagen `defensoria-base-img`
  (cada `build_service` sobreescribía el mismo tag), por eso en `podman ps` los 3 aparecían
  con la misma imagen — confuso y genera imágenes "dangling" en cada rebuild.
- Corregido en el proyecto local: `Backend/Dockerfile` ahora recibe `SERVICE_PORT` como
  build-arg y expone el puerto real (ya no `EXPOSE 8080` fijo); `Backend/podman-compose.sh`
  ahora construye una imagen dedicada por servicio (`defensoria-auth-service`,
  `defensoria-quejas-service`, `defensoria-notificaciones-service`).
- Aplicado en la VPS backend: se reescribieron `Dockerfile` y `podman-compose.sh`
  (nota: hubo que correrlo con `bash podman-compose.sh up`, no `sh`, porque `/bin/sh` en
  esta VPS es `dash` y no soporta arrays de bash — el script ya traía ese shebang mal usado
  desde antes). Resultado confirmado en `podman ps -a`: cada contenedor ahora usa su propia
  imagen (`defensoria-auth-service`, `defensoria-quejas-service`,
  `defensoria-notificaciones-service`), cada una en su puerto correcto.
- Limpieza de imágenes viejas (`defensoria-base-img` + 2 `<none>` dangling) con
  `podman rmi` + `podman image prune -f`.
- ✅ Imágenes por servicio: resuelto.

### 🔴 Bloqueante externo: puerto 80 inalcanzable en la VPS frontend (2.25.64.47)
- Diagnóstico completo hecho, todo descartado de nuestro lado:
  - Nginx corriendo y escuchando en `0.0.0.0:80` (confirmado con `curl localhost` → 200 OK).
  - `ufw status verbose` → 22/80/443 `ALLOW IN Anywhere` (activo).
  - Firewall de hPanel ("Nginx-HTTP") con reglas correctas (22/80/443 Anywhere) — probado
    tanto encendido como **completamente apagado**, sin cambio de comportamiento.
  - Confirmado con 2 fuentes externas independientes (curl desde el backend 2.25.78.22, y
    el checker de terceros yougetsignal.com) que el puerto 80 da timeout / "closed".
  - El puerto 22 (SSH) sí es alcanzable externamente en esa misma IP sin problema — descarta
    que sea un bloqueo total de red/NAT de la IP, apunta a algo específico del puerto 80.
- **Conclusión**: el bloqueo está fuera de lo que controlamos desde la VPS o hPanel →
  Firewall. Pendiente de contactar soporte de Hostinger para que revisen si hay una
  restricción de red a nivel de proveedor sobre el puerto 80 en esta IP/VPS.
- Firewall de hPanel dejado de nuevo **encendido** tras las pruebas.
- Todo lo demás del lado del frontend (Nginx, config, proxy al backend, placeholder) está
  listo y probado localmente — solo falta que se libere el puerto 80 desde el lado de
  Hostinger para poder seguir con DNS + Certbot.
- Soporte de Hostinger (primer contacto) respondió que no ven bloqueo de proveedor. Se hizo
  verificación adicional a pedido suyo:
  - `ss -tlnp` → `LISTEN 0.0.0.0:80` confirmado (no es un problema de binding a loopback).
  - `iptables -t nat` → regla `DNAT ... dpt:80 to:10.88.0.2:80` existe y con tráfico
    procesado (contador no-cero por las pruebas locales) — el contenedor está bien publicado.
  - Conclusión: descartado 100% del lado del host/contenedor. Se le reenvió esta evidencia
    a soporte pidiendo que revisen un filtro de borde/hypervisor específico para HTTP en
    esta IP (no el firewall de hPanel, ya probado apagado).
  - Hallazgo importante: al editar reglas en hPanel → Firewall existe un botón
    **"Synchronize"** obligatorio para que los cambios se apliquen de verdad al servidor —
    no basta con guardar la regla en el panel.
  - Prueba decisiva: se agregó una regla nueva para el puerto 8080 (nunca antes usado),
    se sincronizó, y **también dio timeout** desde 2 orígenes externos — igual que 80 y 443.
    Solo el 22 (SSH) funciona externamente. Esto descarta que sea específico de HTTP/HTTPS:
    es un bloqueo a todo el tráfico entrante que no sea SSH, en esta VPS puntual.
  - Soporte de Hostinger (segunda vuelta) confirma no ver bloqueo de IP ni retención de
    cuenta visible desde sus herramientas, pero coincide en que apunta a un filtro de
    borde de red fuera de su visibilidad. Se le envió un reporte consolidado con toda la
    evidencia (ss, iptables, ufw, pruebas externas) pidiendo escalar a equipo de
    red/infraestructura.
  - Soporte se ofreció a preparar un resumen técnico para abrir revisión interna con el
    equipo que sí tiene visibilidad de la capa de borde/hypervisor — se aceptó.
  - Se probaron los 3 pasos que sugirió el bot (reiniciar VPS, reset SSH, reset firewall):
    ninguno cambió el comportamiento.
  - **Hallazgo decisivo**: se probó un puerto aleatorio (22345) con un Nginx de prueba y
    **sí conectó perfecto** desde 2 orígenes externos — a diferencia de 80/443/8080, que
    siguen en timeout. Esto descarta un bloqueo de red general: es específico a puertos
    web comunes, muy probablemente una retención anti-abuso para VPS nuevas. Se reportó
    esto a soporte con evidencia concreta, pidiendo confirmar duración o si se puede
    levantar manualmente. Sigue en revisión de su lado.
  - Se intentó conseguir una IP nueva (reinstalar el VPS) para probar si el bloqueo era
    específico de la IP — **el reinstall NO cambió la IP** (contradice la documentación
    de Hostinger sobre esto). Cambiar de ubicación sí la cambiaría, pero implica borrar
    todo y posible mayor latencia al backend — se pospuso esa opción.
  - **Workaround temporal activo**: mientras se resuelve lo de 80/443, Nginx en la VPS
    frontend corre sobre el puerto **22345** (`podman-ngnix.sh` con `PORT=22345`).
    Confirmado funcionando completo: estáticos + proxy a los 3 microservicios +
    resuelve por dominio (`http://defensoria-escom.ddns.net:22345/`). Esto permite seguir
    probando/avanzando con el frontend real sin depender de que Hostinger resuelva el
    tema de fondo. **No es la solución final** — muchas redes institucionales solo dejan
    salir tráfico por 80/443, así que el público real necesita el puerto estándar.
  - **Estado: esperando revisión interna de Hostinger para 80/443**, avanzando en paralelo
    con el puerto 22345 como workaround de pruebas.

### Frontend desplegado en la VPS — arquitectura de 2 contenedores

Se compiló `Frontend/` con éxito (`npm install && ng build`, sin errores, 15 chunks lazy
coincidiendo con las 15 vistas) y se desplegó en `srv1824254` con una arquitectura de 2
contenedores (a petición del usuario, para que coincida con el patrón "imagen con contenido
horneado" que ya usa en el backend, y separando router de contenido estático):

- **`defensoria-web`**: imagen propia (`Dockerfile` en `front/`, `COPY dist/browser/` +
  config mínima de estáticos con fallback a `index.html` para el router de Angular). Escucha
  en el puerto interno **8090**, sin exponer al host.
- **`router-nginx`**: recibe todo el tráfico público en **22345** (el puerto temporal
  mientras Hostinger resuelve 80/443) y reparte: `/` → `defensoria-web:8090`, `/api/*` →
  el backend (2.25.78.22:8083/84/85).
- Ambos contenedores corren con `--network host` (no una red de Podman dedicada) porque el
  sistema tiene un bug de compatibilidad conocido entre Podman y el plugin CNI "firewall"
  (`plugin firewall does not support config version "1.0.0"`) que impedía crear/usar redes
  personalizadas. `--network host` evita ese problema por completo.
- **Confirmado funcionando end-to-end** desde el backend: `/` sirve el `index.html` real de
  Angular (22461 bytes, con fuentes/estilos), y `/api/auth/login` llega hasta `auth-service`
  (mismo patrón del 500 "bueno" de siempre). Confirmado también desde navegador real vía
  `http://defensoria-escom.ddns.net:22345/`.
- Errores del camino, ya resueltos: el primer build horneó `dist/` completo en vez de
  `dist/browser/` (por eso servía el "Welcome to nginx" — el índice real quedó un nivel
  más profundo); y el primer intento de red de Podman falló por el bug de CNI mencionado.
- Pendiente: el usuario reportó que el sitio carga pero visualmente no coincide del todo con
  los mockups del PDF — revisando qué ajustar.

## 2026-07-12 (continuación) — Frontend reconstruido de cero

Mientras se esperaba la revisión de Hostinger sobre el puerto bloqueado, se reconstruyó
completo el frontend en `Frontend/` (proyecto Angular nuevo — el anterior era solo el scaffold
con el bug de ruta).

- **Bug original corregido**: `app.routes.ts` ahora tiene el árbol completo de rutas; el login
  navega a `/panel` (antes navegaba a `/dashboard`, que nunca existió).
- Construido: capa `core/` (AuthService, QuejaService, interceptor JWT con auto-logout en
  401/403, guard de autenticación), layout público (header/footer institucional) y layout del
  panel (sidebar + topbar), y las ~13 pantallas del PDF de vistas.
- Conectado de verdad al backend: login, recuperar contraseña, activar cuenta, consultar
  folio, nueva queja (dentro del panel).
- Construido como UI con datos de ejemplo (backend no lo soporta todavía, ver
  `docs/HALLAZGOS.md` sección "Frontend reconstruido"): registro público de quejas, listado y
  detalle de "Mis Quejas", Acuerdos de Conciliación, Centro de Notificaciones, y parte de Mi
  Perfil.
- **Limitación del entorno**: no hubo acceso a los registros de npm en el sandbox donde se
  escribió el código (`npm error 403 blocked-by-allowlist`), así que no se pudo correr
  `npm install` ni `ng build` para verificar que compila. Pendiente que alguien lo compile por
  primera vez y reporte errores si los hay.

## Pendientes abiertos

### Infraestructura — completado ✅
- [x] Firewall hPanel confirmado y sincronizado en ambas VPS (backend y frontend).
- [x] `ufw`/`iptables` revisados en ambas VPS (sin bloqueos internos).
- [x] Backend limpio de nginx/front viejo; imágenes Docker por servicio (no una compartida).
- [x] Podman + Nginx (2 contenedores: `defensoria-web` + `router-nginx`) funcionando en la
      VPS frontend (2.25.64.47).
- [x] Proxy frontend → backend verificado con `curl` (front real + `/api/*` correctos).
- [x] DNS de `defensoria-escom.ddns.net` apuntando a `2.25.64.47`.
- [x] Puerto 80 público confirmado (el bloqueo era un firewall de hPanel sin sincronizar).
- [x] HTTPS con Certbot (método webroot, renovación automática) — ver entrada de arriba.
- [x] Corregir el bug de ruta `/dashboard` y construir las vistas de Angular que faltan
      (ver `docs/HALLAZGOS.md`).
- [x] `npm install && ng build` corrido y verificado sin errores.
- [x] Frontend compilado y desplegado en la VPS (`front/dist/browser/` → imagen
      `defensoria-front-img` → contenedor `defensoria-web`).
- [x] Rediseño visual de Inicio/header/footer inspirado en ipn.mx/defensoria (ver entrada
      de arriba) — pendiente confirmación final del usuario tras revisar en el navegador.

### Backend — pendiente (no iniciado, solo documentado en `HALLAZGOS.md`)
- [ ] Endpoint público de registro de quejas (hoy `/registrar` exige JWT y no acepta datos
      del quejoso en el body — bloquea el flujo de "presentar queja sin cuenta" del diseño).
- [ ] `GET` de listado y detalle de "Mis Quejas" por usuario (no existe hoy).
- [ ] Backend de Acuerdos de Conciliación (no existe ni modelo ni endpoints).
- [ ] Historial consultable de notificaciones (`notificaciones-service` solo envía correos).
- [ ] `GET /api/auth/me` + endpoint de actualización de perfil (boleta, unidad académica,
      correo personal, teléfono).

### Housekeeping — no bloqueante
- [ ] Separar el firewall compartido de hPanel en dos grupos independientes (uno por VPS),
      hoy comparten el mismo objeto de firewall.
- [ ] Typo menor en `auth.service/application.yaml` (`name: auth.serviceç`).

### Rediseño visual de Inicio y header/footer institucional (inspirado en ipn.mx/defensoria)

El usuario reportó que, con el sitio ya desplegado y funcionando, el diseño no se veía tan
"institucional" como el sitio real de la Defensoría (ipn.mx/defensoria) y compartió capturas de
esa página como referencia. Se rediseñó, sin tocar ningún endpoint ni lógica de negocio:

- **`src/styles.scss`**: se agregaron variables de color (`--guinda-vivo`, `--guinda-suave`,
  `--negro-inst`, `--crema`) y utilidades compartidas (`.section-title`, `.icon-grid`) sin
  eliminar las variables/clases existentes, para no romper otras vistas (login, registro de
  queja, panel, etc.).
- **`shared/public-layout/`**: header de 3 franjas al estilo gob.mx — barra superior oscura
  (enlaces institucionales), franja de marca blanca (marca IPN + nombre de la Defensoría) y
  barra de navegación guinda con estado activo (`routerLinkActive`, requirió agregar
  `RouterLinkActive` a los imports del componente). Footer ampliado con bloque de redes
  sociales, columnas de enlaces (Institución / Enlaces / Servicios) y línea de copyright con
  año dinámico.
- **`pages/inicio/`**: hero con degradado guinda + los 3 accesos (queja, folio, login) con
  iconos SVG en vez de emojis; nueva sección "Servicios en línea" con grid de 6 accesos
  (orientación, marco normativo, DDHH, presentar queja, acuerdo de creación, violencia de
  género — igual que la página real); nueva sección de pestañas (Difusión / Institucional /
  Publicaciones / Investigación / Servicios) con contenido de ejemplo, marcado explícitamente
  como "⚠️ Contenido de ejemplo" (no hay backend/CMS para esto todavía, igual que otras vistas
  del proyecto).
- **Verificación**: no se pudo correr `ng build --configuration production` completo en el
  sandbox porque bloquea la salida a `fonts.googleapis.com` (paso de inlining de fuentes, 403);
  se confirmó que no hay ningún error de plantilla/TypeScript corriendo
  `ng build --configuration development` (compiló limpio, los 15 chunks lazy de siempre). El
  build de producción real debe hacerse en una máquina con acceso normal a internet, como ya se
  hizo antes.

**Pendiente**: el usuario va a revisar el nuevo diseño contra las capturas del sitio real y
reportar ajustes adicionales antes de recompilar y redesplegar (`ng build` →
`dist/defensoria-front/browser/*` → `front/dist/browser/` en la VPS → rebuild de
`defensoria-web`).

### 🎉 Puerto 80 desbloqueado y HTTPS con Certbot — migración a 2 VPS cerrada

**Resolución del bloqueo de puerto 80/443/8080** (pendiente desde hacía varias sesiones,
Tarea #18): soporte de Hostinger (Kodee) confirmó que en el firewall de hPanel de la VPS
1824254 (frontend, 2.25.64.47) las reglas `accept` para 22, 80, 443, 8080 y 22345 ya existían
"desde cualquier origen", pero el grupo de firewall aparecía como **no sincronizado**. Tras
pedirle a Kodee que lo sincronizara, se hizo una prueba real: se levantó un contenedor
`nginx:alpine` temporal en `--network host` (sin volúmenes, solo para probar conectividad) y
`curl -Iv http://2.25.64.47/` desde una máquina externa devolvió `200 OK` — el puerto 80 ya
respondía desde fuera. Conclusión: el bloqueo no era un filtro de borde permanente como se
sospechaba, sino que la sincronización pendiente del firewall de hPanel nunca se había
aplicado. Se limpió el contenedor de prueba (`podman stop/rm test-puerto80`).

**Migración de `router-nginx` de 22345 a 80**: se cambió `listen 22345;` por `listen 80;` en
`/apps/aplicaciones/defensoria/router/config/router.conf` y se reinició el contenedor.
Verificado con `curl` local y externo, tanto por IP (`2.25.64.47`) como por dominio
(`defensoria-escom.ddns.net`) — ambos devuelven el `index.html` real de Angular
(`Content-Length: 22461`) y `/api/auth/login` sigue proxying correctamente al backend
(`500` esperado del `GlobalExceptionHandler`, no un error de proxy).

**Limpieza de red huérfana de Podman**: el warning
`Error validating CNI config file /etc/cni/net.d/defensoria-net.conflist: [plugin firewall
does not support config version "1.0.0"]` que aparecía en cada comando `podman` venía de un
intento anterior (abandonado) de crear una red custom `defensoria-net` — nunca se usó porque
se optó por `--network host` para ambos contenedores. Se confirmó que sigue siendo solo un
`WARN` inofensivo, y se eliminó la red huérfana con `podman network rm defensoria-net` para
dejarlo limpio.

**HTTPS con Certbot (método webroot, sin downtime en renovaciones)**:
1. Se instaló Certbot en la VPS frontend (`apt install certbot`) y se creó el directorio
   `/apps/aplicaciones/defensoria/router/certbot-webroot`.
2. Se agregó a `router.conf` el bloque `location /.well-known/acme-challenge/ { root
   /var/www/certbot; }`.
3. Se recreó el contenedor `router-nginx` (no bastaba con reiniciar, había que montar
   volúmenes nuevos) agregando `-v .../certbot-webroot:/var/www/certbot:ro` y
   `-v /etc/letsencrypt:/etc/letsencrypt:ro`, manteniendo el mismo mount del `router.conf` en
   `/etc/nginx/conf.d/default.conf` (confirmado antes con `podman inspect`).
4. Se emitió el certificado: `certbot certonly --webroot -w
   .../certbot-webroot -d defensoria-escom.ddns.net --agree-tos -m josebryanomar2004@gmail.com
   --no-eff-email` → **"Successfully received certificate"**, válido hasta 2026-10-10, con
   renovación automática ya programada por Certbot.
5. Se reescribió `router.conf` con dos bloques `server`: uno en `:80` que solo sirve el reto
   ACME y redirige todo lo demás con `301` a `https://`, y otro en `:443 ssl` con
   `ssl_certificate`/`ssl_certificate_key` apuntando a `/etc/letsencrypt/live/...` y los mismos
   4 `location` (front + 3 proxys al backend), agregando `proxy_set_header X-Forwarded-Proto
   $scheme` en los 4.
6. **Verificado end-to-end**: `curl -Iv https://defensoria-escom.ddns.net/` → `200 OK`,
   handshake TLS 1.3 válido, certificado de Let's Encrypt confirmado (`SSL certificate verify
   ok`); `curl -Iv http://defensoria-escom.ddns.net/` → `301` a `https://`.

**Con esto, la migración a 2 VPS (Backend+BD en 2.25.78.22, Frontend en 2.25.64.47) queda
cerrada por completo**: dominio apuntando a la VPS correcta, puerto 80 público, HTTPS con
certificado válido y renovación automática, arquitectura de 2 contenedores
(`defensoria-web` + `router-nginx`) funcionando tal como se pidió.

### Fusión de "Seguimiento de queja" + "Iniciar sesión" en Inicio, y mejora de alertas

Con el sitio ya funcionando en HTTPS sin puerto, alguien más revisó el diseño y dio dos
observaciones: que tener un card de "Iniciar sesión" separado del de "Seguimiento de queja" se
sentía innecesario/redundante, y que faltaba más contexto en los mensajes de alerta. Se le
preguntó al usuario cómo resolverlo (vía pregunta de opción múltiple) y eligió: fusionar ambos
en un solo card con pestañas, y mejorar tanto los mensajes de error de formularios como agregar
banners informativos — sin tocar la lógica de negocio ni los endpoints.

- **`pages/inicio/`**: el hero pasó de 3 cards a 2 — "Presentar una queja" se queda igual, y
  "Seguimiento de queja" + "Iniciar sesión" se fusionaron en un solo card **"Consultar /
  Acceder"** con dos pestañas: **"Sin cuenta"** (folio + correo, la consulta puntual de
  siempre) y **"Tengo cuenta"** (correo + contraseña, login real contra `/api/auth/login`,
  reutilizando `AuthService.login()` — el mismo que usa `portal-login`). El botón de "Iniciar
  sesión" del header/nav se dejó igual (sigue llevando a `/portal/login`, esa página no se
  tocó ni se eliminó).
- **Validaciones más descriptivas**: antes `consultar()` navegaba aunque los campos estuvieran
  vacíos; ahora valida que correo y folio no estén vacíos y que el correo tenga formato válido,
  con mensajes explicando qué falta. El login embebido valida campos vacíos y traduce el error
  del backend a un mensaje claro ("Correo o contraseña incorrectos...").
- **Banners de contexto** (`.info-banner`, clase global nueva en `styles.scss`): cada pestaña
  explica qué hace antes de mostrar el formulario ("no necesitas cuenta para esto" / "inicia
  sesión para ver el detalle completo...").
- **Mensajes de error ahora sí se ven** (`.error`, clase global mejorada en `styles.scss`): se
  descubrió que en varias pantallas (`portal-login`, `activar-cuenta`, `recuperar-password`,
  `consultar-queja`, `nueva-queja`) el `<p class="error">` no tenía ningún estilo real porque
  la única regla existente estaba anidada como `.input-group .error` y esos párrafos de error
  no viven dentro de un `.input-group` — quedaban como texto plano sin color ni fondo. Se
  corrigió con una regla global `.error` (fondo rojo claro, borde izquierdo, ícono ⚠) que ahora
  aplica en **todas** las pantallas que ya usaban esa clase, no solo en Inicio.
- **Verificación**: `ng build --configuration development` compiló limpio (mismo set de 15
  chunks lazy, `inicio` creció de ~25 kB a ~34 kB por la lógica de login agregada).

**Pendiente**: recompilar y redesplegar para que el usuario vea estos cambios en vivo.

#### Pipeline de redespliegue del frontend (de referencia, se repite en cada cambio visual)

En la compu (dentro de `Frontend/`):

```bash
ng build --configuration production
```

Sube por SFTP el *contenido* de `dist/defensoria-front/browser/` (reemplazando lo que ya
está) a, en la VPS frontend:

```
/apps/aplicaciones/defensoria/front/dist/browser/
```

En la VPS frontend:

```bash
cd /apps/aplicaciones/defensoria/front
podman build --no-cache -t defensoria-front-img .
podman stop defensoria-web
podman rm defensoria-web
podman run -d --name defensoria-web --network host defensoria-front-img
curl -I http://localhost:8090/
```

Y verificar en el navegador contra `https://defensoria-escom.ddns.net`.

### Revertida la fusión de "Seguimiento" + "Iniciar sesión" — no gustó visualmente

Tras compilar y ver la fusión en pestañas en vivo, el usuario reportó que se veía "feo"
comparado con el diseño de 3 cards separados de antes. Se revirtió el cambio estructural,
conservando lo demás:

- **`pages/inicio/`**: el hero vuelve a tener **3 cards**: "Presentar una queja" (igual),
  "Seguimiento de queja" (correo + folio + botón, sin pestañas), e "Iniciar sesión" (vuelve a
  ser un link simple a `/portal/login`, ya no tiene formulario de login embebido). Se
  eliminaron `accesoTab`, `seleccionarAccesoTab()`, `loginCorreo`, `loginPassword`,
  `loginCargando`, `loginError`, `ingresar()` y la dependencia de `AuthService` en `Inicio`
  (vuelve a ser el mismo componente ligero de antes).
- **Se conservó** la validación mejorada de `consultar()` (mensajes claros si faltan datos o
  el correo no tiene formato válido) y la corrección global de estilos de `.error` en
  `styles.scss` (esa sí aplicaba a varias pantallas, no solo a Inicio, y no tenía que ver con
  lo que se veía "feo").
- **Verificación**: `ng build --configuration development` compiló limpio; el chunk de
  `inicio` volvió a su tamaño original (~26 kB, antes había subido a ~34 kB por la lógica de
  login agregada).

**Pendiente**: redesplegar con el mismo pipeline de arriba para que el usuario vea el Inicio
de vuelta a como le gustaba.

## Nueva fase: lógica fuerte de backend, nuevos microservicios/endpoints y mejoras de BD

A partir de aquí arranca la siguiente etapa del proyecto: trabajar los microservicios de
verdad (nuevos endpoints, posibles microservicios nuevos, mejoras de base de datos) y del
frontend en consecuencia. Primer paso: catálogo de dependencias del IPN.

### Catálogo de dependencias del IPN (tabla `dependencias` en `queja-service`)

El formulario de "Presentar una queja" tiene una sección de "datos de la queja" que necesita
un selector real de la dependencia del IPN involucrada, en vez de texto libre. El usuario
proporcionó el índice completo del manual de organización del IPN (7 capturas de pantalla,
listando ~200 dependencias con su página) y pidió transcribirlo a un catálogo, agregando
además los dos planteles de nueva creación que no están en ese manual: **CECyT No. 18
"Zacatecas"** (https://cecyt18.ipn.mx/) y **CECyT No. 19 "Leona Vicario"**
(https://cecyt19.ipn.mx/).

- **Transcripción**: 208 dependencias, organizadas jerárquicamente con una clave propia
  legible (ej. `SA.1.1` = Secretaría Académica → Dirección de Educación Media Superior →
  División de Procesos Formativos) en vez de depender de ids autogenerados, para que la
  carga inicial (seed) sea legible y fácil de auditar. Categorías cubiertas: los órganos y
  áreas de la administración central (secretarías, direcciones, divisiones, coordinaciones),
  y todas las unidades académicas (CECyT/CET de nivel medio superior, ESIME/ESIA/ESCOM/etc.
  de nivel superior, centros de investigación, centros de educación continua, unidades de
  apoyo educativo y de innovación, y el cluster politécnico de Veracruz).
- **Nota de transparencia**: el límite entre "Rama de Ciencias Médico Biológicas" y "Rama de
  Ciencias Sociales y Administrativas" (medio superior, CECyT 5/12/13/14) no se distinguía
  con claridad en el escaneo entre las páginas 165-167; se usó la clasificación pública
  conocida de esos planteles y se marcó con una nota en el catálogo (`notas`) para que se
  pueda verificar contra el manual original si hace falta.
- **Entregable pedido por el usuario**: `dependencias_ipn.csv` (208 filas, columnas: `clave`,
  `clave_padre`, `nombre`, `abreviatura`, `tipo`, `categoria`, `nivel`, `pagina_manual`,
  `activo`, `notas`) — guardado en `Backend/queja-service/src/main/resources/seed/` y
  entregado también directamente al usuario.
- **Entidad JPA nueva**: `Backend/queja-service/.../entity/Dependencia.java` (mismo estilo
  que `Queja.java`: Lombok `@Data`, `@Entity @Table(name = "dependencias")`) +
  `DependenciaRepository.java` (`findByClave`, `findByActivoTrueOrderByNombreAsc`,
  `findByActivoTrueAndTipoOrderByNombreAsc`). Como `queja-service` usa
  `hibernate.ddl-auto: update` (no hay Flyway/Liquibase en el proyecto), la tabla
  `dependencias` se crea sola en el próximo arranque del servicio — no hace falta escribir
  una migración a mano.
- **Seed de datos**: `Backend/queja-service/src/main/resources/seed/dependencias_seed.sql`
  (generado automáticamente a partir del CSV, con escape de comillas) — son 208 sentencias
  `INSERT` para correr **una sola vez, después de que el servicio arranque con la entidad
  nueva y cree la tabla vacía**.
- **Limitación del entorno**: igual que con Angular al inicio del proyecto, este sandbox no
  tiene Maven instalado ni acceso a Maven Central (`wget: Failed to fetch
  https://repo.maven.apache.org/...`), así que no se pudo correr `mvn compile` para verificar
  en automático. El código sigue exactamente el patrón de `Queja.java`/`QuejaRepository.java`
  ya existentes y funcionando, pero falta que el usuario confirme que compila.

**Pendiente**: (superado por la siguiente entrada — el catálogo se movió a su propio
microservicio antes de desplegarse, ver abajo).

### Catálogo movido a un microservicio propio (`catalogo-service`, puerto 8086) + Swagger en los 4

Antes de desplegar nada, se le preguntó al usuario si prefería mantener el catálogo dentro de
`queja-service` (recomendación inicial, por overhead operativo y porque hoy los 3 servicios ya
comparten una sola base de datos) o separarlo en su propio microservicio pensando en
crecimiento futuro. El usuario decidió separarlo: "mejor pensemos a futuro, sé que ahorita
sería engorroso, pero mejor tener los problemas ahorita". En el mismo mensaje pidió agregar
Swagger/OpenAPI 3 a los 4 microservicios.

**Nuevo microservicio `Backend/catalogo-service/`** (scaffold completo, calcado del patrón de
`queja-service`):
- `pom.xml` — Spring Boot 3.5.16, Java 21, mismas dependencias que queja-service
  (`spring-boot-starter-data-jpa/security/web`, `postgresql`, `lombok`, `jjwt` 0.12.6) más
  `springdoc-openapi-starter-webmvc-ui` 2.8.5.
- `entity/Dependencia.java` + `repository/DependenciaRepository.java` — movidos tal cual desde
  `queja-service` (se **eliminaron** de ahí para no duplicar).
- `service/DependenciaService.java` + `controller/DependenciaController.java` — nuevo:
  `GET /api/catalogos/dependencias` (con filtro opcional `?tipo=`) y
  `GET /api/catalogos/dependencias/{clave}`.
- `config/WebConfig.java` + `JwtAuthenticationFilter.java` + `JwtUtil.java` — mismo patrón que
  queja-service (comparte el mismo `jwt.secret`), con `/api/catalogos/**` y las rutas de
  Swagger marcadas `permitAll()` — cualquier futuro endpoint de administración del catálogo
  (crear/editar dependencias) sí requeriría JWT.
- `config/OpenApiConfig.java` — metadata de Swagger (título, descripción, esquema `bearerAuth`).
- `resources/application.yaml` (dev, puerto 8086) y `resources/seed/dependencias_ipn.csv` +
  `dependencias_seed.sql` — movidos desde `queja-service`.
- `Backend/config-files/catalogo-service/config/catalogo-service.yml` — config de producción
  nueva, mismo patrón que `quejas-service.yml` (Postgres por IP pública `2.25.78.22`, no
  `localhost`, porque los contenedores backend no usan `--network host`).

**Swagger/OpenAPI 3 en los 4 microservicios**:
- `auth.service`: ya tenía las rutas de Swagger permitidas en su `WebConfig` (alguien las había
  dejado listas de antes) — solo faltaba la dependencia en el `pom.xml`. Se agregó, más
  `config/OpenApiConfig.java`.
- `queja-service`: se agregó la dependencia + `OpenApiConfig.java` + se agregaron las 3 rutas
  de Swagger (`/v3/api-docs/**`, `/swagger-ui.html`, `/swagger-ui/**`) como `permitAll()` en su
  `WebConfig` existente.
- `notificaciones-service`: **no tenía ningún `WebConfig`** (ver hallazgo nuevo abajo). Se creó
  uno por primera vez, con las rutas de Swagger públicas y todo lo demás autenticado (mismo
  comportamiento restrictivo que ya tenía por default de Spring Security, solo se le agregó la
  excepción de Swagger). Más `OpenApiConfig.java`.
- `catalogo-service`: incluido desde el scaffold inicial.
- Los 4 quedan con Swagger UI en `/swagger-ui.html` y el spec en `/v3/api-docs` — se agregó el
  bloque `springdoc.swagger-ui.path` / `springdoc.api-docs.path` en los 4 `application.yaml` de
  desarrollo y en los 4 `config-files/*/config/*.yml` de producción, por explicitud (son los
  valores por defecto de todos modos).

**Infraestructura actualizada para el 4to servicio**:
- `Backend/podman-compose.sh`: `catalogo-service` agregado al arreglo `SERVICIOS` y a
  `get_port()` (puerto 8086).
- **Pendiente en la VPS backend** (no ejecutado, son pasos que el usuario debe correr):
  1. Compilar los 4 microservicios (`mvn clean package -DskipTests` en cada uno) — en
     particular `queja-service` (por la limpieza de `Dependencia`) y el nuevo `catalogo-service`.
  2. Subir los `.jar` a `/apps/aplicaciones/defensoria/back/artifact/`, respetando el nombre
     que espera `podman-compose.sh`: `quejas-service.jar`, `catalogo-service.jar`, etc.
  3. `bash podman-compose.sh up-container catalogo-service` (crea la imagen, el contenedor, y
     Hibernate crea sola la tabla `dependencias`) y
     `bash podman-compose.sh up-container quejas-service` (para que compile sin la entidad que
     se le quitó).
  4. Abrir el puerto **8086** en el firewall de hPanel de la VPS backend, restringido a la IP
     de la VPS frontend (`2.25.64.47`) — mismo patrón que 8083-8085.
  5. Correr el seed: `psql -U postgres -d defensoria_db -f dependencias_seed.sql`.
  6. Agregar en `router.conf` (VPS frontend, en **ambos** bloques `server` — el de `:80` ya
     solo redirige, pero el de `:443` sí necesita la ruta nueva):
     ```nginx
     location /api/catalogos/ {
         proxy_pass http://2.25.78.22:8086;
         proxy_set_header Host $host;
         proxy_set_header X-Real-IP $remote_addr;
         proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
         proxy_set_header X-Forwarded-Proto $scheme;
     }
     ```
     y `podman restart router-nginx`.
  7. Verificar: `curl -I https://defensoria-escom.ddns.net/api/catalogos/dependencias` debe
     regresar el JSON con las 208 dependencias.
- **Limitación del entorno (otra vez)**: no se pudo compilar ninguno de los 4 microservicios en
  este sandbox (sin Maven ni acceso a Maven Central) para confirmar que el código nuevo/movido
  compila. Sigue exactamente los patrones ya usados en el proyecto, pero falta la confirmación
  real del usuario.

**Pendiente para después**: conectar el frontend (`Frontend/src/app/pages/panel/nueva-queja` y
`registro-queja-publico`) al catálogo real (`GET /api/catalogos/dependencias`) en vez de texto
libre, una vez que el usuario confirme que el endpoint responde en producción.

### Endpoint del catálogo confirmado en producción + formulario de registro conectado

El usuario confirmó `curl -I https://defensoria-escom.ddns.net/api/catalogos/dependencias` →
`200 OK` con JSON — la cadena completa (Postgres → `catalogo-service` → `router-nginx` →
HTTPS) quedó verificada end-to-end. De inmediato pidió dos ajustes al formulario público de
registro de queja: reacomodar el layout (lo sentía "mal acomodado") y agregar un campo para el
número de boleta o de empleado.

- **`proxy.conf.json`**: se agregó la ruta `/api/catalogos` → `2.25.78.22:8086` para desarrollo
  local (ya estaban `/api/auth`, `/api/quejoso`, `/api/notificaciones`).
- **`core/models/catalogo.models.ts`** + **`core/services/catalogo.service.ts`** (nuevos):
  `CatalogoService.listarDependencias(tipo?)` — `GET /api/catalogos/dependencias`, con filtro
  opcional por `tipo` (se usa `tipo=Unidad Académica` para no mezclar el selector de "lugar de
  los hechos" con divisiones administrativas internas del catálogo).
- **`pages/registro-queja-publico/`**:
  - El `<select>` de "Lugar donde sucedieron los hechos" pasó de 4 opciones hardcodeadas
    (ESCOM/ESIA/ESCA/ENCB) a las ~50 Unidades Académicas reales del catálogo, cargadas al
    entrar a la página (`ngOnInit`), con manejo de error si el catálogo no responde.
  - Se reordenó "Datos del Quejoso": Nombre/Apellidos y Correo se quedan juntos; Fecha de
    nacimiento pasó a su propia fila (antes estaba emparejada de forma un poco arbitraria con
    la identificación institucional); "Identificación institucional" (alumno/empleado) ahora
    va emparejada con el campo nuevo **"Número de boleta"/"Número de empleado"** — el rótulo y
    el placeholder cambian dinámicamente según el radio seleccionado (`etiquetaNumeroIdentificacion`).
  - Este campo (`numeroBoletaEmpleado`) todavía no se envía a ningún backend porque el registro
    público sigue bloqueado por el mismo gap documentado (`mensajeBackendPendiente`) — cuando
    se construya el endpoint público de registro, este dato ya está capturado en el formulario.
- **Verificación**: `ng build --configuration development` compiló limpio (chunk de
  `registro-queja-publico` subió de ~35 kB a ~39 kB por el nuevo servicio + lógica).

**Pendiente**: recompilar/redesplegar el frontend para ver el catálogo real en el selector, y
confirmar visualmente el nuevo acomodo del formulario.

## 2026-07-12 (continuación) — Retroalimentación de usuario: evidencias múltiples en BD + rediseño visual

El usuario probó lo anterior en producción y reportó varias observaciones en un solo mensaje:
el catálogo del selector no traía todas las inserciones, los formularios se veían "desacomodados
y muy feos", el input de fecha era "muy simple", el modal de datos del tutor se veía feo y no
dejaba constancia visual de que ya se habían capturado esos datos, solo se podía adjuntar un
archivo (y preguntó dónde se guarda, porque lo va a necesitar después), y la caja de "Nota
importante" debía dejar de ser estática y convertirse en un widget flotante de ayuda. También
preguntó por qué aparece el mensaje "Esta función todavía no está disponible" al enviar el
formulario público.

Antes de implementar se le presentaron 3 preguntas de decisión (catálogo completo vs. filtrado,
un archivo vs. varios, mejorar el `<input type="date">` nativo vs. Angular Material) y una
cuarta sobre dónde guardar los archivos (filesystem+metadata vs. BYTEA en Postgres). El usuario
eligió: catálogo completo (208), varios archivos guardados en la base de datos, mejorar el input
nativo con CSS (no Angular Material), y el archivo completo dentro de Postgres como BYTEA.

### Backend (`queja-service`) — evidencias múltiples en BYTEA
- Nueva entidad `QuejaEvidencia` (`queja_evidencias`): `id`, `queja` (FK), `nombreArchivo`,
  `tipoMime`, `tamanioBytes`, `contenido` (`byte[]`, columna `bytea` explícita — **sin** `@Lob`,
  para evitar que Hibernate 6 la mapee como `oid` en Postgres) y `fechaSubida`.
- `Queja` ahora tiene `@OneToMany` a `QuejaEvidencia` (cascada + orphanRemoval); el campo viejo
  `rutaEvidencia` (ruta en disco) queda marcado `@Deprecated`, ya no se escribe.
- `QuejaService.registrarQueja(...)` cambió su firma de un solo `MultipartFile archivo` a
  `List<MultipartFile> archivos`; se eliminó por completo el guardado en disco
  (`guardarArchivo`/`storage.location`) — todo el contenido se lee a `byte[]` y se persiste en
  Postgres junto con la queja.
- `application.yaml` / `quejas-service.yml`: `max-file-size` 10MB→20MB, `max-request-size`
  10MB→60MB (para permitir varios archivos por queja).

### Frontend — soporte de múltiples archivos
- `queja.service.ts`: `registrarQueja(...)` ahora envía cada archivo bajo la misma clave
  `archivos` repetida en el `FormData` (antes una sola clave `archivo`).
- `nueva-queja` (panel autenticado) y `registro-queja-publico`: el `<input type="file">` ahora
  tiene `multiple`, con una lista `<ul class="lista-archivos">` de los archivos elegidos y botón
  "✕" para quitar uno antes de enviar.

### Frontend — rediseño visual de `registro-queja-publico`
- **Catálogo sin filtrar**: el selector de "Lugar donde sucedieron los hechos" ahora llama
  `listarDependencias()` sin el parámetro `tipo`, mostrando las 208 dependencias completas.
- **Nota importante → widget flotante**: se quitó la caja estática siempre visible; ahora hay un
  botón circular fijo (esquina inferior derecha) que abre/cierra un panel flotante con el mismo
  contenido, controlado por `mostrarNotaFlotante`/`toggleNotaFlotante()`.
- **Banner de confirmación de tutor**: cuando se confirman los datos del tutor
  (`confirmarTutor()`), aparece un banner persistente en el formulario ("Datos del tutor/adulto
  responsable capturados: [nombre]") con botón "Editar" (`editarTutor()`) para reabrir el modal.
  Antes no había ninguna indicación visual de que esos datos ya estaban capturados.
  - Modal del tutor: se le agregó un `modal-header` con título + botón de cerrar, y se restyleó
    con los colores institucionales (guinda) en vez del estilo genérico anterior.
- **Input de fecha nativo mejorado con CSS** (decisión explícita del usuario — no Angular
  Material): borde, radio, sombra de foco y color del ícono del calendario ajustados a la
  paleta institucional vía `::-webkit-calendar-picker-indicator`.
- **Verificación**: `ng build --configuration development` compiló limpio.

### Aclaración: mensaje "Esta función todavía no está disponible"
No es un error — es un aviso a propósito. El backend actual (`queja-service`) solo expone
`POST /api/quejoso/quejas/registrar`, protegido por JWT, y toma el correo del quejoso del token
de sesión. El formulario público (`registro-queja-publico`) está pensado para gente sin cuenta,
así que no hay token que asociar a la queja — el backend no tiene todavía un endpoint público/
anónimo que acepte los datos del quejoso directamente en el cuerpo de la petición. Está
documentado desde antes en `docs/HALLAZGOS.md` y sigue como tarea pendiente (#31 en la lista de
tareas: "Implementar endpoints de backend faltantes").

**Pendiente**: desplegar backend (`queja-service` recompilado) y frontend; agregar
`client_max_body_size` en `router.conf` para que Nginx no rechace las peticiones multipart más
grandes (varios archivos, hasta 60MB).

## 2026-07-13 — Segunda ronda de pulido visual + componente compartido de nota flotante

El usuario probó el sitio (todavía con el build viejo, antes de subir los cambios de esta
sesión) y reportó que los formularios seguían viéndose desacomodados, que quería el mismo
tratamiento de "nota flotante" también en el formulario de `nueva-queja` (panel autenticado,
que aún tenía la caja estática de 90 días), y que la subida de varios archivos no le funcionaba
— esto último porque el sitio en producción todavía no tenía el build con los cambios de
sesión, no por un bug de código.

- **Nuevo componente compartido `app-nota-flotante`** (`shared/nota-flotante/`): encapsula el
  botón circular + panel flotante que antes estaba duplicado directamente en
  `registro-queja-publico`. Recibe el título por `@Input()` y el contenido por
  `<ng-content>`, así que cualquier formulario puede usarlo como
  `<app-nota-flotante titulo="..."><ul>...</ul></app-nota-flotante>`.
- **`nueva-queja`**: se quitó la caja estática `.aviso-pendiente` (que siempre estaba visible) y
  se reemplazó por `<app-nota-flotante>` con el mismo aviso de los 90 días, igual que en
  `registro-queja-publico`.
- **Encabezados de sección numerados**: los `<h3>` de ambos formularios ahora tienen un círculo
  numerado (1, 2, 3…) generado con CSS (`counter-reset`/`counter-increment`), para que las
  secciones del formulario se sientan como pasos en vez de bloques de texto sueltos.
  `nueva-queja` también se dividió en dos secciones ("Detalles del Hecho" y "Datos del
  Denunciado") en vez de mezclarlas en una sola.
- **Estilos globales de formulario** (`styles.scss`, aplican a todos los formularios del sitio):
  estados de `hover`/`focus` con sombra guinda en inputs/selects/textarea, flecha personalizada
  en los `<select>`, y la regla `.input-fecha input[type=date]` (antes solo vivía en
  `registro-queja-publico.scss`) se movió aquí para que cualquier formulario con fecha
  (incluido `nueva-queja`) tenga el mismo calendario mejorado.
- **Verificación**: `ng build --configuration development` compiló limpio; se confirmó que los
   3 archivos `.scss` tocados están dentro del presupuesto de tamaño por componente.

**Aclaración importante para el usuario**: todo lo anterior (multi-archivo, nota flotante,
banner de tutor, calendario, catálogo completo) vive en el código local — el sitio en
producción seguía sirviendo el build de antes de esta ronda de cambios. Falta un redeploy
completo del frontend (y del backend de `queja-service` para BYTEA) para verlo en vivo.

## 2026-07-13 (continuación) — Endpoint público de registro + datos estructurados

Tras confirmar (con `podman ps`/`psql`) que `quejas-service` ni siquiera estaba corriendo en la
VPS y que la tabla `queja_evidencias` no existía, el usuario pidió explícitamente: **"quiero
que las quejas estén tanto para nuevos usuarios como para los que ya están registrados"** — es
decir, construir por fin el endpoint público que documentaba `HALLAZGOS.md` como pendiente
(tarea #31), para que el formulario público deje de mostrar el aviso de "función no disponible".

Se le preguntó si estructurar bien los datos con columnas propias o seguir concatenando texto
libre como hasta ahora; eligió **estructurar bien** (columnas propias), pensando a futuro
(mismo criterio que ya había usado para decidir separar `catalogo-service`).

### Backend (`queja-service`)
- **`Queja`**: se agregaron columnas propias — `nombreQuejoso`, `apellidoPaternoQuejoso`,
  `apellidoMaternoQuejoso`, `fechaNacimientoQuejoso`, `tipoIdentificacionQuejoso`,
  `numeroIdentificacionQuejoso`, `unidadAcademicaClave`, `fechaHechos`, `nombreDenunciado`,
  `apellidoDenunciado`, `origenRegistro` ("AUTENTICADO"/"PUBLICO"). Antes todo esto (salvo el
  correo) se guardaba como texto libre concatenado dentro de `descripcion`.
- **Nueva entidad `QuejaTutor`** (`queja_tutores`, relación `@OneToOne` con `Queja`): nombre,
  apellidos, parentesco, correo, teléfono del tutor/adulto responsable, solo cuando el quejoso
  es menor de edad.
- **Nuevo endpoint público `POST /api/quejoso/quejas/registro-publico`** (`permitAll` en
  `WebConfig`, sin JWT): recibe un `RegistroQuejaPublicaRequest` vía `@ModelAttribute`
  (multipart/form-data, para poder incluir archivos en la misma petición) con todos los datos
  del quejoso + queja + tutor opcional. Valida campos obligatorios a mano y lanza
  `RuntimeException` con mensaje claro si falta algo.
- **Endpoint autenticado `/registrar` actualizado**: ahora recibe `unidadAcademicaClave`,
  `fechaHechos`, `nombreDenunciado`, `apellidoDenunciado` como parámetros propios (antes el
  frontend los concatenaba a mano dentro de la descripción).
- **Manejo global de errores**: se copió el patrón de `auth-service`
  (`GlobalExceptionHandler` + `ErrorResponseModel`) a `queja-service`, que no lo tenía — así
  las validaciones del endpoint público devuelven `{mensaje, timestamp, codigo}`, el mismo
  formato que el frontend ya sabe leer (`err?.error?.mensaje`).
- **Corrección de un bug latente de Lombok**: `Queja`, `QuejaEvidencia` y la nueva `QuejaTutor`
  tienen relaciones bidireccionales; `@Data` genera `toString()`/`equals()`/`hashCode()`
  incluyendo todos los campos por default, lo que habría causado una recursión infinita
  (`Queja.toString()` → `QuejaTutor.toString()` → `Queja.toString()` → ...). Se agregó
  `@ToString.Exclude`/`@EqualsAndHashCode.Exclude` en el lado "hijo" de cada relación para
  cortar el ciclo — el mismo riesgo ya existía sin corregir en `QuejaEvidencia`, se corrigió de
  paso.

### Frontend
- **`queja.models.ts`**: `Queja` ahora expone los campos estructurados nuevos y `tutor`; nueva
  interfaz `RegistroQuejaPublicaRequest`.
- **`queja.service.ts`**: `registrarQueja(...)` cambió a recibir un objeto
  `DatosQuejaAutenticada` (con los campos estructurados) en vez de solo motivo/descripción;
  nuevo método `registrarQuejaPublica(...)` que arma el `FormData` completo (quejoso + queja +
  tutor + archivos) y llama a `/registro-publico`.
- **`nueva-queja.ts`**: ya no concatena texto libre — manda `unidadAcademicaClave`,
  `fechaHechos`, `nombreDenunciado`, `apellidoDenunciado` como campos propios.
- **`registro-queja-publico.ts`**: se quitó el placeholder `mensajeBackendPendiente` — ahora
  valida los campos obligatorios en el cliente, llama a `registrarQuejaPublica(...)` de verdad,
  maneja estado de carga/error, y muestra una pantalla de éxito con el folio (mismo patrón que
  "Nueva Queja" del panel) con un enlace a `/queja/consultar`.
- **Verificación**: `ng build --configuration development` compiló limpio; presupuesto de
  tamaño de `registro-queja-publico.scss` dentro de límite.

**Pendiente**: desplegar el backend de `queja-service` (el usuario confirmó que ni siquiera
estaba corriendo) y el frontend con estos cambios; validar en la base de datos que Hibernate
creó las columnas nuevas y la tabla `queja_tutores`, y probar un registro público real de punta
a punta.

## 2026-08-20 — Rename auth.service→auth-service, 2 bugs de arranque, logging por servicio, reconstrucción y rediseño del Frontend, unificación de puerto interno

Sesión larga que cubre varias rondas de trabajo sin documentar entre la entrada anterior
(2026-07-13) y hoy. Se agrupa todo aquí por tema en vez de por orden cronológico exacto.

### Backend: `auth.service` → `auth-service`
- Se corrigió la inconsistencia de nombres: carpeta `Backend/auth.service/` renombrada a
  `Backend/auth-service/`, `pom.xml` (`<artifactId>`), `application.yaml`
  (`spring.application.name`) y todas las referencias cruzadas en comentarios de otros
  servicios (`admin-service/entity/PersonalAdministrativo.java`,
  `admin-service/config/JwtUtil.java`, `catalogo-service/config/JwtUtil.java`,
  `config-files/admin-service/config/admin-service.yml`, `README.md`,
  `docs/ARQUITECTURA.md`) actualizadas al nuevo nombre. `podman-compose.sh` y `config-files/`
  ya usaban `auth-service` (no necesitaron cambio).

### Backend: 2 bugs de arranque corregidos
- **`auth-service` no levantaba** (`UnsatisfiedDependencyException` por falta de un bean
  `PasswordEncoder`) — se creó `auth-service/.../config/AppConfig.java` con un
  `BCryptPasswordEncoder`, replicando el patrón que `admin-service` ya tenía. No relacionado
  con el rename ni con el refactor previo de inyección por constructor.
- **`revision-service` no levantaba** (`PlaceholderResolutionException` en
  `primer-contacto.base-url`) — la causa real: `SPRING_CONFIG_NAME` hace que Spring ignore el
  `application.yaml` empacado en el jar (que sí traía un default) y solo lea el yml externo
  montado, que no tenía esa clave. Se agregó `primer-contacto.base-url:
  http://2.25.78.22:8082` directamente a `config-files/revision-service/config/revision-service.yml`.

### Backend: nombre del microservicio visible en los logs
- Se agregó `spring.application.name` (donde faltaba) y
  `logging.pattern.level: "%5p [${spring.application.name}]"` a los `application.yaml` locales
  y a los yml de producción (`config-files/*/config/*.yml`) de `admin-service`, `auth-service`,
  `catalogo-service`, `chatbot-service`, `notificaciones-service`, `queja-service` y
  `revision-service` — a petición explícita del usuario, **excluyendo**
  `primer-contacto-service` y `subdefensoria-service`. Patrón oficial de Spring Boot, sin
  dependencias nuevas.
- Se creó `Backend/rebuild-jars.sh`: recompila los 7 jars (`mvn clean package -DskipTests`) y
  los copia a `Backend/_jars-listos/`, con resumen de éxito/fallo por servicio.

### Frontend: reconstrucción de scaffolding faltante
- El build (`ng build`) fallaba por completo: faltaban `package.json`, `tsconfig.json`,
  `tsconfig.app.json`, `src/main.ts`, `src/index.html`, `src/app/app.scss`, `public/.gitkeep`,
  `src/app/app.routes.ts`, `src/app/core/interceptors/jwt.interceptor.ts`,
  `src/app/pages/crear-cuenta/crear-cuenta.ts`, `src/app/pages/portal-login/portal-login.scss`
  y `src/app/pages/recuperar-password/recuperar-password.scss`. `git status` confirmó que
  ninguno de estos archivos estuvo nunca versionado (no eran "eliminados", simplemente nunca se
  agregaron) — no eran recuperables con `git restore`.
- Se reconstruyeron todos desde cero, cruzando cada `routerLink`/`router.navigate()` e import
  real del proyecto para que `app.routes.ts` y el resto reflejaran exactamente la navegación
  existente (no una estructura inventada).
- También se reconstruyó `src/app/shared/public-layout/public-layout.scss` (topbar, franja de
  marca, nav, footer) — confirmado ausente incluso en un build de `dist/` histórico del 14 de
  julio, es decir, llevaba faltando desde antes de esta sesión.
- **Verificación**: `ng build --configuration development` compiló sin errores (48s). La
  build de producción excede el límite de tiempo del sandbox de esta herramienta (~178s); debe
  correrse en la máquina del usuario.

### Frontend: rediseño de la pantalla de Inicio
- **Primera ronda** (jerarquía y limpieza): la tarjeta "Seguimiento de queja" ya no tiene
  inputs propios (antes duplicaba el formulario de consulta), pasó a un botón `btn-secondary`
  que enlaza a `/queja/consultar`; "Iniciar sesión" también bajó a `btn-secondary` para que
  "Presentar una queja" sea la única llamada a la acción primaria. El banner de aviso de
  contenido de ejemplo (`.tabs-note`) y las tarjetas de difusión (`.tab-card`) se rediseñaron
  con más jerarquía visual (ícono, "Leer más →", sombra con hover). Los textos del grid de
  íconos institucionales se uniformaron a 2 líneas (`-webkit-line-clamp`).
- **Segunda ronda** (a petición del usuario: "se ve muy plano, yo desconfiaría"): se rediseñó
  el hero completo para transmitir más confianza —
  - Sello "Instancia oficial del IPN" con ícono de escudo, sobre el título.
  - 3 chips de confianza (confidencial, respaldo normativo, seguimiento en línea).
  - Botones de CTA más grandes con sombra ("Presentar una queja" / "Consultar mi queja").
  - Textura de puntos + mancha de luz dorada de fondo (antes era un degradado plano).
  - Figura ilustrativa de escudo + balanza construida en SVG con gradientes (no hay
    herramienta de generación de imágenes/3D disponible en este entorno).
  - Las 3 tarjetas de servicio pasaron a "flotar" sobre el borde inferior del hero (margen
    negativo) con el ícono dentro de un círculo de color, en vez de quedar incrustadas en el
    degradado.
  - Nueva sección "¿Cómo funciona?" (3 pasos numerados con línea conectora: Registra →
    Seguimiento → Resolución).
  - El grid de "Servicios en línea" pasó de íconos sueltos sobre fondo gris a tarjetas blancas
    con sombra, borde de acento e ícono en círculo.
  - **Verificación**: `ng build --configuration development` compiló sin errores; balance de
    llaves/etiquetas revisado a mano.

### Frontend: rediseño visual de `chatbot-widget`
- A petición del usuario (avatar más profesional, más llamativo, toques de UI): el ícono plano
  se sustituyó por un robot construido con SVG + gradientes (mismo motivo: sin herramienta de
  render 3D/imágenes); halo dorado (`box-shadow` + gradiente radial `botHalo`) y 3 anillos
  "de sonido" con `animation-delay` escalonado simulando actividad; panel con glassmorphism
  (`backdrop-filter: blur(16px)` + fondo blanco translúcido); indicador "En línea" con punto
  verde pulsante junto al nombre del asistente.
- **Verificación**: SVGs balanceados (2 `<svg>`/`<defs>` sin colisión de IDs de gradiente entre
  el ícono mini del panel y el del botón flotante), llaves de SCSS balanceadas (39/39), build
  de desarrollo sin errores.

### Frontend: consolidación de archivos de despliegue + incidente de puerto
- Existían dos carpetas (`Frontend/` y `front/`) con partes del código de despliegue —
  confusión reportada por el usuario con una captura de Finder. Se preguntó explícitamente y el
  usuario eligió consolidar todo en `Frontend/` (mismo patrón que `Frontend-Admin`/
  `Frontend-Revision`: código fuente y archivos de despliegue en la misma carpeta). `front/` se
  eliminó.
- `Dockerfile`, `config/static.conf` y `podman-compose-front.sh` se copiaron **verbatim** del
  servidor real (contenido pegado por el usuario), corrigiendo un solo bug real encontrado en
  el script del servidor: `PORT=22345` + `-p ${PORT}:80` no coincidía con el contenedor real
  (nginx interno escucha en 8090) — corregido a `PORT=8090` + `-p ${PORT}:${PORT}`.
- **Incidente**: el usuario corrió en el servidor el script **viejo** (el fix aún no se había
  subido), lo que dejó el contenedor mapeado `22345->80` mientras nginx adentro escucha 8090 →
  sitio caído (`curl` devolvía "Connection refused"). Se dio una recuperación de emergencia
  (recrear el contenedor con `-p 8090:8090` reutilizando la imagen ya construida) más un
  `sed` para parchar el script en el servidor. Confirmado resuelto por el usuario.

### Backend: puerto interno de los 9 microservicios unificado a 8080
- El usuario mostró capturas de cómo se maneja el despliegue en su trabajo: los backends ahí
  comparten una imagen base genérica y todos escuchan internamente en **8080**, variando solo
  el puerto externo/host publicado por Podman (el puerto interno no necesita ser único porque
  cada contenedor tiene su propio namespace de red). Se decidió, tras consultarlo, adoptar
  **solo** esa parte del patrón (puerto interno uniforme) — **no** migrar a una imagen base
  compartida, ya que con 9 servicios (vs. las decenas del trabajo) seguir con una imagen propia
  por microservicio se mantiene más simple de entender/depurar.
- Cambio: `server.port: 8080` en los 9 `config-files/*/config/*.yml` de producción (sin tocar
  los `application.yaml` locales de cada servicio, que deben seguir con su puerto propio para
  poder correr varios en la misma máquina sin contenedores). `podman-compose.sh`:
  `-p $PORT:$PORT` → `-p $PORT:8080`, y `--build-arg SERVICE_PORT` fijo en 8080 (documentación
  del `EXPOSE` en el Dockerfile). Los puertos externos (8082-8089, 8091) y todas las URLs entre
  microservicios (`http://2.25.78.22:<puerto>`) no cambiaron.
- **Incidente de despliegue parcial**: el primer intento del usuario solo subió la carpeta
  `config-files/` al servidor sin el `podman-compose.sh` actualizado — el script viejo seguía
  mapeando `-p $PORT:$PORT` mientras la app ya escuchaba en 8080 adentro, lo que habría dejado
  los 9 servicios inalcanzables por su puerto externo (mismo patrón que el incidente del
  frontend). Se corrigió subiendo también el script y volviendo a correr `up`;
  `podman ps -a` confirmó el mapeo correcto (`X->8080/tcp`) en 8 de 9 servicios
  (`subdefensoria-service` quedó en `Exited (1)`, pendiente, fuera de alcance por instrucción
  explícita del usuario).
- Tras el redeploy, un `curl` a `https://defensoria-escom.ddns.net/api/chatbot/menu` devolvió
  `504 Gateway Timeout`. Diagnóstico: `curl` directo a `localhost:8089` en el servidor backend
  y a `2.25.78.22:8089` desde el servidor frontend (misma ruta que usa `router-nginx`)
  respondieron `200` con el JSON completo — el backend y la red entre las 2 VPS estaban sanos,
  así que el 504 era transitorio (contenedores aún reiniciando) o de `router-nginx`. El usuario
  confirmó que, al reintentar, ya funcionaba — resuelto sin cambios adicionales.

**Pendiente**: `subdefensoria-service` sigue en `Exited (1)` en producción (no investigado,
pospuesto a propósito). Falta desplegar en producción todo lo del Frontend acumulado en esta
entrada (Inicio rediseñado, chatbot-widget rediseñado) — sigue solo en el árbol de trabajo
hasta el próximo `ng build --configuration production` + subida + `podman-compose-front.sh up`.
