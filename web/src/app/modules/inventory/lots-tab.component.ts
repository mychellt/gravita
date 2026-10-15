import { DatePipe, NgClass } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { LoadStatus } from '../../core/services/customer.service';
import { InventoryAlertsService, daysUntil, lotKey } from '../../core/services/inventory-alerts.service';
import { InventoryApiService, Page, StockUnit, productLabel } from '../../core/services/inventory-api.service';
import { LoadStateComponent } from '../../shared/components/load-state/load-state.component';
import { PagerComponent } from '../../shared/components/pager/pager.component';

const JANELAS_DIAS = [7, 15, 30, 60];
const PAGE_SIZE = 20;

type Validade = 'vencido' | 'a_vencer' | 'ok';

const STATUS: Record<Validade, { rotulo: string; badge: string }> = {
  vencido: { rotulo: 'Vencido — bloqueado para venda', badge: 'canc' },
  a_vencer: { rotulo: 'A vencer', badge: 'pend' },
  ok: { rotulo: 'Dentro da validade', badge: 'auth' },
};

const emptyPage = <T>(): Page<T> => ({ content: [], page: 0, size: PAGE_SIZE, totalElements: 0, totalPages: 0 });

/**
 * Lots (with the expiry window taken from the same `expiring-lots` alert the side panel shows) plus serial-numbered
 * units, both listed by the server.
 */
@Component({
  selector: 'app-lots-tab',
  standalone: true,
  imports: [DatePipe, NgClass, LoadStateComponent, PagerComponent],
  template: `
    <div class="card">
      <div class="card-head">
        <span class="card-title">Lotes e validade</span>
        <label class="window">
          Alertar lotes que vencem em
          <select class="filter-select" aria-label="Janela de validade"
                  (change)="alertas.changeExpiryWindow(+$any($event.target).value)">
            @for (d of janelas; track d) { <option [value]="d" [selected]="d === alertas.expiryWindowDays()">{{ d }} dias</option> }
          </select>
        </label>
      </div>
      @if (statusLotes() !== 'ready') {
        <app-load-state [status]="statusLotes()" loadingText="Carregando lotes…"
                        errorText="Não foi possível carregar os lotes." (retry)="recarregarLotes()" />
      } @else {
        <table class="data-table">
          <thead>
            <tr><th>Produto</th><th>Lote</th><th>Validade</th><th class="td-right">Quantidade</th><th>Situação</th></tr>
          </thead>
          <tbody>
            @for (l of lotes().content; track l.id) {
              <tr>
                <td class="td-name">{{ rotulo(l) }}</td>
                <td class="td-mono">{{ l.code }}</td>
                <td>{{ l.expiryDate | date:'dd/MM/yyyy' }} <span class="muted">({{ prazo(l) }})</span></td>
                <td class="td-mono td-right">{{ l.quantity }}</td>
                <td>
                  <span class="badge" [ngClass]="situacao(l).badge"><span class="badge-dot"></span>{{ situacao(l).rotulo }}</span>
                </td>
              </tr>
            } @empty {
              <tr><td colspan="5" class="empty">Nenhum lote cadastrado</td></tr>
            }
          </tbody>
        </table>
        <app-pager [page]="lotes().page" [totalPages]="lotes().totalPages" [totalElements]="lotes().totalElements"
                   (pageChange)="carregarLotes($event)" />
      }
    </div>

    <div class="card mt-14">
      <div class="card-head"><span class="card-title">Números de série</span></div>
      @if (statusSeries() !== 'ready') {
        <app-load-state [status]="statusSeries()" loadingText="Carregando números de série…"
                        errorText="Não foi possível carregar os números de série." (retry)="carregarSeries(series().page)" />
      } @else {
        <table class="data-table">
          <thead><tr><th>Produto</th><th>Nº de série</th><th>Situação</th></tr></thead>
          <tbody>
            @for (s of series().content; track s.id) {
              <tr>
                <td class="td-name">{{ rotulo(s) }}</td>
                <td class="td-mono">{{ s.code }}</td>
                <td>
                  <span class="badge" [ngClass]="s.status === 'IN_STOCK' ? 'auth' : 'rascunho'">
                    <span class="badge-dot"></span>{{ s.status === 'IN_STOCK' ? 'Em estoque' : 'Saída emitida' }}
                  </span>
                </td>
              </tr>
            } @empty {
              <tr><td colspan="3" class="empty">Nenhum número de série cadastrado</td></tr>
            }
          </tbody>
        </table>
        <app-pager [page]="series().page" [totalPages]="series().totalPages" [totalElements]="series().totalElements"
                   (pageChange)="carregarSeries($event)" />
      }
    </div>
  `,
  styles: [`
    .window { display: flex; align-items: center; gap: 8px; font-size: 12px; color: var(--text2); }
    .filter-select { height: 30px; border: 1px solid var(--border2); border-radius: var(--r8); font-family: var(--font); font-size: 12px; padding: 0 10px; background: var(--surface); color: var(--text); cursor: pointer; }
    .muted { font-size: 11px; color: var(--text3); }
    .empty { text-align: center; padding: 24px; color: var(--text3); }
  `]
})
export class LotsTabComponent implements OnInit {
  private readonly api = inject(InventoryApiService);
  readonly alertas = inject(InventoryAlertsService);
  readonly janelas = JANELAS_DIAS;

  private lotLoads = 0;
  private serialLoads = 0;
  private readonly statusDaListagem = signal<LoadStatus>('loading');

  readonly lotes = signal<Page<StockUnit>>(emptyPage());
  readonly series = signal<Page<StockUnit>>(emptyPage());
  readonly statusSeries = signal<LoadStatus>('loading');

  /** A tabela depende da listagem e do alerta de vencimento; qualquer falha mostra o erro, nunca uma tabela incompleta. */
  readonly statusLotes = computed<LoadStatus>(() => {
    const estados = [this.statusDaListagem(), this.alertas.expiringStatus()];
    if (estados.includes('error')) return 'error';
    return estados.includes('loading') ? 'loading' : 'ready';
  });

  private readonly aVencer = computed(() =>
    new Set(this.alertas.expiringLots().map(l => lotKey(l.productId, l.lotCode))));

  ngOnInit(): void {
    void this.carregarLotes(0);
    void this.carregarSeries(0);
  }

  recarregarLotes(): void {
    void this.carregarLotes(this.lotes().page);
    if (this.alertas.expiringStatus() === 'error') void this.alertas.loadExpiring();
  }

  /** GET /api/inventory/lots?kind=LOT — nunca rejeita: o resultado fica em `statusLotes`. */
  async carregarLotes(page: number): Promise<void> {
    const load = ++this.lotLoads;
    this.statusDaListagem.set('loading');
    try {
      const result = await this.api.units('LOT', page, PAGE_SIZE);
      if (load !== this.lotLoads) return;
      this.lotes.set(result);
      this.statusDaListagem.set('ready');
    } catch {
      if (load === this.lotLoads) this.statusDaListagem.set('error');
    }
  }

  /** GET /api/inventory/lots?kind=SERIAL — nunca rejeita: o resultado fica em `statusSeries`. */
  async carregarSeries(page: number): Promise<void> {
    const load = ++this.serialLoads;
    this.statusSeries.set('loading');
    try {
      const result = await this.api.units('SERIAL', page, PAGE_SIZE);
      if (load !== this.serialLoads) return;
      this.series.set(result);
      this.statusSeries.set('ready');
    } catch {
      if (load === this.serialLoads) this.statusSeries.set('error');
    }
  }

  rotulo(unidade: StockUnit): string {
    return productLabel(unidade.productId, unidade.productCode);
  }

  situacao(lote: StockUnit): { rotulo: string; badge: string } {
    return STATUS[this.validade(lote)];
  }

  prazo(lote: StockUnit): string {
    if (!lote.expiryDate) return 'sem validade';
    const dias = daysUntil(lote.expiryDate);
    if (dias < 0) return `vencido há ${-dias}d`;
    return dias === 0 ? 'vence hoje' : `em ${dias}d`;
  }

  private validade(lote: StockUnit): Validade {
    if (!lote.expiryDate) return 'ok';
    if (daysUntil(lote.expiryDate) < 0) return 'vencido';
    return this.aVencer().has(lotKey(lote.productId, lote.code)) ? 'a_vencer' : 'ok';
  }
}
