import http from 'k6/http';
import { check, sleep, group, fail } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const BASE_URL      = __ENV.BASE_URL      || 'http://nginx:80';
const AUTH_URL     = `${BASE_URL}/api/auth`;
const PRODUCT_URL  = `${BASE_URL}/api/products`;
const CART_URL     = `${BASE_URL}/api/cart`;
const ORDER_URL    = `${BASE_URL}/api/orders`;

const USER_EMAIL    = __ENV.USER_EMAIL || '';
const USER_PASSWORD = __ENV.USER_PASSWORD || 'mypassword';

const flowCheckoutMs = new Trend('flow_checkout_ms');
const flowBrowseMs   = new Trend('flow_browse_ms');
const flowFailed     = new Rate('flow_failed');

export const options = {
  scenarios: {
    browse: {
      executor: 'constant-arrival-rate',
      rate: 5, timeUnit: '1s', duration: '60s',
      preAllocatedVUs: 10, maxVUs: 20,
    },
    checkout: {
      executor: 'ramping-arrival-rate',
      startRate: 10, timeUnit: '1s',
      stages: [
        { target: 50, duration: '60s' },
        { target: 50, duration: '90s' },
        { target: 0, duration: '30s' },
      ],
      preAllocatedVUs: 20, maxVUs: 100,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],
    'http_req_duration{scenario:checkout}': ['p(99)<2000'],
    flow_failed: ['rate<0.20'],
  },
};

function safeJson(res){ try { return res.json(); } catch { return null; } }
function pick(arr){ return Array.isArray(arr) && arr.length ? arr[Math.floor(Math.random()*arr.length)] : null; }

function get(url, token) {
  const headers = token ? { Authorization: `Bearer ${token}` } : {};
  const res = http.get(url, { headers });
  return { res, json: safeJson(res) };
}

function post(url, body, token) {
  const headers = token
    ? { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }
    : { 'Content-Type': 'application/json' };
  const res = http.post(url, JSON.stringify(body), { headers });
  return { res, json: safeJson(res) };
}

// login/register once and share token + userId
export function setup() {
  let email = USER_EMAIL;
  let password = USER_PASSWORD;
  if (!email) {
    email = `user_${Math.random().toString(36).slice(2)}@test.com`;
    // register USER
    const reg = post(`${AUTH_URL}/register`, { email, password, role: 'USER' }, null);
    check(reg.res, { 'register 2xx': r => r.status >= 200 && r.status < 300 }) || fail('register failed');
  }
  const login = post(`${AUTH_URL}/login`, { email, password }, null);
  check(login.res, { 'login 2xx': r => r.status >= 200 && r.status < 300 }) || fail('login failed');
  const token = login.json?.token || login.json?.accessToken;
  check(token, { 'got token': t => !!t }) || fail('no token');
  return { token, userId: Number(__ENV.USER_ID || 1) };
}

function browseFlow(token) {
  const t0 = Date.now();
  group('browse:list-products', () => {
    const { res, json } = get(PRODUCT_URL, token);
    check(res, { 'GET /products 200': r => r.status === 200 });
    check(json, { 'not empty': data => Array.isArray(data) && data.length > 0 });
  });
  flowBrowseMs.add(Date.now() - t0);
  sleep(Math.random() * 0.3 + 0.1);
}

function checkoutFlow(ctx) {
  const { token, userId } = ctx;
  const t0 = Date.now();
  try {
    const list = get(PRODUCT_URL, token).json || [];
    const item = pick(list);
    if (!item) throw new Error('no products');
    const productId = item.id ?? item.productId ?? 1;
    const qty = 1;

    const add = post(`${CART_URL}/add`, { productId, quantity: qty }, token);
    check(add.res, { 'POST /cart/add 2xx': r => r.status >= 200 && r.status < 300 });

    const cart = get(CART_URL, token);
    check(cart.res, { 'GET /cart 200': r => r.status === 200 });

    const order = post(ORDER_URL, { userId, items: [{ productId, quantity: qty }], status: 'PENDING' }, token);
    check(order.res, { 'POST /orders 2xx': r => r.status >= 200 && r.status < 300 });

    flowFailed.add(0);
  } catch (_) {
    flowFailed.add(1);
  } finally {
    flowCheckoutMs.add(Date.now() - t0);
  }
}

export default function (ctx) {
  if (__ENV.K6_SCENARIO === 'browse') browseFlow(ctx.token);
  else checkoutFlow(ctx);
  sleep(Math.random() * 0.3 + 0.2);
}

/* Run:
docker compose run --rm \
  k6 run --out experimental-prometheus-rw=http://prometheus:9090/api/v1/write \
  /scripts/checkout-login.js
*/
