import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FinanceService, PayableItem, ReceivableItem } from '../../core/services/finance.service';
import { ToastService } from '../../core/services/toast.service';
import { BadgeComponent } from '../../shared/components/badge/badge.component';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { BrlPipe } from '../../shared/pipes/brl.pipe';

const WEEKDAYS = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb'];
const CHART_HEIGHT = 80;
const RECEIVABLE_ORIGIN: Record<ReceivableItem['origin'], string> = { INVOICING: 'Faturamento', MANUAL: 'Manual', RENEGOTIATION: 'Renegociação' };
const PAYABLE_ORIGIN: Record<PayableItem['origin'], string> = { PURCHASE_RECEIPT: 'Compra', MANUAL: 'Manual' };

const today = () => {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
};
/** Datas yyyy-MM-dd comparam como texto; vencido é estritamente antes de hoje. */
const isOverdue = (dueDate: string) => dueDate < today();

/** Linha das tabelas, já com o status no vocabulário do badge. */
export interface TitleRow {
  id: string;
  descricao: string;
  parceiro: string;
  vencimento: string;
  origem: string;
  valor: number;
  status: string;
  /** Pode ser baixado/pago (ainda há saldo a liquidar). */
  aberto: boolean;
}

@Component({
  selector: 'app-finance',
  standalone: true,
  imports: [DatePipe, BadgeComponent, PageHeaderComponent, BrlPipe],
  styleUrl: './finance.component.scss',
  templateUrl: './finance.component.html'
})
export class FinanceComponent {
  private readonly finance = inject(FinanceService);
  private readonly toast = inject(ToastService);

  constructor() {
    void this.finance.loadAll();
  }

  activeTab = signal<'receivable' | 'payable' | 'cashFlow' | 'reconciliation'>('receivable');
  /** Id da conta com baixa em andamento, para não baixar duas vezes. */
  readonly busyId = signal<string | null>(null);

  readonly receivablesStatus = this.finance.receivablesStatus;
  readonly payablesStatus = this.finance.payablesStatus;
  readonly cashFlowStatus = this.finance.cashFlowLoadStatus;

  readonly receivables = computed<TitleRow[]>(() => this.finance.receivables().map(r => {
    const outstanding = r.status === 'OPEN' || r.status === 'PARTIALLY_SETTLED';
    const installment = r.installments && r.installments > 1 ? ` ${r.installmentNumber ?? 1}/${r.installments}` : '';
    return {
      id: r.id,
      descricao: `${RECEIVABLE_ORIGIN[r.origin]}${installment}`,
      parceiro: r.customerName ?? '—',
      vencimento: r.dueDate,
      origem: RECEIVABLE_ORIGIN[r.origin],
      valor: outstanding ? r.remainingBalance : r.amount,
      status: this.receivableStatus(r, outstanding),
      aberto: outstanding,
    };
  }));

  readonly payables = computed<TitleRow[]>(() => this.finance.payables().map(p => {
    const outstanding = p.status === 'OPEN' || p.status === 'APPROVED';
    return {
      id: p.id,
      descricao: PAYABLE_ORIGIN[p.origin],
      parceiro: p.supplierName ?? '—',
      vencimento: p.dueDate,
      origem: PAYABLE_ORIGIN[p.origin],
      valor: p.amount,
      status: this.payableStatus(p, outstanding),
      aberto: outstanding,
    };
  }));

  readonly totalReceivable = computed(() => this.sumOpen(this.receivables()));
  readonly openReceivableCount = computed(() => this.receivables().filter(t => t.aberto).length);
  readonly totalPayable = computed(() => this.sumOpen(this.payables()));
  readonly openPayableCount = computed(() => this.payables().filter(t => t.aberto).length);
  readonly overdueReceivables = computed(() => this.receivables().filter(t => t.status === 'vencido'));
  readonly totalOverdue = computed(() => this.overdueReceivables().reduce((sum, t) => sum + t.valor, 0));
  readonly overdueCount = computed(() => this.overdueReceivables().length);

  /** Saldo projetado: entradas menos saídas previstas e realizadas nos próximos 7 dias. */
  readonly projectedBalance = computed(() => this.finance.cashFlow()?.closingBalance ?? 0);

  /** Barras de entradas e saídas dos próximos 7 dias, na mesma escala. */
  readonly cashflowBars = computed(() => {
    const buckets = this.finance.cashFlow()?.buckets ?? [];
    const inflow = (b: { realizedInflow: number; projectedInflow: number }) => b.realizedInflow + b.projectedInflow;
    const outflow = (b: { realizedOutflow: number; projectedOutflow: number }) => b.realizedOutflow + b.projectedOutflow;
    const max = Math.max(...buckets.map(b => Math.max(inflow(b), outflow(b))), 0);
    const height = (value: number) => (max > 0 && value > 0 ? Math.max(Math.round((value / max) * CHART_HEIGHT), 2) : 0);
    return buckets.map((b, i) => ({
      day: i === 0 ? 'Hoje' : WEEKDAYS[new Date(`${b.periodStart}T12:00:00`).getDay()],
      inH: height(inflow(b)), outH: height(outflow(b)), inValue: inflow(b), outValue: outflow(b),
    }));
  });

  reloadReceivables() { void this.finance.loadReceivables(); }
  reloadPayables() { void this.finance.loadPayables(); }
  reloadCashFlow() { void this.finance.loadCashFlow(); }

  /** Baixa integral do que falta na conta a receber. */
  async settleReceivable(row: TitleRow) {
    if (this.busyId()) return;
    this.busyId.set(row.id);
    try {
      await this.finance.settleReceivable(row.id, row.valor);
      void this.finance.loadCashFlow();
      this.toast.success('Baixa registrada com sucesso!');
    } catch (error) {
      const message = error instanceof HttpErrorResponse ? error.error?.message : null;
      this.toast.danger(message || 'Não foi possível registrar a baixa.');
    } finally {
      this.busyId.set(null);
    }
  }

  private sumOpen(rows: TitleRow[]): number {
    return rows.filter(t => t.aberto).reduce((sum, t) => sum + t.valor, 0);
  }

  private receivableStatus(r: ReceivableItem, outstanding: boolean): string {
    if (outstanding) return isOverdue(r.dueDate) ? 'vencido' : r.status === 'PARTIALLY_SETTLED' ? 'parcial' : 'aberto';
    return { SETTLED: 'liquidado', RENEGOTIATED: 'renegociado', CANCELLED: 'cancelado' }[r.status as 'SETTLED' | 'RENEGOTIATED' | 'CANCELLED'];
  }

  private payableStatus(p: PayableItem, outstanding: boolean): string {
    if (outstanding) return isOverdue(p.dueDate) ? 'vencido' : p.status === 'APPROVED' ? 'aprovado' : 'aberto';
    return p.status === 'PAID' ? 'pago' : 'cancelado';
  }
}
