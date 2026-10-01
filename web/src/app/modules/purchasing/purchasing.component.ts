import { Component, computed, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { DataService } from '../../core/services/data.service';
import { ToastService } from '../../core/services/toast.service';
import { BadgeComponent } from '../../shared/components/badge/badge.component';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { BrlPipe } from '../../shared/pipes/brl.pipe';

@Component({
  selector: 'app-purchasing',
  standalone: true,
  imports: [DatePipe, BadgeComponent, PageHeaderComponent, BrlPipe],
  styleUrl: './purchasing.component.scss',
  templateUrl: './purchasing.component.html'
})
export class PurchasingComponent {
  constructor(public data: DataService, private toast: ToastService) {}

  activeTab = signal<'orders' | 'quotations' | 'receipts' | 'returns'>('orders');

  readonly orders = computed(() => this.data.pedidosCompra());

  readonly totals = computed(() => {
    const all = this.data.pedidosCompra();
    return {
      open:         all.filter(p => ['aberto','aguarda_aprovacao'].includes(p.status)).length,
      awaitingDelivery: all.filter(p => p.status === 'em_transito').length,
      received:     all.filter(p => p.status === 'encerrado').length,
      suppliers:    this.data.fornecedores().length
    };
  });

  approve(id: string) {
    this.data.updatePedidoCompraStatus(id, 'aberto');
    this.toast.success('Pedido de compra aprovado!');
  }

  receive(id: string) {
    this.data.updatePedidoCompraStatus(id, 'encerrado');
    this.toast.success('Recebimento confirmado. Estoque e Contas a pagar atualizados.');
  }
}
