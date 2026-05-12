// ===== Config =====
const API_BASE = '';

// ===== Token / Auth helpers =====
function getToken() {
  return localStorage.getItem('token');
}

function getRole() {
  return localStorage.getItem('role') || '';
}

function getUsername() {
  return localStorage.getItem('username') || '';
}

function saveAuth(data) {
  localStorage.setItem('token', data.token || data.accessToken || '');
  localStorage.setItem('role', data.role || '');
  localStorage.setItem('username', data.username || data.email || '');
  if (data.refreshToken) localStorage.setItem('refreshToken', data.refreshToken);
}

function logout() {
  localStorage.clear();
  window.location.href = '/login.html';
}

function isLoggedIn() {
  return !!getToken();
}

function isAdmin() {
  return getRole() === 'ADMIN';
}

function isOwner() {
  return getRole() === 'OWNER' || getRole() === 'ADMIN';
}

// ===== Fetch wrappers =====
async function apiFetch(path, options = {}) {
  const token = getToken();
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
  if (token) headers['Authorization'] = 'Bearer ' + token;

  const res = await fetch(path, { ...options, headers });

  if (res.status === 401) {
    logout();
    return null;
  }

  return res;
}

async function apiGet(path) {
  const res = await apiFetch(path, { method: 'GET' });
  if (!res) return null;
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

async function apiPost(path, body) {
  const res = await apiFetch(path, { method: 'POST', body: JSON.stringify(body) });
  if (!res) return null;
  if (!res.ok) throw new Error(await res.text());
  const text = await res.text();
  return text ? JSON.parse(text) : null;
}

async function apiPut(path, body) {
  const res = await apiFetch(path, { method: 'PUT', body: JSON.stringify(body) });
  if (!res) return null;
  if (!res.ok) throw new Error(await res.text());
  const text = await res.text();
  return text ? JSON.parse(text) : null;
}

async function apiPatch(path, body) {
  const res = await apiFetch(path, { method: 'PATCH', body: body ? JSON.stringify(body) : undefined });
  if (!res) return null;
  if (!res.ok) throw new Error(await res.text());
  const text = await res.text();
  return text ? JSON.parse(text) : null;
}

async function apiDelete(path) {
  const res = await apiFetch(path, { method: 'DELETE' });
  if (!res) return null;
  if (!res.ok) throw new Error(await res.text());
  return true;
}

// ===== Formatting =====
function formatPrice(price) {
  return Number(price).toLocaleString('ru-RU') + '\u00a0₽';
}

function formatDate(dateStr) {
  if (!dateStr) return '—';
  const d = new Date(dateStr);
  return d.toLocaleDateString('ru-RU', { day: '2-digit', month: 'long', year: 'numeric' });
}

function fieldTypeName(type) {
  const map = { FOOTBALL: 'Футбол', BASKETBALL: 'Баскетбол', VOLLEYBALL: 'Волейбол', TENNIS: 'Теннис', PADEL: 'Падел', HOCKEY: 'Хоккей' };
  return map[(type||'').toUpperCase()] || type || '—';
}

// ===== Pitch illustration (CSS gradient) =====
function pitchGradient(type) {
  const t = (type || '').toUpperCase();
  if (t === 'BASKETBALL') return 'linear-gradient(135deg,#c0722a 0%,#9c5a1c 100%)';
  if (t === 'TENNIS')     return 'linear-gradient(135deg,#2563a8 0%,#1a4a8a 100%)';
  if (t === 'VOLLEYBALL') return 'linear-gradient(135deg,#d97706 0%,#b45309 100%)';
  return 'linear-gradient(160deg,#1B6B2E 0%,#145522 60%,#0f3d18 100%)';
}

function pitchLines() {
  return `<div style="position:absolute;inset:0;display:flex;align-items:center;justify-content:center;">
    <div style="width:62%;height:62%;border:2px solid rgba(255,255,255,.35);border-radius:3px;"></div>
    <div style="position:absolute;width:26%;height:26%;border:2px solid rgba(255,255,255,.25);border-radius:50%;"></div>
    <div style="position:absolute;top:50%;left:0;right:0;height:2px;background:rgba(255,255,255,.18);transform:translateY(-50%);"></div>
  </div>`;
}
