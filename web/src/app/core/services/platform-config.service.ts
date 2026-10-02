import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Plan, PlatformConfig } from '../models';

/** Limites legais/de produto que o administrador não pode reduzir. */
export const PLATFORM_MINIMUMS = {
  retencaoXmlAnos: 5,       // exigência do fisco
  retencaoBackupDias: 90,   // M10 — TriggerBackupUseCase
  senhaMinCaracteres: 8,
} as const;

const DEFAULT_CONFIG: PlatformConfig = {
  plans: [], // vêm de GET /api/plans (PlatformConfigService.loadPlans)
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

export type PlansStatus = 'loading' | 'ready' | 'error';

/** Mensagem para o usuário a partir de uma falha de `HttpClient`; o backend responde regras de negócio (409) em texto puro. */
function describeFailure(error: unknown, action: string): string {
  if (!(error instanceof HttpErrorResponse)) return `Não foi possível ${action}.`;
  if (error.status === 0) return `Não foi possível ${action}: sem conexão com o servidor.`;
  const detail = error.status < 500 && typeof error.error === 'string' ? error.error.trim() : '';
  return detail ? `Não foi possível ${action}: ${detail}` : `Não foi possível ${action} (erro ${error.status}).`;
}

const byMonthlyPrice = (a: Plan, b: Plan) => a.priceMonthly - b.priceMonthly;
const sameContent = (a: Plan, b: Plan) => JSON.stringify(a) === JSON.stringify(b);

/**
 * Configuração global da plataforma — vale para todos os clientes (tenants)
 * e só pode ser alterada pela área administrativa (`/admin`).
 * Os planos vêm de `/api/plans`; as demais seções ainda são mock em memória, como o `DataService`.
 */
@Injectable({ providedIn: 'root' })
export class PlatformConfigService {
  private readonly http = inject(HttpClient);
  private readonly plansUrl = `${environment.apiUrl}/plans`;

  private readonly _config = signal<PlatformConfig>(structuredClone(DEFAULT_CONFIG));
  private readonly _lastSaved = signal<{ at: Date; by: string } | null>(null);
  private readonly _plansStatus = signal<PlansStatus>('loading');
  private readonly _plansError = signal('');
  private plansRequest: Promise<void> | null = null;

  readonly config = this._config.asReadonly();
  readonly lastSaved = this._lastSaved.asReadonly();
  readonly plansStatus = this._plansStatus.asReadonly();
  readonly plansError = this._plansError.asReadonly();
  readonly activePlans = computed(() => this._config().plans.filter(p => p.active));

  constructor() {
    void this.loadPlans();
  }

  /** Cópia independente para edição — nunca expõe o estado salvo para mutação. */
  snapshot(): PlatformConfig {
    return structuredClone(this._config());
  }

  /** Busca os planos no backend. Nunca rejeita: o resultado fica em `plansStatus`/`plansError`. */
  loadPlans(): Promise<void> {
    this.plansRequest ??= this.fetchPlans().finally(() => (this.plansRequest = null));
    return this.plansRequest;
  }

  /**
   * Persiste os planos no backend (POST/PUT/DELETE conforme a diferença para o estado salvo) e as demais
   * seções em memória. Se uma chamada falhar, o que já foi gravado fica refletido no estado salvo e o erro é relançado.
   */
  async save(config: PlatformConfig, by: string): Promise<void> {
    const plans = await this.savePlans(config.plans);
    this._config.set(structuredClone({ ...config, plans }));
    this._lastSaved.set({ at: new Date(), by });
  }

  private async fetchPlans(): Promise<void> {
    this._plansStatus.set('loading');
    try {
      const plans = await firstValueFrom(this.http.get<Plan[]>(this.plansUrl));
      this._config.update(c => ({ ...c, plans: [...plans].sort(byMonthlyPrice) }));
      this._plansError.set('');
      this._plansStatus.set('ready');
    } catch (error) {
      this._plansError.set(describeFailure(error, 'carregar os planos'));
      this._plansStatus.set('error');
    }
  }

  private async savePlans(draft: Plan[]): Promise<Plan[]> {
    const saved = this._config().plans;
    const savedById = new Map(saved.map(p => [p.id, p]));
    const draftIds = new Set(draft.map(p => p.id));
    const resolved = new Map<Plan, Plan>();
    let persisted = saved; // o que o backend guarda, atualizado a cada chamada bem-sucedida

    try {
      for (const removed of saved.filter(p => !draftIds.has(p.id))) {
        await firstValueFrom(this.http.delete<void>(`${this.plansUrl}/${removed.id}`));
        persisted = persisted.filter(p => p.id !== removed.id);
      }
      for (const plan of draft.filter(p => savedById.has(p.id))) {
        if (sameContent(plan, savedById.get(plan.id)!)) { resolved.set(plan, plan); continue; }
        const updated = await firstValueFrom(this.http.put<Plan>(`${this.plansUrl}/${plan.id}`, plan));
        persisted = persisted.map(p => (p.id === updated.id ? updated : p));
        resolved.set(plan, updated);
      }
      for (const plan of draft.filter(p => !savedById.has(p.id))) {
        const { id: _clientId, ...body } = plan;
        const created = await firstValueFrom(this.http.post<Plan>(this.plansUrl, body));
        persisted = [...persisted, created];
        resolved.set(plan, created);
      }
    } catch (error) {
      this._config.update(c => ({ ...c, plans: persisted }));
      throw new Error(describeFailure(error, 'salvar os planos'));
    }
    return draft.map(p => resolved.get(p)!);
  }
}
