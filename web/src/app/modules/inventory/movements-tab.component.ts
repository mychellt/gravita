import { DatePipe, NgClass } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { MovimentoTipo } from '../../core/models';
import { DataService } from '../../core/services/data.service';
import { InventoryService } from '../../core/services/inventory.service';

const TIPO_ROTULO: Record<MovimentoTipo, string> = {
  entrada: '↑ Entrada', saida: '↓ Saída', ajuste: '± Ajuste', transferencia: '⇄ Transferência',
};
const TIPO_BADGE: Record<MovimentoTipo, string> = {
  entrada: 'auth', saida: 'canc', ajuste: 'rascunho', transferencia: 'proc',
};
const NIVEL_CRITICO = 20;
const NIVEL_ATENCAO = 50;

/** Append-only movement log, filterable by product, type, warehouse and period. */
@Component({
  selector: 'app-movements-tab',
  standalone: true,
  imports: [DatePipe, NgClass],
  template: `
    <div class="card">
      <div class="card-head"><span class="card-title">Histórico de movimentações</span></div>
      <div class="filter-bar">
        <div class="filters">
          <input class="filter-input" type="text" placeholder="Buscar produto..." aria-label="Buscar produto"
                 [value]="busca()" (input)="busca.set($any($event.target).value)" />
          <select class="filter-select" aria-label="Tipo" (change)="tipo.set($any($event.target).value)">
            <option value="">Todos os tipos</option>
            <option value="entrada">Entrada</option>
            <option value="saida">Saída</option>
            <option value="ajuste">Ajuste</option>
            <option value="transferencia">Transferência</option>
          </select>
          <select class="filter-select" aria-label="Depósito" (change)="deposito.set($any($event.target).value)">
            <option value="">Todos os depósitos</option>
            @for (d of inventory.depositos(); track d.id) { <option [value]="d.id">{{ d.nome }}</option> }
          </select>
          <input class="filter-input date" type="date" aria-label="De" (change)="de.set($any($event.target).value)" />
          <input class="filter-input date" type="date" aria-label="Até" (change)="ate.set($any($event.target).value)" />
        </div>
      </div>
      <table class="data-table">
        <thead>
          <tr>
            <th>Data/Hora</th><th>Produto</th><th>Tipo</th><th>Depósito</th><th>Quantidade</th>
            <th>Saldo após</th><th>Lote / Série</th><th>Origem</th><th>Usuário</th>
          </tr>
        </thead>
        <tbody>
          @for (m of movimentos(); track m.id) {
            <tr>
              <td class="muted">{{ m.dataHora | date:'dd/MM HH:mm' }}</td>
              <td class="td-name">{{ m.produtoNome }}</td>
              <td>
                <span class="badge" [ngClass]="badge(m.tipo)"><span class="badge-dot"></span>{{ rotulo(m.tipo) }}</span>
              </td>
              <td class="small">{{ m.depositoId ? inventory.nomeDeposito(m.depositoId) : '—' }}</td>
              <td class="td-mono" [class.text-success]="m.quantidade > 0" [class.text-danger]="m.quantidade < 0">
                {{ m.quantidade > 0 ? '+' : '' }}{{ m.quantidade }}
              </td>
              <td class="td-mono" [class.text-danger]="m.saldoApos <= critico" [class.text-warn]="m.saldoApos > critico && m.saldoApos <= atencao">
                {{ m.saldoApos }}
                @if (m.saldoApos <= critico) { <i class="ti ti-alert-triangle small"></i> }
              </td>
              <td class="td-mono small">{{ m.lote ?? m.serie ?? '—' }}</td>
              <td class="small">
                {{ m.origem }}
                @if (m.justificativa) { <div class="muted">{{ m.justificativa }}</div> }
              </td>
              <td class="muted">{{ m.usuario }}</td>
            </tr>
          } @empty {
            <tr><td colspan="9" class="empty">Nenhuma movimentação encontrada</td></tr>
          }
        </tbody>
      </table>
    </div>
  `,
  styles: [`
    .filter-bar { padding: 10px 14px; border-bottom: 1px solid var(--border); }
    .filters { display: flex; gap: 8px; flex-wrap: wrap; }
    .filter-input, .filter-select { height: 30px; border: 1px solid var(--border2); border-radius: var(--r8); font-family: var(--font); font-size: 12px; padding: 0 10px; outline: none; background: var(--surface); color: var(--text); }
    .filter-input:focus, .filter-select:focus { border-color: var(--accent); }
    .filter-input { width: 160px; }
    .filter-input.date { width: 128px; }
    .filter-select { cursor: pointer; }
    .muted { font-size: 11px; color: var(--text3); }
    .small { font-size: 11px; }
    .empty { text-align: center; padding: 24px; color: var(--text3); }
  `]
})
export class MovementsTabComponent {
  private readonly data = inject(DataService);
  readonly inventory = inject(InventoryService);

  readonly critico = NIVEL_CRITICO;
  readonly atencao = NIVEL_ATENCAO;

  readonly busca = signal('');
  readonly tipo = signal('');
  readonly deposito = signal('');
  readonly de = signal('');
  readonly ate = signal('');

  readonly movimentos = computed(() => {
    const busca = this.busca().toLowerCase();
    const inicio = this.de() ? new Date(`${this.de()}T00:00:00`).getTime() : -Infinity;
    const fim = this.ate() ? new Date(`${this.ate()}T23:59:59`).getTime() : Infinity;
    return this.data.movimentos().filter(m =>
      (!this.tipo() || m.tipo === this.tipo()) &&
      (!this.deposito() || m.depositoId === this.deposito()) &&
      (!busca || m.produtoNome.toLowerCase().includes(busca)) &&
      m.dataHora.getTime() >= inicio && m.dataHora.getTime() <= fim);
  });

  rotulo(tipo: MovimentoTipo): string {
    return TIPO_ROTULO[tipo];
  }

  badge(tipo: MovimentoTipo): string {
    return TIPO_BADGE[tipo];
  }
}
