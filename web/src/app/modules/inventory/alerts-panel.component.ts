import { Component, EventEmitter, Output, computed, inject } from '@angular/core';
import { LoteEstoque, SugestaoReposicao } from '../../core/models';
import { DataService } from '../../core/services/data.service';
import { InventoryService } from '../../core/services/inventory.service';
import { ToastService } from '../../core/services/toast.service';

/** Side panel of the inventory screen: min-stock, expiring lots and pending transfers in one place. */
@Component({
  selector: 'app-alerts-panel',
  standalone: true,
  template: `
    <div class="card">
      <div class="card-head"><span class="card-title">Alertas críticos</span></div>

      @for (s of sugestoes(); track s.produtoId) {
        <div class="alert-item">
          <div class="alert-dot red"></div>
          <div class="alert-info">
            <div class="alert-prod">{{ s.produtoNome }}</div>
            <div class="alert-stock text-danger">{{ s.disponivel }} disp. (mín: {{ s.pontoReposicao }})</div>
          </div>
          @if (inventory.jaRequisitado(s)) {
            <span class="done"><i class="ti ti-check"></i> Pedido</span>
          } @else {
            <button class="btn btn-ghost btn-sm" type="button" (click)="pedir(s)">Pedir</button>
          }
        </div>
      }

      @for (l of lotes(); track l.codigo + l.depositoId) {
        <div class="alert-item">
          <div class="alert-dot amber"></div>
          <div class="alert-info">
            <div class="alert-prod">{{ nomeProduto(l.produtoId) }} · {{ l.codigo }}</div>
            <div class="alert-stock text-warn">{{ textoValidade(l) }}</div>
          </div>
          <button class="btn btn-ghost btn-sm" type="button" (click)="verLotes.emit()">Ver lote</button>
        </div>
      }

      @for (t of inventory.transferenciasPendentes(); track t.id) {
        <div class="alert-item">
          <div class="alert-dot blue"></div>
          <div class="alert-info">
            <div class="alert-prod">Transferência pendente</div>
            <div class="alert-stock info">{{ t.quantidade }} un {{ t.produtoNome }} → {{ inventory.nomeDeposito(t.destinoId) }}</div>
          </div>
          <button class="btn btn-ghost btn-sm" type="button" (click)="confirmar(t.id)">Confirmar</button>
        </div>
      }

      @if (!sugestoes().length && !lotes().length && !inventory.transferenciasPendentes().length) {
        <div class="alert-item"><div class="alert-info muted">Nenhum alerta no momento.</div></div>
      }
    </div>
  `,
  styles: [`
    .alert-item { display: flex; align-items: center; gap: 10px; padding: 10px 14px; border-bottom: 1px solid var(--border); }
    .alert-item:last-child { border-bottom: none; }
    .alert-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
    .alert-dot.red   { background: var(--danger); }
    .alert-dot.amber { background: var(--warn); }
    .alert-dot.blue  { background: var(--info); }
    .alert-info  { flex: 1; }
    .alert-prod  { font-size: 12px; font-weight: 500; }
    .alert-stock { font-size: 11px; margin-top: 1px; }
    .info  { color: var(--info); }
    .muted { font-size: 12px; color: var(--text3); }
    .done  { font-size: 11px; color: var(--success); }
  `]
})
export class AlertsPanelComponent {
  private readonly data = inject(DataService);
  private readonly toast = inject(ToastService);
  readonly inventory = inject(InventoryService);

  @Output() verLotes = new EventEmitter<void>();

  readonly sugestoes = this.inventory.sugestoesReposicao;
  readonly lotes = computed(() => this.inventory.lotesAVencer());

  nomeProduto(produtoId: string): string {
    return this.data.produtos().find(p => p.id === produtoId)?.descricao ?? produtoId;
  }

  textoValidade(lote: LoteEstoque): string {
    const dias = this.inventory.diasParaVencer(lote);
    if (dias < 0) return `Lote vencido há ${-dias} dia(s)`;
    return dias === 0 ? 'Lote vence hoje' : `Lote vence em ${dias} dia(s)`;
  }

  pedir(sugestao: SugestaoReposicao): void {
    const resultado = this.inventory.solicitarCompra(sugestao);
    if (!resultado.ok) return this.toast.danger(resultado.erro);
    this.toast.success(`Requisição de compra criada para "${sugestao.produtoNome}"`);
  }

  confirmar(transferenciaId: string): void {
    const resultado = this.inventory.confirmarTransferencia(transferenciaId);
    if (!resultado.ok) return this.toast.danger(resultado.erro);
    this.toast.success('Transferência confirmada');
  }
}
