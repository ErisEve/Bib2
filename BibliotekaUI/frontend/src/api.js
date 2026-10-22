export function logout() {
  for (const key of ['jwtToken', 'userEmail', 'email', 'role', 'userId', 'currentBookId']) localStorage.removeItem(key);
  window.location.assign('/login');
}

export async function api(path, { method = 'GET', body, authenticated = true } = {}) {
  const token = localStorage.getItem('jwtToken');
  const response = await fetch(path, {
    method,
    headers: {
      Accept: 'application/json',
      ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
      ...(authenticated && token ? { Authorization: `Bearer ${token}` } : {}),
    },
    ...(body !== undefined ? { body: JSON.stringify(body) } : {}),
    signal: AbortSignal.timeout(60000),
  });
  const text = await response.text();
  let data = text;
  try { data = text ? JSON.parse(text) : null; } catch { /* Some endpoints return plain text. */ }
  if (!response.ok) {
    if (response.status === 401 && authenticated) logout();
    throw new Error(data?.message || (typeof data === 'string' && data) || `Request failed (${response.status})`);
  }
  return data;
}

export function collection(data) {
  if (Array.isArray(data)) return data;
  return Object.values(data || {}).find(Array.isArray) || [];
}
