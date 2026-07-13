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
