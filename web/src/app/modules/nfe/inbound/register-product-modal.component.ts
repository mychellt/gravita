import { Component, EventEmitter, Input, OnInit, Output, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { InboundConferenceItem, NfeService, RegisterInboundProductRequest, nfeFailureDetail } from '../../../core/services/nfe.service';
import { ToastService } from '../../../core/services/toast.service';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { formatUnitValue } from './inbound-format';

const SALE_OPERATION = 'VENDA';
const CFOP_PATTERN = /^\d{4}$/;
const CST_CSOSN_PATTERN = /^\d{2,3}$/;
const STATE_PATTERN = /^[A-Za-z]{2}$/;

/**
 * Cadastra um produto novo a partir de um item sem vínculo da NF-e de entrada (UC-M1-11). Só descrição, NCM e unidade vêm da
 * nota; todo dado fiscal de saída e de preço é preenchido pelo operador. Ao salvar, o backend também cria o vínculo
 * fornecedor × produto, já com o valor unitário deste item.
 */
@Component({
  selector: 'app-register-product-modal',
  standalone: true,
  imports: [FormsModule, ModalComponent],
  template: `
    <app-modal title="Cadastrar produto a partir do item da NF-e" [width]="640" (closed)="closed.emit()">
      <div class="impact-note">
        <i class="ti ti-info-circle"></i>
        <span>
          Descrição, NCM e unidade vieram da nota (código do fornecedor {{ item.supplierProductCode }}, {{ valorNota }} por unidade).
          Informe o preço de venda. Os dados fiscais de saída não vêm da nota: o CFOP e o CST do fornecedor são de saída dele e
          não servem para a sua venda.
        </span>
      </div>
      <div class="form-row">
        <div>
          <label class="form-label" for="prd-codigo">Descrição / código interno</label>
          <input id="prd-codigo" class="form-input" type="text" maxlength="60" [class.invalid]="tentouEnviar() && !codigo().trim()"
                 [ngModel]="codigo()" (ngModelChange)="codigo.set($event)" />
          @if (tentouEnviar() && !codigo().trim()) { <div class="field-error">Informe a descrição do produto.</div> }
        </div>
      </div>
      <div class="form-row cols-3">
        <div>
          <label class="form-label" for="prd-ncm">NCM</label>
          <input id="prd-ncm" class="form-input" type="text" inputmode="numeric" [ngModel]="ncm()" (ngModelChange)="ncm.set($event)" />
        </div>
        <div>
          <label class="form-label" for="prd-un-compra">Unidade de compra</label>
          <input id="prd-un-compra" class="form-input" type="text" maxlength="10" [ngModel]="unidadeCompra()" (ngModelChange)="unidadeCompra.set($event)" />
        </div>
        <div>
          <label class="form-label" for="prd-un-venda">Unidade de venda</label>
          <input id="prd-un-venda" class="form-input" type="text" maxlength="10" [class.invalid]="tentouEnviar() && !unidadeVenda().trim()"
                 [ngModel]="unidadeVenda()" (ngModelChange)="unidadeVenda.set($event)" />
          @if (tentouEnviar() && !unidadeVenda().trim()) { <div class="field-error">Informe a unidade de venda.</div> }
        </div>
      </div>
      <div class="impact-note">
        <i class="ti ti-receipt-tax"></i>
        <span>
          Dados fiscais de saída (origem, CFOP, CST/CSOSN e alíquotas) são opcionais. Só são obrigatórios para emitir nota fiscal
          de venda; sem eles o produto só serve para venda gerencial. Defina-os com sua contabilidade, conforme seu regime tributário.
        </span>
      </div>
      <div class="form-row cols-3">
        <div>
          <label class="form-label" for="prd-origem">Origem da mercadoria (0–8, opcional)</label>
          <input id="prd-origem" class="form-input" type="number" min="0" max="8" step="1" [class.invalid]="tentouEnviar() && !origemValida()"
                 [ngModel]="origem()" (ngModelChange)="origem.set($event)" />
          @if (tentouEnviar() && !origemValida()) { <div class="field-error">A origem vai de 0 a 8.</div> }
        </div>
        <div>
          <label class="form-label" for="prd-cfop">CFOP de venda (opcional)</label>
          <input id="prd-cfop" class="form-input" type="text" inputmode="numeric" maxlength="4" [class.invalid]="tentouEnviar() && !cfopValido()"
                 [ngModel]="cfopVenda()" (ngModelChange)="cfopVenda.set($event)" />
          @if (tentouEnviar() && !cfopValido()) { <div class="field-error">O CFOP tem 4 dígitos.</div> }
        </div>
      </div>
      <div class="form-row cols-2">
        <div>
          <label class="form-label" for="prd-margem">Margem sobre o custo (%)</label>
          <input id="prd-margem" class="form-input" type="number" min="0" step="any"
                 [ngModel]="margem()" (ngModelChange)="aoMudarMargem($event)" />
          <div class="form-hint">Custo da nota: {{ valorNota }}</div>
        </div>
        <div>
          <label class="form-label" for="prd-preco">Preço de venda (R$)</label>
          <input id="prd-preco" class="form-input" type="number" min="0" step="0.01" [class.invalid]="tentouEnviar() && !precoValido()"
                 [ngModel]="precoVenda()" (ngModelChange)="precoVenda.set($event)" />
          @if (tentouEnviar() && !precoValido()) { <div class="field-error">Informe o preço de venda.</div> }
        </div>
      </div>
      <div class="form-row cols-2">
        <div>
          <label class="form-label" for="prd-uf">UF do CST/CSOSN (opcional)</label>
          <input id="prd-uf" class="form-input" type="text" maxlength="2" [class.invalid]="tentouEnviar() && !ufValida()"
                 [ngModel]="uf()" (ngModelChange)="uf.set($event)" />
          @if (tentouEnviar() && !ufValida()) { <div class="field-error">Informe a UF com 2 letras para o CST/CSOSN.</div> }
        </div>
        <div>
          <label class="form-label" for="prd-cst">CST/CSOSN (opcional)</label>
          <input id="prd-cst" class="form-input" type="text" inputmode="numeric" maxlength="3" [class.invalid]="tentouEnviar() && !cstValido()"
                 [ngModel]="cstCsosn()" (ngModelChange)="cstCsosn.set($event)" />
          @if (tentouEnviar() && !cstValido()) { <div class="field-error">O CST/CSOSN tem 2 ou 3 dígitos; informe também a UF.</div> }
        </div>
      </div>
      <div class="form-row cols-3">
        <div>
          <label class="form-label" for="prd-icms">ICMS (%)</label>
          <input id="prd-icms" class="form-input" type="number" min="0" step="any" [ngModel]="icms()" (ngModelChange)="icms.set($event)" />
        </div>
        <div>
          <label class="form-label" for="prd-pis">PIS (%)</label>
          <input id="prd-pis" class="form-input" type="number" min="0" step="any" [ngModel]="pis()" (ngModelChange)="pis.set($event)" />
        </div>
        <div>
          <label class="form-label" for="prd-cofins">COFINS (%)</label>
          <input id="prd-cofins" class="form-input" type="number" min="0" step="any" [ngModel]="cofins()" (ngModelChange)="cofins.set($event)" />
        </div>
      </div>
      @if (erro()) { <div class="field-error" role="alert">{{ erro() }}</div> }
      <div footer>
        <button class="btn btn-ghost" type="button" (click)="closed.emit()">Cancelar</button>
        <button class="btn btn-primary" type="button" [disabled]="enviando()" (click)="salvar()">
          {{ enviando() ? 'Salvando…' : 'Cadastrar e vincular' }}
        </button>
      </div>
    </app-modal>
  `
})
export class RegisterProductModalComponent implements OnInit {
  private readonly service = inject(NfeService);
  private readonly toast = inject(ToastService);

  @Input({ required: true }) inboundNfeId!: string;
  @Input({ required: true }) item!: InboundConferenceItem;
  /** Emite depois que o produto e o vínculo foram criados. */
  @Output() registered = new EventEmitter<void>();
  @Output() closed = new EventEmitter<void>();

  readonly codigo = signal('');
  readonly ncm = signal('');
  readonly unidadeCompra = signal('');
  readonly unidadeVenda = signal('');
  readonly origem = signal<number | null>(null);
  readonly cfopVenda = signal('');
  readonly precoVenda = signal<number | null>(null);
  readonly margem = signal<number | null>(null);
  readonly uf = signal('');
  readonly cstCsosn = signal('');
  readonly icms = signal<number | null>(null);
  readonly pis = signal<number | null>(null);
  readonly cofins = signal<number | null>(null);

  readonly tentouEnviar = signal(false);
  readonly enviando = signal(false);
  readonly erro = signal('');

  readonly origemValida = computed(() => this.origem() === null || (Number.isInteger(this.origem()) && this.origem()! >= 0 && this.origem()! <= 8));
  readonly cfopValido = computed(() => !this.cfopVenda().trim() || CFOP_PATTERN.test(this.cfopVenda().trim()));
  readonly precoValido = computed(() => this.precoVenda() !== null && this.precoVenda()! > 0);
  readonly ufValida = computed(() => (!this.uf().trim() && !this.cstCsosn().trim()) || STATE_PATTERN.test(this.uf().trim()));
  readonly cstValido = computed(() => (!this.uf().trim() && !this.cstCsosn().trim()) || CST_CSOSN_PATTERN.test(this.cstCsosn().trim()));
  private readonly valido = computed(() =>
    !!this.codigo().trim() && !!this.unidadeVenda().trim() && this.origemValida() && this.cfopValido()
    && this.precoValido() && this.ufValida() && this.cstValido());

  /** Sugere o preço de venda a partir do custo da nota e da margem; o operador ainda pode editar o preço. */
  aoMudarMargem(valor: number | null): void {
    this.margem.set(valor);
    const custo = Number(this.item.unitValue);
    if (valor === null || valor === undefined || !Number.isFinite(custo) || custo <= 0) return;
    this.precoVenda.set(Math.round(custo * (1 + valor / 100) * 100) / 100);
  }

  get valorNota(): string {
    return formatUnitValue(this.item.unitValue);
  }

  ngOnInit(): void {
    this.codigo.set(this.item.description.slice(0, 60));
    this.ncm.set(this.item.ncm ?? '');
    this.unidadeCompra.set(this.item.unit ?? '');
    this.unidadeVenda.set(this.item.unit ?? '');
  }

  /** POST …/items/{index}/register-product; só fecha depois do sucesso, mantendo o modal aberto nas falhas. */
  async salvar(): Promise<void> {
    this.tentouEnviar.set(true);
    if (!this.valido() || this.enviando()) return;
    this.enviando.set(true);
    this.erro.set('');
    try {
      await this.service.registerInboundItemProduct(this.inboundNfeId, this.item.index, this.toRequest());
      this.toast.success('Produto cadastrado e vinculado ao código do fornecedor');
      this.registered.emit();
    } catch (error) {
      this.erro.set((await nfeFailureDetail(error)) || 'Não foi possível cadastrar o produto.');
      this.enviando.set(false);
    }
  }

  private toRequest(): RegisterInboundProductRequest {
    const cfop = this.cfopVenda().trim();
    const cst = this.cstCsosn().trim();
    const rates = {
      icmsRate: this.icms() ?? undefined, pisRate: this.pis() ?? undefined, cofinsRate: this.cofins() ?? undefined,
    };
    return {
      internalCode: this.codigo().trim(),
      type: 'SIMPLE',
      ncm: this.ncm().trim(),
      ...(this.origem() !== null ? { origin: this.origem()! } : {}),
      ...(cfop ? { defaultCfopByOperation: { [SALE_OPERATION]: cfop } } : {}),
      ...(cst ? { cstCsosnByState: { [this.uf().trim().toUpperCase()]: cst } } : {}),
      ...(Object.values(rates).some(rate => rate !== undefined) ? { taxProfile: rates } : {}),
      basePrice: this.precoVenda()!,
      purchaseUnit: this.unidadeCompra().trim(),
      saleUnit: this.unidadeVenda().trim(),
    };
  }
}
