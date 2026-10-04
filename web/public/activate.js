// The e-mailed link hits the API, which validates the token and redirects here with the outcome in ?status=.
// The token never reaches this page; the status is also taken out of the address bar so a refresh or a shared
// URL does not replay a stale outcome.
const activationStatus = new URLSearchParams(window.location.search).get('status');
window.history.replaceState(null, '', window.location.pathname);

// Approved copy per outcome (UC-M10-14), keyed by the `status` the API redirects with. `resend` is 'primary' when asking for a new link is the way forward,
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
    message: 'Tente novamente em instantes, clicando outra vez no link do e-mail.',
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
    : null;
  if (next) next.focus();
}

// Anything the API did not say explicitly (no status, a typo, a hand-edited URL) is an unusable link.
function outcomeFor(value) {
  return Object.hasOwn(OUTCOMES, value) ? value : 'invalid';
}

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
showOutcome(outcomeFor(activationStatus));
