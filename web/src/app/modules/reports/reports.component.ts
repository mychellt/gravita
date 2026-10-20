import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';
import type { LoadStatus } from '../../core/services/customer.service';
import { ProductSearchService } from '../../core/services/product-search.service';
import { AbcCurveEntry, ManagerialDre, ReportFormat, ReportsService } from '../../core/services/reports.service';
import { ToastService } from '../../core/services/toast.service';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { BrlPipe } from '../../shared/pipes/brl.pipe';

interface ReportCard {
  icon: string; iconBg: string; iconColor: string; title: string; desc: string;
  /** Segmento de /api/reports/{slug}/export e os parâmetros próprios do relatório. */
  slug: string; extra?: Record<string, string>;
}

const TOP_PRODUCTS = 10;
const LABEL_LENGTH = 8;

const currentPeriod = () => {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
};

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [PageHeaderComponent, BrlPipe],
  styleUrl: './reports.component.scss',
  templateUrl: './reports.component.html'
})
export class ReportsComponent {
  private readonly toast = inject(ToastService);
  private readonly reports = inject(ReportsService);
  private readonly productSearch = inject(ProductSearchService);
  private readonly auth = inject(AuthService);

  /** Mês dos relatórios, yyyy-MM. */
  readonly period = currentPeriod();
  readonly periodLabel = (() => {
    const label = new Date().toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' });
    return label.charAt(0).toUpperCase() + label.slice(1);
  })();

  readonly reportCards: ReportCard[] = [
    { icon: 'ti-chart-line',              iconBg: '#EFF6FF', iconColor: '#2563EB', title: 'DRE Gerencial',    desc: 'Receita bruta, CMV, despesas e resultado líquido', slug: 'dre' },
    { icon: 'ti-sort-descending-letters', iconBg: '#FFFBEB', iconColor: '#D97706', title: 'Curva ABC',        desc: 'Produtos e clientes por representatividade', slug: 'abc-curve', extra: { type: 'product' } },
    { icon: 'ti-report-money',            iconBg: '#ECFDF5', iconColor: '#059669', title: 'Comissões',        desc: 'Por vendedor, produto e período', slug: 'commissions' },
    { icon: 'ti-file-text',               iconBg: '#FEF2F2', iconColor: '#DC2626', title: 'Livros Fiscais',   desc: 'Entradas, Saídas e Apuração ICMS', slug: 'fiscal-books' },
    { icon: 'ti-package',                 iconBg: '#EDE9FE', iconColor: '#7C3AED', title: 'Giro de Estoque',  desc: 'Rotatividade e itens parados', slug: 'stock-turnover' },
    { icon: 'ti-calculator',              iconBg: '#F0F9FF', iconColor: '#0284C7', title: 'Tributos Apurados',desc: 'ICMS, IPI, PIS, COFINS, ISS por período', slug: 'assessed-taxes' },
  ];

  /** Formato dos arquivos exportados pelos cartões. */
  readonly format = signal<ReportFormat>('pdf');
  /** Slug do relatório sendo exportado, para não disparar a mesma exportação duas vezes. */
  readonly exporting = signal<string | null>(null);

  readonly dre = signal<ManagerialDre | null>(null);
  readonly dreStatus = signal<LoadStatus>('loading');
  readonly abcStatus = signal<LoadStatus>('loading');
  private readonly abc = signal<AbcCurveEntry[]>([]);
  private readonly productNames = signal<Map<string, string>>(new Map());

  /** Linhas do DRE; a participação é sobre a receita bruta (nula, mostrada como "—", sem receita). */
  readonly dreRows = computed(() => {
    const dre = this.dre();
    if (!dre) return null;
    const share = (value: number) => (dre.grossRevenue > 0 ? (value / dre.grossRevenue) * 100 : null);
    const grossProfit = dre.grossRevenue - dre.deductions - dre.cmv;
    return {
      grossRevenue: dre.grossRevenue, deductions: dre.deductions, cmv: dre.cmv, grossProfit, expenses: dre.totalExpenses,
      netResult: dre.netResult,
      deductionsShare: share(dre.deductions), cmvShare: share(dre.cmv), grossProfitShare: share(grossProfit),
      expensesShare: share(dre.totalExpenses), netShare: share(dre.netResult),
    };
  });

  /** Os 10 produtos de maior receita do mês, com a barra proporcional à participação do maior. */
  readonly abcData = computed(() => {
    const top = this.abc().slice(0, TOP_PRODUCTS);
    const max = Math.max(...top.map(e => e.revenueShare), 0);
    return top.map(entry => {
      const name = this.productNames().get(entry.entityId) ?? 'Produto';
      return {
        label: name.length > LABEL_LENGTH ? `${name.slice(0, LABEL_LENGTH - 1)}…` : name,
        name, pct: entry.revenueShare, cls: entry.abcClass.toLowerCase(), height: max > 0 ? (entry.revenueShare / max) * 90 : 0,
      };
    });
  });

  /** Participação da receita de cada classe, somando todos os produtos da curva (não só os 10 exibidos). */
  readonly abcShares = computed(() => {
    const share = (cls: string) => this.abc().filter(e => e.abcClass === cls).reduce((sum, e) => sum + e.revenueShare, 0);
    return { a: share('A'), b: share('B'), c: share('C') };
  });

  constructor() {
    void this.load();
  }

  reload() { void this.load(); }

  /** Participação sobre a receita bruta, em pt-BR; "—" sem receita para comparar. */
  pct(value: number | null): string {
    return value === null ? '—' : `${value.toLocaleString('pt-BR', { maximumFractionDigits: 1 })}%`;
  }

  /** Exporta o relatório do cartão, no formato escolhido, para o mês corrente. */
  async generate(card: ReportCard) {
    if (this.exporting()) return;
    this.exporting.set(card.slug);
    try {
      const blob = await this.reports.export(card.slug, this.format(), this.period, card.extra, this.auth.currentUser()?.companyId);
      this.saveFile(blob, `${card.slug}-${this.period}.${this.format()}`);
      this.toast.success(`Relatório "${card.title}" gerado.`);
    } catch (error) {
      this.toast.danger(error instanceof HttpErrorResponse && error.status === 403
        ? `Seu perfil não pode exportar "${card.title}".`
        : `Não foi possível gerar o relatório "${card.title}".`);
    } finally {
      this.exporting.set(null);
    }
  }

  /** Entrega o arquivo ao navegador. */
  protected saveFile(blob: Blob, fileName: string) {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    link.click();
    URL.revokeObjectURL(url);
  }

  private async load(): Promise<void> {
    // Os relatórios podem abrir antes de a barra lateral identificar o usuário; a empresa dele restringe os números.
    if (!this.auth.currentUser()) await this.auth.loadCurrentUser();
    const companyId = this.auth.currentUser()?.companyId;
    this.dreStatus.set('loading');
    this.abcStatus.set('loading');
    await Promise.all([this.loadDre(companyId), this.loadAbc(companyId)]);
  }

  private async loadDre(companyId?: string | null) {
    try {
      this.dre.set(await this.reports.dre(this.period, companyId));
      this.dreStatus.set('ready');
    } catch {
      this.dreStatus.set('error');
    }
  }

  private async loadAbc(companyId?: string | null) {
    try {
      this.abc.set(await this.reports.abcCurve('product', this.period, companyId));
      this.abcStatus.set('ready');
    } catch {
      this.abcStatus.set('error');
      return;
    }
    // Só a curva depende dos nomes para ficar legível; sem eles os produtos aparecem como "Produto".
    try {
      const products = await this.productSearch.search('');
      this.productNames.set(new Map(products.map(p => [p.productId, p.description])));
    } catch { /* mantém os rótulos genéricos */ }
  }
}
