import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { PurchaseOrderItem, PurchasingService } from '../../core/services/purchasing.service';
import { ToastService } from '../../core/services/toast.service';
import { BadgeComponent } from '../../shared/components/badge/badge.component';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { BrlPipe } from '../../shared/pipes/brl.pipe';

/** Linha da tabela, com o status no vocabulário do badge. */
export interface OrderRow {
  id: string;
  numero: string;
  fornecedorNome: string;
  dataPedido: string;
  valorTotal: number;
  status: string;
  /** O pedido pode ser aprovado ou rejeitado. */
  aguardaAprovacao: boolean;
  /** Dá para abrir o recebimento (aprovado e ainda não recebido). */
  recebivel: boolean;
  order: PurchaseOrderItem;
}

const sameMonth = (iso: string, now: Date) => {
  const at = new Date(iso);
  return at.getFullYear() === now.getFullYear() && at.getMonth() === now.getMonth();
};

@Component({
  selector: 'app-purchasing',
  standalone: true,
  imports: [DatePipe, BadgeComponent, PageHeaderComponent, BrlPipe],
  styleUrl: './purchasing.component.scss',
  templateUrl: './purchasing.component.html'
})
export class PurchasingComponent {
  private readonly purchasing = inject(PurchasingService);
  private readonly toast = inject(ToastService);

  constructor() {
    void this.purchasing.load();
  }

  activeTab = signal<'orders' | 'quotations' | 'receipts' | 'returns'>('orders');
  readonly loadStatus = this.purchasing.status;
  /** Id do pedido com uma ação em andamento. */
  readonly busyId = signal<string | null>(null);

  readonly orders = computed<OrderRow[]>(() => this.purchasing.orders().map(order => {
    const aguardaAprovacao = order.status === 'OPEN' && order.approvalRequired;
    return {
      id: order.id,
      numero: `PC-${order.id.slice(0, 8).toUpperCase()}`,
      fornecedorNome: order.supplierName ?? '—',
      dataPedido: order.createdAt,
      valorTotal: order.totalValue,
      status: this.statusOf(order, aguardaAprovacao),
      aguardaAprovacao,
      recebivel: order.status === 'OPEN' && !order.approvalRequired,
      order,
    };
  }));

  readonly totals = computed(() => {
    const all = this.purchasing.orders();
    const now = new Date();
    const open = all.filter(p => p.status === 'OPEN' || p.status === 'PARTIALLY_RECEIVED');
    return {
      open: open.length,
      awaitingDelivery: open.filter(p => !p.approvalRequired).length,
      closedThisMonth: all.filter(p => p.status === 'CLOSED' && sameMonth(p.createdAt, now)).length,
      openValue: open.reduce((sum, p) => sum + p.totalValue, 0),
    };
  });

  reload() { void this.purchasing.load(); }

  async decide(row: OrderRow, decision: 'APPROVE' | 'REJECT') {
    await this.run(row, () => this.purchasing.decide(row.id, decision),
      decision === 'APPROVE' ? 'Pedido de compra aprovado!' : 'Pedido de compra rejeitado.', 'Não foi possível registrar a decisão.');
  }

  /** Abre o recebimento de todos os itens; o estoque e as contas a pagar só são atualizados quando a conferência é confirmada. */
  async receive(row: OrderRow) {
    await this.run(row, () => this.purchasing.openReceipt(row.order),
      'Recebimento aberto. O estoque e as contas a pagar são atualizados quando a conferência for confirmada.',
      'Não foi possível abrir o recebimento.');
  }

  private async run(row: OrderRow, action: () => Promise<void>, success: string, failure: string) {
    if (this.busyId()) return;
    this.busyId.set(row.id);
    try {
      await action();
      this.toast.success(success);
    } catch (error) {
      const message = error instanceof HttpErrorResponse ? error.error?.message : null;
      this.toast.danger(message || failure);
    } finally {
      this.busyId.set(null);
    }
  }

  private statusOf(order: PurchaseOrderItem, aguardaAprovacao: boolean): string {
    if (aguardaAprovacao) return 'aguarda_aprovacao';
    return { OPEN: 'aberto', PARTIALLY_RECEIVED: 'recebido_parcial', CLOSED: 'encerrado', CANCELLED: 'cancelado' }[order.status];
  }
}
