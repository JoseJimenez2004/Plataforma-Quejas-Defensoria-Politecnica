# Contexto: Validación y corrección del módulo Primer Contacto

> Resumen de una conversación previa, escrito para darle contexto a un chat nuevo.
> Repo: `Plataforma-Quejas-Defensoria-Politecnica`, rama de trabajo `Pre-Produccion2`.
> Fecha del resumen: 2026-09-27.

## 1. Objetivo original

Validar si los casos de uso CU-PC-01 a CU-PC-10 (módulo Primer Contacto) ya están
implementados en el código (`Produccion/Backend/primercontacto` +
`Produccion/Frontend-PrimerContacto`), y luego corregir/implementar lo que falte.

## 2. Hallazgo bloqueante: dos bases de datos distintas

De los 9 microservicios, **7 comparten una sola PostgreSQL**
(`jdbc:postgresql://2.25.78.22:5432/defensoria_db`): auth, queja, revision, admin,
catalogo, notificaciones, chatbot.

**2 corren en H2 en memoria**, sin persistencia real: `primercontacto` y `subdefensoria`.
Ambos yml de producción dicen explícitamente "no tocar el datasource, sigue en H2".

Consecuencia concreta: `personal_administrativo` es un espejo de solo lectura (dueño:
admin-service) que `primercontacto` también replica. En H2 esa tabla se crea **vacía**,
así que `AnalistaAutenticadoService.obtenerAnalista()`
(`Produccion/Backend/primercontacto/.../service/AnalistaAutenticadoService.java`) nunca
encuentra al analista del JWT → **401 en producción** en toda operación de escritura:
crear cita, crear nota, dictaminar, crear remisión. El código de identificación del
analista está bien escrito; el problema es que apunta a una base vacía.

También implica que expedientes, notas, citas, dictámenes y remisiones de Primer Contacto
**se pierden al reiniciar** el contenedor.

## 3. Decisiones ya tomadas por el usuario

1. **Migrar `primercontacto` a la PostgreSQL compartida.** ✅ Decidido.
2. **Evidencias reales (CU-06/CU-07):** proxy a revision-service — nuevo endpoint
   `GET /api/primer-contacto/evidencias/{id}` que reenvía a
   `GET /api/revision/evidencias/{id}` (usando `evidenciaOrigenId`, ya guardado en
   `EvidenciaPrimerContacto`). No se duplican archivos.
3. **CU-PC-09 (remisión):** por ahora solo agregar un estatus real a `RemisionExterna`
   (hoy no tiene ninguno) + notificar al quejoso. Sin generación de PDF ni envío externo
   real todavía.
4. **CU-PC-10 (conciliación):** reusar la tabla compartida `acuerdos_conciliacion`
   (no crear tabla propia en Primer Contacto).

## 4. Cambio de código ya aplicado (sin commitear)

Único archivo tocado hasta ahora:
`Produccion/Backend/primercontacto/src/main/resources/application.properties`
— cambiado de H2 a la PostgreSQL compartida, agregado `notificaciones.service.url` y
`revision.base-url`. **Es un cambio local de desarrollo**; el yml de producción
(`Produccion/Backend/config-files/primer-contacto-service/config/primer-contacto-service.yml`)
**no se tocó a propósito**, pendiente de que el usuario confirme el despliegue (Hibernate
con `ddl-auto=update` crearía tablas nuevas en la base de producción).

El driver de PostgreSQL ya estaba en el `pom.xml` de `primercontacto` (scope `runtime`),
no hizo falta agregar dependencias.

### Actualización 2026-09-27 — config del servidor (mensaje del compañero)

> "No incluyan ni reemplacen los archivos de configuración de producción del servidor
> (config-files). La configuración de PostgreSQL y JWT ya está establecida directamente en
> el servidor y debe conservarse durante el despliegue."

- En el **servidor** `primer-contacto-service` ya corre sobre PostgreSQL `defensoria_db`
  (confirmado también en `docs/ACCESO-BASES-DATOS.md`, que lo lista entre los usuarios de
  esa base). El `primer-contacto-service.yml` del repo (que dice H2) está **desfasado** y
  **no debe desplegarse ni editarse** para "arreglarlo": al subir, se excluye `config-files/`.
- La migración a PostgreSQL de la Fase 1 ya está hecha del lado del servidor; en el repo solo
  queda el `application.properties` de desarrollo, ahora con `${DB_PASSWORD}` por variable
  de entorno (antes tenía la contraseña en texto plano).

## 5. Tabla de CU-PC — observaciones corregidas/ampliadas

Nota transversal (aplica a CU-04, 05, 06, 07, 09): la causa raíz de "no sabe quién es el
analista" es el problema de la sección 2 (H2 vs PostgreSQL), no falta de lógica.

| CU | Observación corregida/ampliada |
|---|---|
| CU-PC-01 | Prioridad siempre llega null desde Revisión. El endpoint de ingesta es público, sin autenticación entre servicios. |
| CU-PC-02 | El backend ya tiene filtros por folio/prioridad/estatus, pero el frontend no los usa: filtra todo del lado del cliente. Catálogos de tema/escuela hardcodeados. |
| CU-PC-03 | Al abrir el expediente no cambia su estado a "En análisis" (Revisión sí hace esto al abrir una queja); falta replicar ese mismo patrón. |
| CU-PC-04 | Confirmar cita existe en el backend pero no hay botón en pantalla para usarlo. Reagendar no es un endpoint real: se simula cancelando y creando de nuevo. |
| CU-PC-05 | El frontend no muestra opción de editar/eliminar nota; al cargar el expediente se pierde el autor y la fecha de cada nota. |
| CU-PC-06 | No existe un estado "Procedente" intermedio: el expediente salta directo del análisis al turnado a Subdefensoría, sin quedar registrado el momento del dictamen. |
| CU-PC-07 | Mismo problema que CU-06: no queda registrado un estado propio del dictamen antes de cerrar el flujo. |
| CU-PC-08 | El backend ya tiene todo listo (consulta por folio y por id); solo falta la pantalla. Hoy, si ya existe un dictamen, el sistema no avisa y solo da error al intentar registrar otro. |
| CU-PC-09 | La remisión no tiene su propio estatus: solo cambia el estatus del expediente, y el frontend muestra igual "creada" que "enviada". |
| CU-PC-10 | Corregir a "No implementado": no hay pantalla ni backend de conciliación en Primer Contacto. Existe en otro módulo (revision-service), pero ese permiso no incluye al analista de Primer Contacto (`@PreAuthorize` solo permite RECEPCIONISTA/SUBDEFENSOR/DEFENSOR/ADMIN_SISTEMAS). |

**Pendiente de aclarar:** CU-PC-10 dice que Primer Contacto propone conciliación, pero el
diagrama de estados corregido (sección 6) ubica la conciliación en Subdefensoría. Falta
que el usuario confirme si CU-PC-10 está mal ubicado en el catálogo original de CU, o si
Primer Contacto también puede proponer conciliación en paralelo a Subdefensoría.

## 6. Diagrama de estados de la queja

Se contrastó el diagrama de estados (PlantUML, provisto por el usuario) contra el código
real.

**Hallazgo estructural:** `quejas.estatus` (PostgreSQL, dueño revision-service) es la
única fuente de estado que ve el quejoso. Después de `TURNADA`, **nadie vuelve a tocar
ese campo** — `PrimerContactoClientService` es unidireccional y revision-service solo
expone `/rechazar` y `/turnar`. El quejoso nunca ve avance real después de ser turnado.

**Estados que el backend de Primer Contacto realmente asigna hoy** (ninguno coincide 1:1
con el diagrama): `PENDIENTE_ANALISIS`, `TURNADO_SUBDEFENSORIA`, `IMPROCEDENTE`,
`REMITIDA`, `REMISION_ENVIADA`.

Problemas detectados:
- `EN_ANALISIS` y `COMPETENTE`: el frontend los traduce pero el backend **nunca los
  asigna** — ramas muertas.
- `CON_CITA`: no es un estado real, es un cálculo hecho en `BandejaAnalisisService`, y el
  dashboard lo recalcula con una regla **distinta** — dos definiciones de lo mismo. Debe
  eliminarse como estado y convertirse en un indicador aparte (no bloquea transiciones,
  igual que citas y notas).
- `Improcedente` es tratado como terminal en el código, pero el usuario confirmó: **toda
  queja improcedente siempre termina en Remitida** (no es terminal por sí sola).

**Decisión tomada:** `quejas.estatus` como fuente única de verdad (implica que Primer
Contacto, ya migrado a PostgreSQL, escriba las transiciones directo ahí).

### Conciliación vs. Conclusión — aclarado en esta conversación

El usuario había confundido ambos conceptos al dibujar el diagrama. En el código son dos
cosas distintas y ya existen por separado:

| | Conciliación | Conclusión |
|---|---|---|
| Tabla | `acuerdos_conciliacion` (PostgreSQL) | `acuerdos_conclusion` (H2, subdefensoria) |
| Mecánica | **Bilateral**: `estado` PENDIENTE→ACEPTADO/RECHAZADO, con `comentario_quejoso` y `fecha_respuesta` | **Unilateral**: el abogado redacta y marca `concluido=true` directo, sin esperar respuesta. Campos `rutaPdfGenerado`, `fechaEnvioSecretarial` |
| Quién emite hoy | revision-service (`RECEPCIONISTA`/`SUBDEFENSOR`/`DEFENSOR`/`ADMIN_SISTEMAS`) | subdefensoria-service |

El loop que el usuario ya tenía dibujado (`Elaboro_Acuerdo → Pendiente_conclusión →
quejoso rechaza/acepta`) en realidad describe bien la mecánica de **conciliación**
(confirmado por el usuario: "lo del diagrama está bien con respecto al de conciliación").
Lo que faltaba era un estado nuevo de **conclusión**, que va *después* de que el quejoso
acepta el acuerdo de conciliación, un paso antes del estado terminal `Concluido`.

**Cambio acordado en el diagrama** (PlantUML):
```plantuml
Pendiente_conclusion --> En_investigacion : Quejoso rechaza
Pendiente_conclusion --> Elaborando_Conclusion : Quejoso acepta el acuerdo de conciliación

state Elaborando_Conclusion as "Elaborando\nacuerdo de conclusión" #EAE7F3;line:5B4A8F
Elaborando_Conclusion --> Concluido : Se redacta y envía a archivo secretarial
```

**Cambio acordado en la tabla ECA** (modifica fila 19 existente, agrega fila 20):

| # | De... | ...a | Evento | Condición | Acción | Quien | CU |
|---|---|---|---|---|---|---|---|
| 19 (modificada) | Pendiente_conclusion | Elaborando_Conclusion | Quejoso acepta el acuerdo de conciliación | Que acepte | Acuerdo de conciliación queda ACEPTADO; se procede a redactar el acuerdo de conclusión | Quejoso | CU-QJ-14 |
| 20 (nueva) | Elaborando_Conclusion | Concluido | Se envía el acuerdo de conclusión a archivo secretarial | Que esté redactado | El expediente se marca concluido definitivamente | Subdefensor | CU-SD-09 |

**Estado del diagrama al cierre de esta conversación:** el usuario estaba redibujando el
diagrama completo con esta corrección. No se ha confirmado la versión final.

## 7. Incidente de ramas borradas (resuelto, contexto histórico)

Durante esta conversación se detectó que 6 ramas remotas habían sido borradas
(probablemente por un merge de un compañero con auto-delete activado): `DEV`,
`DEV-MICRO-PRIMER-CONTACTO`, `DEV-MICRO-SUBDEFENSORIA`, `FEATURE-BASTIAN`,
`FEATURE-BRYAN`, `FEATURE-JAIR`. Solo quedaban en remoto: `main`, `Produccion`,
`Pre-Produccion`.

El equipo decidió **no restaurarlas** (las 3 ramas vivas ya tenían lo más actualizado).
Se generó un respaldo por si acaso: `git bundle` con las 6 ramas completas, comprimido en
zip, compartido con el equipo. Ubicación local:
`C:\Users\leish\respaldo-ramas-git\ramas-borradas-20260922.bundle` (y `.zip`).
**Este tema ya está cerrado**, se incluye solo para contexto histórico de la conversación.

## 8. Plan de implementación (acordado, no ejecutado)

**Fase 1 — Fundación**
- Migrar `primercontacto` a PostgreSQL `defensoria_db` (config local ✅ hecha, producción
  pendiente).
- Espejo de la entidad `Queja` en `primercontacto`, escribiendo directo sobre
  `quejas.estatus` con el catálogo de estados del diagrama corregido.
- Cliente de notificaciones en `primercontacto` (copiar patrón de
  `NotificacionQuejaService` en revision-service: `POST /api/notificaciones/registrar` +
  `/enviar`, nunca debe tumbar la operación si falla).

**Fase 2 — Máquina de estados** (CU-01, 03, 06, 07, 09)
- Eliminar `PENDIENTE_ANALISIS` y `CON_CITA` del código; la queja llega en `TURNADA`.
- `Turnada → En_Análisis` al abrir el detalle (copiar patrón de
  `RevisionQuejaService.detalle()`).
- `En_Análisis → Procedente → Recibida (Subdefensoría)` como dos pasos, no uno.
- `En_Análisis → Improcedente → Remitida` (improcedente deja de ser terminal).
- Colapsar `REMITIDA`/`REMISION_ENVIADA` a un único `Remitida` con estatus propio en
  `RemisionExterna`.

**Fase 3 — Huecos por CU** (ver tabla sección 5 para detalle de cada uno)
- CU-04: `confirmarCita` en el front + notificación al quejoso.
- CU-05: validación de autoría en editar/borrar nota + exponer notas completas en el
  front (hoy se aplanan a string, se pierde autor/fecha).
- CU-06/07: proxy de evidencias a revision-service + enlace real en la UI.
- CU-08: pantalla de consulta de dictamen (backend ya listo).
- CU-09: estatus real de remisión + notificación al quejoso.
- CU-10: conciliación en Primer Contacto sobre `acuerdos_conciliacion` — **pendiente
  resolver si de verdad pertenece a Primer Contacto** (ver sección 5, "Pendiente de
  aclarar").
- CU-02: conectar los filtros de servidor que hoy están muertos en el frontend.

**Fase 4 — Despliegue**
- No existe ruta `/api/primer-contacto/` en
  `Produccion/nginx/config/defensoria.conf`, ni contenedor de frontend para Primer
  Contacto. El módulo no está publicado todavía.

## 9. Próximo paso

Fases 1–3 implementadas (ver 9b) y pantalla de antecedentes hecha (ver 9c). Pendiente:
conectar el modelo de búsqueda cuando exista, y el despliegue (Fase 4).

## 9c. Búsqueda de antecedentes (2026-09-27)

- Detalle del expediente: al abrirlo se hace una búsqueda automática y un aviso muestra
  "N posibles antecedentes"; el botón **Buscar antecedentes** lleva a
  `expediente/:id/antecedentes` (resultados con % de similitud, motivos de coincidencia,
  filtros, "Marcar como antecedente" que deja una nota en el expediente).
- Backend: `GET /api/primer-contacto/antecedentes/{folio}`. El parecido lo calcula la interfaz
  `service/antecedentes/MotorAntecedentes`. Hoy la implementa `MotorAntecedentesReglas`
  (PROVISIONAL: mismo quejoso 45 pts, misma unidad 15, palabras en común hasta 40; umbral 25).
- **Para conectar el modelo:** crear otra implementación de `MotorAntecedentes` (por ejemplo,
  una que llame por HTTP al servicio del modelo) y marcarla `@Primary`. El endpoint, el DTO
  y la pantalla no cambian; la pantalla deja de mostrar el aviso "Motor provisional" cuando
  `motor` ya no es `REGLAS_PROVISIONAL`.
- Candidatas: todas las filas de `quejas` salvo la propia (no incluye `defensoria_historico_db`).

## 9b. Implementación de CU-PC-01..10 (2026-09-27) — HECHO, sin commitear

**Decisiones del usuario en esta ronda:** diagrama de estados final (Turnada → En_Análisis →
Procedente | Improcedente → Remitida); no tocar el panel del quejoso por ahora; oficio de
remisión inventado, solo PDF descargable + registrar envío; CU-PC-10 sí es de Primer Contacto
(si el quejoso acepta, pasa a Subdefensoría); notificar también por correo; probar con una
Postgres LOCAL.

**Antes de empezar:** el commit `848135c` (compañero) traía marcadores de conflicto sin resolver
en 6 archivos (`CitaPrimerContactoService`, `SubdefensoriaClientService`, 3 DTOs de primercontacto
y `subdefensoria/config/CorsConfig`). Se resolvieron quedándose con el lado de `Produccion`.

| CU | Qué se hizo |
|---|---|
| 01 | El expediente nace `TURNADA` (catálogo en `entity/EstatusExpediente`). |
| 02 | `POST /bandeja/filtrar` acepta listas (prioridades, estatusLista, unidadesAcademicas, temas, texto, orden); el front filtra en el servidor y arma las facetas con datos reales. `CON_CITA` eliminado → indicador `tieneCitaActiva` (misma regla en bandeja y dashboard). |
| 03 | `POST /expedientes/folio/{folio}/iniciar-analisis`: TURNADA → EN_ANALISIS, guarda analista y fecha. Solo lo llama la pantalla de detalle. |
| 04 | Confirmar/reagendar/cancelar con analista del JWT (`actualizadoPor*`); `PUT /citas/{id}/reagendar` real; aviso al quejoso (panel + correo) en cada movimiento. Botón Confirmar en el diálogo. |
| 05 | Editar/borrar nota solo el autor (403 si no). Front muestra autor/fecha y acciones solo en notas propias (`GET /analistas/yo`). |
| 06/07 | Evidencias: **no se hizo proxy** a revision-service (su descarga exige rol RECEPCIONISTA → 403); se lee `queja_evidencias` con espejo de solo lectura `QuejaEvidenciaArchivo`, `GET /evidencias/{id}`. Dictamen solo en expediente abierto, sin dictamen previo y sin conciliación pendiente (409 con mensaje). Procedente: el dictamen y el estado se guardan aunque Subdefensoría falle; `POST /dictamenes/folio/{folio}/reenviar-subdefensoria`. |
| 08 | Pantalla nueva `dictamen/:id/consulta`; la pantalla de dictamen detecta si ya existe y redirige. 404 real (antes 500). |
| 09 | `RemisionExterna.estatus` GENERADA → ENVIADA, `numeroOficio` (DDP/PC/REM/año/id), `fechaEnvio`. PDF con OpenPDF (`OficioRemisionPdfService`), `GET /remisiones/folio/{folio}/pdf`. Enviar → expediente REMITIDA + aviso al quejoso. `PENDIENTE_REMISION` deja de usarse. |
| 10 | Sobre `acuerdos_conciliacion` (espejo `AcuerdoConciliacion`): `POST /conciliaciones`, `GET /conciliaciones/expediente/{folio}`. Aceptación detectada cada 60 s (`@Scheduled`) y al consultar → dictamen automático COMPETENTE + envío a Subdefensoría. |

**Sincronización con `quejas.estatus`:** implementada en `TransicionExpedienteService` (espejo
`QuejaReferencia` con `@DynamicUpdate`, solo toca `estatus`) pero **apagada**
(`primer-contacto.sincronizar-estatus-queja=false`). Motivo: el panel del quejoso mostraría
EN_ANALISIS como "Recibida" y el Historial de Recepción (revision-service) solo lista
RECHAZADA/TURNADA. Probada encendida en local: funciona.

**Pruebas:** base local `Backend/primercontacto/dev-local/` (docker compose, puerto 5433,
esquema del dump de agosto + datos de prueba). Prueba E2E de 48 comprobaciones contra la API
real, todas OK; capturas del front revisadas.

### Para desplegar en el servidor (compañero)
- **No reemplazar `config-files/`.** Agregar a mano en el yml del servidor de primer-contacto:
  `notificaciones.service.url: http://2.25.78.22:8085` (sin esto usa localhost:8085 y los avisos
  fallan en silencio; la operación no se rompe).
- Correr `docs/migracion-estados-primer-contacto-2026-09-27.sql` (idempotente; probado con datos
  viejos).
- Pendientes fuera de este módulo: etiquetas nuevas en el panel del quejoso y en el Historial de
  Recepción → luego encender la bandera; ruta nginx y contenedor del front de Primer Contacto
  (Fase 4); CU-PC-01 prioridad sigue llegando null (Revisión no la captura).

## 10. Archivos clave (para no re-explorar desde cero)

**Backend Primer Contacto** (`Produccion/Backend/primercontacto/src/main/java/ipn/escom/defensoria/primercontacto/`):
- `service/AnalistaAutenticadoService.java` — resuelve el analista desde el JWT (correcto,
  bloqueado por el problema de la sección 2).
- `service/BandejaAnalisisService.java` — calcula el pseudo-estado `CON_CITA`.
- `service/DictamenPrimerContactoService.java` — dictámenes de competencia/improcedencia.
- `service/RemisionExternaService.java` — remisión externa (sin campo de estatus propio).
- `service/CitaPrimerContactoService.java`, `service/NotaAnalisisService.java`,
  `service/IngresoPrimerContactoService.java`, `service/ExpedienteAnalisisService.java`.
- `config/SecurityConfig.java` — `/ingesta/**` público, resto requiere rol
  `ANALISTA_PRIMER_CONTACTO`.
- `entity/PersonalAdministrativo.java` — espejo de solo lectura de `personal_administrativo`.

**Frontend Primer Contacto** (`Produccion/Frontend-PrimerContacto/src/app/`):
- `core/services/bandeja.service.ts`, `expediente.service.ts`, `agenda.service.ts`,
  `dictamen.service.ts`, `remision.service.ts`, `nota-analisis.service.ts`.
- `features/expediente/expediente.ts`, `features/dictamen/dictamen.ts`,
  `features/agenda/agenda.ts`, `features/remision/remision.ts`.
- `app.routes.ts` — no existe ruta para consultar dictamen (CU-PC-08).

**Revision-service** (referencia de patrones a copiar):
- `service/RevisionQuejaService.java` — máquina de estados de `quejas.estatus`
  (`RECIBIDA`→`EN_VALIDACION`→`TURNADA`/`RECHAZADA`), patrón de transición al abrir
  detalle.
- `service/NotificacionQuejaService.java` — patrón de cliente de notificaciones a copiar.
- `service/ConciliacionRevisionService.java` + `controller/ConciliacionRevisionController.java`
  — emisión de conciliación (permiso no incluye Primer Contacto).
- `service/PrimerContactoClientService.java` — cliente que envía el expediente a Primer
  Contacto al turnar.

**Subdefensoria-service:**
- `entity/AcuerdoConclusion.java` + `controller/AcuerdoConclusionController.java` —
  acuerdo de conclusión (unilateral).

**Config:**
- `Produccion/Backend/config-files/primer-contacto-service/config/primer-contacto-service.yml`
  — **desactualizado en el repo** (sigue diciendo H2). El real, en el servidor, ya estaba en
  PostgreSQL antes de esta ronda y se le agregó a mano `notificaciones.service.url` el
  2026-09-27 (ver sección 11). El repo nunca tuvo una copia fiel de este archivo; si hace
  falta volver a verlo, hay que leerlo del servidor, no de git.
- `Produccion/Backend/config-files/revision-service/config/revision-service.yml` — ejemplo
  de config ya en PostgreSQL, con `primer-contacto.base-url` apuntando a
  `http://2.25.78.22:8082`.
- `Produccion/nginx/config/defensoria.conf` — **desactualizado en el repo**: no tiene HTTPS/
  certbot ni las rutas `/primer-contacto/`, `/revision/`, `/admin/`, `/subdefensoria/`. El real
  (`/apps/aplicaciones/defensoria/router/config/router.conf` en 2.25.64.47) sí las tiene todas
  desde antes de esta ronda. Mismo problema que el yml de arriba: no confiar en este archivo
  del repo, leer del servidor.

## 11. Desplegado en producción (2026-09-27/28)

Las Fases 1–3 (sección 9b) y la búsqueda de antecedentes (9c) ya están en vivo, no solo
committeadas. Rama `Pre-Produccion2` empujada a GitHub (3 commits: `c45eb30`, `9b67735`,
`1c199fc`), todavía no mergeada a `Pre-Produccion` ni a `main`.

**Cómo se desplegó** (el usuario no tenía experiencia previa con esto; se hizo como lección
guiada, paso a paso, con el usuario ejecutando los comandos en su propia PowerShell):
1. Respaldo antes de tocar nada: `pg_dump` completo de `defensoria_db`, `podman save` de la
   imagen anterior de `primer-contacto-service` y copia del `.jar` viejo — los tres en
   `/apps/utiles/respaldos/` en `2.25.78.22`. Mismo patrón para el frontend (imagen + `dist/`
   viejos) en `/apps/utiles/respaldos/` de `2.25.64.47`.
2. Backend: `mvn clean package` → subir `primer-contacto-service.jar` a
   `/apps/aplicaciones/defensoria/back/artifact/` → correr
   `docs/migracion-estados-primer-contacto-2026-09-27.sql` contra `defensoria_db` → agregar a
   mano `notificaciones.service.url: http://2.25.78.22:8085` al yml de producción (sin tocar
   el resto) → `bash podman-compose.sh up-container primer-contacto-service`.
3. **Bug encontrado en el primer arranque en producción** (no aparecía en local): Hibernate
   intentaba `ALTER TABLE quejas ALTER COLUMN descripcion TYPE varchar(255)` en cada arranque
   y Postgres lo rechazaba (`value too long`) porque `QuejaReferencia.descripcion` no tenía
   `columnDefinition = "TEXT"`. Inofensivo (Postgres nunca aplicó el ALTER, no se perdió nada),
   pero ensuciaba el log. Corregido y redesplegado; commit `1c199fc`.
4. Frontend: **ya existía un contenedor `primer-contacto-web` corriendo** en `2.25.64.47`
   (puerto 22348) desde el 18 de septiembre — el compañero lo había armado directo en el
   servidor, sin subir `Dockerfile`/`nginx.conf`/`podman-compose-front-primer-contacto.sh` al
   repo. Se compiló la versión de hoy (`ng build --configuration production`; hubo que subir
   el budget de `angular.json` de 1MB a 1.5MB, ya no cabía con las pantallas nuevas), se
   reemplazó el `dist/browser/` viejo y se corrió el script que ya estaba en el servidor. La
   ruta pública (`/primer-contacto/` y `/api/primer-contacto/`) **ya estaba en el router-nginx
   real desde antes** (ver nota de la sección "Config" arriba) — no hubo que tocar nginx.
5. Los tres archivos de despliegue del frontend que solo vivían en el servidor se agregaron al
   repo en el commit `1c199fc` (mismo patrón que `Frontend-Revision`/`Frontend-Admin`).

**Verificado en producción:** logs limpios, `Started PrimercontactoApplication`, la página
pública carga el build de hoy, y un expediente real preexistente (`PC-8AF293B4`, del
15-sep, antes de esta ronda) confirma que la llamada real Primer-Contacto→Subdefensoría
funciona en producción (folio `SD-FEDCDCFC` recibido). A esa fecha, producción solo tenía ese
expediente (sin citas/notas/remisiones) — las pantallas nuevas de hoy (CU-04, 05, 09, 10,
antecedentes) siguen sin evidencia en vivo, solo local (48/48 en el suite de pruebas).

**Pendiente para la próxima sesión:** el usuario quiere hacer una prueba completa en vivo
(crear una queja de prueba claramente marcada, recorrer todo el flujo, borrarla al final) —
quedó pactado para "mañana", no ejecutado todavía. Ver [[feedback-defensoria-deploy]] y
[[reference-defensoria-servers]] en la memoria de Claude para el procedimiento y los datos
de acceso.

### Hallazgos nuevos, no relacionados con Primer Contacto pero relevantes para el proyecto

- **`buscador-antecedentes-service`**: microservicio FastAPI ya construido por el compañero en
  `2.25.78.22:/apps/aplicaciones/defensoria/ia/buscador-antecedentes-service/` (imagen creada,
  container detenido tras una prueba exitosa). Es el modelo real para CU de antecedentes:
  embeddings (`paraphrase-multilingual-MiniLM-L12-v2`) + TF-IDF sobre un índice de 4000 casos
  **históricos reales** (`casos.json`, exportado de `defensoria_historico_db`). Contrato
  documentado en su propio `README.md`/`app/schemas.py`
  (`POST /buscar-antecedentes`, `POST /buscar-por-persona`, `POST /recargar-indice`). No
  incluye quejas nuevas del sistema en vivo todavía (solo históricas). El
  `MotorAntecedentes` que se dejó en `primercontacto` (sección 9c) está diseñado justo para
  enchufar esto sin tocar el resto del código, pero **el usuario pidió NO conectarlo todavía**
  hasta que el compañero confirme que está listo.
- **`resumen-service`**: otro microservicio de IA del compañero (resumen extractivo de la
  narrativa de una queja, con un modelo entrenado propio — `model.safetensors`). Fuera del
  alcance de Primer Contacto, solo anotado para que el equipo lo tenga presente.
- Ninguno de los dos vive en el repositorio de git — solo en `2.25.78.22`.

## 12. Prueba en vivo en producción (2026-09-29/30) — todo validado

Se probaron los 10 CU-PC más antecedentes directamente en producción (no solo local), con
una cuenta de analista de prueba y 3 quejas de prueba, todas claramente marcadas
`[PRUEBA - borrar]`. **Todos los casos de uso funcionaron correctamente**, incluyendo el
turno automático a Subdefensoría por conciliación aceptada.

**Se dejaron listos a propósito para que el usuario los reuse al presentar**, reseteados a su
estado original (`TURNADA`, sin citas/notas/dictamen/remisión/conciliación):
- Cuenta: `prueba.demo@ipn.mx` / `PruebaPC2026!` (rol `ANALISTA_PRIMER_CONTACTO`, id 12).
- Quejas: `FOL-DEMO-A`, `FOL-DEMO-B`, `FOL-DEMO-C` (pensadas para demostrar, respectivamente:
  competente→Subdefensoría, improcedente→remisión, y conciliación→turno automático; A y C
  comparten quejoso para que el buscador de antecedentes encuentre relación entre ellas).

**Incidente encontrado, no relacionado con Primer Contacto, sin resolver:** `revision-service`
y `historico-service` llevan caídos desde el 2026-09-29 ~03:30 UTC. Se cayeron juntos durante
un reinicio de todos los servicios (probablemente del compañero) — el VPS backend solo tiene
3.8GB de RAM entre 11+ contenedores, y arrancar muchos JVMs a la vez agotó la memoria por un
instante; el sistema mató a los dos que más pedían en ese momento (no fue el límite propio de
cada contenedor: `OOMKilled` sale `false` en ambos). No bloquea nada de Primer Contacto ni del
portal público del quejoso — solo el panel de recepcionistas. Se le dieron al usuario los
comandos para reintentar (`podman start revision-service` / `historico-service`, uno a la vez
para no repetir el problema), pendiente de confirmar si ya se corrieron.

**Hallazgo menor, pendiente de investigar:** el envío de **correo** al quejoso falla con 403
en notificaciones-service (el aviso dentro de su panel sí funciona bien). No es urgente.

**Sobre cómo se hicieron estas pruebas — límites del entorno del asistente:** el harness de
Claude Code bloquea, para el asistente, prácticamente cualquier escritura en el servidor de
producción (reiniciar un contenedor, generar un token de acceso, crear datos vía la API),
aunque el usuario ya haya autorizado la acción. El patrón que funcionó: el asistente
diagnostica y prepara el comando exacto, listo para copiar y pegar; el usuario lo corre él
mismo por SSH; el asistente verifica después con lecturas. Para "entrar como analista" sin
tropezar con esos bloqueos, se usó el login real de la app (usuario/contraseña de una cuenta
de prueba) en vez de fabricar un token.

## 13. Migración a 3 servidores y observaciones de Primer Contacto (2026-10-08)

**Servidores nuevos** (el compañero migró el 2026-10-04; los viejos `2.25.78.22`/`2.25.64.47`
quedan obsoletos): BD `169.58.62.99` (`defensoria-db` 5432, `historico-db` 5433), backend
`156.67.26.73`, frontend `169.58.62.111`. La base se limpió a propósito: ya no existen la cuenta
`prueba.demo@ipn.mx` ni las quejas `FOL-DEMO-*`. **Primer Contacto, Subdefensoría e Histórico
todavía no están desplegados en los servidores nuevos** (hay imagen, no contenedor; el jar y el yml
de primer-contacto que hay allá son los viejos, en H2). Tampoco hay front ni ruta nginx de Primer
Contacto. `origin/Produccion` se mergeó a `Pre-Produccion2` sin conflictos (`28528f6`).

**Observaciones implementadas** (probadas en local, 39/39 en la prueba E2E de la API + capturas):

1. **Antecedentes manual + modelo.** Pestaña *Búsqueda manual* (por quejoso y/o denunciado, en
   `quejas` y en `historico_db`, sin importar acentos) y pestaña *Con el modelo*
   (`MotorAntecedentesModelo` → `antecedentes-service` del compañero, `POST /api/antecedentes/buscar`;
   si no responde, cae al motor por reglas y lo avisa). La selección se mantiene entre pestañas y
   se guarda como antecedentes finales en la tabla nueva `antecedentes_primer_contacto`
   (`GET/POST /antecedentes/{folio}/guardados`, `DELETE .../guardados/{id}`). Sustituye al viejo
   "Marcar como antecedente" que dejaba una nota. El histórico se lee directo y en solo lectura
   (`HistoricoAntecedentesRepository`) porque la API interna de historico-service solo busca al
   quejoso por nombre exacto. Ojo: el modelo hoy solo busca en su dataset de prueba (50 quejas
   sintéticas), con similitudes bajas (≤ 0.13), por eso el umbral por defecto es 0.05.
2. **Resumen simulado** (solo front): botón *Resumen* → resumen propuesto + frases de impacto,
   calculados en el navegador (`core/utils/resumen-simulado.ts`) con la etiqueta "Simulado".
3. **Quejas por analista:** NO se hizo. El compañero pidió dejar que Primer Contacto vea todas por
   ahora (tema de roles pendiente). Roles: ADMIN_SISTEMAS, RECEPCIONISTA, ANALISTA_PRIMER_CONTACTO,
   SUBDEFENSOR, DEFENSOR (sin pantalla) + quejoso sin rol. El combo "Defensor / Abogado
   responsable" del turnado lista DEFENSOR/SUBDEFENSOR y ese dato no llega a Primer Contacto.
4. **Citas con 48 h:** al agendar/reagendar se fija `fecha_limite_respuesta`; estados nuevos
   `CANCELADA_QUEJOSO` (con motivo) y `SIN_RESPUESTA` (proceso cada 5 min). El analista puede
   registrar la respuesta del quejoso. Para el portal del quejoso (lo hace el compañero) quedan
   listos `/api/primer-contacto/quejoso/citas/...`; contrato en `docs/CONTRATO-CITAS-QUEJOSO.md`.

**Para desplegar en el servidor nuevo, agregar al yml de primer-contacto** (además de datasource,
JWT, `notificaciones.service.url` y `subdefensoria.base-url` con las IPs nuevas):
`antecedentes.modelo.url: http://156.67.26.73:8093` y `historico.datasource.url/username/password`
apuntando a `169.58.62.99:5433/historico_db`.

**Pruebas locales:** `dev-local/init/04-busqueda-manual-prueba.sql` (idempotente) agrega
denunciados de prueba y crea `historico_db` con 4 casos.
