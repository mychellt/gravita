import { Component, computed, inject, signal } from '@angular/core';
import { DataService } from '../../core/services/data.service';
import { InventoryService } from '../../core/services/inventory.service';
import { BrlPipe } from '../../shared/pipes/brl.pipe';
import { ProductDetailModalComponent } from './product-detail-modal.component';

interface LinhaSaldo {
  produtoId: string;
  descricao: string;
  depositoId: string;
  fisico: number;
  reservado: number;
  emTransito: number;
  disponivel: number;
  custoMedio: number;
}

/** Balance per product × warehouse (on hand, reserved, in transit, available, average cost). */
@Component({
  selector: 'app-balances-tab',
  standalone: true,
  imports: [BrlPipe, ProductDetailModalComponent],
  template: `
    <div class="card">
      <div class="card-head">
        <span class="card-title">Saldo por depósito</span>
        <div class="filters">
          <input class="filter-input" type="text" placeholder="Buscar produto..." aria-label="Buscar produto"
                 [value]="busca()" (input)="busca.set($any($event.target).value)" />
          <select class="filter-select" aria-label="Depósito" (change)="deposito.set($any($event.target).value)">
            <option value="">Todos os depósitos</option>
            @for (d of inventory.depositos(); track d.id) { <option [value]="d.id">{{ d.nome }}</option> }
          </select>
        </div>
      </div>
      <table class="data-table">
        <thead>
          <tr>
            <th>Produto</th><th>Depósito</th><th class="td-right">Físico</th><th class="td-right">Reservado</th>
            <th class="td-right">Em trânsito</th><th class="td-right">Disponível</th><th class="td-right">Custo médio</th>
            <th>Saldo negativo</th><th></th>
          </tr>
        </thead>
        <tbody>
          @for (l of linhas(); track l.produtoId + l.depositoId) {
            <tr>
              <td class="td-name">{{ l.descricao }}</td>
              <td class="small">{{ inventory.nomeDeposito(l.depositoId) }}</td>
              <td class="td-mono td-right">{{ l.fisico }}</td>
              <td class="td-mono td-right">{{ l.reservado }}</td>
              <td class="td-mono td-right">{{ l.emTransito }}</td>
              <td class="td-mono td-right" [class.text-danger]="l.disponivel < 0">{{ l.disponivel }}</td>
              <td class="td-mono td-right">{{ l.custoMedio | brl }}</td>
              <td>
                <div class="toggle" role="switch" tabindex="0" [class.on]="permiteNegativo(l.produtoId)"
                     [attr.aria-checked]="permiteNegativo(l.produtoId)" [attr.aria-label]="'Permitir saldo negativo de ' + l.descricao"
                     (click)="alternarNegativo(l.produtoId)" (keydown.enter)="alternarNegativo(l.produtoId)"></div>
              </td>
              <td class="td-actions">
                <button class="btn btn-ghost btn-sm" type="button" (click)="detalhe.set(l.produtoId)">Detalhes</button>
              </td>
            </tr>
          } @empty {
            <tr><td colspan="9" class="empty">Nenhum saldo encontrado</td></tr>
          }
        </tbody>
      </table>
    </div>

    @if (detalhe(); as produtoId) {
      <app-product-detail-modal [produtoId]="produtoId" (closed)="detalhe.set(null)" />
    }
  `,
  styles: [`
    .filters { display: flex; gap: 8px; }
    .filter-input, .filter-select { height: 30px; border: 1px solid var(--border2); border-radius: var(--r8); font-family: var(--font); font-size: 12px; padding: 0 10px; outline: none; background: var(--surface); color: var(--text); }
    .filter-input { width: 180px; }
    .small { font-size: 11px; }
    .empty { text-align: center; padding: 24px; color: var(--text3); }
  `]
})
export class BalancesTabComponent {
  private readonly data = inject(DataService);
  readonly inventory = inject(InventoryService);

  readonly busca = signal('');
  readonly deposito = signal('');
  readonly detalhe = signal<string | null>(null);

  readonly linhas = computed<LinhaSaldo[]>(() => {
    const busca = this.busca().toLowerCase();
    return this.inventory.saldos().flatMap(saldo => {
      const produto = this.data.produtos().find(p => p.id === saldo.produtoId);
      const visivel = produto && (!this.deposito() || saldo.depositoId === this.deposito())
        && (!busca || produto.descricao.toLowerCase().includes(busca));
      if (!produto || !visivel) return [];
      const disponivel = this.inventory.disponivel(saldo);
      return [{
        produtoId: produto.id, descricao: produto.descricao, depositoId: saldo.depositoId,
        fisico: saldo.fisico, reservado: saldo.reservado, emTransito: saldo.emTransito, disponivel,
        custoMedio: saldo.custoMedio,
      }];
    });
  });

  permiteNegativo(produtoId: string): boolean {
    return this.data.produtos().find(p => p.id === produtoId)?.permiteEstoqueNegativo === true;
  }

  alternarNegativo(produtoId: string): void {
    this.inventory.definirPermiteEstoqueNegativo(produtoId, !this.permiteNegativo(produtoId));
  }
}
