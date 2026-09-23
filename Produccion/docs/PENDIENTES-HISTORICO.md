# Pendientes del módulo histórico

Nota de trabajo. **Nada de esto está todavía en el catálogo de casos de uso**
(`Casos de Uso Defensoria.xlsx`), por decisión explícita: agregarlos obliga a renumerar
o a romper el agrupamiento por departamento, y eso se decide más adelante.

Fecha: 2026-09-17

---

## 1. Casos de uso por agregar (4 nuevos, actor Recepción)

| Provisional | Nombre | Endpoint que ya existe |
|---|---|---|
| — | Capturar una queja de años anteriores | `POST /api/historico/quejas` |
| — | Consultar el archivo histórico | `GET /api/historico/quejas` (paginado + filtros) |
| — | Consultar detalle de una queja histórica | `GET /api/historico/quejas/{folio}` |
| — | Adjuntar documento digitalizado a una queja histórica | `POST /api/historico/quejas/{folio}/evidencias` |

El backend de los cuatro **ya está escrito** en `historico-service`. Lo que falta es la
pantalla en `Frontend-Revision` y darlos de alta en el catálogo.

## 2. Caso de uso existente que CAMBIA

**CU-R20 — Consultar antecedentes del quejoso.** Hoy consulta solo `defensoria_db`. Debe
consultar además `GET /api/historico/interno/antecedentes` en `historico-service` y unir
ambos resultados ordenados por fecha. No es un CU nuevo: es el mismo con alcance ampliado,
y hay que actualizar su descripción.

## 3. La decisión de numeración, pendiente

Meter 4 CU de Recepción en su bloque (CU-R32 a CU-R35) recorre **todos los IDs
posteriores**: Administración pasaría a empezar en CU-A36, Primer Contacto en CU-PC59,
Subdefensoría en CU-SD79, y el catálogo terminaría en 93 en vez de 89. Son unos 58 IDs
que cambian, y el HAPPY_PAD se reescribe completo. Sería la segunda renumeración.

La alternativa es agregarlos al final (CU-R90 a CU-R93): ningún ID existente se mueve,
pero el catálogo deja de estar agrupado visualmente por departamento.

**Sin decidir.**

## 4. Otros pendientes técnicos del módulo

- Abrir el puerto **8092** en el firewall de Hostinger, restringido a `2.25.64.47` y a la
  propia VPS de backend, igual que 8082-8091.
- `historico-service` **no se ha compilado**: no hay Maven en el equipo de desarrollo y
  Maven Central está bloqueado desde el entorno donde se escribió. Primer `mvn clean
  package` pendiente.
- Definir qué hace el recepcionista cuando captura un folio duplicado. Hoy el servicio
  responde con un mensaje sugiriendo un sufijo (`DDP/2019/0087-B`), pero no hay flujo de
  pantalla para eso.
- Los tres campos de identificación del denunciado (`tipoIdentificacion`,
  `numeroIdentificacion`, `tipoUsuario`) existen en `quejas_historicas` pero **no** en la
  tabla `quejas` de `defensoria_db`. Mientras no se agreguen ahí, las quejas del sistema
  los entregarán siempre en `null` aunque el contrato ya los contemple.
- `estatus_global` y `resultado` en `quejas` (defensoria_db) siguen sin existir. Sin ellas,
  armar el campo `resultado` del contrato para una queja del sistema obliga a recorrer 4
  tablas de 3 microservicios.
