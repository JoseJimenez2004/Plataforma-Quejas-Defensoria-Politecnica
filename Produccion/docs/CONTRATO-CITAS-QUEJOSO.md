# Citas de Primer Contacto en el portal del quejoso

Primer Contacto ya expone lo necesario para que el quejoso **confirme o cancele** desde su
panel la cita que le agenda un analista. Falta solo la pantalla en el portal del quejoso
(`Produccion/Frontend`), que es de quien lleva ese módulo.

## Cómo funciona

1. El analista agenda (o reagenda) la cita. El quejoso recibe un aviso en su panel y un correo
   con la fecha, la hora y el plazo para responder (**48 horas**).
2. Dentro del plazo, el quejoso puede:
   - **Confirmar** su asistencia, o
   - **Cancelar**, escribiendo **obligatoriamente el motivo** (para que Primer Contacto le
     proponga otra fecha).
3. Si no responde en 48 horas, la cita pasa sola a **Sin respuesta**.
4. Si cancela o no responde, el analista decide si reagenda (vuelven a correr 48 horas) o
   cancela la cita.

## Endpoints

Pasan por el nginx como el resto de Primer Contacto (`/api/primer-contacto/...`). Requieren
el **mismo token del quejoso** que ya usa el portal (`Authorization: Bearer ...`). No hace
falta rol: el backend comprueba que la cita sea del correo del token.

| Método | Ruta | Cuerpo | Para qué |
|---|---|---|---|
| GET | `/api/primer-contacto/quejoso/citas/mias` | — | Citas del quejoso (todas sus quejas) |
| PUT | `/api/primer-contacto/quejoso/citas/{id}/confirmar` | — | Confirmar asistencia |
| PUT | `/api/primer-contacto/quejoso/citas/{id}/cancelar` | `{ "motivo": "texto" }` | Cancelar con motivo (obligatorio, máx. 1000 caracteres) |

### Respuesta (cada cita)

```json
{
  "id": 7,
  "folioQueja": "FOL-1A2B3C4D",
  "fechaCita": "2026-10-15",
  "horaCita": "10:30:00",
  "tipoCita": "PRESENCIAL",
  "motivo": "Entrevista inicial sobre los hechos",
  "estatus": "PROGRAMADA",
  "fechaLimiteRespuesta": "2026-10-10T09:12:44",
  "fechaRespuestaQuejoso": null,
  "motivoCancelacionQuejoso": null,
  "respuestaRegistradaPor": null,
  "analistaNombre": "Nombre del analista"
}
```

Trae también otros campos internos (`folio` PC-..., `expedienteId`...) que el portal puede
ignorar. `folioQueja` es el folio que conoce el quejoso.

### Estatus

| `estatus` | Qué mostrar | ¿Botones? |
|---|---|---|
| `PROGRAMADA` | "Pendiente de tu respuesta, tienes hasta *fechaLimiteRespuesta*" | Confirmar / Cancelar |
| `CONFIRMADA` | "Confirmada" | No |
| `CANCELADA_QUEJOSO` | "Cancelaste esta cita: *motivoCancelacionQuejoso*. Primer Contacto te propondrá otra fecha." | No |
| `SIN_RESPUESTA` | "El plazo para responder venció. Primer Contacto se comunicará contigo." | No |
| `CANCELADA` | "Cancelada por Primer Contacto" | No |

Mostrar los botones solo si `estatus` es `PROGRAMADA` y la hora actual es anterior a
`fechaLimiteRespuesta` (si viene `null`, es una cita anterior a este cambio y no vence).

### Errores

| Código | Cuándo | Mensaje (en el campo `error` de la respuesta) |
|---|---|---|
| 400 | Cancelar sin motivo | "Indica el motivo de la cancelación." |
| 404 | La cita no existe o es de otro quejoso | "Cita no encontrada" |
| 409 | Ya respondió, o venció el plazo | "Esta cita ya no está esperando tu respuesta." / "El plazo para responder esta cita ya venció..." |

## Dónde ponerlo en el portal

Sugerencia: en el detalle de la queja (`/panel/mis-quejas/:folio`), filtrando `mias` por
`folioQueja`, igual que la sección de conciliación. El aviso que recibe el quejoso ya enlaza a
esa misma página.
