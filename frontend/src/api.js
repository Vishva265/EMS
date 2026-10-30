// All server communication lives here. The JWT stays in an HttpOnly cookie.

let csrfToken;
let onSessionExpired = () => {};
export function setSessionHandler(handler) {
  onSessionExpired = handler;
}

export async function api(path, options = {}) {
  const method = options.method || 'GET';
  if (method !== 'GET' && !csrfToken) {
    const response = await fetch('/api/auth/csrf', { credentials: 'same-origin' });
    csrfToken = (await response.json()).token;
  }
  const response = await fetch(`/api${path}`, {
    ...options,
    credentials: 'same-origin',
    headers: {
      'Content-Type': 'application/json',
      ...(method !== 'GET' ? { 'X-XSRF-TOKEN': csrfToken } : {}),
      ...options.headers,
    },
    body: options.body ? JSON.stringify(options.body) : undefined,
  });
  if (response.status === 204) return null;
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    if (response.status === 401 && path !== '/auth/login') onSessionExpired();
    const error = new Error(data.detail || 'Unable to connect. Please try again.');
    error.fields = data.errors || {};
    error.status = response.status;
    throw error;
  }
  return data;
}
export function resetSecurityToken() {
  csrfToken = undefined;
}
export const money = (value) =>
  new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(value);
export const dateLabel = (value) =>
  value
    ? new Date(`${value}T00:00:00`).toLocaleDateString('en-IN', {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
      })
    : '—';
