document.getElementById('signup-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  clearMessage();
  const btn = e.target.querySelector('button[type="submit"]');
  btn.disabled = true;

  const email = document.getElementById('email').value;
  const password = document.getElementById('password').value;
  try {
    await api('/api/auth/signup', {
      method: 'POST',
      body: JSON.stringify({
        username: document.getElementById('username').value,
        email,
        password,
      }),
    });
    // signup oturum açmıyor, o yüzden hemen ardından login çağırıyoruz
    await api('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    });
    window.location.href = 'booking.html';
  } catch (err) {
    showMessage(err.message);
    btn.disabled = false;
  }
});
