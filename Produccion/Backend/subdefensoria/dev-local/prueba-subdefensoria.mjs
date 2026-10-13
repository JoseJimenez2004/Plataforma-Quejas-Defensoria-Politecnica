// Prueba E2E LOCAL de Subdefensoría (CU-SD-01..09), de punta a punta desde Primer Contacto,
// con los estados de la tabla ECA del diagrama de la queja (filas 9 a 20) y su reflejo en
// quejas.estatus.
//
// Requisitos:
//   - base local de primercontacto/dev-local arriba (docker compose, puerto 5433);
//   - primercontacto en 8082 (su subdefensoria.base-url por defecto apunta a 8083);
//   - subdefensoria en 8083 con DB_PASSWORD=defensoria_local y el JWT_SECRET local.
//
//   node dev-local/prueba-subdefensoria.mjs
//
// Crea su propia queja FOL-SDP-... y borra las de corridas anteriores. Solo para desarrollo.
import crypto from 'node:crypto';
import fs from 'node:fs';
import { execSync } from 'node:child_process';

const PC = 'http://localhost:8082/api/primer-contacto';
const SD = 'http://localhost:8083/api/subdefensoria';
const SECRETO = 'gKxw6SOEVL3NQQzDdXk22-UPYsUnafSyQx0eBbgVrN4fqnrh';
// Archivo jsonl donde un receptor falso en 8085 guarda avisos y correos (opcional).
const NOTIFICACIONES = process.env.NOTIFICACIONES;

function token(sub, rol) {
  const b64 = o => Buffer.from(JSON.stringify(o)).toString('base64url');
  const ahora = Math.floor(Date.now() / 1000);
  const cuerpo = { sub, iat: ahora, exp: ahora + 3600 };
  if (rol) cuerpo.rol = rol;
  const h = b64({ alg: 'HS256', typ: 'JWT' }), c = b64(cuerpo);
  const f = crypto.createHmac('sha256', Buffer.from(SECRETO, 'utf8')).update(`${h}.${c}`).digest('base64url');
  return `${h}.${c}.${f}`;
}

const ANALISTA = token('analista.pc@ipn.mx', 'ANALISTA_PRIMER_CONTACTO');
const SUBDEFENSOR = token('subdefensor@ipn.mx', 'SUBDEFENSOR');

async function llamar(base, metodo, ruta, { tk, body } = {}) {
  const r = await fetch(base + ruta, {
    method: metodo,
    headers: { 'Content-Type': 'application/json', ...(tk ? { Authorization: `Bearer ${tk}` } : {}) },
    body: body ? JSON.stringify(body) : undefined
  });
  const texto = await r.text();
  let json; try { json = texto ? JSON.parse(texto) : null; } catch { json = texto; }
  return { status: r.status, json };
}
const pc = (m, ruta, o = {}) => llamar(PC, m, ruta, { tk: ANALISTA, ...o });
const sd = (m, ruta, o = {}) => llamar(SD, m, ruta, { tk: SUBDEFENSOR, ...o });

const sql = q => execSync(`docker exec -i defensoria-db-local psql -U postgres -d defensoria_db -tA`, { input: q }).toString().trim();

let ok = 0, fallas = 0;
function check(nombre, cond, detalle) {
  if (cond) { ok++; console.log(`  OK   ${nombre}`); }
  else { fallas++; console.log(`  FALLA ${nombre}`, detalle !== undefined ? JSON.stringify(detalle).slice(0, 400) : ''); }
}

// ------------------------------------------------------------------ Preparación
sql(`DELETE FROM respuestas_externas WHERE expediente_id IN (SELECT id FROM expedientes_investigacion WHERE folio_origen LIKE 'FOL-SDP-%');
     DELETE FROM recordatorios_urgencia WHERE oficio_id IN (SELECT o.id FROM oficios_informacion o JOIN expedientes_investigacion e ON e.id = o.expediente_id WHERE e.folio_origen LIKE 'FOL-SDP-%');
     DELETE FROM oficios_informacion WHERE expediente_id IN (SELECT id FROM expedientes_investigacion WHERE folio_origen LIKE 'FOL-SDP-%');
     DELETE FROM acuerdos_conclusion WHERE expediente_id IN (SELECT id FROM expedientes_investigacion WHERE folio_origen LIKE 'FOL-SDP-%');
     DELETE FROM expedientes_investigacion WHERE folio_origen LIKE 'FOL-SDP-%';
     DELETE FROM dictamenes_primer_contacto WHERE expediente_id IN (SELECT id FROM expedientes_primer_contacto WHERE folio_origen LIKE 'FOL-SDP-%');
     DELETE FROM expedientes_primer_contacto WHERE folio_origen LIKE 'FOL-SDP-%';
     DELETE FROM quejas WHERE numero_folio LIKE 'FOL-SDP-%';`);

const FOLIO_Q = 'FOL-SDP-' + Date.now().toString().slice(-6);
sql(`INSERT INTO quejas (id, numero_folio, correo_institucional, motivo, descripcion, fecha_creacion,
       nombre_quejoso, apellido1_quejoso, unidad_academica_clave, origen_registro, estatus)
     VALUES ((SELECT coalesce(max(id),0)+1 FROM quejas), '${FOLIO_Q}', 'quejoso1@alumno.ipn.mx',
       'Trato indebido de un docente', 'El docente me trató de forma indebida frente al grupo.',
       now(), 'Ana', 'López', 'ESCOM', 'AUTENTICADO', 'TURNADA');`);

const ing = await llamar(PC, 'POST', '/ingesta/expedientes', { body: {
  folioOrigen: FOLIO_Q, tema: 'Trato indebido de un docente',
  descripcionHechos: 'El docente me trató de forma indebida frente al grupo.',
  fechaRecepcion: new Date().toISOString().slice(0, 19),
  quejoso: { nombreCompleto: 'Ana López', correo: 'quejoso1@alumno.ipn.mx', unidadAcademica: 'ESCOM' }
}});
if (ing.status !== 201) throw new Error('Ingesta de Primer Contacto falló: ' + JSON.stringify(ing));
const PC_FOLIO = ing.json.folio;
console.log(`Expediente de Primer Contacto: ${PC_FOLIO} (queja ${FOLIO_Q})`);

// ------------------------------------------------------------------ CU-SD-01
console.log('\n== CU-SD-01 Recibir expediente desde Primer Contacto (filas 10 y 12)');
const estatusQueja = () => sql(`SELECT estatus FROM quejas WHERE numero_folio = '${FOLIO_Q}'`);

let r = await pc('POST', `/expedientes/folio/${PC_FOLIO}/iniciar-analisis`);
check('Abrir en Primer Contacto → la queja pasa a EN_ANALISIS (fila 9)', estatusQueja() === 'EN_ANALISIS', estatusQueja());

r = await pc('POST', '/dictamenes/competente', { body: {
  folio: PC_FOLIO, justificacion: 'Hechos dentro de la competencia de la Defensoría.',
  areaTurno: 'Subdefensoría', responsableTurno: 'Subdefensor en turno'
}});
check('Dictamen competente en Primer Contacto', r.status === 200, r);
const folioSD = r.json?.folioSubdefensoria;
check('Primer Contacto recibe el folio SD-... de Subdefensoría', /^SD-/.test(folioSD ?? ''), r.json);
check('La queja queda en RECIBIDA_SUBDEFENSORIA (fila 12)', estatusQueja() === 'RECIBIDA_SUBDEFENSORIA', estatusQueja());

const expRecibido = sql(`SELECT estatus || '|' || coalesce(folio_queja,'') || '|' || coalesce(quejoso_correo,'') FROM expedientes_investigacion WHERE folio = '${folioSD}'`);
check('Expediente RECIBIDO, con folio de la queja y correo del quejoso', expRecibido === `RECIBIDO|${FOLIO_Q}|quejoso1@alumno.ipn.mx`, expRecibido);

r = await sd('GET', '/bandeja-nuevas');
check('CU-SD-02 Bandeja de nuevos lo incluye (siguiente: solicitud de información)',
  r.status === 200 && r.json.some(e => e.folio === folioSD && e.siguienteFase === 'SOLICITUD_INFORMACION'), r);

r = await sd('GET', `/expedientes/folio/${folioSD}`);
check('Consultar (GET) no cambia el estatus', r.json?.estatus === 'RECIBIDO', r.json?.estatus);
r = await sd('POST', `/expedientes/folio/${folioSD}/iniciar-investigacion`);
check('CU-SD-05 Abrir el detalle → EN_INVESTIGACION (fila 14)', r.status === 200 && r.json.estatus === 'EN_INVESTIGACION' && r.json.folioQueja === FOLIO_Q, r.json);
const EXP = r.json;
check('La queja también pasa a EN_INVESTIGACION', estatusQueja() === 'EN_INVESTIGACION', estatusQueja());

r = await llamar(SD, 'POST', '/ingesta/expedientes', { body: { folioOrigen: PC_FOLIO, asunto: 'x', fechaAdmision: new Date().toISOString().slice(0, 10) } });
check('Reenviar el mismo expediente no duplica (mismo folio SD)', r.json?.folio === folioSD, r);

r = await llamar(SD, 'GET', '/expedientes');
check('Sin token de SUBDEFENSOR → 401/403', r.status === 401 || r.status === 403, r.status);
r = await llamar(SD, 'GET', '/expedientes', { tk: ANALISTA });
check('Con token de otro rol → 403', r.status === 403, r.status);
r = await sd('GET', '/expedientes');
check('CU-SD-05 Listado de expedientes lo incluye', r.status === 200 && r.json.some(e => e.folio === folioSD), r);

// ------------------------------------------------------------------ Oficios
const oficio = contenido => sd('POST', '/oficios', { body: {
  expedienteId: EXP.id, destinatarioNombre: 'Director de ESCOM', destinatarioCorreo: 'director.escom@ipn.mx',
  unidadAcademica: 'ESCOM', contenidoRedactado: contenido
}});
const estatusExp = async () => (await sd('GET', `/expedientes/folio/${folioSD}`)).json?.estatus;

console.log('\n== CU-SD-06 Emitir oficio de solicitud de información (fila 15)');
r = await oficio('Se solicita información sobre los hechos referidos.');
check('Emitir oficio', r.status === 200 || r.status === 201, r);
const OF1 = r.json;
check('Fase SOLICITUD_INFORMACION, EN_ESPERA, con número y fecha límite', OF1?.fase === 'SOLICITUD_INFORMACION' && OF1?.estatus === 'EN_ESPERA' && OF1?.numeroOficio && OF1?.fechaLimite, OF1);
check('Expediente y queja en EN_ESPERA_OFICIO', (await estatusExp()) === 'EN_ESPERA_OFICIO' && estatusQueja() === 'EN_ESPERA_OFICIO', [await estatusExp(), estatusQueja()]);
r = await oficio('Otro oficio');
check('Con un oficio esperando respuesta no se emite otro → 409', r.status === 409, r);
r = await sd('GET', '/bandeja-nuevas');
check('Ya no aparece en la bandeja de nuevos', !r.json.some(e => e.folio === folioSD), r.json.map(e => e.folio));
r = await sd('GET', '/control-plazos');
check('CU-SD-03 Semáforo incluye el oficio vigente', r.status === 200 && r.json.some(c => c.oficioId === OF1.id), r);

console.log('\n== CU-SD-04 Alertas y CU-SD-07 Recordatorio');
sql(`UPDATE oficios_informacion SET fecha_limite = current_date - 3 WHERE id = ${OF1.id};`);
r = await sd('GET', '/alertas/vencidos');
check('CU-SD-04 Alerta de oficio vencido', r.status === 200 && r.json.some(a => a.oficioId === OF1.id && a.diasRetraso > 0), r);
r = await sd('POST', '/recordatorios', { body: { oficioId: OF1.id, mensaje: 'Se recuerda la respuesta pendiente.' } });
check('CU-SD-07 Generar recordatorio', r.status === 200 || r.status === 201, r);
r = await sd('GET', `/oficios/${OF1.id}`);
check('El oficio vuelve a EN_ESPERA con plazo SUBSECUENTE y fecha nueva', r.json?.estatus === 'EN_ESPERA' && r.json?.tipoPlazo === 'SUBSECUENTE' && r.json?.fechaLimite > new Date().toISOString().slice(0, 10), r.json);
check('El recordatorio no cambia el estatus (sigue EN_ESPERA_OFICIO)', (await estatusExp()) === 'EN_ESPERA_OFICIO');

console.log('\n== CU-SD-08 Respuesta del director (fila 16)');
r = await sd('POST', '/respuestas-externas', { body: { oficioId: OF1.id, canalRecepcion: 'Oficio físico', numeroOficioRespuestaUA: 'DIR-ESCOM-123', resumen: 'El director atendió la situación.' } });
check('El director responde el oficio', r.status === 200 || r.status === 201, r);
check('→ ELABORO_ACUERDO (fila 16), también en la queja', (await estatusExp()) === 'ELABORO_ACUERDO' && estatusQueja() === 'ELABORO_ACUERDO', estatusQueja());
r = await sd('POST', '/respuestas-externas', { body: { oficioId: OF1.id, canalRecepcion: 'Correo', resumen: 'Otra' } });
check('Segunda respuesta al mismo oficio → 409', r.status === 409, r);
r = await sd('POST', '/recordatorios', { body: { oficioId: OF1.id, mensaje: 'x' } });
check('Recordatorio de un oficio ya respondido → 409', r.status === 409, r);
r = await sd('GET', `/oficios/folio/${folioSD}`);
check('Historial de oficios del expediente: 1', r.status === 200 && r.json.length === 1, r);
r = await oficio('Otro oficio con el acuerdo por elaborar');
check('En ELABORO_ACUERDO no se emiten más oficios → 409', r.status === 409, r);

// ------------------------------------------------------------------ CU-SD-09
console.log('\n== CU-SD-09 Acuerdo de conciliación (filas 17 y 18)');
const acuerdo = (texto, concluir) => sd('POST', '/acuerdos-conclusion', { body: { expedienteId: EXP.id, textoAcuerdo: texto, concluir } });
r = await acuerdo('Borrador del acuerdo.', false);
check('Guardar borrador (sigue ELABORO_ACUERDO)', (r.status === 200 || r.status === 201) && (await estatusExp()) === 'ELABORO_ACUERDO', r);
r = await sd('POST', `/acuerdos-conclusion/${EXP.id}/conclusion`, { body: { textoConclusion: 'x' } });
check('Concluir sin acuerdo de conciliación aceptado → 409', r.status === 409, r);
r = await acuerdo('Acuerdo de conciliación: el director se compromete a ...', true);
check('Enviar el acuerdo al quejoso → PENDIENTE_CONCILIACION', (r.status === 200 || r.status === 201) && (await estatusExp()) === 'PENDIENTE_CONCILIACION' && estatusQueja() === 'PENDIENTE_CONCILIACION', estatusQueja());

// CU-QJ-14: el quejoso responde desde el enlace del correo (sin sesión).
const tokenAcuerdo = () => sql(`SELECT token_respuesta FROM acuerdos_conclusion WHERE expediente_id = ${EXP.id}`);
const publico = (m, ruta, body) => llamar(SD, m, `/publico/acuerdos/${ruta}`, { body });
const TOK1 = tokenAcuerdo();
check('El acuerdo enviado tiene token largo', TOK1.length === 64, TOK1);
if (NOTIFICACIONES) {
  const avisos = fs.readFileSync(NOTIFICACIONES, 'utf8').trim().split('\n').filter(Boolean).map(l => JSON.parse(l));
  const correo = avisos.find(a => a.ruta === '/api/notificaciones/enviar' && a.cuerpo.cuerpo.includes(TOK1));
  const panel = avisos.find(a => a.ruta === '/api/notificaciones/registrar' && a.cuerpo.enlace === `/acuerdo/responder?token=${TOK1}`);
  check('Correo al quejoso con el texto del acuerdo y el enlace', correo && correo.cuerpo.destinatario === 'quejoso1@alumno.ipn.mx' && correo.cuerpo.cuerpo.includes('el director se compromete') && correo.cuerpo.cuerpo.includes(`/acuerdo/responder?token=${TOK1}`), correo?.cuerpo);
  check('Notificación del panel que abre la página del acuerdo', !!panel && panel.cuerpo.tipo === 'CONCILIACION', panel?.cuerpo);
}
r = await publico('GET', TOK1);
check('Consultar el acuerdo sin sesión', r.status === 200 && r.json.folioQueja === FOLIO_Q && r.json.puedeResponder === true && r.json.textoAcuerdo.includes('el director'), r);
check('La vista pública no expone ids internos', r.json.expedienteId === undefined && r.json.id === undefined, r.json);
r = await publico('GET', 'no-existe-123');
check('Token inválido → 404', r.status === 404, r);
r = await publico('PUT', `${TOK1}/rechazar`, { comentario: '  ' });
check('Rechazar sin motivo → 409 con mensaje', r.status === 409 && /motivo/.test(r.json?.error), r);
r = await publico('PUT', `${TOK1}/rechazar`, { comentario: 'No estoy de acuerdo con las medidas' });
check('El quejoso rechaza por enlace → EN_INVESTIGACION (fila 19)', r.status === 200 && r.json.respuestaQuejoso === 'RECHAZADO' && !r.json.puedeResponder && (await estatusExp()) === 'EN_INVESTIGACION' && estatusQueja() === 'EN_INVESTIGACION', r);
r = await publico('PUT', `${TOK1}/aceptar`, {});
check('Ya rechazado no se puede aceptar → 409', r.status === 409, r);
r = await sd('GET', '/bandeja-nuevas');
check('Tras el rechazo la bandeja lo muestra listo para otro oficio', r.json.some(e => e.folio === folioSD && e.siguienteFase === 'SOLICITUD_INFORMACION'), r.json);
r = await oficio('Nuevo oficio tras el rechazo del quejoso.');
check('Tras el rechazo se emite otro oficio (ya no se atora)', r.status === 200 || r.status === 201, r);
const OF3 = r.json;
r = await sd('POST', '/respuestas-externas', { body: { oficioId: OF3.id, canalRecepcion: 'Correo', resumen: 'El director propone nuevas medidas.' } });
check('Nueva respuesta del director → ELABORO_ACUERDO', (await estatusExp()) === 'ELABORO_ACUERDO', r);
r = await acuerdo('Segundo acuerdo de conciliación con las nuevas medidas.', true);
check('Nuevo acuerdo enviado; la respuesta anterior se limpia', r.json?.respuestaQuejoso == null && (await estatusExp()) === 'PENDIENTE_CONCILIACION', r.json);

const TOK2 = tokenAcuerdo();
check('El nuevo acuerdo trae un token nuevo', TOK2.length === 64 && TOK2 !== TOK1, [TOK1, TOK2]);
r = await publico('GET', TOK1);
check('El enlace del acuerdo anterior ya no sirve → 404', r.status === 404, r);

console.log('\n== Acepta y acuerdo de conclusión (filas 20 y 21)');
r = await publico('PUT', `${TOK2}/aceptar`, { comentario: 'De acuerdo' });
check('El quejoso acepta por enlace → PENDIENTE_CONCLUSION (fila 20)', r.status === 200 && r.json.respuestaQuejoso === 'ACEPTADO' && (await estatusExp()) === 'PENDIENTE_CONCLUSION' && estatusQueja() === 'PENDIENTE_CONCLUSION', r);
check('Aceptar no concluye el expediente todavía', r.json?.concluido === false && r.json?.puedeResponder === false, r.json);
r = await publico('PUT', `${TOK2}/aceptar`, {});
check('Aceptar dos veces → 409', r.status === 409, r);
r = await sd('POST', `/acuerdos-conclusion/${EXP.id}/conclusion`, { body: { textoConclusion: ' ' } });
check('Acuerdo de conclusión vacío → 400', r.status === 400, r);
r = await sd('POST', `/acuerdos-conclusion/${EXP.id}/conclusion`, { body: { textoConclusion: 'Se concluye el expediente por haberse cumplido el acuerdo de conciliación.' } });
check('Registrar acuerdo de conclusión → CONCLUIDO (fila 21)', r.status === 200 && r.json.concluido === true && r.json.textoConclusion && r.json.fechaConclusion, r);
check('Expediente CONCLUIDO y queja CONCLUIDA', (await estatusExp()) === 'CONCLUIDO' && estatusQueja() === 'CONCLUIDA', [await estatusExp(), estatusQueja()]);
if (NOTIFICACIONES) {
  const avisos = fs.readFileSync(NOTIFICACIONES, 'utf8').trim().split('\n').filter(Boolean).map(l => JSON.parse(l));
  const fin = avisos.find(a => a.ruta === '/api/notificaciones/enviar' && a.cuerpo.asunto.includes('Tu caso ha concluido') && a.cuerpo.asunto.includes(FOLIO_Q));
  check('Correo al quejoso de que su caso concluyó, con el acuerdo de conclusión', fin && fin.cuerpo.cuerpo.includes('haberse cumplido'), fin?.cuerpo);
}
r = await publico('GET', TOK2);
check('El enlace ya muestra el caso concluido', r.status === 200 && r.json.concluido === true, r.json);
r = await oficio('Después de concluir');
check('Concluido no admite más oficios → 409', r.status === 409, r);

console.log(`\nTotal final: ${ok} OK, ${fallas} fallas. Expediente SD ${folioSD}.`);
process.exit(fallas ? 1 : 0);
