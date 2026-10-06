import { DatePipe, NgClass } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DataService } from '../../core/services/data.service';
import { InventoryService } from '../../core/services/inventory.service';
import { ToastService } from '../../core/services/toast.service';
import { ModalComponent } from '../../shared/components/modal/modal.component';

/** Two-step transfers: dispatch moves stock to "in transit" at the destination until it is confirmed there. */
@Component({
  selector: 'app-transfers-tab',
  standalone: true,
  imports: [DatePipe, NgClass, FormsModule, ModalComponent],
  template: `
    <div class="card">
      <div class="card-head">
        <span class="card-title">Transferências entre depósitos</span>
        <button class="btn btn-primary btn-sm" type="button" (click)="aberto.set(true)">
          <i class="ti ti-plus"></i> Nova transferência
        </button>
      </div>
      <table class="data-table">
        <thead>
          <tr><th>Criada em</th><th>Produto</th><th>Lote</th><th>Origem</th><th>Destino</th><th class="td-right">Quantidade</th><th>Situação</th><th></th></tr>
        </thead>
        <tbody>
          @for (t of inventory.transferencias(); track t.id) {
            <tr>
              <td class="muted">{{ t.criadaEm | date:'dd/MM HH:mm' }}</td>
              <td class="td-name">{{ t.produtoNome }}</td>
              <td class="td-mono small">{{ t.lote ?? '—' }}</td>
              <td class="small">{{ inventory.nomeDeposito(t.origemId) }}</td>
              <td class="small">{{ inventory.nomeDeposito(t.destinoId) }}</td>
              <td class="td-mono td-right">{{ t.quantidade }}</td>
              <td>
                <span class="badge" [ngClass]="t.status === 'pendente' ? 'proc' : 'auth'">
                  <span class="badge-dot"></span>{{ t.status === 'pendente' ? 'Em trânsito' : 'Confirmada' }}
                </span>
              </td>
              <td class="td-actions">
                @if (t.status === 'pendente') {
                  <button class="btn btn-ghost btn-sm" type="button" (click)="confirmar(t.id)">Confirmar recebimento</button>
                }
              </td>
            </tr>
          } @empty {
            <tr><td colspan="8" class="empty">Nenhuma transferência registrada</td></tr>
          }
        </tbody>
      </table>
    </div>

    @if (aberto()) {
      <app-modal title="Nova transferência" (closed)="fechar()">
        <div class="form-row">
          <div>
            <label class="form-label" for="tr-produto">Produto</label>
            <select id="tr-produto" class="form-select" [ngModel]="produtoId()" (ngModelChange)="trocarProduto($event)">
              @for (p of produtos(); track p.id) { <option [value]="p.id">{{ p.descricao }}</option> }
            </select>
          </div>
        </div>
        <div class="form-row cols-2">
          <div>
            <label class="form-label" for="tr-origem">Origem</label>
            <select id="tr-origem" class="form-select" [ngModel]="origemId()" (ngModelChange)="origemId.set($event)">
              @for (d of inventory.depositos(); track d.id) { <option [value]="d.id">{{ d.nome }}</option> }
            </select>
          </div>
          <div>
            <label class="form-label" for="tr-destino">Destino</label>
            <select id="tr-destino" class="form-select" [ngModel]="destinoId()" (ngModelChange)="destinoId.set($event)">
              @for (d of inventory.depositos(); track d.id) { <option [value]="d.id">{{ d.nome }}</option> }
            </select>
          </div>
        </div>
        <div class="form-row cols-2">
          <div>
            <label class="form-label" for="tr-qtd">Quantidade</label>
            <input id="tr-qtd" class="form-input" type="number" min="0" step="any"
                   [ngModel]="quantidade()" (ngModelChange)="quantidade.set($event)" />
            <div class="field-hint">Disponível na origem: {{ disponivelNaOrigem() }}</div>
          </div>
          @if (lotesDaOrigem().length) {
            <div>
              <label class="form-label" for="tr-lote">Lote</label>
              <select id="tr-lote" class="form-select" [ngModel]="lote()" (ngModelChange)="lote.set($event)">
                <option value="">Selecione…</option>
                @for (l of lotesDaOrigem(); track l.codigo) { <option [value]="l.codigo">{{ l.codigo }} ({{ l.quantidade }})</option> }
              </select>
            </div>
          }
        </div>
        <div footer>
          <button class="btn btn-ghost" type="button" (click)="fechar()">Cancelar</button>
          <button class="btn btn-primary" type="button" (click)="salvar()">Enviar</button>
        </div>
      </app-modal>
    }
  `,
  styles: [`
    .muted { font-size: 11px; color: var(--text3); }
    .small { font-size: 11px; }
    .empty { text-align: center; padding: 24px; color: var(--text3); }
  `]
})
export class TransfersTabComponent {
  private readonly data = inject(DataService);
  private readonly toast = inject(ToastService);
  readonly inventory = inject(InventoryService);

  readonly aberto = signal(false);
  readonly produtoId = signal('');
  readonly origemId = signal(this.inventory.depositos()[0].id);
  readonly destinoId = signal(this.inventory.depositos()[1]?.id ?? '');
  readonly quantidade = signal<number | null>(null);
  readonly lote = signal('');

  readonly produtos = computed(() => this.data.produtos().filter(p => p.tipo !== 'servico' && p.status === 'ativo'));

  readonly disponivelNaOrigem = computed(() => {
    const saldo = this.inventory.saldosDoProduto(this.produtoId()).find(s => s.depositoId === this.origemId());
    return saldo ? this.inventory.disponivel(saldo) : 0;
  });

  readonly lotesDaOrigem = computed(() => this.inventory.lotes().filter(l =>
    l.produtoId === this.produtoId() && l.depositoId === this.origemId() && l.quantidade > 0));

  constructor() {
    this.produtoId.set(this.produtos()[0]?.id ?? '');
  }

  trocarProduto(produtoId: string): void {
    this.produtoId.set(produtoId);
    this.lote.set('');
  }

  salvar(): void {
    const resultado = this.inventory.iniciarTransferencia({
      produtoId: this.produtoId(), origemId: this.origemId(), destinoId: this.destinoId(),
      quantidade: Number(this.quantidade()) || 0, lote: this.lote() || undefined,
    });
    if (!resultado.ok) return this.toast.danger(resultado.erro);
    this.toast.success('Transferência enviada — estoque em trânsito até a confirmação no destino');
    this.fechar();
  }

  confirmar(id: string): void {
    const resultado = this.inventory.confirmarTransferencia(id);
    if (!resultado.ok) return this.toast.danger(resultado.erro);
    this.toast.success('Recebimento confirmado');
  }

  fechar(): void {
    this.aberto.set(false);
    this.quantidade.set(null);
    this.lote.set('');
  }
}
