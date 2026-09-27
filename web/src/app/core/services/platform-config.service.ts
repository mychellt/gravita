import { Injectable, computed, signal } from '@angular/core';
import { PlatformConfig } from '../models';

/** Limites legais/de produto que o administrador não pode reduzir. */
export const PLATFORM_MINIMUMS = {
  retencaoXmlAnos: 5,       // exigência do fisco
  retencaoBackupDias: 90,   // M10 — TriggerBackupUseCase
  senhaMinCaracteres: 8,
} as const;

const DEFAULT_CONFIG: PlatformConfig = {
  plans: [
    {
      id: 'plan-bronze', tier: 'BRONZE', name: 'Bronze',
      description: 'Para pequenos varejos com até 1 CNPJ e operação simplificada.',
      priceMonthly: 297, priceAnnual: 247, featured: false, active: true,
      limits: { cnpjs: 1, filiais: 1, caixasPdv: 1, usuarios: 3 },
      features: [
        { label: 'NF-e e NFC-e ilimitadas',          included: true },
        { label: 'Estoque e Compras',                included: true },
        { label: 'Financeiro básico (CR/CP)',        included: true },
        { label: 'Dashboard executivo',              included: true },
        { label: 'NFS-e (serviços)',                 included: false },
        { label: 'Integração bancária (boleto/PIX)', included: false },
        { label: 'Integração e-commerce',            included: false },
        { label: 'API REST + Webhooks',              included: false },
      ],
      support: { email: true, chat: true, telefone: false, slaHoras: 8, horarioComercial: true, gerenteDedicado: false },
    },
    {
      id: 'plan-silver', tier: 'SILVER', name: 'Silver',
      description: 'Para varejos em crescimento com múltiplos caixas, NFS-e e integrações.',
      priceMonthly: 597, priceAnnual: 497, featured: true, active: true,
      limits: { cnpjs: 1, filiais: 3, caixasPdv: 5, usuarios: 10 },
      features: [
        { label: 'NF-e, NFC-e e NFS-e ilimitadas',     included: true },
        { label: 'Estoque, Compras e CRM completo',    included: true },
        { label: 'Financeiro + integração bancária',   included: true },
        { label: 'NFS-e multi-município',              included: true },
        { label: 'WhatsApp Business API',              included: true },
        { label: 'Relatórios avançados + DRE',         included: true },
        { label: 'Conciliação bancária (OFX/CSV)',     included: true },
        { label: 'API REST + Webhooks',                included: false },
      ],
      support: { email: true, chat: true, telefone: false, slaHoras: 8, horarioComercial: true, gerenteDedicado: false },
    },
    {
      id: 'plan-gold', tier: 'GOLD', name: 'Gold',
      description: 'Para redes de varejo e operações complexas com múltiplos CNPJs e API aberta.',
      priceMonthly: 1197, priceAnnual: 997, featured: false, active: true,
      limits: { cnpjs: null, filiais: null, caixasPdv: null, usuarios: null },
      features: [
        { label: 'NF-e, NFC-e e NFS-e ilimitadas',          included: true },
        { label: 'Todos os módulos incluídos',              included: true },
        { label: 'API REST + Webhooks',                     included: true },
        { label: 'Integração e-commerce (Shopify, VTEX)',   included: true },
        { label: 'Ambiente sandbox incluso',                included: true },
        { label: 'Exportação contábil configurável',        included: true },
      ],
      support: { email: true, chat: true, telefone: true, slaHoras: 2, horarioComercial: false, gerenteDedicado: true },
    },
  ],
  billing: {
    trialDias: 14,
    trialExigeCartao: false,
    taxaImplantacao: 0,
    fidelidadeMeses: 0,
    mesesGratisAnual: 2,
  },
  integracoes: [
    { id: 'itau',        nome: 'Itaú',              categoria: 'banco',      icon: 'ti-building-bank',    ativa: true,  planoMinimo: 'SILVER' },
    { id: 'bradesco',    nome: 'Bradesco',          categoria: 'banco',      icon: 'ti-building-bank',    ativa: true,  planoMinimo: 'SILVER' },
    { id: 'bb',          nome: 'Banco do Brasil',   categoria: 'banco',      icon: 'ti-building-bank',    ativa: true,  planoMinimo: 'SILVER' },
    { id: 'sicoob',      nome: 'Sicoob',            categoria: 'banco',      icon: 'ti-building-bank',    ativa: true,  planoMinimo: 'SILVER' },
    { id: 'sicredi',     nome: 'Sicredi',           categoria: 'banco',      icon: 'ti-building-bank',    ativa: true,  planoMinimo: 'SILVER' },
    { id: 'whatsapp',    nome: 'WhatsApp Business', categoria: 'mensageria', icon: 'ti-brand-whatsapp',   ativa: true,  planoMinimo: 'SILVER' },
    { id: 'shopify',     nome: 'Shopify',           categoria: 'ecommerce',  icon: 'ti-shopping-bag',     ativa: true,  planoMinimo: 'GOLD' },
    { id: 'vtex',        nome: 'VTEX',              categoria: 'ecommerce',  icon: 'ti-shopping-bag',     ativa: true,  planoMinimo: 'GOLD' },
    { id: 'woocommerce', nome: 'WooCommerce',       categoria: 'ecommerce',  icon: 'ti-shopping-bag',     ativa: false, planoMinimo: 'GOLD' },
  ],
  compliance: {
    retencaoXmlAnos: 5,
    retencaoBackupDias: 90,
    horarioBackup: '02:00',
    exigir2faAdmin: true,
    senhaMinCaracteres: 8,
    uptimeSla: 99.98,
  },
  institucional: {
    razaoSocial: 'Gravita Tecnologia Ltda',
    cnpj: '', // o site exibe um placeholder (00.000.000/0001-00) — preencher com o CNPJ real
    cidadeUf: 'São Paulo, SP',
    emailContato: 'contato@gravita.com.br',
    urlCentralAjuda: '',
    urlDocsApi: '',
    urlStatus: '',
    urlPrivacidade: '',
    urlTermos: '',
    urlLgpd: '',
  },
  siteStats: [
    { valor: '+2.400',   legenda: 'empresas ativas' },
    { valor: 'R$ 4,2 bi', legenda: 'em NF-e emitidas' },
    { valor: '99,98%',   legenda: 'uptime garantido' },
    { valor: '< 3s',     legenda: 'tempo médio de autorização' },
  ],
  faq: [
    { pergunta: 'Preciso instalar algum software?', resposta: 'Não. A Gravita é 100% web e acessível em qualquer navegador moderno (Chrome, Firefox, Edge, Safari). O PDV funciona em modo offline e sincroniza automaticamente ao reconectar à internet.' },
    { pergunta: 'Como funciona a migração do sistema atual?', resposta: 'Nossa equipe realiza a migração de cadastros (clientes, produtos, fornecedores) e configurações fiscais em até 24h. Disponibilizamos importadores para os principais sistemas do mercado brasileiro.' },
    { pergunta: 'O sistema atende Simples Nacional e Lucro Presumido?', resposta: 'Sim. O motor fiscal suporta Simples Nacional, Lucro Presumido e Lucro Real na mesma base de código, sem nenhuma alteração de código ou módulo adicional. A troca de regime é feita por parâmetro de cadastro.' },
    { pergunta: 'O PDV funciona sem internet?', resposta: 'Sim. O modo de contingência offline numera as vendas com flag de pendência e sincroniza automaticamente ao reconectar. A numeração não transmitida é inutilizada automaticamente no final do dia. Nenhuma venda é perdida.' },
    { pergunta: 'Posso cancelar ou mudar de plano a qualquer momento?', resposta: 'Sim, sem multa e sem fidelidade. Você pode cancelar, fazer upgrade ou downgrade a qualquer momento pelo painel de configurações. O cancelamento é efetivo ao final do período já pago.' },
    { pergunta: 'Os XMLs fiscais ficam armazenados por quanto tempo?', resposta: 'Por no mínimo 5 anos, conforme exigência legal do fisco brasileiro. Os XMLs são armazenados em storage de objetos com backup redundante diário e disponíveis para download a qualquer momento pelo próprio usuário.' },
    { pergunta: 'Quais bancos têm integração para boleto e PIX?', resposta: 'Atualmente integramos com Itaú, Bradesco, Banco do Brasil, Sicoob e Sicredi via API de boleto e PIX Cobrança. A remessa CNAB 240/400 e o retorno funcionam com qualquer banco que suporte o padrão FEBRABAN.' },
    { pergunta: 'Como funciona o suporte técnico?', resposta: 'Os planos Bronze e Silver incluem suporte via e-mail e chat com SLA de resposta em até 8h úteis. O plano Gold inclui suporte prioritário 24/7 por telefone, chat e gerente de conta dedicado com SLA de 2h.' },
  ],
};

/**
 * Configuração global da plataforma — vale para todos os clientes (tenants)
 * e só pode ser alterada pela área administrativa (`/admin`).
 * Mock em memória, como o `DataService`; substituir por chamadas à API.
 */
@Injectable({ providedIn: 'root' })
export class PlatformConfigService {
  private readonly _config = signal<PlatformConfig>(structuredClone(DEFAULT_CONFIG));
  private readonly _lastSaved = signal<{ at: Date; by: string } | null>(null);

  readonly config = this._config.asReadonly();
  readonly lastSaved = this._lastSaved.asReadonly();
  readonly activePlans = computed(() => this._config().plans.filter(p => p.active));

  /** Cópia independente para edição — nunca expõe o estado salvo para mutação. */
  snapshot(): PlatformConfig {
    return structuredClone(this._config());
  }

  async save(config: PlatformConfig, by: string): Promise<void> {
    await new Promise(r => setTimeout(r, 500));
    this._config.set(structuredClone(config));
    this._lastSaved.set({ at: new Date(), by });
  }
}
