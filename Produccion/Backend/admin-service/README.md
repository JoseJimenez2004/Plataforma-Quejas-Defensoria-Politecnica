# admin-service — Administración y panel de la Defensora

Login del personal, cuentas y roles, plantillas oficiales, respaldos de la base de datos, bitácora de acciones críticas y el panel de supervisión de la Defensora.

Microservicio **Java 21 + Spring Boot 3 + Apache Maven** de la Plataforma de Quejas de la Defensoría de
los Derechos Politécnicos (TT 2026-B009). Se empaqueta como `target/admin-service.jar`.

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
| `PersonalAdministrativoServiceTest` | 19 | CU-A01 a CU-A04, RF-A09 | Login (cuenta desactivada, mismo mensaje para correo inexistente y contraseña incorrecta), alta con contraseña temporal cifrada, correo duplicado, reglas de contraseña, y que nunca se pueda dar de baja o quitar el rol a la última cuenta de administración ni a la propia. |
| `ServiciosAdministracionTest` | 17 | RF-A02, RF-A05, RF-A06, RF-A07, RNF-A02 | Bitácora con la IP real del cliente (X-Real-IP, X-Forwarded-For) y recorte a 250 caracteres; previsualización de plantillas; listado y descarga de respaldos con carpeta temporal (sin ejecutar pg_dump); el tablero no se cae si catalogo-service no responde. |
| **Total** | **36** | | |

### Qué no cubren las pruebas unitarias (y dónde se prueba)

SeguimientoQuejasService, PanelDefensoraService e IndicadoresQuejasService son SQL directo (JdbcTemplate): su lógica vive en las consultas, así que se prueban en integración contra una BD de prueba. RespaldoService.ejecutarRespaldoManual/restaurar llaman a pg_dump/psql (prueba de sistema FIA-03 del plan).

Las pruebas que necesitan base de datos u otros servicios encendidos se marcan con `@Tag("integracion")`
y **no** corren con `mvn test` (ver `docs/PLAN-PRUEBAS.md`, niveles 2 y 3).

---

## 🛠️ Comandos de Maven

```bash
# Pruebas unitarias + reporte de cobertura
mvn clean test

# Una sola clase de prueba
mvn test -Dtest=PersonalAdministrativoServiceTest

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

Con el servicio en marcha (`mvn spring-boot:run`, puerto **8087**):

* **Swagger UI:** `http://localhost:8087/swagger-ui.html`
* **Especificación OpenAPI (JSON):** `http://localhost:8087/v3/api-docs`

Los endpoints protegidos piden el token JWT: botón **Authorize** → `Bearer <token>`.

---

## 📊 Métricas de calidad

* **Cobertura de líneas y ramas (JaCoCo)** sobre la lógica de negocio.
* **Aislamiento de componentes:** sin base de datos ni red durante la fase unitaria.
* **Documentación viva:** Swagger se genera a partir de los controladores y DTO.
* Análisis estático con SonarLint/Kiuwan (ver `docs/PLAN-PRUEBAS.md`, EST-01).
