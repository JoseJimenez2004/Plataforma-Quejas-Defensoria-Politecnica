# denunciado-service — Versión del denunciado

Registro público de la respuesta de la persona señalada, con credencial y evidencias.

Microservicio **Java 21 + Spring Boot 3 + Apache Maven** de la Plataforma de Quejas de la Defensoría de
los Derechos Politécnicos (TT 2026-B009). Se empaqueta como `target/denunciado-service.jar`.

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
| `RespuestaDenunciadoServiceTest` | 29 | Solicitud al denunciado (Primer Contacto) | Formato de folio FOL-XXXXXXXX, nombres, unidad, boleta/número de empleado, descripción de 20 a 4000 caracteres, aviso de privacidad, credencial (1–2 imágenes de 3 MB), evidencias (máximo 10, 30 MB cada una, 95 MB en total), tipo real por firma binaria y limpieza del nombre del archivo. |
| **Total** | **29** | | |

### Qué no cubren las pruebas unitarias (y dónde se prueba)

Controladores y repositorio: integración.

Las pruebas que necesitan base de datos u otros servicios encendidos se marcan con `@Tag("integracion")`
y **no** corren con `mvn test` (ver `docs/PLAN-PRUEBAS.md`, niveles 2 y 3).

---

## 🛠️ Comandos de Maven

```bash
# Pruebas unitarias + reporte de cobertura
mvn clean test

# Una sola clase de prueba
mvn test -Dtest=RespuestaDenunciadoServiceTest

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

Con el servicio en marcha (`mvn spring-boot:run`, puerto **8094**):

* **Swagger UI:** `http://localhost:8094/swagger-ui.html`
* **Especificación OpenAPI (JSON):** `http://localhost:8094/v3/api-docs`

Los endpoints protegidos piden el token JWT: botón **Authorize** → `Bearer <token>`.

---

## 📊 Métricas de calidad

* **Cobertura de líneas y ramas (JaCoCo)** sobre la lógica de negocio.
* **Aislamiento de componentes:** sin base de datos ni red durante la fase unitaria.
* **Documentación viva:** Swagger se genera a partir de los controladores y DTO.
* Análisis estático con SonarLint/Kiuwan (ver `docs/PLAN-PRUEBAS.md`, EST-01).
