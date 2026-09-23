# Estados de la queja — referencia única

Alineado al diagrama de estados del 2026-09-18. **Este documento manda sobre el código**:
si algo no coincide, es el código el que está mal.

---

## 1. Tabla de estados

| Estado | Etapa | Tabla | Cómo se entra | Cómo se sale |
|---|---|---|---|---|
| `RECIBIDA` | Recepción | `quejas` | El quejoso manda la queja | El recepcionista abre el detalle · el quejoso cancela |
| `CANCELADA` | Recepción | `quejas` | El quejoso retira su queja | **Final** |
| `EN_VALIDACION` | Recepción | `quejas` | El recepcionista abre el detalle (desde `RECIBIDA` o `CORREGIDA`) | Checklist completo · falta algo |
| `RECHAZADA` | Recepción | `quejas` | Falta algo del checklist | El quejoso corrige |
| `CORREGIDA` | Recepción | `quejas` | El quejoso atiende las observaciones y reenvía | El recepcionista vuelve a validar |
| `TURNADA` | Recepción | `quejas` | Checklist completo | Pasa a Primer Contacto (automático) |
| `EN_ANALISIS` | Primer Contacto | `expedientes_primer_contacto` | Llega el expediente turnado | Dictamen del analista |
| `PROCEDENTE` | Primer Contacto | `expedientes_primer_contacto` | El analista dictamina competente | Pasa a Subdefensoría (automático) |
| `IMPROCEDENTE` | Primer Contacto | `expedientes_primer_contacto` | El analista dictamina improcedente | Se redacta la remisión |
| `PENDIENTE_REMISION` | Primer Contacto | `expedientes_primer_contacto` | Se redacta la remisión | Se envía |
| `REMITIDA` | Cierre | `expedientes_primer_contacto` | Se envía la remisión | **Final** |
| `RECIBIDO` | Subdefensoría | `expedientes_investigacion` | Llega el expediente procedente | Se elabora el oficio |
| `EN_INVESTIGACION` | Subdefensoría | `expedientes_investigacion` | Se elabora el oficio · **el quejoso rechaza el acuerdo** | Se envía el oficio |
| `EN_ESPERA_OFICIO` | Subdefensoría | `expedientes_investigacion` | Oficio enviado | El director responde |
| `ELABORO_ACUERDO` | Subdefensoría | `expedientes_investigacion` | El director responde | Se manda el acuerdo al quejoso |
| `PENDIENTE_CONCLUSION` | Subdefensoría | `expedientes_investigacion` | El acuerdo se envió al quejoso | El quejoso acepta o rechaza |
| `CONCLUIDO` | Cierre | `expedientes_investigacion` | El quejoso acepta el acuerdo | **Final** |

## 2. Lo que cambió el 2026-09-18

**Renombres**, para que el código diga lo que dice el diagrama:

| Antes | Ahora |
|---|---|
| `PENDIENTE_ANALISIS` | `EN_ANALISIS` |
| `TURNADO_SUBDEFENSORIA` | `PROCEDENTE` |
| `EN_GESTION_DIRECTOR` | `EN_ESPERA_OFICIO` |
| `LISTO_A_DICTAMINAR` | `ELABORO_ACUERDO` |

**El par de remisión estaba invertido.** El expediente pasaba a `REMITIDA` al **redactar** la
remisión y a `REMISION_ENVIADA` al enviarla — al revés de lo que cualquiera entendería.
Ahora: redactar → `PENDIENTE_REMISION`, enviar → `REMITIDA`.

**Dos estados nuevos, que son funcionalidad, no nombres:**

- **`CORREGIDA`.** Antes una queja rechazada era un callejón sin salida: no se podía editar
  (el estatus ya no era `RECIBIDA`) ni turnar (*"Una queja rechazada no puede ser turnada"*),
  así que quedaba muerta en la base. Ahora el quejoso atiende las observaciones, reenvía, y
  vuelve a la bandeja.
- **`PENDIENTE_CONCLUSION`.** Antes, Subdefensoría concluía el expediente sola: pasaba de
  `ELABORO_ACUERDO` a `CONCLUIDO` sin que el quejoso interviniera. Ahora el acuerdo se le
  manda y él decide. **Si acepta** → `CONCLUIDO`. **Si rechaza** → regresa a
  `EN_INVESTIGACION` para otra ronda.

`RECHAZADA` dejó de contar como cierre del trámite en el portal del quejoso, porque ya se
sale de ahí.

## 3. Endpoints nuevos

| Endpoint | Quién | Para qué |
|---|---|---|
| `PUT /api/quejoso/quejas/mias/{folio}/corregir` | Quejoso (JWT) | Reenviar una queja rechazada → `CORREGIDA` |
| `POST /api/subdefensoria/acuerdos-conclusion/interno/{id}/aceptar` | queja-service | El quejoso acepta → `CONCLUIDO` |
| `POST /api/subdefensoria/acuerdos-conclusion/interno/{id}/rechazar` | queja-service | El quejoso rechaza → `EN_INVESTIGACION` |

Los dos de `/interno` van sin JWT (mismo criterio que `/ingesta`): el quejoso trae un token
de quejoso que subdefensoria no reconoce, así que quien los invoca es queja-service en su
nombre. Quedan protegidos por la restricción de IP del firewall.

## 4. Pendiente

**El quejoso todavía no tiene pantalla para responder el acuerdo de conclusión.** El estado,
la máquina de estados y los endpoints ya existen y funcionan; falta el puente en
queja-service (que llame a esos dos endpoints) y la pantalla en el portal.

Mientras tanto, un expediente que llegue a `PENDIENTE_CONCLUSION` **se queda ahí**. Eso es
intencional: es preferible a cerrarlo sin preguntarle al quejoso, que es lo que hacía antes.

La decisión de diseño que falta tomar: ¿la respuesta al acuerdo de conclusión se modela como
los acuerdos de conciliación (tabla propia en queja-service, CU-QJ-12/13), o queja-service
solo hace de proxy hacia subdefensoria?

## 5. Divergencia conocida con el diagrama

El código tiene **dos vueltas de oficio** (`FaseOficio.SOLICITUD_INFORMACION` y
`GESTION_DIRECTOR`): primero se le pide información a la unidad académica, después se manda
un oficio formal al director. El diagrama muestra **una sola vuelta** y en su lugar permite
regresar desde `PENDIENTE_CONCLUSION`.

No se tocó porque son dos modelos distintos del proceso, no dos formas de decir lo mismo, y
la decisión es del área, no técnica. Es lo que hace que el HAPPY_PAD tenga 34 pasos y no 32.
