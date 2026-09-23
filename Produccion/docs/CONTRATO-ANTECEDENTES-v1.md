# Contrato de datos — Búsqueda de antecedentes (v1)

Formato que la Plataforma de Quejas de la Defensoría entrega al modelo de búsqueda de
antecedentes. Se entrega un arreglo de objetos con esta forma.

Fecha: 2026-09-17 · Alineado al esquema real del sistema (rename apellido1/apellido2).

---

## 1. Ejemplo — queja capturada en el sistema

```json
{
  "folio": "FOL-A1B2C3D4",
  "origen": "SISTEMA",
  "fuente": null,

  "fechaRegistro": "2026-09-16T11:20:00",
  "fechaHechos": "2026-09-01",

  "unidadAcademicaClave": "ESCOM",
  "motivo": "Queja en ESCOM",
  "descripcion": "Descripción completa de los hechos de la queja.",

  "estatus": "CONCLUIDA",
  "resultado": "CONCLUIDA_CON_ACUERDO",

  "quejoso": {
    "tipoIdentificacion": "BOLETA",
    "numeroIdentificacion": "2022630123",
    "nombre": "Juan",
    "apellido1": "Pérez",
    "apellido2": "García",
    "tipoUsuario": "ALUMNO"
  },

  "denunciado": {
    "tipoIdentificacion": null,
    "numeroIdentificacion": null,
    "nombre": "María",
    "apellido1": "López",
    "apellido2": "García",
    "tipoUsuario": "EMPLEADO"
  }
}
```

## 2. Ejemplo — queja histórica (capturada a mano desde archivos previos)

Misma forma. El `folio` es **el que traía el expediente original** (no se reasigna).
Cambia `origen`, se llena `fuente`, y varios campos pueden venir en `null` porque el
archivo de origen no los tenía.

```json
{
  "folio": "DDP/2019/0087",
  "origen": "HISTORICO",
  "fuente": "EXCEL",

  "fechaRegistro": "2019-04-02T00:00:00",
  "fechaHechos": "2019-03-14",

  "unidadAcademicaClave": "ESIME-ZAC",
  "motivo": "Presunta irregularidad en evaluación",
  "descripcion": "Texto capturado del expediente físico.",

  "estatus": "CONCLUIDA",
  "resultado": "SIN_DATO",

  "quejoso": {
    "tipoIdentificacion": "BOLETA",
    "numeroIdentificacion": "2016320456",
    "nombre": "Ana",
    "apellido1": "Ramírez",
    "apellido2": null,
    "tipoUsuario": "ALUMNO"
  },

  "denunciado": {
    "tipoIdentificacion": null,
    "numeroIdentificacion": null,
    "nombre": "Jorge",
    "apellido1": "Hernández",
    "apellido2": null,
    "tipoUsuario": null
  }
}
```

---

## 3. Catálogos de valores permitidos

| Campo | Valores |
|---|---|
| `origen` | `SISTEMA`, `HISTORICO` |
| `fuente` | solo si `origen=HISTORICO`: `EXCEL`, `LLAMADA`, `OFICIO_FISICO`, `SISTEMA_ANTERIOR`, `OTRO` |
| `tipoIdentificacion` | `BOLETA`, `EMPLEADO`, `CURP`, `INE`, `OTRO`, `null` |
| `tipoUsuario` | `ALUMNO`, `EMPLEADO`, `EXTERNO`, `null` — son los 3 valores que ofrece el sistema; docentes y administrativos caen ambos en `EMPLEADO` |
| `estatus` | `RECIBIDA`, `EN_VALIDACION`, `TURNADA`, `RECHAZADA`, `EN_ANALISIS`, `IMPROCEDENTE`, `REMITIDA`, `EN_INVESTIGACION`, `CONCLUIDA` |
| `resultado` | `RECHAZADA_EN_RECEPCION`, `IMPROCEDENTE`, `REMITIDA_A_OTRA_AUTORIDAD`, `CONCLUIDA_CON_ACUERDO`, `CONCLUIDA_SIN_ACUERDO`, `SIN_DATO`, `null` (null = sigue en trámite) |

## 4. Reglas

1. **`folio` es la llave.** Es único y estable entre sistemas. No hay `id` numérico en el
   contrato: un autoincremental no significa nada fuera de la base que lo generó.
   - Si `origen=SISTEMA`, es el folio que generó la plataforma (`FOL-` + 8 hexadecimales).
   - Si `origen=HISTORICO`, es **el folio que ya traía el expediente**, tal cual venía
     (`DDP/2019/0087`, `2019-087`, lo que sea). No se normaliza ni se reasigna: es el
     número con el que la Defensoría lo ha buscado siempre.
   - Excepción: si el registro histórico **no tenía folio** (una queja levantada por
     llamada, por ejemplo), el sistema genera uno con el prefijo `SF-` (sin folio) y lo
     marca así, para que se distinga de un folio real.
2. **La identidad de una persona es el par `(tipoIdentificacion, numeroIdentificacion)`**,
   nunca el número solo. La boleta `2022630123` y el empleado `2022630123` son personas
   distintas.
3. **Los apellidos van separados y sin etiqueta paterno/materno.** `apellido1` es el
   primero; `apellido2` puede ser `null` (gente con un solo apellido, o registros
   históricos donde el archivo de origen no lo traía). Si el modelo los necesita juntos,
   los concatena él; el sistema nunca los manda pegados.
4. **`resultado` es el campo que le da peso al antecedente.** Tres quejas previas
   concluidas con acuerdo significan lo contrario que tres rechazadas por improcedentes.
   `SIN_DATO` es un valor legítimo y frecuente en los históricos.
5. **`origen` le dice al modelo qué esperar.** Un registro `HISTORICO` trae menos campos
   por naturaleza; no debe penalizarse por eso.
6. Fechas: `fechaRegistro` en ISO-8601 con hora (`YYYY-MM-DDTHH:MM:SS`), `fechaHechos`
   solo fecha (`YYYY-MM-DD`). `fechaHechos` puede ser `null`.
7. **`fechaRegistro` es cuándo se presentó la queja ante la Defensoría, NO cuándo se
   capturó en el sistema.** En los históricos es la fecha del expediente original (2019,
   2021…), nunca la fecha en que el recepcionista lo tecleó. Si fuera la de captura, todos
   los históricos parecerían de 2026 y el orden temporal de los antecedentes quedaría
   inservible.

## 5. Pendiente de decidir — identificador del denunciado

Hoy el sistema **no guarda** número de boleta o de empleado del denunciado: solo nombre y
apellidos. Por eso `denunciado.tipoIdentificacion` y `denunciado.numeroIdentificacion`
van en `null` en todos los registros actuales.

Los campos ya están en el contrato para no tener que cambiarlo después. Si se decide
capturarlos, empezarán a llegar con valor sin modificar la estructura.

**El modelo no debe depender de ellos como entrada obligatoria.**
