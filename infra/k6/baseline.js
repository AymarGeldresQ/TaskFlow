import http from 'k6/http';
import { check, sleep } from 'k6';
import { randomString } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

export const options = {
  stages: [
    { duration: '30s', target: 10 },  // ramp up
    { duration: '1m',  target: 10 },  // steady state
    { duration: '15s', target: 0  },  // ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],  // 95th percentile under 500ms
    http_req_failed:   ['rate<0.01'],  // error rate under 1%
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

function registerAndLogin() {
  const email = `user-${randomString(8)}@k6.test`;
  const password = 'TestPassword123!';

  const reg = http.post(`${BASE_URL}/api/v1/auth/register`, JSON.stringify({
    email,
    password,
    fullName: 'K6 User',
  }), { headers: { 'Content-Type': 'application/json' } });

  check(reg, { 'register 201': (r) => r.status === 201 });
  return JSON.parse(reg.body).accessToken;
}

function authHeaders(token) {
  return {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  };
}

export default function () {
  const token = registerAndLogin();
  const headers = authHeaders(token);

  // Create team
  const teamRes = http.post(`${BASE_URL}/api/v1/teams`, JSON.stringify({
    name: `Team ${randomString(6)}`,
  }), { headers });
  check(teamRes, { 'create team 201': (r) => r.status === 201 });
  const teamId = JSON.parse(teamRes.body).id;

  // Create project
  const projectRes = http.post(`${BASE_URL}/api/v1/teams/${teamId}/projects`, JSON.stringify({
    name: `Project ${randomString(6)}`,
  }), { headers });
  check(projectRes, { 'create project 201': (r) => r.status === 201 });
  const projectId = JSON.parse(projectRes.body).id;

  // Create task
  const taskRes = http.post(`${BASE_URL}/api/v1/projects/${projectId}/tasks`, JSON.stringify({
    title: `Task ${randomString(8)}`,
    priority: 'MEDIUM',
  }), { headers });
  check(taskRes, { 'create task 201': (r) => r.status === 201 });
  const taskId = JSON.parse(taskRes.body).id;

  // List tasks
  const listRes = http.get(`${BASE_URL}/api/v1/projects/${projectId}/tasks`, { headers });
  check(listRes, { 'list tasks 200': (r) => r.status === 200 });

  // Update task status
  const statusRes = http.patch(`${BASE_URL}/api/v1/tasks/${taskId}/status`, JSON.stringify({
    status: 'IN_PROGRESS',
  }), { headers });
  check(statusRes, { 'update status 200': (r) => r.status === 200 });

  // Add comment
  const commentRes = http.post(`${BASE_URL}/api/v1/tasks/${taskId}/comments`, JSON.stringify({
    body: 'k6 baseline comment',
  }), { headers });
  check(commentRes, { 'add comment 201': (r) => r.status === 201 });

  sleep(1);
}
