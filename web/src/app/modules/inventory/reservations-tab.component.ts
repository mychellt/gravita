import { NgClass } from '@angular/common';
import { Component, OnInit, effect, inject, signal, untracked } from '@angular/core';
import { LoadStatus } from '../../core/services/customer.service';
import { InventoryChangesService } from '../../core/services/inventory-changes.service';
import {
  InventoryApiService, Page, ReservationStatus, StockReservationItem, inventoryFailureDetail, productLabel
} from '../../core/services/inventory-api.service';
import { ToastService } from '../../core/services/toast.service';
import { LoadStateComponent } from '../../shared/components/load-state/load-state.component';
import { PagerComponent } from '../../shared/components/pager/pager.component';

const PAGE_SIZE = 20;

const STATUS: Record<ReservationStatus, { rotulo: string; badge: string }> = {
  ACTIVE: { rotulo: 'Ativa', badge: 'proc' },
  CONSUMED: { rotulo: 'Consumida', badge: 'auth' },
  RELEASED: { rotulo: 'Liberada', badge: 'rascunho' },
};

/** Quantities held for approved sales orders; created by sales (M7), released here or on order cancellation. */
@Component({
  selector: 'app-reservations-tab',
  standalone: true,
  imports: [NgClass, LoadStateComponent, PagerComponent],
  template: `
    <div class="card">
      <div class="card-head">
        <span class="card-title">Reservas de estoque</span>
        <select class="filter-select" aria-label="Situação" (change)="definirSituacao($any($event.target).value)">
          <option value="ACTIVE">Ativas</option>
          <option value="">Todas</option>
          <option value="CONSUMED">Consumidas</option>
          <option value="RELEASED">Liberadas</option>
        </select>
      </div>
      @if (status() !== 'ready') {
        <app-load-state [status]="status()" loadingText="Carregando reservas…"
                        errorText="Não foi possível carregar as reservas." (retry)="load(pagina().page)" />
      } @else {
        <table class="data-table">
          <thead><tr><th>Pedido</th><th>Produto</th><th class="td-right">Quantidade</th><th>Situação</th><th></th></tr></thead>
          <tbody>
            @for (r of pagina().content; track r.id) {
              <tr>
                <td class="td-mono">{{ r.orderRef.slice(0, 8) }}</td>
                <td class="td-name">{{ rotulo(r) }}</td>
                <td class="td-mono td-right">{{ r.quantity }}</td>
                <td>
                  <span class="badge" [ngClass]="situacao(r.status).badge"><span class="badge-dot"></span>{{ situacao(r.status).rotulo }}</span>
                </td>
                <td class="td-actions">
                  @if (r.status === 'ACTIVE') {
                    <button class="btn btn-ghost btn-sm" type="button" [disabled]="liberando() === r.id" (click)="liberar(r)">
                      {{ liberando() === r.id ? 'Liberando…' : 'Liberar' }}
                    </button>
                  }
                </td>
              </tr>
            } @empty {
              <tr><td colspan="5" class="empty">Nenhuma reserva</td></tr>
            }
          </tbody>
        </table>
        <app-pager [page]="pagina().page" [totalPages]="pagina().totalPages" [totalElements]="pagina().totalElements"
                   (pageChange)="load($event)" />
      }
    </div>
  `,
  styles: [`
    .filter-select { height: 30px; border: 1px solid var(--border2); border-radius: var(--r8); font-family: var(--font); font-size: 12px; padding: 0 10px; background: var(--surface); color: var(--text); cursor: pointer; }
    .empty { text-align: center; padding: 24px; color: var(--text3); }
  `]
})
export class ReservationsTabComponent implements OnInit {
  private readonly api = inject(InventoryApiService);
  private readonly changes = inject(InventoryChangesService);
  private readonly toast = inject(ToastService);
  private loads = 0;
  private filtro: ReservationStatus | null = 'ACTIVE';

  readonly status = signal<LoadStatus>('loading');
  readonly pagina = signal<Page<StockReservationItem>>({ content: [], page: 0, size: PAGE_SIZE, totalElements: 0, totalPages: 0 });
  /** Reserva cuja liberação está em andamento: a linha só sai da lista depois que o servidor responde. */
  readonly liberando = signal<string | null>(null);

  constructor() {
    effect(() => {
      if (this.changes.version()) untracked(() => void this.load(this.pagina().page));
    });
  }

  ngOnInit(): void {
    void this.load(0);
  }

  definirSituacao(valor: string): void {
    this.filtro = (valor || null) as ReservationStatus | null;
    void this.load(0);
  }

  /** GET /api/inventory/reservations — nunca rejeita: o resultado fica em `status`. */
  async load(page: number): Promise<void> {
    const load = ++this.loads;
    this.status.set('loading');
    try {
      const result = await this.api.reservations(this.filtro, page, PAGE_SIZE);
      if (load !== this.loads) return;
      this.pagina.set(result);
      this.status.set('ready');
    } catch {
      if (load === this.loads) this.status.set('error');
    }
  }

  situacao(status: ReservationStatus): { rotulo: string; badge: string } {
    return STATUS[status];
  }

  rotulo(reserva: StockReservationItem): string {
    return productLabel(reserva.productId, reserva.productCode);
  }

  /** DELETE /api/inventory/reservations/{id}; em caso de sucesso, busca a lista de novo (e os saldos mudam junto). */
  async liberar(reserva: StockReservationItem): Promise<void> {
    if (this.liberando()) return;
    this.liberando.set(reserva.id);
    try {
      await this.api.releaseReservation(reserva.id);
    } catch (error) {
      this.liberando.set(null);
      return this.toast.danger(inventoryFailureDetail(error) || 'Não foi possível liberar a reserva.');
    }
    this.liberando.set(null);
    this.toast.success('Reserva liberada — quantidade disponível para nova venda');
    this.changes.stockChanged();
  }
}
