import { Component, computed, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { DataService } from '../../core/services/data.service';
import { ToastService } from '../../core/services/toast.service';
import { BadgeComponent } from '../../shared/components/badge/badge.component';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { BrlPipe } from '../../shared/pipes/brl.pipe';

@Component({
  selector: 'app-compras',
  standalone: true,
  imports: [DatePipe, BadgeComponent, PageHeaderComponent, BrlPipe],
  styleUrl: './compras.component.scss',
  templateUrl: './compras.component.html'
})
export class ComprasComponent {
  constructor(public data: DataService, private toast: ToastService) {}

  activeTab = signal<'pedidos' | 'cotacoes' | 'recebimentos' | 'devolucoes'>('pedidos');

  readonly pedidos = computed(() => this.data.pedidosCompra());

  readonly totals = computed(() => {
    const all = this.data.pedidosCompra();
    return {
      abertos:     all.filter(p => ['aberto','aguarda_aprovacao'].includes(p.status)).length,
      entrega:     all.filter(p => p.status === 'em_transito').length,
      recebidos:   all.filter(p => p.status === 'encerrado').length,
      fornecedores: this.data.fornecedores().length
    };
  });

  aprovar(id: string) {
    this.data.updatePedidoCompraStatus(id, 'aberto');
    this.toast.success('Pedido de compra aprovado!');
  }

  receber(id: string) {
    this.data.updatePedidoCompraStatus(id, 'encerrado');
    this.toast.success('Recebimento confirmado. Estoque e Contas a pagar atualizados.');
  }
}
