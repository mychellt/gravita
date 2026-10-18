import { Component, ElementRef, EventEmitter, OnDestroy, Output, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { EnterInboundNfeRequest, ManualInboundItem, NfeService, nfeFailureDetail } from '../../../core/services/nfe.service';
import { ProductSearchResult, ProductSearchService } from '../../../core/services/product-search.service';
import { SupplierService } from '../../../core/services/supplier.service';
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
    <app-modal title="Lançar NF-e de entrada manualmente" [width]="980" (closed)="closed.emit()">
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
                 [ngModel]="cnpj()" (ngModelChange)="aoMudarCnpj($event)" />
          @if (tentouEnviar() && !cnpjValido()) { <div class="field-error">O CNPJ deve ter 14 dígitos.</div> }
          @switch (fornecedorStatus()) {
            @case ('buscando') { <div class="field-hint">Buscando fornecedor…</div> }
            @case ('cadastrado') { <div class="field-hint supplier-ok"><i class="ti ti-circle-check"></i> Fornecedor cadastrado — dados preenchidos.</div> }
            @case ('novo') { <div class="field-hint">Fornecedor ainda não cadastrado; você poderá cadastrá-lo na conferência.</div> }
          }
        </div>
        <div>
          <label class="form-label" for="man-fornecedor">Fornecedor</label>
          <input id="man-fornecedor" class="form-input" type="text" [class.invalid]="tentouEnviar() && !fornecedor().trim()"
                 [ngModel]="fornecedor()" (ngModelChange)="fornecedor.set($event)" />
        </div>
      </div>

      <div class="items-head">
        <span class="form-label">Itens ({{ itens().length }})</span>
        <button class="btn btn-ghost btn-sm" type="button" (click)="detalhesFiscais.set(!detalhesFiscais())">
          <i class="ti" [class.ti-chevron-down]="!detalhesFiscais()" [class.ti-chevron-up]="detalhesFiscais()"></i>
          {{ detalhesFiscais() ? 'Ocultar NCM e CFOP' : 'Mostrar NCM e CFOP' }}
        </button>
      </div>
      <div class="items-grid" [class.with-fiscal]="detalhesFiscais()">
        <div class="items-row items-header" aria-hidden="true">
          <span>Cód. fornecedor</span><span>Descrição / produto</span>
          @if (detalhesFiscais()) { <span>NCM</span><span>CFOP</span> }
          <span>Un.</span><span class="num">Qtd.</span><span class="num">Valor unit.</span><span class="num">Total</span><span></span>
        </div>
        @for (item of itens(); track i; let i = $index) {
          <div class="items-row" [attr.aria-label]="'Item ' + (i + 1)">
            <input class="form-input" type="text" placeholder="Código" [attr.data-cell]="i + ':0'" [attr.aria-label]="'Código do fornecedor do item ' + (i + 1)"
                   [ngModel]="item.supplierProductCode" (ngModelChange)="alterar(i, { supplierProductCode: $event })" />
            <div class="desc-cell">
              <input class="form-input" type="text" placeholder="Digite para buscar no catálogo ou descreva o item" autocomplete="off"
                     [attr.data-cell]="i + ':1'" [attr.aria-label]="'Descrição do item ' + (i + 1)"
                     [class.invalid]="tentouEnviar() && !item.description.trim()"
                     [ngModel]="item.description" (ngModelChange)="aoDigitarDescricao(i, $event)"
                     (focus)="linhaAtiva.set(i)" (blur)="fecharSugestoes(i)" (keydown)="aoTeclarDescricao($event, i)" />
              @if (linhaAtiva() === i && sugestoes().length) {
                <ul class="suggestions" role="listbox">
                  @for (p of sugestoes(); track p.productId; let s = $index) {
                    <li role="option" [class.active]="s === sugestaoAtiva()" [attr.aria-selected]="s === sugestaoAtiva()"
                        (mousedown)="$event.preventDefault(); escolherSugestao(i, p)">
                      {{ p.description }}@if (p.ncm) { <span class="sug-ncm">NCM {{ p.ncm }}</span> }
                    </li>
                  }
                </ul>
              }
            </div>
            @if (detalhesFiscais()) {
              <input class="form-input" type="text" placeholder="NCM" [attr.aria-label]="'NCM do item ' + (i + 1)"
                     [ngModel]="item.ncm" (ngModelChange)="alterar(i, { ncm: $event })" />
              <input class="form-input" type="text" placeholder="CFOP" [attr.aria-label]="'CFOP do item ' + (i + 1)"
                     [ngModel]="item.cfop" (ngModelChange)="alterar(i, { cfop: $event })" />
            }
            <input class="form-input" type="text" placeholder="Un." [attr.aria-label]="'Unidade do item ' + (i + 1)"
                   [ngModel]="item.unit" (ngModelChange)="alterar(i, { unit: $event })" />
            <input class="form-input num" type="number" min="0" step="any" placeholder="0" [attr.data-cell]="i + ':2'"
                   [attr.aria-label]="'Quantidade do item ' + (i + 1)" [class.invalid]="tentouEnviar() && !((item.quantity ?? 0) > 0)"
                   [ngModel]="item.quantity" (ngModelChange)="alterar(i, { quantity: $event })" />
            <input class="form-input num" type="number" min="0" step="any" placeholder="0,00" [attr.data-cell]="i + ':3'"
                   [attr.aria-label]="'Valor unitário do item ' + (i + 1)" [class.invalid]="tentouEnviar() && !((item.unitValue ?? 0) > 0)"
                   [ngModel]="item.unitValue" (ngModelChange)="alterar(i, { unitValue: $event })"
                   (keydown.enter)="aoEnterNoValor($event, i)" />
            <span class="line-total num">{{ totalDoItem(item) | brl }}</span>
            <button class="btn btn-ghost btn-sm" type="button" [attr.aria-label]="'Remover item ' + (i + 1)"
                    [disabled]="itens().length === 1" (click)="remover(i)"><i class="ti ti-trash"></i></button>
          </div>
        }
      </div>
      @if (tentouEnviar() && !itensValidos()) {
        <div class="field-error">Cada item precisa de descrição, quantidade e valor unitário maiores que zero.</div>
      }
      <div class="items-actions">
        <button class="btn btn-ghost btn-sm" type="button" (click)="adicionar(true)"><i class="ti ti-plus"></i> Adicionar item</button>
        <span class="field-hint">Dica: Enter no valor unitário adiciona uma nova linha.</span>
      </div>

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
          <div class="field-hint">{{ itens().length }} {{ itens().length === 1 ? 'item' : 'itens' }} · produtos {{ totalProdutos() | brl }}</div>
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
    .items-head { display: flex; align-items: center; justify-content: space-between; margin-top: 4px; }
    .items-grid { border: 1px solid var(--border); border-radius: var(--r8, 8px); overflow: visible; margin-bottom: 8px; }
    .items-row { display: grid; grid-template-columns: 1fr 3fr 0.7fr 0.9fr 1.1fr 1.1fr auto; gap: 6px; align-items: center; padding: 6px 8px; border-top: 1px solid var(--border); }
    .items-grid.with-fiscal .items-row { grid-template-columns: 1fr 3fr 1fr 0.8fr 0.7fr 0.9fr 1.1fr 1.1fr auto; }
    .items-row.items-header { border-top: none; background: var(--surface2); font-size: 11px; font-weight: 600; color: var(--text3); text-transform: uppercase; border-radius: 8px 8px 0 0; }
    .num { text-align: right; }
    .line-total { font-weight: 600; font-size: 13px; }
    .desc-cell { position: relative; }
    .suggestions { position: absolute; z-index: 20; left: 0; right: 0; top: 100%; margin: 2px 0 0; padding: 4px 0; list-style: none; background: var(--surface); border: 1px solid var(--border); border-radius: var(--r8, 8px); box-shadow: 0 8px 24px rgba(0,0,0,.15); max-height: 220px; overflow-y: auto; }
    .suggestions li { padding: 7px 12px; font-size: 13px; cursor: pointer; display: flex; justify-content: space-between; gap: 12px; }
    .suggestions li.active, .suggestions li:hover { background: var(--surface2); }
    .sug-ncm { color: var(--text3); font-size: 11px; white-space: nowrap; }
    .items-actions { display: flex; align-items: center; gap: 12px; }
    .supplier-ok { color: var(--success); }
    .total-value { font-weight: 600; padding-top: 8px; }
  `]
})
export class ManualEntryModalComponent implements OnDestroy {
  private readonly nfe = inject(NfeService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly suppliers = inject(SupplierService);
  private readonly productSearch = inject(ProductSearchService);
  private readonly host = inject(ElementRef<HTMLElement>);
  private buscaFornecedor = 0;
  private buscaProduto = 0;
  private temporizador: ReturnType<typeof setTimeout> | undefined;

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
  /** Situação da consulta do CNPJ digitado: fornecedor cadastrado, ainda não cadastrado, ou sem consulta. */
  readonly fornecedorStatus = signal<'nenhum' | 'buscando' | 'cadastrado' | 'novo'>('nenhum');
  readonly detalhesFiscais = signal(false);
  /** Linha cuja descrição está em edição e as sugestões do catálogo que ela mostra. */
  readonly linhaAtiva = signal<number | null>(null);
  readonly sugestoes = signal<ProductSearchResult[]>([]);
  readonly sugestaoAtiva = signal(0);

  readonly totalProdutos = computed(() => this.itens().reduce((total, item) => total + itemTotal(item), 0));
  readonly totalNota = computed(() => this.totalProdutos() + (this.frete() ?? 0) - (this.desconto() ?? 0));
  readonly itensValidos = computed(() => this.itens().every(itemValid));

  chaveValida(): boolean {
    return ACCESS_KEY_PATTERN.test(onlyDigits(this.chave()));
  }

  cnpjValido(): boolean {
    return onlyDigits(this.cnpj()).length === 14;
  }

  totalDoItem = itemTotal;

  ngOnDestroy(): void {
    clearTimeout(this.temporizador);
  }

  /** Ao completar 14 dígitos, preenche o nome do fornecedor cadastrado (ou, não cadastrado, o da consulta de CNPJ). */
  aoMudarCnpj(valor: string): void {
    this.cnpj.set(valor);
    const busca = ++this.buscaFornecedor;
    if (onlyDigits(valor).length !== 14) {
      this.fornecedorStatus.set('nenhum');
      return;
    }
    this.fornecedorStatus.set('buscando');
    void this.preencherFornecedor(onlyDigits(valor), busca);
  }

  private async preencherFornecedor(cnpj: string, busca: number): Promise<void> {
    const cadastrado = await this.suppliers.findByDocument(cnpj);
    if (busca !== this.buscaFornecedor) return;
    if (cadastrado) {
      this.fornecedor.set(cadastrado.name);
      this.fornecedorStatus.set('cadastrado');
      return;
    }
    const consulta = await this.suppliers.lookupCnpj(cnpj);
    if (busca !== this.buscaFornecedor) return;
    if (consulta?.name && !this.fornecedor().trim()) this.fornecedor.set(consulta.name);
    this.fornecedorStatus.set('novo');
  }

  /** Adiciona uma linha; com `focar`, leva o cursor para o código dela. */
  adicionar(focar = false): void {
    this.itens.update(list => [...list, emptyItem()]);
    if (focar) this.focarCelula(this.itens().length - 1, 0);
  }

  /** Enter no valor unitário da última linha abre a próxima (entrada contínua só pelo teclado). */
  aoEnterNoValor(evento: Event, index: number): void {
    evento.preventDefault();
    if (index === this.itens().length - 1) this.adicionar(true);
    else this.focarCelula(index + 1, 0);
  }

  private focarCelula(linha: number, coluna: number): void {
    setTimeout(() => (this.host.nativeElement as HTMLElement).querySelector<HTMLInputElement>(`[data-cell="${linha}:${coluna}"]`)?.focus());
  }

  /** Digitar na descrição também busca no catálogo (a partir de 2 letras), com espera para não consultar a cada tecla. */
  aoDigitarDescricao(index: number, valor: string): void {
    this.alterar(index, { description: valor });
    clearTimeout(this.temporizador);
    const termo = valor.trim();
    const busca = ++this.buscaProduto;
    if (termo.length < 2) {
      this.sugestoes.set([]);
      return;
    }
    this.temporizador = setTimeout(async () => {
      try {
        const encontrados = await this.productSearch.search(termo);
        if (busca !== this.buscaProduto) return;
        this.sugestoes.set(encontrados.slice(0, 6));
        this.sugestaoAtiva.set(0);
      } catch {
        if (busca === this.buscaProduto) this.sugestoes.set([]);
      }
    }, 250);
  }

  aoTeclarDescricao(evento: KeyboardEvent, index: number): void {
    const lista = this.sugestoes();
    if (!lista.length || this.linhaAtiva() !== index) return;
    if (evento.key === 'ArrowDown') {
      evento.preventDefault();
      this.sugestaoAtiva.update(s => Math.min(s + 1, lista.length - 1));
    } else if (evento.key === 'ArrowUp') {
      evento.preventDefault();
      this.sugestaoAtiva.update(s => Math.max(s - 1, 0));
    } else if (evento.key === 'Enter') {
      evento.preventDefault();
      this.escolherSugestao(index, lista[this.sugestaoAtiva()]);
    } else if (evento.key === 'Escape') {
      evento.stopPropagation();
      this.sugestoes.set([]);
    }
  }

  /** Usa o produto do catálogo na linha: descrição e NCM vêm dele; o cursor segue para a quantidade. */
  escolherSugestao(index: number, produto: ProductSearchResult): void {
    this.alterar(index, { description: produto.description, ...(produto.ncm ? { ncm: produto.ncm } : {}) });
    this.sugestoes.set([]);
    this.focarCelula(index, 2);
  }

  fecharSugestoes(index: number): void {
    if (this.linhaAtiva() === index) this.sugestoes.set([]);
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
      await this.nfe.enterInboundManually(this.montarRequisicao(companyId));
      this.toast.success('NF-e de entrada lançada — aguardando conferência');
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
