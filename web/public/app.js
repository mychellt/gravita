
const nav = document.getElementById('nav');
window.addEventListener('scroll', () => {
  nav.classList.toggle('scrolled', window.scrollY > 8);
});

const mobileToggle = document.getElementById('mobile-toggle');
const mobileNav    = document.getElementById('mobile-nav');
mobileToggle.addEventListener('click', () => {
  mobileNav.classList.toggle('open');
  const isOpen = mobileNav.classList.contains('open');
  mobileToggle.setAttribute('aria-expanded', isOpen);
  mobileToggle.innerHTML = isOpen
    ? '<i class="ti ti-x"></i>'
    : '<i class="ti ti-menu-2"></i>';
});

mobileNav.querySelectorAll('a').forEach(link => {
  link.addEventListener('click', () => {
    mobileNav.classList.remove('open');
    mobileToggle.innerHTML = '<i class="ti ti-menu-2"></i>';
    mobileToggle.setAttribute('aria-expanded', 'false');
  });
});

// Fallback shown if GET /api/plans is unavailable; replaced by the live catalog below.
let PRICES = {
  monthly: { bronze: '297', silver: '597', gold: '1.197' },
  annual:  { bronze: '247', silver: '497', gold:   '997' },
};

let isAnnual = false;
const toggleBtn   = document.getElementById('annual-toggle');
const annualBadge = document.getElementById('annual-badge');

function toggleAnnual() {
  isAnnual = !isAnnual;
  toggleBtn.classList.toggle('active', isAnnual);
  annualBadge.classList.toggle('visible', isAnnual);

  const set = isAnnual ? PRICES.annual : PRICES.monthly;
  const billing = isAnnual ? 'annual' : 'monthly';
  Object.keys(set).forEach(plan => {
    const price = document.getElementById('price-' + plan);
    if (price) price.textContent = set[plan];
    const cta = document.getElementById('cta-' + plan);
    if (cta) cta.href = `register.html?plan=${plan}&billing=${billing}`;
  });
}

const TIER_ICONS = { bronze: 'ti-medal', silver: 'ti-star', gold: 'ti-crown' };

const formatPrice = value =>
  Number(value).toLocaleString('pt-BR', { minimumFractionDigits: 0, maximumFractionDigits: 2 });

function el(tag, className, text) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text !== undefined) node.textContent = text;
  return node;
}

function icon(name) {
  const i = el('i', 'ti ' + name);
  i.setAttribute('aria-hidden', 'true');
  return i;
}

function featureItem(label, included) {
  const li = el('li', 'feature-item ' + (included ? 'feature-yes' : 'feature-no'));
  const mark = el('span', 'feature-icon ' + (included ? 'check' : 'cross'));
  mark.appendChild(icon(included ? 'ti-check' : 'ti-minus'));
  li.append(mark, label);
  return li;
}

// Limit lines come from the plan's numeric limits (null = unlimited), then its feature list.
function planFeatureLines(plan) {
  const { cnpjs, filiais, caixasPdv, usuarios } = plan.limits ?? {};
  const lines = [];
  if (cnpjs == null && filiais == null) lines.push('CNPJs ilimitados + filiais ilimitadas');
  else if (cnpjs === 1 && filiais === 1) lines.push('1 CNPJ / 1 filial');
  else lines.push(`${cnpjs ?? 'CNPJs ilimitados'}${cnpjs == null ? '' : cnpjs === 1 ? ' CNPJ' : ' CNPJs'} + ${filiais == null ? 'filiais ilimitadas' : `até ${filiais} filiais`}`);
  lines.push(caixasPdv == null ? 'PDV — caixas ilimitados' : caixasPdv === 1 ? 'PDV — 1 caixa' : `PDV — até ${caixasPdv} caixas`);
  lines.push(usuarios == null ? 'Usuários ilimitados' : `${usuarios} usuários incluídos`);
  const included = lines.map(label => ({ label, included: true }));

  const support = plan.support;
  const supportLine = support && (support.telefone
    ? `Suporte prioritário por telefone${support.slaHoras ? ` (SLA ${support.slaHoras}h)` : ''}`
    : `Suporte por e-mail e chat${support.slaHoras ? ` (SLA ${support.slaHoras}h úteis)` : ''}`);
  const extra = supportLine ? [{ label: supportLine, included: true }] : [];
  if (support?.gerenteDedicado) extra.push({ label: 'Gerente de conta dedicado', included: true });

  return [...included, ...(plan.features ?? []), ...extra];
}

function planCard(plan) {
  const key = plan.tier.toLowerCase();
  const card = el('div', 'pricing-card' + (plan.featured ? ' featured' : ''));
  card.id = 'card-' + key;

  if (plan.featured) {
    const wrap = el('div', 'popular-badge-wrap');
    wrap.appendChild(el('span', 'popular-badge', 'Mais popular'));
    card.appendChild(wrap);
  }

  const header = el('div', 'pricing-header');
  const tier = el('div', 'plan-tier');
  const badge = el('div', 'tier-badge ' + key);
  badge.appendChild(icon(TIER_ICONS[key] ?? 'ti-medal'));
  tier.append(badge, el('span', 'plan-name', plan.name));
  const price = el('div', 'plan-price');
  const amount = el('span', 'price-amount', formatPrice(plan.priceMonthly));
  amount.id = 'price-' + key;
  price.append(el('span', 'price-currency', 'R$'), amount, el('span', 'price-period', '/mês'));
  header.append(tier, price, el('p', 'plan-desc', plan.description ?? ''));

  const features = el('div', 'pricing-features');
  const list = el('ul', 'feature-list');
  planFeatureLines(plan).forEach(f => list.appendChild(featureItem(f.label, f.included)));
  features.appendChild(list);

  const footer = el('div', 'pricing-footer');
  const cta = el('a', 'btn-plan' + (plan.featured ? ' btn-plan-featured' : ''), `Começar com ${plan.name}`);
  cta.id = 'cta-' + key;
  cta.href = `register.html?plan=${key}`;
  footer.appendChild(cta);

  card.append(header, el('div', 'pricing-divider'), features, footer);
  return card;
}

async function loadPlans() {
  const grid = document.querySelector('.pricing-grid');
  if (!grid) return;
  try {
    const response = await fetch('/api/plans', { headers: { Accept: 'application/json' } });
    if (!response.ok) throw new Error('HTTP ' + response.status);
    const plans = (await response.json())
      .filter(p => p.active)
      .sort((a, b) => a.priceMonthly - b.priceMonthly);
    if (!plans.length) return;

    grid.replaceChildren(...plans.map(planCard));
    PRICES = {
      monthly: Object.fromEntries(plans.map(p => [p.tier.toLowerCase(), formatPrice(p.priceMonthly)])),
      annual:  Object.fromEntries(plans.map(p => [p.tier.toLowerCase(), formatPrice(p.priceAnnual ?? p.priceMonthly)])),
    };
    if (isAnnual) { isAnnual = false; toggleAnnual(); }
  } catch (error) {
    console.warn('Não foi possível carregar os planos; exibindo o catálogo padrão.', error);
  }
}

loadPlans();

function toggleFaq(el) {
  const isOpen = el.classList.contains('open');
  document.querySelectorAll('.faq-item.open').forEach(item => item.classList.remove('open'));
  if (!isOpen) el.classList.add('open');
}

const sections = document.querySelectorAll('section[id]');
const navLinks  = document.querySelectorAll('.nav-links a, .mobile-nav a');

const observer = new IntersectionObserver(entries => {
  entries.forEach(entry => {
    if (entry.isIntersecting) {
      navLinks.forEach(link => {
        const href = link.getAttribute('href');
        link.style.color = href === '#' + entry.target.id
          ? 'var(--accent)'
          : '';
      });
    }
  });
}, { rootMargin: '-40% 0px -55% 0px' });

sections.forEach(s => observer.observe(s));
