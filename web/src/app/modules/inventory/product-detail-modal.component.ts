import { DatePipe } from '@angular/common';
import { Component, EventEmitter, Input, OnInit, Output, computed, inject, signal } from '@angular/core';
import { LoadStatus } from '../../core/services/customer.service';
import {
  InventoryApiService, Page, ProductBalance, StockMovementItem, StockMovementType, StockUnit
} from '../../core/services/inventory-api.service';
import { LoadStateComponent } from '../../shared/components/load-state/load-state.component';
import { ModalComponent } from '../../shared/components/modal/modal.component';
import { BrlPipe } from '../../shared/pipes/brl.pipe';

const GRAFICO = { largura: 420, altura: 90, margem: 8 };
const RECENT = 20;

const TIPO_ROTULO: Record<StockMovementType, string> = {
  ENTRY: 'Entrada', EXIT: 'Saída', ADJUSTMENT: 'Ajuste', TRANSFER: 'Transferência',
};

/** One server-backed section of the modal: its data, its load state and a loader that never rejects. */
class Section<T> {
  readonly data = signal<T | null>(null);
  readonly status = signal<LoadStatus>('loading');
  private loads = 0;

  async load(fetch: () => Promise<T>): Promise<void> {
    const load = ++this.loads;
    this.status.set('loading');
    try {
      const result = await fetch();
      if (load !== this.loads) return;
      this.data.set(result);
      this.status.set('ready');
    } catch {
      if (load === this.loads) this.status.set('error');
    }
  }
}

/** One product's picture from the same endpoints as the tabs, filtered by product: balance, cost, movements, lots. */
@Component({
  selector: 'app-product-detail-modal',
  standalone: true,
  imports: [DatePipe, BrlPipe, LoadStateComponent, ModalComponent],
  template: `
    <app-modal [title]="nome || 'Produto'" [width]="720" (closed)="closed.emit()">
      <h4 class="sec">Saldo</h4>
      @if (saldo.status() !== 'ready') {
        <app-load-state [status]="saldo.status()" loadingText="Carregando saldo…"
                        errorText="Não foi possível carregar o saldo." (retry)="carregarSaldo()" />
      } @else {
        @if (saldo.data(); as s) {
          <table class="data-table">
            <thead><tr><th class="td-right">Físico</th><th class="td-right">Reservado</th><th class="td-right">Em trânsito</th><th class="td-right">Disponível</th><th class="td-right">Custo médio</th></tr></thead>
            <tbody>
              <tr>
                <td class="td-mono td-right">{{ s.onHand }}</td>
                <td class="td-mono td-right">{{ s.reserved }}</td>
                <td class="td-mono td-right">{{ s.inTransit }}</td>
                <td class="td-mono td-right">{{ s.available }}</td>
                <td class="td-mono td-right">{{ s.averageCost | brl }}</td>
              </tr>
            </tbody>
          </table>
        }
      }

      <h4 class="sec">Histórico de custo unitário</h4>
      @if (movimentos.status() !== 'ready') {
        <app-load-state [status]="movimentos.status()" loadingText="Carregando histórico…"
                        errorText="Não foi possível carregar o histórico." (retry)="carregarMovimentos()" />
      } @else if (custos().length > 1) {
        <svg class="chart" [attr.viewBox]="'0 0 ' + grafico.largura + ' ' + grafico.altura" role="img"
             [attr.aria-label]="'Evolução do custo unitário de ' + (nome || 'produto')">
          <polyline [attr.points]="pontos()" fill="none" stroke="var(--accent)" stroke-width="2" />
        </svg>
        <div class="axis">
          @for (c of custos(); track c.id) {
            <span>{{ c.timestamp | date:'dd/MM' }}<br /><b>{{ c.unitCost | brl }}</b></span>
          }
        </div>
      } @else {
        <p class="muted">Sem histórico de custo.</p>
      }

      <h4 class="sec">Lotes</h4>
      @if (lotes.status() !== 'ready') {
        <app-load-state [status]="lotes.status()" loadingText="Carregando lotes…"
                        errorText="Não foi possível carregar os lotes." (retry)="carregarLotes()" />
      } @else {
        <table class="data-table">
          <thead><tr><th>Lote</th><th>Validade</th><th class="td-right">Quantidade</th></tr></thead>
          <tbody>
            @for (l of lotes.data()?.content; track l.id) {
              <tr>
                <td class="td-mono">{{ l.code }}</td>
                <td>{{ l.expiryDate ? (l.expiryDate + 'T00:00:00' | date:'dd/MM/yyyy') : '—' }}</td>
                <td class="td-mono td-right">{{ l.quantity }}</td>
              </tr>
            } @empty {
              <tr><td colspan="3" class="empty">Sem lotes</td></tr>
            }
          </tbody>
        </table>
      }

      <h4 class="sec">Movimentações recentes (somente leitura)</h4>
      @if (movimentos.status() !== 'ready') {
        <app-load-state [status]="movimentos.status()" loadingText="Carregando movimentações…"
                        errorText="Não foi possível carregar as movimentações." (retry)="carregarMovimentos()" />
      } @else {
        <table class="data-table">
          <thead><tr><th>Data/Hora</th><th>Tipo</th><th class="td-right">Qtd</th><th>Origem</th></tr></thead>
          <tbody>
            @for (m of movimentos.data()?.content; track m.id) {
              <tr>
                <td class="muted">{{ m.timestamp | date:'dd/MM HH:mm' }}</td>
                <td>{{ tipo(m.type) }}</td>
                <td class="td-mono td-right">{{ m.quantity }}</td>
                <td class="small">{{ m.originReference ?? '—' }}@if (m.justification) { · {{ m.justification }} }</td>
              </tr>
            } @empty {
              <tr><td colspan="4" class="empty">Sem movimentações</td></tr>
            }
          </tbody>
        </table>
      }

      <div footer><button class="btn btn-ghost" type="button" (click)="closed.emit()">Fechar</button></div>
    </app-modal>
  `,
  styles: [`
    .sec { font-size: 12px; font-weight: 600; text-transform: uppercase; letter-spacing: .04em; color: var(--text3); margin: 16px 0 8px; }
    .sec:first-child { margin-top: 0; }
    .chart { width: 100%; height: 90px; background: var(--surface2); border-radius: var(--r8); }
    .axis { display: flex; justify-content: space-between; font-size: 11px; color: var(--text3); margin-top: 4px; }
    .muted { font-size: 11px; color: var(--text3); }
    .small { font-size: 11px; }
    .empty { text-align: center; padding: 16px; color: var(--text3); }
  `]
})
export class ProductDetailModalComponent implements OnInit {
  private readonly api = inject(InventoryApiService);
  readonly grafico = GRAFICO;

  @Input({ required: true }) produtoId!: string;
  /** Descrição para o título; o backend só devolve o código do produto. */
  @Input() nome = '';
  @Output() closed = new EventEmitter<void>();

  readonly saldo = new Section<ProductBalance>();
  readonly movimentos = new Section<Page<StockMovementItem>>();
  readonly lotes = new Section<Page<StockUnit>>();

  /** Unit cost of each recent movement that carries one, oldest first — the cost history comes from the log itself. */
  readonly custos = computed(() => (this.movimentos.data()?.content ?? [])
    .filter((m): m is StockMovementItem & { unitCost: number } => m.unitCost !== null)
    .reverse());

  /** SVG polyline points scaled to the chart box; a flat series is drawn mid-height. */
  readonly pontos = computed(() => {
    const valores = this.custos().map(c => c.unitCost);
    const { largura, altura, margem } = GRAFICO;
    const min = Math.min(...valores);
    const faixa = Math.max(...valores) - min || 1;
    return valores.map((v, i) => {
      const x = margem + (i / (valores.length - 1)) * (largura - 2 * margem);
      const y = altura - margem - ((v - min) / faixa) * (altura - 2 * margem);
      return `${x.toFixed(1)},${y.toFixed(1)}`;
    }).join(' ');
  });

  ngOnInit(): void {
    void this.carregarSaldo();
    void this.carregarMovimentos();
    void this.carregarLotes();
  }

  tipo(tipo: StockMovementType): string {
    return TIPO_ROTULO[tipo];
  }

  carregarSaldo(): Promise<void> {
    return this.saldo.load(() => this.api.productBalance(this.produtoId));
  }

  carregarMovimentos(): Promise<void> {
    return this.movimentos.load(() => this.api.movements({ productId: this.produtoId }, 0, RECENT));
  }

  carregarLotes(): Promise<void> {
    return this.lotes.load(() => this.api.units('LOT', 0, RECENT, this.produtoId));
  }
}
