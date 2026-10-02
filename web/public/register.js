
const PLANS = {
  bronze: {
    name: 'Bronze',
    icon: '<i class="ti ti-medal"></i>',
    badgeClass: 'bronze',
    priceMonthly: '297',
    priceAnnual: '247',
    features: [
      '1 CNPJ · 1 filial',
      'NF-e e NFC-e ilimitadas',
      'PDV — 1 caixa',
      'Estoque e Compras',
      '3 usuários incluídos',
    ],
  },
  silver: {
    name: 'Silver',
    icon: '<i class="ti ti-star"></i>',
    badgeClass: 'silver',
    priceMonthly: '597',
    priceAnnual: '497',
    features: [
      '1 CNPJ + até 3 filiais',
      'NF-e, NFC-e e NFS-e ilimitadas',
      'PDV — até 5 caixas',
      'CRM completo + WhatsApp API',
      '10 usuários incluídos',
    ],
  },
  gold: {
    name: 'Gold',
    icon: '<i class="ti ti-crown"></i>',
    badgeClass: 'gold',
    priceMonthly: '1.197',
    priceAnnual: '997',
    features: [
      'CNPJs e filiais ilimitados',
      'Todos os módulos incluídos',
      'PDV — caixas ilimitados',
      'API REST + Webhooks',
      'Suporte 24/7 + gerente dedicado',
    ],
  },
};

const params   = new URLSearchParams(window.location.search);
const planKey  = (params.get('plan') || 'silver').toLowerCase();
const billing  = (params.get('billing') || 'monthly').toLowerCase();
const plan     = PLANS[planKey] || PLANS.silver;
const isAnnual = billing === 'annual';

function buildPlanSummary() {
  const price = isAnnual ? plan.priceAnnual : plan.priceMonthly;
  const billingLabel = isAnnual ? 'Cobrança anual · 2 meses grátis' : 'Cobrança mensal';

  const featuresHtml = plan.features
    .map(f => `
      <div class="ps-feature">
        <div class="ps-feature-icon"><i class="ti ti-check"></i></div>
        <span>${f}</span>
      </div>`)
    .join('');

  document.getElementById('plan-summary').innerHTML = `
    <div class="ps-header">
      <div class="ps-plan-info">
        <div class="ps-badge ${plan.badgeClass}">${plan.icon}</div>
        <div>
          <div class="ps-plan-name">Plano ${plan.name}</div>
          <div class="ps-billing">${billingLabel}</div>
        </div>
      </div>
      <a href="index.html#pricing" class="ps-change-link">Trocar</a>
    </div>

    <div class="ps-price">
      <span class="ps-price-currency">R$</span>
      <span class="ps-price-amount">${price}</span>
      <span class="ps-price-period">/mês</span>
    </div>

    <div class="ps-divider"></div>

    <div class="ps-features">${featuresHtml}</div>

    <div class="ps-trial">
      <i class="ti ti-gift"></i>
      14 dias grátis · sem cartão de crédito
    </div>
  `;
}

buildPlanSummary();

let currentStep = 1;

function goToStep(step) {
  document.getElementById('form-step-1').classList.add('hidden');
  document.getElementById('form-step-2').classList.add('hidden');
  document.getElementById('form-step-3').classList.add('hidden');

  document.getElementById(`form-step-${step}`).classList.remove('hidden');

  [1, 2, 3].forEach(n => {
    const dot = document.getElementById(`step-dot-${n}`);
    dot.classList.remove('active', 'done');
    if (n < step)  dot.classList.add('done');
    if (n === step) dot.classList.add('active');
  });

  const lines = document.querySelectorAll('.step-line');
  lines.forEach((line, i) => {
    line.classList.toggle('done', i + 1 < step);
  });

  currentStep = step;
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function setError(fieldId, errId, msg) {
  const field = document.getElementById(fieldId);
  const err   = document.getElementById(errId);
  if (msg) {
    field.classList.add('invalid');
    field.classList.remove('valid');
    err.textContent = msg;
    return false;
  }
  field.classList.remove('invalid');
  field.classList.add('valid');
  err.textContent = '';
  return true;
}

function validateEmail(v) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v);
}

function validateCNPJ(raw) {
  const n = raw.replace(/\D/g, '');
  if (n.length !== 14) return false;
  if (/^(\d)\1+$/.test(n)) return false;

  function calcDigit(digits, weights) {
    let sum = 0;
    for (let i = 0; i < digits.length; i++) sum += parseInt(digits[i]) * weights[i];
    const rem = sum % 11;
    return rem < 2 ? 0 : 11 - rem;
  }

  const d1 = calcDigit(n.slice(0,12), [5,4,3,2,9,8,7,6,5,4,3,2]);
  const d2 = calcDigit(n.slice(0,13), [6,5,4,3,2,9,8,7,6,5,4,3,2]);
  return parseInt(n[12]) === d1 && parseInt(n[13]) === d2;
}

const pwInput     = document.getElementById('password');
const strengthBox = document.getElementById('pw-strength');
const strengthFill = document.getElementById('strength-fill');
const strengthLabel = document.getElementById('strength-label');

pwInput.addEventListener('input', () => {
  const v = pwInput.value;
  if (!v) { strengthBox.classList.remove('visible'); return; }
  strengthBox.classList.add('visible');

  let score = 0;
  if (v.length >= 8)  score++;
  if (v.length >= 12) score++;
  if (/[A-Z]/.test(v)) score++;
  if (/[0-9]/.test(v)) score++;
  if (/[^A-Za-z0-9]/.test(v)) score++;

  const levels = [
    { label: 'Fraca',    color: '#DC2626', width: '20%' },
    { label: 'Razoável', color: '#D97706', width: '40%' },
    { label: 'Boa',      color: '#F59E0B', width: '60%' },
    { label: 'Forte',    color: '#059669', width: '80%' },
    { label: 'Ótima',    color: '#059669', width: '100%' },
  ];

  const lvl = levels[Math.max(0, Math.min(score - 1, 4))];
  strengthFill.style.width = lvl.width;
  strengthFill.style.backgroundColor = lvl.color;
  strengthLabel.textContent = lvl.label;
  strengthLabel.style.color = lvl.color;
});

document.getElementById('eye-toggle').addEventListener('click', () => {
  const isText = pwInput.type === 'text';
  pwInput.type = isText ? 'password' : 'text';
  document.getElementById('eye-icon').className = isText ? 'ti ti-eye' : 'ti ti-eye-off';
});

document.getElementById('cnpj').addEventListener('input', function () {
  let v = this.value.replace(/\D/g, '').slice(0, 14);
  if (v.length > 12)      v = v.replace(/^(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})$/, '$1.$2.$3/$4-$5');
  else if (v.length > 8)  v = v.replace(/^(\d{2})(\d{3})(\d{3})(\d+)$/, '$1.$2.$3/$4');
  else if (v.length > 5)  v = v.replace(/^(\d{2})(\d{3})(\d+)$/, '$1.$2.$3');
  else if (v.length > 2)  v = v.replace(/^(\d{2})(\d+)$/, '$1.$2');
  this.value = v;
});

document.getElementById('phone').addEventListener('input', function () {
  let v = this.value.replace(/\D/g, '').slice(0, 11);
  if (v.length > 10)      v = v.replace(/^(\d{2})(\d{5})(\d{4})$/, '($1) $2-$3');
  else if (v.length > 6)  v = v.replace(/^(\d{2})(\d{4})(\d+)$/, '($1) $2-$3');
  else if (v.length > 2)  v = v.replace(/^(\d{2})(\d+)$/, '($1) $2');
  this.value = v;
});

document.getElementById('form1').addEventListener('submit', function (e) {
  e.preventDefault();

  const name  = document.getElementById('full-name').value.trim();
  const email = document.getElementById('email').value.trim();
  const pw    = document.getElementById('password').value;

  let valid = true;

  if (!name || name.split(' ').filter(Boolean).length < 2) {
    valid = setError('full-name', 'err-name', 'Por favor, informe seu nome completo.') && valid;
  } else {
    setError('full-name', 'err-name', '');
  }

  if (!validateEmail(email)) {
    valid = setError('email', 'err-email', 'Informe um e-mail válido.') && valid;
  } else {
    setError('email', 'err-email', '');
  }

  if (pw.length < 8) {
    valid = setError('password', 'err-password', 'A senha precisa ter pelo menos 8 caracteres.') && valid;
  } else {
    setError('password', 'err-password', '');
  }

  if (valid) goToStep(2);
});

document.getElementById('btn-back').addEventListener('click', () => goToStep(1));

document.getElementById('form2').addEventListener('submit', async function (e) {
  e.preventDefault();

  const company = document.getElementById('company-name').value.trim();
  const cnpj    = document.getElementById('cnpj').value.trim();

  let valid = true;

  if (!company) {
    valid = setError('company-name', 'err-company', 'Informe o nome da empresa.') && valid;
  } else {
    setError('company-name', 'err-company', '');
  }

  if (!validateCNPJ(cnpj)) {
    valid = setError('cnpj', 'err-cnpj', 'CNPJ inválido. Verifique e tente novamente.') && valid;
  } else {
    setError('cnpj', 'err-cnpj', '');
  }

  if (!valid) return;

  const btnText   = document.getElementById('btn-submit-text');
  const btnIcon   = document.getElementById('btn-submit-icon');
  const spinner   = document.getElementById('btn-spinner');
  const submitBtn = document.getElementById('btn-submit');

  const setSubmitting = submitting => {
    btnText.textContent = submitting ? 'Criando conta…' : 'Criar conta';
    btnIcon.classList.toggle('hidden', submitting);
    spinner.classList.toggle('hidden', !submitting);
    submitBtn.disabled = submitting;
  };

  const errForm = document.getElementById('err-form');
  errForm.textContent = '';
  setSubmitting(true);

  try {
    const response = await fetch('/api/signup', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify({
        fullName:    document.getElementById('full-name').value.trim(),
        email:       document.getElementById('email').value.trim(),
        password:    document.getElementById('password').value,
        companyName: company,
        cnpj,
        phone:       document.getElementById('phone').value.trim(),
        plan:        params.get('plan') || undefined,
        billing:     params.get('billing') || undefined,
      }),
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({}));
      showSignupError(response.status, error);
      setSubmitting(false);
      return;
    }
  } catch (networkError) {
    errForm.textContent = 'Não foi possível criar a conta agora. Tente novamente em instantes.';
    setSubmitting(false);
    return;
  }

  document.getElementById('confirm-email').textContent =
    document.getElementById('email').value.trim();

  goToStep(3);
});

// A 400 names the offending field: the email lives on step 1, so bring the user back to it.
function showSignupError(status, error) {
  if (status === 400 && error.field === 'email') {
    goToStep(1);
    setError('email', 'err-email', error.message);
  } else if (status === 400 && error.field === 'cnpj') {
    setError('cnpj', 'err-cnpj', error.message);
  } else {
    document.getElementById('err-form').textContent = status === 400 && error.message
      ? error.message
      : 'Não foi possível criar a conta agora. Tente novamente em instantes.';
  }
}

document.getElementById('resend-btn').addEventListener('click', function () {
  this.textContent = 'Enviado!';
  this.disabled = true;
  setTimeout(() => {
    this.textContent = 'Reenviar';
    this.disabled = false;
  }, 5000);
});

document.getElementById('cnpj').addEventListener('blur', function () {
  const v = this.value.trim();
  if (v.length > 0 && !validateCNPJ(v)) {
    setError('cnpj', 'err-cnpj', 'CNPJ inválido. Verifique e tente novamente.');
  } else if (v.length > 0) {
    setError('cnpj', 'err-cnpj', '');
  }
});

document.getElementById('email').addEventListener('blur', function () {
  const v = this.value.trim();
  if (v.length > 0 && !validateEmail(v)) {
    setError('email', 'err-email', 'Informe um e-mail válido.');
  } else if (v.length > 0) {
    setError('email', 'err-email', '');
  }
});
