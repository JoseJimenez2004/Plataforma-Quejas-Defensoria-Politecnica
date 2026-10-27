# quejas-service — Registro y seguimiento de quejas

Formulario público y autenticado de quejas, consulta por folio, edición, corrección, evidencias y acuerdos de conciliación del lado del quejoso.

Microservicio **Java 21 + Spring Boot 3 + Apache Maven** de la Plataforma de Quejas de la Defensoría de
los Derechos Politécnicos (TT 2026-B009). Se empaqueta como `target/quejas-service.jar`.

---

## 🧪 Estrategia de pruebas unitarias

Las pruebas unitarias evalúan de forma **aislada** la lógica que toma decisiones (servicios `@Service`,
validadores y utilerías):

1. **Aislamiento estricto (Mockito):** no se levanta el contexto de Spring (`@SpringBootTest`) ni se usa
   base de datos; repositorios, clientes HTTP y correo se simulan con `@Mock` / `@InjectMocks`.
   Todo el conjunto corre en segundos.
2. **Particiones de equivalencia:** las entradas se agrupan en clases válidas e inválidas.
3. **Valores de borde:** se prueba el límite exacto, justo debajo y justo arriba (por ejemplo 2 y 1
   caracteres de un nombre, 3 MB y 3 MB + 1 byte de un archivo).
4. **Patrón AAA (Arrange – Act – Assert)** y aserciones con mensaje, para evitar *Assertion Roulette*.
   Los límites se toman de constantes con nombre, no de números mágicos.

### Qué se prueba

| Clase de prueba | Pruebas | Requisito / caso de uso | Qué verifica |
|---|---:|---|---|
| `ValidadorQuejaPublicaTest` | 26 | CU-Q01 | Aviso de privacidad, nombres (2–50, letras con acentos, guion y apóstrofe), boleta de hasta 10 dígitos, tipo alumno/empleado, unidad y descripción mínima de 20 caracteres, con normalización. |
| `ValidadorCorreoTest` | 23 | CU-Q01 | Dos niveles: formato general para cualquier dominio y reglas de Gmail (6–30 caracteres) solo para gmail.com/googlemail.com; puntos al inicio, final o dobles; longitud máxima 254. |
| `ValidadorFechasTest` | 12 | CU-Q01 | Nacimiento entre 1920-01-01 y el 31/dic del año pasado; hechos no futuros ni antes de nacer. |
| `ValidadorArchivosTest` | 14 | CU-Q01, CU-Q07 | Credencial obligatoria (1–2 imágenes de hasta 3 MB, sin PDF) y evidencias de hasta 30 MB. |
| `DetectorTipoArchivoTest` | 11 | Seguridad | Tipo real por firma binaria: un .exe renombrado a .jpg no pasa. |
| `QuejaServiceTest` | 21 | CU-Q01, CU-Q02, CU-Q06, CU-Q07 | Consulta normalizando folio y correo, edición solo en RECIBIDA, corrección de RECHAZADA a CORREGIDA con avisos, cancelación sin borrar, no quitar la última identificación, evidencia ajena, folio FOL-XXXXXXXX y tutor de menores. |
| `ConciliacionServiceTest` | 5 | CU-Q08 | Solo el dueño responde, solo si está PENDIENTE y solo ACEPTADO o RECHAZADO. |
| **Total** | **112** | | |

### Qué no cubren las pruebas unitarias (y dónde se prueba)

NotificacionClienteService (RestTemplate hacia notificaciones-service) se simula; controladores: integración.

Las pruebas que necesitan base de datos u otros servicios encendidos se marcan con `@Tag("integracion")`
y **no** corren con `mvn test` (ver `docs/PLAN-PRUEBAS.md`, niveles 2 y 3).

---

## 🛠️ Comandos de Maven

```bash
# Pruebas unitarias + reporte de cobertura
mvn clean test

# Una sola clase de prueba
mvn test -Dtest=ValidadorQuejaPublicaTest

# Incluir también las pruebas de integración (requieren la BD de pruebas encendida)
mvn test -Dgrupos.excluidos=none

# Empaquetar sin correr pruebas (como en el despliegue)
mvn clean package -DskipTests
```

### Reporte de cobertura (JaCoCo)

Después de `mvn clean test`, abre `target/site/jacoco/index.html`. Se excluyen del conteo las clases sin
lógica (`*Application`, `config`, `dto`, `entity`, `model`). Para correr sin JaCoCo:
`mvn test -DsinCobertura`.

---

## 📘 Documentación interactiva OpenAPI 3 / Swagger UI

Con el servicio en marcha (`mvn spring-boot:run`, puerto **8084**):

* **Swagger UI:** `http://localhost:8084/swagger-ui.html`
* **Especificación OpenAPI (JSON):** `http://localhost:8084/v3/api-docs`

Los endpoints protegidos piden el token JWT: botón **Authorize** → `Bearer <token>`.

---

## 📊 Métricas de calidad

* **Cobertura de líneas y ramas (JaCoCo)** sobre la lógica de negocio.
* **Aislamiento de componentes:** sin base de datos ni red durante la fase unitaria.
* **Documentación viva:** Swagger se genera a partir de los controladores y DTO.
* Análisis estático con SonarLint/Kiuwan (ver `docs/PLAN-PRUEBAS.md`, EST-01).
