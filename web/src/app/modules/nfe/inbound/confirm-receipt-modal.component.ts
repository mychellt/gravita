import { Component, EventEmitter, Input, OnInit, Output, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { BrlPipe } from '../../../shared/pipes/brl.pipe';
import { InventoryChangesService } from '../../../core/services/inventory-changes.service';
import {
  ConferenceLine, InboundNfeConferenceItem, InboundNfeConferenceView, InboundNfeListItem, NfeService, RegisteredProduct,
  nfeFailureDetail,
} from '../../../core/services/nfe.service';
import { ProductSearchResult } from '../../../core/services/product-search.service';
import { ToastService } from '../../../core/services/toast.service';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { ProductPickerComponent } from './product-picker.component';
import { RegisterProductFromItemModalComponent } from './register-product-from-item-modal.component';

/** Uma linha da conferência: dados da nota (imutáveis) + a resolução de produto, que pode mudar nesta tela. */
interface LineView extends InboundNfeConferenceItem {
  orderedQty: number | null;
  receivedQty: number | null;
}

const toLine = (item: InboundNfeConferenceItem): LineView =>
  ({ ...item, orderedQty: item.quantity, receivedQty: item.quantity });

@Component({
  selector: 'app-confirm-receipt-modal',
  standalone: true,
  imports: [FormsModule, BrlPipe, ModalComponent, ProductPickerComponent, RegisterProductFromItemModalComponent],
  template: `
    <app-modal [title]="'Conferir NF-e #' + nfe.number + ' — ' + nfe.supplierName" [width]="780" (closed)="closed.emit()">
      @if (carregando()) {
        <div class="nfe-state">Carregando conferência…</div>
      } @else if (erroCarregar()) {
        <div class="alert danger" role="alert">
          {{ erroCarregar() }}
          <button class="action-btn" (click)="carregar()">Tentar novamente</button>
        </div>
      } @else {
        @if (!vista()?.supplierResolved) {
          <div class="alert danger" role="alert">
            <i class="ti ti-building-off"></i>
            <span>Nenhum fornecedor está cadastrado para o CNPJ desta nota. Cadastre o fornecedor antes de confirmar o recebimento.</span>
          </div>
        } @else {
          <div class="impact-note">
            <i class="ti ti-info-circle"></i>
            <span>Resolva cada item sem produto vinculado e informe o que chegou. Ao confirmar, o estoque recebe a entrada e a nota não pode mais ser conferida.</span>
          </div>
        }

        @for (line of linhas(); track $index) {
          <div class="line" [attr.aria-label]="'Item ' + ($index + 1)">
            <div class="line-head">
              <strong>{{ line.description }}</strong>
              <span class="field-hint">cód. {{ line.supplierProductCode }} · NCM {{ line.ncm }} · {{ line.unit }}</span>
            </div>

            @if (line.resolvedProductId) {
              <div class="resolved-product"><i class="ti ti-link"></i> Produto vinculado: <strong>{{ line.resolvedProductLabel }}</strong></div>
              @if (variancia(line) !== null) {
                <div class="alert warn">
                  <i class="ti ti-alert-triangle"></i>
                  <span>
                    Preço nesta nota: {{ line.unitValue | brl }} · Última compra: {{ line.lastPurchaseUnitValue | brl }}
                    · Diferença: {{ variancia(line) | brl }}
                  </span>
                </div>
              }
            } @else if (vista()?.supplierResolved) {
              <div class="alert warn">
                <i class="ti ti-help-circle"></i>
                <span>Este item ainda não está vinculado a um produto do catálogo.</span>
              </div>
              <div class="resolve-actions">
                <button class="btn btn-ghost btn-sm" type="button" (click)="registeringIndex.set($index)">
                  <i class="ti ti-plus"></i> Cadastrar novo produto
                </button>
                <button class="btn btn-ghost btn-sm" type="button"
                        (click)="linkingIndex.set(linkingIndex() === $index ? null : $index)">
                  <i class="ti ti-search"></i> Vincular produto existente
                </button>
              </div>
              @if (linkingIndex() === $index) {
                <app-product-picker [label]="'Produto do item ' + ($index + 1)"
                                     (picked)="vincularExistente($index, $event)" />
              }
            }

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
        @if (erro()) { <div class="field-error" role="alert">{{ erro() }}</div> }
      }
      <div footer>
        <button class="btn btn-ghost" type="button" (click)="closed.emit()">Cancelar</button>
        <button class="btn btn-primary" type="button" [disabled]="carregando() || !!erroCarregar() || enviando()" (click)="confirmar()">
          {{ enviando() ? 'Confirmando…' : 'Confirmar recebimento' }}
        </button>
      </div>
    </app-modal>

    @if (registeringIndex(); as idx) {
      <app-register-product-from-item-modal [item]="linhas()[idx]" [inboundNfeId]="nfe.id" [itemIndex]="idx"
                                              (linked)="onNewProductLinked(idx, $event)" (closed)="registeringIndex.set(null)" />
    }
  `,
  styles: [`
    .line { border: 1px solid var(--border); border-radius: var(--r8, 8px); padding: 12px; margin-bottom: 10px; }
    .line-head { display: flex; justify-content: space-between; align-items: baseline; gap: 8px; margin-bottom: 8px; }
    .resolved-product { font-size: 12px; color: var(--text2); display: flex; align-items: center; gap: 6px; }
    .resolve-actions { display: flex; gap: 8px; margin-top: 8px; }
    .mt-8 { margin-top: 8px; }
  `]
})
export class ConfirmReceiptModalComponent implements OnInit {
  private readonly service = inject(NfeService);
  private readonly toast = inject(ToastService);
  private readonly inventoryChanges = inject(InventoryChangesService);

  @Input({ required: true }) nfe!: InboundNfeListItem;
  @Output() closed = new EventEmitter<void>();

  readonly carregando = signal(true);
  readonly erroCarregar = signal('');
  readonly vista = signal<InboundNfeConferenceView | null>(null);
  readonly linhas = signal<LineView[]>([]);
  readonly linkingIndex = signal<number | null>(null);
  readonly registeringIndex = signal<number | null>(null);
  readonly tentouEnviar = signal(false);
  readonly enviando = signal(false);
  readonly erro = signal('');

  private readonly todosResolvidos = computed(() => !!this.vista()?.supplierResolved && this.linhas().every(l => !!l.resolvedProductId));
  private readonly todasValidas = computed(() => this.todosResolvidos() && this.linhas().every(l =>
    l.orderedQty !== null && l.orderedQty >= 0 && l.receivedQty !== null && l.receivedQty >= 0));

  ngOnInit(): void {
    void this.carregar();
  }

  async carregar(): Promise<void> {
    this.carregando.set(true);
    this.erroCarregar.set('');
    try {
      const vista = await this.service.getConferenceView(this.nfe.id);
      this.vista.set(vista);
      this.linhas.set(vista.items.map(toLine));
    } catch {
      this.erroCarregar.set('Não foi possível carregar a conferência desta nota.');
    } finally {
      this.carregando.set(false);
    }
  }

  divergente(line: LineView): boolean {
    return line.orderedQty !== null && line.receivedQty !== null && line.orderedQty !== line.receivedQty;
  }

  /** BR7: a diferença entre o preço desta nota e o da última compra registrada no vínculo; `null` quando não há o que comparar. */
  variancia(line: LineView): number | null {
    if (line.lastPurchaseUnitValue === null || line.lastPurchaseUnitValue === line.unitValue) return null;
    return line.unitValue - line.lastPurchaseUnitValue;
  }

  alterar(index: number, patch: Partial<LineView>): void {
    this.linhas.update(list => list.map((line, i) => i === index ? { ...line, ...patch } : line));
  }

  /** BR5: vincula o item a um produto já existente — nenhum produto novo é criado. */
  async vincularExistente(index: number, resultado: ProductSearchResult | null): Promise<void> {
    if (!resultado) return;
    try {
      await this.service.linkExistingProduct(this.nfe.id, index, resultado.productId);
      this.alterar(index, { resolvedProductId: resultado.productId, resolvedProductLabel: resultado.description, lastPurchaseUnitValue: null });
      this.linkingIndex.set(null);
      this.toast.success(`Item vinculado a ${resultado.description}.`);
    } catch (error) {
      this.toast.danger((await nfeFailureDetail(error)) || 'Não foi possível vincular o produto.');
    }
  }

  /** BR4: o produto acabou de ser cadastrado (e vinculado) a partir deste item. */
  onNewProductLinked(index: number, produto: RegisteredProduct): void {
    this.alterar(index, { resolvedProductId: produto.id, resolvedProductLabel: produto.internalCode, lastPurchaseUnitValue: null });
    this.registeringIndex.set(null);
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

function toConferenceLine(line: LineView): ConferenceLine {
  return { itemRef: line.resolvedProductId ?? '', orderedQty: line.orderedQty ?? 0, receivedQty: line.receivedQty ?? 0 };
}
