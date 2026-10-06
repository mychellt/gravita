import { NgClass } from '@angular/common';
import { Component, inject } from '@angular/core';
import { ReservaStatus } from '../../core/models';
import { InventoryService } from '../../core/services/inventory.service';
import { ToastService } from '../../core/services/toast.service';

const STATUS: Record<ReservaStatus, { rotulo: string; badge: string }> = {
  ativa: { rotulo: 'Ativa', badge: 'proc' },
  consumida: { rotulo: 'Consumida', badge: 'auth' },
  liberada: { rotulo: 'Liberada', badge: 'rascunho' },
};

/** Quantities held for approved sales orders; created by sales (M7), released here or on order cancellation. */
@Component({
  selector: 'app-reservations-tab',
  standalone: true,
  imports: [NgClass],
  template: `
    <div class="card">
      <div class="card-head"><span class="card-title">Reservas de estoque</span></div>
      <table class="data-table">
        <thead><tr><th>Pedido</th><th>Produto</th><th>Depósito</th><th class="td-right">Quantidade</th><th>Situação</th><th></th></tr></thead>
        <tbody>
          @for (r of inventory.reservas(); track r.id) {
            <tr>
              <td class="td-mono">{{ r.pedidoRef }}</td>
              <td class="td-name">{{ r.produtoNome }}</td>
              <td class="small">{{ inventory.nomeDeposito(r.depositoId) }}</td>
              <td class="td-mono td-right">{{ r.quantidade }}</td>
              <td>
                <span class="badge" [ngClass]="status(r.status).badge"><span class="badge-dot"></span>{{ status(r.status).rotulo }}</span>
              </td>
              <td class="td-actions">
                @if (r.status === 'ativa') {
                  <button class="btn btn-ghost btn-sm" type="button" (click)="liberar(r.id)">Liberar</button>
                }
              </td>
            </tr>
          } @empty {
            <tr><td colspan="6" class="empty">Nenhuma reserva</td></tr>
          }
        </tbody>
      </table>
    </div>
  `,
  styles: [`
    .small { font-size: 11px; }
    .empty { text-align: center; padding: 24px; color: var(--text3); }
  `]
})
export class ReservationsTabComponent {
  private readonly toast = inject(ToastService);
  readonly inventory = inject(InventoryService);

  status(status: ReservaStatus): { rotulo: string; badge: string } {
    return STATUS[status];
  }

  liberar(id: string): void {
    const resultado = this.inventory.liberarReserva(id);
    if (!resultado.ok) return this.toast.danger(resultado.erro);
    this.toast.success('Reserva liberada — quantidade disponível para nova venda');
  }
}
