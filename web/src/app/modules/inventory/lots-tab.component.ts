import { DatePipe, NgClass } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { LoteEstoque } from '../../core/models';
import { DataService } from '../../core/services/data.service';
import { InventoryService } from '../../core/services/inventory.service';

const JANELAS_DIAS = [7, 15, 30, 60];

const STATUS: Record<ReturnType<InventoryService['statusValidade']>, { rotulo: string; badge: string }> = {
  vencido: { rotulo: 'Vencido — bloqueado para venda', badge: 'canc' },
  a_vencer: { rotulo: 'A vencer', badge: 'pend' },
  ok: { rotulo: 'Dentro da validade', badge: 'auth' },
};

/** Lots ordered by expiry (earliest first, so the oldest is picked first) plus serial-numbered units. */
@Component({
  selector: 'app-lots-tab',
  standalone: true,
  imports: [DatePipe, NgClass],
  template: `
    <div class="card">
      <div class="card-head">
        <span class="card-title">Lotes e validade</span>
        <label class="window">
          Alertar lotes que vencem em
          <select class="filter-select" aria-label="Janela de validade"
                  (change)="inventory.janelaValidadeDias.set(+$any($event.target).value)">
            @for (d of janelas; track d) { <option [value]="d" [selected]="d === inventory.janelaValidadeDias()">{{ d }} dias</option> }
          </select>
        </label>
      </div>
      <table class="data-table">
        <thead>
          <tr><th>Produto</th><th>Lote</th><th>Depósito</th><th>Validade</th><th class="td-right">Quantidade</th><th>Situação</th></tr>
        </thead>
        <tbody>
          @for (l of lotes(); track l.codigo + l.depositoId) {
            <tr>
              <td class="td-name">{{ nomeProduto(l.produtoId) }}</td>
              <td class="td-mono">{{ l.codigo }}</td>
              <td class="small">{{ inventory.nomeDeposito(l.depositoId) }}</td>
              <td>{{ l.validade | date:'dd/MM/yyyy' }} <span class="muted">({{ prazo(l) }})</span></td>
              <td class="td-mono td-right">{{ l.quantidade }}</td>
              <td>
                <span class="badge" [ngClass]="situacao(l).badge"><span class="badge-dot"></span>{{ situacao(l).rotulo }}</span>
              </td>
            </tr>
          } @empty {
            <tr><td colspan="6" class="empty">Nenhum lote cadastrado</td></tr>
          }
        </tbody>
      </table>
    </div>

    <div class="card mt-14">
      <div class="card-head"><span class="card-title">Números de série</span></div>
      <table class="data-table">
        <thead><tr><th>Produto</th><th>Nº de série</th><th>Depósito</th><th>Situação</th></tr></thead>
        <tbody>
          @for (s of inventory.series(); track s.numero) {
            <tr>
              <td class="td-name">{{ nomeProduto(s.produtoId) }}</td>
              <td class="td-mono">{{ s.numero }}</td>
              <td class="small">{{ inventory.nomeDeposito(s.depositoId) }}</td>
              <td>
                <span class="badge" [ngClass]="s.status === 'em_estoque' ? 'auth' : 'rascunho'">
                  <span class="badge-dot"></span>{{ s.status === 'em_estoque' ? 'Em estoque' : 'Saída emitida' }}
                </span>
              </td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  `,
  styles: [`
    .window { display: flex; align-items: center; gap: 8px; font-size: 12px; color: var(--text2); }
    .filter-select { height: 30px; border: 1px solid var(--border2); border-radius: var(--r8); font-family: var(--font); font-size: 12px; padding: 0 10px; background: var(--surface); color: var(--text); cursor: pointer; }
    .muted { font-size: 11px; color: var(--text3); }
    .small { font-size: 11px; }
    .empty { text-align: center; padding: 24px; color: var(--text3); }
  `]
})
export class LotsTabComponent {
  private readonly data = inject(DataService);
  readonly inventory = inject(InventoryService);
  readonly janelas = JANELAS_DIAS;

  readonly lotes = computed(() => [...this.inventory.lotes()].sort((a, b) => a.validade.getTime() - b.validade.getTime()));

  nomeProduto(produtoId: string): string {
    return this.data.produtos().find(p => p.id === produtoId)?.descricao ?? produtoId;
  }

  situacao(lote: LoteEstoque): { rotulo: string; badge: string } {
    return STATUS[this.inventory.statusValidade(lote)];
  }

  prazo(lote: LoteEstoque): string {
    const dias = this.inventory.diasParaVencer(lote);
    if (dias < 0) return `vencido há ${-dias}d`;
    return dias === 0 ? 'vence hoje' : `em ${dias}d`;
  }
}
