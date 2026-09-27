
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

const PRICES = {
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
  document.getElementById('price-bronze').textContent = set.bronze;
  document.getElementById('price-silver').textContent = set.silver;
  document.getElementById('price-gold').textContent   = set.gold;

  const billing = isAnnual ? 'annual' : 'monthly';
  ['bronze', 'silver', 'gold'].forEach(plan => {
    const cta = document.getElementById('cta-' + plan);
    if (cta) cta.href = `register.html?plan=${plan}&billing=${billing}`;
  });
}

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
