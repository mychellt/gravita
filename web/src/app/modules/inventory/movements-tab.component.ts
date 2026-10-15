import { DatePipe, NgClass } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { LoadStatus } from '../../core/services/customer.service';
import {
  InventoryApiService, MovementFilter, Page, StockMovementItem, StockMovementType, productLabel
} from '../../core/services/inventory-api.service';
import { LoadStateComponent } from '../../shared/components/load-state/load-state.component';
import { PagerComponent } from '../../shared/components/pager/pager.component';

const PAGE_SIZE = 20;
const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

const TIPO_ROTULO: Record<StockMovementType, string> = {
  ENTRY: '↑ Entrada', EXIT: '↓ Saída', ADJUSTMENT: '± Ajuste', TRANSFER: '⇄ Transferência',
};
const TIPO_BADGE: Record<StockMovementType, string> = {
  ENTRY: 'auth', EXIT: 'canc', ADJUSTMENT: 'rascunho', TRANSFER: 'proc',
};

/** Append-only movement log, newest first; product, type and period are filtered by the server. */
@Component({
  selector: 'app-movements-tab',
  standalone: true,
  imports: [DatePipe, NgClass, LoadStateComponent, PagerComponent],
  template: `
    <div class="card">
      <div class="card-head"><span class="card-title">Histórico de movimentações</span></div>
      <div class="filter-bar">
        <div class="filters">
          <input class="filter-input product" type="text" placeholder="ID do produto" aria-label="ID do produto"
                 [class.invalid]="produtoInvalido()" (change)="definirProduto($any($event.target).value)" />
          <select class="filter-select" aria-label="Tipo" (change)="definirTipo($any($event.target).value)">
            <option value="">Todos os tipos</option>
            <option value="ENTRY">Entrada</option>
            <option value="EXIT">Saída</option>
            <option value="ADJUSTMENT">Ajuste</option>
            <option value="TRANSFER">Transferência</option>
          </select>
          <input class="filter-input date" type="date" aria-label="De" (change)="definirDe($any($event.target).value)" />
          <input class="filter-input date" type="date" aria-label="Até" (change)="definirAte($any($event.target).value)" />
        </div>
        @if (produtoInvalido()) { <div class="hint" role="alert">Informe o ID do produto no formato UUID.</div> }
      </div>
      @if (status() !== 'ready') {
        <app-load-state [status]="status()" loadingText="Carregando movimentações…"
                        errorText="Não foi possível carregar as movimentações." (retry)="load(pagina().page)" />
      } @else {
        <table class="data-table">
          <thead>
            <tr>
              <th>Data/Hora</th><th>Produto</th><th>Tipo</th><th>Quantidade</th>
              <th>Lote / Série</th><th>Origem</th><th>Usuário</th>
            </tr>
          </thead>
          <tbody>
            @for (m of pagina().content; track m.id) {
              <tr>
                <td class="muted">{{ m.timestamp | date:'dd/MM HH:mm' }}</td>
                <td class="td-name">{{ rotulo(m) }}</td>
                <td>
                  <span class="badge" [ngClass]="badge(m.type)"><span class="badge-dot"></span>{{ tipo(m.type) }}</span>
                </td>
                <td class="td-mono" [class.text-success]="sinal(m) > 0" [class.text-danger]="sinal(m) < 0">
                  {{ quantidade(m) }}
                </td>
                <td class="td-mono small">{{ loteOuSerie(m) }}</td>
                <td class="small">
                  {{ m.originReference ?? '—' }}
                  @if (m.justification) { <div class="muted">{{ m.justification }}</div> }
                </td>
                <td class="muted">{{ m.user ? m.user.slice(0, 8) : '—' }}</td>
              </tr>
            } @empty {
              <tr><td colspan="7" class="empty">Nenhuma movimentação encontrada</td></tr>
            }
          </tbody>
        </table>
        <app-pager [page]="pagina().page" [totalPages]="pagina().totalPages" [totalElements]="pagina().totalElements"
                   (pageChange)="load($event)" />
      }
    </div>
  `,
  styles: [`
    .filter-bar { padding: 10px 14px; border-bottom: 1px solid var(--border); }
    .filters { display: flex; gap: 8px; flex-wrap: wrap; }
    .filter-input, .filter-select { height: 30px; border: 1px solid var(--border2); border-radius: var(--r8); font-family: var(--font); font-size: 12px; padding: 0 10px; outline: none; background: var(--surface); color: var(--text); }
    .filter-input:focus, .filter-select:focus { border-color: var(--accent); }
    .filter-input.product { width: 280px; }
    .filter-input.invalid { border-color: var(--danger); }
    .filter-input.date { width: 128px; }
    .filter-select { cursor: pointer; }
    .hint { margin-top: 6px; font-size: 11px; color: var(--danger); }
    .muted { font-size: 11px; color: var(--text3); }
    .small { font-size: 11px; }
    .empty { text-align: center; padding: 24px; color: var(--text3); }
  `]
})
export class MovementsTabComponent implements OnInit {
  private readonly api = inject(InventoryApiService);
  private loads = 0;

  private produto = '';
  private tipoFiltro: StockMovementType | '' = '';
  private de = '';
  private ate = '';

  readonly produtoInvalido = signal(false);
  readonly status = signal<LoadStatus>('loading');
  readonly pagina = signal<Page<StockMovementItem>>({ content: [], page: 0, size: PAGE_SIZE, totalElements: 0, totalPages: 0 });

  ngOnInit(): void {
    void this.load(0);
  }

  definirProduto(valor: string): void {
    this.produto = valor.trim();
    this.produtoInvalido.set(!!this.produto && !UUID.test(this.produto));
    if (!this.produtoInvalido()) void this.load(0);
  }

  definirTipo(valor: string): void {
    this.tipoFiltro = valor as StockMovementType | '';
    void this.load(0);
  }

  definirDe(valor: string): void {
    this.de = valor;
    void this.load(0);
  }

  definirAte(valor: string): void {
    this.ate = valor;
    void this.load(0);
  }

  /** GET /api/inventory/movements com os filtros atuais — nunca rejeita: o resultado fica em `status`. */
  async load(page: number): Promise<void> {
    if (this.produtoInvalido()) return;
    const load = ++this.loads;
    this.status.set('loading');
    try {
      const result = await this.api.movements(this.filtro(), page, PAGE_SIZE);
      if (load !== this.loads) return;
      this.pagina.set(result);
      this.status.set('ready');
    } catch {
      if (load === this.loads) this.status.set('error');
    }
  }

  tipo(tipo: StockMovementType): string {
    return TIPO_ROTULO[tipo];
  }

  badge(tipo: StockMovementType): string {
    return TIPO_BADGE[tipo];
  }

  rotulo(movimento: StockMovementItem): string {
    return productLabel(movimento.productId, movimento.productCode);
  }

  /** Saídas chegam em módulo (só o ajuste traz sinal); a transferência não diz o sentido, então fica sem sinal. */
  sinal(movimento: StockMovementItem): number {
    switch (movimento.type) {
      case 'ENTRY': return 1;
      case 'EXIT': return -1;
      case 'ADJUSTMENT': return Math.sign(movimento.quantity);
      default: return 0;
    }
  }

  quantidade(movimento: StockMovementItem): string {
    const modulo = Math.abs(movimento.quantity);
    return `${this.sinal(movimento) > 0 ? '+' : this.sinal(movimento) < 0 ? '−' : ''}${modulo}`;
  }

  loteOuSerie(movimento: StockMovementItem): string {
    return movimento.lotCode ?? (movimento.serialNumbers?.length ? movimento.serialNumbers.join(', ') : '—');
  }

  private filtro(): MovementFilter {
    return {
      productId: this.produto || undefined,
      type: this.tipoFiltro || undefined,
      from: this.de ? new Date(`${this.de}T00:00:00`).toISOString() : undefined,
      to: this.ate ? new Date(`${this.ate}T23:59:59.999`).toISOString() : undefined,
    };
  }
}
