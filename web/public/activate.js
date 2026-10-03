const token = new URLSearchParams(window.location.search).get('token');

const states = ['loading', 'success', 'failed'];

function showState(name) {
  states.forEach(s => document.getElementById(`state-${s}`).classList.toggle('hidden', s !== name));
}

function showFailure(status, error) {
  // 410 means the link was real but is spent or stale; anything else (400, a missing token, no answer) is just unusable.
  const title = status === 410 && error.reason === 'EXPIRED' ? 'Link expirado' : 'Link inválido ou expirado';
  document.getElementById('failed-title').textContent = title;
  document.getElementById('failed-message').textContent = error.message
    || 'Não foi possível ativar sua conta com este link. Peça um novo abaixo.';
  showState('failed');
}

async function activate() {
  if (!token) {
    showFailure(400, {});
    return;
  }
  try {
    const response = await fetch(`/api/activate?token=${encodeURIComponent(token)}`, {
      headers: { Accept: 'application/json' },
    });
    if (response.ok) {
      showState('success');
      return;
    }
    showFailure(response.status, await response.json().catch(() => ({})));
  } catch (networkError) {
    showFailure(0, { message: 'Não foi possível ativar sua conta agora. Tente novamente em instantes.' });
  }
}

document.getElementById('resend-form').addEventListener('submit', async function (e) {
  e.preventDefault();

  const input    = document.getElementById('resend-email');
  const error    = document.getElementById('resend-error');
  const feedback = document.getElementById('resend-feedback');
  const button   = document.getElementById('resend-btn');
  const email    = input.value.trim();

  feedback.textContent = '';
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    error.textContent = 'Informe um e-mail válido.';
    input.classList.add('invalid');
    return;
  }
  error.textContent = '';
  input.classList.remove('invalid');

  button.disabled = true;
  button.textContent = 'Enviando…';
  const result = await resendActivation(email);

  if (!result.ok) {
    error.textContent = result.message;
    button.textContent = 'Reenviar';
    button.disabled = false;
    return;
  }
  feedback.textContent = result.message;
  button.textContent = 'Enviado!';
  setTimeout(() => {
    button.textContent = 'Reenviar';
    button.disabled = false;
  }, RESEND_COOLDOWN_MS);
});

activate();
