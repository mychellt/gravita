import { Component, EventEmitter, Input, OnInit, Output, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { InventoryChangesService } from '../../../core/services/inventory-changes.service';
import {
  ConferenceLine, InboundConference, InboundConferenceItem, InboundNfeListItem, NfeService, nfeFailureDetail,
} from '../../../core/services/nfe.service';
import { ToastService } from '../../../core/services/toast.service';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { formatCnpj, formatDelta, formatUnitValue } from './inbound-format';
import { ProductPickerComponent } from './product-picker.component';
import { RegisterProductModalComponent } from './register-product-modal.component';
import { RegisterSupplierModalComponent } from './register-supplier-modal.component';

export interface LineDraft {
  item: InboundConferenceItem;
  /** Produto escolhido na busca, ainda não vinculado ao código do fornecedor. */
  pick: string;
  orderedQty: number | null;
  receivedQty: number | null;
}

const lineOf = (item: InboundConferenceItem): LineDraft =>
  ({ item, pick: '', orderedQty: item.quantity, receivedQty: item.quantity });

const lineValid = (line: LineDraft) =>
  line.item.resolved && line.orderedQty !== null && line.orderedQty >= 0 && line.receivedQty !== null && line.receivedQty >= 0;

@Component({
  selector: 'app-confirm-receipt-modal',
  standalone: true,
  imports: [FormsModule, ModalComponent, ProductPickerComponent, RegisterProductModalComponent, RegisterSupplierModalComponent],
  template: `
    <app-modal [title]="'Conferir NF-e #' + nfe.number + ' — ' + nfe.supplierName" [width]="760" (closed)="closed.emit()">
      <div class="impact-note">
        <i class="ti ti-info-circle"></i>
        <span>
          Para confirmar, a nota precisa de um fornecedor cadastrado e de um produto do catálogo em cada item.
          <strong>O que estiver faltando pode ser cadastrado aqui mesmo</strong>, sem sair desta tela. Informe o que
          chegou; ao confirmar, o estoque recebe a entrada e a nota não pode mais ser conferida.
        </span>
      </div>
      @if (carregamento() === 'loading') { <div class="field-hint">Carregando os itens da nota…</div> }
      @if (carregamento() === 'error') {
        <div class="field-error" role="alert">
          Não foi possível carregar os itens da nota.
          <button class="btn btn-ghost btn-sm" type="button" (click)="carregar()">Tentar novamente</button>
        </div>
      }
      @if (conferencia(); as conf) {
        <ul class="checklist" aria-label="Situação da conferência">
          <li [class.ok]="conf.supplierResolved" data-testid="check-supplier">
            <i class="ti" [class.ti-circle-check]="conf.supplierResolved" [class.ti-alert-circle]="!conf.supplierResolved"></i>
            @if (conf.supplierResolved) {
              Fornecedor cadastrado: <strong>{{ conf.supplierName }}</strong> ({{ cnpj(conf.supplierDocument) }})
            } @else {
              Fornecedor <strong>{{ conf.supplierName }}</strong> (CNPJ {{ cnpj(conf.supplierDocument) }}) ainda não está cadastrado.
              <button class="btn btn-primary btn-sm" type="button" data-testid="register-supplier"
                      (click)="cadastrandoFornecedor.set(true)">
                <i class="ti ti-plus"></i> Cadastrar fornecedor
              </button>
            }
          </li>
          <li [class.ok]="pendentes() === 0" data-testid="check-items">
            <i class="ti" [class.ti-circle-check]="pendentes() === 0" [class.ti-alert-circle]="pendentes() > 0"></i>
            {{ vinculados() }} de {{ linhas().length }} itens vinculados a um produto do catálogo.
            @if (pendentes() > 0) {
              {{ conf.supplierResolved
                ? 'Para os demais, cadastre o produto ou vincule um que já exista.'
                : 'Cadastre o fornecedor para liberar o cadastro e o vínculo dos produtos.' }}
            }
          </li>
        </ul>
      }
      @for (line of linhas(); track line.item.index) {
        <div class="line" [attr.aria-label]="'Item ' + (line.item.index + 1)">
          <div class="line-head">
            <strong>{{ line.item.description }}</strong>
            <span class="field-hint">cód. do fornecedor {{ line.item.supplierProductCode }}</span>
          </div>
          @if (line.item.resolved) {
            <div class="linked">
              <i class="ti ti-link"></i> Produto vinculado: <strong>{{ line.item.productLabel || line.item.productId }}</strong>
            </div>
            @if (houveVariacao(line.item)) {
              <div class="price-variance" role="status">
                <i class="ti ti-trending-up"></i>
                Preço mudou: última compra {{ valor(line.item.lastPurchaseUnitValue!) }} → nesta nota {{ valor(line.item.unitValue) }}
                ({{ variacao(line.item.priceDelta!) }} por unidade).
              </div>
            }
          } @else if (!conferencia()?.supplierResolved) {
            <div class="field-hint" [class.field-error]="tentouEnviar()" data-testid="item-needs-supplier">
              Este produto ainda não está vinculado. Cadastre o fornecedor primeiro para poder cadastrá-lo ou vinculá-lo.
            </div>
          } @else {
            <div class="field-hint" [class.field-error]="tentouEnviar()">
              Este produto ainda não existe no cadastro vinculado a este fornecedor: cadastre-o como novo ou vincule um produto que já exista.
            </div>
            <div class="resolve-actions">
              <button class="btn btn-ghost btn-sm" type="button" (click)="cadastrando.set(line.item)">
                <i class="ti ti-plus"></i> Cadastrar produto novo
              </button>
            </div>
            <app-product-picker [label]="'Produto existente do item ' + (line.item.index + 1)" [invalid]="false"
                                (picked)="alterar(line.item.index, { pick: $event })" />
            <button class="btn btn-ghost btn-sm mt-8" type="button" [disabled]="!line.pick || vinculando()"
                    (click)="vincular(line)">
              <i class="ti ti-link"></i> Vincular produto selecionado
            </button>
          }
          <div class="form-row cols-2 mt-8">
            <div>
              <label class="form-label" [for]="'conf-nf-' + line.item.index">Qtd. na nota / pedida</label>
              <input [id]="'conf-nf-' + line.item.index" class="form-input" type="number" min="0" step="any"
                     [class.invalid]="tentouEnviar() && line.orderedQty === null"
                     [ngModel]="line.orderedQty" (ngModelChange)="alterar(line.item.index, { orderedQty: $event })" />
            </div>
            <div>
              <label class="form-label" [for]="'conf-rec-' + line.item.index">Qtd. recebida</label>
              <input [id]="'conf-rec-' + line.item.index" class="form-input" type="number" min="0" step="any"
                     [class.invalid]="tentouEnviar() && line.receivedQty === null"
                     [ngModel]="line.receivedQty" (ngModelChange)="alterar(line.item.index, { receivedQty: $event })" />
              @if (divergente(line)) { <div class="field-hint">Diverge da quantidade da nota.</div> }
            </div>
          </div>
        </div>
      }
      @if (registrandoErro()) { <div class="field-error" role="alert">{{ registrandoErro() }}</div> }
      @if (erro()) { <div class="field-error" role="alert">{{ erro() }}</div> }
      <div footer>
        <button class="btn btn-ghost" type="button" (click)="closed.emit()">Cancelar</button>
        <button class="btn btn-primary" type="button" [disabled]="enviando() || carregamento() !== 'ready'" (click)="confirmar()">
          {{ enviando() ? 'Confirmando…' : 'Confirmar recebimento' }}
        </button>
      </div>
    </app-modal>
    @if (cadastrandoFornecedor() && conferencia(); as conf) {
      <app-register-supplier-modal [cnpj]="conf.supplierDocument" [nomeInicial]="conf.supplierName"
                                   (registered)="aoCadastrarFornecedor()" (closed)="cadastrandoFornecedor.set(false)" />
    }
    @if (cadastrando(); as item) {
      <app-register-product-modal [inboundNfeId]="nfe.id" [item]="item"
                                  (registered)="aoResolver()" (closed)="cadastrando.set(null)" />
    }
  `,
  styles: [`
    .line { border: 1px solid var(--border); border-radius: var(--r8, 8px); padding: 12px; margin-bottom: 10px; }
    .line-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
    .linked { margin-bottom: 6px; }
    .price-variance { background: #FFFBEB; color: #92400E; border: 1px solid #FDE68A; border-radius: var(--r8, 8px); padding: 8px 10px; font-size: 13px; }
    .resolve-actions { margin: 6px 0; }
    .checklist { list-style: none; margin: 0 0 12px; padding: 0; display: grid; gap: 6px; }
    .checklist li { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; font-size: 13px; color: #92400E; }
    .checklist li.ok { color: var(--text2); }
  `]
})
export class ConfirmReceiptModalComponent implements OnInit {
  private readonly service = inject(NfeService);
  private readonly toast = inject(ToastService);
  private readonly inventoryChanges = inject(InventoryChangesService);

  @Input({ required: true }) nfe!: InboundNfeListItem;
  @Output() closed = new EventEmitter<void>();

  readonly conferencia = signal<InboundConference | null>(null);
  readonly carregamento = signal<'loading' | 'ready' | 'error'>('loading');
  readonly linhas = signal<LineDraft[]>([]);
  /** Item sem vínculo para o qual o formulário de novo produto está aberto. */
  readonly cadastrando = signal<InboundConferenceItem | null>(null);
  /** Formulário de novo fornecedor aberto (CNPJ e nome vêm da conferência). */
  readonly cadastrandoFornecedor = signal(false);
  readonly vinculando = signal(false);
  readonly registrandoErro = signal('');
  readonly tentouEnviar = signal(false);
  readonly enviando = signal(false);
  readonly erro = signal('');
  readonly vinculados = computed(() => this.linhas().filter(line => line.item.resolved).length);
  readonly pendentes = computed(() => this.linhas().length - this.vinculados());
  /** O que impede a confirmação, em português; vazio quando a nota está pronta para confirmar. */
  private readonly impedimento = computed(() => {
    const linhas = this.linhas();
    if (!this.conferencia()?.supplierResolved) {
      return 'Cadastre o fornecedor desta NF-e (botão "Cadastrar fornecedor") antes de confirmar o recebimento.';
    }
    if (linhas.length === 0) return 'A nota não tem itens para conferir.';
    const semProduto = linhas.filter(line => !line.item.resolved).length;
    if (semProduto > 0) {
      return `Vincule ${semProduto === 1 ? 'o item sem produto' : `os ${semProduto} itens sem produto`} a um produto do catálogo antes de confirmar.`;
    }
    if (!linhas.every(lineValid)) return 'Informe as quantidades da nota e recebida de todos os itens (zero ou mais).';
    return '';
  });

  ngOnInit(): void {
    void this.carregar();
  }

  /** GET /api/nfe/inbound/{id}/conference — itens, fornecedor e vínculos resolvidos agora (nada disso é gravado na nota). */
  async carregar(): Promise<void> {
    this.erro.set('');
    this.carregamento.set('loading');
    try {
      const conferencia = await this.service.loadInboundConference(this.nfe.id);
      this.aplicar(conferencia);
      this.carregamento.set('ready');
    } catch {
      this.carregamento.set('error');
    }
  }

  /** Atualiza fornecedor e produtos das linhas sem perder as quantidades que o operador já digitou. */
  private aplicar(conferencia: InboundConference): void {
    const anteriores = new Map(this.linhas().map(line => [line.item.index, line]));
    this.conferencia.set(conferencia);
    this.linhas.set(conferencia.items.map(item => {
      const anterior = anteriores.get(item.index);
      return anterior ? { ...anterior, item, pick: item.resolved ? '' : anterior.pick } : lineOf(item);
    }));
  }

  cnpj(value: string): string {
    return formatCnpj(value);
  }

  /** O fornecedor já foi cadastrado pelo backend: fecha o formulário e relê a conferência (os itens passam a poder ser vinculados). */
  async aoCadastrarFornecedor(): Promise<void> {
    this.cadastrandoFornecedor.set(false);
    await this.carregar();
  }

  houveVariacao(item: InboundConferenceItem): boolean {
    return item.priceDelta !== null && item.priceDelta !== 0 && item.lastPurchaseUnitValue !== null;
  }

  valor(value: number): string {
    return formatUnitValue(value);
  }

  variacao(delta: number): string {
    return formatDelta(delta);
  }

  divergente(line: LineDraft): boolean {
    return line.orderedQty !== null && line.receivedQty !== null && line.orderedQty !== line.receivedQty;
  }

  alterar(index: number, patch: Partial<Omit<LineDraft, 'item'>>): void {
    this.erro.set('');
    this.linhas.update(list => list.map(line => line.item.index === index ? { ...line, ...patch } : line));
  }

  /** POST …/link-product: o produto escolhido na busca passa a valer para este código do fornecedor. */
  async vincular(line: LineDraft): Promise<void> {
    if (!line.pick || this.vinculando()) return;
    this.vinculando.set(true);
    this.registrandoErro.set('');
    try {
      await this.service.linkInboundItemToProduct(this.nfe.id, line.item.index, line.pick);
      await this.carregar();
    } catch (error) {
      this.registrandoErro.set((await nfeFailureDetail(error)) || 'Não foi possível vincular o produto.');
    } finally {
      this.vinculando.set(false);
    }
  }

  /** O produto novo já foi cadastrado e vinculado pelo backend: fecha o formulário e relê os vínculos. */
  async aoResolver(): Promise<void> {
    this.cadastrando.set(null);
    await this.carregar();
  }

  /** POST /api/nfe/inbound/{id}/confirm-receipt; só fecha depois do sucesso, mantendo o modal aberto nas falhas. */
  async confirmar(): Promise<void> {
    this.tentouEnviar.set(true);
    if (this.enviando()) return;
    const impedimento = this.impedimento();
    if (impedimento) return this.erro.set(impedimento);
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
  return { itemRef: line.item.productId!, orderedQty: line.orderedQty ?? 0, receivedQty: line.receivedQty ?? 0 };
}
