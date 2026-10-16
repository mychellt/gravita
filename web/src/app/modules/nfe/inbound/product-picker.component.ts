import { Component, EventEmitter, Input, Output, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import type { LoadStatus } from '../../../core/services/customer.service';
import { ProductSearchResult, ProductSearchService } from '../../../core/services/product-search.service';

/** Busca um produto do catálogo (GET /api/pdv/products/search) e emite o `productId` escolhido (`''` ao limpar). */
@Component({
  selector: 'app-product-picker',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="picker-row">
      <input class="form-input" type="text" placeholder="Código, cód. de barras ou descrição"
             [attr.aria-label]="label" [class.invalid]="invalid"
             [ngModel]="termo()" (ngModelChange)="termo.set($event)" (keydown.enter)="buscar()" />
      <button class="btn btn-ghost btn-sm" type="button" [disabled]="status() === 'loading'" (click)="buscar()">Buscar</button>
    </div>
    @if (status() === 'error') { <div class="field-error" role="alert">Não foi possível buscar os produtos.</div> }
    @if (status() === 'ready' && !resultados().length) { <div class="field-hint">Nenhum produto encontrado.</div> }
    @if (resultados().length) {
      <select class="form-select mt-8" [attr.aria-label]="label + ' encontrado'" [ngModel]="escolhido()" (ngModelChange)="escolher($event)">
        <option value="">Selecione o produto…</option>
        @for (p of resultados(); track p.productId) { <option [value]="p.productId">{{ p.description }}</option> }
      </select>
    }
  `,
  styles: [`
    .picker-row { display: flex; gap: 8px; }
    .picker-row .form-input { flex: 1; min-width: 0; }
  `]
})
export class ProductPickerComponent {
  private readonly search = inject(ProductSearchService);
  private buscas = 0;

  @Input() label = 'Produto';
  @Input() invalid = false;
  /** Emite o produto escolhido (com descrição, para exibição) ou `null` ao limpar a seleção. */
  @Output() picked = new EventEmitter<ProductSearchResult | null>();

  readonly termo = signal('');
  readonly resultados = signal<ProductSearchResult[]>([]);
  readonly status = signal<LoadStatus | 'idle'>('idle');
  readonly escolhido = signal('');

  async buscar(): Promise<void> {
    const termo = this.termo().trim();
    if (!termo) return;
    const busca = ++this.buscas;
    this.status.set('loading');
    try {
      const encontrados = await this.search.search(termo);
      if (busca !== this.buscas) return;
      this.resultados.set(encontrados);
      this.status.set('ready');
      this.escolher(encontrados.length === 1 ? encontrados[0].productId : '');
    } catch {
      if (busca === this.buscas) this.status.set('error');
    }
  }

  escolher(productId: string): void {
    this.escolhido.set(productId);
    this.picked.emit(this.resultados().find(p => p.productId === productId) ?? null);
  }
}
