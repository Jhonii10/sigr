// Prueba de carga "hora pico" de SIGR (k6).
// Mezcla de tráfico: 60 % menú público, 30 % disponibilidad de reservas, 10 % mis reservas.
// Variables: BASE_URL (p. ej. http://localhost:9081/api), VUS (100), ADMIN_EMAIL, ADMIN_PASSWORD.
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.BASE_URL || 'http://localhost:8080/api';
const VUS = parseInt(__ENV.VUS || '100', 10);

export const options = {
  scenarios: {
    hora_pico: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: VUS }, // llegada de clientes
        { duration: '1m', target: VUS },  // pico sostenido
        { duration: '15s', target: 0 },   // salida
      ],
      gracefulRampDown: '10s',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
  },
};

// Se ejecuta una sola vez: el token del administrador lo comparten todos los usuarios virtuales.
export function setup() {
  const res = http.post(
    `${BASE}/auth/login`,
    JSON.stringify({
      email: __ENV.ADMIN_EMAIL || 'admin@sigr.local',
      password: __ENV.ADMIN_PASSWORD || 'Admin123*',
    }),
    { headers: { 'Content-Type': 'application/json' }, tags: { name: 'login' } },
  );
  if (res.status !== 200 || !res.json('token')) {
    throw new Error(`No se pudo iniciar sesión como administrador (HTTP ${res.status})`);
  }
  return { token: res.json('token') };
}

// Fecha entre 1 y 14 días a partir de hoy, en formato AAAA-MM-DD.
function fechaProxima() {
  const d = new Date();
  d.setDate(d.getDate() + 1 + Math.floor(Math.random() * 14));
  return d.toISOString().slice(0, 10);
}

export default function (data) {
  const auth = { headers: { Authorization: `Bearer ${data.token}` } };
  const r = Math.random();

  if (r < 0.6) {
    const res = http.get(`${BASE}/menu/platos`, { tags: { name: 'menu_platos' } });
    check(res, { 'menú 200': (x) => x.status === 200 });
  } else if (r < 0.9) {
    const url = `${BASE}/reservas/disponibilidad?fecha=${fechaProxima()}&hora=19:00&personas=4`;
    const res = http.get(url, { ...auth, tags: { name: 'reservas_disponibilidad' } });
    check(res, { 'disponibilidad 200': (x) => x.status === 200 });
  } else {
    const res = http.get(`${BASE}/reservas/mias`, { ...auth, tags: { name: 'reservas_mias' } });
    check(res, { 'mis reservas 200': (x) => x.status === 200 });
  }

  sleep(0.5 + Math.random()); // tiempo de "pensar" del usuario
}

// Resumen propio (independiente de la versión de k6): JSON completo para la evidencia
// y un texto corto en consola.
export function handleSummary(data) {
  const m = data.metrics;
  const p95 = m.http_req_duration ? m.http_req_duration.values['p(95)'] : NaN;
  const errores = m.http_req_failed ? m.http_req_failed.values.rate : NaN;
  const peticiones = m.http_reqs ? m.http_reqs.values.count : 0;
  const rps = m.http_reqs ? m.http_reqs.values.rate : 0;
  const umbrales = Object.entries(m)
    .filter(([, v]) => v.thresholds)
    .map(([nombre, v]) => Object.entries(v.thresholds)
      .map(([regla, t]) => `  ${t.ok ? 'OK   ' : 'FALLA'} ${nombre} ${regla}`).join('\n'))
    .join('\n');
  const texto = [
    '',
    '===== Resumen prueba de carga SIGR (hora pico) =====',
    `Usuarios virtuales: ${VUS}`,
    `Peticiones:         ${peticiones} (${rps.toFixed(1)} req/s)`,
    `Latencia p95:       ${p95.toFixed(1)} ms`,
    `Tasa de errores:    ${(errores * 100).toFixed(2)} %`,
    'Umbrales:',
    umbrales,
    '',
  ].join('\n');
  return {
    '/evidencias/k6-resumen.json': JSON.stringify(data, null, 2),
    '/evidencias/k6-resumen.txt': texto,
    stdout: texto,
  };
}
