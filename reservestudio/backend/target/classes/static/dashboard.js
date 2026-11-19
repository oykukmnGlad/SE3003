(async function () {
  const user = await requireLogin();
  if (!user) return;

  const upcomingEl = document.getElementById('upcoming-list');
  const pastEl = document.getElementById('past-list');

  function line(label, value) {
    const p = document.createElement('p');
    const strong = document.createElement('strong');
    strong.textContent = label + ' ';
    p.appendChild(strong);
    p.appendChild(document.createTextNode(value));
    return p;
  }

  function render(container, bookings, cancellable, emptyText) {
    container.textContent = '';
    if (bookings.length === 0) {
      const p = document.createElement('p');
      p.className = 'empty-text';
      p.textContent = emptyText;
      container.appendChild(p);
      return;
    }
    bookings.forEach((b) => {
      const card = document.createElement('div');
      card.className = 'reservation-card' + (cancellable ? '' : ' past');
      card.appendChild(line('Studio:', 'Main Recording Studio'));
      card.appendChild(line('Date:',
        formatDate(b.slotStart) + ' (' + formatTime(b.slotStart) + ' - ' + formatTime(b.slotEnd) + ')'));

      if (cancellable) {
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'cancel-btn';
        btn.textContent = 'Cancel Booking';
        btn.addEventListener('click', () => cancelBooking(b, btn));
        card.appendChild(btn);
      }
      container.appendChild(card);
    });
  }

  async function load() {
    try {
      const bookings = await api('/api/bookings/my'); // slot başlangıcına göre artan sırada
      const now = new Date();
      const upcoming = bookings.filter((b) => new Date(b.slotStart) > now);
      const past = bookings.filter((b) => new Date(b.slotStart) <= now).reverse();
      render(upcomingEl, upcoming, true, 'You have no upcoming reservations.');
      render(pastEl, past, false, 'No past reservations.');
    } catch (err) {
      if (err.status === 401) {
        window.location.href = 'index.html';
        return;
      }
      showMessage(err.message);
    }
  }

  async function cancelBooking(b, btn) {
    if (!window.confirm('Cancel your booking on ' + formatDate(b.slotStart) + ' at ' + formatTime(b.slotStart) + '?')) {
      return;
    }
    clearMessage();
    btn.disabled = true;
    try {
      await api('/api/bookings/' + b.id, { method: 'DELETE' });
      await load();
      showMessage('Booking cancelled.', 'success');
    } catch (err) {
      showMessage(err.message);
      btn.disabled = false;
    }
  }

  load();
})();
