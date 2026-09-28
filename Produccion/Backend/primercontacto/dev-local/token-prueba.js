// Genera un JWT de prueba para usar Primer Contacto EN LOCAL sin levantar admin-service.
//
//   node dev-local/token-prueba.js                       -> analista.pc@ipn.mx
//   node dev-local/token-prueba.js analista2.pc@ipn.mx   -> el segundo analista
//
// Se firma con el JWT_SECRET local de dev-local/variables-entorno.ps1 (HS256, mismo formato
// que emite admin-service: sub = correo, claim "rol"). Para el front de Primer Contacto,
// pega en la consola del navegador (http://localhost:4200) las lineas que imprime.
const crypto = require('crypto');

const SECRETO = process.env.JWT_SECRET || 'gKxw6SOEVL3NQQzDdXk22-UPYsUnafSyQx0eBbgVrN4fqnrh';
const correo = process.argv[2] || 'analista.pc@ipn.mx';

const b64 = (obj) => Buffer.from(JSON.stringify(obj)).toString('base64url');
const ahora = Math.floor(Date.now() / 1000);

const encabezado = b64({ alg: 'HS256', typ: 'JWT' });
const cuerpo = b64({ sub: correo, rol: 'ANALISTA_PRIMER_CONTACTO', iat: ahora, exp: ahora + 12 * 3600 });
const firma = crypto
  .createHmac('sha256', Buffer.from(SECRETO, 'utf8'))
  .update(`${encabezado}.${cuerpo}`)
  .digest('base64url');

const token = `${encabezado}.${cuerpo}.${firma}`;

console.log(token);
console.log('\n// Consola del navegador en el front de Primer Contacto:');
console.log(`localStorage.setItem('ddp_revision_token', '${token}');`);
console.log(`localStorage.setItem('ddp_revision_rol', 'ANALISTA_PRIMER_CONTACTO');`);
console.log(`localStorage.setItem('ddp_revision_nombre', '${correo}');`);
