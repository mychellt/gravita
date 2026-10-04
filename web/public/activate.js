// The link is single-use and its token is a secret: keep it in memory and take it out of the address bar,
// so it does not linger in the browser history or get copied with the page URL.
const token = new URLSearchParams(window.location.search).get('token');
window.history.replaceState(null, '', window.location.pathname);

const ACTIVATE_LOGIN_URL = 'app.html';

// Approved copy per outcome (UC-M10-14). `resend` is 'primary' when asking for a new link is the way forward,
// 'secondary' when logging in is, and absent when neither applies.
const OUTCOMES = {
  activated: {
    icon: 'ti-check', tone: '', event: 'activation_succeeded',
    title: 'Conta ativada',
    message: 'Tudo certo! Seu e-mail foi confirmado e você já pode entrar.',
    login: true,
  },
  expired: {
    icon: 'ti-clock-exclamation', tone: 'failed', event: 'activation_link_expired',
    title: 'Link expirado',
    message: 'Este link de ativação expirou. Peça um novo abaixo.',
    resend: 'primary',
  },
  used: {
    icon: 'ti-info-circle', tone: 'neutral', event: 'activation_link_used',
    title: 'Link já utilizado',
    message: 'Este link de ativação já foi utilizado. Se a sua conta já está ativa, é só entrar.',
    login: true, resend: 'secondary',
  },
  invalid: {
    icon: 'ti-alert-triangle', tone: 'failed', event: 'activation_link_invalid',
    title: 'Link inválido',
    message: 'Não foi possível ativar sua conta com este link. Peça um novo abaixo.',
    resend: 'primary',
  },
  unavailable: {
    icon: 'ti-plug-connected-x', tone: 'failed',
    title: 'Não foi possível ativar agora',
    message: 'Tente novamente em instantes.',
    retry: true,
  },
};

const el = id => document.getElementById(id);

// Page hook for analytics: payloads never carry the token or the e-mail address.
function track(name) {
  window.dispatchEvent(new CustomEvent('gravita:track', { detail: { name } }));
}

function show(cardId) {
  ['state-loading', 'state-result'].forEach(id => el(id).classList.toggle('hidden', id !== cardId));
}

function showOutcome(key) {
  const outcome = OUTCOMES[key];
  el('result-icon').className = `success-icon ${outcome.tone}`.trim();
  el('result-glyph').className = `ti ${outcome.icon}`;
  el('result-title').textContent = outcome.title;
  el('result-message').textContent = outcome.message;
  el('login-btn').classList.toggle('hidden', !outcome.login);
  el('retry-btn').classList.toggle('hidden', !outcome.retry);
  el('resend-form').classList.toggle('hidden', !outcome.resend);
  el('login-hint').classList.toggle('hidden', outcome.resend !== 'primary');
  show('state-result');

  // Screen readers get the title when it appears; focus goes to the control the person most likely needs next.
  el('announcer').textContent = outcome.title;
  if (outcome.event) track(outcome.event);
  focusNextAction(outcome);
}

function focusNextAction(outcome) {
  const next = outcome.resend === 'primary' ? el('resend-email')
    : outcome.login ? el('login-btn')
    : outcome.retry ? el('retry-btn')
    : null;
  if (next) next.focus();
}

// 410 means the link was real but is stale or spent; 400 or no token at all is simply unusable;
// anything else (5xx, no answer) says nothing about the link, so the person should try again.
function outcomeFor(status, body) {
  if (status === 410) return body.reason === 'ALREADY_USED' ? 'used' : body.reason === 'EXPIRED' ? 'expired' : 'invalid';
  if (status === 400) return 'invalid';
  return 'unavailable';
}

async function activate() {
  el('state-loading').setAttribute('aria-busy', 'true');
  show('state-loading');
  if (!token) {
    showOutcome('invalid');
    return;
  }
  try {
    const response = await fetch(`/api/activate?token=${encodeURIComponent(token)}`, {
      headers: { Accept: 'application/json' },
    });
    if (response.ok) {
      showOutcome('activated');
      return;
    }
    showOutcome(outcomeFor(response.status, await response.json().catch(() => ({}))));
  } catch (networkError) {
    showOutcome('unavailable');
  }
}

el('retry-btn').addEventListener('click', activate);
el('login-btn').addEventListener('click', () => track('activation_login_clicked'));

el('resend-form').addEventListener('submit', async function (e) {
  e.preventDefault();

  const input    = el('resend-email');
  const error    = el('resend-error');
  const feedback = el('resend-feedback');
  const button   = el('resend-btn');
  const email    = input.value.trim();

  feedback.textContent = '';
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    error.textContent = 'Informe um e-mail válido.';
    input.classList.add('invalid');
    input.setAttribute('aria-invalid', 'true');
    input.focus();
    return;
  }
  error.textContent = '';
  input.classList.remove('invalid');
  input.removeAttribute('aria-invalid');

  button.disabled = true;
  button.textContent = 'Enviando…';
  const result = await resendActivation(email);

  if (!result.ok) {
    error.textContent = result.message;
    button.textContent = 'Reenviar';
    button.disabled = false;
    return;
  }
  track('activation_resend_requested');
  feedback.textContent = result.message;
  button.textContent = 'Enviado!';
  setTimeout(() => {
    button.textContent = 'Reenviar';
    button.disabled = false;
  }, RESEND_COOLDOWN_MS);
});

track('activation_page_viewed');
activate();
