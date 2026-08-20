# Especificación de Requerimientos — Plataforma de Quejas de la Defensoría de los Derechos Politécnicos (DDP)

Documento generado a partir del análisis directo del código fuente (backend, frontend y
configuración de despliegue) tal como existe hoy en el repositorio. Complementa — no
sustituye — a `docs/ARQUITECTURA.md`, `docs/CAMBIOS.md` y `docs/HALLAZGOS.md`, que documentan
la historia y el razonamiento de las decisiones. Este documento describe el sistema **como
está construido actualmente**, organizado por los tres actores del sistema: **Quejoso**,
**Recepcionista** (personal de revisión) y **Administrador de TI**.

Por seguridad, este documento no incluye contraseñas, secretos JWT ni credenciales reales
aunque existan en el repositorio (`config-files/`, `cONTEXTOQUEJOSO/accesos BASE DE DATOS.txt`)
— se describe su mecanismo y ubicación, no su valor.

---

## 0. Resumen ejecutivo

La plataforma permite a la comunidad politécnica presentar y dar seguimiento a quejas ante la
Defensoría de los Derechos Politécnicos, y da al personal de la Defensoría (recepción,
análisis y administración de TI) las herramientas para recibirlas, validarlas, canalizarlas y
resolverlas. Está construida como **7 microservicios Spring Boot** independientes que
comparten una sola base de datos Postgres, **3 aplicaciones Angular** (una por tipo de
usuario) y **4 contenedores Nginx** que hacen de proxy inverso público y de servidores de
estáticos. Todo corre en contenedores Podman sobre 2 VPS (backend+BD en una, frontend en
otra), con HTTPS público vía Let's Encrypt/Certbot.

---

## 1. Arquitectura general (transversal a los 3 actores)

### 1.1 Microservicios

| Microservicio | Puerto | Responsabilidad | Emite JWT | Base de datos |
|---|---|---|---|---|
| `auth-service` | 8083 | Login y sesión del quejoso, activación de cuenta "just-in-time", recuperación de contraseña, perfil del quejoso | Sí (quejosos) | `defensoria_db` (tabla `usuarios`) |
| `queja-service` (imagen/artefacto `quejas-service`) | 8084 | Registro de quejas (público y autenticado), consulta/edición de "mis quejas", evidencias, acuerdos de conciliación (lado quejoso) | No (solo verifica) | `defensoria_db` (tablas `quejas`, `queja_tutores`, `queja_evidencias`, `acuerdos_conciliacion`) |
| `notificaciones-service` | 8085 | Envío de correo (Gmail SMTP) y centro de notificaciones persistido por usuario | No (solo verifica) | `defensoria_db` (tabla `notificaciones`) |
| `catalogo-service` | 8086 | Catálogo de dependencias/unidades académicas del IPN (208 registros) | No (solo verifica) | `defensoria_db` (tabla `dependencias`) |
| `admin-service` | 8087 | Login del personal administrativo, gestión de cuentas de personal (usuarios y roles), catálogo (alta/edición), plantillas de oficios, respaldos y bitácora de seguridad | Sí (personal: los 5 roles de `RolStaff`) | `defensoria_db` (tablas `personal_administrativo`, `plantillas_documentos`, `bitacora_acciones`) |
| `revision-service` | 8088 | Bandeja de recepción, validación/rechazo/turnado de quejas, registro manual de quejas en papel, historial exportable, conciliación (lado staff) | No (solo verifica el JWT que emite `admin-service`) | `defensoria_db` (misma tabla `quejas`, vista extendida con columnas de flujo de revisión) |
| `chatbot-service` | 8089 | Menú de preguntas frecuentes fijas (mini-chat/tutorial) del portal público, y su CRUD administrativo | No (solo verifica) | `defensoria_db` (tabla `preguntas_chatbot`) |
| `primer-contacto-service` 🚧 | 8082 | Etapa de análisis de primer contacto: bandeja de análisis, citas, dictámenes, expedientes de análisis, notas y remisiones externas; turna expedientes admitidos a `subdefensoria-service` | No (sin seguridad todavía, ver §5) | H2 en memoria (propia, **no** `defensoria_db`, ver §5) |
| `subdefensoria-service` 🚧 | 8091 (ver §5, reasignado por choque de puerto) | Etapa de investigación de la Subdefensoría: bandeja de investigación, expedientes, control de plazos y alertas de vencimiento, recordatorios, oficios de información, respuestas externas y acuerdos de conclusión | No (sin seguridad todavía, ver §5) | H2 en memoria (propia, **no** `defensoria_db`, ver §5) |

🚧 = en desarrollo activo, agregados el 2026-08-14; ver §5 para su estado real, limitaciones
conocidas y lo que falta antes de considerarlos al mismo nivel de madurez que los 7 anteriores.

Los primeros 7 comparten la misma instancia de Postgres (`defensoria_db`, puerto 5432) y el mismo
`jwt.secret` simétrico (HS256) — es lo que permite que un JWT emitido por `auth-service` (para
un quejoso) sea válido en `queja-service`/`notificaciones-service`/`catalogo-service`, y que
uno emitido por `admin-service` (para personal) sea válido en `revision-service`/
`catalogo-service`/`chatbot-service`. Ningún microservicio salvo `auth-service` y
`admin-service` tiene lógica de login propia — todos los demás solo **verifican** el token
(`JwtAuthenticationFilter` + `JwtUtil`, idéntico patrón replicado en cada uno).

Todos los 7 exponen Swagger/OpenAPI en `/swagger-ui.html` y `/v3/api-docs` (rutas públicas).

### 1.2 Frontends (Angular 21, standalone, zoneless)

| Aplicación | Público | Ruta pública (nginx) | Puerto interno del contenedor |
|---|---|---|---|
| `Frontend` (portal del quejoso) | Comunidad politécnica en general, con o sin cuenta | `/` (dominio raíz) | 8090 (contenedor `defensoria-web`) |
| `Frontend-Admin` (consola de administración) | Personal con rol `ADMIN_SISTEMAS` | `/admin/` | 80 (contenedor `admin-web`, publicado en el host como 22346) |
| `Frontend-Revision` (panel de recepción/revisión) | Personal con rol `RECEPCIONISTA` (hoy el único rol habilitado en los controladores, ver §1.4) | `/revision/` | 80 (contenedor `revision-web`, publicado en el host como 22347) |

Los 3 son proyectos Angular independientes (sin monorepo), cada uno con su propio
`app.routes.ts`, guard de autenticación (`CanActivateFn` que revisa si hay JWT en el cliente),
interceptor que inyecta `Authorization: Bearer <token>` y un interceptor adicional
(`change-detection.interceptor.ts`) que fuerza `ApplicationRef.tick()` tras cada respuesta HTTP
porque Angular 21 zoneless no dispara detección de cambios automáticamente.

### 1.3 Nginx y puertos expuestos

```
Internet
   │
   ▼  HTTPS :443 (HTTP :80 → redirect 301)
┌─────────────────────────────┐
│  router-nginx  (VPS frontend, 2.25.64.47) │   ← único punto público, certificado
│  Let's Encrypt / Certbot (dominio          │     Let's Encrypt para defensoria-escom.ddns.net,
│  defensoria-escom.ddns.net)                │     renovación automática
└───────────────┬─────────────┘
   ┌─────────────┼──────────────────┬───────────────────┬─────────────────────┐
   │             │                  │                    │                     │
   ▼             ▼                  ▼                    ▼                     ▼
 /               /admin/           /revision/           /api/*                (proxy directo
   │             │                  │                    │                     a cada micro-
   ▼             ▼                  ▼                    ▼                     servicio, ver
defensoria-web  admin-web         revision-web      VPS backend (2.25.78.22)   tabla abajo)
:8090           :22346            :22347            puertos 8083-8089
(Angular        (Angular          (Angular
 quejoso)        admin)            revisión)
```

Reglas de `location` en el Nginx público (`router.conf`, análogo a
`nginx/config/defensoria.conf` conservado en el repo como referencia del patrón):

| Ruta pública | Destino | Puerto |
|---|---|---|
| `/` | `defensoria-web` (portal del quejoso) | 8090 |
| `/admin/` | `admin-web` | 22346 |
| `/revision/` | `revision-web` | 22347 |
| `/api/auth/` | `auth-service` (VPS backend) | 8083 |
| `/api/quejoso/` | `queja-service` | 8084 |
| `/api/notificaciones/` | `notificaciones-service` | 8085 |
| `/api/catalogos/` | `catalogo-service` | 8086 |
| `/api/admin/` | `admin-service` | 8087 |
| `/api/revision/` | `revision-service` | 8088 |
| `/api/chatbot/` | `chatbot-service` | 8089 |

Los puertos 8083-8089 del backend **no están abiertos a Internet**: el firewall de la VPS
backend solo acepta esas conexiones desde la IP de la VPS frontend. Los puertos 22346/22347
tampoco se exponen directamente al público — solo los usa `router-nginx` internamente. El
único puerto realmente público es 443 (y 80, únicamente para el redirect a HTTPS).

### 1.4 Roles y seguridad transversal

- `RolStaff` (definido igual en `admin-service` y `revision-service`, deben mantenerse
  sincronizados a mano): `ADMIN_SISTEMAS`, `RECEPCIONISTA`, `ANALISTA_PRIMER_CONTACTO`,
  `SUBDEFENSOR`, `DEFENSOR`.
- Hoy **todos los endpoints de `revision-service` exigen específicamente `RECEPCIONISTA`**
  (`@PreAuthorize("hasRole('RECEPCIONISTA')")`) — los otros 3 roles (analista, subdefensor,
  defensor) existen como datos capturables desde el panel de administración (se pueden crear
  cuentas con esos roles, y `revision-service` los usa como catálogo de "defensores
  disponibles" para turnar una queja) pero **no tienen todavía un panel ni permisos propios
  distintos al de recepción**. Es una limitación conocida, no un error.
- Contraseñas: BCrypt en los 2 servicios que emiten login (`auth-service`, `admin-service`).
  Política de complejidad: mínimo 8 caracteres, una mayúscula y un dígito (regex compartida).
  El personal administrativo se crea con contraseña temporal y `forzarCambioPassword=true`
  (debe cambiarla en el primer login).
  No existe todavía el cierre de sesión automático por inactividad (15 minutos) que se pidió
  para las cuentas de administración/recepción — está pendiente de implementar.
- CORS: `allowedOriginPatterns: *` + `allowCredentials(true)` en los 7 microservicios — seguro
  en este diseño porque la autenticación viaja por header `Authorization`, nunca por cookies.

---

## 2. Actor: QUEJOSO

Cualquier integrante de la comunidad politécnica (alumno, personal académico o
administrativo) que presenta una queja o le da seguimiento. Puede operar **sin cuenta**
(registro público + consulta por folio) o **con cuenta** (panel autenticado con historial,
edición, conciliación y notificaciones).

### 2.1 Requerimientos funcionales

- **RF-Q01 — Consultar el estado de una queja sin cuenta**: dado un folio (`FOL-XXXXXXXX`) y
  el correo con el que se presentó, mostrar el detalle de la queja (motivo, descripción,
  estatus, fecha).
- **RF-Q02 — Registrar una queja sin cuenta (registro público)**: capturar identidad completa
  del quejoso (nombre, apellidos, correo, fecha de nacimiento, tipo y número de
  identificación), unidad académica, fecha de los hechos, datos del denunciado, descripción
  libre, y hasta varios archivos de evidencia en la misma petición; genera un folio de
  seguimiento.
- **RF-Q03 — Captura de datos del tutor para menores de edad**: si la fecha de nacimiento
  indica menor de 18 años, exigir datos de un tutor/adulto responsable (nombre, apellidos,
  parentesco, correo, teléfono) antes de permitir el envío.
- **RF-Q04 — Crear/activar una cuenta de seguimiento**: a partir de un folio + correo ya
  registrados en una queja existente, definir una contraseña y activar la cuenta ("activación
  just-in-time" — la cuenta se crea en ese momento, no antes).
- **RF-Q05 — Iniciar sesión** con correo institucional y contraseña, obteniendo un token de
  sesión válido por 1 hora.
- **RF-Q06 — Recuperar contraseña olvidada** mediante un código de verificación de 6 dígitos
  enviado por correo, válido 10 minutos.
- **RF-Q07 — Ver un resumen** de la actividad propia (conteo de quejas por estatus) al entrar
  al panel.
- **RF-Q08 — Listar "Mis Quejas"** con filtros (por estatus y/o texto) y ver el detalle
  completo de cada una, incluida su línea de tiempo de estatus.
- **RF-Q09 — Editar una queja propia** (descripción, unidad académica, fecha de hechos, datos
  del denunciado) **solo mientras siga en estatus "RECIBIDA"** — una vez que Recepción la
  mueve a "EN_VALIDACION" y más adelante, deja de ser editable.
- **RF-Q10 — Registrar una nueva queja estando autenticado**, sin tener que volver a capturar
  su identidad (se toma del token de sesión), con el mismo soporte de evidencias múltiples.
- **RF-Q11 — Ver y descargar/previsualizar** las evidencias adjuntas a una queja propia.
- **RF-Q12 — Ver y responder acuerdos de conciliación** propuestos por la Defensoría
  (aceptar o rechazar, con comentario opcional).
- **RF-Q13 — Ver un centro de notificaciones** persistente (inicios de sesión, quejas creadas,
  cambios de estatus, conciliaciones) con contador de no leídas y marcado individual como
  leída.
- **RF-Q14 — Ver y editar su perfil**: correo personal, teléfono celular, unidad académica y
  domicilio son editables; nombre, correo institucional y boleta/número de empleado son de
  solo lectura (vienen de la activación de cuenta).
- **RF-Q15 — Consultar el catálogo de dependencias/unidades académicas del IPN** como parte
  del formulario de queja (selector, no texto libre).
- **RF-Q16 — Consultar un mini-chat de preguntas frecuentes** (árbol fijo de preguntas y
  respuestas sobre la Defensoría y el proceso de queja) sin necesidad de sesión.
- **RF-Q17 — Recibir avisos por correo electrónico**: confirmación de creación de cuenta,
  confirmación de restablecimiento de contraseña, y aviso al tutor cuando se registra una
  queja a nombre de un menor de edad.

### 2.2 Requerimientos no funcionales

- **RNF-Q01 — Disponibilidad del registro público**: debe poder presentarse una queja sin
  cuenta ni sesión, en cualquier momento, desde cualquier dispositivo con navegador.
- **RNF-Q02 — Confidencialidad en tránsito**: todo el tráfico público va sobre HTTPS
  (TLS 1.3, certificado Let's Encrypt); la autenticación viaja como `Bearer` token, no como
  credenciales en texto plano tras el login inicial.
- **RNF-Q03 — Tamaño de subida**: el proxy público acepta peticiones de hasta 100 MB
  (`client_max_body_size`) para permitir varios archivos de evidencia por queja; el backend
  acepta hasta 60 MB de tamaño total de petición multipart.
- **RNF-Q04 — Resiliencia de notificaciones**: un fallo al enviar un correo o registrar una
  notificación (ej. Gmail caído) nunca debe impedir que la queja se registre o la acción
  principal se complete — se degrada a solo quedar en el log del servidor.
- **RNF-Q05 — Usabilidad en formularios largos**: el formulario de registro público usa pasos
  numerados, previsualización de archivos elegidos, y una nota de ayuda flotante en vez de un
  bloque de texto fijo que reduce el espacio útil del formulario.
- **RNF-Q06 — Sesión expirable**: el token de sesión del quejoso expira en 1 hora; al expirar
  o ser rechazado (401/403), el cliente cierra sesión automáticamente y redirige a login.
- **RNF-Q07 — Feedback de estado en formularios**: toda operación (envío, error de validación,
  éxito) debe mostrar retroalimentación visual inmediata (toasts/alertas), no fallos
  silenciosos.

### 2.3 Reglas de negocio

- **RN-Q01**: una queja de un menor de 18 años **requiere** datos de tutor/adulto responsable
  capturados y confirmados antes de poder enviarse (el botón de enviar se bloquea y reabre el
  modal de tutor si faltan).
- **RN-Q02**: un usuario **menor de 14 años no puede autorregistrarse ni gestionar la queja
  directamente por sí mismo** — el mensaje de la interfaz deja explícito que debe ser el
  tutor quien complete el formulario a su nombre. No es un bloqueo duro de envío (la regla
  RN-Q01 ya cubre el caso, exigiendo tutor), es una regla de conducta/mensaje reforzado para
  este rango de edad específico.
- **RN-Q03**: el número de boleta o de número de empleado debe ser únicamente numérico y no
  puede exceder 12 caracteres.
- **RN-Q04**: una cuenta de quejoso solo puede activarse si existe al menos una queja previa
  con ese folio **y** ese correo exactamente (par folio+correo como llave de activación, no
  solo el folio).
- **RN-Q05**: una cuenta ya activa no puede volver a activarse (rechazo explícito si
  `usuario.activo == true`).
- **RN-Q06**: la contraseña debe tener mínimo 8 caracteres, al menos una mayúscula y al menos
  un dígito — aplica tanto a la activación de cuenta como al restablecimiento por código.
- **RN-Q07**: un código de recuperación de contraseña expira 10 minutos después de solicitado
  y solo es válido una vez.
- **RN-Q08**: una queja solo es editable por su autor mientras su `estatus` sea exactamente
  `"RECIBIDA"` — en cualquier otro estatus (`EN_VALIDACION`, `RECHAZADA`, `TURNADA`) la edición
  se rechaza en el backend, no solo se oculta en la interfaz.
- **RN-Q09**: un acuerdo de conciliación solo puede ser respondido (aceptado/rechazado) por el
  quejoso al que va dirigido (`correoInstitucional` del acuerdo debe coincidir con el correo
  del token de sesión).
- **RN-Q10**: el origen de una queja se etiqueta automáticamente como `"PUBLICO"` (registro sin
  sesión), `"AUTENTICADO"` (registro dentro del panel) o `"MANUAL"` (dada de alta por
  Recepción a partir de un documento físico) — no es un dato que el usuario declare, se infiere
  del endpoint usado.

### 2.4 Requerimientos técnicos

Microservicios que participan en los flujos del quejoso, y el puerto/ruta pública que los
expone (ver también §1.3):

| Función | Microservicio | Endpoint(s) principales | Ruta pública (nginx) |
|---|---|---|---|
| Login / activación / recuperación / perfil | `auth-service` (8083) | `POST /api/auth/login`, `POST /api/auth/activar-cuenta`, `POST /api/auth/solicitar-codigo`, `POST /api/auth/reset-password`, `GET/PUT /api/auth/me`, `/api/auth/perfil` | `/api/auth/` |
| Registro/consulta/edición de quejas, evidencias, conciliación (lado quejoso) | `queja-service` (8084) | `POST /api/quejoso/quejas/registro-publico` (público), `POST /api/quejoso/quejas/registrar` (JWT), `GET /api/quejoso/quejas/mias`, `GET/PUT /api/quejoso/quejas/mias/{folio}`, `GET /api/quejoso/quejas/folio/{folio}` (público), `GET/PUT /api/quejoso/conciliaciones/**` | `/api/quejoso/` |
| Centro de notificaciones | `notificaciones-service` (8085) | `GET /api/notificaciones/mias`, `GET /api/notificaciones/mias/no-leidas`, `PUT /api/notificaciones/{id}/leida` (JWT); `POST /api/notificaciones/enviar` y `/registrar` son de uso interno entre microservicios | `/api/notificaciones/` |
| Catálogo de dependencias | `catalogo-service` (8086) | `GET /api/catalogos/dependencias`, `GET /api/catalogos/dependencias/{clave}` (públicos) | `/api/catalogos/` |
| Mini-chat de preguntas frecuentes | `chatbot-service` (8089) | `GET /api/chatbot/menu` (público) | `/api/chatbot/` |
| Interfaz | `Frontend` (Angular) | — | `/` vía `defensoria-web` (puerto interno 8090) |

Autenticación: JWT emitido únicamente por `auth-service`, firmado HS256, expira en 1 hora,
verificado de forma independiente por `queja-service`, `notificaciones-service` y
`catalogo-service` mediante el mismo `jwt.secret` compartido.

### 2.5 Casos de uso

| ID | Nombre | Precondición | Flujo principal | Postcondición |
|---|---|---|---|---|
| CU-Q01 | Presentar una queja sin cuenta | Ninguna | El quejoso llena el formulario público (identidad, motivo, evidencias, tutor si aplica) y lo envía → `queja-service` valida datos, genera folio, guarda evidencias como BYTEA, dispara notificación al tutor si aplica | Queja creada con estatus `RECIBIDA` y origen `PUBLICO`; se muestra el folio generado |
| CU-Q02 | Consultar una queja por folio | Se conoce folio + correo | El quejoso los captura en "Consultar queja" → `GET /api/quejoso/quejas/folio/{folio}` | Se muestra el detalle/estatus de la queja |
| CU-Q03 | Crear cuenta de seguimiento | Existe una queja previa con ese folio + correo | El quejoso define una contraseña en "Activar cuenta" → `POST /api/auth/activar-cuenta` valida folio+correo contra `queja-service` (vía Feign) y crea/activa el `Usuario` | Cuenta activa; puede iniciar sesión |
| CU-Q04 | Iniciar sesión | Cuenta activa | Correo + contraseña → `POST /api/auth/login` | Token JWT emitido; se registra una notificación de "inicio de sesión" |
| CU-Q05 | Recuperar contraseña | Cuenta activa existente | Solicita código → lo recibe por correo → lo captura junto con nueva contraseña | Contraseña actualizada; se envía correo de confirmación |
| CU-Q06 | Presentar una queja autenticado | Sesión iniciada | Desde "Nueva Queja" del panel, llena motivo/descripción/evidencias (identidad ya conocida por el token) | Queja creada con estatus `RECIBIDA` y origen `AUTENTICADO` |
| CU-Q07 | Editar una queja propia | Sesión iniciada; queja en estatus `RECIBIDA` | Desde el detalle de "Mis Quejas", edita descripción/unidad/fecha/denunciado y guarda | Queja actualizada; si ya no está en `RECIBIDA`, el backend rechaza la edición |
| CU-Q08 | Responder un acuerdo de conciliación | Sesión iniciada; existe un acuerdo `PENDIENTE` dirigido a ese correo | Revisa el acuerdo en "Acuerdos de Conciliación" y lo acepta o rechaza, con comentario opcional | Acuerdo pasa a `ACEPTADO` o `RECHAZADO`, con fecha de respuesta registrada |
| CU-Q09 | Revisar notificaciones | Sesión iniciada | Abre "Notificaciones", ve la lista y marca como leídas | Contador de no leídas se actualiza en tiempo real (signal compartido con la barra lateral) |
| CU-Q10 | Actualizar perfil | Sesión iniciada | Edita correo personal/teléfono/unidad académica/domicilio en "Configuración de Perfil" y guarda | Perfil actualizado en `usuarios` |

### 2.6 Flujo de trabajo

**Camino sin cuenta:** Inicio → "Presentar una queja" → formulario público (+ modal de tutor
si es menor) → envío a `queja-service` (`/registro-publico`, público) → folio generado →
pantalla de éxito con opción de "Crear cuenta con este folio" → Activar cuenta
(`auth-service` valida el folio contra `queja-service` vía Feign) → login → panel autenticado.

**Camino con cuenta:** Login (`auth-service`) → token JWT → panel (`/panel`, protegido por
`authGuard`) → Resumen / Mis Quejas / Nueva Queja / Conciliación / Notificaciones / Perfil, cada
uno llamando directamente al microservicio dueño de esos datos (`queja-service` o
`notificaciones-service`) con el JWT en el header `Authorization`. Toda acción relevante
(login, queja creada, cambio de estatus hecho por Recepción, conciliación) deja un registro en
`notificaciones-service` visible en "Notificaciones" del panel, además del correo
correspondiente cuando aplica.

---

## 3. Actor: RECEPCIONISTA (personal de revisión)

Personal de la Defensoría con rol `RECEPCIONISTA`, encargado de recibir, validar y canalizar
las quejas que llegan tanto por el portal público como en papel. Opera desde
`Frontend-Revision` (`/revision/`), autenticado contra `admin-service`.

### 3.1 Requerimientos funcionales

- **RF-R01 — Iniciar sesión** con las mismas credenciales de personal administrativo
  (`admin-service`), pero usando el panel de revisión.
- **RF-R02 — Ver una bandeja de entrada** con contadores (pendientes, en proceso, turnadas hoy)
  y la lista de quejas por trabajar.
- **RF-R03 — Ver el detalle de una queja** (resumen completo + documentos/evidencias
  adjuntos) para validarla.
- **RF-R04 — Buscar antecedentes**: otras quejas previas de la misma persona, para detectar
  posibles duplicados antes de turnar.
- **RF-R05 — Descargar/previsualizar los documentos adjuntos** de una queja.
- **RF-R06 — Rechazar una queja** por documentación incompleta, indicando motivos
  (selección) y observaciones libres; esto notifica automáticamente al quejoso por correo.
- **RF-R07 — Turnar una queja**: canalizarla a un área/dependencia y a un
  defensor/subdefensor específico, con comentarios, generando el folio oficial de turnado.
- **RF-R08 — Registrar manualmente una queja recibida en papel** (identidad del quejoso,
  dependencia, número de oficio, fecha de recepción física, tipo de documento, descripción,
  ubicación física del expediente, y un archivo digitalizado opcional), generando su propio
  folio de seguimiento.
- **RF-R09 — Consultar un historial de trámites** ya procesados (turnados o rechazados), con
  filtros por texto libre, estatus y fecha.
- **RF-R10 — Exportar el historial filtrado a Excel** (.xlsx).
- **RF-R11 — Emitir un acuerdo de conciliación** dirigido al quejoso de una queja, con asunto
  y términos.
- **RF-R12 — Consultar catálogos de apoyo**: lista de áreas/dependencias (desde
  `catalogo-service`) y de personal con rol Defensor/Subdefensor disponible, para los combos
  de turnado.
- **RF-R13 — Cambiar su propia contraseña** desde el panel.

### 3.2 Requerimientos no funcionales

- **RNF-R01 — Trazabilidad**: cada rechazo y turnado queda con el correo de quien lo procesó
  (`validadoPor`) y su fecha, sin excepción.
- **RNF-R02 — Un solo punto de autenticación para el personal**: el panel de revisión no tiene
  login propio, siempre valida contra `admin-service` — evita duplicar lógica de contraseñas.
- **RNF-R03 — Exportación en formato estándar**: el historial se exporta en `.xlsx` real
  (Apache POI), no CSV, para abrirse directamente en Excel/Sheets sin conversión.
- **RNF-R04 — Resiliencia del correo de rechazo**: si el envío de correo al quejoso falla, el
  rechazo en sí no debe fallar — se degrada a quedar solo en el log del servidor.
- **RNF-R05 — Consistencia con el resto del sistema**: mismos componentes visuales, mismos
  patrones de error y mismos criterios de accesibilidad que el panel de administración
  (comparten los mismos estilos base).

### 3.3 Reglas de negocio

- **RN-R01 — Máquina de estados de una queja**: `RECIBIDA → EN_VALIDACION → RECHAZADA` o
  `RECIBIDA → EN_VALIDACION → TURNADA`. Una vez `RECHAZADA` o `TURNADA`, la queja no vuelve a
  un estado anterior desde este panel.
- **RN-R02**: solo cuentas con rol `RECEPCIONISTA` pueden usar cualquier endpoint de
  `revision-service` (`@PreAuthorize("hasRole('RECEPCIONISTA')")` a nivel de cada controlador,
  no solo a nivel de ruta).
- **RN-R03**: rechazar una queja exige al menos un motivo y genera automáticamente el correo
  de rechazo al quejoso — no es una acción silenciosa.
- **RN-R04**: turnar una queja exige área y defensor asignado; al turnar se genera un folio
  oficial de turnado, distinto (o derivado) del folio original de seguimiento.
- **RN-R05**: una queja registrada manualmente (papel) recibe `origenRegistro = "MANUAL"` y
  puede no tener boleta/número de empleado conocido de antemano (`tipoUsuarioManual` admite
  `"externo"`, a diferencia del registro propio del quejoso).
- **RN-R06**: el catálogo de "defensores disponibles" para turnar solo incluye personal con
  rol `DEFENSOR` o `SUBDEFENSOR` activo — no se puede turnar a un `RECEPCIONISTA` ni a un
  `ADMIN_SISTEMAS`.
- **RN-R07**: `revision-service` y `queja-service` leen y escriben la **misma tabla física**
  `quejas` mediante dos clases `@Entity` independientes (patrón "entidad propia sobre tabla
  compartida") — un cambio de estatus hecho por Recepción es visible de inmediato para el
  quejoso sin llamada entre servicios, porque ambos leen directo de Postgres.

### 3.4 Requerimientos técnicos

| Función | Microservicio | Endpoint(s) principales | Ruta pública (nginx) |
|---|---|---|---|
| Login del personal | `admin-service` (8087) | `POST /api/admin/auth/login` | `/api/admin/` |
| Bandeja, validación, rechazo, turnado, registro manual, historial, conciliación (lado staff) | `revision-service` (8088) | `GET /api/revision/bandeja`, `GET /api/revision/quejas/{folio}`, `GET /api/revision/quejas/{folio}/antecedentes`, `GET /api/revision/quejas/evidencias/{id}`, `POST /api/revision/quejas/{folio}/rechazar`, `POST /api/revision/quejas/{folio}/turnar`, `POST /api/revision/registro-manual`, `GET /api/revision/historial`, `GET /api/revision/historial/exportar`, `GET /api/revision/catalogos/areas`, `GET /api/revision/catalogos/defensores`, `POST/GET /api/revision/conciliaciones` | `/api/revision/` |
| Catálogo de dependencias (combo de área) | `catalogo-service` (8086) | `GET /api/catalogos/dependencias` | `/api/catalogos/` |
| Correo de rechazo al quejoso | `notificaciones-service` (8085) | `POST /api/notificaciones/enviar` (llamada interna) | `/api/notificaciones/` (uso interno) |
| Interfaz | `Frontend-Revision` (Angular) | — | `/revision/` vía `revision-web` (puerto interno 80, publicado como 22347) |

Autenticación: JWT emitido por `admin-service` (mismo formato/secreto que el de quejosos, pero
con claim de `rol`), verificado por `revision-service` con `@EnableMethodSecurity` +
`@PreAuthorize` a nivel de cada controlador.

### 3.5 Casos de uso

| ID | Nombre | Precondición | Flujo principal | Postcondición |
|---|---|---|---|---|
| CU-R01 | Iniciar sesión en el panel de revisión | Cuenta de personal activa con rol `RECEPCIONISTA` | Login contra `admin-service` | Token JWT emitido; acceso a `/revision/` |
| CU-R02 | Revisar la bandeja de entrada | Sesión iniciada | Abre "Bandeja" → ve contadores + lista de pendientes | — |
| CU-R03 | Validar y rechazar una queja | Queja en estatus `RECIBIDA`/`EN_VALIDACION` | Abre el detalle, revisa documentos y antecedentes, marca motivos de rechazo + observaciones, confirma | Queja pasa a `RECHAZADA`; se envía correo al quejoso |
| CU-R04 | Validar y turnar una queja | Queja en estatus `RECIBIDA`/`EN_VALIDACION` | Abre el detalle, revisa antecedentes, elige área y defensor, agrega comentarios, confirma | Queja pasa a `TURNADA` con folio oficial, área y defensor asignados |
| CU-R05 | Registrar una queja recibida en papel | Documento físico recibido | Llena "Registro Manual" con los datos del documento y, opcionalmente, adjunta el escaneo | Nueva queja con origen `MANUAL` y folio de seguimiento propio |
| CU-R06 | Consultar y exportar historial | Existen trámites ya procesados | Filtra por texto/estatus/fecha en "Historial"; exporta a Excel si lo necesita | Archivo `.xlsx` descargado con el filtro aplicado |
| CU-R07 | Emitir un acuerdo de conciliación | Existe una queja válida con folio conocido | Captura asunto y términos, dirigido al folio/correo del quejoso | Acuerdo creado en estado `PENDIENTE`, visible para el quejoso en su panel |

### 3.6 Flujo de trabajo

Login (`admin-service`) → Bandeja (`revision-service`) → selecciona una queja pendiente →
Detalle + Antecedentes (misma tabla `quejas`, distinta entidad JPA que la que usa
`queja-service`) → decide: **Rechazar** (motivos + observaciones → correo automático al
quejoso vía `notificaciones-service`) o **Turnar** (área desde `catalogo-service` + defensor
desde el catálogo de personal → folio oficial) → la queja sale de la bandeja de pendientes y
pasa al Historial, consultable y exportable en cualquier momento. En paralelo, puede dar de
alta quejas recibidas en papel (Registro Manual) y emitir acuerdos de conciliación sobre
cualquier queja por folio.

---

## 4. Actor: ADMINISTRADOR DE TI

Personal con rol `ADMIN_SISTEMAS`, responsable de la operación técnica y administrativa de la
plataforma: cuentas del resto del personal, catálogo institucional, plantillas de documentos
oficiales, respaldos de base de datos y bitácora de seguridad. Opera desde `Frontend-Admin`
(`/admin/`).

### 4.1 Requerimientos funcionales

- **RF-A01 — Iniciar sesión** como personal administrativo.
- **RF-A02 — Ver un dashboard general** con un resumen del estado del sistema (tarjetas de
  "Configuración General").
- **RF-A03 — Gestionar cuentas de personal** ("Usuarios y Roles"): listar, crear (con
  contraseña temporal autogenerada), editar (nombre/correo/rol), restablecer contraseña, dar
  de baja y reactivar cuentas de cualquiera de los 5 roles de `RolStaff`.
- **RF-A04 — Administrar el catálogo de dependencias/unidades académicas**: listar todas
  (activas e inactivas), crear, editar, e **importar/actualizar masivamente desde un archivo
  Excel** (formato SIA/IPN).
- **RF-A05 — Administrar plantillas de documentos oficiales**: listar, ver placeholders
  disponibles (ej. `{folio_queja}`, `{nombre_quejoso}`), previsualizar con datos de ejemplo, y
  editar/publicar el contenido de cada plantilla.
- **RF-A06 — Gestionar respaldos de la base de datos**: listar respaldos existentes, ejecutar
  un respaldo manual bajo demanda, descargar un respaldo específico, y restaurar la base de
  datos desde un respaldo (operación destructiva, exige confirmación explícita).
- **RF-A07 — Consultar la bitácora de acciones críticas**: últimas 50 acciones registradas
  (creación/edición/baja/reactivación de usuarios, restablecimientos de contraseña,
  actualizaciones de plantillas, respaldos manuales, restauraciones e inicios de sesión), con
  usuario, acción, IP y fecha.
- **RF-A08 — Administrar el contenido del mini-chat**: listar (activas e inactivas), crear,
  editar y eliminar preguntas frecuentes del chatbot del portal público.
- **RF-A09 — Cambiar su propia contraseña** desde el panel.

### 4.2 Requerimientos no funcionales

- **RNF-A01 — Autoprovisión del primer acceso**: si la tabla de personal está vacía en el
  primer arranque de `admin-service`, se crea automáticamente una cuenta `ADMIN_SISTEMAS`
  inicial con contraseña temporal impresa una sola vez en el log del servidor — nunca queda
  hardcodeada en el frontend ni se expone por API.
- **RNF-A02 — Trazabilidad total**: toda acción administrativa relevante (no solo login) queda
  registrada en la bitácora con IP de origen — requisito de auditoría.
- **RNF-A03 — Operación destructiva protegida**: restaurar la base de datos exige mandar
  `{"confirmar": true}` explícito en el cuerpo de la petición — no puede dispararse por
  accidente con un único clic.
- **RNF-A04 — Importación masiva tolerante a errores**: la carga de dependencias desde Excel
  debe reportar un resumen (filas procesadas/creadas/actualizadas/con error), no fallar en
  bloque ante una fila mal formada.
- **RNF-A05 — Densidad de información**: el panel de administración debe aprovechar el
  espacio en pantalla (se identificó como pendiente de mejora: letra pequeña y espacio en
  blanco desperdiciado en el diseño actual).
- **RNF-A06 — Backups como responsabilidad exclusiva de este rol**: solo `ADMIN_SISTEMAS`
  puede generar, descargar o restaurar respaldos — ningún otro rol tiene ese endpoint
  disponible ni siquiera a nivel de ruta.

### 4.3 Reglas de negocio

- **RN-A01**: todos los endpoints de `admin-service` (excepto `/api/admin/auth/**` y
  `/api/admin/perfil/**`, que cualquier cuenta autenticada puede usar sobre sí misma) exigen
  específicamente rol `ADMIN_SISTEMAS`.
- **RN-A02**: una cuenta nueva de personal se crea siempre con `cuentaTemporal=true` y
  `forzarCambioPassword=true` — debe cambiar su contraseña en el primer inicio de sesión.
- **RN-A03**: dar de baja una cuenta **desactiva**, no elimina — conserva el historial e
  integridad referencial con acciones pasadas en la bitácora.
- **RN-A04**: el correo institucional y el número de empleado de personal administrativo son
  únicos en el sistema (constraint de base de datos, no solo validación de interfaz).
- **RN-A05**: la ruta de administración del catálogo (`/api/catalogos/dependencias/admin/**`)
  se evalúa **antes** que la regla pública general del mismo prefijo — sin ese orden explícito,
  Spring Security aplicaría la regla pública (menos restrictiva) por ser más genérica. Mismo
  patrón aplicado en `chatbot-service` para `/api/chatbot/admin/**`.
- **RN-A06**: una plantilla de documento se identifica por una clave estable de negocio
  (`tipo`, ej. `"OFICIO_SOLICITUD_INFORMACION"`), no por el id autogenerado — permite
  referenciarla desde otros lugares del sistema sin depender del orden de creación.
- **RN-A07**: cada vez que se actualiza el contenido de una plantilla se registra quién la
  actualizó y cuándo (`actualizadoPor`, `actualizadoEn`), además del registro en bitácora.

### 4.4 Requerimientos técnicos

| Función | Microservicio | Endpoint(s) principales | Ruta pública (nginx) |
|---|---|---|---|
| Login, cuentas de personal, plantillas, respaldos, bitácora, dashboard, perfil propio | `admin-service` (8087) | `POST /api/admin/auth/login`, `GET/POST/PUT/DELETE /api/admin/personal/**`, `GET/PUT /api/admin/plantillas/**`, `GET/POST /api/admin/seguridad/respaldos/**`, `POST /api/admin/seguridad/restaurar`, `GET /api/admin/seguridad/bitacora`, `GET /api/admin/dashboard/resumen`, `PUT /api/admin/perfil/password` | `/api/admin/` |
| Administración del catálogo de dependencias | `catalogo-service` (8086) | `GET /api/catalogos/dependencias/admin` (todas), `POST/PUT /api/catalogos/dependencias/admin/**`, `POST /api/catalogos/dependencias/admin/importar-excel` | `/api/catalogos/` |
| Administración del contenido del chatbot | `chatbot-service` (8089) | `GET/POST/PUT/DELETE /api/chatbot/admin/preguntas/**` | `/api/chatbot/` |
| Interfaz | `Frontend-Admin` (Angular) | — | `/admin/` vía `admin-web` (puerto interno 80, publicado como 22346) |

`admin-service` tiene su propio `Dockerfile` (distinto al compartido por los demás
microservicios) porque necesita `postgresql-client` instalado para ejecutar `pg_dump`/`psql`
al generar y restaurar respaldos; monta además un volumen persistente para que los archivos de
respaldo sobrevivan a la reconstrucción del contenedor.

Autenticación: JWT propio, emitido por `admin-service`, con claim `rol`; verificado con
`@EnableMethodSecurity` en `admin-service`, `catalogo-service`, `chatbot-service` y
`revision-service`.

### 4.5 Casos de uso

| ID | Nombre | Precondición | Flujo principal | Postcondición |
|---|---|---|---|---|
| CU-A01 | Iniciar sesión como administrador | Cuenta `ADMIN_SISTEMAS` activa | Login en `/admin/` | Token JWT emitido; acceso al panel |
| CU-A02 | Dar de alta a un miembro del personal | Sesión iniciada | En "Usuarios y Roles", captura nombre/número de empleado/correo/rol y confirma | Cuenta creada con contraseña temporal; queda registrada en bitácora |
| CU-A03 | Restablecer la contraseña de otra cuenta | Sesión iniciada; cuenta destino existe | Selecciona la cuenta → "Restablecer contraseña" | Nueva contraseña temporal generada; queda registrada en bitácora |
| CU-A04 | Dar de baja/reactivar una cuenta | Sesión iniciada | Selecciona la cuenta → confirma baja o reactivación | Cuenta pasa a `activo=false`/`true`; queda registrada en bitácora |
| CU-A05 | Importar el catálogo desde Excel | Sesión iniciada; archivo Excel con formato SIA/IPN | Sube el archivo en "Catálogo de Unidades Académicas" | Dependencias creadas/actualizadas masivamente; se muestra un resumen del resultado |
| CU-A06 | Editar una plantilla de oficio | Sesión iniciada | Abre la plantilla, edita el contenido usando los placeholders disponibles, previsualiza, publica | Plantilla actualizada; queda registrada en bitácora |
| CU-A07 | Ejecutar un respaldo manual | Sesión iniciada | En "Seguridad y Respaldos", solicita un respaldo manual | Archivo de respaldo generado y listado; queda registrado en bitácora |
| CU-A08 | Restaurar desde un respaldo | Sesión iniciada; existe al menos un respaldo | Selecciona el archivo, confirma explícitamente la operación destructiva | Base de datos restaurada al estado del respaldo; queda registrada en bitácora |
| CU-A09 | Revisar la bitácora de seguridad | Sesión iniciada | Abre "Historial de Acciones Críticas" | Se listan las últimas 50 acciones con usuario/IP/fecha |
| CU-A10 | Administrar el mini-chat | Sesión iniciada | Crea, edita o elimina preguntas frecuentes en el módulo del chatbot | Cambios visibles de inmediato en el widget público del portal |

### 4.6 Flujo de trabajo

Login (`admin-service`) → Dashboard (resumen general) → cuatro frentes de trabajo
independientes entre sí: (1) **Usuarios y Roles** — ciclo de vida completo de las cuentas de
todo el personal, incluidas las que usará Recepción; (2) **Catálogo de Unidades Académicas** —
alta/edición individual o importación masiva desde Excel, consumida en tiempo real por el
formulario público de quejas y por el turnado de Recepción; (3) **Plantillas Oficiales** —
contenido reutilizable con placeholders para los documentos que en el futuro generará el flujo
de revisión; (4) **Seguridad y Respaldos** — resguardo periódico de la base de datos y
auditoría de toda acción administrativa vía bitácora. El administrador de TI es, en la
práctica, quien habilita al resto del personal (crea sus cuentas) y mantiene los datos de
referencia (catálogo, plantillas) que usan tanto el portal público como el panel de revisión.

---

## 5. Microservicios en desarrollo — Primer Contacto y Subdefensoría

Agregados al repositorio el 2026-08-14 (`Backend/primercontacto/`, `Backend/subdefensoria/`).
Cubren las dos etapas del proceso que siguen después de Recepción (`revision-service`) y que
corresponden a los roles `ANALISTA_PRIMER_CONTACTO` y `SUBDEFENSOR` de `RolStaff` (definidos
desde el principio en `admin-service`/`revision-service`, pero sin backend propio hasta ahora).
Esta sección documenta **solo lo que ya existe y su estado real** — su lógica de negocio no se
modificó ni se completó como parte de esta tarea, únicamente se les dio forma de despliegue
(imagen, contenedor, puertos, configuración de producción) para que puedan compilarse y
levantarse igual que el resto de los microservicios.

### 5.1 Qué hace cada uno (según su código actual)

- **`primer-contacto-service`** (carpeta local `Backend/primercontacto/`): bandeja de análisis
  de primer contacto, citas, dictámenes, expedientes de análisis, notas de análisis y
  remisiones externas. Al emitir un acuerdo de admisión, notifica a `subdefensoria-service`
  (`POST /api/subdefensoria/ingesta/expedientes`) para turnarle el expediente — llamada
  "best-effort" (si Subdefensoría no responde, el dictamen ya guardado no se revierte, solo se
  registra en el log del servidor). El código referencia explícitamente los puntos "TPR-07/09
  del BPMN" del proceso institucional.
- **`subdefensoria-service`** (carpeta local `Backend/subdefensoria/`): bandeja e investigación
  de expedientes turnados desde Primer Contacto, control de plazos y alertas de vencimiento,
  recordatorios, oficios de solicitud de información, registro de respuestas externas, y
  acuerdos de conclusión. Trae ya codificados los plazos oficiales del **Manual de
  Procedimientos DDP-PO-02**: primera solicitud de información a una unidad académica = 10 días
  hábiles; solicitudes subsecuentes (recordatorios) = 5 días hábiles
  (`DiasHabilesCalculator`, `plazos.*` en su configuración).
- Comunicación entre ambos: push servidor-a-servidor vía `RestTemplate` en ambas direcciones
  (Primer Contacto → Subdefensoría al admitir; Subdefensoría → Primer Contacto, según su propia
  config, para reflejar conclusión o regresar un expediente) — no pasa por Nginx ni por el
  navegador, así que no le aplica CORS.

### 5.2 Limitaciones conocidas (no corregidas a propósito, fuera de alcance de esta tarea)

- **Sin seguridad**: ambos traen un `SecurityFilterChain` que marca `permitAll()` en
  absolutamente todo (incluida `/h2-console/**`). No verifican JWT ni exigen rol — a diferencia
  de los 7 microservicios ya integrados, que verifican el mismo `jwt.secret` compartido. No se
  tocó porque es lógica de estos servicios, pero es el hallazgo más importante antes de
  exponerlos al dominio público: hoy cualquiera que alcance su puerto puede llamar cualquier
  endpoint.
- **Base de datos propia y en memoria**: cada uno usa H2 (`jdbc:h2:mem:...`), no la
  `defensoria_db` de Postgres compartida por el resto del sistema. Los datos se pierden en cada
  reinicio del contenedor — aceptable mientras están en desarrollo, no para producción real.
  Sus propios `application.properties` ya lo señalan como "H2 temporal".
- **Sin ruta pública ni frontend**: no existe todavía un `Frontend-PrimerContacto` ni
  `Frontend-Subdefensoria`, ni una entrada en `router.conf`/`nginx/config/defensoria.conf` para
  ellos (a diferencia de `/admin/` y `/revision/`). Sus `cors.allowed-origins` de desarrollo
  apuntan a `localhost:4300`/`localhost:4301` (puertos típicos de un `ng serve` futuro). Por
  ahora solo son alcanzables dentro de la VPS backend o entre sí.
- **Dependencia de MySQL sin usar**: ambos `pom.xml` incluyen el conector de MySQL, pero
  ninguno tiene un datasource de MySQL configurado — probablemente la intención original antes
  de decidir usar H2 temporalmente. No se tocó.

### 5.3 Qué se hizo en esta tarea (solo despliegue, cero cambios de lógica)

| Archivo | Cambio |
|---|---|
| `Backend/primercontacto/pom.xml` | Se agregó `<finalName>primer-contacto-service</finalName>` en `<build>` (antes generaba `primercontacto-0.0.1-SNAPSHOT.jar`; ahora genera `primer-contacto-service.jar`, el nombre que espera `podman-compose.sh`). |
| `Backend/subdefensoria/pom.xml` | Mismo cambio: `<finalName>subdefensoria-service</finalName>`. |
| `Backend/podman-compose.sh` | `primer-contacto-service` y `subdefensoria-service` agregados al arreglo `SERVICIOS` y a `get_port()`. |
| `Backend/config-files/primer-contacto-service/config/primer-contacto-service.yml` (nuevo) | Config de producción: `server.port: 8082` (sin cambio, no chocaba con nada), `cors.allowed-origins` apuntando al dominio público (placeholder hasta que exista su frontend), `subdefensoria.base-url` apuntando al puerto real de producción de `subdefensoria-service`. |
| `Backend/config-files/subdefensoria-service/config/subdefensoria-service.yml` (nuevo) | Config de producción: **`server.port: 8091`** (su default de código, 8083, choca con `auth-service`; 8090 también se descartó por ya estar asignado a `defensoria-web` en la VPS frontend — se reasignó solo a nivel de config de despliegue, sin tocar `application.properties`), `cors.allowed-origins` (mismo criterio que arriba), `primer-contacto.base-url` apuntando al puerto real de `primer-contacto-service`. |
| `Backend/Dockerfile` | **No se creó un Dockerfile nuevo** — el compartido en la raíz de `Backend/` (genérico, recibe `JAR_FILE`/`SERVICE_PORT` como build-args) ya sirve para estos dos, igual que para la mayoría de los microservicios (solo `admin-service` necesita uno propio, por `pg_dump`). |

Puertos de producción resultantes: `primer-contacto-service` **8082**, `subdefensoria-service`
**8091** (no 8083, tampoco 8090). Ambos siguen sin exponerse por Nginx ni tener un firewall específico
abierto hacia la VPS frontend — eso queda pendiente para cuando tengan frontend real.

### 5.4 Cómo se genera el folio, y cómo se "pasa" una queja entre áreas

Pregunta frecuente para quien vaya a tocar estos dos servicios — documentado directamente del
código, no es una decisión de esta tarea.

**Folio**: se genera igual en los dos únicos lugares que hoy crean una queja nueva
(`QuejaService` en `queja-service`, y `RevisionQuejaService.registrarManual` en
`revision-service`, para el registro en papel) — cada uno tiene su propia copia idéntica de
esta lógica, no está compartida en una librería común:

```java
"FOL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
// ej. FOL-A1B2C3D4
```

No es secuencial ni por fecha, es un fragmento de UUID aleatorio. `numero_folio` tiene
`UNIQUE` en Postgres, pero el código no reintenta si llegara a chocar (colisión
prácticamente imposible con 8 caracteres hexadecimales, pero técnicamente no está cubierta).

**La "relación" NO es igual entre todos los servicios** — hay dos mecanismos distintos
conviviendo en el sistema, y es la parte que más confunde:

1. **`queja-service` ↔ `revision-service`**: comparten la misma fila física en Postgres (tabla
   `quejas`, ver §6). No hay ninguna llamada HTTP entre ellos para esto — ambos leen/escriben
   directo la misma tabla, y se "encuentran" porque los dos usan `numero_folio` como criterio
   de búsqueda (`findByNumeroFolio`). Es el patrón que ya se explicó en el respaldo de
   estructura (`docs/ESQUEMA-BD-*.md`).
2. **`primer-contacto-service` ↔ `subdefensoria-service`**: **no** comparten base de datos
   (cada uno tiene su propio H2 en memoria, ver §5.2) — la única forma en que uno se entera de
   algo del otro es que alguien le haga un **POST explícito** con los datos completos en el
   cuerpo. No hay tabla en común que consultar.

**Cómo se pasa hoy una queja entre estas dos áreas nuevas, en la práctica**:

- **Hacia Primer Contacto**: `POST /api/primer-contacto/subdefensoria/quejas`
  (`IngestaSubdefensoriaController`, body `QuejaEntranteDTO`: `quejaId`, `folio`, `tema`,
  `descripcionHechos`, `fechaRecepcion`, `prioridad`, `quejoso`, `evidencias`). Primer Contacto
  la guarda en memoria (`QuejaEnMemoriaStore`, indexada por `folio` y por `quejaId`) — **no la
  persiste en su BD**, porque según el propio comentario del código "la fuente de verdad de la
  queja es Subdefensoría". ⚠️ **Hallazgo**: en todo el código revisado, **nada llama todavía a
  este endpoint** — ni `queja-service` ni `revision-service` tienen un cliente HTTP hacia
  `primer-contacto-service`. Es una puerta de entrada lista, pero sin quién toque la puerta
  todavía. Habrá que decidir con tu compañero quién la va a llamar (¿`revision-service` al
  turnar? ¿un nuevo cliente en otro servicio?) y con qué `quejaId` (todo apunta a que debería
  ser el mismo `id` — el bigint autogenerado, no el folio — de la fila real en
  `quejas` de Postgres, para que folio y quejaId sigan siendo el mismo par en todos lados).
- **Hacia Subdefensoría**: esta sí está conectada de punta a punta. Cuando el analista dictamina
  `POST /api/primer-contacto/dictamenes/competente` (admite la queja), `DictamenPrimerContactoService`
  automáticamente arma un `ExpedienteEntranteRequest` (mismos campos + `abogadoAsesorNombre`,
  `fechaAdmision`, `observacionesAnalista`) y hace
  `POST http://<host-subdefensoria>:8091/api/subdefensoria/ingesta/expedientes` — llamada
  "best-effort" (si Subdefensoría no responde, el dictamen ya guardado en Primer Contacto no se
  revierte, solo se registra en el log del servidor, ver `SubdefensoriaClientService`).
- ⚠️ **Segundo hallazgo**: cuando el analista cambia el estatus de una queja dentro de Primer
  Contacto (`actualizarEstatusQueja`), ese cambio **solo se refleja en el `QuejaEnMemoriaStore`
  en memoria de ese servicio** — no hay ninguna llamada de vuelta hacia `queja-service`/
  `revision-service` para actualizar la columna real `quejas.estatus` en Postgres. Hoy, un
  cambio de estatus dentro de Primer Contacto o Subdefensoría **no lo ve ni Recepción ni el
  quejoso en su panel** — viven todavía como universos separados del resto del sistema. Es
  probablemente el hueco más importante a cerrar antes de considerar esta integración completa.

### 5.5 Checklist para desplegar estos dos por primera vez en el servidor

1. `mvn clean package -DskipTests` dentro de `Backend/primercontacto/` y de
   `Backend/subdefensoria/` (genera `target/primer-contacto-service.jar` y
   `target/subdefensoria-service.jar` gracias al `finalName` agregado).
2. Copiar ambos jars a `/apps/aplicaciones/defensoria/back/artifact/` en la VPS backend, con
   esos nombres exactos.
3. Copiar `Backend/config-files/primer-contacto-service/` y
   `Backend/config-files/subdefensoria-service/` (carpetas completas) al mismo `config-files/`
   del servidor.
4. `bash podman-compose.sh up-container primer-contacto-service` y
   `bash podman-compose.sh up-container subdefensoria-service`.
5. Abrir en el firewall de hPanel de la VPS backend los puertos **8082** y **8091**, restringidos
   a la IP de la VPS frontend (mismo patrón que 8083-8089) — solo si en algún momento necesitan
   ser alcanzables desde fuera de la VPS backend; si de momento solo se llaman entre sí dentro
   de la misma VPS, no es indispensable todavía.
6. Verificar con `podman ps -a` y `podman logs primer-contacto-service` /
   `podman logs subdefensoria-service`.

## 6. Anexo — Esquema de base de datos (Postgres, `defensoria_db`)

Base única compartida por los 7 microservicios; cada tabla es gestionada por Hibernate
(`ddl-auto: update`, sin Flyway/Liquibase) desde el microservicio que la "posee"; dos tablas
(`quejas`, `acuerdos_conciliacion`) son escritas por dos microservicios distintos, cada uno con
su propia clase `@Entity` mapeando la misma tabla física.

| Tabla | Propietario(s) | Columnas relevantes |
|---|---|---|
| `usuarios` | `auth-service` | id, nombre, correo_institucional (único), boleta (único), password (BCrypt), unidad_academica, activo, correo_personal, telefono_celular, domicilio, nombre_tutor, parentesco_tutor, telefono_tutor, codigo_recuperacion, fecha_expiracion_codigo |
| `quejas` | `queja-service` **y** `revision-service` (entidades JPA independientes sobre la misma tabla) | id, numero_folio (único), correo_institucional, motivo, descripcion, fecha_creacion, nombre/apellidos/fecha_nacimiento/tipo y número de identificación del quejoso, unidad_academica_clave, fecha_hechos, nombre/apellido_denunciado, origen_registro, **estatus** (RECIBIDA/EN_VALIDACION/RECHAZADA/TURNADA), motivo_rechazo, area_turnada, defensor_asignado, comentarios_recepcion, validado_por, fecha_validacion, fecha_turnado, numero_oficio, fecha_recepcion_fisica, tipo_documento_fisico, ubicacion_fisica_expediente, tipo_usuario_manual |
| `queja_tutores` | `queja-service` | id, queja_id (FK, único — relación 1 a 1), nombre, apellido_paterno, apellido_materno, parentesco, correo, telefono |
| `queja_evidencias` | `queja-service` (escritura); `revision-service` (lectura/descarga) | id, queja_id (FK), nombre_archivo, tipo_mime, tamanio_bytes, contenido (`bytea`, archivo completo), fecha_subida |
| `acuerdos_conciliacion` | `queja-service` (lectura/respuesta) **y** `revision-service` (creación) | id, numero_folio, correo_institucional, asunto, terminos, estado (PENDIENTE/ACEPTADO/RECHAZADO), fecha_emision, fecha_respuesta, comentario_quejoso, creado_por |
| `notificaciones` | `notificaciones-service` | id, correo_destino, tipo (LOGIN/QUEJA_CREADA/CAMBIO_ESTATUS/CONCILIACION/GENERAL), titulo, mensaje, leida, fecha_creacion, enlace |
| `dependencias` | `catalogo-service` | id, clave (único), clave_padre, nombre, abreviatura, tipo, categoria, nivel, pagina_manual, activo, notas, correo_contacto, nombre_titular, creado_en |
| `personal_administrativo` | `admin-service` | id, nombre_completo, numero_empleado (único), correo_institucional (único), rol (RolStaff), password (BCrypt), cuenta_temporal, forzar_cambio_password, activo, fecha_creacion, ultimo_login |
| `plantillas_documentos` | `admin-service` | id, tipo (único, clave de negocio), nombre, contenido, activa, actualizado_en, actualizado_por |
| `bitacora_acciones` | `admin-service` | id, usuario, accion_realizada, ip, fecha |
| `preguntas_chatbot` | `chatbot-service` | id, categoria, pregunta, respuesta, orden (global, no por categoría), activo, creado_en, actualizado_en |

---

## 7. Anexo — Matriz de despliegue (contenedores Podman)

| Contenedor | VPS | Puerto(s) | Imagen |
|---|---|---|---|
| `defensoria-db` | Backend (2.25.78.22) | 5432 | `postgres:16` |
| `auth-service` | Backend | 8083 | `defensoria-auth-service` |
| `quejas-service` | Backend | 8084 | `defensoria-quejas-service` |
| `notificaciones-service` | Backend | 8085 | `defensoria-notificaciones-service` |
| `catalogo-service` | Backend | 8086 | `defensoria-catalogo-service` |
| `admin-service` | Backend | 8087 | `defensoria-admin-service` (Dockerfile propio, con cliente Postgres) |
| `revision-service` | Backend | 8088 | `defensoria-revision-service` |
| `chatbot-service` | Backend | 8089 | `defensoria-chatbot-service` |
| `primer-contacto-service` 🚧 | Backend | 8082 | `defensoria-primer-contacto-service` |
| `subdefensoria-service` 🚧 | Backend | 8091 | `defensoria-subdefensoria-service` |
| `defensoria-web` | Frontend (2.25.64.47) | 8090 (interno) | `defensoria-front-img` (portal del quejoso) |
| `admin-web` | Frontend | 22346→80 | `defensoria-admin-img` |
| `revision-web` | Frontend | 22347→80 | `defensoria-revision-img` |
| `router-nginx` | Frontend | 80/443 (público) | `nginx:alpine` + config propia + certificados Let's Encrypt |

Cada microservicio backend monta su configuración de producción (`config-files/<servicio>/config`)
como volumen (no horneada en la imagen), lo que permite cambiar variables de config sin
reconstruir — solo reiniciar el contenedor. El código en sí (el `.jar`) sí requiere
reconstrucción completa (`mvn clean package` → nuevo artefacto → `podman-compose.sh
up-container <servicio>`) para tomar cambios de lógica.

---

## 8. Anexo — Reglas de firewall (hPanel, VPS backend `2.25.78.22` / `srv1804187`)

Confirmadas directamente por el usuario desde el panel de Hostinger. Se evalúan en orden, de
arriba hacia abajo, con un `drop` final (catch-all) — cualquier puerto que no tenga una regla
`accept` explícita antes de esa última línea queda bloqueado.

| # | Acción | Protocolo | Puerto | Origen | Detalle de origen |
|---|---|---|---|---|---|
| 1 | accept | TCP | 443 | any | any |
| 2 | accept | TCP | 8083 (`auth-service`) | custom | `2.25.64.47/32` |
| 3 | accept | TCP | 8084 (`quejas-service`) | custom | `2.25.64.47/32` |
| 4 | accept | TCP | 8085 (`notificaciones-service`) | custom | `2.25.64.47/32` |
| 5 | accept | TCP | 22 (SSH) | any | any |
| 6 | accept | TCP | 80 | any | any |
| 7 | accept | TCP | 8080 | any | any |
| 8 | accept | TCP | 22345 | any | any |
| 9 | accept | TCP | 8086 (`catalogo-service`) | custom | `2.25.64.47/32` |
| 10 | accept | TCP | 8087 (`admin-service`) | custom | `2.25.64.47/32` |
| 11 | accept | TCP | 8088 (`revision-service`) | custom | `2.25.64.47/32` |
| 12 | accept | TCP | 8089 (`chatbot-service`) | custom | `2.25.64.47/32` |
| 13 | accept | TCP | 8090 | custom | `2.25.64.47/32` |
| 14 | accept | TCP | 8082 (`primer-contacto-service` 🚧) | custom | `2.25.64.47/32` |
| 15 | accept | TCP | 8091 (`subdefensoria-service` 🚧) | custom | `2.25.64.47/32` |
| 16 | drop | any | any | any | any |

Notas de lectura, cruzando esto con lo que corre hoy en el `podman ps -a` de esta VPS:

- Los puertos 8082-8089 y 8091 ya están correctamente restringidos únicamente a la IP de la
  VPS frontend (`2.25.64.47`, la que hospeda `router-nginx`) — nadie más en Internet puede
  llegar directo a un microservicio backend, tienen que pasar por el proxy público.
- La regla del **8090** existe en este firewall pero, en la arquitectura actual, **ningún
  contenedor de la VPS backend escucha ahí** (`subdefensoria-service` quedó asignado a 8091
  justamente para no chocar con el 8090 que ya usa `defensoria-web` en la *otra* VPS, la
  frontend — ver §5). Es una regla que no hace daño (no hay nada detrás que exponga), pero
  tampoco protege nada hoy; queda documentada tal cual por transparencia.
- **80, 8080 y 22345 abiertos a `any` en la VPS backend** no corresponden a ningún servicio de
  los documentados aquí (ni `router-nginx` ni ningún microservicio corren en esta VPS con esos
  puertos) — por el número de puerto y el patrón, todo apunta a que son remanentes de una
  sesión de diagnóstico anterior (`docs/CAMBIOS.md` documenta un workaround temporal con el
  puerto **22345** durante el bloqueo del 80/443, aunque ese episodio ocurrió en la VPS
  *frontend*, no en esta). No se tocaron ni se investigaron más a fondo porque no fue parte de
  lo pedido en esta tarea — vale la pena que el administrador de TI las revise y las cierre si
  confirma que no las usa nada en esta VPS.
- **443 abierto a `any`** en la VPS backend tampoco corresponde a nada de lo documentado (HTTPS
  se sirve desde la VPS frontend, no desde esta) — mismo comentario que el punto anterior.

## 9. Anexo — Catálogo de endpoints por microservicio

Extraído directamente de las clases `@RestController` de cada servicio (no de Swagger). "Auth"
indica qué exige cada endpoint: **Público** (sin token), **JWT** (cualquier token válido del
emisor correspondiente), o **JWT + rol** (token válido y el/los rol(es) exacto(s) vía
`@PreAuthorize`).

### 9.1 `auth-service` (8083) — prefijo `/api/auth`

| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| POST | `/login` | Público | Login del quejoso, devuelve JWT |
| POST | `/solicitar-codigo` | Público | Genera y envía código de recuperación (6 dígitos, 10 min) |
| POST | `/reset-password` | Público | Cambia contraseña validando el código |
| POST | `/activar-cuenta` | Público | Activación "just-in-time" con folio + correo |
| GET | `/me` | JWT (quejoso) | Perfil completo del usuario autenticado |
| PUT | `/perfil` | JWT (quejoso) | Actualiza correo personal/teléfono/unidad académica/domicilio |

### 9.2 `queja-service` (8084) — prefijo `/api/quejoso`

| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| POST | `/quejas/validar-folio` | Público | Valida folio+correo (lo consume `auth-service` vía Feign) |
| POST | `/quejas/registrar` | JWT (quejoso) | Registra queja autenticada (multipart, varios archivos) |
| GET | `/quejas/mias` | JWT (quejoso) | Lista las quejas propias |
| GET | `/quejas/mias/{folio}` | JWT (quejoso) | Detalle de una queja propia |
| PUT | `/quejas/mias/{folio}` | JWT (quejoso) | Edita una queja propia (solo si `estatus=RECIBIDA`) |
| GET | `/quejas/mias/{folio}/evidencias` | JWT (quejoso) | Metadatos de evidencias de una queja propia |
| GET | `/quejas/folio/{folio}?correo=` | Público | Detalle por folio+correo (consulta pública / activación) |
| POST | `/quejas/registro-publico` | Público | Registro de queja sin sesión (multipart, identidad + tutor opcional) |
| GET | `/conciliaciones/mias` | JWT (quejoso) | Lista acuerdos de conciliación dirigidos al usuario |
| PUT | `/conciliaciones/{id}/respuesta` | JWT (quejoso) | Acepta o rechaza un acuerdo propio |

### 9.3 `notificaciones-service` (8085) — prefijo `/api/notificaciones`

| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| POST | `/enviar` | Público (uso interno entre microservicios) | Envía un correo simple vía Gmail SMTP |
| POST | `/registrar` | Público (uso interno entre microservicios) | Persiste una notificación para un usuario |
| GET | `/mias` | JWT | Lista notificaciones del usuario autenticado |
| GET | `/mias/no-leidas` | JWT | Cuenta notificaciones no leídas |
| PUT | `/{id}/leida` | JWT | Marca una notificación propia como leída |

### 9.4 `catalogo-service` (8086) — prefijo `/api/catalogos`

| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| GET | `/dependencias?tipo=` | Público | Lista dependencias activas (filtro opcional por tipo) |
| GET | `/dependencias/{clave}` | Público | Detalle de una dependencia por clave |
| GET | `/dependencias/admin` | JWT + `ADMIN_SISTEMAS` | Lista todas (activas e inactivas) |
| POST | `/dependencias/admin` | JWT + `ADMIN_SISTEMAS` | Crea una dependencia |
| PUT | `/dependencias/admin/{clave}` | JWT + `ADMIN_SISTEMAS` | Edita una dependencia |
| POST | `/dependencias/admin/importar-excel` | JWT + `ADMIN_SISTEMAS` | Importación/actualización masiva desde Excel |

### 9.5 `admin-service` (8087) — prefijo `/api/admin`

| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| POST | `/auth/login` | Público | Login del personal administrativo (cualquier rol) |
| GET | `/personal` | JWT + `ADMIN_SISTEMAS` | Lista el personal administrativo |
| POST | `/personal` | JWT + `ADMIN_SISTEMAS` | Crea una cuenta con contraseña temporal |
| PUT | `/personal/{id}` | JWT + `ADMIN_SISTEMAS` | Edita nombre/correo/rol |
| POST | `/personal/{id}/resetear-password` | JWT + `ADMIN_SISTEMAS` | Genera nueva contraseña temporal |
| DELETE | `/personal/{id}` | JWT + `ADMIN_SISTEMAS` | Da de baja (desactiva) una cuenta |
| POST | `/personal/{id}/reactivar` | JWT + `ADMIN_SISTEMAS` | Reactiva una cuenta dada de baja |
| GET | `/plantillas` | JWT + `ADMIN_SISTEMAS` | Lista plantillas de documentos |
| GET | `/plantillas/placeholders` | JWT + `ADMIN_SISTEMAS` | Placeholders disponibles y su significado |
| GET | `/plantillas/{tipo}` | JWT + `ADMIN_SISTEMAS` | Obtiene una plantilla por tipo |
| GET | `/plantillas/{tipo}/previsualizar` | JWT + `ADMIN_SISTEMAS` | Previsualiza con datos de ejemplo |
| PUT | `/plantillas/{tipo}` | JWT + `ADMIN_SISTEMAS` | Actualiza y publica el contenido |
| GET | `/seguridad/respaldos` | JWT + `ADMIN_SISTEMAS` | Lista respaldos disponibles |
| POST | `/seguridad/respaldos/manual` | JWT + `ADMIN_SISTEMAS` | Ejecuta un respaldo manual |
| GET | `/seguridad/respaldos/{nombreArchivo}/descargar` | JWT + `ADMIN_SISTEMAS` | Descarga un respaldo |
| POST | `/seguridad/restaurar` | JWT + `ADMIN_SISTEMAS` | Restaura la BD desde un respaldo (requiere `confirmar:true`) |
| GET | `/seguridad/bitacora` | JWT + `ADMIN_SISTEMAS` | Últimas 50 acciones críticas |
| GET | `/dashboard/resumen` | JWT + `ADMIN_SISTEMAS` | Resumen para las tarjetas del dashboard |
| PUT | `/perfil/password` | JWT (cualquier rol de personal) | Cambia la contraseña de la cuenta propia |

### 9.6 `revision-service` (8088) — prefijo `/api/revision`

| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| GET | `/bandeja` | JWT + `RECEPCIONISTA` | Contadores + lista de quejas por trabajar |
| GET | `/quejas/{folio}` | JWT + `RECEPCIONISTA` | Detalle de una queja para validarla |
| GET | `/quejas/{folio}/antecedentes` | JWT + `RECEPCIONISTA` | Otras quejas previas de la misma persona |
| GET | `/quejas/evidencias/{id}` | JWT + `RECEPCIONISTA` | Descarga un documento adjunto |
| POST | `/quejas/{folio}/rechazar` | JWT + `RECEPCIONISTA` | Rechaza y notifica por correo al quejoso |
| POST | `/quejas/{folio}/turnar` | JWT + `RECEPCIONISTA` | Canaliza a área/defensor, genera folio oficial |
| GET | `/catalogos/areas` | JWT + `RECEPCIONISTA` | Dependencias/unidades académicas (combo) |
| GET | `/catalogos/defensores` | JWT + `RECEPCIONISTA` | Personal Defensor/Subdefensor disponible |
| POST | `/registro-manual` | JWT + `RECEPCIONISTA` | Alta de queja recibida en papel (multipart) |
| GET | `/historial` | JWT + `RECEPCIONISTA` | Trámites procesados, con filtros |
| GET | `/historial/exportar` | JWT + `RECEPCIONISTA` | Exporta el historial filtrado a `.xlsx` |
| POST | `/conciliaciones` | JWT + (`RECEPCIONISTA`\|`SUBDEFENSOR`\|`DEFENSOR`\|`ADMIN_SISTEMAS`) | Emite un acuerdo de conciliación |
| GET | `/conciliaciones?folio=` | JWT + (`RECEPCIONISTA`\|`SUBDEFENSOR`\|`DEFENSOR`\|`ADMIN_SISTEMAS`) | Lista acuerdos emitidos |

### 9.7 `chatbot-service` (8089) — prefijo `/api/chatbot`

| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| GET | `/menu` | Público | Categorías + preguntas/respuestas del mini-chat |
| GET | `/admin/preguntas` | JWT + `ADMIN_SISTEMAS` | Lista todas (activas e inactivas) |
| POST | `/admin/preguntas` | JWT + `ADMIN_SISTEMAS` | Crea una pregunta |
| PUT | `/admin/preguntas/{id}` | JWT + `ADMIN_SISTEMAS` | Edita una pregunta |
| DELETE | `/admin/preguntas/{id}` | JWT + `ADMIN_SISTEMAS` | Elimina una pregunta |

### 9.8 `primer-contacto-service` 🚧 (8082) — prefijo `/api/primer-contacto`

Sin seguridad todavía (`permitAll()` total, ver §5.2) — la columna "Auth" describe la intención
funcional, no una restricción real hoy.

| Método | Ruta | Auth (intención) | Descripción |
|---|---|---|---|
| GET | `/bandeja` | Analista | Bandeja de análisis completa |
| GET | `/bandeja/folio/{folio}` | Analista | Busca un elemento de la bandeja por folio |
| POST | `/bandeja/filtrar` | Analista | Filtra la bandeja (`FiltroExpedienteDTO`) |
| GET | `/bandeja/prioridad/{prioridad}` | Analista | Filtra por prioridad |
| GET | `/bandeja/estatus/{estatus}` | Analista | Filtra por estatus |
| POST | `/citas` | Analista | Agenda una cita de primer contacto |
| GET | `/citas/queja/{quejaId}` | Analista | Citas de una queja (por id interno) |
| GET | `/citas/folio/{folio}` | Analista | Citas de una queja (por folio) |
| GET | `/citas/agenda?fecha=` | Analista | Agenda del día |
| GET | `/citas/analista/{analistaId}` | Analista | Agenda de un analista específico |
| PUT | `/citas/{id}/confirmar` | Analista | Confirma una cita |
| PUT | `/citas/{id}/cancelar` | Analista | Cancela una cita |
| POST | `/dictamenes/competente` | Analista | Registra dictamen de competencia (admite) |
| POST | `/dictamenes/improcedente` | Analista | Registra dictamen de improcedencia |
| GET | `/dictamenes/queja/{quejaId}` | Analista | Dictamen de una queja (por id interno) |
| GET | `/dictamenes/folio/{folio}` | Analista | Dictamen de una queja (por folio) |
| GET | `/expedientes/{quejaId}` | Analista | Expediente de análisis (por id interno) |
| GET | `/expedientes/folio/{folio}` | Analista | Expediente de análisis (por folio) |
| POST | `/subdefensoria/quejas` | Interno (servidor a servidor) | Ingesta de una queja nueva/turnada a analizar |
| GET | `/subdefensoria/quejas` | Diagnóstico | Lista lo que hay en el store en memoria |
| GET | `/subdefensoria/status` | Diagnóstico | Cuenta total de quejas en memoria |
| POST | `/notas` | Analista | Crea una nota de análisis |
| GET | `/notas/queja/{quejaId}` | Analista | Notas de una queja (por id interno) |
| GET | `/notas/folio/{folio}` | Analista | Notas de una queja (por folio) |
| PUT | `/notas/{id}` | Analista | Edita una nota |
| DELETE | `/notas/{id}` | Analista | Elimina una nota |
| POST | `/remisiones` | Analista | Crea una remisión externa |
| GET | `/remisiones/queja/{quejaId}` | Analista | Remisión de una queja (por id interno) |
| GET | `/remisiones/folio/{folio}` | Analista | Remisión de una queja (por folio) |
| PUT | `/remisiones/queja/{quejaId}/enviar` | Analista | Envía/despacha la remisión |

### 9.9 `subdefensoria-service` 🚧 (8091) — prefijo `/api/subdefensoria`

Sin seguridad todavía (`permitAll()` total, ver §5.2) — la columna "Auth" describe la intención
funcional, no una restricción real hoy.

| Método | Ruta | Auth (intención) | Descripción |
|---|---|---|---|
| POST | `/acuerdos-conclusion` | Subdefensor/Abogado Asesor | Redacta y, si aplica, concluye el expediente |
| GET | `/acuerdos-conclusion/expediente/{expedienteId}` | Subdefensor/Abogado Asesor | Acuerdo de conclusión de un expediente |
| GET | `/alertas/vencidos` | Subdefensor/Abogado Asesor | Expedientes con plazo vencido |
| GET | `/bandeja-nuevas` | Subdefensor/Abogado Asesor | Quejas/expedientes recién ingresados |
| GET | `/control-plazos` | Subdefensor/Abogado Asesor | "Semáforo" de plazos por expediente |
| GET | `/expedientes` | Subdefensor/Abogado Asesor | Bandeja unificada de expedientes con su progreso |
| GET | `/expedientes/folio/{folio}` | Subdefensor/Abogado Asesor | Detalle de un expediente por folio |
| POST | `/ingesta/expedientes` | Interno (servidor a servidor) | Recibe un expediente admitido desde `primer-contacto-service` |
| POST | `/oficios` | Subdefensor/Abogado Asesor | Redacta y envía el oficio vigente de solicitud de información |
| GET | `/oficios/folio/{folio}` | Subdefensor/Abogado Asesor | Historial de oficios de un expediente |
| GET | `/oficios/{oficioId}` | Subdefensor/Abogado Asesor | Detalle de un oficio |
| POST | `/recordatorios` | Subdefensor/Abogado Asesor | Genera/envía un recordatorio firmado |
| POST | `/respuestas-externas` | Subdefensor/Abogado Asesor | Registra la respuesta de la unidad académica |
