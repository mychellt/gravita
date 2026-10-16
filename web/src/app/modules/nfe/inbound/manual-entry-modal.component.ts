import { Component, EventEmitter, Output, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { EnterInboundNfeRequest, ManualInboundItem, NfeService, nfeFailureDetail } from '../../../core/services/nfe.service';
import { ToastService } from '../../../core/services/toast.service';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { BrlPipe } from '../../../shared/pipes/brl.pipe';
import { ACCESS_KEY_PATTERN, onlyDigits } from './inbound-format';

interface ItemDraft {
  supplierProductCode: string;
  description: string;
  ncm: string;
  cfop: string;
  unit: string;
  quantity: number | null;
  unitValue: number | null;
}

const emptyItem = (): ItemDraft => ({
  supplierProductCode: '', description: '', ncm: '', cfop: '1102', unit: 'UN', quantity: null, unitValue: null,
});

const itemTotal = (item: ItemDraft) => (item.quantity ?? 0) * (item.unitValue ?? 0);
const itemValid = (item: ItemDraft) =>
  !!item.description.trim() && (item.quantity ?? 0) > 0 && (item.unitValue ?? 0) > 0;

@Component({
  selector: 'app-manual-entry-modal',
  standalone: true,
  imports: [FormsModule, ModalComponent, BrlPipe],
  template: `
    <app-modal title="Lançar NF-e de entrada manualmente" [width]="820" (closed)="closed.emit()">
      <div class="form-row">
        <div>
          <label class="form-label" for="man-chave">Chave de acesso (44 dígitos)</label>
          <input id="man-chave" class="form-input td-mono" type="text" inputmode="numeric" maxlength="60"
                 [class.invalid]="tentouEnviar() && !chaveValida()"
                 [ngModel]="chave()" (ngModelChange)="chave.set($event)" />
          @if (tentouEnviar() && !chaveValida()) { <div class="field-error">A chave de acesso deve ter 44 dígitos.</div> }
        </div>
      </div>
      <div class="form-row cols-3">
        <div>
          <label class="form-label" for="man-serie">Série</label>
          <input id="man-serie" class="form-input" type="text" [class.invalid]="tentouEnviar() && !serie().trim()"
                 [ngModel]="serie()" (ngModelChange)="serie.set($event)" />
        </div>
        <div>
          <label class="form-label" for="man-numero">Número</label>
          <input id="man-numero" class="form-input" type="text" [class.invalid]="tentouEnviar() && !numero().trim()"
                 [ngModel]="numero()" (ngModelChange)="numero.set($event)" />
        </div>
        <div>
          <label class="form-label" for="man-emissao">Emissão</label>
          <input id="man-emissao" class="form-input" type="datetime-local" [class.invalid]="tentouEnviar() && !emissao()"
                 [ngModel]="emissao()" (ngModelChange)="emissao.set($event)" />
        </div>
      </div>
      <div class="form-row cols-2">
        <div>
          <label class="form-label" for="man-cnpj">CNPJ do fornecedor</label>
          <input id="man-cnpj" class="form-input" type="text" inputmode="numeric" [class.invalid]="tentouEnviar() && !cnpjValido()"
                 [ngModel]="cnpj()" (ngModelChange)="cnpj.set($event)" />
          @if (tentouEnviar() && !cnpjValido()) { <div class="field-error">O CNPJ deve ter 14 dígitos.</div> }
        </div>
        <div>
          <label class="form-label" for="man-fornecedor">Fornecedor</label>
          <input id="man-fornecedor" class="form-input" type="text" [class.invalid]="tentouEnviar() && !fornecedor().trim()"
                 [ngModel]="fornecedor()" (ngModelChange)="fornecedor.set($event)" />
        </div>
      </div>

      <div class="form-label">Itens</div>
      @for (item of itens(); track $index) {
        <div class="form-row item-row" [attr.aria-label]="'Item ' + ($index + 1)">
          <input class="form-input" type="text" placeholder="Cód. fornecedor" [ngModel]="item.supplierProductCode"
                 (ngModelChange)="alterar($index, { supplierProductCode: $event })" />
          <input class="form-input" type="text" placeholder="Descrição" [class.invalid]="tentouEnviar() && !item.description.trim()"
                 [ngModel]="item.description" (ngModelChange)="alterar($index, { description: $event })" />
          <input class="form-input" type="text" placeholder="NCM" [ngModel]="item.ncm"
                 (ngModelChange)="alterar($index, { ncm: $event })" />
          <input class="form-input" type="text" placeholder="CFOP" [ngModel]="item.cfop"
                 (ngModelChange)="alterar($index, { cfop: $event })" />
          <input class="form-input" type="text" placeholder="Un." [ngModel]="item.unit"
                 (ngModelChange)="alterar($index, { unit: $event })" />
          <input class="form-input" type="number" min="0" step="any" placeholder="Qtd."
                 [class.invalid]="tentouEnviar() && !((item.quantity ?? 0) > 0)"
                 [ngModel]="item.quantity" (ngModelChange)="alterar($index, { quantity: $event })" />
          <input class="form-input" type="number" min="0" step="any" placeholder="Valor unit."
                 [class.invalid]="tentouEnviar() && !((item.unitValue ?? 0) > 0)"
                 [ngModel]="item.unitValue" (ngModelChange)="alterar($index, { unitValue: $event })" />
          <button class="btn btn-ghost btn-sm" type="button" aria-label="Remover item"
                  [disabled]="itens().length === 1" (click)="remover($index)"><i class="ti ti-trash"></i></button>
        </div>
      }
      @if (tentouEnviar() && !itensValidos()) {
        <div class="field-error">Cada item precisa de descrição, quantidade e valor unitário maiores que zero.</div>
      }
      <button class="btn btn-ghost btn-sm" type="button" (click)="adicionar()"><i class="ti ti-plus"></i> Adicionar item</button>

      <div class="form-row cols-3 mt-8">
        <div>
          <label class="form-label" for="man-frete">Frete</label>
          <input id="man-frete" class="form-input" type="number" min="0" step="any"
                 [ngModel]="frete()" (ngModelChange)="frete.set($event)" />
        </div>
        <div>
          <label class="form-label" for="man-desconto">Desconto</label>
          <input id="man-desconto" class="form-input" type="number" min="0" step="any"
                 [ngModel]="desconto()" (ngModelChange)="desconto.set($event)" />
        </div>
        <div>
          <div class="form-label">Total da nota</div>
          <div class="total-value">{{ totalNota() | brl }}</div>
        </div>
      </div>
      @if (erro()) { <div class="field-error" role="alert">{{ erro() }}</div> }
      <div footer>
        <button class="btn btn-ghost" type="button" (click)="closed.emit()">Cancelar</button>
        <button class="btn btn-primary" type="button" [disabled]="enviando()" (click)="enviar()">
          {{ enviando() ? 'Lançando…' : 'Lançar NF-e' }}
        </button>
      </div>
    </app-modal>
  `,
  styles: [`
    .item-row { grid-template-columns: 1fr 2fr 1fr 0.8fr 0.6fr 0.8fr 1fr auto; align-items: center; }
    .total-value { font-weight: 600; padding-top: 8px; }
  `]
})
export class ManualEntryModalComponent {
  private readonly nfe = inject(NfeService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);

  @Output() closed = new EventEmitter<void>();

  readonly chave = signal('');
  readonly serie = signal('1');
  readonly numero = signal('');
  readonly emissao = signal('');
  readonly cnpj = signal('');
  readonly fornecedor = signal('');
  readonly itens = signal<ItemDraft[]>([emptyItem()]);
  readonly frete = signal<number | null>(null);
  readonly desconto = signal<number | null>(null);
  readonly tentouEnviar = signal(false);
  readonly enviando = signal(false);
  readonly erro = signal('');

  readonly totalProdutos = computed(() => this.itens().reduce((total, item) => total + itemTotal(item), 0));
  readonly totalNota = computed(() => this.totalProdutos() + (this.frete() ?? 0) - (this.desconto() ?? 0));
  readonly itensValidos = computed(() => this.itens().every(itemValid));

  chaveValida(): boolean {
    return ACCESS_KEY_PATTERN.test(onlyDigits(this.chave()));
  }

  cnpjValido(): boolean {
    return onlyDigits(this.cnpj()).length === 14;
  }

  adicionar(): void {
    this.itens.update(list => [...list, emptyItem()]);
  }

  remover(index: number): void {
    this.itens.update(list => list.filter((_, i) => i !== index));
  }

  alterar(index: number, patch: Partial<ItemDraft>): void {
    this.itens.update(list => list.map((item, i) => i === index ? { ...item, ...patch } : item));
  }

  /** POST /api/nfe/inbound; só fecha depois da resposta de sucesso, mantendo o modal aberto nas falhas. */
  async enviar(): Promise<void> {
    this.tentouEnviar.set(true);
    const companyId = this.auth.currentUser()?.companyId;
    if (!this.formularioValido() || this.enviando()) return;
    if (!companyId) return this.erro.set('Usuário sem empresa vinculada.');
    this.enviando.set(true);
    this.erro.set('');
    try {
      const nota = await this.nfe.enterInboundManually(this.montarRequisicao(companyId));
      if (nota.supplierResolved) {
        this.toast.success('NF-e de entrada lançada — aguardando conferência');
      } else {
        this.toast.warn('NF-e de entrada lançada, mas o fornecedor ainda não está cadastrado — cadastre-o antes de conferir.');
      }
      this.closed.emit();
    } catch (error) {
      this.erro.set((await nfeFailureDetail(error)) || 'Não foi possível lançar a NF-e.');
      this.enviando.set(false);
    }
  }

  private formularioValido(): boolean {
    return this.chaveValida() && this.cnpjValido() && this.itensValidos() && !!this.emissao()
      && !!this.serie().trim() && !!this.numero().trim() && !!this.fornecedor().trim();
  }

  private montarRequisicao(companyId: string): EnterInboundNfeRequest {
    const accessKey = onlyDigits(this.chave());
    return {
      companyId,
      accessKey,
      manualData: {
        accessKey,
        series: this.serie().trim(),
        number: this.numero().trim(),
        supplierCnpj: onlyDigits(this.cnpj()),
        supplierName: this.fornecedor().trim(),
        issuedAt: new Date(this.emissao()).toISOString(),
        items: this.itens().map(toRequestItem),
        totals: {
          productsValue: this.totalProdutos(), freightValue: this.frete() ?? 0, insuranceValue: 0,
          discountValue: this.desconto() ?? 0, otherExpensesValue: 0, icmsValue: 0, ipiValue: 0,
          pisValue: 0, cofinsValue: 0, totalValue: this.totalNota(),
        },
      },
    };
  }
}

function toRequestItem(item: ItemDraft): ManualInboundItem {
  return {
    supplierProductCode: item.supplierProductCode.trim(), description: item.description.trim(), ncm: item.ncm.trim(),
    cfop: item.cfop.trim(), unit: item.unit.trim(), quantity: item.quantity ?? 0, unitValue: item.unitValue ?? 0,
    totalValue: itemTotal(item), icmsValue: 0, ipiValue: 0, pisValue: 0, cofinsValue: 0,
  };
}
