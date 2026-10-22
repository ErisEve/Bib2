// Load scenario for the availability experiments (see experiments/README.md).
//
//   k6 run -e RATE=20 -e DURATION=6m experiments/k6/load.js
//
// Two open-model scenarios (constant-arrival-rate): new requests keep arriving
// at the configured rate even while earlier ones hang, so an outage shows up as
// errors and timeouts instead of as a quiet drop in throughput.
//
//   reads   RATE/s, a round-robin of GET /api/books/booksAll, /api/lendings and
//           /api/seats with one bearer token. Each of them also crosses the
//           gateway a second time: the service's JwtFilter looks the user up in
//           user-service through the gateway.
//   logins  1/s POST /api/auth/login - bcrypt in user-service.
//
// Read-only, so the data is identical between runs. Only HTTP 200 counts as a
// success.
//
// Environment: BASE_URL (http://10.100.50.11:8080), RATE (20), DURATION (10m),
// LOGIN_RATE (1), EMAIL (test@example.com), PASSWORD (123).
import http from 'k6/http';
import exec from 'k6/execution';
import { sleep } from 'k6';
import { Rate } from 'k6/metrics';

const BASE_URL = (__ENV.BASE_URL || 'http://10.100.50.11:8080').replace(/\/$/, '');
const RATE = parseInt(__ENV.RATE || '20', 10);
const LOGIN_RATE = parseInt(__ENV.LOGIN_RATE || '1', 10);
const DURATION = __ENV.DURATION || '10m';
const EMAIL = __ENV.EMAIL || 'test@example.com';
const PASSWORD = __ENV.PASSWORD || '123';
const TIMEOUT = '10s';

const READ_PATHS = ['/api/books/booksAll', '/api/lendings', '/api/seats'];

const success = new Rate('success');

// http_req_failed (and the default expected_response tag) should treat anything
// but 200 as a failure, not just >= 400.
http.setResponseCallback(http.expectedStatuses(200));

// A 10 s timeout at RATE/s can keep up to 10*RATE requests in flight.
const maxVUs = (rate) => Math.max(10, rate * 12);

export const options = {
  scenarios: {
    reads: {
      executor: 'constant-arrival-rate',
      exec: 'reads',
      rate: RATE,
      timeUnit: '1s',
      duration: DURATION,
      preAllocatedVUs: Math.max(5, RATE),
      maxVUs: maxVUs(RATE),
    },
    logins: {
      executor: 'constant-arrival-rate',
      exec: 'logins',
      rate: LOGIN_RATE,
      timeUnit: '1s',
      duration: DURATION,
      preAllocatedVUs: 2,
      maxVUs: maxVUs(LOGIN_RATE),
    },
  },
  setupTimeout: '180s',
  summaryTrendStats: ['avg', 'p(50)', 'p(95)', 'p(99)', 'max', 'count'],
  // Never-failing thresholds, only so the per-scenario and per-path
  // sub-metrics appear in the end-of-test summary (--summary-export).
  thresholds: Object.fromEntries(
    [
      'http_req_duration{scenario:reads}',
      'http_req_duration{scenario:logins}',
      ...READ_PATHS.map((p) => `http_req_duration{name:${p}}`),
      'success{scenario:reads}',
      'success{scenario:logins}',
    ].map((m) => [m, [m.startsWith('success') ? 'rate>=0' : 'max>=0']]),
  ),
};

function login() {
  return http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({ email: EMAIL, password: PASSWORD }), {
    headers: { 'Content-Type': 'application/json' },
    timeout: TIMEOUT,
    tags: { name: '/api/auth/login' },
  });
}

// One login for the read scenario. Retries, because a run may start while the
// stack is still settling after a reset.
export function setup() {
  for (let attempt = 1; attempt <= 30; attempt++) {
    const res = login();
    if (res.status === 200) {
      return { token: res.json('token') };
    }
    console.warn(`setup: login attempt ${attempt} -> ${res.status}`);
    sleep(5);
  }
  exec.test.abort('setup: could not log in');
}

export function reads(data) {
  const path = READ_PATHS[exec.scenario.iterationInTest % READ_PATHS.length];
  const res = http.get(`${BASE_URL}${path}`, {
    headers: { Authorization: `Bearer ${data.token}` },
    timeout: TIMEOUT,
    tags: { name: path },
  });
  success.add(res.status === 200);
}

export function logins() {
  const res = login();
  success.add(res.status === 200);
}
