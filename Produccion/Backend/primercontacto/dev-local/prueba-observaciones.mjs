// Prueba E2E LOCAL de las observaciones del 2026-10-08 (39 comprobaciones contra la API):
// antecedentes (manual, modelo, guardados) y citas con respuesta del quejoso (48 h).
//
// Requisitos (ver CONTEXTO-CHAT-PRIMER-CONTACTO-2026-09.md, sección 13):
//   - base local arriba (docker compose de dev-local) con 04-busqueda-manual-prueba.sql cargado;
//   - antecedentes-service local en 8093 (Modelo-Java);
//   - primercontacto en 8082 con --primer-contacto.citas.revision-ms=10000 (para que el
//     vencimiento de 48 h se pruebe en segundos).
//
//   node dev-local/prueba-observaciones.mjs
//
// Crea su propia queja FOL-OBS-... y borra las de corridas anteriores. Solo para desarrollo.
import crypto from 'node:crypto';
import { execSync } from 'node:child_process';

const API = 'http://localhost:8082/api/primer-contacto';
const SECRETO = 'gKxw6SOEVL3NQQzDdXk22-UPYsUnafSyQx0eBbgVrN4fqnrh';

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
const QUEJOSO = token('quejoso1@alumno.ipn.mx');
const OTRO_QUEJOSO = token('otro.alumno@alumno.ipn.mx');

async function llamar(metodo, ruta, { tk = ANALISTA, body } = {}) {
  const r = await fetch(API + ruta, {
    method: metodo,
    headers: { 'Content-Type': 'application/json', ...(tk ? { Authorization: `Bearer ${tk}` } : {}) },
    body: body ? JSON.stringify(body) : undefined
  });
  const texto = await r.text();
  let json; try { json = texto ? JSON.parse(texto) : null; } catch { json = texto; }
  return { status: r.status, json };
}

const sql = q => execSync(`docker exec -i defensoria-db-local psql -U postgres -d defensoria_db -tA`, { input: q }).toString().trim();

let ok = 0, fallas = 0;
function check(nombre, cond, detalle) {
  if (cond) { ok++; console.log(`  OK   ${nombre}`); }
  else { fallas++; console.log(`  FALLA ${nombre}`, detalle !== undefined ? JSON.stringify(detalle).slice(0, 400) : ''); }
}
const sleep = ms => new Promise(r => setTimeout(r, ms));

// ------------------------------------------------------------------ Preparación
// Limpia lo que dejaron corridas anteriores de esta misma prueba.
sql(`DELETE FROM antecedentes_primer_contacto WHERE expediente_id IN (SELECT id FROM expedientes_primer_contacto WHERE folio_origen LIKE 'FOL-OBS-%');
     DELETE FROM citas_primer_contacto WHERE expediente_id IN (SELECT id FROM expedientes_primer_contacto WHERE folio_origen LIKE 'FOL-OBS-%');
     DELETE FROM expedientes_primer_contacto WHERE folio_origen LIKE 'FOL-OBS-%';
     DELETE FROM quejas WHERE numero_folio LIKE 'FOL-OBS-%';`);
const FOLIO_Q = 'FOL-OBS-' + Date.now().toString().slice(-6);
sql(`INSERT INTO quejas (id, numero_folio, correo_institucional, motivo, descripcion, fecha_creacion,
       nombre_quejoso, apellido1_quejoso, nombre_denunciado, apellido1_denunciado, unidad_academica_clave,
       origen_registro, estatus)
     VALUES ((SELECT coalesce(max(id),0)+1 FROM quejas), '${FOLIO_Q}', 'quejoso1@alumno.ipn.mx',
       'Comentarios discriminatorios del profesor',
       'El profesor de Cálculo volvió a hacer comentarios discriminatorios frente al grupo. Me gritó y amenazó con reprobarme si lo denunciaba. Tengo miedo de entrar a su clase.',
       now(), 'Ana', 'López', 'Roberto', 'Gómez', 'ESCOM', 'AUTENTICADO', 'TURNADA');`);

const ing = await llamar('POST', '/ingesta/expedientes', { tk: null, body: {
  folioOrigen: FOLIO_Q, tema: 'Comentarios discriminatorios del profesor',
  descripcionHechos: 'El profesor de Cálculo volvió a hacer comentarios discriminatorios frente al grupo. Me gritó y amenazó con reprobarme si lo denunciaba.',
  fechaRecepcion: new Date().toISOString().slice(0, 19),
  quejoso: { nombreCompleto: 'Ana López', correo: 'quejoso1@alumno.ipn.mx', unidadAcademica: 'ESCOM' }
}});
check('Ingesta crea expediente', ing.status === 201, ing);
const PC = ing.json.folio;
console.log(`Expediente de prueba: ${PC} (queja ${FOLIO_Q})`);

// ------------------------------------------------------------------ Antecedentes
console.log('\n== Antecedentes');
let r = await llamar('GET', `/antecedentes/${PC}`);
check('Modelo responde (motor MODELO, sin avisos)', r.status === 200 && r.json.motor === 'MODELO' && r.json.avisos.length === 0, r.json?.motor);
check('Modelo trae resultados con origen y similitud', r.json.resultados.length > 0 && r.json.resultados.every(a => a.origen && a.similitud >= 0 && a.descripcion), r.json.resultados[0]);
const deModelo = r.json.resultados[0];

r = await llamar('GET', `/antecedentes/${PC}/manual?quejoso=${encodeURIComponent('ana lopez')}`);
const folios = x => x.json.resultados.map(a => a.folioQueja);
check('Manual por quejoso (sin acentos) encuentra sistema + histórico', r.status === 200
  && folios(r).includes('FOL-HIST0020') && folios(r).includes('FOL-HIST0021') && folios(r).includes('HIST-2022-0033'), folios(r));
check('Manual no incluye la propia queja', !folios(r).includes(FOLIO_Q), folios(r));
check('Manual: motor MANUAL y sin avisos (histórico configurado)', r.json.motor === 'MANUAL' && r.json.avisos.length === 0, r.json.avisos);

r = await llamar('GET', `/antecedentes/${PC}/manual?denunciado=gomez`);
check('Manual por denunciado encuentra 2 del sistema + 2 históricos', ['FOL-HIST0020', 'FOL-HIST0022', 'HIST-2019-0101', 'HIST-2021-0457'].every(f => folios(r).includes(f)) && folios(r).length === 4, folios(r));
check('Manual: históricos marcados como HISTORICO', r.json.resultados.filter(a => a.folioQueja.startsWith('HIST-')).every(a => a.origen === 'HISTORICO'));

r = await llamar('GET', `/antecedentes/${PC}/manual?quejoso=Ana&denunciado=${encodeURIComponent('Roberto Gómez')}`);
check('Coincidencia en quejoso y denunciado sale primero', r.json.resultados[0]?.folioQueja === 'FOL-HIST0020' && r.json.resultados[0].coincidencias.length === 2, r.json.resultados[0]);

r = await llamar('GET', `/antecedentes/${PC}/manual`);
check('Manual sin nombres → 409', r.status === 409, r);

const manualSel = (await llamar('GET', `/antecedentes/${PC}/manual?denunciado=gomez`)).json.resultados.find(a => a.folioQueja === 'HIST-2019-0101');
const item = (a, fuente) => ({ origen: a.origen, folioQueja: a.folioQueja, fuente, similitud: a.similitud, asunto: a.asunto,
  fecha: a.fecha, nombreQuejoso: a.nombreQuejoso, nombreDenunciado: a.nombreDenunciado, unidadAcademica: a.unidadAcademica,
  estatus: a.estatus, extracto: a.extracto });

r = await llamar('POST', `/antecedentes/${PC}/guardados`, { body: { antecedentes: [item(manualSel, 'MANUAL'), item(deModelo, 'MODELO')] } });
check('Guardar selección de ambas pestañas → 2 guardados', r.status === 200 && r.json.length === 2, r);
check('Guardado manual sin similitud; con analista', r.json.find(g => g.fuente === 'MANUAL')?.similitud === null && r.json.every(g => g.analistaNombre), r.json);

r = await llamar('POST', `/antecedentes/${PC}/guardados`, { body: { antecedentes: [item(manualSel, 'MANUAL')] } });
check('Volver a guardar el mismo no duplica', r.json.length === 2, r.json.length);

r = await llamar('POST', `/antecedentes/${PC}/guardados`, { body: { antecedentes: [{ origen: 'SISTEMA', folioQueja: FOLIO_Q, fuente: 'MANUAL' }] } });
check('No se puede guardar la propia queja → 409', r.status === 409, r);

r = await llamar('POST', `/antecedentes/${PC}/guardados`, { body: { antecedentes: [] } });
check('Guardar lista vacía → 400', r.status === 400, r);

r = await llamar('POST', `/antecedentes/${PC}/guardados`, { tk: null, body: { antecedentes: [item(manualSel, 'MANUAL')] } });
check('Guardar sin token → rechazado', r.status === 401 || r.status === 403, r.status);

const guardados = (await llamar('GET', `/antecedentes/${PC}/guardados`)).json;
r = await llamar('DELETE', `/antecedentes/PC-A5046128/guardados/${guardados[0].id}`);
check('Quitar un guardado desde otro expediente → 404', r.status === 404, r);
r = await llamar('DELETE', `/antecedentes/${PC}/guardados/${guardados[0].id}`);
check('Quitar guardado → 204', r.status === 204, r);
check('Queda 1 guardado', (await llamar('GET', `/antecedentes/${PC}/guardados`)).json.length === 1);

// ------------------------------------------------------------------ Citas
console.log('\n== Citas');
const manana = new Date(Date.now() + 3 * 86400000).toISOString().slice(0, 10);
r = await llamar('POST', '/citas', { body: { folio: PC, fechaCita: manana, horaCita: '10:30', tipoCita: 'PRESENCIAL', motivo: 'Entrevista inicial' } });
check('Agendar → PROGRAMADA con plazo ≈ 48 h', r.status === 200 && r.json.estatus === 'PROGRAMADA'
  && Math.abs(new Date(r.json.fechaLimiteRespuesta) - Date.now() - 48 * 3600000) < 120000, r.json);
check('Cita trae folioQueja', r.json.folioQueja === FOLIO_Q, r.json.folioQueja);
const CITA = r.json.id;

r = await llamar('POST', '/citas', { body: { folio: PC, fechaCita: manana, horaCita: '12:00', tipoCita: 'PRESENCIAL', motivo: 'Otra' } });
check('Segunda cita con una activa → 409', r.status === 409, r);

r = await llamar('GET', '/quejoso/citas/mias', { tk: QUEJOSO });
check('Quejoso ve su cita en /mias', r.status === 200 && r.json.some(c => c.id === CITA), r);
r = await llamar('GET', '/quejoso/citas/mias', { tk: OTRO_QUEJOSO });
check('Otro quejoso no la ve', r.status === 200 && !r.json.some(c => c.id === CITA), r.json);
r = await llamar('GET', '/quejoso/citas/mias', { tk: null });
check('Sin token → rechazado', r.status === 401 || r.status === 403, r.status);
r = await llamar('PUT', `/quejoso/citas/${CITA}/confirmar`, { tk: OTRO_QUEJOSO });
check('Otro quejoso no puede confirmarla → 404', r.status === 404, r);
r = await llamar('PUT', `/quejoso/citas/${CITA}/cancelar`, { tk: QUEJOSO, body: { motivo: '  ' } });
check('Cancelar sin motivo → 400', r.status === 400, r);
r = await llamar('PUT', `/quejoso/citas/${CITA}/cancelar`, { tk: QUEJOSO, body: { motivo: 'Tengo examen ese día' } });
check('Quejoso cancela con motivo → CANCELADA_QUEJOSO', r.status === 200 && r.json.estatus === 'CANCELADA_QUEJOSO'
  && r.json.motivoCancelacionQuejoso === 'Tengo examen ese día' && r.json.respuestaRegistradaPor === 'QUEJOSO', r.json);
r = await llamar('PUT', `/quejoso/citas/${CITA}/confirmar`, { tk: QUEJOSO });
check('Ya respondió: confirmar después → 409', r.status === 409, r);
r = await llamar('POST', '/citas', { body: { folio: PC, fechaCita: manana, horaCita: '12:00', tipoCita: 'PRESENCIAL', motivo: 'Otra' } });
check('Cancelada por quejoso sigue activa (no deja crear otra) → 409', r.status === 409, r);

const pasado = new Date(Date.now() + 4 * 86400000).toISOString().slice(0, 10);
r = await llamar('PUT', `/citas/${CITA}/reagendar`, { body: { fechaCita: pasado, horaCita: '09:00' } });
check('Reagendar → PROGRAMADA, plazo nuevo y respuesta limpia', r.status === 200 && r.json.estatus === 'PROGRAMADA'
  && !r.json.motivoCancelacionQuejoso && !r.json.respuestaRegistradaPor && r.json.fechaLimiteRespuesta, r.json);

r = await llamar('PUT', `/quejoso/citas/${CITA}/confirmar`, { tk: QUEJOSO });
check('Quejoso confirma la nueva fecha → CONFIRMADA', r.status === 200 && r.json.estatus === 'CONFIRMADA' && r.json.respuestaRegistradaPor === 'QUEJOSO', r.json);

// Vencimiento: se reagenda y se fuerza el plazo al pasado; el proceso corre cada 10 s en esta prueba.
await llamar('PUT', `/citas/${CITA}/reagendar`, { body: { fechaCita: pasado, horaCita: '11:00' } });
sql(`UPDATE citas_primer_contacto SET fecha_limite_respuesta = timestamp '2000-01-01 00:00' WHERE id = ${CITA};`);
r = await llamar('PUT', `/quejoso/citas/${CITA}/confirmar`, { tk: QUEJOSO });
check('Plazo vencido (antes del proceso): quejoso no puede confirmar → 409', r.status === 409, r);
let estatus = '';
for (let i = 0; i < 15 && estatus !== 'SIN_RESPUESTA'; i++) { await sleep(1500); estatus = sql(`SELECT estatus FROM citas_primer_contacto WHERE id = ${CITA};`); }
check('El proceso automático la marca SIN_RESPUESTA', estatus === 'SIN_RESPUESTA', estatus);

r = await llamar('PUT', `/citas/${CITA}/cancelacion-quejoso`, { body: { motivo: 'Avisó por teléfono que no puede' } });
check('Analista registra cancelación del quejoso desde SIN_RESPUESTA', r.status === 200 && r.json.estatus === 'CANCELADA_QUEJOSO' && r.json.respuestaRegistradaPor === 'ANALISTA', r.json);
r = await llamar('PUT', `/citas/${CITA}/cancelacion-quejoso`, { body: { motivo: 'otra vez' } });
check('Registrar cancelación sobre una ya cancelada por quejoso → 409', r.status === 409, r);
r = await llamar('PUT', `/citas/${CITA}/confirmar`);
check('Analista registra confirmación → CONFIRMADA (ANALISTA)', r.status === 200 && r.json.estatus === 'CONFIRMADA' && r.json.respuestaRegistradaPor === 'ANALISTA', r.json);
r = await llamar('PUT', `/citas/${CITA}/cancelar`);
check('Analista cancela → CANCELADA', r.status === 200 && r.json.estatus === 'CANCELADA', r.json);
r = await llamar('PUT', `/quejoso/citas/${CITA}/confirmar`, { tk: QUEJOSO });
check('Cita cancelada: quejoso no puede responder → 409', r.status === 409, r);

console.log(`\nResultado: ${ok} OK, ${fallas} fallas. Expediente ${PC}, queja ${FOLIO_Q}`);
process.exit(fallas ? 1 : 0);
