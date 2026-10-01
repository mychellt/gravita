import { Component, DoCheck, HostListener, Input, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import {
  InstitucionalSettings, IntegracaoCatalogo, IntegracaoCategoria, Plan, PlanLimits, PlanTier, PlatformConfig
} from '../../../core/models';
import { PLATFORM_MINIMUMS, PlatformConfigService } from '../../../core/services/platform-config.service';
import { ToastService } from '../../../core/services/toast.service';
import { HasUnsavedChanges } from '../../../core/guards/unsaved-changes.guard';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { BrlPipe } from '../../../shared/pipes/brl.pipe';
import { ValidationErrors, validatePlatformConfig } from './platform-settings.validation';

type SectionKey = 'planos' | 'cobranca' | 'suporte' | 'integracoes' | 'conformidade' | 'institucional' | 'site';

interface Section { key: SectionKey; label: string; icon: string; title: string; desc: string; }

/** Parte da configuração que cada seção edita — usada para marcar seções alteradas. */
const SECTION_SLICES: Record<SectionKey, (c: PlatformConfig) => unknown> = {
  planos:        c => c.plans.map(({ support, ...plan }) => plan),
  cobranca:      c => c.billing,
  suporte:       c => [c.plans.map(p => p.support), c.compliance.uptimeSla],
  integracoes:   c => c.integracoes,
  conformidade:  c => ({ ...c.compliance, uptimeSla: undefined }),
  institucional: c => c.institucional,
  site:          c => [c.siteStats, c.faq],
};

const CURRENT_USER = 'Ricardo Lima';

@Component({
  selector: 'app-platform-settings',
  standalone: true,
  imports: [FormsModule, RouterLink, DatePipe, PageHeaderComponent, BrlPipe],
  styleUrls: ['../../settings/settings.component.scss', './platform-settings.component.scss'],
  templateUrl: './platform-settings.component.html'
})
export class PlatformSettingsComponent implements DoCheck, HasUnsavedChanges {
  readonly draftScope = '/admin/settings/';
  readonly minimums = PLATFORM_MINIMUMS;

  readonly sections: Section[] = [
    { key: 'planos',        label: 'Planos & Preços',   icon: 'ti-tags',            title: 'Planos & Preços',          desc: 'Preços, limites e recursos exibidos no site e aplicados às assinaturas' },
    { key: 'cobranca',      label: 'Assinatura',        icon: 'ti-receipt-2',       title: 'Assinatura & Cobrança',    desc: 'Período de teste, taxas e regras do plano anual' },
    { key: 'suporte',       label: 'Suporte & SLA',     icon: 'ti-headset',         title: 'Suporte & SLA',            desc: 'Canais de atendimento e tempo de resposta por plano' },
    { key: 'integracoes',   label: 'Integrações',       icon: 'ti-plug',            title: 'Catálogo de integrações',  desc: 'Integrações disponíveis aos clientes e plano mínimo exigido' },
    { key: 'conformidade',  label: 'Segurança',         icon: 'ti-shield-check',    title: 'Segurança & Conformidade', desc: 'Regras obrigatórias para todos os clientes' },
    { key: 'institucional', label: 'Institucional',     icon: 'ti-building',        title: 'Dados institucionais',     desc: 'Identificação da Gravita, contato e links legais do rodapé' },
    { key: 'site',          label: 'Site público',      icon: 'ti-world',           title: 'Conteúdo do site',         desc: 'Números de destaque e perguntas frequentes da página inicial' },
  ];

  readonly limitFields: { key: keyof PlanLimits; label: string; one: string; many: string; unlimited: string }[] = [
    { key: 'cnpjs',     label: 'CNPJs',              one: 'CNPJ',             many: 'CNPJs',              unlimited: 'CNPJs ilimitados' },
    { key: 'filiais',   label: 'Filiais',            one: 'filial',           many: 'filiais',            unlimited: 'Filiais ilimitadas' },
    { key: 'caixasPdv', label: 'Caixas PDV',         one: 'caixa PDV',        many: 'caixas PDV',         unlimited: 'Caixas PDV ilimitados' },
    { key: 'usuarios',  label: 'Usuários incluídos', one: 'usuário incluído', many: 'usuários incluídos', unlimited: 'Usuários ilimitados' },
  ];

  readonly tiers: { key: PlanTier; label: string; icon: string }[] = [
    { key: 'BRONZE', label: 'Bronze', icon: 'ti-medal' },
    { key: 'SILVER', label: 'Silver', icon: 'ti-star' },
    { key: 'GOLD',   label: 'Gold',   icon: 'ti-crown' },
  ];

  readonly categorias: { key: IntegracaoCategoria; label: string }[] = [
    { key: 'banco',      label: 'Bancos — boleto e PIX Cobrança' },
    { key: 'ecommerce',  label: 'E-commerce' },
    { key: 'mensageria', label: 'Mensageria' },
  ];

  readonly footerLinks: { key: Exclude<keyof InstitucionalSettings, 'razaoSocial' | 'cnpj' | 'cidadeUf' | 'emailContato'>; label: string }[] = [
    { key: 'urlCentralAjuda', label: 'Central de ajuda' },
    { key: 'urlDocsApi',      label: 'Documentação da API' },
    { key: 'urlStatus',       label: 'Status do sistema' },
    { key: 'urlPrivacidade',  label: 'Política de privacidade' },
    { key: 'urlTermos',       label: 'Termos de uso' },
    { key: 'urlLgpd',         label: 'LGPD' },
  ];

  readonly activeSection = signal<SectionKey>('planos');
  readonly selectedPlanId = signal('');
  readonly previewAnual = signal(false);
  readonly saving = signal(false);

  draft: PlatformConfig;
  errors: ValidationErrors = {};
  dirty = false;
  private dirtySections = new Set<SectionKey>();
  private errorCounts: Partial<Record<SectionKey, number>> = {};

  constructor(
    public cfg: PlatformConfigService,
    private toast: ToastService,
    private router: Router,
  ) {
    this.draft = cfg.snapshot();
    this.selectedPlanId.set(this.draft.plans.find(p => p.featured)?.id ?? this.draft.plans[0]?.id ?? '');
  }

  /** Bound to the route param `:section` (withComponentInputBinding). */
  @Input() set section(value: string) {
    if (this.sections.some(s => s.key === value)) {
      this.activeSection.set(value as SectionKey);
    } else {
      this.router.navigate(['/admin/settings/planos'], { replaceUrl: true });
    }
  }

  ngDoCheck(): void {
    const saved = this.cfg.config();
    this.errors = validatePlatformConfig(this.draft);
    this.dirty = JSON.stringify(this.draft) !== JSON.stringify(saved);

    this.dirtySections.clear();
    this.errorCounts = {};
    if (this.dirty) {
      for (const s of this.sections) {
        const slice = SECTION_SLICES[s.key];
        if (JSON.stringify(slice(this.draft)) !== JSON.stringify(slice(saved))) this.dirtySections.add(s.key);
      }
    }
    for (const key of Object.keys(this.errors)) {
      const s = key.split('.')[0] as SectionKey;
      this.errorCounts[s] = (this.errorCounts[s] ?? 0) + 1;
    }
  }

  // ── Estado geral ──

  hasUnsavedChanges(): boolean { return this.dirty; }
  get currentSection(): Section { return this.sections.find(s => s.key === this.activeSection())!; }
  get errorTotal(): number { return Object.keys(this.errors).length; }
  isSectionDirty(key: SectionKey): boolean { return this.dirtySections.has(key); }
  sectionErrors(key: SectionKey): number { return this.errorCounts[key] ?? 0; }
  err(path: string): string | undefined { return this.errors[path]; }

  @HostListener('window:beforeunload', ['$event'])
  onBeforeUnload(e: BeforeUnloadEvent) {
    if (this.dirty) e.preventDefault();
  }

  @HostListener('document:keydown', ['$event'])
  onKeydown(e: KeyboardEvent) {
    if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 's') {
      e.preventDefault();
      this.save();
    }
  }

  async save() {
    if (!this.dirty || this.saving()) return;
    if (this.errorTotal > 0) {
      const first = this.sections.find(s => this.sectionErrors(s.key) > 0);
      this.toast.danger(`Corrija ${this.errorTotal} ${this.errorTotal === 1 ? 'campo' : 'campos'} antes de salvar.`);
      if (first && first.key !== this.activeSection()) this.router.navigate(['/admin/settings', first.key]);
      return;
    }
    this.saving.set(true);
    try {
      await this.cfg.save(this.draft, CURRENT_USER);
      this.draft = this.cfg.snapshot();
      this.toast.success('Configurações da plataforma salvas.');
    } finally {
      this.saving.set(false);
    }
  }

  discard() {
    if (!this.dirty) return;
    if (!confirm('Descartar todas as alterações não salvas?')) return;
    this.draft = this.cfg.snapshot();
    if (!this.draft.plans.some(p => p.id === this.selectedPlanId())) this.selectedPlanId.set(this.draft.plans[0]?.id ?? '');
    this.toast.show('Alterações descartadas.');
  }

  // ── Planos ──

  get selectedPlan(): Plan | undefined {
    return this.draft.plans.find(p => p.id === this.selectedPlanId());
  }

  tierOf(tier: PlanTier) { return this.tiers.find(t => t.key === tier)!; }

  planErrors(plan: Plan): number {
    return Object.keys(this.errors).filter(k => k.startsWith(`planos.${plan.id}.`)).length;
  }

  setFeatured(plan: Plan, on: boolean) {
    this.draft.plans.forEach(p => p.featured = on && p.id === plan.id);
  }

  toggleUnlimited(plan: Plan, key: keyof PlanLimits) {
    plan.limits[key] = plan.limits[key] === null ? 1 : null;
  }

  addFeature(plan: Plan) { plan.features.push({ label: '', included: true }); }
  removeFeature(plan: Plan, i: number) { plan.features.splice(i, 1); }

  move<T>(list: T[], i: number, dir: -1 | 1) {
    const j = i + dir;
    if (j < 0 || j >= list.length) return;
    [list[i], list[j]] = [list[j], list[i]];
  }

  annualDiscountPct(plan: Plan): number {
    if (!plan.priceMonthly || plan.priceAnnual == null) return 0;
    return Math.round((1 - plan.priceAnnual / plan.priceMonthly) * 1000) / 10;
  }

  annualSavings(plan: Plan): number {
    return Math.max(0, ((plan.priceMonthly ?? 0) - (plan.priceAnnual ?? 0)) * 12);
  }

  suggestedAnnual(plan: Plan): number {
    const free = this.draft.billing.mesesGratisAnual ?? 0;
    return Math.floor((plan.priceMonthly ?? 0) * (12 - free) / 12);
  }

  get annualOutOfSync(): boolean {
    return this.draft.plans.some(p => p.priceAnnual !== this.suggestedAnnual(p));
  }

  applySuggestedAnnual() {
    this.draft.plans.forEach(p => p.priceAnnual = this.suggestedAnnual(p));
    this.toast.show('Preços anuais recalculados. Revise e salve para publicar.');
  }

  fmtPrice(v: number | null | undefined): string {
    return new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 2 }).format(v ?? 0);
  }

  limitText(plan: Plan, key: keyof PlanLimits): string {
    const f = this.limitFields.find(l => l.key === key)!;
    const v = plan.limits[key];
    if (v === null) return f.unlimited;
    return `${v} ${v === 1 ? f.one : f.many}`;
  }

  // ── Suporte ──

  supportSummary(plan: Plan): string {
    const s = plan.support;
    const canais = [s.email && 'e-mail', s.chat && 'chat', s.telefone && 'telefone'].filter(Boolean) as string[];
    const lista = canais.length > 1 ? `${canais.slice(0, -1).join(', ')} e ${canais[canais.length - 1]}` : canais[0] ?? '—';
    const janela = s.horarioComercial ? 'úteis' : '(24/7)';
    const texto = `${lista} · resposta em até ${s.slaHoras}h ${janela}${s.gerenteDedicado ? ' · gerente de conta' : ''}`;
    return texto.charAt(0).toUpperCase() + texto.slice(1);
  }

  // ── Integrações ──

  integracoesDe(cat: IntegracaoCategoria): IntegracaoCatalogo[] {
    return this.draft.integracoes.filter(i => i.categoria === cat);
  }

  // ── Site ──

  addFaq() {
    this.draft.faq.push({ pergunta: '', resposta: '' });
  }

  removeFaq(i: number) { this.draft.faq.splice(i, 1); }
}
