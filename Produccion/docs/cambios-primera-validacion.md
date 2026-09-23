# Cambios — Primera Validación

**Proyecto:** Plataforma de Defensoría de los Derechos Politécnicos (TT)
**Tipo de prueba:** Validación de happy path (front + back)
**Ambiente:** `defensoria-escom.ddns.net` · Front VPS 2.25.64.47 · Back + BD VPS 2.25.78.22
**Stack:** Angular 21 (zoneless) · Spring Boot 3.5.16 / Java 21 · PostgreSQL 16 · Podman
**Inicio:** 2026-09-08
**Última actualización:** 2026-09-08 — BD desplegada y verificada; faltan Frontend y Backend

> **Despliegue en curso.** Base de datos ✅ aplicada (migración + vaciado de datos de prueba +
> las 10 restricciones promovidas a `VALIDATE`). Siguen Frontend y luego Backend.

---

## Leyenda de estados

| Estado | Significado |
|---|---|
| ✅ OK | El happy path corre completo, sin errores |
| ⚠️ OBS | Funciona, pero con observaciones |
| ❌ FALLA | Se rompe en algún paso; requiere cambio |
| 🔧 IMPLEMENTADO | Cambio hecho en código, pendiente de desplegar y reprobar |
| ⏳ PEND | Aún no se prueba |

**Severidad:** Bloqueante (impide operar / corrompe datos) · Mayor (permite datos inválidos) · Menor (texto, UX)

---

## Resumen

| # | Caso de uso | Pantalla | Front | Back | BD | Estado | Hallazgos |
|---|---|---|---|---|---|---|---|
| CU-Q01 | Presentar una queja sin cuenta | `/queja/registro` | 🔧 | 🔧 | ✅ | 🔧 IMPLEMENTADO | 14 detectados · 13 resueltos · 1 pospuesto |
| CU-Q07 | Consultar mis quejas | `/panel/mis-quejas` | 🔧 | 🔧 | — | 🔧 IMPLEMENTADO | 7 detectados · 7 resueltos |

---

## CU-Q01 — Presentar una queja sin cuenta

**Actor:** Quejoso (usuario anónimo, sin sesión iniciada)
**Pantalla:** `defensoria-escom.ddns.net/queja/registro` → `Frontend/src/app/pages/registro-queja-publico/`
**Endpoint:** `POST /api/quejoso/quejas/registro-publico` (público, sin JWT) → `queja-service`
**Fecha de prueba:** 2026-09-08
**Estado:** 🔧 Cambios aplicados en código. Falta desplegar y reprobar.

---

### Tabla de hallazgos

| ID | Hallazgo | Capa | Severidad | Estado |
|---|---|---|---|---|
| H-01.1 | Nombre y apellidos del quejoso aceptan números | F+B+BD | Mayor | 🔧 Implementado |
| H-01.2 | Correo sin validación de formato | F+B | Mayor | 🔧 Implementado |
| H-01.3 | Fecha de nacimiento sin rango válido | F+B+BD | Mayor | 🔧 Implementado |
| H-01.4 | Boleta acepta letras y no limita longitud | F+B+BD | Mayor | 🔧 Implementado |
| H-01.5 | Identificación: extensión, tamaño y cantidad sin restringir | F+B+BD | Bloqueante | 🔧 Implementado |
| H-01.6 | Título de la sección 2 | F | Menor | 🔧 Implementado |
| H-01.7 | Nombre y apellido del denunciado aceptan números | F+B+BD | Mayor | 🔧 Implementado |
| H-01.8 | Falta "Segundo Apellido del denunciado" | F+B+BD | Mayor | 🔧 Implementado |
| H-01.9 | Sección de tutor para menores | — | — | ✅ Ya funcionaba |
| H-01.10 | Previsualización de archivos cargados | F | Mayor | 🔧 Implementado |
| H-01.11 | Endpoint público sin CAPTCHA ni rate limiting | B | Mayor | ⏳ Pospuesto por decisión del usuario |
| H-01.12 | Sin aviso de privacidad ni consentimiento | F+B+BD | Mayor | 🔧 Implementado |
| H-01.13 | Datepicker genera fechas inexistentes (`2004-21-19`) | F | **Bloqueante** | 🔧 Corregido |
| H-01.14 | Fecha inválida responde 500 en vez de 400 legible | B | Mayor | 🔧 Corregido |

---

### Detalle de los cambios

#### H-01.1 / H-01.7 — Nombres y apellidos solo admiten letras

**Regla:** letras con acentos y ñ, permitiendo espacio interno, apóstrofe y guion entre
palabras. Longitud 2–50. Se aplica al quejoso, al denunciado y al tutor.

```
^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+(?:[ '\-][A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+)*$
```

Deliberadamente **no** es "solo A-Z": eso rechazaría `María José`, `D'Angelo` o
`Pérez-Gómez`, que son nombres reales.

- **Front:** los dígitos y símbolos ya no se pueden teclear (`(keypress)="soloLetras($event)"`), y si se pegan aparece el mensaje debajo del campo.
- **Back:** `ValidadorQuejaPublica.validarNombre()`, con `Textos.normalizar()` antes de validar — así `"Juan   Carlos"` con espacios de más no se rechaza por algo que el usuario no puede ver.
- **BD:** `CHECK` de formato en las seis columnas de nombre.

#### H-01.2 — Correo en dos niveles

- **Nivel 1 (todos los dominios):** formato general, máximo 254 caracteres, el punto no puede abrir ni cerrar la parte local ni repetirse.
- **Nivel 2 (solo `gmail.com` / `googlemail.com`):** parte local únicamente letras, dígitos y punto, de 6 a 30 caracteres.

Las restricciones que reportaste (`&`, `=`, `_`, `'`, `-`, `+`, `,`, `<`, `>`) son de Gmail al
crear una cuenta, no del correo en general. Aplicándolas solo al dominio que las impone,
`juan-perez@outlook.com` y `maria_lopez@yahoo.com.mx` siguen entrando, y `juan_perez@gmail.com`
se rechaza.

El correo se normaliza a minúsculas antes de guardarse.

#### H-01.3 — Fecha de nacimiento entre 1920 y el año pasado

Con la fecha de hoy (08/09/2026) el rango válido es **01/01/1920 – 31/12/2025**.

El año se calcula en tiempo de ejecución con el reloj del servidor en zona
`America/Mexico_City`, no como constante: si se dejara fijo, la regla caducaría el 1 de enero.

- **Front:** `min`/`max` en el datepicker (solo UX).
- **Back:** `ValidadorFechas` — fuente de verdad.
- **BD:** `CHECK (fecha_nacimiento_quejoso >= DATE '1920-01-01')`. El tope superior **no** puede ir en un `CHECK` porque `CURRENT_DATE` no es inmutable en Postgres; queda a cargo del backend.

**Extra:** también se validó la fecha de los hechos, que tampoco lo estaba — no puede ser
futura ni anterior a la fecha de nacimiento.

#### H-01.4 — Boleta / número de empleado: solo dígitos, máximo 10

`^\d{1,10}$`. Antes el campo permitía 12 caracteres.

Se conserva como `VARCHAR`, no numérico: los ceros a la izquierda de una boleta del IPN son
significativos y un entero los destruiría.

#### H-01.5 — Identificación oficial: JPG/PNG, 3MB, hasta 2 imágenes

Cambios respecto de lo que había:

| | Antes | Ahora |
|---|---|---|
| Tipos | PDF, JPG, PNG (y en la práctica cualquier cosa) | **Solo JPG y PNG** |
| Tamaño | 30MB | **3MB** por imagen |
| Cantidad | 1 | **Hasta 2** (frente y reverso) |
| Verificación | por extensión | **por firma binaria del archivo** |

El texto de ayuda del uploader se corrigió: decía "PDF, JPG o PNG — máximo 30MB", que ya no
era cierto.

Lo importante es la última fila. La extensión y el `Content-Type` los controla el cliente, así
que renombrar un ejecutable a `credencial.jpg` bastaba para subirlo. Ahora el backend lee los
primeros bytes del archivo y compara contra la firma real de JPEG (`FF D8 FF`) y PNG
(`89 50 4E 47`). En un endpoint público sin autenticación, ese es el único control que
realmente cuenta.

Se subió de 1MB a 3MB como pediste: 1MB rechazaría fotos de credencial tomadas con celular.

#### H-01.6 — Título de la sección 2

"Datos de la Queja" → **"Lugar de los hechos"**. También cambió el encabezado equivalente en
el acuse de recibo imprimible.

#### H-01.8 — Segundo apellido del denunciado

Campo nuevo en las tres capas: input en el formulario, `apellidoMaternoDenunciado` en el DTO y
la entidad, columna `apellido_materno_denunciado` en `quejas`. Nullable, porque el quejoso
puede no conocerlo. También aparece ya en el acuse y se puede editar desde el panel autenticado.

#### H-01.10 — Previsualización de archivos

Las imágenes cargadas se muestran como miniatura con su nombre y peso, y cada una se puede
quitar antes de enviar. Aplica tanto a la identificación como a las evidencias; los adjuntos
que no son imagen (PDF, MP4, MP3) muestran un recuadro con su extensión.

Las miniaturas usan `URL.createObjectURL`, que reserva memoria del navegador hasta liberarse:
cada URL se revoca al quitar el archivo y al salir de la pantalla.

#### H-01.13 — El datepicker generaba fechas que no existen

**Encontrado al probar la pantalla ya desplegada.** Al enviar la queja salía "Ocurrió un error
inesperado en el servidor", y el log del backend mostraba `rejected value [2004-21-19]`.

Los `<select>` de mes y año del calendario usaban `[value]="i"`, que guarda el índice como
**cadena**. Al armar la fecha ISO, `"2" + 1` concatena en vez de sumar y da `"21"`: seleccionar
Marzo producía el mes 21. El calendario se veía correcto porque `new Date()` sí convierte sus
argumentos a número — solo la cadena que se manda al backend salía mal, así que el error era
invisible en pantalla.

Es un bug **anterior a CU-Q01** (el archivo no se tocaba desde el 20 de agosto) y afecta a las
cuatro pantallas que usan el datepicker. Corregido con `[ngValue]` más `Number()` defensivo.

**Y una falla propia de la validación de CU-Q01**: `errorFechaNacimiento` comparaba cadenas
contra los límites, y `"2004-21-19"` es mayor que `"1920-01-01"` y menor que `"2025-12-31"`, así
que pasaba. Comparar fechas ISO como texto sirve para ordenar, no para validar. Se agregó
`esFechaValida()`, que reconstruye la fecha y verifica que año, mes y día coincidan con lo
escrito — descarta también los "31 de febrero" que JavaScript convierte en 3 de marzo.

#### H-01.14 — Una fecha inválida respondía 500

El `MethodArgumentNotValidException` de la conversión caía en el manejador genérico y salía como
**500 "Ocurrió un error inesperado en el servidor"**. Engañoso dos veces: no es un error del
servidor sino del dato enviado, y no dice qué corregir.

`GlobalExceptionHandler` ahora maneja `BindException` y responde **400** nombrando el campo y el
valor rechazado. Se agregó también el manejo de `MaxUploadSizeExceededException` (413), que
tenía el mismo problema.

#### H-01.12 — Aviso de privacidad

Ventana emergente al entrar a `/queja/registro`. La casilla "He leído y acepto" está
deshabilitada hasta que el texto se desliza hasta el final; al marcarla, el modal se cierra y
se puede capturar la queja. Hay un botón "No acepto" que sale del formulario — el
consentimiento tiene que poder negarse.

Si el texto cabe completo sin necesidad de scroll (pantalla grande, zoom reducido), la casilla
se habilita sola: de lo contrario nunca habría evento de scroll y el formulario quedaría
bloqueado para siempre.

La aceptación queda registrada con la queja: `aviso_privacidad_aceptado`,
`aviso_privacidad_fecha` (la pone el servidor, no el cliente) y `aviso_privacidad_version`. Si
el texto del aviso cambia, se sabe qué versión aceptó cada quejoso. El backend rechaza toda
queja que no lo traiga en `true`.

**El texto del aviso debe revisarlo alguien con criterio legal antes de considerarlo
definitivo.** Es un aviso simplificado y genérico: no inventa correo de contacto (remite a la
sección Contacto del portal) ni cita artículos concretos.

#### H-01.11 — CAPTCHA y rate limiting: pospuesto

Por decisión tuya no se implementó en esta pasada. Queda anotado: el endpoint
`/registro-publico` es anónimo y acepta hasta 100MB de archivos por petición, así que hoy nada
impide llenar la tabla `queja_evidencias` con un script. Antes de que el portal se difunda
conviene retomarlo.

---

### Decisiones que quedaron abiertas

| # | Tema | Situación |
|---|---|---|
| P5 | ¿Boleta y número de empleado comparten campo y longitud? | Hoy ambos validan igual (1–10 dígitos). Si la boleta es siempre de 10 y el empleado tiene otra longitud, conviene validar según el tipo. |
| P7 | Longitud de la descripción | Se implementó **20–4000 caracteres**. Si no te acomoda, se cambia en una constante (`ReglasQueja.DESCRIPCION_*` y `reglas-queja.ts`). |
| — | Deduplicación de correos Gmail | No se implementó. `juan.perez@gmail.com` y `juanperez@gmail.com` son el mismo buzón, así que hoy se pueden registrar como quejosos distintos. |
| — | Registro autenticado (`/registrar`) | Las validaciones nuevas aplican solo al formulario público. La pantalla "Nueva queja" del panel sigue con las reglas viejas. Es otro caso de uso, aún sin validar. |

---

## Archivos tocados

### Nuevos

```
Backend/migracion_cu_q01_validaciones.sql
Backend/queja-service/.../validacion/ReglasQueja.java            (todas las reglas en un lugar)
Backend/queja-service/.../validacion/ValidacionException.java
Backend/queja-service/.../validacion/Textos.java
Backend/queja-service/.../validacion/ValidadorCorreo.java        (dos niveles)
Backend/queja-service/.../validacion/ValidadorFechas.java
Backend/queja-service/.../validacion/TipoArchivoDetectado.java
Backend/queja-service/.../validacion/DetectorTipoArchivo.java    (firma binaria)
Backend/queja-service/.../validacion/ValidadorArchivos.java
Backend/queja-service/.../validacion/ValidadorQuejaPublica.java  (orquestador)
Frontend/src/app/core/validaciones/reglas-queja.ts               (espejo del backend)
Frontend/src/app/shared/aviso-privacidad/{ts,html,scss}
docs/DESPLIEGUE-CU-Q01.md
```

### Modificados

```
Backend/queja-service/.../dto/RegistroQuejaPublicaRequest.java
Backend/queja-service/.../dto/EditarQuejaRequest.java
Backend/queja-service/.../entity/Queja.java
Backend/queja-service/.../entity/QuejaEvidencia.java
Backend/queja-service/.../service/QuejaService.java
Frontend/src/app/pages/registro-queja-publico/{ts,html,scss}
Frontend/src/app/core/models/queja.models.ts
Frontend/src/app/core/services/queja.service.ts
```

Los originales quedaron respaldados en `_backups/cu-q01-20260908/`.

---

## Verificación hecha antes de entregar

| Qué | Resultado |
|---|---|
| Compilación del frontend (`ng build`, AOT + type-check de plantillas) | ✅ Sin errores |
| Compilación del paquete `validacion` del backend (javac, Java 21) | ✅ Sin errores |
| Pruebas de las reglas de validación | ✅ **60 de 60** |
| Build completo de Maven | ⚠️ No se pudo correr aquí (sin acceso a Maven Central desde este entorno). **Confírmalo tú con `mvn clean package`.** |

Las 60 pruebas cubren: correo en ambos niveles (Gmail vs. otros dominios, puntos, longitudes),
nombres válidos e inválidos, boleta, límites exactos de fecha de nacimiento y de hechos,
detección de tipo por firma binaria (incluido un ejecutable renombrado a `.jpg`), límites de
cantidad y tamaño de la identificación, y el formulario completo campo por campo.

---

## Siguiente paso

Desplegar siguiendo `docs/DESPLIEGUE-CU-Q01.md` — **el orden importa: BD → Frontend → Backend**,
para que no haya ventana en la que el formulario deje de funcionar. Ese documento trae las 20
pruebas a repetir en el ambiente ya desplegado.


---

## CU-Q07 — Consultar mis quejas

**Actor:** Quejoso con sesión iniciada
**Pantallas:** `/panel/mis-quejas` y `/panel/mis-quejas/{folio}`
**Fecha de prueba:** 2026-09-09
**Estado:** 🔧 Cambios aplicados en código. Falta desplegar y reprobar.

---

### Tabla de hallazgos

| ID | Hallazgo | Capa | Severidad | Estado |
|---|---|---|---|---|
| H-07.1 | El panel mostraba "Recibida" a quejas rechazadas o turnadas | F | **Bloqueante** | 🔧 Corregido |
| H-07.2 | Cinco filtros para una lista de uno a cinco renglones | F | Menor | 🔧 Rediseñado |
| H-07.3 | Acciones como texto; faltaba eliminar | F | Menor | 🔧 Implementado |
| H-07.4 | No existía forma de retirar una queja | F+B | Mayor | 🔧 Implementado |
| H-07.5 | No se podían agregar ni quitar evidencias | F+B | Mayor | 🔧 Implementado |
| H-07.6 | No se podía ver lo que se había subido | F+B | Mayor | 🔧 Implementado |
| H-07.7 | El detalle no mostraba varios campos que el backend ya devolvía | F | Mayor | 🔧 Corregido |

---

### H-07.1 — El panel le mentía al quejoso sobre el estado de su trámite

El traductor de estatus del frontend conocía `RECIBIDA`, `EN_REVISION` y `FINALIZADA`. De esos
tres, **el backend solo produce el primero**: el flujo real es
`RECIBIDA → EN_VALIDACION → TURNADA`, o `→ RECHAZADA`.

Como el `switch` caía en `default: 'Recibida'`, una queja **rechazada seguía apareciendo como
"Recibida"** en el panel del quejoso. Es el peor tipo de error de los que se ven bien: la
pantalla no falla, informa mal.

Corregido con los códigos reales más `CANCELADA`, y se agregaron funciones (`claseEstatus`,
`esEditable`, `estaCerrada`) para que ninguna pantalla vuelva a comparar cadenas de estatus a
mano — que es exactamente como se desincronizó. Arrastró correcciones en `resumen` (contaba
"Finalizadas", un estatus que nunca existió) y en `consultar-queja`.

### H-07.2 — Cinco filtros para una lista de un renglón

Folio, asunto, unidad académica, fecha y estatus, encima de una tabla que en la captura tenía
**una sola queja**. Se reemplazaron por un buscador que cubre folio, asunto y escuela a la vez,
más pestañas Todas / En trámite / Cerradas con su conteo.

El de unidad académica se fue porque un quejoso se queja de su propia escuela; el de fecha
exacta, porque con menos de diez renglones no le ahorra trabajo a nadie. Ninguna información se
perdió: sigue toda en la tabla.

### H-07.3 / H-07.4 — Acciones con iconos y retiro de la queja

Ojo, lápiz y bote (lucide-angular, ver `docs/ICONOS.md`). Editar y eliminar se **deshabilitan**
cuando la queja salió de RECIBIDA, con el motivo en el `title` — un botón que desaparece no
explica por qué.

"Eliminar" **no borra**: marca la queja como `CANCELADA`. Decisión tomada tras exponer el
trade-off: destruir el registro elimina la constancia de que la queja existió, y si alguien la
presentó y luego la retiró bajo presión no quedaría ningún rastro. El diálogo se lo dice al
usuario con todas sus letras.

### H-07.5 / H-07.6 — Evidencias: agregar, quitar y ver

Tres endpoints nuevos, todos restringidos a estatus RECIBIDA. Dos detalles que importan:

- La evidencia se busca **dentro de la queja del usuario**, no por id suelto en la tabla: así
  nadie puede borrar la evidencia de otra persona mandando un id ajeno.
- **No se puede quitar la última identificación oficial.** La queja se quedaría sin con qué
  acreditar quién la presentó y el recepcionista tendría que rechazarla.

Para la previsualización hizo falta un endpoint que sirva el archivo. Como exige JWT, la
miniatura no se puede poner en un `<img src="...">` — una etiqueta `<img>` no manda cabeceras.
Se pide con HttpClient y se arma un object URL con el blob.

### H-07.7 — Campos que el backend ya devolvía y la pantalla no pintaba

Nombre completo del quejoso, correo, boleta o número de empleado, fecha de nacimiento,
**segundo apellido del denunciado**, datos del tutor, constancia del aviso de privacidad y el
**motivo del rechazo**. Este último ni siquiera estaba declarado en el modelo del frontend,
aunque el backend lo mandaba desde que existe el panel del recepcionista.

### Línea del tiempo: cuatro pasos, no seis

El ejemplo que se pidió tenía seis pasos genéricos. Se implementaron **cuatro**, que son los
que hoy se pueden comprobar con un dato real: Recibida → En validación → Turnada al área → En
atención, con RECHAZADA y CANCELADA como finales alternativos que cortan la línea.

Los seis se veían mejor, pero tres no tendrían de dónde encenderse y la barra se quedaría
clavada para siempre — peor que mostrar menos. Se amplía cuando primer contacto y
subdefensoría expongan su avance al quejoso.
