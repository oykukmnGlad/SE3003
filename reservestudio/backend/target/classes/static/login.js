document.getElementById('login-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  clearMessage();
  const btn = e.target.querySelector('button[type="submit"]');
  btn.disabled = true;
  try {
    await api('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({
        email: document.getElementById('email').value,
        password: document.getElementById('password').value,
      }),
    });
    window.location.href = 'booking.html';
  } catch (err) {
    showMessage(err.message);
    btn.disabled = false;
  }
});
