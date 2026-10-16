import { Component, EventEmitter, Input, OnInit, Output, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { InboundNfeConferenceItem, NfeService, RegisteredProduct, nfeFailureDetail } from '../../../core/services/nfe.service';
import { ToastService } from '../../../core/services/toast.service';
import { ModalComponent } from '../../../shared/components/modal/modal.component';

/**
 * Cadastra um produto novo a partir de um item sem vínculo (GRA-246 BR4) e já cria o vínculo fornecedor/produto.
 * Reaproveita os campos de UC-M1-11 (RegisterProductUseCase); identificação vem pré-preenchida do item da NF-e,
 * mas os campos fiscais de saída e de preço continuam exigidos do operador, como no cadastro direto de produto.
 */
@Component({
  selector: 'app-register-product-from-item-modal',
  standalone: true,
  imports: [FormsModule, ModalComponent],
  template: `
    <app-modal title="Cadastrar novo produto" [width]="560" (closed)="closed.emit()">
      <div class="impact-note">
        <i class="ti ti-info-circle"></i>
        <span>Dados pré-preenchidos a partir do item da nota. Complete os dados fiscais de saída e de preço antes de salvar.</span>
      </div>

      <div class="form-section">
        <div class="form-section-head"><i class="ti ti-package form-section-icon"></i><span class="form-section-title">Identificação</span></div>
        <div style="padding:12px 16px">
          <div class="form-row">
            <div>
              <label class="form-label" for="rpi-codigo">Código / descrição interna</label>
              <input id="rpi-codigo" class="form-input" type="text" [class.invalid]="tentouEnviar() && !internalCode().trim()"
                     [ngModel]="internalCode()" (ngModelChange)="internalCode.set($event)" />
              @if (tentouEnviar() && !internalCode().trim()) { <div class="field-error">Informe o código do produto.</div> }
            </div>
          </div>
          <div class="form-row cols-3">
            <div>
              <label class="form-label" for="rpi-ncm">NCM</label>
              <input id="rpi-ncm" class="form-input" type="text" [class.invalid]="tentouEnviar() && !ncm().trim()"
                     [ngModel]="ncm()" (ngModelChange)="ncm.set($event)" />
            </div>
            <div>
              <label class="form-label" for="rpi-compra">Unidade de compra</label>
              <input id="rpi-compra" class="form-input" type="text" [ngModel]="purchaseUnit()" (ngModelChange)="purchaseUnit.set($event)" />
            </div>
            <div>
              <label class="form-label" for="rpi-venda">Unidade de venda</label>
              <input id="rpi-venda" class="form-input" type="text" [class.invalid]="tentouEnviar() && !saleUnit().trim()"
                     [ngModel]="saleUnit()" (ngModelChange)="saleUnit.set($event)" />
            </div>
          </div>
          <div class="form-row cols-2">
            <div>
              <label class="form-label" for="rpi-fator">Fator de conversão (compra → venda)</label>
              <input id="rpi-fator" class="form-input" type="number" min="0" step="any"
                     [ngModel]="conversionFactor()" (ngModelChange)="conversionFactor.set($event)" />
            </div>
            <div>
              <label class="form-label" for="rpi-origem">Origem</label>
              <select id="rpi-origem" class="form-select" [ngModel]="origin()" (ngModelChange)="origin.set($event)">
                @for (o of origens; track o) { <option [value]="o">{{ o }} — {{ origemLabel(o) }}</option> }
              </select>
            </div>
          </div>
        </div>
      </div>

      <div class="form-section">
        <div class="form-section-head"><i class="ti ti-receipt-tax form-section-icon"></i><span class="form-section-title">Dados fiscais de saída</span></div>
        <div style="padding:12px 16px">
          <div class="form-row cols-3">
            <div>
              <label class="form-label" for="rpi-cfop">CFOP de venda</label>
              <input id="rpi-cfop" class="form-input mono" type="text" placeholder="5102" [class.invalid]="tentouEnviar() && !cfopVenda().trim()"
                     [ngModel]="cfopVenda()" (ngModelChange)="cfopVenda.set($event)" />
            </div>
            <div>
              <label class="form-label" for="rpi-uf">UF de destino</label>
              <input id="rpi-uf" class="form-input mono" type="text" maxlength="2" placeholder="SP" [class.invalid]="tentouEnviar() && !ufCst().trim()"
                     [ngModel]="ufCst()" (ngModelChange)="ufCst.set($event.toUpperCase())" />
            </div>
            <div>
              <label class="form-label" for="rpi-cst">CST/CSOSN</label>
              <input id="rpi-cst" class="form-input mono" type="text" placeholder="000" [class.invalid]="tentouEnviar() && !cstCsosn().trim()"
                     [ngModel]="cstCsosn()" (ngModelChange)="cstCsosn.set($event)" />
            </div>
          </div>
          @if (tentouEnviar() && (!cfopVenda().trim() || !ufCst().trim() || !cstCsosn().trim())) {
            <div class="field-error">Informe o CFOP de venda, a UF e o CST/CSOSN.</div>
          }
          <div class="form-row cols-3 mt-8">
            <div>
              <label class="form-label" for="rpi-icms">ICMS %</label>
              <input id="rpi-icms" class="form-input" type="number" min="0" step="any" [class.invalid]="tentouEnviar() && icmsRate() === null"
                     [ngModel]="icmsRate()" (ngModelChange)="icmsRate.set($event)" />
            </div>
            <div>
              <label class="form-label" for="rpi-ipi">IPI %</label>
              <input id="rpi-ipi" class="form-input" type="number" min="0" step="any" [ngModel]="ipiRate()" (ngModelChange)="ipiRate.set($event)" />
            </div>
            <div>
              <label class="form-label" for="rpi-pis">PIS %</label>
              <input id="rpi-pis" class="form-input" type="number" min="0" step="any" [ngModel]="pisRate()" (ngModelChange)="pisRate.set($event)" />
            </div>
          </div>
          <div class="form-row cols-3 mt-8">
            <div>
              <label class="form-label" for="rpi-cofins">COFINS %</label>
              <input id="rpi-cofins" class="form-input" type="number" min="0" step="any" [ngModel]="cofinsRate()" (ngModelChange)="cofinsRate.set($event)" />
            </div>
            <div>
              <label class="form-label" for="rpi-icmsst">ICMS-ST %</label>
              <input id="rpi-icmsst" class="form-input" type="number" min="0" step="any" [ngModel]="icmsStRate()" (ngModelChange)="icmsStRate.set($event)" />
            </div>
            <div>
              <label class="form-label" for="rpi-fcp">FCP %</label>
              <input id="rpi-fcp" class="form-input" type="number" min="0" step="any" [ngModel]="fcpRate()" (ngModelChange)="fcpRate.set($event)" />
            </div>
          </div>
        </div>
      </div>

      <div class="form-section">
        <div class="form-section-head"><i class="ti ti-currency-dollar form-section-icon"></i><span class="form-section-title">Preços</span></div>
        <div style="padding:12px 16px">
          <div class="form-row cols-2">
            <div>
              <label class="form-label" for="rpi-custo">Custo médio</label>
              <input id="rpi-custo" class="form-input" type="number" min="0" step="any"
                     [ngModel]="averageCost()" (ngModelChange)="averageCost.set($event)" />
              <div class="field-hint">Pré-preenchido com o valor unitário desta nota.</div>
            </div>
            <div>
              <label class="form-label" for="rpi-preco">Preço de venda</label>
              <input id="rpi-preco" class="form-input" type="number" min="0" step="any" [class.invalid]="tentouEnviar() && basePrice() === null"
                     [ngModel]="basePrice()" (ngModelChange)="basePrice.set($event)" />
              @if (tentouEnviar() && basePrice() === null) { <div class="field-error">Informe o preço de venda.</div> }
            </div>
          </div>
        </div>
      </div>

      @if (erro()) { <div class="field-error" role="alert">{{ erro() }}</div> }
      <div footer>
        <button class="btn btn-ghost" type="button" (click)="closed.emit()">Cancelar</button>
        <button class="btn btn-primary" type="button" [disabled]="salvando()" (click)="salvar()">
          {{ salvando() ? 'Salvando…' : 'Cadastrar e vincular' }}
        </button>
      </div>
    </app-modal>
  `,
  styles: [`.mt-8 { margin-top: 8px; }`]
})
export class RegisterProductFromItemModalComponent implements OnInit {
  private readonly nfe = inject(NfeService);
  private readonly toast = inject(ToastService);

  @Input({ required: true }) item!: InboundNfeConferenceItem;
  @Input({ required: true }) inboundNfeId!: string;
  @Input({ required: true }) itemIndex!: number;
  @Output() linked = new EventEmitter<RegisteredProduct>();
  @Output() closed = new EventEmitter<void>();

  readonly origens = [0, 1, 2, 3, 4, 5, 6, 7, 8];

  internalCode = signal('');
  ncm = signal('');
  purchaseUnit = signal('');
  saleUnit = signal('');
  conversionFactor = signal(1);
  origin = signal(0);
  cfopVenda = signal('');
  ufCst = signal('');
  cstCsosn = signal('');
  icmsRate = signal<number | null>(null);
  ipiRate = signal<number | null>(0);
  pisRate = signal<number | null>(0);
  cofinsRate = signal<number | null>(0);
  icmsStRate = signal<number | null>(0);
  fcpRate = signal<number | null>(0);
  averageCost = signal<number | null>(null);
  basePrice = signal<number | null>(null);

  readonly tentouEnviar = signal(false);
  readonly salvando = signal(false);
  readonly erro = signal('');

  ngOnInit(): void {
    this.internalCode.set(this.item.description);
    this.ncm.set(this.item.ncm ?? '');
    this.purchaseUnit.set(this.item.unit ?? '');
    this.saleUnit.set(this.item.unit ?? '');
    this.averageCost.set(this.item.unitValue);
  }

  origemLabel(code: number): string {
    return ['Nacional', 'Estrangeira (importação direta)', 'Estrangeira (mercado interno)', 'Nacional (conteúdo import. 40-70%)',
      'Nacional (processos produtivos básicos)', 'Nacional (conteúdo import. ≤40%)', 'Estrangeira (sem similar nacional)',
      'Estrangeira (mercado interno, sem similar)', 'Nacional (conteúdo import. >70%)'][code] ?? '';
  }

  private valido(): boolean {
    return !!this.internalCode().trim() && !!this.ncm().trim() && !!this.saleUnit().trim() && !!this.cfopVenda().trim()
      && !!this.ufCst().trim() && !!this.cstCsosn().trim() && this.icmsRate() !== null && this.basePrice() !== null;
  }

  async salvar(): Promise<void> {
    this.tentouEnviar.set(true);
    if (!this.valido() || this.salvando()) return;
    this.salvando.set(true);
    this.erro.set('');
    try {
      const produto = await this.nfe.linkNewProduct(this.inboundNfeId, this.itemIndex, {
        internalCode: this.internalCode().trim(),
        type: 'SIMPLE',
        ncm: this.ncm().trim(),
        origin: this.origin(),
        defaultCfopByOperation: { VENDA: this.cfopVenda().trim() },
        cstCsosnByState: { [this.ufCst().trim()]: this.cstCsosn().trim() },
        taxProfile: {
          icmsRate: this.icmsRate() ?? 0,
          ipiRate: this.ipiRate() ?? 0,
          pisRate: this.pisRate() ?? 0,
          cofinsRate: this.cofinsRate() ?? 0,
          icmsStRate: this.icmsStRate() ?? 0,
          fcpRate: this.fcpRate() ?? 0,
        },
        averageCost: this.averageCost() ?? this.item.unitValue,
        basePrice: this.basePrice() ?? 0,
        purchaseUnit: this.purchaseUnit().trim(),
        saleUnit: this.saleUnit().trim(),
        conversionFactor: this.conversionFactor(),
      });
      this.toast.success(`Produto ${produto.internalCode} cadastrado e vinculado ao item.`);
      this.linked.emit(produto);
    } catch (error) {
      this.erro.set((await nfeFailureDetail(error)) || 'Não foi possível cadastrar o produto.');
      this.salvando.set(false);
    }
  }
}
