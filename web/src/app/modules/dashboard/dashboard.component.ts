import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterModule } from '@angular/router';
import { DatePipe, DecimalPipe } from '@angular/common';
import { AuthService } from '../../core/services/auth.service';
import { DashboardService } from '../../core/services/dashboard.service';
import { NfeListItem, NfeService, nfeFailureDetail } from '../../core/services/nfe.service';
import { ToastService } from '../../core/services/toast.service';
import { BadgeComponent } from '../../shared/components/badge/badge.component';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { BrlPipe } from '../../shared/pipes/brl.pipe';

const WEEKDAYS = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb'];
const ACTIVITY_LABEL: Record<string, string> = { AUTHORIZED: 'autorizada', CANCELLED: 'cancelada', QUEUED: 'em fila', SENT: 'enviada' };

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, RouterModule, DecimalPipe, DatePipe, BadgeComponent, PageHeaderComponent, BrlPipe],
  styleUrl: './dashboard.component.scss',
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent {
  private readonly nfeService = inject(NfeService);
  private readonly toast = inject(ToastService);

  private readonly dashboardService = inject(DashboardService);
  private readonly auth = inject(AuthService);

  constructor() {
    void this.nfeService.loadIssued();
    void this.loadDashboard();
  }

  /** Estado de carga das NF-e emitidas (GET /api/nfe). */
  readonly nfeStatus = this.nfeService.status;
  /** Estado de carga dos indicadores (GET /api/reports/dashboard). */
  readonly dashboardStatus = this.dashboardService.status;
  readonly dashboard = this.dashboardService.dashboard;
  /** Id da NF-e com download em andamento, para não disparar o mesmo download duas vezes. */
  readonly busyId = signal<string | null>(null);

  readonly faturamentoHoje = computed(() => this.dashboard()?.revenue.day.current ?? 0);
  /** Variação do dia contra ontem; nula sem faturamento ontem. */
  readonly variacaoDia = computed(() => this.dashboard()?.revenue.day.variationPercent ?? null);
  readonly vencido = computed(() => this.dashboard()?.delinquency.totalOverdue ?? 0);
  readonly titulosVencidos = computed(() => this.dashboard()?.delinquency.titleCount ?? 0);
  readonly estoqueCritico = computed(() => this.dashboard()?.criticalStock.length ?? 0);
  readonly margemBruta = computed(() => this.dashboard()?.margin.grossMarginPercent ?? null);

  /** NF-e emitidas (não canceladas) no mês corrente e o valor total delas. */
  readonly nfeDoMes = computed(() => {
    const now = new Date();
    const issued = this.nfeService.nfes().filter(n => {
      const at = new Date(n.emittedAt);
      return n.status !== 'CANCELLED' && at.getFullYear() === now.getFullYear() && at.getMonth() === now.getMonth();
    });
    return { count: issued.length, total: issued.reduce((sum, n) => sum + n.totalValue, 0) };
  });

  /** As 5 NF-e emitidas mais recentes, da mais nova para a mais antiga. */
  readonly recentNfes = computed(() => [...this.nfeService.nfes()]
    .sort((a, b) => new Date(b.emittedAt).getTime() - new Date(a.emittedAt).getTime())
    .slice(0, 5));

  /** Barras dos últimos 7 dias; a altura é proporcional ao maior dia (mínimo visível para dias sem venda). */
  readonly chartBars = computed(() => {
    const days = this.dashboard()?.lastSevenDays ?? [];
    const max = Math.max(...days.map(d => d.value), 0);
    return days.map((d, i) => ({
      day: i === days.length - 1 ? 'Hoje' : WEEKDAYS[new Date(`${d.date}T12:00:00`).getDay()],
      value: d.value,
      height: max > 0 ? Math.max(Math.round((d.value / max) * 90), 4) : 4,
      highlight: max > 0 && d.value === max,
    }));
  });
  readonly totalSemana = computed(() => (this.dashboard()?.lastSevenDays ?? []).reduce((sum, d) => sum + d.value, 0));

  /** Atividade recente: as últimas NF-e autorizadas ou canceladas. */
  readonly activity = computed(() => this.recentNfes().map(nf => ({
    icon: nf.status === 'CANCELLED' ? 'ti-file-x' : 'ti-file-check',
    type: nf.status === 'CANCELLED' ? 'alert' : 'nfe',
    title: `NF-e #${nf.documentNumber ?? '—'} ${ACTIVITY_LABEL[nf.status] ?? 'registrada'} — ${nf.recipientName}`,
    at: nf.cancelledAt ?? nf.emittedAt,
    value: nf.totalValue,
  })));

  reloadDashboard(): void {
    void this.loadDashboard();
  }

  private async loadDashboard(): Promise<void> {
    // O dashboard pode abrir antes de a barra lateral identificar o usuário; a empresa dele restringe os indicadores.
    if (!this.auth.currentUser()) await this.auth.loadCurrentUser();
    await this.dashboardService.load(this.auth.currentUser()?.companyId);
  }

  reloadNfes(): void {
    void this.nfeService.loadIssued();
  }

  downloadDanfe(nfe: NfeListItem): Promise<void> {
    return this.download(nfe, 'pdf', 'DANFE', () => this.nfeService.danfe(nfe.id));
  }

  downloadXml(nfe: NfeListItem): Promise<void> {
    return this.download(nfe, 'xml', 'XML', () => this.nfeService.xml(nfe.id));
  }

  private async download(nfe: NfeListItem, extension: string, label: string, fetchFile: () => Promise<Blob>): Promise<void> {
    this.busyId.set(nfe.id);
    try {
      const url = URL.createObjectURL(await fetchFile());
      const link = document.createElement('a');
      link.href = url;
      link.download = `nfe-${nfe.documentSeries}-${nfe.documentNumber}.${extension}`;
      link.click();
      URL.revokeObjectURL(url);
    } catch (error) {
      this.toast.danger((await nfeFailureDetail(error)) || `Não foi possível baixar o ${label} da NF-e #${nfe.documentNumber}.`);
    } finally {
      this.busyId.set(null);
    }
  }
}
