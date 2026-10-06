import { Component, inject } from '@angular/core';
import { SugestaoReposicao } from '../../core/models';
import { InventoryService } from '../../core/services/inventory.service';
import { ToastService } from '../../core/services/toast.service';

/** Items whose available balance reached the reorder point, with the quantity that refills them to the maximum. */
@Component({
  selector: 'app-reorder-tab',
  standalone: true,
  template: `
    <div class="card">
      <div class="card-head">
        <span class="card-title">Sugestões de reposição</span>
        <button class="btn btn-primary btn-sm" type="button" [disabled]="!pendentes().length" (click)="pedirTodas()">
          Criar requisições ({{ pendentes().length }})
        </button>
      </div>
      <table class="data-table">
        <thead>
          <tr><th>Produto</th><th class="td-right">Disponível (todos os depósitos)</th><th class="td-right">Ponto de reposição</th><th class="td-right">Qtd sugerida</th><th></th></tr>
        </thead>
        <tbody>
          @for (s of inventory.sugestoesReposicao(); track s.produtoId) {
            <tr>
              <td class="td-name">{{ s.produtoNome }}</td>
              <td class="td-mono td-right text-danger">{{ s.disponivel }}</td>
              <td class="td-mono td-right">{{ s.pontoReposicao }}</td>
              <td class="td-mono td-right">{{ s.quantidadeSugerida }}</td>
              <td class="td-actions">
                @if (inventory.jaRequisitado(s)) {
                  <span class="done"><i class="ti ti-check"></i> Requisição criada</span>
                } @else {
                  <button class="btn btn-ghost btn-sm" type="button" (click)="pedir(s)">Criar requisição</button>
                }
              </td>
            </tr>
          } @empty {
            <tr><td colspan="5" class="empty">Nenhum item atingiu o ponto de reposição</td></tr>
          }
        </tbody>
      </table>
    </div>
  `,
  styles: [`
    .done { font-size: 11px; color: var(--success); }
    .empty { text-align: center; padding: 24px; color: var(--text3); }
  `]
})
export class ReorderTabComponent {
  private readonly toast = inject(ToastService);
  readonly inventory = inject(InventoryService);

  pendentes(): SugestaoReposicao[] {
    return this.inventory.sugestoesReposicao().filter(s => !this.inventory.jaRequisitado(s));
  }

  pedir(sugestao: SugestaoReposicao): void {
    const resultado = this.inventory.solicitarCompra(sugestao);
    if (!resultado.ok) return this.toast.danger(resultado.erro);
    this.toast.success(`Requisição de compra criada para "${sugestao.produtoNome}"`);
  }

  pedirTodas(): void {
    const pendentes = this.pendentes();
    pendentes.forEach(s => this.inventory.solicitarCompra(s));
    this.toast.success(`${pendentes.length} requisição(ões) de compra criada(s)`);
  }
}
