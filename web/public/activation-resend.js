// Shared by register.js and activate.js: asks the API for a new activation e-mail.
// The API answers 202 with the same message whether or not the address has a pending account, so the caller
// must show that message as-is and never imply the e-mail exists.
async function resendActivation(email) {
  try {
    const response = await fetch('/api/activate/resend', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify({ email }),
    });
    if (response.ok) {
      const body = await response.json().catch(() => ({}));
      return { ok: true, message: body.message || 'Se este e-mail estiver aguardando ativação, enviaremos um novo link em instantes.' };
    }
    if (response.status === 400) {
      return { ok: false, message: 'Informe um e-mail válido.' };
    }
  } catch (networkError) {
    // falls through to the generic message
  }
  return { ok: false, message: 'Não foi possível reenviar agora. Tente novamente em instantes.' };
}

// The server ignores a second request for the same e-mail within this window, so the buttons wait it out too.
const RESEND_COOLDOWN_MS = 60000;
