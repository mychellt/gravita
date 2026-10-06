import { DatePipe } from '@angular/common';
import { Component, EventEmitter, Input, Output, computed, inject, signal } from '@angular/core';
import { DataService } from '../../core/services/data.service';
import { InventoryService } from '../../core/services/inventory.service';
import { BrlPipe } from '../../shared/pipes/brl.pipe';
import { ModalComponent } from '../../shared/components/modal/modal.component';

const GRAFICO = { largura: 420, altura: 90, margem: 8 };

/** One product's full picture: balances, cost (CMV) history, movement log, lots and serials. */
@Component({
  selector: 'app-product-detail-modal',
  standalone: true,
  imports: [DatePipe, BrlPipe, ModalComponent],
  template: `
    <app-modal [title]="produto()?.descricao ?? 'Produto'" [width]="720" (closed)="closed.emit()">
      <h4 class="sec">Saldo por depósito</h4>
      <table class="data-table">
        <thead><tr><th>Depósito</th><th class="td-right">Físico</th><th class="td-right">Reservado</th><th class="td-right">Em trânsito</th><th class="td-right">Disponível</th></tr></thead>
        <tbody>
          @for (s of saldos(); track s.depositoId) {
            <tr>
              <td>{{ inventory.nomeDeposito(s.depositoId) }}</td>
              <td class="td-mono td-right">{{ s.fisico }}</td>
              <td class="td-mono td-right">{{ s.reservado }}</td>
              <td class="td-mono td-right">{{ s.emTransito }}</td>
              <td class="td-mono td-right">{{ inventory.disponivel(s) }}</td>
            </tr>
          }
        </tbody>
      </table>

      <h4 class="sec">Histórico de custo médio (CMV)</h4>
      @if (custos().length > 1) {
        <svg class="chart" [attr.viewBox]="'0 0 ' + grafico.largura + ' ' + grafico.altura" role="img"
             [attr.aria-label]="'Evolução do custo médio de ' + (produto()?.descricao ?? '')">
          <polyline [attr.points]="pontos()" fill="none" stroke="var(--accent)" stroke-width="2" />
        </svg>
        <div class="axis">
          @for (c of custos(); track c.data) {
            <span>{{ c.data | date:'dd/MM' }}<br /><b>{{ c.custoMedio | brl }}</b></span>
          }
        </div>
      } @else {
        <p class="muted">Sem histórico de custo.</p>
      }

      @if (lotes().length) {
        <h4 class="sec">Lotes</h4>
        <table class="data-table">
          <thead><tr><th>Lote</th><th>Depósito</th><th>Validade</th><th class="td-right">Quantidade</th></tr></thead>
          <tbody>
            @for (l of lotes(); track l.codigo + l.depositoId) {
              <tr>
                <td class="td-mono">{{ l.codigo }}</td>
                <td>{{ inventory.nomeDeposito(l.depositoId) }}</td>
                <td>{{ l.validade | date:'dd/MM/yyyy' }}</td>
                <td class="td-mono td-right">{{ l.quantidade }}</td>
              </tr>
            }
          </tbody>
        </table>
      }

      <h4 class="sec">Movimentações (somente leitura)</h4>
      <table class="data-table">
        <thead><tr><th>Data/Hora</th><th>Tipo</th><th class="td-right">Qtd</th><th class="td-right">Saldo após</th><th>Origem</th><th>Usuário</th></tr></thead>
        <tbody>
          @for (m of movimentos(); track m.id) {
            <tr>
              <td class="muted">{{ m.dataHora | date:'dd/MM HH:mm' }}</td>
              <td>{{ m.tipo }}</td>
              <td class="td-mono td-right">{{ m.quantidade > 0 ? '+' : '' }}{{ m.quantidade }}</td>
              <td class="td-mono td-right">{{ m.saldoApos }}</td>
              <td class="small">{{ m.origem }}@if (m.justificativa) { · {{ m.justificativa }} }</td>
              <td class="muted">{{ m.usuario }}</td>
            </tr>
          } @empty {
            <tr><td colspan="6" class="empty">Sem movimentações</td></tr>
          }
        </tbody>
      </table>

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
export class ProductDetailModalComponent {
  private readonly data = inject(DataService);
  readonly inventory = inject(InventoryService);
  readonly grafico = GRAFICO;

  private readonly id = signal('');
  @Input({ required: true }) set produtoId(valor: string) { this.id.set(valor); }
  @Output() closed = new EventEmitter<void>();

  readonly produto = computed(() => this.data.produtos().find(p => p.id === this.id()));
  readonly saldos = computed(() => this.inventory.saldosDoProduto(this.id()));
  readonly lotes = computed(() => this.inventory.lotes().filter(l => l.produtoId === this.id()));
  readonly movimentos = computed(() => this.inventory.movimentosDoProduto(this.id()));
  readonly custos = computed(() => this.inventory.historicoCusto()
    .filter(c => c.produtoId === this.id())
    .sort((a, b) => a.data.getTime() - b.data.getTime()));

  /** SVG polyline points scaled to the chart box; a flat series is drawn mid-height. */
  readonly pontos = computed(() => {
    const valores = this.custos().map(c => c.custoMedio);
    const { largura, altura, margem } = GRAFICO;
    const min = Math.min(...valores);
    const faixa = Math.max(...valores) - min || 1;
    return valores.map((v, i) => {
      const x = margem + (i / (valores.length - 1)) * (largura - 2 * margem);
      const y = altura - margem - ((v - min) / faixa) * (altura - 2 * margem);
      return `${x.toFixed(1)},${y.toFixed(1)}`;
    }).join(' ');
  });
}
