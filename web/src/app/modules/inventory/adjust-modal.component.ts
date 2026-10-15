import { Component, EventEmitter, Output, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LoadStatus } from '../../core/services/customer.service';
import { InventoryChangesService } from '../../core/services/inventory-changes.service';
import { InventoryApiService, ProductBalance, inventoryFailureDetail } from '../../core/services/inventory-api.service';
import { ProductSearchResult, ProductSearchService } from '../../core/services/product-search.service';
import { ToastService } from '../../core/services/toast.service';
import { ModalComponent } from '../../shared/components/modal/modal.component';

type Sentido = 'entrada' | 'saida';

@Component({
  selector: 'app-adjust-modal',
  standalone: true,
  imports: [FormsModule, ModalComponent],
  template: `
    <app-modal title="Ajuste manual de estoque" (closed)="closed.emit()">
      <div class="impact-note">
        <i class="ti ti-info-circle"></i>
        <span>O ajuste gera um lançamento contábil automático e fica registrado no histórico de movimentações.</span>
      </div>
      <div class="form-row">
        <div>
          <label class="form-label" for="adj-busca">Produto</label>
          <div class="search-row">
            <input id="adj-busca" class="form-input" type="text" placeholder="Código, código de barras ou descrição"
                   [ngModel]="termo()" (ngModelChange)="termo.set($event)" (keydown.enter)="buscar()" />
            <button class="btn btn-ghost" type="button" [disabled]="buscaStatus() === 'loading'" (click)="buscar()">Buscar</button>
          </div>
          @if (buscaStatus() === 'loading') { <div class="field-hint">Buscando…</div> }
          @if (buscaStatus() === 'error') { <div class="field-error" role="alert">Não foi possível buscar os produtos.</div> }
          @if (buscaStatus() === 'ready' && !resultados().length) { <div class="field-hint">Nenhum produto encontrado.</div> }
          @if (resultados().length) {
            <select class="form-select mt-8" aria-label="Produto encontrado" [ngModel]="produtoId()" (ngModelChange)="escolher($event)">
              <option value="">Selecione o produto…</option>
              @for (p of resultados(); track p.productId) { <option [value]="p.productId">{{ p.description }}</option> }
            </select>
          }
          @if (tentouSalvar() && !produtoId()) { <div class="field-error">Escolha o produto a ajustar.</div> }
        </div>
      </div>
      <div class="form-row cols-2">
        <div>
          <label class="form-label" for="adj-sentido">Tipo de ajuste</label>
          <select id="adj-sentido" class="form-select" [ngModel]="sentido()" (ngModelChange)="sentido.set($event)">
            <option value="entrada">Positivo (acrescenta)</option>
            <option value="saida">Negativo (retira)</option>
          </select>
        </div>
        <div>
          <label class="form-label" for="adj-qtd">Quantidade</label>
          <input id="adj-qtd" class="form-input" type="number" min="0" step="any"
                 [class.invalid]="tentouSalvar() && !variacao()"
                 [ngModel]="quantidade()" (ngModelChange)="quantidade.set($event)" />
          @if (tentouSalvar() && !variacao()) { <div class="field-error">Informe uma quantidade maior que zero.</div> }
          @if (produtoId()) {
            @switch (saldoStatus()) {
              @case ('loading') { <div class="field-hint">Carregando saldo…</div> }
              @case ('error') {
                <div class="field-error" role="alert">
                  Não foi possível carregar o saldo.
                  <button class="btn btn-ghost btn-sm" type="button" (click)="carregarSaldo()">Tentar novamente</button>
                </div>
              }
              @default { <div class="field-hint">Saldo atual: {{ saldoAtual() }} · depois: {{ saldoDepois() }}</div> }
            }
          }
        </div>
      </div>
      <div class="form-row">
        <div>
          <label class="form-label" for="adj-just">Justificativa (obrigatória)</label>
          <textarea id="adj-just" class="form-textarea" rows="3" placeholder="Ex.: avaria, quebra, erro de lançamento…"
                    [class.invalid]="tentouSalvar() && !justificativa().trim()"
                    [ngModel]="justificativa()" (ngModelChange)="justificativa.set($event)"></textarea>
          @if (tentouSalvar() && !justificativa().trim()) {
            <div class="field-error">Informe o motivo do ajuste.</div>
          }
        </div>
      </div>
      <div footer>
        <button class="btn btn-ghost" type="button" (click)="closed.emit()">Cancelar</button>
        <button class="btn btn-primary" type="button" [disabled]="enviando()" (click)="salvar()">
          {{ enviando() ? 'Registrando…' : 'Registrar ajuste' }}
        </button>
      </div>
    </app-modal>
  `,
  styles: [`
    .search-row { display: flex; gap: 8px; }
    .search-row .form-input { flex: 1; }
  `]
})
export class AdjustModalComponent {
  private readonly api = inject(InventoryApiService);
  private readonly search = inject(ProductSearchService);
  private readonly changes = inject(InventoryChangesService);
  private readonly toast = inject(ToastService);
  private buscas = 0;
  private saldos = 0;

  @Output() closed = new EventEmitter<void>();

  readonly termo = signal('');
  readonly resultados = signal<ProductSearchResult[]>([]);
  readonly buscaStatus = signal<LoadStatus | 'idle'>('idle');
  readonly produtoId = signal('');
  readonly saldo = signal<ProductBalance | null>(null);
  readonly saldoStatus = signal<LoadStatus>('loading');
  readonly sentido = signal<Sentido>('entrada');
  readonly quantidade = signal<number | null>(null);
  readonly justificativa = signal('');
  readonly tentouSalvar = signal(false);
  readonly enviando = signal(false);

  readonly variacao = computed(() => {
    const quantidade = Math.abs(Number(this.quantidade()) || 0);
    return this.sentido() === 'entrada' ? quantidade : -quantidade;
  });
  readonly saldoAtual = computed(() => this.saldo()?.onHand ?? 0);
  readonly saldoDepois = computed(() => this.saldoAtual() + this.variacao());

  /** GET /api/pdv/products/search — nunca rejeita: o resultado fica em `buscaStatus`. */
  async buscar(): Promise<void> {
    const termo = this.termo().trim();
    if (!termo) return;
    const busca = ++this.buscas;
    this.buscaStatus.set('loading');
    try {
      const encontrados = await this.search.search(termo);
      if (busca !== this.buscas) return;
      this.resultados.set(encontrados);
      this.buscaStatus.set('ready');
      this.escolher(encontrados.length === 1 ? encontrados[0].productId : '');
    } catch {
      if (busca === this.buscas) this.buscaStatus.set('error');
    }
  }

  escolher(produtoId: string): void {
    this.produtoId.set(produtoId);
    void this.carregarSaldo();
  }

  /** GET /api/inventory/products/{id}/balance — nunca rejeita: o resultado fica em `saldoStatus`. */
  async carregarSaldo(): Promise<void> {
    const produtoId = this.produtoId();
    const carga = ++this.saldos;
    this.saldo.set(null);
    if (!produtoId) return;
    this.saldoStatus.set('loading');
    try {
      const saldo = await this.api.productBalance(produtoId);
      if (carga !== this.saldos) return;
      this.saldo.set(saldo);
      this.saldoStatus.set('ready');
    } catch {
      if (carga === this.saldos) this.saldoStatus.set('error');
    }
  }

  /** POST /api/inventory/adjustments; só fecha e avisa as outras telas depois da resposta de sucesso. */
  async salvar(): Promise<void> {
    this.tentouSalvar.set(true);
    if (!this.produtoId() || !this.variacao() || !this.justificativa().trim() || this.enviando()) return;
    this.enviando.set(true);
    try {
      await this.api.adjust({
        productId: this.produtoId(), quantityDelta: this.variacao(), justification: this.justificativa(),
      });
    } catch (error) {
      this.enviando.set(false);
      return this.toast.danger(inventoryFailureDetail(error) || 'Não foi possível registrar o ajuste.');
    }
    this.toast.success('Ajuste registrado e lançamento contábil gerado');
    this.changes.stockChanged();
    this.closed.emit();
  }
}
