// Prueba E2E LOCAL de las observaciones del 2026-10-12:
//   - citas: no se encima un analista (1 h por cita), respuesta del quejoso por ENLACE
//     (correo / notificación, sin sesión);
//   - antecedentes con el modelo V5: tres listas (fuertes, por involucrados, por hechos) y
//     el respaldo por reglas con las mismas tres listas si el modelo no responde.
//
// Requisitos (ver CONTEXTO-CHAT-PRIMER-CONTACTO-2026-09.md, sección 15):
//   - base local arriba (docker compose de dev-local) con 04-busqueda-manual-prueba.sql;
//   - antecedentes_service V5 en 8093 (o nada en 8093 con MODELO_APAGADO=1);
//   - un receptor de notificaciones en 8085 que guarde en NOTIFICACIONES (jsonl) lo recibido;
//   - primercontacto en 8082 con --primer-contacto.portal.url=http://localhost:4200.
//
//   node dev-local/prueba-citas-antecedentes-v5.mjs
//
// Crea sus quejas FOL-CIT-... y borra las de corridas anteriores. Solo para desarrollo.
import crypto from 'node:crypto';
import fs from 'node:fs';
import { execSync } from 'node:child_process';

const API = 'http://localhost:8082/api/primer-contacto';
const SECRETO = 'gKxw6SOEVL3NQQzDdXk22-UPYsUnafSyQx0eBbgVrN4fqnrh';
const NOTIFICACIONES = process.env.NOTIFICACIONES;
const MODELO_APAGADO = process.env.MODELO_APAGADO === '1';

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
const ANALISTA2 = token('analista2.pc@ipn.mx', 'ANALISTA_PRIMER_CONTACTO');

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

// ------------------------------------------------------------------ Preparación
sql(`DELETE FROM citas_primer_contacto WHERE expediente_id IN (SELECT id FROM expedientes_primer_contacto WHERE folio_origen LIKE 'FOL-CIT-%');
     DELETE FROM expedientes_primer_contacto WHERE folio_origen LIKE 'FOL-CIT-%';
     DELETE FROM quejas WHERE numero_folio LIKE 'FOL-CIT-%';`);

async function nuevoExpediente(sufijo, correo, nombre, apellido1, denunciado = [null, null]) {
  const folio = `FOL-CIT-${sufijo}${Date.now().toString().slice(-5)}`;
  sql(`INSERT INTO quejas (id, numero_folio, correo_institucional, motivo, descripcion, fecha_creacion,
         nombre_quejoso, apellido1_quejoso, nombre_denunciado, apellido1_denunciado, unidad_academica_clave,
         origen_registro, estatus, fecha_hechos)
       VALUES ((SELECT coalesce(max(id),0)+1 FROM quejas), '${folio}', '${correo}', 'Comentarios del profesor',
         'El profesor de Cálculo hizo comentarios ofensivos frente al grupo.', now(), '${nombre}', '${apellido1}',
         ${denunciado[0] ? `'${denunciado[0]}'` : 'NULL'}, ${denunciado[1] ? `'${denunciado[1]}'` : 'NULL'},
         'ESCOM', 'AUTENTICADO', 'TURNADA', current_date - 10);`);
  const ing = await llamar('POST', '/ingesta/expedientes', { tk: null, body: {
    folioOrigen: folio, tema: 'Comentarios del profesor',
    descripcionHechos: 'El profesor de Cálculo hizo comentarios ofensivos frente al grupo.',
    fechaRecepcion: new Date().toISOString().slice(0, 19),
    quejoso: { nombreCompleto: `${nombre} ${apellido1}`, correo, unidadAcademica: 'ESCOM' }
  }});
  if (ing.status !== 201) throw new Error('Ingesta falló: ' + JSON.stringify(ing));
  return { folioQueja: folio, pc: ing.json.folio };
}

const A = await nuevoExpediente('A', 'quejoso1@alumno.ipn.mx', 'Ana', 'López', ['Roberto', 'Gómez']);
const B = await nuevoExpediente('B', 'otro.alumno@alumno.ipn.mx', 'Pedro', 'Salas');
const C = await nuevoExpediente('C', 'tercero@alumno.ipn.mx', 'Sofía', 'Méndez');

// Un día sin otras citas de prueba.
const dia = new Date(Date.now() + (40 + Math.floor(Math.random() * 200)) * 86400000).toISOString().slice(0, 10);

// ------------------------------------------------------------------ Traslape
console.log(`\n== Traslape de citas (${dia})`);
const citar = (exp, hora, tk = ANALISTA) => llamar('POST', '/citas', { tk, body: { folio: exp.pc, fechaCita: dia, horaCita: hora, tipoCita: 'PRESENCIAL', motivo: 'Entrevista inicial' } });

let r = await citar(A, '10:00');
check('Agendar cita de A a las 10:00', r.status === 200, r);
const citaA = r.json;

r = await citar(B, '10:30');
check('Mismo analista a las 10:30 (encimada) → 409 con mensaje claro', r.status === 409 && /10:00/.test(r.json?.error) && /otro horario/.test(r.json?.error), r);
r = await citar(B, '09:01');
check('Mismo analista a las 09:01 (termina 10:01) → 409', r.status === 409, r);
r = await citar(B, '11:00');
check('Mismo analista a las 11:00 (justo al terminar) → se permite', r.status === 200, r);
const citaB = r.json;
r = await citar(C, '10:00', ANALISTA2);
check('OTRO analista a las 10:00 → se permite', r.status === 200, r);

r = await llamar('PUT', `/citas/${citaB.id}/reagendar`, { body: { fechaCita: dia, horaCita: '10:15' } });
check('Reagendar B encima de A → 409', r.status === 409, r);
r = await llamar('PUT', `/citas/${citaB.id}/reagendar`, { body: { fechaCita: dia, horaCita: '11:30' } });
check('Reagendar B a las 11:30 (se encima solo con su propia hora anterior) → se permite', r.status === 200, r);
const citaB2 = r.json;

r = await llamar('PUT', `/citas/${citaA.id}/cancelar`);
check('Cancelar A libera su horario', r.status === 200, r);
r = await llamar('PUT', `/citas/${citaB2.id}/reagendar`, { body: { fechaCita: dia, horaCita: '10:00' } });
check('Ya se puede reagendar B a las 10:00', r.status === 200, r);
const citaB3 = r.json;

// ------------------------------------------------------------------ Enlace del correo
console.log('\n== Respuesta del quejoso por enlace');
const tokenDe = id => sql(`SELECT token_respuesta FROM citas_primer_contacto WHERE id = ${id}`);
const tokB3 = tokenDe(citaB3.id);
const tokB2 = tokenDe(citaB2.id);
check('Cada cita nueva trae su propio token largo', tokB3.length === 64 && tokB2.length === 64 && tokB3 !== tokB2, [tokB2, tokB3]);

if (NOTIFICACIONES) {
  const avisos = fs.readFileSync(NOTIFICACIONES, 'utf8').trim().split('\n').map(l => JSON.parse(l));
  const correo = avisos.find(a => a.ruta === '/api/notificaciones/enviar' && a.cuerpo.cuerpo.includes(tokB3));
  const panel = avisos.find(a => a.ruta === '/api/notificaciones/registrar' && a.cuerpo.enlace === `/cita/responder?token=${tokB3}`);
  check('El correo trae el enlace completo del portal', correo && correo.cuerpo.cuerpo.includes(`http://localhost:4200/cita/responder?token=${tokB3}`), correo?.cuerpo);
  check('La notificación del panel abre la página de respuesta', !!panel && panel.cuerpo.tipo === 'CITA', panel?.cuerpo);
}

r = await llamar('GET', `/publico/citas/${tokB3}`, { tk: null });
check('Consultar la cita sin sesión', r.status === 200 && r.json.folioQueja === B.folioQueja && r.json.puedeResponder === true && r.json.estatus === 'PROGRAMADA', r);
check('La vista pública no expone ids internos', r.json.id === undefined && r.json.expedienteId === undefined && r.json.analistaId === undefined, r.json);

r = await llamar('GET', `/publico/citas/${tokB2}`, { tk: null });
check('El enlace de la cita reagendada ya no deja responder', r.status === 200 && r.json.estatus === 'REAGENDADA' && r.json.puedeResponder === false, r.json);
r = await llamar('PUT', `/publico/citas/${tokB2}/confirmar`, { tk: null });
check('Confirmar con el enlace viejo → 409', r.status === 409, r);

r = await llamar('GET', '/publico/citas/no-existe-123', { tk: null });
check('Token inválido → 404', r.status === 404, r);

r = await llamar('PUT', `/publico/citas/${tokB3}/cancelar`, { tk: null, body: { motivo: '  ' } });
check('Cancelar sin motivo → 400', r.status === 400, r);
r = await llamar('PUT', `/publico/citas/${tokB3}/cancelar`, { tk: null, body: { motivo: 'Tengo examen ese día' } });
check('Cancelar con motivo → CANCELADA_QUEJOSO', r.status === 200 && r.json.estatus === 'CANCELADA_QUEJOSO' && r.json.motivoCancelacionQuejoso === 'Tengo examen ese día', r);
r = await llamar('PUT', `/publico/citas/${tokB3}/confirmar`, { tk: null });
check('Ya cancelada no se puede confirmar → 409', r.status === 409, r);

r = await llamar('GET', `/citas/folio/${B.pc}`);
const vistaAnalista = r.json.find(c => c.id === citaB3.id);
check('El analista ve la cancelación del quejoso (respondió QUEJOSO)', vistaAnalista?.estatus === 'CANCELADA_QUEJOSO' && vistaAnalista.respuestaRegistradaPor === 'QUEJOSO', vistaAnalista);

r = await citar(A, '10:00');
check('La cita cancelada por el quejoso libera el horario', r.status === 200, r);
const citaA2 = r.json;
r = await llamar('PUT', `/publico/citas/${tokenDe(citaA2.id)}/confirmar`, { tk: null });
check('Confirmar por enlace → CONFIRMADA', r.status === 200 && r.json.estatus === 'CONFIRMADA' && r.json.puedeResponder === false, r);

sql(`UPDATE citas_primer_contacto SET fecha_limite_respuesta = now() - interval '1 minute' WHERE id = ${citaB3.id};`);

// ------------------------------------------------------------------ Antecedentes
console.log(`\n== Antecedentes (${MODELO_APAGADO ? 'modelo apagado: respaldo por reglas' : 'modelo V5'})`);
r = await llamar('GET', `/antecedentes/${A.pc}`);
const b = r.json;
check('Responde con las tres listas', r.status === 200 && Array.isArray(b.porHechos) && Array.isArray(b.porInvolucrados) && Array.isArray(b.coincidenciasFuertes), b);
check(`Motor ${MODELO_APAGADO ? 'REGLAS_PROVISIONAL con aviso' : 'MODELO sin avisos'}`,
  MODELO_APAGADO ? b.motor === 'REGLAS_PROVISIONAL' && b.avisos.length === 1 : b.motor === 'MODELO' && b.avisos.length === 0, [b.motor, b.avisos]);
check('"resultados" = por involucrados (compatibilidad)', JSON.stringify(b.resultados) === JSON.stringify(b.porInvolucrados));
check('Por involucrados: todas del mismo quejoso o denunciado, con tipo de coincidencia',
  b.porInvolucrados.length > 0 && b.porInvolucrados.every(a => (a.mismoQuejoso || a.mismoDenunciado) && a.tipoCoincidencia), b.porInvolucrados.map(a => [a.folioQueja, a.tipoCoincidencia]));
const hist20 = b.porInvolucrados.find(a => a.folioQueja === 'FOL-HIST0020');
check('FOL-HIST0020 (Ana López vs Roberto Gómez Hernández): quejoso y denunciado', hist20?.mismoQuejoso && hist20?.mismoDenunciado, hist20);
// El motor por reglas solo compara con las quejas del sistema; el V5 también con el histórico.
if (!MODELO_APAGADO) {
  check('Por involucrados incluye históricos (Roberto Gómez en HIST-2019-0101)', b.porInvolucrados.some(a => a.folioQueja === 'HIST-2019-0101' && a.origen === 'HISTORICO' && a.mismoDenunciado), b.porInvolucrados.map(a => a.folioQueja));
} else {
  check('Respaldo: coincidencia fuerte por correo del quejoso con hechos parecidos', b.coincidenciasFuertes.some(a => a.tipoCoincidencia.startsWith('Correo del quejoso')), b.coincidenciasFuertes);
}
check('Por involucrados no trae personas ajenas (Pedro Salas)', !b.porInvolucrados.some(a => a.folioQueja === 'FOL-HIST0024'), b.porInvolucrados.map(a => a.folioQueja));
check('Por hechos: ordenados por similitud, máximo 20, con narrativa',
  b.porHechos.length > 0 && b.porHechos.length <= 20 && b.porHechos.every((a, i, l) => i === 0 || l[i - 1].similitud >= a.similitud) && b.porHechos.every(a => a.descripcion), b.porHechos.map(a => [a.folioQueja, a.similitud]));
check('Ninguna lista incluye la propia queja', ![...b.porHechos, ...b.porInvolucrados, ...b.coincidenciasFuertes].some(a => a.folioQueja === A.folioQueja));

// Coincidencia fuerte: misma persona por identificación/correo + hechos parecidos.
check('Coincidencias fuertes: identidad verificada, similitud ≥ 50 y razón',
  b.coincidenciasFuertes.every(a => a.identidadVerificada && a.similitud >= 50 && a.razonCoincidencia), b.coincidenciasFuertes);
const deFuerte = b.porInvolucrados.find(a => a.folioQueja.startsWith('FOL-OBS-') || a.folioQueja === 'FOL-LOCAL001');
if (deFuerte) {
  console.log(`  (info) ${deFuerte.folioQueja}: ${deFuerte.tipoCoincidencia}, similitud ${deFuerte.similitud}%`);
}

// Sus quejas no deben estorbar a prueba-observaciones.mjs (que cuenta resultados exactos).
sql(`DELETE FROM citas_primer_contacto WHERE expediente_id IN (SELECT id FROM expedientes_primer_contacto WHERE folio_origen LIKE 'FOL-CIT-%');
     DELETE FROM expedientes_primer_contacto WHERE folio_origen LIKE 'FOL-CIT-%';
     DELETE FROM quejas WHERE numero_folio LIKE 'FOL-CIT-%';`);

console.log(`\nTotal final: ${ok} OK, ${fallas} fallas.`);
process.exit(fallas ? 1 : 0);
