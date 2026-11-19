// Ortak yardımcılar: backend çağrıları, mesaj kutusu, menü
// Backend adresi sayfanın geldiği adrese göre belirlenir (oturum cookie'si için host aynı kalmalı):
//  - Arayüz backend'den açıldıysa (http://localhost:8080)  -> aynı origin, '' kullanılır
//  - Ayrı sunucudan açıldıysa (Live Server 5500, http.server 8000, vite 5173 ...) -> aynı host, 8080 port
//  - Dosya olarak açıldıysa (file://) -> CORS/cookie çalışmaz; yine de varsayılan adres döner
const API_BASE = (function () {
  const { protocol, hostname, port } = window.location;
  if (protocol !== 'http:' && protocol !== 'https:') return 'http://localhost:8080';
  if (port === '8080') return '';
  return protocol + '//' + hostname + ':8080';
})();

async function api(path, options = {}) {
  let res;
  try {
    res = await fetch(API_BASE + path, {
      credentials: 'include', // JSESSIONID cookie'si için şart
      headers: { 'Content-Type': 'application/json' },
      ...options,
    });
  } catch (e) {
    const err = new Error('Cannot reach the server. Is the backend running?');
    err.status = 0;
    throw err;
  }

  if (res.status === 204) return null; // logout ve iptalde gövde yok

  let data = null;
  try {
    data = await res.json();
  } catch (e) {
    // gövde boş ya da JSON değil
  }

  if (!res.ok) {
    const err = new Error((data && data.message) || 'Something went wrong (' + res.status + ')');
    err.status = res.status;
    throw err;
  }
  return data;
}

function showMessage(text, type = 'error') {
  const el = document.getElementById('message');
  if (!el) return;
  el.textContent = text;
  el.className = 'message ' + type;
}

function clearMessage() {
  const el = document.getElementById('message');
  if (el) el.textContent = '';
}

// Yerel saate göre YYYY-MM-DD (toISOString UTC verdiği için kullanmıyoruz)
function toDateString(d) {
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return d.getFullYear() + '-' + m + '-' + day;
}

// "2026-11-23T09:00:00" -> "09:00"
function formatTime(iso) {
  return iso.slice(11, 16);
}

// "2026-11-23T09:00:00" -> "23 Nov 2026"
function formatDate(iso) {
  return new Date(iso).toLocaleDateString('en-GB', {
    day: '2-digit', month: 'short', year: 'numeric',
  });
}

// Giriş yoksa index.html'e yönlendirir; varsa kullanıcıyı döndürür
async function requireLogin() {
  try {
    const user = await api('/api/auth/me');
    renderNav(user);
    return user;
  } catch (err) {
    if (err.status === 401) {
      window.location.href = 'index.html';
    } else {
      showMessage(err.message);
    }
    return null;
  }
}

function renderNav(user) {
  const nav = document.getElementById('nav');
  if (!nav) return;
  nav.textContent = '';

  [['booking.html', 'Book a Slot'], ['dashboard.html', 'My Bookings']].forEach(([href, label]) => {
    const a = document.createElement('a');
    a.href = href;
    a.textContent = label;
    if (window.location.pathname.endsWith(href)) a.className = 'active';
    nav.appendChild(a);
  });

  const who = document.createElement('span');
  who.className = 'user';
  who.textContent = user.username;
  nav.appendChild(who);

  const logout = document.createElement('a');
  logout.href = '#';
  logout.textContent = 'Logout';
  logout.addEventListener('click', async (e) => {
    e.preventDefault();
    try {
      await api('/api/auth/logout', { method: 'POST' });
    } finally {
      window.location.href = 'index.html';
    }
  });
  nav.appendChild(logout);
}
