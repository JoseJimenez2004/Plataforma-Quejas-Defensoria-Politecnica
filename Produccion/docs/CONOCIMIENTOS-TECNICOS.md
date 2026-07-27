# Conocimientos técnicos detrás de la plataforma DDP

Inventario de todo lo que hay que dominar (o al menos entender bien) para haber diseñado,
construido y desplegado este proyecto tal como está hoy: 6 microservicios en Spring Boot,
3 frontends en Angular, Nginx como proxy inverso, Postgres compartido, y todo corriendo en
contenedores Podman sobre dos VPS. Organizado por capa.

---

## 1. Arquitectura general

- **Microservicios vs monolito**: por qué se dividió en `auth-service`, `quejas-service`,
  `notificaciones-service`, `catalogo-service`, `admin-service`, `revision-service` en lugar
  de una sola aplicación. Cada uno con responsabilidad única, su propio puerto, su propio
  Dockerfile/imagen, pero **compartiendo una sola base de datos** (Postgres) — un híbrido
  entre "microservicios puros" (BD por servicio) y monolito, elegido a propósito por
  simplicidad operativa.
- **Comunicación entre servicios**: síncrona vía REST (`RestTemplate`), no colas de mensajes
  ni eventos. Ejemplo: `admin-service` y `revision-service` llaman a `catalogo-service` y a
  `notificaciones-service` por HTTP directo.
- **Patrón de tabla compartida**: dos servicios distintos (`queja-service` y
  `revision-service`) tienen cada uno su propia clase `@Entity` mapeando la MISMA tabla física
  `quejas`. Hibernate con `ddl-auto: update` permite que cada servicio agregue columnas nuevas
  sin pisar al otro. Hay que entender el riesgo de esto (dos aplicaciones escribiendo la misma
  tabla sin coordinarse) y por qué aun así es una decisión razonable a esta escala.
- **Máquina de estados de una queja**: `RECIBIDA → EN_VALIDACION → RECHAZADA / TURNADA`, y
  cómo cada pantalla del recepcionista corresponde a una transición de estado.
- **Autenticación centralizada, sin login propio por servicio**: solo `auth-service` (quejoso)
  y `admin-service` (todo el personal, incluido recepcionista) emiten JWT. Los demás servicios
  (`revision-service`, `catalogo-service`, etc.) solo **verifican** el token — nunca lo emiten.
  Esto evita duplicar lógica de login y contraseñas en cada microservicio.

## 2. Backend — Java / Spring Boot

- **Java 21** y **Maven**: ciclo `mvn clean package`, `pom.xml` (dependencias, plugins), y el
  detalle importante de este proyecto: **cada microservicio es un proyecto Maven
  independiente**, sin `pom.xml` padre/agregador — por eso `mvn -pl <servicio>` no funciona
  desde `Backend/`, hay que entrar a la carpeta del servicio y compilar ahí directo.
- **Spring Boot**: autoconfiguración, `application.yaml` vs variables de entorno
  (`SPRING_CONFIG_ADDITIONAL_LOCATION`, `SPRING_CONFIG_NAME`) para inyectar configuración de
  producción sin reconstruir la imagen.
- **Spring Web (`@RestController`)**: DTOs de request/response (`model/`), separación entre
  entidad de persistencia y el objeto que viaja por HTTP.
- **Spring Data JPA**: entidades (`@Entity`, `@Column`), repositorios (`JpaRepository`) y
  **derivación de queries por nombre de método** (`findByEstatus`, `existsByCorreo...`) sin
  escribir SQL. `ddl-auto: update` (y sus riesgos en producción vs `validate`/`none`).
- **Spring Security + JWT**:
  - Estructura de un JWT (header.payload.firma), firma HMAC con un secreto compartido entre
    los 6 microservicios.
  - Diferencia entre un servicio que **emite** tokens (`JwtUtil.generarToken`, con contraseña
    + `passwordEncoder`) y uno que solo **verifica** (`JwtAuthenticationFilter` leyendo el
    claim `rol`).
  - `@PreAuthorize("hasRole('...')")` para autorización por endpoint/controlador.
  - CORS (`WebConfig`) para permitir que el frontend en otro dominio/puerto llame a la API.
- **BCrypt** para hash de contraseñas — nunca texto plano, y por qué `matches()` no es
  reversible.
- **Manejo de errores centralizado**: `@ControllerAdvice` / `GlobalExceptionHandler` para no
  repetir try/catch en cada controlador.
- **Apache POI**: generación de archivos Excel desde Java (usado en el export de historial del
  recepcionista).
- **Logging de peticiones**: un `Filter` (`RequestLoggingFilter`) que registra cada request
  entrante, útil para depurar en producción sin acceso a debugger.
- **OpenAPI/Swagger**: documentación automática de endpoints (`/swagger-ui.html`,
  `/v3/api-docs`).
- **Descargas de archivos protegidas por JWT**: por qué un `<a href>` normal no sirve (no manda
  el header `Authorization`) y hay que pedir el archivo como `Blob` desde el cliente.

## 3. Frontend — Angular

- **Angular standalone components** (sin `NgModule`), imports explícitos por componente.
- **Angular 21 zoneless**: qué significa que la detección de cambios ya no dependa de
  `zone.js`, y por qué hubo que escribir un interceptor manual (`ApplicationRef.tick()`) para
  forzar refresco de vista tras cada respuesta HTTP.
- **Routing**: rutas *lazy* (`loadComponent`), rutas anidadas con `children`, *guards*
  (`CanActivate`) para proteger secciones según si hay sesión/rol válido.
- **`HttpClient` + interceptores funcionales** (`HttpInterceptorFn`): uno para inyectar el JWT
  en cada request, otro para el refresco de vista (zoneless).
- **RxJS básico**: `Observable`, `subscribe`, manejo de errores de una petición HTTP.
- **Formularios**: *template-driven* (`[(ngModel)]` + `FormsModule`) usados aquí, vs la
  alternativa *reactive forms* — y el error típico de usar `ngModel` sin importar
  `FormsModule` en el componente standalone.
- **SCSS con variables CSS (custom properties)**: sistema de diseño compartido
  (`--acento-azul`, `--sombra-suave`, etc.) reutilizado entre los tres frontends para
  consistencia visual.
- **Build de producción** (`ng build --configuration production`): entender que con
  `outputPath: dist/<nombre-proyecto>` el resultado real queda en
  `dist/<nombre-proyecto>/browser/`, y que el `Dockerfile` debe apuntar a esa subcarpeta
  correcta (fuente de más de un error de despliegue en este proyecto).
- **`base-href`**: por qué cada frontend se compila con un `--base-href` distinto
  (`/admin/`, `/revision/`) para que las rutas funcionen detrás de un proxy con subcarpeta.

## 4. Infraestructura / DevOps

- **Contenedores con Podman** (equivalente *rootless* a Docker): `podman build`, `podman run`,
  `podman ps -a`, `podman logs -f <contenedor>`, `podman stop`/`rm`, `podman restart`.
- **Dockerfile**: `FROM`, `ARG` de build (`JAR_FILE`, `SERVICE_PORT`), `COPY`, `EXPOSE`,
  `ENTRYPOINT`. Por qué algunos servicios (`admin-service`) tienen su propio `Dockerfile`
  distinto al compartido (necesitan `postgresql-client` instalado para `pg_dump`).
  Diferencia entre imagen (`FROM nginx:alpine` para servir estáticos, vs
  `FROM eclipse-temurin:21-jre-jammy` para correr un `.jar`).
- **Bind mounts vs volúmenes nombrados**: la config de cada servicio se monta como bind mount
  (`-v host:/app/config:Z`) para poder editarla sin reconstruir la imagen — y la lección dura
  de este proyecto: **editar el archivo en el host no basta si el contenedor tiene el bind
  mount cacheado por inode; hay que `podman restart`, no solo recargar.**
- **Nginx con dos roles distintos**:
  - Como **proxy inverso** (`router-nginx`): bloques `location /api/... { proxy_pass ...; }`
    enrutando tráfico HTTPS público hacia los puertos internos de cada backend/frontend.
  - Como **servidor de estáticos** dentro de cada contenedor de frontend: `root`,
    `try_files $uri $uri/ /index.html` (fallback obligatorio para que el router de Angular
    funcione al recargar una URL interna).
- **Dos VPS separadas** (backend en `2.25.78.22`, frontend/router en `2.25.64.47`) y por qué
  conviene aislar responsabilidades así — cómputo/BD por un lado, entrada pública HTTPS por
  el otro.
- **PostgreSQL en producción**: conexión remota entre VPS (`postgresql.conf` `listen_addresses`,
  `pg_hba.conf`), respaldo/restauración con `pg_dump`/`psql`, y por qué la imagen que ejecuta
  `pg_dump` necesita el cliente de Postgres instalado aunque la base viva en otro contenedor.
- **HTTPS/dominio**: DNS dinámico (`.ddns.net`) y certificado TLS (típicamente Let's Encrypt /
  certbot) frente al dominio público.
- **Firewall** (`ufw` o similar): qué puertos exponer al público (443) vs solo entre VPS
  (8083-8088, 5432) — y cómo diagnosticar un `504 Gateway Timeout` distinguiendo "el servicio
  no corre" de "el firewall bloquea el puerto".

## 5. Linux / línea de comandos

- Navegación y archivos: `cd`, `ls -la`, `mkdir -p`, `mv`, `rmdir`.
- Edición de texto en servidor: `nano` (o `vim`).
- Permisos: `chmod +x` para scripts ejecutables.
- Transferencia de archivos: `scp -r` (subir carpetas completas conservando estructura).
- Diagnóstico: `curl -s -o /dev/null -w "%{http_code}\n"` para probar un endpoint sin ver el
  cuerpo, `podman logs -f` para seguir un log en vivo, `grep` para filtrar salida.
- Variables de entorno y cómo un script bash (`case` sobre `$1`, arreglos `SERVICIOS=(...)`,
  funciones) orquesta build+deploy de varios servicios desde un solo archivo
  (`podman-compose.sh`).

## 6. Seguridad aplicada en el proyecto

- JWT compartido entre microservicios: ventaja (simplicidad, un solo punto de emisión) vs.
  la alternativa "correcta" a mayor escala (OAuth2/OIDC con un Authorization Server real,
  rotación de claves, tokens de corta duración + refresh).
- Contraseñas: hashing con BCrypt, políticas de complejidad (regex con mayúscula + dígito +
  longitud mínima), cuentas temporales que fuerzan cambio de contraseña en el primer login.
- CORS configurado explícitamente en vez de permitir todo origen.
- Principio de menor privilegio por rol (`@PreAuthorize`) — cada endpoint declara qué rol
  puede tocarlo.

## 7. Buenas prácticas de diseño aplicadas

- Separación DTO (`model/`) vs entidad de persistencia (`entity/`) — nunca exponer la entidad
  JPA directo en la respuesta HTTP.
- Responsabilidad única por microservicio y por clase (`Controller` → `Service` →
  `Repository`).
- Configuración externalizada del código (variables de entorno + archivos `.yml` montados),
  para que la misma imagen sirva en cualquier ambiente sin reconstruirse.
- Consistencia de patrones entre servicios nuevos y viejos: `revision-service` se construyó
  copiando deliberadamente la misma estructura de carpetas, filtros y manejo de errores que
  ya existía en `admin-service`, en vez de inventar un estilo nuevo.

---

### Cómo usar este documento

No es necesario dominar todo antes de tocar el proyecto — es un mapa de qué researchear
cuando algo falle. Si un despliegue da error, ubica primero en qué capa está (¿Java no
compila? → sección 2. ¿el contenedor no responde? → sección 4. ¿la ruta del navegador da
404? → secciones 3 y 4 sobre Nginx/`base-href`) y profundiza ahí.
