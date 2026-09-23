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

---

## 2026-09-08 — Primera validación de casos de uso: CU-Q01 (queja sin cuenta)

Inicio de la ronda de validación del happy path caso por caso. El usuario prueba front y back
en el ambiente desplegado y reporta lo que encuentra; cada caso se registra en
`docs/cambios-primera-validacion.md`, que es la bitácora de esta ronda.

### CU-Q01 — Presentar una queja sin cuenta (`/queja/registro`)

12 hallazgos: 8 reportados por el usuario y 4 detectados al revisar la pantalla contra el
caso de uso. 11 resueltos, 1 pospuesto por decisión explícita.

**Validaciones de captura (H-01.1 a H-01.4, H-01.7)**
- Nombres y apellidos (quejoso, denunciado y tutor): letras con acentos y ñ, permitiendo
  espacio interno, apóstrofe y guion — `María José`, `D'Angelo`, `Pérez-Gómez`. **No** se
  usó "solo A-Z" a propósito: rechazaría nombres reales. Los dígitos ya no se pueden teclear.
- Correo: validación en **dos niveles**. Nivel 1 de formato general para cualquier dominio;
  nivel 2 con las reglas de Google (solo letras, dígitos y punto, 6–30 caracteres) aplicadas
  **únicamente** a `gmail.com`/`googlemail.com`. El usuario había pedido prohibir globalmente
  `& = _ ' - + , < >`, pero esas son restricciones de Gmail al crear una cuenta, no del correo
  electrónico: aplicadas a todos los dominios habrían rechazado direcciones válidas y en uso
  (`juan-perez@outlook.com`, `maria_lopez@yahoo.com.mx`). Se le expuso el riesgo y aceptó el
  esquema de dos niveles.
- Fecha de nacimiento: entre 1920-01-01 y el 31 de diciembre del **año pasado**. El año se
  calcula en runtime con el reloj del servidor en `America/Mexico_City`, no como constante,
  para que la regla no caduque cada 1 de enero.
- Fecha de los hechos (no reportada, también sin validar): no futura y no anterior al
  nacimiento.
- Boleta/empleado: solo dígitos, máximo 10 (antes permitía 12 y aceptaba letras). Se conserva
  como `VARCHAR`, no numérico — los ceros a la izquierda de una boleta son significativos.

**Identificación oficial (H-01.5)** — era el hallazgo bloqueante
- De "PDF, JPG, PNG hasta 30MB (y en la práctica cualquier cosa)" a **solo JPG/PNG, 3MB, hasta
  2 imágenes** (frente y reverso). El usuario pidió inicialmente 1MB y se le señaló que una
  foto de credencial desde celular pesa 2–5MB; subió el tope a 3MB. También pidió quitar PDF.
- El cambio de fondo: la validación ya **no es por extensión sino por firma binaria**. La
  extensión y el `Content-Type` los controla el cliente, así que renombrar cualquier archivo a
  `credencial.jpg` bastaba para subirlo a un endpoint público sin autenticación. Ahora el
  backend lee los primeros bytes y compara contra las firmas reales de JPEG y PNG.
- Se corrigió el texto de ayuda del uploader, que seguía anunciando las reglas viejas.

**Estructura (H-01.6, H-01.8)**
- Sección 2 renombrada de "Datos de la Queja" a "Lugar de los hechos".
- Campo nuevo "Segundo Apellido del denunciado" en las tres capas
  (`apellidoMaternoDenunciado` / `apellido_materno_denunciado`), nullable.

**Previsualización de archivos (H-01.10)**
- Miniatura de cada imagen cargada, con nombre, peso y botón de quitar, tanto para la
  credencial como para las evidencias. Los adjuntos que no son imagen muestran su extensión.
- Los object URL se revocan al quitar el archivo y en `ngOnDestroy` — si no, cada archivo
  seleccionado y descartado deja memoria reservada en el navegador.

**Aviso de privacidad (H-01.12)**
- Componente nuevo `shared/aviso-privacidad`. Modal bloqueante al entrar a `/queja/registro`;
  la casilla "He leído y acepto" se habilita solo tras deslizar el texto hasta el final, y al
  marcarla se cierra. Botón "No acepto" que sale del formulario: el consentimiento tiene que
  poder negarse.
- Caso borde cubierto: si el texto cabe sin scroll (pantalla grande o zoom reducido) nunca
  habría evento de scroll y la casilla quedaría deshabilitada para siempre; se detecta al
  renderizar y se habilita sola.
- La aceptación se persiste con la queja (`aviso_privacidad_aceptado`, `_fecha` puesta por el
  servidor, `_version`) y el backend rechaza toda queja pública que no la traiga en `true`.
- **Pendiente**: el texto del aviso es genérico y debe revisarlo alguien con criterio legal
  antes de darlo por definitivo. No inventa correo de contacto (remite a la sección Contacto).

**H-01.9 — tutor de menores**: no requirió cambios, el usuario confirmó que ya funciona bien.

**H-01.11 — CAPTCHA y rate limiting: pospuesto por instrucción explícita del usuario.** Queda
anotado que `/registro-publico` es anónimo y acepta hasta 100MB por petición, así que hoy nada
impide llenar `queja_evidencias` con un script. Conviene retomarlo antes de difundir el portal.

### Arquitectura de la solución
- Paquete nuevo `queja-service/.../validacion/` con las reglas en un solo lugar
  (`ReglasQueja`), validadores separados por responsabilidad (`ValidadorCorreo`,
  `ValidadorFechas`, `ValidadorArchivos`, `DetectorTipoArchivo`) y un orquestador
  (`ValidadorQuejaPublica`) que además **normaliza** el request (trim, espacios colapsados,
  correo en minúsculas) antes de validar.
- Espejo en el frontend: `core/validaciones/reglas-queja.ts`, con los mismos límites. El
  servidor sigue siendo la fuente de verdad; el front solo existe para dar el error al
  instante. Ambos archivos se referencian entre sí en comentarios para que no se desincronicen.
- Columna `tipo` nueva en `queja_evidencias` (`IDENTIFICACION` | `EVIDENCIA`), con backfill
  por el prefijo del nombre. Antes la credencial solo se distinguía por el prefijo
  `IDENTIFICACION_` en el nombre del archivo.

### Compatibilidad y orden de despliegue
`BD → Frontend → Backend`, y el orden no es negociable: el frontend nuevo funciona contra el
backend viejo (Spring ignora los campos multipart que no conoce), pero el backend nuevo exige
`avisoPrivacidadAceptado` y tumbaría el formulario viejo. Con este orden no hay ventana de
caída. Documentado en `docs/DESPLIEGUE-CU-Q01.md` con las 20 pruebas a repetir ya desplegado.

### Verificación previa a la entrega
- Frontend: `ng build` compila limpio (AOT, incluido el type-check de plantillas).
- Backend: el paquete `validacion` compila con `javac` (Java 21) y pasa **60 de 60** pruebas
  escritas para las reglas — correo en ambos niveles, límites exactos de fecha, un ejecutable
  renombrado a `.jpg`, cantidades y tamaños de la credencial, y el formulario campo por campo.
- El `mvn package` completo no se pudo correr en el entorno de trabajo (sin acceso a Maven
  Central); queda a cargo del usuario antes de subir el jar.

### Incidentes durante el despliegue
- Los comandos de la BD se entregaron asumiendo ejecución desde la laptop; el usuario los
  corrió dentro del servidor → `scp` falló por ruta inexistente. Además `psql` **no está
  instalado en el host**: Postgres corre en el contenedor `defensoria-db`, así que la
  migración va por `podman cp` + `podman exec ... psql`. Corregido en `DESPLIEGUE-CU-Q01.md`.
- Se agregó `\encoding UTF8` / `SET client_encoding` al inicio del script de migración: los
  regex de los `CHECK` llevan acentos y ñ, y una sesión de psql con otra codificación los
  guardaría mal.

### Reorganización: carpeta `basedatos/` en el servidor backend
- **Decisión del usuario**: sacar los `.sql` sueltos de `/apps/aplicaciones/defensoria/back/`
  y concentrarlos en `/apps/aplicaciones/defensoria/basedatos/`, para que la carpeta del
  backend quede solo con lo que se despliega (`artifact/`, `config-files/`, `Dockerfile`,
  `podman-compose.sh`) y los scripts de base de datos tengan su propio lugar.
- Archivos que se mueven: `chatbot_seed.sql`, `dependencias_seed.sql`,
  `defensoria_db_estructura.sql`, `defensoria_db_completo_20260817.sql` (~207MB).
- Se verificó antes de mover que **ningún script ni microservicio los lee por ruta**: son
  archivos de semilla y volcado que solo se usan a mano. `podman-compose.sh` no los toca.
- A partir de aquí, los scripts de migración (`migracion_cu_q01_validaciones.sql` y los que
  sigan) viven también en esa carpeta.

### Reorganización: `respaldos/` y `backups/` a `/apps/utiles/`
- **Decisión del usuario**: sacar también estas dos carpetas de la del backend y llevarlas a
  `/apps/utiles/respaldos` y `/apps/utiles/backups`.
- `respaldos/` **no es un directorio suelto**: está montado como volumen en `admin-service`
  (`-v $BASE_DIR/respaldos:/app/respaldos:Z`), y ahí es donde `RespaldoService` deja los
  `.sql` de los respaldos manuales y automáticos del panel de administración. Moverla sin
  tocar el script habría dejado los respaldos escribiendo en una carpeta recreada vacía en la
  ruta vieja (`podman-compose.sh` hace `mkdir -p` de esa ruta en cada `up-container`), y los
  respaldos históricos habrían quedado huérfanos sin que nada avisara.
- Cambio aplicado en `podman-compose.sh`: variable nueva `RESPALDOS_DIR="/apps/utiles/respaldos"`,
  usada en el `mkdir -p` y en el `-v` del volumen, en lugar de `$BASE_DIR/respaldos`.
- **`config-files/admin-service/config/admin-service.yml` NO se toca**: `respaldos.directorio`
  apunta a `/app/respaldos`, que es la ruta DENTRO del contenedor y no cambia. Lo único que se
  movió es el lado del host del bind mount.
- `backups/` no está referenciada por ningún script ni servicio; se mueve sin efectos.
- `uploads/` se queda donde está: es un remanente de cuando las evidencias se guardaban en
  disco (hoy van como BYTEA en `queja_evidencias`), tampoco la referencia nadie.
- **Para que surta efecto** hay que subir el `podman-compose.sh` actualizado al servidor y
  recrear el contenedor: `bash podman-compose.sh up-container admin-service`. Mover la carpeta
  sin recrear el contenedor deja al `admin-service` en ejecución con el bind mount viejo.

### Mapa de dependencias de `defensoria_db` y limpieza de datos de prueba
- El usuario pidió mapear todas las tablas antes de borrar los datos de prueba para arrancar
  limpio con la estructura nueva de CU-Q01. Se levantó el mapa cruzando el esquema vivo de la
  base con las entidades JPA de los nueve microservicios.
- **Hallazgo principal: en toda la base existen solo DOS llaves foráneas declaradas** —
  `queja_evidencias.queja_id` y `queja_tutores.queja_id` → `quejas.id`, ambas con
  `ON DELETE NO ACTION`. Todo lo demás se enlaza **por texto**: `primer-contacto-service` y
  `subdefensoria-service` guardan el folio de la queja en una columna suelta (`folio_origen`)
  y sus tablas hijas cuelgan de un `expediente_id` que tampoco es FK.
- Consecuencia práctica: borrar `quejas` no lanza error, no cascadea y no avisa. Los
  expedientes, dictámenes, citas, notas y oficios quedan apuntando a folios inexistentes y los
  paneles los siguen listando. Un `TRUNCATE ... CASCADE` tampoco los alcanza porque no hay
  cascada que seguir — hay que nombrar las 17 tablas una por una.
- Estado al 2026-09-08: 22 tablas, 425 filas. `quejas` 28 (2 con `origen_registro` nulo, de
  antes de que existiera la columna), `queja_evidencias` 52, `queja_tutores` 6,
  `notificaciones` 19, `bitacora_acciones` 64, `usuarios` 9 (cuentas de quejoso),
  `personal_administrativo` 9 (el staff, con lo que se entra a los paneles),
  `dependencias` 209, `preguntas_chatbot` 15, `plantillas_documentos` 3. Las cadenas de
  primer contacto y subdefensoría tienen 1 expediente cada una con sus hijas.
- **Ya había un huérfano antes de tocar nada**: el `expedientes_investigacion` existente tiene
  un `folio_origen` que no corresponde a ningún `folio_subdefensoria` de primer contacto. Es
  justo el modo de falla que el enlace por texto no puede prevenir.
- Plan acordado: respaldo completo → migración CU-Q01 → `TRUNCATE` de 17 tablas con
  `RESTART IDENTITY` (sin `CASCADE`, a propósito: las dos tablas con FK van nombradas en la
  lista, y `CASCADE` arrastraría en silencio cualquier tabla que un servicio haya creado
  después) → `VALIDATE CONSTRAINT` de las diez restricciones, que la migración creó como
  `NOT VALID` y que con las tablas limpias ya se pueden aplicar sobre toda la tabla.
- **No se tocan**: `dependencias` (sin ella el formulario se queda sin selector de lugar de los
  hechos), `personal_administrativo` (borrarla deja al usuario fuera de todos los paneles),
  `preguntas_chatbot` y `plantillas_documentos`.
- El mapa quedó publicado como artefacto ("Mapa de defensoria_db") con el diagrama de
  dependencias, la tabla de las 22 con su veredicto y los comandos en orden.
- **Deuda anotada**: los enlaces por folio deberían ser llaves foráneas reales, o al menos
  tener una verificación periódica de huérfanos. Mientras sigan siendo texto, cualquier
  borrado o corrección de folios puede romper la trazabilidad sin que nada lo reporte.

### Ejecutado: migración CU-Q01 + vaciado de datos de prueba (2026-09-08, 22:5x)
- **Migración aplicada sin errores** en `defensoria_db`: 5 `ALTER TABLE`, `UPDATE 52` del
  backfill de `tipo`, `COMMIT`. Quedaron las 4 columnas nuevas en `quejas`, la columna `tipo`
  en `queja_evidencias` y las 10 restricciones `CHECK`.
- El backfill confirmó que el prefijo `IDENTIFICACION_` sí distinguía bien: de 52 evidencias,
  **43 quedaron como EVIDENCIA y 9 como IDENTIFICACION**. Que solo 9 de las 28 quejas tuvieran
  credencial adjunta indica que el archivo no siempre fue obligatorio en el formulario viejo.
- **Respaldo previo**: `/apps/utiles/backups/antes-cu-q01-2026-09-08.sql`, 273 MB, verificado
  con `grep "PostgreSQL database dump complete"` antes de tocar nada. Ese peso es normal: las
  evidencias van como BYTEA dentro de la base.
- **Vaciado ejecutado**: `TRUNCATE` de las 17 tablas con `RESTART IDENTITY`, en una sola
  transacción. Las 5 tablas de catálogo/configuración quedaron intactas — `dependencias` 209,
  `bitacora_acciones` 64, `preguntas_chatbot` 15, `personal_administrativo` 9,
  `plantillas_documentos` 3.
- **Las 10 restricciones se promovieron a `VALIDATE`**: `convalidated = t` en las diez. Ya no
  aplican solo a los registros nuevos, sino a toda la tabla — cualquier `INSERT` o `UPDATE` que
  meta un nombre con números o una boleta con letras es rechazado por la propia base, no solo
  por el backend.
- `bitacora_acciones` se conservó por decisión implícita (borrar es irreversible, conservar no);
  queda como historial de las pruebas previas.
- **Estado**: capa de base de datos del CU-Q01 desplegada y verificada. Siguen Frontend y
  Backend, en ese orden.

### Efecto colateral detectado: `revision-service.registrarManual()` y los CHECK validados
- Al promover las restricciones a `VALIDATE`, dejaron de aplicar solo a lo que escribe
  `queja-service`: valen para **cualquier** escritor de la tabla `quejas`.
- `RevisionQuejaService.registrarManual()` (panel del recepcionista, registro de documentos
  físicos) escribe `nombre_quejoso`, `apellido_paterno_quejoso` y `apellido_materno_quejoso`
  con una sola comprobación de "no vacío" — sin validar formato.
- Consecuencia: si un recepcionista captura un nombre con dígitos, el `INSERT` ahora lo rechaza
  **la base de datos**, y el usuario ve un error crudo de violación de restricción (500) en vez
  de un mensaje útil. La integridad queda protegida, que es lo correcto, pero la experiencia es
  mala.
- **No bloquea el despliegue de CU-Q01** (es otro caso de uso, aún sin validar). Queda anotado
  como lo primero a revisar cuando se valide el CU del registro manual: reutilizar
  `ValidadorQuejaPublica`/`ReglasQueja` desde `revision-service`, o replicar el regex de nombre
  ahí mismo.
- Mismo razonamiento para el registro autenticado (`/registrar` en `queja-service`): tampoco
  pasa por el validador nuevo, solo el público lo hace.

### Jars a redesplegar
- **Solo `quejas-service.jar`** (carpeta `queja-service`). Ningún otro microservicio se tocó:
  los 14 archivos `.java` modificados están todos bajo `Backend/queja-service/`.
- `revision-service` comparte la tabla `quejas` pero su entidad no mapea las columnas nuevas.
  No necesita redesplegarse para que nada se rompa (Hibernate ignora columnas que no mapea),
  pero **el panel del recepcionista no mostrará** `apellido_materno_denunciado` ni la
  constancia del aviso de privacidad hasta que se le agreguen esos campos a su propia entidad.

## 2026-09-09 — Autocompletado del catálogo de dependencias

- **Problema reportado por el usuario** al revisar CU-Q01: el `<select>` de "Lugar donde
  sucedieron los hechos" tiene 209 opciones y deslizarlo es impráctico, sobre todo en celular.
  Además nadie piensa en el nombre oficial: piensa "soy de ESCOM", no "Escuela Superior de
  Cómputo".
- Componente nuevo `shared/autocompletar-dependencia/`, `ControlValueAccessor` como el
  datepicker, que sigue exponiendo la **clave** al formulario. Backend y base de datos no se
  tocan: para ellos no cambió nada.
- **Filtrado en el cliente, no en el servidor**, y a propósito: el catálogo completo (~40 KB)
  ya se descargaba de todos modos porque el `<select>` lo necesitaba entero, así que buscar en
  memoria responde al instante, aguanta una red lenta y no le pega al backend una vez por
  tecla. Con decenas de miles de registros la decisión se invertiría; el documento explica qué
  cambiaría exactamente en ese caso.
- Búsqueda por **clave, abreviatura y nombre**, con puntaje por calidad de la coincidencia
  (siglas exactas > prefijo de siglas > inicio del nombre > todas las palabras escritas como
  prefijo de alguna palabra del nombre > subcadena). Sin esa escala, `ESCOM` empataría con
  cualquier dependencia que contenga "com" y la que el usuario quiere quedaría enterrada.
- **Desempate por tipo**: a igual puntaje van primero las unidades académicas. Sin esto,
  `computo` ponía "División de Cómputo" (área administrativa) arriba de "Escuela Superior de
  Cómputo" solo porque el nombre es más corto. Son 45 escuelas de 209 dependencias, pero son el
  destino de casi todas las quejas.
- Normalización de acentos **carácter por carácter** en vez del `normalize('NFD').replace()`
  habitual: descomponer la cadena completa la alarga (una `ó` pasa a ser dos caracteres) y las
  posiciones dejarían de corresponder con el texto original, que es justo lo que hace falta
  para resaltar el tramo coincidente.
- El resaltado se arma partiendo el nombre en tres `<span>`, **sin `innerHTML`**: insertar HTML
  con texto que viene de la base es el patrón que abre un XSS.
- **No se puede escribir texto libre**: el componente guarda por separado lo escrito y la clave
  elegida, y en cuanto se teclea algo la clave se borra. Escribir "mi escuela" y enviar manda
  vacío, no un lugar inventado. Se optó por avisar en vez de borrarle el texto al usuario.
- `Enter` con la lista abierta lleva `preventDefault()`: el campo vive dentro de un `<form>` y
  sin eso elegir una escuela con Enter enviaría la queja a medio llenar.
- Patrón *combobox* de ARIA completo (`aria-expanded`, `aria-activedescendant`, roles
  listbox/option) — en un portal de gobierno la accesibilidad no es opcional.
- Verificado contra el catálogo real: `ESCOM` → 1 resultado exacto; `escuela superior co` →
  ESCOM primero; `computo` y `cómputo` → ESCOM primero; `esime zac` → ESIME Zacatenco (acierta
  por la **abreviatura**, no por la clave, que lleva guion); `cecyt 9`, `upiicsa`, `medicina`,
  `zacatenco` correctos.
- **Solo se cambió `registro-queja-publico`.** Siguen con el `<select>` viejo:
  `panel/nueva-queja`, `panel/perfil`, `panel/mis-quejas` y `panel/queja-detalle`. Son una
  etiqueta cada una, pero en las dos de filtro hay que decidir antes qué pasa con la opción
  "todas", que el autocompletado no contempla.
- Documentado en `docs/AUTOCOMPLETADO-DEPENDENCIAS.md`.

### 🔴 Bug encontrado al probar CU-Q01 desplegado: el datepicker generaba fechas inexistentes

- **Síntoma**: al enviar la queja, toast "Ocurrió un error inesperado en el servidor" y en el
  log del contenedor un `MethodArgumentNotValidException` con
  `rejected value [2004-21-19]` → `POST /registro-publico -> 500`.
- **Causa raíz**: en `shared/datepicker/datepicker.html`, los `<select>` de mes y año usaban
  `[value]="i"` en el `<option>`. `value` es un atributo del DOM, así que `ngModel` guarda el
  índice como **cadena**, no como número. En `crearCelda()` la fecha se arma con
  `String(mes + 1)`, y con una cadena eso concatena en vez de sumar: seleccionar **Marzo**
  (índice `"2"`) producía `"2" + 1 = "21"` y la fecha `2004-21-19`.
- Por qué el calendario **se veía bien**: `new Date(anio, mes, dia)` convierte sus argumentos a
  número, así que la rejilla de días se pintaba correcta. Solo la cadena ISO —la que se manda
  al backend— salía mal. El error era invisible en pantalla.
- **Es un bug anterior** (el archivo no se había tocado desde el 20 de agosto), no lo
  introdujeron los cambios de CU-Q01. Salió a la luz ahora porque el usuario usó el desplegable
  de mes para llegar a 2004 en vez de las flechas.
- **Afecta a las 4 pantallas** que usan el datepicker: `registro-queja-publico`,
  `panel/nueva-queja`, `panel/mis-quejas` y `panel/queja-detalle`. La corrección es en el
  componente compartido, así que las cuatro quedan arregladas de una vez.

**Corrección en tres capas** (una sola no bastaba):

1. **Datepicker** — `[ngValue]` en vez de `[value]` en ambos `<select>`, que conserva el tipo
   numérico. Más `Number()` defensivo en `cambiarMesVista()`, `crearCelda()` y `semanas()`, por
   si algo vuelve a dejar esos valores como texto.
2. **Validación del formulario** — nueva función `esFechaValida()` en `reglas-queja.ts`, usada
   por `errorFechaNacimiento` y `errorFechaHechos`. Comparar cadenas no alcanzaba:
   `"2004-21-19"` es mayor que `"1920-01-01"` y menor que `"2025-12-31"`, así que pasaba ambos
   límites y llegaba al servidor. Ahora se reconstruye la fecha y se comprueba que año, mes y
   día coincidan con lo escrito — eso descarta tanto meses inexistentes como los "31 de
   febrero", que JavaScript convertiría silenciosamente en 3 de marzo.
3. **Backend** — `GlobalExceptionHandler` ahora maneja `BindException` (de la que hereda
   `MethodArgumentNotValidException`) y devuelve **400** nombrando el campo y el valor
   rechazado, en vez de caer en el manejador genérico y salir como 500 "Ocurrió un error
   inesperado en el servidor". Ese mensaje era engañoso por partida doble: no era un error del
   servidor sino del dato enviado, y no le decía al usuario qué corregir. Se agregó también un
   manejador de `MaxUploadSizeExceededException` (413) por el mismo motivo.

**Lección anotada**: la validación del frontend comparaba cadenas de fecha con `<` y `>` sin
verificar que la fecha existiera. Comparar fechas ISO como texto funciona para ordenar, pero no
valida nada — cualquier cadena con el formato correcto pasa.

## 2026-09-09 — CU-Q07: consultar, editar y retirar mis quejas

Segundo caso de uso de la ronda de validación. Cambios en `panel/mis-quejas` y
`panel/queja-detalle`, más endpoints nuevos en `queja-service`.

### 🔴 Hallazgo previo: el panel le mentía al quejoso sobre su estatus
- `etiquetaEstatus()` del frontend traducía `RECIBIDA`, `EN_REVISION` y `FINALIZADA`. De esos,
  **el backend solo emite el primero**: `RevisionQuejaService` produce `EN_VALIDACION`,
  `TURNADA` y `RECHAZADA`.
- Como el traductor caía en `default: 'Recibida'`, una queja **rechazada o turnada se le
  mostraba al quejoso como "Recibida"**. Alguien cuya queja fue rechazada seguía viendo que
  estaba en trámite.
- Corregido: los códigos del modelo ahora son los reales, más `CANCELADA`. Se agregaron
  `claseEstatus()`, `esEditable()` y `estaCerrada()` para que ninguna pantalla vuelva a
  comparar cadenas de estatus a mano — que es como se desincronizó.
- Arrastró correcciones en `resumen` (contaba "Finalizadas", que nunca existieron; ahora cuenta
  "Cerradas") y en `consultar-queja`. Los colores de la insignia se movieron a `styles.scss`:
  cada pantalla tenía su copia y ya estaban distintas entre sí.

### Eliminar queja = cancelar, no borrar
- Decisión del usuario tras exponerle el trade-off: `DELETE /api/quejoso/quejas/mias/{folio}`
  **no borra**, marca la queja como `CANCELADA`. En un sistema de quejas institucional,
  destruir el registro elimina la constancia de que la queja existió — y si alguien la
  presentó y luego la retiró bajo presión, no quedaría ningún rastro.
- Solo se permite mientras la queja siga en `RECIBIDA`, igual que la edición: una vez que el
  recepcionista empezó a validarla ya hay trabajo institucional invertido.
- El diálogo de confirmación se lo dice al usuario con todas sus letras: el registro se
  conserva y no podrá reactivarla por su cuenta.

### Endpoints nuevos en queja-service
| Método | Ruta | Qué hace |
|---|---|---|
| DELETE | `/mias/{folio}` | Retira (cancela) la queja. Solo en RECIBIDA |
| POST | `/mias/{folio}/evidencias` | Agrega evidencias. Solo en RECIBIDA |
| DELETE | `/mias/{folio}/evidencias/{id}` | Quita una evidencia. Solo en RECIBIDA |
| GET | `/mias/{folio}/evidencias/{id}/contenido` | Sirve el archivo para previsualizarlo |

- La evidencia se busca **dentro de la queja del usuario**, no por id suelto en la tabla: así
  nadie puede borrar la evidencia de otra persona mandando un id ajeno.
- **No se puede quitar la última identificación oficial**: la queja se quedaría sin con qué
  acreditar quién la presentó y el recepcionista tendría que rechazarla.
- El endpoint de contenido exige JWT, así que la miniatura **no** se puede poner como
  `<img src="...">` — una etiqueta `<img>` no manda cabeceras. El frontend pide el archivo con
  HttpClient (que sí pasa por el interceptor del token) y arma un object URL con el blob.

### Mis Quejas: de cinco filtros a un buscador
- Se quitaron los filtros de folio, asunto, unidad académica, fecha y estatus. Eran **cinco
  campos para una lista que en la práctica tiene entre una y cinco quejas** — en la captura del
  usuario había cinco filtros encima de un solo renglón.
- En su lugar: un buscador que cubre folio, asunto y escuela a la vez (el usuario escribe lo
  que recuerda sin decidir en qué campo va) y pestañas **Todas / En trámite / Cerradas** con su
  conteo. Ninguna información se perdió: sigue toda en la tabla.
- El filtro de unidad académica se fue porque un quejoso se queja de su propia escuela; el de
  fecha exacta, porque con menos de diez renglones no ayuda a nadie.
- Acciones como iconos: ojo, lápiz y bote. Editar y eliminar se **deshabilitan** cuando la
  queja salió de RECIBIDA, con el motivo en el `title` en vez de desaparecer — un botón que se
  esfuma no explica nada.

### Detalle de queja
- **Línea del tiempo arriba**, horizontal, con cuatro pasos: Recibida → En validación → Turnada
  al área → En atención. `RECHAZADA` y `CANCELADA` no son un paso más sino finales
  alternativos, y cortan la línea en rojo en vez de avanzarla.
- Son cuatro y no los seis del ejemplo que mandó el usuario **porque son los cuatro que hoy se
  pueden comprobar con un dato real**. Los seis se veían mejor, pero tres no tendrían de dónde
  encenderse y la barra se quedaría clavada — peor que mostrar menos. Cuando primer contacto y
  subdefensoría expongan su avance se amplían.
- Cuando la queja fue rechazada se muestra el **motivo del rechazo**, que el backend ya
  devolvía pero el modelo del frontend ni siquiera declaraba.
- **Panel de evidencias** en la columna derecha, con miniatura de cada imagen, tamaño, etiqueta
  de "Identificación" para la credencial, y botón para abrir el archivo completo. Agregar y
  quitar habilitado solo mientras la queja siga en RECIBIDA.
- **Campos que faltaban y ahora se muestran**: nombre completo del quejoso, correo, número de
  boleta o empleado, fecha de nacimiento, **segundo apellido del denunciado**, datos del tutor
  y constancia del aviso de privacidad. El backend ya los devolvía todos; la pantalla
  simplemente no los pintaba.
- El formulario de edición ahora usa el autocompletado de dependencias y aplica las mismas
  validaciones de nombres y fechas que el formulario público.

### Iconos: lucide-angular
- Se instaló `lucide-angular@1.0.0` (compatible con Angular 13–21).
- Catálogo único en `shared/iconos/iconos.ts` con nombres en español (`ICONOS.ojo`,
  `ICONOS.lapiz`, `ICONOS.bote`) y registro selectivo con `pick()` en `app.config.ts`: Lucide
  trae más de 1500 iconos y solo entran al bundle los 16 declarados.
- Se documentó en `docs/ICONOS.md`, incluyendo cómo reutilizarlos en el login.
- Se le propuso al usuario en vez de descargar archivos: los vectoriales heredan el color con
  `currentColor` (un mismo icono sirve gris deshabilitado y guinda al pasar el mouse, sin un
  archivo por color), se ven nítidos en retina y no agregan peticiones HTTP.
