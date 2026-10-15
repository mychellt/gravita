import { Component, inject } from '@angular/core';
import { InventoryAlertsService, LowStockRow } from '../../core/services/inventory-alerts.service';
import { productLabel } from '../../core/services/inventory-api.service';
import { ToastService } from '../../core/services/toast.service';
import { LoadStateComponent } from '../../shared/components/load-state/load-state.component';

/** Items whose available balance reached the reorder point (GET /api/inventory/alerts/low-stock). */
@Component({
  selector: 'app-reorder-tab',
  standalone: true,
  imports: [LoadStateComponent],
  template: `
    <div class="card">
      <div class="card-head">
        <span class="card-title">Sugestões de reposição</span>
        <button class="btn btn-primary btn-sm" type="button" [disabled]="!alertas.pendingRequests().length" (click)="pedirTodas()">
          Criar requisições ({{ alertas.pendingRequests().length }})
        </button>
      </div>
      @if (alertas.lowStockStatus() !== 'ready') {
        <app-load-state [status]="alertas.lowStockStatus()" loadingText="Carregando sugestões de reposição…"
                        errorText="Não foi possível carregar as sugestões de reposição." (retry)="alertas.loadLowStock()" />
      } @else {
        <table class="data-table">
          <thead>
            <tr><th>Produto</th><th class="td-right">Disponível</th><th class="td-right">Ponto de reposição</th><th class="td-right">Qtd sugerida</th><th></th></tr>
          </thead>
          <tbody>
            @for (s of alertas.lowStock(); track s.productId) {
              <tr>
                <td class="td-name">{{ rotulo(s) }}</td>
                <td class="td-mono td-right text-danger">{{ s.available }}</td>
                <td class="td-mono td-right">{{ s.reorderPoint }}</td>
                <td class="td-mono td-right">{{ s.suggestedQuantity }}</td>
                <td class="td-actions">
                  @if (s.request === 'requested') {
                    <span class="done"><i class="ti ti-check"></i> Requisição criada</span>
                  } @else {
                    <button class="btn btn-ghost btn-sm" type="button" [disabled]="s.request === 'sending' || s.suggestedQuantity <= 0"
                            (click)="pedir(s)">
                      {{ s.request === 'sending' ? 'Enviando…' : 'Criar requisição' }}
                    </button>
                  }
                </td>
              </tr>
            } @empty {
              <tr><td colspan="5" class="empty">Nenhum item atingiu o ponto de reposição</td></tr>
            }
          </tbody>
        </table>
      }
    </div>
  `,
  styles: [`
    .done { font-size: 11px; color: var(--success); }
    .empty { text-align: center; padding: 24px; color: var(--text3); }
  `]
})
export class ReorderTabComponent {
  private readonly toast = inject(ToastService);
  readonly alertas = inject(InventoryAlertsService);

  rotulo(sugestao: LowStockRow): string {
    return productLabel(sugestao.productId);
  }

  async pedir(sugestao: LowStockRow): Promise<void> {
    const resultado = await this.alertas.requestPurchase(sugestao.productId);
    if (!resultado.ok) return this.toast.danger(resultado.error);
    this.toast.success(`Requisição de compra criada para "${this.rotulo(sugestao)}"`);
  }

  async pedirTodas(): Promise<void> {
    const { created, failed } = await this.alertas.requestAll();
    if (created) this.toast.success(`${created} requisição(ões) de compra criada(s)`);
    if (failed) this.toast.danger(`${failed} requisição(ões) não puderam ser criadas`);
  }
}
