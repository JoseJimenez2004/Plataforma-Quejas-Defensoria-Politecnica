# Despliegue (estado actual, 1 VPS)

Todo se ejecuta a mano por SSH sobre la VPS (2.25.78.22), con Podman como motor de
contenedores. No hay CI/CD ni docker-compose: son 2 scripts shell + comandos sueltos.

## 1. Base de datos

Ya está levantada y persistente, no forma parte de los scripts de despliegue:

```bash
podman run -d --name defensoria-db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=Temporal2026@ \
  -e POSTGRES_DB=defensoria_db \
  -p 5432:5432 --restart=always \
  docker.io/library/postgres:16
```

## 2. Backend — `Backend/podman-compose.sh`

Construye **una sola imagen** (`defensoria-base-img`) reutilizada por los 3 microservicios;
lo único que cambia es qué JAR se copia al construirla y qué puerto/config se le pasa al correr.

- `Dockerfile`: `FROM eclipse-temurin:21-jre-jammy`, copia `artifact/<SERVICIO>.jar` como
  `app.jar`. El `EXPOSE 8080` es solo documentación — el puerto real lo define cada
  `application.yml` de producción (`server.port`), no el Dockerfile.
- El JAR de cada servicio debe generarse antes (`mvn package` en cada carpeta
  `auth.service/`, `queja-service/`, `notificaciones-service/`) y copiarse a
  `Backend/artifact/<servicio>.jar` en el VPS.
- `get_port()` mapea cada servicio a su puerto: auth 8083, quejas 8084, notificaciones 8085.
  **Estos deben coincidir con el `server.port` dentro de `config-files/<servicio>/config/*.yml`**
  (ya lo verifiqué: coinciden).

Comandos:

```bash
sh podman-compose.sh up                          # (re)construye y levanta los 3
sh podman-compose.sh up-container auth-service    # solo uno
sh podman-compose.sh delete                       # detiene y borra los 3
sh podman-compose.sh delete-container quejas-service
```

Cada contenedor monta `config-files/<servicio>/config` como volumen y usa
`SPRING_CONFIG_ADDITIONAL_LOCATION` + `SPRING_CONFIG_NAME` para que Spring Boot cargue
esa config externa **por encima** de la que trae el JAR (`src/main/resources/application.yaml`
de cada proyecto Maven es solo el default de desarrollo).

⚠️ Nota: `QUEJAS_SERVICE_URL` se pasa como variable de entorno fija
(`http://2.25.78.22:8084`) en el script, apuntando a la IP pública en vez de a la red interna
del contenedor. Funciona porque todo vive en el mismo host, pero es más frágil de lo necesario
(depende de que el hairpin NAT/loopback público funcione). Revisar si conviene usar una red
Podman compartida (`--network`) y nombres de contenedor en vez de IP pública.

## 3. Nginx — `nginx/podman-ngnix.sh`

```bash
sh podman-ngnix.sh up     # levanta/reemplaza el contenedor defensoria-nginx en :80
sh podman-ngnix.sh down   # lo detiene y elimina
```

Monta dos volúmenes de solo lectura:
- `nginx/config/defensoria.conf` → `/etc/nginx/conf.d/default.conf`
- `front/` (build estático de Angular) → `/usr/share/nginx/html`

Esto significa que **el build de producción del Angular (`ng build`) debe copiarse a
`front/` en el VPS** antes de levantar/reiniciar Nginx — hoy `front/` está vacío, por eso
aunque arreglemos el código, todavía falta el paso de build + copia para que se vea algo.

## Checklist para desplegar una actualización del backend

1. `mvn clean package` en el servicio modificado (genera el nuevo `target/*.jar`).
2. Copiar el jar a `Backend/artifact/<servicio>.jar` en el VPS.
3. `sh podman-compose.sh up-container <servicio>`.
4. Verificar con `podman ps -a` y `podman logs <servicio>`.

## Checklist para desplegar el frontend (cuando exista el build)

1. `ng build --configuration production` dentro del proyecto Angular.
2. Copiar el contenido de `dist/<app>/browser` a `front/` en el VPS.
3. `sh podman-ngnix.sh up` (recrea el contenedor para que tome los archivos nuevos —
   ojo, al ser volumen de solo montaje, en teoría ni hace falta recrear el contenedor,
   basta con reemplazar los archivos; solo se necesita recrear si cambia `defensoria.conf`).
