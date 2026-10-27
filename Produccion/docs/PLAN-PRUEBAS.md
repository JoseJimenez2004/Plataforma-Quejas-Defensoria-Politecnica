# Plan de trabajo de pruebas
**Plataforma de Quejas – Defensoría de los Derechos Politécnicos (TT ESCOM‑IPN)**

| Versión | Fecha | Autor | Estado |
|---|---|---|---|
| 1.0 | 2026‑10‑19 | Equipo TT | Borrador para revisión del asesor |

> Basado en las presentaciones *Pruebas de software* (1_pruebas) y *Proceso de pruebas* (2_proceso_de_pruebas): modelo en V, niveles de prueba, atributos ISO 9126, principios de prueba y proceso fundamental (planificación → análisis y diseño → implementación y ejecución → evaluación e informe → cierre).

---

## 1. Objetivo

Comprobar, con evidencia documentada, que la plataforma cumple los requerimientos funcionales y no funcionales de `REQUERIMIENTOS.md`, y encontrar los defectos de mayor riesgo **antes** de la entrega y la demostración del TT.

## 2. Alcance

### 2.1 Qué se prueba (objetos de prueba)

| # | Componente | Tipo | Actor / rol |
|---|---|---|---|
| 1 | `queja-service` + Frontend‑Quejoso | Microservicio + front | Quejoso (público y con cuenta) |
| 2 | `auth-service` | Microservicio | Quejoso |
| 3 | `revision-service` + Frontend‑Revisión | Microservicio + front | RECEPCIONISTA |
| 4 | `primercontacto` (primer‑contacto‑service) + Frontend‑PrimerContacto | Microservicio + front | ANALISTA_PRIMER_CONTACTO |
| 5 | `subdefensoria` | Microservicio | SUBDEFENSOR |
| 6 | `admin-service` + Frontend‑Admin (panel de la Defensora y administración) | Microservicio + front | DEFENSOR, ADMIN_SISTEMAS |
| 7 | `antecedentes_service` / Modelo‑Java (buscador de antecedentes) | Componente de IA | Recepción, Primer Contacto |
| 8 | `resumen-service-final` (resúmenes) | Componente de IA | Primer Contacto |
| 9 | `chatbot-service` | Microservicio | Público |
| 10 | `catalogo-service`, `denunciado-service`, `historico-service`, `notificaciones-service` | Microservicios de apoyo | Varios |
| 11 | Infraestructura (Podman, nginx, Postgres, 3 VPS) | Despliegue | ADMIN_SISTEMAS |

### 2.2 Qué NO se prueba (fuera de alcance)

- Pruebas exhaustivas de todas las combinaciones de entrada (principio 2: no es viable).
- Pruebas unitarias de getters/setters, DTOs y CRUD generado por Spring Data.
- Rendimiento a gran escala (miles de usuarios concurrentes); solo se prueba la carga realista de la Defensoría.
- Librerías de terceros (Spring, Angular Material, Postgres) — se asumen probadas.
- Pruebas Beta con usuarios reales en producción durante un periodo largo.
- Pruebas en el **entorno de producción** con datos reales.

## 3. Riesgos y prioridad (pruebas basadas en riesgo)

| Prioridad | Riesgo | Impacto si falla | Se cubre en |
|---|---|---|---|
| **P1** | Acceso no autorizado: un rol entra a endpoints de otro, o un quejoso ve la queja de otro | Fuga de datos personales y sensibles | §5.2, §6.1 |
| **P1** | El ciclo de estatus de la queja se rompe (estado inválido, regreso a un estado anterior) | Quejas perdidas o mal tramitadas | §5.1, §5.2 |
| **P1** | Dos recepcionistas toman la misma queja | Trabajo duplicado, decisiones contradictorias | §5.2 |
| **P1** | Falla la integración entre servicios (Recepción → Primer Contacto → Subdefensoría) | El expediente se queda "atorado" | §5.2, §5.3 |
| **P2** | Validaciones de plazos legales y datos obligatorios incorrectas | Se admiten quejas fuera de plazo o se rechazan válidas | §5.1 |
| **P2** | La IA no responde (resumen, antecedentes) | El analista no puede trabajar | §6.3 |
| **P2** | Pérdida de datos (respaldo que no restaura) | Pérdida total de expedientes | §6.3 |
| **P3** | Lentitud en bandejas y tablero | Mala experiencia, abandono | §6.2 |
| **P3** | Pantallas confusas | Errores de captura | §6.4 |

## 4. Entorno de pruebas

> Principio: **no se prueba en producción** (presentación 2, "entorno de pruebas").

| Elemento | Configuración |
|---|---|
| Base de datos | Postgres 16 con una base dedicada `defensoria_test` (o el `dev-local` con Podman), cargada con los scripts `dev-local/init/01‑04` y la carga masiva de 170 quejas de prueba |
| Backend | Los mismos jars que se despliegan, con perfil `test` |
| Fronts | Build de producción (`ng build`) servido localmente |
| Datos | Ficticios. Cuentas de prueba, una por rol: quejoso, recepcionista ×2, analista, subdefensor, defensora, admin |
| Herramientas | JUnit 5, Mockito, Spring Boot Test + MockMvc, Testcontainers (opcional), Jasmine/Karma, Playwright, Postman/Newman o curl, k6, OWASP ZAP, SonarLint/SonarQube, Lighthouse |

## 5. Pruebas funcionales por nivel (modelo en V)

Leyenda de técnica: **CB** = caja blanca, **CN** = caja negra. Leyenda de automatización: **A** = automatizada, **M** = manual.

### 5.1 Nivel 1 – Pruebas de componente (unitarias) · CB · A

Solo sobre la lógica que **toma decisiones**.

| ID | Objeto de prueba | Qué se verifica | Técnica de diseño | Herramienta | Prior. |
|---|---|---|---|---|---|
| UT‑01 | `ReglasQueja` / validador de queja pública (`queja-service` y front) | Plazo máximo para presentar la queja (dentro, en el límite, fuera); boleta y nombre válidos/ inválidos; tutor obligatorio si es menor de edad | Valores límite, particiones de equivalencia | JUnit / Jasmine | P1 |
| UT‑02 | Máquina de estados de la queja (`revision-service`) | Transiciones permitidas `RECIBIDA → EN_VALIDACION → RECHAZADA/TURNADA`; transiciones prohibidas lanzan error (RN‑R01) | Tabla de transición de estados | JUnit + Mockito | P1 |
| UT‑03 | `RevisionQuejaService.tomarParaRevision` / `exigirQueSeaMia` | Una queja tomada por A no puede procesarla B | Caminos de decisión | JUnit + Mockito | P1 |
| UT‑04 | Rechazo de queja | Exige al menos un motivo (RN‑R03) | Particiones | JUnit | P1 |
| UT‑05 | Turnado | Exige área y defensor; el defensor solo puede ser DEFENSOR/SUBDEFENSOR activo (RN‑R04, RN‑R06) | Particiones | JUnit | P1 |
| UT‑06 | `CitaPrimerContactoService` | No se empalman dos citas a la misma hora; `normalizarCategoria` (valor nulo/desconocido → COMPARECENCIA); reagendar conserva la categoría; `citasEnRango` rechaza rangos mayores a ~400 días | Valores límite, tabla de decisión | JUnit + Mockito | P2 |
| UT‑07 | `DictamenPrimerContactoService` | Competente cambia a PROCEDENTE y arma la solicitud a Subdefensoría; improcedente exige justificación | Tabla de decisión | JUnit + Mockito | P1 |
| UT‑08 | `DiasHabilesCalculator` (Subdefensoría) | 10 días hábiles para primera solicitud, 5 para recordatorios; salta fines de semana | Valores límite | JUnit | P2 |
| UT‑09 | `PanelDefensoraService` – reglas de "Casos que requieren atención" | Cada regla (sin tomar >3 días, validación detenida >1 día, vencida >10 días, etc.) se activa justo al pasar el umbral; "rojo" al doble del plazo | Valores límite | JUnit (con BD de prueba) | P2 |
| UT‑10 | Motor de antecedentes (TF‑IDF / coseno, coincidencia de nombres) | Textos idénticos → similitud 1; sin palabras en común → 0; orden correcto de resultados | Particiones | JUnit | P2 |
| UT‑11 | Front Primer Contacto: `estadoHorario`, `esHabil`, `siguienteDiaHabil`, `etiquetaCategoria` | Horarios libres/ocupados/no disponibles; no permite fin de semana | Valores límite | Jasmine | P3 |
| UT‑12 | Front: guards de ruta y servicio de sesión | Sin token redirige a login; rol incorrecto no entra | Particiones | Jasmine | P2 |

**Criterio de cobertura:** ≥ 70 % de líneas en las clases de reglas y servicios listados (no en todo el proyecto), medido con JaCoCo.

### 5.2 Nivel 2 – Pruebas de integración · CN/CB · A

Estrategia: **ascendente (bottom‑up)**. Primero cada servicio con su base de datos real de prueba; después pares de servicios; los servicios externos que no se están probando se reemplazan por **stubs** (WireMock o un controlador falso).

#### 5.2.1 Integración servicio ↔ base de datos y seguridad (por microservicio)

| ID | Servicio | Qué se verifica | Herramienta | Prior. |
|---|---|---|---|---|
| IT‑01 | Todos | Arrancan con la BD de prueba (corrige el `contextLoads` actual que falla sin BD) | Spring Boot Test + perfil `test` / Testcontainers | P1 |
| IT‑02 | Todos los protegidos | Sin token → **401**; token de otro rol → **403**; token vencido → **401**; token correcto → **200** (RN‑R02) | MockMvc o Postman/Newman | P1 |
| IT‑03 | `primercontacto`, `subdefensoria` | **Verificar que ya exigen JWT** (en `REQUERIMIENTOS.md §5.2` se documentó `permitAll()` en todo, incluida `/h2-console`) | curl / Postman | P1 |
| IT‑04 | `queja-service` | Un quejoso autenticado no puede consultar ni editar la queja de otro cambiando el folio o el id en la URL | MockMvc | P1 |
| IT‑05 | `queja-service` | Editar una queja que ya no está en RECIBIDA es rechazado (CU‑Q07) | MockMvc | P2 |
| IT‑06 | `revision-service` | **Concurrencia:** dos recepcionistas hacen "tomar" sobre la misma queja al mismo tiempo → solo una lo logra | Prueba con 2 hilos / script con 2 peticiones simultáneas | P1 |
| IT‑07 | `admin-service` | Restaurar respaldo sin `{"confirmar": true}` es rechazado (RNF‑A03); toda acción crítica queda en bitácora (RNF‑A02) | MockMvc | P1 |
| IT‑08 | `admin-service` | Importación de catálogo Excel con filas erróneas devuelve resumen sin fallar completo (RNF‑A04) | MockMvc + archivo de prueba | P2 |
| IT‑09 | `admin-service` seguimiento | Endpoints del panel (`/tablero`, `/atencion`, `/quejas`, `/carga`, …) responden con datos consistentes con la BD sembrada | MockMvc / Postman | P2 |
| IT‑10 | Subida de archivos | Rechaza extensiones no permitidas y archivos que exceden el tamaño máximo | Postman | P1 |

#### 5.2.2 Integración entre servicios

| ID | Interfaz | Qué se verifica | Stub para | Prior. |
|---|---|---|---|---|
| IT‑20 | `auth-service` → `queja-service` (Feign) | Activar cuenta solo con folio + correo válidos (CU‑Q03) | — | P1 |
| IT‑21 | `revision-service` → `notificaciones-service` | Rechazar envía correo; si el correo falla, el rechazo **sí** se guarda (RNF‑R04) | Notificaciones caído | P1 |
| IT‑22 | Turnado → Primer Contacto | La queja turnada aparece en la bandeja de Primer Contacto | — | P1 |
| IT‑23 | `primercontacto` → `subdefensoria` | Dictamen competente crea el expediente en Subdefensoría; si Subdefensoría no responde, el dictamen no se pierde y se puede reintentar | Subdefensoría caído | P1 |
| IT‑24 | Primer Contacto / Subdefensoría → tabla `quejas` | El cambio de estatus se refleja para Recepción, la Defensora y el quejoso (hallazgo documentado en `REQUERIMIENTOS.md §5`) | — | P1 |
| IT‑25 | `primercontacto` → `resumen-service-final` | Resumen correcto; si no responde, se muestra el resumen de respaldo | Resumen caído | P2 |
| IT‑26 | `revision-service`/`primercontacto` → `antecedentes_service` | Antecedentes ordenados por similitud | — | P2 |
| IT‑27 | `catalogo-service` → formulario público y turnado | Dependencias activas disponibles | — | P3 |

### 5.3 Nivel 3 – Pruebas de sistema (extremo a extremo) · CN · A + M

En el entorno de pruebas completo, a través del navegador, con Playwright.

| ID | Flujo | Pasos resumidos | Resultado esperado | CU | Prior. |
|---|---|---|---|---|---|
| ST‑01 | Queja pública sin cuenta | Llenar formulario con evidencias → enviar | Folio generado; queja en RECIBIDA | CU‑Q01 | P1 |
| ST‑02 | Queja de menor de edad | Marcar menor → llenar tutor → enviar | Queja creada; notificación al tutor | CU‑Q01 | P2 |
| ST‑03 | Activar cuenta y consultar | Activar con folio + correo → login → "Mis quejas" | Ve solo sus quejas | CU‑Q03, Q04 | P1 |
| ST‑04 | Rechazo y corrección | Recepcionista rechaza con motivos → quejoso recibe correo y corrige → vuelve a Recepción | Estatus correctos en cada paso | CU‑R03, Q07 | P1 |
| ST‑05 | Turnado | Recepcionista revisa antecedentes → turna con área y defensor | Folio oficial; aparece en Primer Contacto | CU‑R04 | P1 |
| ST‑06 | Atención en Primer Contacto | Analista abre expediente (pestañas IA, Expediente, Gestión) → agenda cita → registra nota → solicita versión del denunciado | Cita sin empalmes; nota y solicitud guardadas | Primer Contacto | P1 |
| ST‑07 | Dictamen competente | Pestaña Resolución → Competente → elige abogado → confirma | Expediente PROCEDENTE; llega a Subdefensoría | Primer Contacto | P1 |
| ST‑08 | Dictamen improcedente | Improcedente con justificación | Queja cerrada como IMPROCEDENTE | Primer Contacto | P2 |
| ST‑09 | Supervisión de la Defensora | Panel general → periodo → "Casos que requieren atención" → abrir queja | Indicadores coinciden con los datos sembrados | Defensora | P2 |
| ST‑10 | Registro manual (papel) | Recepcionista captura queja manual | Origen MANUAL | CU‑R05 | P3 |
| ST‑11 | Exportar historial | Filtrar historial → exportar | `.xlsx` válido con el filtro | CU‑R06 | P3 |
| ST‑12 | Administración | Alta de usuario → restablecer contraseña → baja; respaldo manual | Todo en bitácora | CU‑A02‑A04, A07, A09 | P2 |
| ST‑13 | Acuerdo de conciliación | Recepción emite acuerdo → quejoso lo acepta | Estado ACEPTADO con fecha | CU‑R07, Q08 | P3 |
| ST‑14 | Recuperar contraseña | Solicitar código → cambiar contraseña | Acceso con la nueva | CU‑Q05 | P3 |

### 5.4 Nivel 4 – Pruebas de aceptación (Alfa) · CN · M

- **Quiénes:** 3 a 5 personas (personal de la Defensoría, o en su defecto el asesor y compañeros que actúen cada rol).
- **Dónde:** entorno de pruebas, bajo nuestra supervisión (pruebas **Alfa**).
- **Cómo:** guion de tareas por rol tomado de los casos de uso (CU‑Q, CU‑R, CU‑A, Primer Contacto, Defensora). Cada participante marca "cumple / no cumple / observación".
- **Evidencia:** formato de aceptación firmado y lista de observaciones.

## 6. Pruebas no funcionales (ISO 9126)

### 6.1 Funcionalidad – Seguridad

| ID | Prueba | Herramienta | Criterio | Prior. |
|---|---|---|---|---|
| SEG‑01 | Matriz rol × endpoint (todas las rutas con cada rol) | Postman/Newman | 0 accesos indebidos | P1 |
| SEG‑02 | Escaneo de vulnerabilidades web (baseline) sobre los 4 fronts | OWASP ZAP | Sin alertas altas | P1 |
| SEG‑03 | Inyección SQL / XSS en buscadores y formularios | ZAP + pruebas manuales | No se ejecuta ni se filtra | P1 |
| SEG‑04 | Dependencias vulnerables | `npm audit`, OWASP Dependency‑Check | Sin críticas sin justificar | P2 |
| SEG‑05 | Contraseñas almacenadas con hash; secreto JWT fuera del código | Revisión de código / BD | Cumple | P1 |
| SEG‑06 | Puertos expuestos en las VPS y HTTPS | nmap / revisión de firewall | Solo los necesarios | P2 |

### 6.2 Eficiencia (rendimiento)

| ID | Prueba | Herramienta | Criterio de aceptación | Prior. |
|---|---|---|---|---|
| REN‑01 | Carga sobre registro de queja, bandeja de Recepción y tablero de la Defensora con 20‑50 usuarios virtuales | k6 / `diagnostico/medir-carga.sh` | p95 < 1 s; 0 % errores | P2 |
| REN‑02 | Carga masiva de 170 quejas | `Backend/carga-masiva` | Todas registradas sin error | P2 |
| REN‑03 | Tiempo de carga de los fronts | Lighthouse | Performance ≥ 80 | P3 |
| REN‑04 | Tiempo de respuesta de la IA (resumen y antecedentes) | k6 / medición | Definir umbral (ej. < 5 s) | P2 |
| REN‑05 | Consumo de memoria/CPU de los contenedores bajo carga | `podman stats` / `revisar-servidor.sh` | Sin agotar la VPS | P3 |

### 6.3 Fiabilidad

| ID | Prueba | Criterio | Prior. |
|---|---|---|---|
| FIA‑01 | Detener un contenedor (p. ej. `resumen-service`) mientras se usa el sistema | El resto sigue funcionando; mensaje claro al usuario | P2 |
| FIA‑02 | Reiniciar la VPS backend | Los contenedores se levantan solos | P2 |
| FIA‑03 | Respaldo y **restauración** en una base limpia | Los datos quedan idénticos | P1 |
| FIA‑04 | Subdefensoría caída al dictaminar | El dictamen no se pierde; se puede reintentar | P1 |

### 6.4 Usabilidad

| ID | Prueba | Criterio | Prior. |
|---|---|---|---|
| USA‑01 | Cuestionario **SUS** a 5 usuarios después de las pruebas Alfa | Puntaje ≥ 68 | P2 |
| USA‑02 | Tareas cronometradas (presentar queja, turnar, agendar cita, dictaminar) | Tiempo y errores por tarea | P3 |
| USA‑03 | Accesibilidad básica (contraste, etiquetas, teclado) | Lighthouse Accessibility ≥ 85 | P3 |
| USA‑04 | Diseño responsivo del portal del quejoso en celular | Se puede presentar la queja completa | P2 |

### 6.5 Mantenibilidad (pruebas estáticas)

| ID | Prueba | Herramienta | Criterio | Prior. |
|---|---|---|---|---|
| EST‑01 | Análisis estático de todos los microservicios | SonarQube / SonarLint | Sin bugs ni vulnerabilidades "blocker/critical"; reporte de deuda técnica y duplicación | P2 |
| EST‑02 | Análisis de los fronts | ESLint + `ng build` sin advertencias de presupuesto | Cumple | P3 |
| EST‑03 | Revisión de código entre pares con lista de verificación | Manual | Checklist llenado por módulo | P3 |
| EST‑04 | Revisión de documentos (requerimientos vs. implementación) | Manual | Matriz de trazabilidad completa | P2 |

### 6.6 Portabilidad

| ID | Prueba | Criterio | Prior. |
|---|---|---|---|
| POR‑01 | Instalar el sistema en una VPS limpia siguiendo **solo** el Manual de Instalación | Sistema funcionando sin pasos no documentados | P2 |
| POR‑02 | Navegadores: Chrome, Firefox, Edge y Safari/celular | Flujos ST‑01, ST‑05, ST‑07 funcionan | P3 |

### 6.7 Calidad del componente de IA

| ID | Prueba | Método | Criterio | Prior. |
|---|---|---|---|---|
| IA‑01 | Precisión del buscador de antecedentes | Conjunto etiquetado de quejas (170 de carga masiva) → precisión en los 5 primeros (P@5) | Definir meta (ej. P@5 ≥ 0.6) | P2 |
| IA‑02 | Calidad de resúmenes | Rúbrica 1‑5 (fidelidad, completitud, claridad) calificada por 2 personas sobre 20 quejas | Promedio ≥ 4 | P2 |
| IA‑03 | Robustez | Quejas muy cortas, muy largas, con faltas de ortografía (`Modelo-Java/pruebas/*.json`) | No falla; responde algo útil | P3 |

## 7. Pruebas de regresión

- Suite mínima automatizada: **IT‑02, IT‑06, ST‑01, ST‑05, ST‑07** + todas las unitarias.
- Se corre **antes de cada despliegue** a las VPS y después de corregir cualquier defecto (**re‑test** del caso que falló + regresión).
- Comandos objetivo:
  - Backend: `mvn test` en cada servicio (con perfil `test`).
  - Fronts: `ng test --watch=false`.
  - Sistema: `npx playwright test`.

## 8. Formato de caso de prueba

Cada caso de las tablas anteriores se documenta así (campos de la presentación 2):

| Campo | Ejemplo |
|---|---|
| **ID** | IT‑06 |
| **Requisito / CU** | CU‑R03, CU‑R04, RN‑R01 |
| **Objetivo** | Dos recepcionistas no pueden procesar la misma queja |
| **Prioridad** | P1 |
| **Precondiciones** | Queja `FOL-XXXX` en RECIBIDA; cuentas recep1 y recep2 activas |
| **Dependencias** | IT‑02 aprobado |
| **Datos de entrada** | Dos `POST /tomar` simultáneos sobre el mismo folio |
| **Procedimiento** | 1. Obtener token de ambas cuentas. 2. Lanzar ambas peticiones en paralelo. 3. Consultar la queja. |
| **Resultado esperado (oráculo)** | Una petición 200, la otra 409; la queja queda asignada a una sola cuenta |
| **Resultado obtenido** | (se llena al ejecutar) |
| **Estado** | Aprobado / Fallido / Bloqueado |
| **Evidencia** | Captura, log o reporte |
| **Fecha y ejecutor** | |

Los defectos se registran con: ID, caso que lo detectó, severidad (crítica, alta, media, baja), pasos para reproducirlo, estado (abierto, corregido, verificado, cerrado).

## 9. Matriz de trazabilidad (resumen)

| Requisito / CU | Unitarias | Integración | Sistema | No funcional |
|---|---|---|---|---|
| CU‑Q01 Presentar queja | UT‑01 | IT‑10 | ST‑01, ST‑02 | REN‑01, USA‑04 |
| CU‑Q03/Q04 Cuenta y login | UT‑12 | IT‑02, IT‑20 | ST‑03 | SEG‑01 |
| CU‑Q07 Editar queja | UT‑02 | IT‑04, IT‑05 | ST‑04 | SEG‑01 |
| CU‑R03 Rechazar | UT‑02, UT‑04 | IT‑06, IT‑21 | ST‑04 | — |
| CU‑R04 Turnar | UT‑02, UT‑05 | IT‑06, IT‑22, IT‑26 | ST‑05 | — |
| CU‑R05/R06 Manual e historial | — | — | ST‑10, ST‑11 | — |
| CU‑R07/Q08 Conciliación | — | — | ST‑13 | — |
| Primer Contacto: citas | UT‑06, UT‑11 | IT‑03 | ST‑06 | — |
| Primer Contacto: dictamen | UT‑07 | IT‑23, IT‑24, IT‑25 | ST‑07, ST‑08 | FIA‑04 |
| Subdefensoría: plazos | UT‑08 | IT‑03, IT‑23 | — | — |
| Panel de la Defensora | UT‑09 | IT‑09 | ST‑09 | REN‑01 |
| CU‑A02‑A04, A09 Usuarios y bitácora | — | IT‑07 | ST‑12 | SEG‑05 |
| CU‑A05 Catálogo Excel | — | IT‑08 | — | — |
| CU‑A07/A08 Respaldos | — | IT‑07 | ST‑12 | FIA‑03 |
| Antecedentes / Resumen (IA) | UT‑10 | IT‑25, IT‑26 | ST‑06 | IA‑01‑03, REN‑04 |

## 10. Criterios de entrada, salida y suspensión

**Entrada** (para empezar a ejecutar):
- Entorno de pruebas levantado y con datos sembrados.
- Build compilado sin errores (`mvn clean package`, `ng build`).

**Salida** (las pruebas se dan por terminadas):
- 100 % de los casos **P1** ejecutados y aprobados.
- ≥ 90 % de los casos **P2** ejecutados.
- 0 defectos críticos o altos abiertos.
- Cobertura ≥ 70 % en las clases de reglas (§5.1).
- p95 < 1 s en REN‑01; SUS ≥ 68.

**Suspensión:** si el entorno no está disponible o un defecto crítico bloquea más del 30 % de los casos, se detiene la ejecución hasta corregirlo.

## 11. Cronograma (proceso fundamental de pruebas)

| Semana | Fase | Actividades | Entregable |
|---|---|---|---|
| 1 | Planificación y control | Aprobar este plan; preparar entorno `defensoria_test` y cuentas por rol | Plan aprobado, entorno listo |
| 1‑2 | Análisis y diseño | Escribir los casos detallados (formato §8) y la matriz de trazabilidad | Matriz de casos |
| 2‑3 | Implementación (componente + integración) | Corregir `contextLoads`; unitarias UT‑01…12; pruebas de API IT‑01…27 | Suites `mvn test` / Newman |
| 3‑4 | Implementación (sistema + no funcionales) | Playwright ST‑01…14; k6; ZAP; SonarQube | Suites Playwright y k6, reportes |
| 4 | Ejecución y re‑test | Ejecutar todo, registrar defectos, corregir, volver a probar, regresión | Bitácora de ejecución y de defectos |
| 5 | Aceptación | Sesión Alfa con usuarios + SUS | Formatos de aceptación |
| 5 | Evaluación, informe y cierre | Métricas, conclusiones y lecciones aprendidas | Informe de pruebas (capítulo del TT) |

## 12. Entregables

1. Este plan de pruebas.
2. Matriz de casos de prueba con trazabilidad.
3. Código de pruebas: `src/test/java` por servicio, `*.spec.ts`, carpeta `pruebas/e2e` (Playwright), `pruebas/carga` (k6), colección Postman.
4. Bitácora de ejecución y registro de defectos.
5. Reportes: JaCoCo, SonarQube, ZAP, k6, Lighthouse.
6. Formatos de aceptación y resultados SUS.
7. **Informe final de pruebas:**
   - casos planeados, ejecutados y aprobados;
   - defectos por módulo y severidad;
   - cobertura de requisitos;
   - conclusiones.

## 13. Estado actual (línea base)

| Aspecto | Situación al 2026‑10‑19 |
|---|---|
| Pruebas unitarias backend | 1 (`PrimercontactoApplicationTests.contextLoads`), falla sin BD |
| Pruebas front | 18 `*.spec.ts` generados por Angular, sin pruebas reales |
| Pruebas manuales ya hechas (por documentar) | Concurrencia en Recepción, carga masiva de 170 quejas, medición de carga, flujos de Primer Contacto verificados con Playwright |
| Hallazgos conocidos a verificar | Seguridad `permitAll` en primercontacto/subdefensoria (IT‑03); sincronización de estatus con la tabla `quejas` (IT‑24) |
