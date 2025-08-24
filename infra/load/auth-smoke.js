import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    auth_smoke: {
      executor: 'constant-arrival-rate',
      rate: 10, timeUnit: '1s', duration: '2m',
      preAllocatedVUs: 20, maxVUs: 50,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.02'],
    http_req_duration: ['p(95)<800'],
  },
};

const AUTH_URL = __ENV.AUTH_URL || 'http://auth-service:8080/api/auth';

export default function () {
  const email = `user_${Math.random().toString(36).slice(2)}@test.com`;
  let res = http.post(`${AUTH_URL}/register`, JSON.stringify({ email, password: 'Passw0rd!', role: 'USER' }), {
    headers: { 'Content-Type': 'application/json' },
  });
  check(res, { 'register 2xx': r => r.status >= 200 && r.status < 300 });

  res = http.post(`${AUTH_URL}/login`, JSON.stringify({ email, password: 'Passw0rd!' }), {
    headers: { 'Content-Type': 'application/json' },
  });
  check(res, { 'login 2xx': r => r.status >= 200 && r.status < 300 });

  const token = (() => { try { return res.json().token || res.json().accessToken; } catch { return null; } })();
  if (token) {
    const me = http.get(`${AUTH_URL}/me`, { headers: { Authorization: `Bearer ${token}` } });
    check(me, { 'me 200': r => r.status === 200 });
  }

  sleep(Math.random() * 0.3 + 0.1);
}
