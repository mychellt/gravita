import { Component, EventEmitter, Output, inject } from '@angular/core';
import { InventoryAlertsService, LowStockRow, daysUntil } from '../../core/services/inventory-alerts.service';
import { ExpiringLot, productLabel } from '../../core/services/inventory-api.service';
import { ToastService } from '../../core/services/toast.service';
import { LoadStateComponent } from '../../shared/components/load-state/load-state.component';

/** Side panel of the inventory screen: low-stock and expiring-lots alerts, straight from the two alert endpoints. */
@Component({
  selector: 'app-alerts-panel',
  standalone: true,
  imports: [LoadStateComponent],
  template: `
    <div class="card">
      <div class="card-head"><span class="card-title">Alertas críticos</span></div>

      @if (alertas.lowStockStatus() !== 'ready') {
        <app-load-state [status]="alertas.lowStockStatus()" loadingText="Carregando estoque baixo…"
                        errorText="Não foi possível carregar os alertas de estoque baixo." (retry)="alertas.loadLowStock()" />
      }
      @if (alertas.expiringStatus() !== 'ready') {
        <app-load-state [status]="alertas.expiringStatus()" loadingText="Carregando lotes a vencer…"
                        errorText="Não foi possível carregar os alertas de validade." (retry)="alertas.loadExpiring()" />
      }

      @for (s of alertas.lowStock(); track s.productId) {
        <div class="alert-item">
          <div class="alert-dot red"></div>
          <div class="alert-info">
            <div class="alert-prod">{{ rotulo(s.productId, s.productCode) }}</div>
            <div class="alert-stock text-danger">{{ s.available }} disp. (ponto de pedido: {{ s.reorderPoint }})</div>
          </div>
          @if (s.request === 'requested') {
            <span class="done"><i class="ti ti-check"></i> Pedido</span>
          } @else {
            <button class="btn btn-ghost btn-sm" type="button" [disabled]="s.request === 'sending' || s.suggestedQuantity <= 0"
                    (click)="pedir(s)">
              {{ s.request === 'sending' ? 'Enviando…' : 'Pedir' }}
            </button>
          }
        </div>
      }

      @for (l of alertas.expiringLots(); track l.productId + l.lotCode) {
        <div class="alert-item">
          <div class="alert-dot amber"></div>
          <div class="alert-info">
            <div class="alert-prod">{{ rotulo(l.productId, l.productCode) }} · {{ l.lotCode }}</div>
            <div class="alert-stock text-warn">{{ textoValidade(l) }}</div>
          </div>
          <button class="btn btn-ghost btn-sm" type="button" (click)="verLotes.emit()">Ver lote</button>
        </div>
      }

      @if (alertas.lowStockStatus() === 'ready' && alertas.expiringStatus() === 'ready'
           && !alertas.lowStock().length && !alertas.expiringLots().length) {
        <div class="alert-item"><div class="alert-info muted">Nenhum alerta no momento.</div></div>
      }
    </div>
  `,
  styles: [`
    .alert-item { display: flex; align-items: center; gap: 10px; padding: 10px 14px; border-bottom: 1px solid var(--border); }
    .alert-item:last-child { border-bottom: none; }
    .alert-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
    .alert-dot.red   { background: var(--danger); }
    .alert-dot.amber { background: var(--warn); }
    .alert-info  { flex: 1; }
    .alert-prod  { font-size: 12px; font-weight: 500; }
    .alert-stock { font-size: 11px; margin-top: 1px; }
    .muted { font-size: 12px; color: var(--text3); }
    .done  { font-size: 11px; color: var(--success); }
  `]
})
export class AlertsPanelComponent {
  private readonly toast = inject(ToastService);
  readonly alertas = inject(InventoryAlertsService);

  @Output() verLotes = new EventEmitter<void>();

  rotulo(productId: string, productCode?: string | null): string {
    return productLabel(productId, productCode);
  }

  textoValidade(lote: ExpiringLot): string {
    const dias = daysUntil(lote.expiryDate);
    if (dias < 0) return `Lote vencido há ${-dias} dia(s)`;
    return dias === 0 ? 'Lote vence hoje' : `Lote vence em ${dias} dia(s)`;
  }

  async pedir(sugestao: LowStockRow): Promise<void> {
    const resultado = await this.alertas.requestPurchase(sugestao.productId);
    if (!resultado.ok) return this.toast.danger(resultado.error);
    this.toast.success(`Requisição de compra criada para "${this.rotulo(sugestao.productId, sugestao.productCode)}"`);
  }
}
