import { Component, OnInit, inject, signal } from '@angular/core';
import { LoadStatus } from '../../core/services/customer.service';
import { InventoryApiService, Page, StockBalanceItem, productLabel } from '../../core/services/inventory-api.service';
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
              </tr>
            } @empty {
              <tr><td colspan="6" class="empty">Nenhum saldo encontrado</td></tr>
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
  `]
})
export class BalancesTabComponent implements OnInit {
  private readonly api = inject(InventoryApiService);
  private loads = 0;

  readonly status = signal<LoadStatus>('loading');
  readonly pagina = signal<Page<StockBalanceItem>>({ content: [], page: 0, size: PAGE_SIZE, totalElements: 0, totalPages: 0 });

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
}
