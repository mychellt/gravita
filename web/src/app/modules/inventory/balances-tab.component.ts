import { Component, OnInit, effect, inject, signal, untracked } from '@angular/core';
import { LoadStatus } from '../../core/services/customer.service';
import { InventoryChangesService } from '../../core/services/inventory-changes.service';
import {
  InventoryApiService, Page, StockBalanceItem, inventoryFailureDetail, productLabel
} from '../../core/services/inventory-api.service';
import { ToastService } from '../../core/services/toast.service';
import { LoadStateComponent } from '../../shared/components/load-state/load-state.component';
import { PagerComponent } from '../../shared/components/pager/pager.component';
import { BrlPipe } from '../../shared/pipes/brl.pipe';

const PAGE_SIZE = 20;

/** Balance per product in the default warehouse (on hand, reserved, in transit, available, average cost). */
@Component({
  selector: 'app-balances-tab',
  standalone: true,
  imports: [BrlPipe, LoadStateComponent, PagerComponent],
  template: `
    <div class="card">
      <div class="card-head"><span class="card-title">Saldo em estoque</span></div>
      @if (status() !== 'ready') {
        <app-load-state [status]="status()" loadingText="Carregando saldos…"
                        errorText="Não foi possível carregar os saldos." (retry)="load(pagina().page)" />
      } @else {
        <table class="data-table">
          <thead>
            <tr>
              <th>Produto</th><th class="td-right">Físico</th><th class="td-right">Reservado</th>
              <th class="td-right">Em trânsito</th><th class="td-right">Disponível</th><th class="td-right">Custo médio</th>
              <th>Saldo negativo</th>
            </tr>
          </thead>
          <tbody>
            @for (l of pagina().content; track l.productId) {
              <tr>
                <td class="td-name">{{ rotulo(l) }}</td>
                <td class="td-mono td-right">{{ l.onHand }}</td>
                <td class="td-mono td-right">{{ l.reserved }}</td>
                <td class="td-mono td-right">{{ l.inTransit }}</td>
                <td class="td-mono td-right" [class.text-danger]="l.available < 0">{{ l.available }}</td>
                <td class="td-mono td-right">{{ l.averageCost | brl }}</td>
                <td>
                  <div class="toggle" role="switch" tabindex="0" [class.on]="l.allowNegativeStock"
                       [class.busy]="alternando() === l.productId"
                       [attr.aria-disabled]="alternando() === l.productId"
                       [attr.aria-checked]="l.allowNegativeStock" [attr.aria-label]="'Permitir saldo negativo de ' + rotulo(l)"
                       (click)="alternarNegativo(l)" (keydown.enter)="alternarNegativo(l)"
                       (keydown.space)="$event.preventDefault(); alternarNegativo(l)"></div>
                </td>
              </tr>
            } @empty {
              <tr><td colspan="7" class="empty">Nenhum saldo encontrado</td></tr>
            }
          </tbody>
        </table>
        <app-pager [page]="pagina().page" [totalPages]="pagina().totalPages" [totalElements]="pagina().totalElements"
                   (pageChange)="load($event)" />
      }
    </div>
  `,
  styles: [`
    .empty { text-align: center; padding: 24px; color: var(--text3); }
    .toggle.busy { opacity: .5; pointer-events: none; }
  `]
})
export class BalancesTabComponent implements OnInit {
  private readonly api = inject(InventoryApiService);
  private readonly changes = inject(InventoryChangesService);
  private readonly toast = inject(ToastService);
  private loads = 0;

  readonly status = signal<LoadStatus>('loading');
  readonly pagina = signal<Page<StockBalanceItem>>({ content: [], page: 0, size: PAGE_SIZE, totalElements: 0, totalPages: 0 });
  /** Produto cujo saldo negativo está sendo alternado: o toggle fica desabilitado até o servidor responder. */
  readonly alternando = signal<string | null>(null);

  constructor() {
    // Ajuste, contagem aprovada ou reserva liberada mudam o servidor: busca de novo a página que está na tela.
    effect(() => {
      if (this.changes.version()) untracked(() => void this.load(this.pagina().page));
    });
  }

  ngOnInit(): void {
    void this.load(0);
  }

  /** GET /api/inventory/balances — nunca rejeita: o resultado fica em `status`. */
  async load(page: number): Promise<void> {
    const load = ++this.loads;
    this.status.set('loading');
    try {
      const result = await this.api.balances(page, PAGE_SIZE);
      if (load !== this.loads) return;
      this.pagina.set(result);
      this.status.set('ready');
    } catch {
      if (load === this.loads) this.status.set('error');
    }
  }

  rotulo(saldo: StockBalanceItem): string {
    return productLabel(saldo.productId, saldo.productCode);
  }

  /** PATCH /api/inventory/products/{id}/allow-negative-stock; a linha só muda depois que o servidor confirma. */
  async alternarNegativo(saldo: StockBalanceItem): Promise<void> {
    if (this.alternando()) return;
    this.alternando.set(saldo.productId);
    try {
      const allowNegativeStock = await this.api.setAllowNegativeStock(saldo.productId, !saldo.allowNegativeStock);
      this.pagina.update(p => ({
        ...p,
        content: p.content.map(l => l.productId === saldo.productId ? { ...l, allowNegativeStock } : l),
      }));
    } catch (error) {
      this.toast.danger(inventoryFailureDetail(error) || 'Não foi possível alterar o saldo negativo.');
    } finally {
      this.alternando.set(null);
    }
  }
}
