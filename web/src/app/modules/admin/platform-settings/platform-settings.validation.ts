import { PlatformConfig } from '../../../core/models';
import { PLATFORM_MINIMUMS } from '../../../core/services/platform-config.service';

/** Erros indexados por caminho `secao.alvo.campo` — o primeiro segmento é a seção do menu. */
export type ValidationErrors = Record<string, string>;

const isBlank = (v: unknown) => v == null || String(v).trim() === '';
const isInt = (v: unknown): v is number => typeof v === 'number' && Number.isInteger(v);
const isNum = (v: unknown): v is number => typeof v === 'number' && !Number.isNaN(v);

export function isValidEmail(v: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v);
}

export function isValidUrl(v: string): boolean {
  try {
    const u = new URL(v);
    return u.protocol === 'https:' || u.protocol === 'http:';
  } catch {
    return false;
  }
}

export function isValidCnpj(raw: string): boolean {
  const n = raw.replace(/\D/g, '');
  if (n.length !== 14 || /^(\d)\1+$/.test(n)) return false;
  const digit = (base: string, weights: number[]) => {
    const rem = base.split('').reduce((s, d, i) => s + Number(d) * weights[i], 0) % 11;
    return rem < 2 ? 0 : 11 - rem;
  };
  const d1 = digit(n.slice(0, 12), [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]);
  const d2 = digit(n.slice(0, 13), [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]);
  return Number(n[12]) === d1 && Number(n[13]) === d2;
}

export function validatePlatformConfig(c: PlatformConfig): ValidationErrors {
  const e: ValidationErrors = {};

  // ── Planos ──
  const names = new Map<string, number>();
  c.plans.forEach(p => {
    const k = `planos.${p.id}`;
    const name = (p.name ?? '').trim().toLowerCase();
    names.set(name, (names.get(name) ?? 0) + 1);

    if (isBlank(p.name)) e[`${k}.name`] = 'Informe o nome do plano.';
    if (isBlank(p.description)) e[`${k}.description`] = 'Informe uma descrição curta.';
    if (!isNum(p.priceMonthly) || p.priceMonthly < 0) e[`${k}.priceMonthly`] = 'Informe um valor maior ou igual a zero.';
    if (!isNum(p.priceAnnual) || p.priceAnnual < 0) e[`${k}.priceAnnual`] = 'Informe um valor maior ou igual a zero.';
    else if (isNum(p.priceMonthly) && p.priceAnnual > p.priceMonthly)
      e[`${k}.priceAnnual`] = 'O valor no plano anual não pode ser maior que o mensal.';

    (Object.keys(p.limits) as (keyof typeof p.limits)[]).forEach(l => {
      const v = p.limits[l];
      if (v !== null && (!isInt(v) || v < 1)) e[`${k}.limits.${l}`] = 'Use um número inteiro ≥ 1 ou marque "Ilimitado".';
    });

    p.features.forEach((f, i) => {
      if (isBlank(f.label)) e[`${k}.features.${i}`] = 'Descreva o recurso ou remova a linha.';
    });
    if (!p.features.some(f => f.included && !isBlank(f.label)))
      e[`${k}.features`] = 'O plano precisa de ao menos um recurso incluído.';

    if (p.featured && !p.active) e[`${k}.featured`] = 'Um plano inativo não pode ser destacado.';

    // ── Suporte (por plano) ──
    const s = p.support;
    if (!s.email && !s.chat && !s.telefone) e[`suporte.${p.id}.canais`] = 'Selecione ao menos um canal.';
    if (!isInt(s.slaHoras) || s.slaHoras < 1 || s.slaHoras > 72) e[`suporte.${p.id}.slaHoras`] = 'Entre 1 e 72 horas.';
  });
  c.plans.forEach(p => {
    if (!isBlank(p.name) && (names.get(p.name.trim().toLowerCase()) ?? 0) > 1)
      e[`planos.${p.id}.name`] = 'Já existe outro plano com este nome.';
  });
  if (!c.plans.some(p => p.active)) e['planos.geral'] = 'Mantenha ao menos um plano ativo.';

  // ── Cobrança ──
  const b = c.billing;
  if (!isInt(b.trialDias) || b.trialDias < 0 || b.trialDias > 90) e['cobranca.trialDias'] = 'Entre 0 e 90 dias.';
  if (!isNum(b.taxaImplantacao) || b.taxaImplantacao < 0) e['cobranca.taxaImplantacao'] = 'Informe um valor maior ou igual a zero.';
  if (!isInt(b.fidelidadeMeses) || b.fidelidadeMeses < 0 || b.fidelidadeMeses > 36) e['cobranca.fidelidadeMeses'] = 'Entre 0 e 36 meses.';
  if (!isInt(b.mesesGratisAnual) || b.mesesGratisAnual < 0 || b.mesesGratisAnual > 11) e['cobranca.mesesGratisAnual'] = 'Entre 0 e 11 meses.';

  // ── Suporte (global) ──
  const cp = c.compliance;
  if (!isNum(cp.uptimeSla) || cp.uptimeSla < 90 || cp.uptimeSla > 100) e['suporte.uptimeSla'] = 'Entre 90 e 100%.';

  // ── Segurança & conformidade ──
  if (!isInt(cp.retencaoXmlAnos) || cp.retencaoXmlAnos < PLATFORM_MINIMUMS.retencaoXmlAnos)
    e['conformidade.retencaoXmlAnos'] = `Mínimo legal de ${PLATFORM_MINIMUMS.retencaoXmlAnos} anos.`;
  if (!isInt(cp.retencaoBackupDias) || cp.retencaoBackupDias < PLATFORM_MINIMUMS.retencaoBackupDias)
    e['conformidade.retencaoBackupDias'] = `Mínimo de ${PLATFORM_MINIMUMS.retencaoBackupDias} dias.`;
  if (!/^([01]\d|2[0-3]):[0-5]\d$/.test(cp.horarioBackup ?? '')) e['conformidade.horarioBackup'] = 'Informe um horário válido (HH:MM).';
  if (!isInt(cp.senhaMinCaracteres) || cp.senhaMinCaracteres < PLATFORM_MINIMUMS.senhaMinCaracteres || cp.senhaMinCaracteres > 64)
    e['conformidade.senhaMinCaracteres'] = `Entre ${PLATFORM_MINIMUMS.senhaMinCaracteres} e 64 caracteres.`;

  // ── Institucional ──
  const inst = c.institucional;
  if (isBlank(inst.razaoSocial)) e['institucional.razaoSocial'] = 'Informe a razão social.';
  if (!isBlank(inst.cnpj) && !isValidCnpj(inst.cnpj)) e['institucional.cnpj'] = 'CNPJ inválido.';
  if (isBlank(inst.cidadeUf)) e['institucional.cidadeUf'] = 'Informe a cidade e UF.';
  if (!isValidEmail(inst.emailContato ?? '')) e['institucional.emailContato'] = 'Informe um e-mail válido.';
  (['urlCentralAjuda', 'urlDocsApi', 'urlStatus', 'urlPrivacidade', 'urlTermos', 'urlLgpd'] as const).forEach(f => {
    if (!isBlank(inst[f]) && !isValidUrl(inst[f])) e[`institucional.${f}`] = 'Use um endereço completo, ex.: https://…';
  });

  // ── Site público ──
  c.siteStats.forEach((s, i) => {
    if (isBlank(s.valor)) e[`site.stats.${i}.valor`] = 'Obrigatório.';
    if (isBlank(s.legenda)) e[`site.stats.${i}.legenda`] = 'Obrigatório.';
  });
  c.faq.forEach((f, i) => {
    if (isBlank(f.pergunta)) e[`site.faq.${i}.pergunta`] = 'Informe a pergunta.';
    if (isBlank(f.resposta)) e[`site.faq.${i}.resposta`] = 'Informe a resposta.';
  });

  return e;
}
