// ============================================================================
// Capacidad de usuarios concurrentes (REN-01 del plan) — k6
// Solo endpoints públicos de LECTURA: no crea datos.
//   k6 run -e BASE_URL=https://pruebas.ejemplo -e FOLIO=FOL-XXXX -e CORREO=x@alumno.ipn.mx carga.k6.js
// Criterios: p95 < 1 s y < 1 % de errores con 50 usuarios virtuales.
// ============================================================================
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.BASE_URL;

export const options = {
  scenarios: {
    rampa: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 20 },
        { duration: '1m', target: 50 },
        { duration: '30s', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<1000'],
    http_req_failed: ['rate<0.01'],
    checks: ['rate>0.99'],
  },
};

export default function () {
  const portada = http.get(`${BASE}/`);
  check(portada, { 'portada 200': (r) => r.status === 200 });

  const cat = http.get(`${BASE}/api/catalogos/dependencias`);
  check(cat, { 'catálogo 200': (r) => r.status === 200 });

  const faq = http.get(`${BASE}/api/chatbot/menu`);
  check(faq, { 'preguntas frecuentes 200': (r) => r.status === 200 });

  if (__ENV.FOLIO && __ENV.CORREO) {
    const q = http.get(`${BASE}/api/quejoso/quejas/folio/${__ENV.FOLIO}?correo=${encodeURIComponent(__ENV.CORREO)}`);
    check(q, { 'consulta por folio 200': (r) => r.status === 200 });
  }
  sleep(1);
}
