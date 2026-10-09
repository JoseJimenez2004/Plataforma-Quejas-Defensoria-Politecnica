# Versión final TT — despliegue en 3 VPS

Última actualización: 2026-10-07. Este documento **reemplaza** a `Backend/ARQUITECTURA-3-SERVIDORES.md`,
`Backend/CONFIGURACION-BD.md` y `Backend/actualizar-ip-bd.sh` (se conservan solo como historial).

## 1. Los 3 servidores

| Servidor | IP | Qué corre |
|---|---|---|
| **BD** | `169.58.62.99` | `defensoria-db` (Postgres 16, :5432, `defensoria_db`) y `historico-db` (Postgres 16, :5433, `defensoria_historico_db`) |
| **Backend** | `156.67.26.73` | 10 microservicios Spring Boot (8082–8092) + `antecedentes-service` (modelo IA, 8093) |
| **Frontend** | `169.58.62.111` | `router-nginx` (80/443, HTTPS) + `defensoria-web` (8090) + `admin-web` (8091) + `revision-web` (8092) |

```
Internet ──443──> 169.58.62.111 (router-nginx) ──/api/*──> 156.67.26.73:8082-8092 ──5432/5433──> 169.58.62.99
```

Reglas de red:
- 5432 y 5433 del servidor BD: **solo** desde `156.67.26.73`.
- 8082–8093 del backend: **solo** desde `169.58.62.111`.
- 80/443 del frontend: abiertos a todo el mundo.

> ⚠️ Podman publica puertos con reglas DNAT de iptables que **se saltan ufw** (igual que Docker).
> Un `ufw allow from X to any port 5432` no protege nada. Por eso el script de BD bloquea en
> la tabla `raw` de iptables (`bash podman-compose-bd.sh firewall`). Comprueba siempre desde
> fuera que el puerto esté cerrado.

## 2. Decisiones de esta versión

- **Bases vacías.** No se migran datos de los VPS viejos. Las tablas las crea Hibernate
  (`ddl-auto: update`) la primera vez que arranca cada microservicio.
- **El histórico vive en su propio contenedor** (`historico-db`, puerto 5433), base
  `defensoria_historico_db`. Otra persona lo poblará; solo lo usa `historico-service`.
- **Una sola contraseña de Postgres** para los dos contenedores y los `config-files`. Vive en
  `POSTGRES_PASSWORD` de `Backend/podman-compose-bd.sh` y en `spring.datasource.password`
  (+ `db.password` de admin-service) de cada `config-files/*/config/*.yml`. Si se cambia, se
  cambia en los dos lados (antes los config-files tenían un typo, `Temportal…`, y no conectaban).
- **Los seeds ya no corren al crear el contenedor.** Antes estaban en
  `docker-entrypoint-initdb.d` y fallaban porque las tablas todavía no existen en ese
  momento. Ahora:
  - `database-scripts/init/` → solo configuración (extensión `uuid-ossp`, zona horaria). Corre sola al crear `defensoria-db`.
  - `database-scripts/seeds/` → datos (dependencias, chatbot, personal de prueba). Se corren
    a mano con `bash podman-compose-bd.sh seed` **después** de levantar el backend.
- `primer-contacto-service` y `subdefensoria-service` **no usan el servidor BD**: corren con
  H2 en memoria (así venía de antes). Sus datos se pierden al reiniciar el contenedor. Pendiente aparte.

## 3. Servidor BD — 169.58.62.99

### 3.1 Estructura en el servidor

```
/apps/aplicaciones/defensoria/
├── database/
│   └── podman-compose-bd.sh
└── database-scripts/
    ├── init/
    │   └── 00-init.sql
    └── seeds/
        ├── 01-dependencias_seed.sql
        ├── 02-chatbot_seed.sql
        └── 03-personal_test.sql
/apps/data/postgresql/defensoria_db/   <- datos de defensoria-db (lo crea el script)
/apps/data/postgresql/historico_db/    <- datos de historico-db  (lo crea el script)
/apps/respaldos/bd/                    <- respaldos de "backup"
```

### 3.2 Qué subir (desde la carpeta `Produccion/` de tu compu)

```bash
scp Backend/podman-compose-bd.sh root@169.58.62.99:/apps/aplicaciones/defensoria/database/
scp -r Backend/database-scripts/init Backend/database-scripts/seeds \
    root@169.58.62.99:/apps/aplicaciones/defensoria/database-scripts/
```

### 3.3 Reconstruir desde cero (en el servidor BD)

Los contenedores que existen hoy se crearon con el script viejo: los seeds se montaron en
`initdb.d` y la contraseña no coincide con los config-files. Como están vacíos, se tiran y se
recrean:

```bash
# 1. Tirar los contenedores viejos y sus datos (están vacíos)
podman rm -f defensoria-db historico-db
rm -rf /apps/data/postgresql/defensoria_db /apps/data/postgresql/historico_db

# 2. Quitar los .sql sueltos del script viejo (ahora van en init/ y seeds/)
cd /apps/aplicaciones/defensoria/database-scripts
rm -f *.sql README.md
ls -R            # deben quedar solo init/ y seeds/

# 3. Crear las 2 bases vacías
cd /apps/aplicaciones/defensoria/database
bash podman-compose-bd.sh up

# 4. Cerrar 5432/5433 a todo el que no sea el backend, y que sobreviva a reinicios
bash podman-compose-bd.sh firewall
bash podman-compose-bd.sh firewall-boot
```

Resultado esperado de `bash podman-compose-bd.sh status`:

```
NAMES          STATUS        PORTS
defensoria-db  Up ...        0.0.0.0:5432->5432/tcp
historico-db   Up ...        0.0.0.0:5433->5432/tcp
```

Verificación:

```bash
bash podman-compose-bd.sh psql-defensoria    # \l  -> debe aparecer defensoria_db ; \q
bash podman-compose-bd.sh psql-historico     # \l  -> debe aparecer defensoria_historico_db ; \q
```

Desde el **backend** (156.67.26.73) debe conectar, y desde tu compu **no**:

```bash
timeout 3 bash -c '</dev/tcp/169.58.62.99/5432' && echo ABIERTO || echo CERRADO
timeout 3 bash -c '</dev/tcp/169.58.62.99/5433' && echo ABIERTO || echo CERRADO
```

### 3.4 Seeds (después del paso 4 del backend)

```bash
cd /apps/aplicaciones/defensoria/database
bash podman-compose-bd.sh seed
```

- Carga `dependencias` (catalogo-service), `preguntas_chatbot` (chatbot-service) y
  `personal_administrativo` (admin-service).
- Si una tabla todavía no existe, avisa qué microservicio falta levantar y no hace nada.
- Si `dependencias` o `preguntas_chatbot` ya tienen filas, no las duplica.
  `personal_test.sql` es UPSERT y se puede repetir.
- Cada archivo corre en una sola transacción: o entra completo o no entra nada.
- `personal_test.sql` crea cuentas de prueba (contraseña `Prueba2026!`, ver el archivo). Sin
  ellas nadie puede entrar a los paneles; cámbialas o desactívalas antes de la entrega real.

### 3.5 Para quien cargue el histórico

Sin abrir el puerto a internet, de dos formas:

```bash
# A) Dentro del servidor BD
podman exec -i historico-db psql -U postgres -d defensoria_historico_db < archivo.sql

# B) Desde su compu, por túnel SSH, y luego DBeaver a localhost:5434
ssh -L 5434:localhost:5433 root@169.58.62.99
```

Las tablas de `historico-service` las crea Hibernate cuando ese servicio arranca. Conviene
levantarlo antes de cargar datos para que la estructura sea la que espera el código.

### 3.6 Comandos del día a día

| Comando | Para qué |
|---|---|
| `bash podman-compose-bd.sh status` | Estado |
| `bash podman-compose-bd.sh logs defensoria-db` | Logs |
| `bash podman-compose-bd.sh backup` | Respaldo de las 2 bases a `/apps/respaldos/bd` (.sql.gz) |
| `bash podman-compose-bd.sh restore <archivo>` | Restaura; elige la base por el nombre del archivo |
| `bash podman-compose-bd.sh down` / `up` | Recrea contenedores **conservando** los datos |
| `bash podman-compose-bd.sh reset` | Borra todo y recrea vacío (pide escribir BORRAR) |

Respaldo diario a las 4 AM: `crontab -e` →
`0 4 * * * /bin/bash /apps/aplicaciones/defensoria/database/podman-compose-bd.sh backup`

Los contenedores usan `--restart always` y el script habilita `podman-restart.service`: vuelven solos tras reiniciar el VPS.

## 4. Servidor Backend — 156.67.26.73

### 4.1 Estructura y qué subir

```
/apps/aplicaciones/defensoria/back/
├── Dockerfile                          <- Backend/Dockerfile
├── podman-compose.sh                   <- Backend/podman-compose.sh
├── podman-compose-modelo.sh            <- Modelo-Java/podman-compose-modelo.sh
├── firewall-backend.sh                 <- Backend/firewall-backend.sh
├── admin-service/Dockerfile            <- Backend/admin-service/Dockerfile (trae pg_dump)
├── config-files/                       <- Backend/config-files/ (COMPLETA, incluye historico-service/ nueva)
├── artifact/                           <- los .jar (tabla de abajo)
└── modelo/Dataset/                     <- Modelo-Java/Dataset/*.json
/apps/utiles/respaldos/                 <- respaldos del panel admin (lo crea el script)
```

| Jar en `artifact/` | Sale de |
|---|---|
| `auth-service.jar` | `Backend/auth-service/target/` |
| `quejas-service.jar` | `Backend/queja-service/target/` |
| `notificaciones-service.jar` | `Backend/notificaciones-service/target/` |
| `catalogo-service.jar` | `Backend/catalogo-service/target/` |
| `admin-service.jar` | `Backend/admin-service/target/` |
| `revision-service.jar` | `Backend/revision-service/target/` |
| `chatbot-service.jar` | `Backend/chatbot-service/target/` |
| `primer-contacto-service.jar` | `Backend/primercontacto/target/` |
| `subdefensoria-service.jar` | `Backend/subdefensoria/target/` |
| `historico-service.jar` | `Backend/historico-service/target/` |
| `antecedentes-service.jar` | `Modelo-Java/target/` |

```bash
ssh root@156.67.26.73 'mkdir -p /apps/aplicaciones/defensoria/back/{artifact,admin-service,modelo/Dataset}'
cd Produccion
scp Backend/Dockerfile Backend/podman-compose.sh Backend/firewall-backend.sh Modelo-Java/podman-compose-modelo.sh \
    root@156.67.26.73:/apps/aplicaciones/defensoria/back/
scp Backend/admin-service/Dockerfile root@156.67.26.73:/apps/aplicaciones/defensoria/back/admin-service/
scp -r Backend/config-files root@156.67.26.73:/apps/aplicaciones/defensoria/back/
scp Backend/*/target/*-service.jar Modelo-Java/target/antecedentes-service.jar \
    root@156.67.26.73:/apps/aplicaciones/defensoria/back/artifact/
scp Modelo-Java/Dataset/*.json root@156.67.26.73:/apps/aplicaciones/defensoria/back/modelo/Dataset/
```

Si recompilas algún jar, súbelo con el mismo nombre de la tabla.

### 4.2 Orden de arranque

1. Servidor BD arriba (sección 3.3).
2. En el backend:
   ```bash
   cd /apps/aplicaciones/defensoria/back
   bash podman-compose.sh up
   bash podman-compose-modelo.sh up
   podman ps -a          # los 11 deben estar "Up"; si uno sale Exited: podman logs <nombre>
   ```
3. En el servidor BD: `bash podman-compose-bd.sh seed` (sección 3.4).
4. Firewall del backend: 8082–8093 solo desde `169.58.62.111`:
   ```bash
   bash firewall-backend.sh          # aplica (y quita la regla vieja si existe)
   bash firewall-backend.sh boot     # la reaplica en cada reinicio
   ```
   ⚠️ **No uses la regla que venía antes en esta guía** (`iptables ... --dport 8082:8093 ! -s
   169.58.62.111 ! -i lo -j DROP`): también tiraba las llamadas entre microservicios, ver
   sección 7. El script solo filtra lo que entra por la interfaz pública.

## 5. Servidor Frontend — 169.58.62.111

| En el servidor | Subir desde |
|---|---|
| `/apps/aplicaciones/defensoria/front/` (Dockerfile, config/, dist/browser/) | `Frontend/` → `podman-compose-front.sh up` |
| `/apps/aplicaciones/defensoria/front-admin/` | `Frontend-Admin/` → `podman-compose-front-admin.sh` |
| `/apps/aplicaciones/defensoria/front-revision/` | `Frontend-Revision/` → `podman-compose-front-revision.sh` |
| `/apps/aplicaciones/defensoria/router/config/router.conf` | **copiar del VPS viejo 2.25.64.47** y cambiar `2.25.78.22` → `156.67.26.73` |

- El `router.conf` real (con los dos bloques 80/443 y TLS) **solo existe en 2.25.64.47**; el
  `nginx/config/defensoria.conf` del repo es una versión vieja solo-HTTP. No uses el bloque
  `upstream backend` de `ARQUITECTURA-3-SERVIDORES.md`.
- DNS: en No-IP apuntar `defensoria-escom.ddns.net` a `169.58.62.111`; después, Certbot con el
  mismo método webroot que en `CAMBIOS.md` (o copiar `/etc/letsencrypt` del VPS viejo).
- `proxy.conf.json` de los 3 frontends sigue con `2.25.78.22`: solo afecta a `ng serve` en
  desarrollo, no a producción.

## 6. Pendientes conocidos (no bloquean el arranque)

1. **Respaldos del panel admin**: la imagen de admin-service instala `postgresql-client` de
   Ubuntu jammy (versión 14) y el servidor es 16; `pg_dump` 14 se niega a respaldar un
   servidor 16. Probar un respaldo desde el panel. Si falla, instalar `postgresql-client-16`
   desde el repo de PGDG en `admin-service/Dockerfile`.
2. **Restricciones CHECK de CU-Q01**: en la base vieja se agregaron a mano (migración del
   2026-09-08). Hibernate no las crea, así que la base nueva no las tiene; la validación sigue
   en backend y frontend. Si se quieren de vuelta, hay que volver a correr ese script.
3. primer-contacto y subdefensoría en H2 en memoria (ver sección 2).
4. Todos los servicios entran como el superusuario `postgres`, y el tráfico backend→BD va sin TLS por internet. El firewall es la única protección.
5. Cambiar la contraseña root de los 3 VPS y entrar con llaves SSH.

## 7. Incidente 2026-10-07: "error al enviar la queja"

**Síntoma.** El formulario público daba error al enviar. En el log de `quejas-service` llegaba
la petición (`Initializing Spring DispatcherServlet`), pero nunca aparecía la línea
`POST /api/quejoso/quejas/registro-publico -> ...` del `RequestLoggingFilter`, que se escribe
al terminar cada petición: la petición **nunca terminó**, se quedó colgada.

**Causa.** La regla de firewall del backend que traía esta guía
(`--dport 8082:8093 ! -s 169.58.62.111 ! -i lo -j DROP`) no distinguía de dónde venía el tráfico.
Al registrar, `quejas-service` guarda la queja y luego llama a `notificaciones-service` en
`156.67.26.73:8085`. Esa llamada sale de un contenedor (IP 10.88.x.x), la regla la tiraba sin
responder, y `RestTemplate` no tenía timeout → la petición esperaba hasta que nginx cortaba.
**La queja sí se guardaba en la BD** (el guardado ocurre antes de notificar); lo que fallaba era la respuesta.

**Corrección.**
1. `Backend/firewall-backend.sh`: la regla solo aplica a la interfaz pública (`-i eth0` o la
   que tenga la ruta por defecto). Mismo criterio en `podman-compose-bd.sh firewall`.
2. Timeouts (3 s conexión / 10 s respuesta) en `RestTemplate` de quejas, revision y admin
   (requiere recompilar esos 3 jars). Timeouts de Feign en `auth-service.yml` y de SMTP en
   `auth-service.yml` y `notificaciones-service.yml` (solo config, sin recompilar).
3. `revision-service` moría al arrancar (log cortado después de `No active profile set`, sin
   error de Java = el kernel lo mató por memoria). Se subió de `-m 100m / -Xmx70m` a
   `-m 250m / -Xmx150m` en `podman-compose.sh`. Confirmar con
   `podman inspect revision-service --format '{{.State.OOMKilled}}'`.

**Hallazgo colateral.** En `Backend/queja-service/src/` faltaba el paquete `validacion/`
completo (9 clases que `QuejaService` importa): el código fuente **no compilaba**, solo
funcionaba el jar del 18-sep. Se restauró desde `_backups/queja-service-src.tgz` y se le
aplicó el renombrado `apellidoPaterno → apellido1` (etc.). Se verificó contra el jar desplegado:
mismos métodos y mismos mensajes de error.

Al compilar apareció un segundo faltante. Se comparó el respaldo
`_backups/pre-estados-20260918-0236.tar.gz` contra el código actual y se restauraron **solo**
los archivos que ya no existían (sin sobrescribir nada):
- Backend: `queja-service/.../dto/AcuerdoConciliacionModel.java` (coincide con la entidad actual).
- Frontend (el build de Angular también habría fallado): `shared/iconos/iconos.ts`,
  `shared/aviso-privacidad/*`, `shared/autocompletar-dependencia/*`,
  `core/validaciones/reglas-queja.ts`. Se verificó que todo lo que usan las pantallas
  (`Reglas.*`, `ICONOS.*`, inputs/outputs de los componentes) existe en lo restaurado.

Después se revisaron los imports de los 11 servicios Java y los 5 frontends: ninguno apunta a
un archivo inexistente.

**Qué redesplegar tras este incidente:** recompilar y subir solo `quejas-service.jar`,
`revision-service.jar` y `admin-service.jar`; luego `bash podman-compose.sh up` recrea los 10
contenedores con los jars y config-files actuales (los demás jars no cambiaron).

## 8. Estado de los casos de uso del quejoso (revisión de código, 2026-10-07)

Revisión estática: endpoint en backend + pantalla y servicio en el frontend. No sustituye
probarlos en el ambiente desplegado.

| CU | Backend | Frontend | Veredicto | Observaciones |
|---|---|---|---|---|
| QJ-01 Iniciar sesión | `POST /api/auth/login` | `portal/login` | ✅ Programado | Exige cuenta activada. |
| QJ-02 Activar cuenta | `POST /api/auth/activar-cuenta` (valida folio+correo con quejas vía Feign) | `cuenta/activar` | ✅ Programado | El correo debe escribirse igual que en la queja (sensible a mayúsculas). |
| QJ-03 Solicitar código | `POST /api/auth/solicitar-codigo` | `portal/recuperar` | ✅ Programado | Código cifrado, vence en 10 min. El correo se envía de forma síncrona: si el VPS bloquea el puerto 587 falla (ahora con timeout). |
| QJ-04 Restablecer con código | `POST /api/auth/reset-password` | `portal/recuperar` | ✅ Programado | Límite de intentos y aviso por correo. |
| QJ-05 Consultar/editar perfil | `GET /api/auth/me`, `PUT /api/auth/perfil` | `panel/perfil` | ⚠️ Parcial | Solo el teléfono se valida (10 dígitos). **Correo personal sin validar formato**, domicilio sin límite y unidad académica como texto libre: no aplica "las mismas reglas del registro". |
| QJ-06 Menú del chatbot | `GET /api/chatbot/menu` | `chatbot-widget` | ✅ Programado | Requiere correr `seed` (preguntas_chatbot). |
| QJ-07 Registrar queja pública | `POST /registro-publico` + paquete `validacion` | `queja/registro` | ✅ Programado | El error del 07-oct era de infraestructura (sección 7). |
| QJ-08 Mis quejas / detalle | `GET /mias`, `GET /mias/{folio}` (correo del JWT) | `panel/mis-quejas`, `panel/mis-quejas/:folio` | ✅ Programado | La pertenencia se valida con el correo del token. |
| QJ-09 Editar mi queja | `PUT /mias/{folio}`, `POST`/`DELETE /mias/{folio}/evidencias` | `queja-detalle` | ✅ Programado | Solo en RECIBIDA; no deja quitar la última identificación. |
| QJ-10 Evidencias | `GET /mias/{folio}/evidencias` + `/{id}/contenido` | `queja-detalle` | ✅ Programado | La descarga/visualización **ya está implementada**; actualizar la descripción del CU. |
| QJ-11 Consultar por folio sin sesión | `GET /folio/{folio}?correo=` | `queja/consultar` | ✅ Programado | Ahora ignora mayúsculas/espacios en correo y folio (requiere recompilar quejas). Pendiente tuyo: ajustar la pantalla al estilo de QJ-08. |
| QJ-12 Corregir queja | `PUT /mias/{folio}/corregir` (RECHAZADA → CORREGIDA) | `queja-detalle` (botón corregir y reenviar) | ✅ Programado | Depende de que Recepción rechace con observaciones. |
| QJ-13 Mis acuerdos | `GET /api/quejoso/conciliaciones/mias` | `panel/conciliacion` | ⚠️ Parcial | Los acuerdos los crea `revision-service` (`POST` en `ConciliacionRevisionController`), pero **ningún panel del personal tiene pantalla para crearlos**: hoy solo se pueden crear por Swagger/API. |
| QJ-14 Responder acuerdo | `PUT /api/quejoso/conciliaciones/{id}/respuesta` | `panel/conciliacion` | ⚠️ Parcial | Lógica correcta (solo el dueño, solo PENDIENTE, definitiva). El comentario no es obligatorio aunque el CU dice "dejando un comentario". Mismo bloqueo que QJ-13. |
| QJ-15 Cancelar queja | `DELETE /mias/{folio}` → estatus `CANCELADA` | `mis-quejas` / `queja-detalle` (bote) | ⚠️ Parcial | Sí cambia a CANCELADA en BD (no borra). **No captura el motivo** ni la fecha de cancelación que pide el CU. |

Verificar QJ-15 en la BD (servidor BD):

```sql
-- bash podman-compose-bd.sh psql-defensoria
SELECT numero_folio, estatus, fecha_creacion FROM quejas ORDER BY fecha_creacion DESC LIMIT 10;
```

Verificar si las quejas "fallidas" del 07-oct sí se guardaron: la misma consulta.

## 9. Incidente 2026-10-07 (2): quejas-service se cae al recibir una queja

**Síntoma.** Al enviar una queja con 6 archivos (uno de 12.5 MB), el navegador recibe
`502 Bad Gateway` y el log de `quejas-service` se corta justo después de
`Initializing Spring DispatcherServlet`, sin ningún error: el kernel mató el contenedor por
memoria (cgroup OOM), no fue una excepción de Java.

**Causa.** Las evidencias se guardan como BYTEA: el archivo se lee completo a `byte[]` y el
driver de Postgres hace más copias al enviarlo. Con `-m 250m / -Xmx150m` no alcanza.

**Corrección en `podman-compose.sh`.**
- `quejas-service`: `-m 600m`, `-Xmx320m` y `-XX:+ExitOnOutOfMemoryError` (si vuelve a pasar,
  queda un `OutOfMemoryError` en el log en vez de una muerte silenciosa).
- Los 10 servicios ahora llevan `--restart always`: si uno muere, Podman lo levanta solo
  (antes se quedaba caído hasta recrearlo a mano).

**Decisión pendiente.** `quejas-service.yml` acepta hasta 30 MB por archivo y **100 MB por
petición**, y el formulario anuncia lo mismo. Una petición de ~100 MB puede necesitar más de
320 MB de heap. Opciones: bajar `max-request-size` (por ejemplo a 50 MB) y el texto del
formulario, o darle más memoria al contenedor si el VPS la tiene (`free -h`).

Diagnóstico rápido si un contenedor desaparece:
```bash
podman inspect <servicio> --format 'OOMKilled={{.State.OOMKilled}} ExitCode={{.State.ExitCode}}'
dmesg -T | grep -i -E 'oom|killed process' | tail
```
`OOMKilled=true` o `ExitCode=137` = se quedó sin memoria.

**Ajuste final de memoria (confirmado: `ExitCode=137`, `Memory cgroup out of memory`).** Con
250 MB todos los servicios estaban al 87–93 % sin carga. El VPS backend tiene 23 GB, así que:

| Servicio | Límite | Heap |
|---|---|---|
| quejas-service | 1g | `-Xmx640m` (aguanta los 100 MB por petición del yml) |
| auth, notificaciones, catalogo, admin, revision, chatbot | 512m | `-Xmx320m` |
| primer-contacto, subdefensoria | 250m | `-Xmx150m` (sin cambios, no son nuestros) |
| historico-service | 512m | `-Xmx320m`, pero **fuera del `up` general**: `bash podman-compose.sh up-container historico-service` cuando su base esté lista |

Todos los nuestros llevan `-XX:+ExitOnOutOfMemoryError` y `--restart always`. `delete` tampoco
toca historico-service; para quitarlo: `bash podman-compose.sh delete-container historico-service`.

## 10. Recepcionista (2026-10-07)

### 10.1 Lista de verificación en "Validación de datos" (`Frontend-Revision/pages/validacion`)

La pantalla pasó de 2 preguntas a 5. Cada una es Sí / No y, si es "No", pide una observación
para el quejoso. Ya no se buscan antecedentes en este paso (CU-REC-03 ya no aplica): solo se
verifica que los datos existan y coincidan con la identificación.

| # | Pregunta | Qué se le muestra al recepcionista |
|---|---|---|
| 1 | ¿El quejoso proporcionó su nombre completo (nombre y apellidos*)? | Nombre, correo, teléfono. *Hay personas con un solo apellido |
| 2 | Verifique que la identificación sea legible, oficial, vigente y coincida con el nombre. ¿Presentó una identificación oficial válida y vigente de acuerdo al artículo 48? | Botón "Ver listado oficial" (catálogo `identificaciones-oficiales`), combo de la identificación presentada, observaciones siempre visibles. Nota extra si es alumno |
| 3 | ¿El número de boleta / empleado coincide con el de la identificación? | El número capturado, con la etiqueta según sea alumno o empleado |
| 4 | ¿Los hechos ocurrieron dentro de los 90 días previos al registro? | Fecha de los hechos, fecha de registro y los días transcurridos calculados, en verde o rojo |
| 5 | ¿Proporcionó los datos del denunciado (nombre y apellidos*)? | Nombre del denunciado |

- **Canalizar** solo con las 5 en "Sí".
- **Regresar al quejoso** con al menos un "No" y la observación de cada "No". Los motivos que
  viajan al correo se arman solos a partir de las preguntas en "No" (texto predefinido por
  pregunta) + la observación de cada una + las observaciones generales.
- Los 90 días son **naturales**, de la fecha de los hechos a la fecha de registro. La constante
  es `PLAZO_DIAS_QUEJA` en `validacion.ts`.
- Solo cambió el frontend; el backend recibe lo mismo de antes (`motivos` + `observaciones`).
- Respaldo de la versión anterior: `_backups/validacion-pre-checklist-20261007/`.
- Verificado con `ng build --configuration development` (sin errores). El CSS del componente
  bajó de 8.9 a 6.9 kB quitando estilos que ya no se usaban.

### 10.2 🔴 El correo de rechazo nunca se enviaba

`revision-service` (rechazo) y `queja-service` (aviso al tutor de un menor) llaman a
`POST /api/notificaciones/enviar`, pero **ese endpoint no existía** en notificaciones-service,
ni en el código ni en el jar desplegado. Cada llamada fallaba, se registraba en el log y se
ignoraba: el quejoso nunca recibía el correo con las observaciones (CU-REC-05).

Se reconstruyó: `CorreoService` + `EnviarCorreoRequest` + `POST /enviar` en
`notificaciones-service`, con el mismo contrato que ya usan los dos servicios
(`{destinatario, asunto, cuerpo}`) y el SMTP de `spring.mail`. Las direcciones internas
`registro-manual+...@defensoria.ipn.mx` se omiten. **Recompilar `notificaciones-service.jar`.**

### 10.3 🔴 Rutas internas de notificaciones expuestas a internet

`/api/notificaciones/enviar` y `/registrar` son `permitAll` y router-nginx reenvía todo
`/api/notificaciones/`. Cualquiera podía mandar correos desde la cuenta de la Defensoría o
meter avisos falsos a cualquier quejoso. Los microservicios se llaman directo por
`156.67.26.73:8085`, sin pasar por nginx, así que se cierran en nginx. Agregar en
`router.conf` (VPS frontend), dentro del `server` de 443, **antes** de `location /api/notificaciones/`:

```nginx
location = /api/notificaciones/enviar    { return 403; }
location = /api/notificaciones/registrar { return 403; }
```

y `podman restart router-nginx`. El archivo real se llama `/apps/aplicaciones/defensoria/router/config/defensoria.conf` (no `router.conf`). **No subas `nginx/config/defensoria.conf` del repo**: es una versión vieja solo HTTP; subirla tumbó el HTTPS el 2026-10-08 (se restauró desde `defensoria.conf.ssl`).

### 10.4 Estado de los casos de uso del recepcionista

| CU | Backend | Frontend | Veredicto | Observaciones |
|---|---|---|---|---|
| REC-01 Bandeja | `GET /api/revision/bandeja` | `bandeja` | ✅ | Contadores pendientes (RECIBIDA + CORREGIDA), en proceso, turnadas hoy. Lista RECIBIDA/CORREGIDA/EN_VALIDACION. Bloquea la queja mientras alguien la revisa y la libera sola por inactividad |
| REC-02 Detalle | `GET /quejas/{folio}` | `validacion` | ✅ | Abrirla la pasa a EN_VALIDACION |
| REC-03 Antecedentes | — | — | Ya no aplica | El endpoint sigue y se muestra en Turnado (REC-06 lo pide para duplicados) |
| REC-04 Ver evidencia | `GET /quejas/evidencias/{id}` (`inline`) | Abre el archivo en una pestaña nueva | ⚠️ | Se muestra en el navegador, pero el visor de PDF del navegador siempre ofrece "Descargar": técnicamente no se puede impedir del todo |
| REC-05 Rechazar | `POST /quejas/{folio}/rechazar` | `validacion` (lista de verificación) | ✅ con 10.2 | La ruta `rechazo/:folio` (motivos con checkbox) quedó sin ningún enlace que lleve a ella |
| REC-06 Turnar | `POST /quejas/{folio}/turnar` | `turnado` | ⚠️ | El área es fija ("Primer Contacto", decisión previa); se elige defensor y se ven antecedentes. Depende de primer-contacto-service, que guarda en **H2 en memoria**: los expedientes se pierden si ese contenedor se reinicia |
| REC-07 Catálogos | `GET /catalogos/areas`, `/defensores`, `/identificaciones-oficiales` | Combos de turnado / registro manual / validación | ✅ | |
| REC-08 Historial / Excel | `GET /historial`, `/historial/exportar` | `historial` | ✅ | El rango de fechas filtra por fecha de **registro** de la queja, no por la fecha en que se rechazó o turnó |
| REC-09 Registro manual | `POST /registro-manual` | `registro-manual` | ⚠️ | Folio y correo/teléfono sí. Pero no valida formato de nombre, correo ni teléfono; no envía aviso de "queja registrada"; y no captura fecha de los hechos, así que en la validación la pregunta 4 dice "sin fecha, verifíquela en el documento" |

### 10.5 Qué redesplegar

1. `notificaciones-service.jar` (endpoint `/enviar`) → `bash podman-compose.sh up-container notificaciones-service`.
2. `Frontend-Revision`: `ng build --configuration production`, subir `dist/` y `podman-compose-front-revision.sh`.
3. `router.conf` en la VPS frontend con las 2 líneas de 10.3.

## 11. Nuevo microservicio: `denunciado-service` (puerto 8094) — 2026-10-08

La persona señalada en una queja captura sus datos, su versión de los hechos, su credencial y
sus evidencias. Arriba del formulario se muestra el folio de la queja (**por ahora fijo**:
`FOLIO_QUEJA_PRUEBA = 'FOL-DEMO0001'` en `respuesta-denunciado.ts`; después se leerá de la URL).

### 11.1 Backend (`Backend/denunciado-service/`)

- Spring Boot 3.5.16, Java 21, **sin Lombok** (getters/setters explícitos).
- `POST /api/denunciado/respuestas` — **público** (el denunciado no tiene cuenta), multipart:
  `folioQueja, nombre, apellido1, apellido2?, unidadProcedenciaClave, tipoIdentificacion
  (ALUMNO|EMPLEADO), numeroIdentificacion, descripcionHechos, avisoPrivacidadAceptado,
  avisoPrivacidadVersion, credencial[] (1-2), evidencias[] (0-10)`. Devuelve folio `RD-XXXXXXXX`.
- `GET /api/denunciado/respuestas?folioQueja=` y `GET /api/denunciado/respuestas/archivos/{id}`
  — solo personal con JWT (RECEPCIONISTA, ANALISTA_PRIMER_CONTACTO, SUBDEFENSOR, DEFENSOR,
  ADMIN_SISTEMAS). Quedan listos para cuando el panel del personal muestre las respuestas.
- Mismas reglas que el quejoso: nombres (acentos, ñ, guion, apóstrofe; un solo apellido
  permitido), boleta/empleado solo dígitos (máx. 10), descripción 20–4000, credencial solo
  JPG/PNG ≤3 MB **verificada por firma binaria**, evidencias PDF/JPG/PNG/MP4/MP3 ≤30 MB y
  ≤95 MB en total, aviso de privacidad obligatorio. El folio de la queja debe ser `FOL-XXXXXXXX`.
- Verificado: compila con `javac -Xlint:all` contra las librerías de Spring Boot 3.5.16 sin
  advertencias, y un arnés con repositorios simulados pasó **19 de 19 casos** (válidos,
  nombres con dígitos, `.exe` renombrado a `.jpg`, credencial PDF o de 4 MB, 3 credenciales,
  sin aviso, etc.).

### 11.2 Tablas en `defensoria_db` (las crea Hibernate al arrancar el servicio)

| Tabla | Columnas |
|---|---|
| `respuestas_denunciado` | id, folio_respuesta (único), folio_queja (índice), nombre, apellido1, apellido2, unidad_procedencia_clave, tipo_identificacion, numero_identificacion, descripcion_hechos (TEXT), aviso_privacidad_aceptado, aviso_privacidad_version, aviso_privacidad_fecha, estatus, fecha_registro |
| `respuesta_denunciado_archivos` | id, respuesta_id (FK real), tipo (IDENTIFICACION\|EVIDENCIA), nombre_archivo, tipo_mime, tamanio_bytes, contenido (BYTEA), fecha_subida |

`folio_queja` es texto, igual que los demás enlaces entre microservicios (la tabla `quejas`
es de queja-service).

### 11.3 Frontend (portal del quejoso, `Frontend/`)

- Ruta nueva: `https://defensoria-escom.ddns.net/denunciado/respuesta`.
- Archivos: `pages/respuesta-denunciado/*`, `core/services/denunciado.service.ts`,
  `core/models/denunciado.models.ts`; ruta en `app.routes.ts`; `/api/denunciado` en
  `proxy.conf.json` (que además se actualizó de 2.25.78.22 a 156.67.26.73).
- Reutiliza el autocompletado de dependencias y las reglas de `reglas-queja.ts`.
- Aviso de privacidad **propio del denunciado** (versión `D-1.0`), dentro del formulario; el
  del quejoso habla de "tu queja" y no aplica. Igual que aquel, debe revisarlo alguien con
  criterio legal.
- Verificado con `ng build` y con capturas en navegador (formulario vacío, con errores,
  lleno y acuse); el multipart que sale coincide campo por campo con el DTO del backend.

### 11.4 Despliegue

1. **Backend**: `cd Backend/denunciado-service && mvn clean package -DskipTests` → subir
   `target/denunciado-service.jar` a `back/artifact/`, la carpeta
   `config-files/denunciado-service/`, `podman-compose.sh` y `firewall-backend.sh` (ahora cubre
   8082–**8094**). En el backend:
   ```bash
   bash podman-compose.sh up-container denunciado-service
   bash firewall-backend.sh
   podman logs --tail 20 denunciado-service      # "Started DenunciadoServiceApplication"
   ```
2. **Base de datos** (comprobar que Hibernate creó las tablas):
   ```bash
   podman exec defensoria-db psql -U postgres -d defensoria_db -c "\dt respuesta*"
   ```
3. **nginx** (VPS frontend), dentro del `server` de 443, junto a las demás `/api/`:
   ```nginx
   location /api/denunciado/     { proxy_pass http://156.67.26.73:8094; }
   ```
   (ya está en `nginx/config/defensoria.conf`; se puede subir ese archivo completo) →
   probar con `nginx -t` como en 10.3 → `podman restart router-nginx`.
4. **Frontend**: `ng build --configuration production` → subir `dist/.../browser/*` a
   `/apps/aplicaciones/defensoria/front/dist/browser/` → `bash podman-compose-front.sh up`.
5. Prueba: abrir `/denunciado/respuesta`, enviar, y en la BD:
   ```sql
   SELECT folio_respuesta, folio_queja, nombre, apellido1, numero_identificacion, fecha_registro
   FROM respuestas_denunciado ORDER BY id DESC;
   SELECT respuesta_id, tipo, nombre_archivo, tipo_mime, tamanio_bytes
   FROM respuesta_denunciado_archivos ORDER BY id DESC;
   ```

**Pendiente:** el endpoint de registro es público y acepta hasta 100 MB por petición, igual que
el registro público de quejas (sin CAPTCHA ni límite de peticiones). Cuando el folio deje de ser
fijo conviene que llegue en un enlace con un token, para que solo el denunciado real pueda responder.

## 12. Panel de la Defensora + login unificado del personal — 2026-10-08

El antiguo panel de administración (`Frontend-Admin`, `/admin/`) ahora es el **panel de la
Defensora**: el rol `DEFENSOR` puede hacer todo lo que hacía `ADMIN_SISTEMAS` (que se conserva
para la cuenta técnica inicial `admin.sistemas@ipn.mx`).

### 12.1 Login único

Todo el personal entra por **`https://defensoria-escom.ddns.net/revision/login`**:

| Rol | Destino |
|---|---|
| RECEPCIONISTA | `/revision/` (bandeja) |
| DEFENSOR, ADMIN_SISTEMAS | `/admin/` (panel de la Defensora) |
| ANALISTA_PRIMER_CONTACTO / SUBDEFENSOR | `/primer-contacto/` / `/subdefensoria/` (sin desplegar hoy) |

- `/admin/login` ya no es un login: redirige a `/revision/login`.
- `/admin/` sin sesión, o con sesión de otro rol → `/revision/login`.
- La Defensora que abre `/revision/` → va a `/admin/`.
- Cerrar sesión en cualquier panel cierra **ambas** sesiones, y **cada login borra antes
  cualquier sesión previa** (computadora compartida: la recepcionista ya no hereda la sesión
  de la Defensora).
- Lógica compartida en `core/sesion/sesion-personal.ts` (copia idéntica en Frontend-Revision y
  Frontend-Admin — si cambias uno, cambia el otro).
- Verificado en navegador con los dos builds servidos bajo `/revision/` y `/admin/`: 7 de 7
  escenarios (sin sesión, login viejo, login Defensora, Defensora en /revision/, cerrar sesión,
  login recepcionista, recepcionista intentando /admin/).

### 12.2 Backend

- `hasRole('ADMIN_SISTEMAS')` → `hasAnyRole('ADMIN_SISTEMAS','DEFENSOR')` en: admin-service
  (`PersonalController`, `PlantillaController`, `SeguridadController`, `DashboardController`),
  catalogo-service (`DependenciaAdminController`) y chatbot-service (`ChatbotAdminController`).
- **Protección contra quedarse sin administración** (`PersonalAdministrativoService`): no se
  puede desactivar ni quitarse el rol a la propia cuenta, ni a la **última** cuenta activa
  DEFENSOR/ADMIN_SISTEMAS. Probado con 6 escenarios.
- **Respaldos (CU-ADM-08)**: `admin-service/Dockerfile` ahora instala `postgresql-client-16`
  desde el repositorio oficial de PostgreSQL. El de Ubuntu era el 14 y `pg_dump` 14 se niega a
  respaldar el servidor 16. **Además, en el servidor no existe `back/admin-service/Dockerfile`**:
  admin-service se estaba construyendo con el Dockerfile genérico, sin `pg_dump`, así que los
  respaldos no podían funcionar. Hay que subirlo (ver 12.4).

### 12.3 Estado de los casos de uso

| CU | Backend | Frontend (panel Defensora) | Veredicto |
|---|---|---|---|
| ADM-01 Iniciar sesión de personal | `POST /api/admin/auth/login` (+ bitácora) | Login único `/revision/login` | ✅ |
| ADM-02 Cambiar mi contraseña | `PUT /api/admin/perfil/password` (cualquier rol) | Menú de perfil en ambos paneles; obligatorio si la contraseña es temporal | ✅ |
| ADM-03 Gestionar personal | Alta, edición, baja lógica, reactivación | Usuarios y Roles | ✅ + protección 12.2 |
| ADM-04 Resetear contraseña de un tercero | `POST /personal/{id}/resetear-password` (temporal + cambio obligatorio) | Usuarios y Roles | ✅ |
| ADM-05 Dashboard | `GET /dashboard/resumen` + indicadores de quejas (13.1) | Configuración General | ✅ (completado en 13.1) |
| ADM-06 Plantillas | Listar / editar / placeholders | Plantillas Oficiales | ✅ |
| ADM-07 Previsualizar plantilla | `GET /plantillas/{tipo}/previsualizar` | Plantillas Oficiales | ✅ |
| ADM-08 Respaldos | Manual, automático 4:00 AM, listar, descargar, restaurar | Seguridad y Respaldos | ⚠️ → ✅ con 12.2 y 12.4 |
| ADM-09 Bitácora | Login (exitoso y **fallido**), cambios de personal, plantillas, respaldos y restauraciones, con la IP real | Seguridad y Respaldos | ✅ (completado en 13.3) |
| ADM-10 Catálogo de dependencias | Alta, edición, importación Excel | Catálogo de Dependencias | ✅ |
| BOT-01 Preguntas del chatbot | CRUD `/api/chatbot/admin/preguntas` | Preguntas del Chatbot (13.2) | ✅ |
| CAT-01 Consultar catálogo | `GET /api/catalogos/dependencias` (público) | Usado en formularios | ✅ |
| NOT-01 Mis notificaciones | `/mias`, `/no-leidas`, `/{id}/leida` (cualquier JWT) | Portal del quejoso + campana en ambos paneles del personal (13.4) | ✅ |
| NOT-02 Registrar notificación (interno) | `POST /registrar` | — (interno) | ✅ Bloqueado desde internet (10.3) |

### 12.4 Despliegue

1. **Recompilar** `admin-service`, `catalogo-service` y `chatbot-service`
   (`mvn clean package -DskipTests`) y subir los 3 jars a `back/artifact/`.
2. **Subir el Dockerfile de admin-service** (no existe en el servidor):
   ```bash
   ssh root@156.67.26.73 'mkdir -p /apps/aplicaciones/defensoria/back/admin-service'
   scp Backend/admin-service/Dockerfile root@156.67.26.73:/apps/aplicaciones/defensoria/back/admin-service/
   ```
3. En el backend: `bash podman-compose.sh up-container admin-service` (verás `pg_dump (PostgreSQL) 16.x`
   durante el build), y lo mismo para `catalogo-service` y `chatbot-service`.
4. **Frontends**: `ng build --configuration production` en `Frontend-Revision` y `Frontend-Admin`;
   subir cada `dist/.../browser/*` a `front-revision/dist/browser/` y `front-admin/dist/browser/`
   y correr `podman-compose-front-revision.sh up` y `podman-compose-front-admin.sh up`.
5. **Cuenta de la Defensora** (servidor BD), con la contraseña que quieras (generar el hash
   como en la sección del recepcionista):
   ```sql
   UPDATE personal_administrativo SET rol = 'DEFENSOR' WHERE correo_institucional = '<correo>';
   ```
6. Prueba: `/revision/login` con la cuenta DEFENSOR → debe llegar a `/admin/`. En Seguridad y
   Respaldos, "Generar respaldo" debe crear un `.sql` (antes fallaba).

Respaldos de antes de estos cambios: `_backups/defensora-20261008/`.

## 13. Pendientes de la Defensora completados — 2026-10-08

### 13.1 Indicadores de quejas en el dashboard (CU-ADM-05)
- admin-service: `IndicadoresQuejasService` (JdbcTemplate, solo lectura sobre `quejas`,
  `dependencias` y `respuestas_denunciado`) → campo `quejas` en `GET /api/admin/dashboard/resumen`:
  total, por estatus (las quejas sin estatus cuentan como RECIBIDA), últimos 30 días, turnadas
  este mes, respuestas de denunciados, las 5 unidades con más quejas y la tendencia de 6 meses
  (incluye meses en 0). Si la tabla `quejas` no existe, el dashboard sigue funcionando sin esa sección.
- `DashboardResumenModel` pasó a Java sin Lombok; `IndicadoresQuejasModel` es un `record`.
- **Verificado contra PostgreSQL 16 real** con datos de prueba (conteos, unión con
  `dependencias`, meses vacíos, tabla ausente).
- Frontend-Admin: sección "Quejas" con 8 indicadores, gráfica por mes y ranking de unidades.

### 13.2 Pantalla "Preguntas del Chatbot" (CU-BOT-01)
`Frontend-Admin/pages/chatbot`: búsqueda, filtro por categoría y por visibles/ocultas, crear y
editar en modal (categorías existentes como sugerencia), mostrar/ocultar con un clic y eliminar
con confirmación. Menú lateral → "Preguntas del Chatbot".

### 13.3 Bitácora (CU-ADM-09)
- Se registran los **intentos de inicio de sesión fallidos** (correo + motivo).
- La IP ahora es la **real del cliente** (`X-Real-IP` / `X-Forwarded-For`). Antes siempre
  quedaba la del servidor frontend, porque todas las peticiones pasan por nginx.

### 13.4 Notificaciones del personal (CU-NOT-01)
- Campana con conteo de no leídas en los dos paneles (`shared/campana-notificaciones`, copia
  idéntica en Frontend-Admin y Frontend-Revision); se actualiza cada minuto. Si
  notificaciones-service falla, los interceptores **no** cierran la sesión.
- Avisos que ahora se generan:
  - **revision-service** → al turnar, aviso al defensor/subdefensor asignado (se busca su
    correo por nombre en `personal_administrativo`).
  - **queja-service** → cuando el quejoso corrige una queja rechazada, aviso a la
    recepcionista que la rechazó (`validado_por`, mapeado solo lectura y con `@JsonIgnore`).

### 13.5 Verificación
Backend: archivos modificados compilados contra las clases y librerías de los jars desplegados
(admin y revision); indicadores probados en PostgreSQL 16; protección de administración con
6 casos. Frontend: `ng build` de ambos paneles y prueba en navegador (dashboard, campana,
crear/ocultar pregunta del chatbot, 7 escenarios de login unificado).

## 14. QUÉ RECOMPILAR Y QUÉ SUBIR (todo lo pendiente al 2026-10-08)

### 14.1 Backend — recompilar (`mvn clean package -DskipTests` en cada carpeta)

| Carpeta | Jar resultante | Por qué |
|---|---|---|
| `Backend/admin-service` | `admin-service.jar` | Rol DEFENSOR, protección de administración, indicadores de quejas, bitácora |
| `Backend/queja-service` | `quejas-service.jar` | Aviso a la recepcionista al corregir |
| `Backend/revision-service` | `revision-service.jar` | Aviso al defensor al turnar |
| `Backend/notificaciones-service` | `notificaciones-service.jar` | Endpoint `/enviar` (correo de rechazo) |
| `Backend/catalogo-service` | `catalogo-service.jar` | Rol DEFENSOR en administración del catálogo |
| `Backend/chatbot-service` | `chatbot-service.jar` | Rol DEFENSOR en administración del chatbot |

Subir al **backend (156.67.26.73)**:
```bash
cd Produccion/Backend
scp admin-service/target/admin-service.jar queja-service/target/quejas-service.jar \
    revision-service/target/revision-service.jar notificaciones-service/target/notificaciones-service.jar \
    catalogo-service/target/catalogo-service.jar chatbot-service/target/chatbot-service.jar \
    root@156.67.26.73:/apps/aplicaciones/defensoria/back/artifact/
ssh root@156.67.26.73 'mkdir -p /apps/aplicaciones/defensoria/back/admin-service'
scp admin-service/Dockerfile root@156.67.26.73:/apps/aplicaciones/defensoria/back/admin-service/
```
En el backend:
```bash
cd /apps/aplicaciones/defensoria/back
for s in admin-service quejas-service revision-service notificaciones-service catalogo-service chatbot-service; do
  bash podman-compose.sh up-container $s
done
podman ps -a          # todos "Up"
```

### 14.2 Frontends — recompilar (`ng build --configuration production` en cada carpeta)

| Carpeta | Sale en | Se sube a (VPS frontend 169.58.62.111) | Script |
|---|---|---|---|
| `Frontend-Admin` | `dist/defensoria-admin/browser/` | `/apps/aplicaciones/defensoria/front-admin/dist/browser/` | `podman-compose-front-admin.sh up` |
| `Frontend-Revision` | `dist/defensoria-revision/browser/` | `/apps/aplicaciones/defensoria/front-revision/dist/browser/` | `podman-compose-front-revision.sh up` |
| `Frontend` (si no subiste aún la pantalla del denunciado) | `dist/defensoria-front/browser/` | `/apps/aplicaciones/defensoria/front/dist/browser/` | `podman-compose-front.sh up` |

Para cada uno (ejemplo con Admin):
```bash
ssh root@169.58.62.111 'rm -rf /apps/aplicaciones/defensoria/front-admin/dist/browser && mkdir -p /apps/aplicaciones/defensoria/front-admin/dist/browser'
scp -r Frontend-Admin/dist/defensoria-admin/browser/* root@169.58.62.111:/apps/aplicaciones/defensoria/front-admin/dist/browser/
ssh root@169.58.62.111 'cd /apps/aplicaciones/defensoria/front-admin && bash podman-compose-front-admin.sh up'
```

### 14.3 Base de datos
```sql
UPDATE personal_administrativo SET rol = 'DEFENSOR' WHERE correo_institucional = '<correo de la defensora>';
```

### 14.4 Nada más
nginx (`defensoria.conf`), firewall, `podman-compose.sh` y `config-files` ya están aplicados.
Prueba final: `/revision/login` con la cuenta DEFENSOR → `/admin/` con indicadores de quejas,
campana y "Preguntas del Chatbot"; "Generar respaldo" debe crear el `.sql`.
