import { Component, EventEmitter, Input, OnInit, Output, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { InventoryChangesService } from '../../../core/services/inventory-changes.service';
import { ConferenceLine, InboundNfeListItem, NfeService, nfeFailureDetail } from '../../../core/services/nfe.service';
import { ToastService } from '../../../core/services/toast.service';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { ProductPickerComponent } from './product-picker.component';

interface LineDraft {
  description: string;
  productId: string;
  orderedQty: number | null;
  receivedQty: number | null;
}

const emptyLine = (description = '', orderedQty: number | null = null): LineDraft =>
  ({ description, productId: '', orderedQty, receivedQty: orderedQty });

const lineValid = (line: LineDraft) =>
  !!line.productId && line.orderedQty !== null && line.orderedQty >= 0 && line.receivedQty !== null && line.receivedQty >= 0;

@Component({
  selector: 'app-confirm-receipt-modal',
  standalone: true,
  imports: [FormsModule, ModalComponent, ProductPickerComponent],
  template: `
    <app-modal [title]="'Conferir NF-e #' + nfe.number + ' — ' + nfe.supplierName" [width]="760" (closed)="closed.emit()">
      <div class="impact-note">
        <i class="ti ti-info-circle"></i>
        <span>
          Escolha o produto do catálogo para cada item, na mesma ordem da nota, e informe o que chegou.
          Ao confirmar, o estoque recebe a entrada e a nota não pode mais ser conferida.
        </span>
      </div>
      @if (!itensConhecidos()) {
        <div class="impact-note" role="note">
          <i class="ti ti-alert-triangle"></i>
          <span>
            Os itens desta nota não estão disponíveis nesta tela: inclua uma linha para cada item, na ordem da NF-e.
            O backend recusa a confirmação se o número de linhas for diferente do número de itens da nota.
          </span>
        </div>
      }
      @for (line of linhas(); track $index) {
        <div class="line" [attr.aria-label]="'Item ' + ($index + 1)">
          <div class="line-head">
            <strong>{{ line.description || 'Item ' + ($index + 1) }}</strong>
            @if (!itensConhecidos()) {
              <button class="btn btn-ghost btn-sm" type="button" aria-label="Remover item"
                      [disabled]="linhas().length === 1" (click)="remover($index)"><i class="ti ti-trash"></i></button>
            }
          </div>
          <app-product-picker [label]="'Produto do item ' + ($index + 1)" [invalid]="tentouEnviar() && !line.productId"
                              (picked)="alterar($index, { productId: $event })" />
          @if (tentouEnviar() && !line.productId) { <div class="field-error">Escolha o produto deste item.</div> }
          <div class="form-row cols-2 mt-8">
            <div>
              <label class="form-label" [for]="'conf-nf-' + $index">Qtd. na nota / pedida</label>
              <input [id]="'conf-nf-' + $index" class="form-input" type="number" min="0" step="any"
                     [class.invalid]="tentouEnviar() && line.orderedQty === null"
                     [ngModel]="line.orderedQty" (ngModelChange)="alterar($index, { orderedQty: $event })" />
            </div>
            <div>
              <label class="form-label" [for]="'conf-rec-' + $index">Qtd. recebida</label>
              <input [id]="'conf-rec-' + $index" class="form-input" type="number" min="0" step="any"
                     [class.invalid]="tentouEnviar() && line.receivedQty === null"
                     [ngModel]="line.receivedQty" (ngModelChange)="alterar($index, { receivedQty: $event })" />
              @if (divergente(line)) { <div class="field-hint">Diverge da quantidade da nota.</div> }
            </div>
          </div>
        </div>
      }
      @if (!itensConhecidos()) {
        <button class="btn btn-ghost btn-sm" type="button" (click)="adicionar()"><i class="ti ti-plus"></i> Adicionar item</button>
      }
      @if (erro()) { <div class="field-error" role="alert">{{ erro() }}</div> }
      <div footer>
        <button class="btn btn-ghost" type="button" (click)="closed.emit()">Cancelar</button>
        <button class="btn btn-primary" type="button" [disabled]="enviando()" (click)="confirmar()">
          {{ enviando() ? 'Confirmando…' : 'Confirmar recebimento' }}
        </button>
      </div>
    </app-modal>
  `,
  styles: [`
    .line { border: 1px solid var(--border); border-radius: var(--r8, 8px); padding: 12px; margin-bottom: 10px; }
    .line-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
  `]
})
export class ConfirmReceiptModalComponent implements OnInit {
  private readonly service = inject(NfeService);
  private readonly toast = inject(ToastService);
  private readonly inventoryChanges = inject(InventoryChangesService);

  @Input({ required: true }) nfe!: InboundNfeListItem;
  @Output() closed = new EventEmitter<void>();

  readonly linhas = signal<LineDraft[]>([emptyLine()]);
  readonly itensConhecidos = signal(false);
  readonly tentouEnviar = signal(false);
  readonly enviando = signal(false);
  readonly erro = signal('');
  private readonly todasValidas = computed(() => this.linhas().every(lineValid));

  ngOnInit(): void {
    const known = this.service.knownItemsOf(this.nfe.id);
    if (known === null) return;
    this.itensConhecidos.set(true);
    this.linhas.set(typeof known === 'number'
      ? Array.from({ length: known }, (_, i) => emptyLine(`Item ${i + 1}`))
      : known.map(item => emptyLine(item.description, item.quantity)));
  }

  divergente(line: LineDraft): boolean {
    return line.orderedQty !== null && line.receivedQty !== null && line.orderedQty !== line.receivedQty;
  }

  adicionar(): void {
    this.linhas.update(list => [...list, emptyLine()]);
  }

  remover(index: number): void {
    this.linhas.update(list => list.filter((_, i) => i !== index));
  }

  alterar(index: number, patch: Partial<LineDraft>): void {
    this.linhas.update(list => list.map((line, i) => i === index ? { ...line, ...patch } : line));
  }

  /** POST /api/nfe/inbound/{id}/confirm-receipt; só fecha depois do sucesso, mantendo o modal aberto nas falhas. */
  async confirmar(): Promise<void> {
    this.tentouEnviar.set(true);
    if (!this.todasValidas() || this.enviando()) return;
    this.enviando.set(true);
    this.erro.set('');
    try {
      await this.service.confirmInboundReceipt(this.nfe.id, this.linhas().map(toConferenceLine));
      this.inventoryChanges.stockChanged();
      this.toast.success(`NF-e #${this.nfe.number} confirmada — entrada registrada no estoque`);
      this.closed.emit();
    } catch (error) {
      this.erro.set((await nfeFailureDetail(error)) || 'Não foi possível confirmar o recebimento.');
      this.enviando.set(false);
    }
  }
}

function toConferenceLine(line: LineDraft): ConferenceLine {
  return { itemRef: line.productId, orderedQty: line.orderedQty ?? 0, receivedQty: line.receivedQty ?? 0 };
}
