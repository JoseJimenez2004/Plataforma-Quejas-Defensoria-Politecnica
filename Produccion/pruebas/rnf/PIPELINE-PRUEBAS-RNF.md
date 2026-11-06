# Pipeline de pruebas de requerimientos no funcionales
**Plataforma de Quejas – Defensoría de los Derechos Politécnicos (TT 2026‑B009)**

| Versión | Fecha | Estado |
|---|---|---|
| 1.0 | 2026‑10‑28 | Borrador — complementa `claude/PLAN-PRUEBAS.md` §6 (pruebas no funcionales) |

Código: `Produccion/pruebas/rnf/` (scripts y configuración) y `Produccion/pruebas/rnf/ci/pruebas-rnf.yml` (workflow).

## 1. Pipeline

Los RNF de la Tabla 5.5 se verifican con un pipeline de GitHub Actions (`Produccion/pruebas/rnf/ci/pruebas-rnf.yml`, se instala en `.github/workflows/` de la raíz del repositorio). Cada característica de ISO/IEC 25010 es una etapa, y cada etapa usa el tipo de prueba que le corresponde (estática / dinámica, caja blanca / caja negra). Alcance acordado: **Funcionalidad, Mantenibilidad, Fiabilidad, Seguridad, Confidencialidad y Capacidad**; **Usabilidad queda por definir**.

```
push / PR a Produccion ──► 1 Mantenibilidad ─┐
                           2 Funcionalidad   ├──► 9 Informe por RNF (pestaña Summary)
                           3 Seguridad estát.┘          ▲
manual o lunes 03:00 ────► 4 Seguridad dinámica ────────┤
(entorno de pruebas)       5 Confidencialidad ──────────┤
                           6 Capacidad ─────────────────┤
                           7 Fiabilidad ────────────────┤
                           8 Usabilidad (informativa) ──┘
```

### 1.1 Mapa RNF → etapa → prueba

| Característica | RNF / RN | ID prueba | Qué se verifica | Tipo | Herramienta / archivo | Criterio de aceptación | Bloquea |
|---|---|---|---|---|---|---|---|
| **Mantenibilidad** | EST-01, guía de estilo (§2) | MAN-01 | Reglas obligatorias de la guía de estilo | Estática, CB | Checkstyle 10 · `estilo/checkstyle-defensoria.xml` | 0 violaciones severidad *error* | Sí |
| | EST-01 | MAN-02 | Complejidad ciclomática ≤ 15, NPath ≤ 400, método ≤ 80 líneas, ≤ 8 parámetros, fan‑out ≤ 30, archivo ≤ 800 líneas | Estática, CB | Checkstyle (métricas) | Se reporta como deuda técnica | No |
| | §5.1 | MAN-03 | Cobertura de líneas en `service`/`validacion`/`util` | Dinámica, CB | JaCoCo (`-Pcobertura`) · `mantenibilidad/cobertura.py` | ≥ 70 % por servicio | Sí |
| **Funcionalidad** | RF/RN | FUN-01 | Pruebas unitarias de reglas y servicios (PLAN‑PRUEBAS §5.1, UT‑01…12) | Dinámica, CB | JUnit 5 + Mockito, `mvn verify` | 100 % aprobadas | Sí |
| | — | FUN-02 | Build de producción de los fronts (presupuestos de tamaño) | Estática | `ng build --configuration production` | Compila sin errores | Sí |
| **Fiabilidad** | RNF-Q04 | FIA-05 | Notificaciones caído (sin conexión / 500) no tumba registro, cambio de estatus ni avisos | Dinámica, CB | `NotificacionClienteServiceResilienciaTest` (queja‑service) | Ninguna excepción se propaga | Sí |
| | RNF-R04 | FIA-06 | Falla el correo de rechazo y el rechazo se completa | Dinámica, CB | `NotificacionQuejaServiceResilienciaTest` (revision‑service) | Ninguna excepción se propaga | Sí |
| | RNF-A04 | FIA-07 | Importación de catálogo con filas mal formadas reporta resumen | Dinámica, CB | `DependenciaServiceTest.importarDesdeExcel_creaActualizaYReportaErroresSinFallarEnBloque` (ya existe) | Resumen creadas/actualizadas/errores | Sí |
| | RF-A06 | FIA-03 | Respaldo manual → descarga → restauración en base desechable | Dinámica, CN | `fiabilidad/respaldo-restaurable.sh` | 0 errores de `psql`; `quejas`, `personal_administrativo`, `dependencias` con filas | Sí |
| | — | FIA-01/02 | Detener un contenedor / reiniciar la VPS | Dinámica, CN | Manual (PLAN‑PRUEBAS §6.3) | El resto sigue operando | — |
| **Seguridad** | RNF-A06, RN-R02, RNF-R02, RNF-Q01 | SEG-07 | Matriz rol × endpoint **extraída del código** (`@PreAuthorize` + `permitAll`) contra las reglas | Estática, CB | `seguridad/extraer_endpoints.py` + `verificar_autorizacion.py` + `reglas-autorizacion.csv` | 0 diferencias | Sí |
| | SEG-01, RN-R02 | SEG-01 | La misma matriz en vivo: anónimo → 401/403; rol no permitido → 403; rol permitido → pasa el filtro | Dinámica, CN | `seguridad/matriz-roles.sh` (un token por rol) | 0 accesos indebidos | Sí |
| | RNF-Q06 | SEG-08 | Token con vigencia de 1 h (`exp − iat = 3600`) y token alterado rechazado | Dinámica, CN | `matriz-roles.sh` (extras) | 3600 s; 401/403 | Sí |
| | RNF-A03 | SEG-09 | Restaurar sin `"confirmar": true` es rechazado | Dinámica, CN | `matriz-roles.sh` (extras) | Respuesta 4xx | Sí |
| | — | SEG-03 | *Path traversal* en descarga de respaldos | Dinámica, CN | `matriz-roles.sh` (extras) | ≠ 200 | Sí |
| | SEG-05 | SEG-05 | Secretos (contraseñas, JWT, SMTP) en el repositorio | Estática | gitleaks | 0 hallazgos | Sí |
| | SEG-04 | SEG-04 | Dependencias Maven/npm con CVE altas o críticas con parche | Estática | Trivy (`fs`) | 0 | Sí |
| | SEG-02 | SEG-02 | Escaneo pasivo de vulnerabilidades web | Dinámica, CN | OWASP ZAP baseline | Sin alertas altas | No (reporte) |
| **Confidencialidad** | RNF-Q02 | CON-01 | HTTP redirige con 301 a HTTPS | Dinámica, CN | `confidencialidad/confidencialidad.sh` | 301 → https | Sí |
| | RNF-Q02 | CON-02/03 | TLS 1.2 y 1.3 aceptados; TLS 1.0 y 1.1 rechazados | Dinámica, CN | openssl `s_client` | Cumple | Sí |
| | RNF-Q02 | CON-04 | Certificado válido con ≥ 14 días de vigencia | Dinámica, CN | openssl | Cumple | Sí |
| | RNF-Q02 | CON-05 | Encabezado HSTS | Dinámica, CN | curl | Presente | Sí |
| | RNF-Q02 | CON-06 | Respuestas JSON sin compresión (mitigación BREACH) | Dinámica, CN | curl | Sin `Content-Encoding` | Sí |
| | RNF-Q02 | CON-07 | `/api/notificaciones/enviar` y `/registrar` bloqueados desde fuera | Dinámica, CN | curl | 403 | Sí |
| | RNF-Q02 | CON-08 | El login no usa cookies ni acepta credenciales en la URL; la sesión viaja como `Authorization: Bearer` | Dinámica, CN | curl | Sin `Set-Cookie`; query string rechazado | Sí |
| **Capacidad** | RNF-Q03 | CAP-01/02 | Petición multiparte de (límite − 5) MB pasa el proxy; (límite + 5) MB → 413 | Dinámica, CN | `capacidad/capacidad.sh` (formulario vacío a propósito: no crea quejas) | ≠ 413 / = 413 | Sí |
| | RNF-Q03 | CAP-03/04 | Lo mismo directo contra queja‑service | Dinámica, CN | `capacidad.sh` con `BACKEND_URL` | ≠ 413 / = 413 | Sí |
| | REN-01 | CAP-05 | 50 usuarios virtuales en endpoints públicos de lectura | Dinámica, CN | k6 · `capacidad/carga.k6.js` | p95 < 1 s; errores < 1 % | Sí |
| **Usabilidad** *(por definir)* | USA-03 | USA-03 | Accesibilidad de los portales | Dinámica, CN | Lighthouse CI · `usabilidad/lighthouserc.json` | ≥ 0.85 | No |
| | RNF-Q05, RNF-Q07, RNF-A05 | USA-05…07 | Propuesta: pasos numerados + previsualización de archivos; toda operación muestra éxito/error (sin fallos silenciosos); densidad de la consola | Dinámica, CN | Playwright (por escribir) | Por definir | — |
| | — | USA-01 | Cuestionario SUS tras pruebas Alfa | Manual | Formulario | ≥ 68 | — |

**Entornos.** Las etapas 1–3 no necesitan el sistema levantado (usan un Postgres 16 desechable en el *runner*). Las etapas 4, 6 y 7 se ejecutan **solo** contra `URL_PRUEBAS` con cuentas de prueba guardadas como *secrets*; la 5 es pasiva y puede apuntar a producción. Servicios que no mantiene el equipo (`primercontacto`, `subdefensoria`) se miden en la etapa 1 de forma informativa y no bloquean.

### 1.2 Primera ejecución (línea base 2026‑10‑28)

| Prueba | Resultado | Detalle |
|---|---|---|
| MAN-01 (servicios propios, 225 archivos) | ❌ 4 violaciones obligatorias | `RevisionQuejaService.java` L121 (dos enunciados en una línea), L656 (dos variables en una declaración), L756 (`if` sin llaves); `RespuestaDenunciadoService.java` L3 (`import static ...ReglasDenunciado.*`) |
| MAN-02 | Deuda: 117 tipos públicos sin Javadoc, 90 líneas > 120 col., 77 llaves en una línea (getters de DTO), 5 métodos con complejidad > 15 | Informativo |
| SEG-07 | ❌ 6 diferencias → ✅ resuelta | Las reglas de RNF-A06 (respaldos) y RN-R02 (acuerdos de conciliación) no coincidían con el código. Se alinearon los requerimientos con la convención acordada con la Defensoría: la administración corresponde a la Defensora (`DEFENSOR`) y la conciliación la consultan y emiten Recepción, Subdefensoría y la Defensora. Re-ejecución: 0 diferencias |
| SEG-05 | ❌ (por revisión manual) | Los `application.yaml` de los servicios y `Backend/config-files/` tienen contraseña de BD, `jwt.secret` y contraseña de aplicación de Gmail en texto plano, y la rama `Produccion` del repositorio pudo clonarse sin credenciales (parece público). Rotar esos secretos y pasarlos a variables de entorno |
| CON-05 | ❌ (por revisión de `defensoria.conf`) | nginx no envía `Strict-Transport-Security` |
| CAP (RNF-Q03) | ⚠️ Inconsistencia documental | RNF‑Q03 dice backend 60 MB; `queja-service`, `revision-service` y `denunciado-service` aceptan 100 MB (`max-request-size`) y `subdefensoria` 11 MB. Alinear la tabla 5.5 con la configuración |
| SEG-09 (RNF-A03) | ⚠️ Probable | Sin `confirmar` el controlador lanza `RuntimeException`; si no la mapea un `@ExceptionHandler`, responde 500 en vez de 400 |
| FIA-05/06 | Pendiente de primera corrida en CI | Pruebas nuevas, no se pudieron ejecutar en local (sin acceso a Maven Central) |

## 2. Guía de estilo de código Java (Coding Style)

El proyecto adopta como guía el **Google Java Style Guide** con cuatro desviaciones documentadas. Se eligió porque es la guía pública más completa y la que Checkstyle trae como configuración de referencia; las desviaciones reflejan lo que el código ya sigue de forma consistente, para no reformatear todo el proyecto sin aportar calidad. La verificación es automática (etapa MAN‑01): las reglas **obligatorias** rompen el pipeline; las **recomendadas** se reportan como deuda técnica.

**Desviaciones respecto a Google:**

| ID | Google pide | El proyecto usa | Motivo |
|---|---|---|---|
| D1 | Sangría de 2 espacios | 4 espacios | Convención de Spring/IntelliJ; ya la sigue ~96 % de los archivos |
| D2 | 100 columnas | 120 columnas | Con 100 col. habría ~880 líneas fuera en todo el backend; con 120 quedan 90 en los servicios propios |
| D3 | Paquetes sin `_` | Se permite `_` (`queja_service`) | Lo genera Spring Initializr a partir del `artifactId` con guion |
| D4 | Constantes `UPPER_SNAKE_CASE` | Se permite el logger `log` | Convención de SLF4J/Lombok |

**Reglas que se usan** (cumplimiento medido sobre los 8 servicios propios, 225 archivos):

| Sección Google | Regla | Módulo Checkstyle | Nivel | Violaciones |
|---|---|---|---|---|
| 2.1 | El archivo se llama como su clase de nivel superior | `OuterTypeFilename` | Obligatoria | 0 |
| 2.3.1 | Solo espacios, sin tabuladores | `FileTabCharacter` | Obligatoria | 0 |
| 3.3.1 | Sin imports con comodín (incluye `import static ...*`) | `AvoidStarImport` | Obligatoria | 1 |
| 3.3 | Sin imports sin usar | `UnusedImports` | Obligatoria | 0 |
| 3.4.1 | Una clase de nivel superior por archivo | `OneTopLevelClass` | Obligatoria | 0 |
| 4.1.1 | Llaves siempre, aunque el bloque sea de una línea | `NeedBraces` | Obligatoria | 1 |
| 4.1.2 | Llave de apertura al final de la línea (K&R) | `LeftCurly` | Recomendada | 77 (getters de una línea) |
| 4.2 | Sangría (D1) | `Indentation` | Recomendada | 13 |
| 4.3 | Un enunciado por línea | `OneStatementPerLine` | Obligatoria | 1 |
| 4.4 | Límite de columnas (D2) | `LineLength` | Recomendada | 90 |
| 4.6 | Espacios alrededor de operadores y palabras clave | `WhitespaceAround`, `WhitespaceAfter`, `ParenPad` | Recomendada | 0 |
| 4.8.2.1 | Una variable por declaración | `MultipleVariableDeclarations` | Obligatoria | 1 |
| 4.8.3.2 | Arreglos estilo Java (`String[] a`) | `ArrayTypeStyle` | Obligatoria | 0 |
| 4.8.4 | `switch` con `default` y sin caídas silenciosas | `MissingSwitchDefault`, `FallThrough` | Recomendada | 0 |
| 4.8.7 | Orden de modificadores del JLS | `ModifierOrder` | Obligatoria | 0 |
| 4.8.8 | `long` con `L` mayúscula | `UpperEll` | Obligatoria | 0 |
| 5.2.1 | Paquetes en minúsculas (D3) | `PackageName` | Obligatoria | 0 |
| 5.2.2 | Clases en `UpperCamelCase` | `TypeName` | Obligatoria | 0 |
| 5.2.3 | Métodos en `lowerCamelCase` | `MethodName` | Obligatoria | 0 |
| 5.2.4 | Constantes en `UPPER_SNAKE_CASE` (D4) | `ConstantName` | Recomendada | 0 |
| 5.2.5–5.2.7 | Campos, parámetros y variables en `lowerCamelCase` | `MemberName`, `ParameterName`, `LocalVariableName` | Recomendada | 0 |
| 6.1 | `@Override` siempre que aplique | `MissingOverride` | Recomendada | 0 |
| 6.2 | Las excepciones capturadas no se ignoran | `EmptyCatchBlock` | Obligatoria | 0 |
| 7.3 | Javadoc en tipos públicos | `MissingJavadocType` | Recomendada | 117 |
| — | Sin `System.out` ni `printStackTrace` (todo por SLF4J) | `RegexpSinglelineJava` | Obligatoria | 0 |
| — | `equals` y `hashCode` juntos; sin `==` entre `String` | `EqualsHashCode`, `StringLiteralEquality` | Obligatoria | 0 |

El cumplimiento global de las reglas obligatorias es de **4 violaciones en 225 archivos**; corregirlas deja la etapa MAN‑01 en verde.
