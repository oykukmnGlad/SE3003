(async function () {
  const user = await requireLogin();
  if (!user) return;

  const dateInput = document.getElementById('booking-date');
  const grid = document.getElementById('slots-grid');
  const confirmBtn = document.getElementById('confirm-btn');
  let selectedSlot = null;

  // Backend en fazla 30 gün ilerisini gösteriyor
  const today = new Date();
  const maxDate = new Date();
  maxDate.setDate(maxDate.getDate() + 30);
  dateInput.min = toDateString(today);
  dateInput.max = toDateString(maxDate);
  dateInput.value = toDateString(today);

  function rangeText(slot) {
    return formatTime(slot.start) + ' - ' + formatTime(slot.end);
  }

  function renderSlots(slots) {
    grid.textContent = '';
    if (slots.length === 0) {
      const p = document.createElement('p');
      p.className = 'empty-text';
      p.textContent = 'No slots for this date.';
      grid.appendChild(p);
      return;
    }
    slots.forEach((slot) => {
      const btn = document.createElement('button');
      btn.type = 'button';
      if (slot.available) {
        btn.className = 'slot available';
        btn.textContent = rangeText(slot);
        btn.addEventListener('click', () => selectSlot(slot, btn));
      } else {
        btn.className = 'slot booked';
        btn.disabled = true;
        btn.textContent = rangeText(slot) + ' (Unavailable)';
      }
      grid.appendChild(btn);
    });
  }

  function selectSlot(slot, btn) {
    grid.querySelectorAll('.slot.selected').forEach((b) => b.classList.remove('selected'));
    btn.classList.add('selected');
    selectedSlot = slot;
    confirmBtn.disabled = false;
  }

  async function loadSlots() {
    selectedSlot = null;
    confirmBtn.disabled = true;
    if (!dateInput.value) {
      grid.textContent = '';
      return;
    }
    grid.textContent = 'Loading...';
    try {
      const slots = await api('/api/slots?date=' + encodeURIComponent(dateInput.value));
      renderSlots(slots);
    } catch (err) {
      grid.textContent = '';
      showMessage(err.message);
    }
  }

  dateInput.addEventListener('change', () => {
    clearMessage();
    loadSlots();
  });

  confirmBtn.addEventListener('click', async () => {
    if (!selectedSlot) {
      showMessage('Please select a time slot first.');
      return;
    }
    clearMessage();
    confirmBtn.disabled = true;
    const booked = selectedSlot;
    try {
      await api('/api/bookings', {
        method: 'POST',
        body: JSON.stringify({ slotId: booked.id }),
      });
      await loadSlots();
      showMessage('Reservation confirmed: ' + formatDate(booked.start) + ', ' + rangeText(booked), 'success');
    } catch (err) {
      if (err.status === 401) {
        window.location.href = 'index.html';
        return;
      }
      if (err.status === 409) {
        await loadSlots(); // başkası almış olabilir, takvimi tazele
      }
      showMessage(err.message);
    } finally {
      confirmBtn.disabled = !selectedSlot;
    }
  });

  loadSlots();
})();
