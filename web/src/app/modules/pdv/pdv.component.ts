import { Component, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { PaymentMethodType, PdvSaleResult, PdvSalesService } from '../../core/services/pdv-sales.service';
import { InventoryApiService } from '../../core/services/inventory-api.service';
import { ProductSearchService } from '../../core/services/product-search.service';
import { ToastService } from '../../core/services/toast.service';
import type { LoadStatus } from '../../core/services/customer.service';
import { productIconEmoji } from '../../core/models/product-icon';
import { BrlPipe } from '../../shared/pipes/brl.pipe';

/** Produto à venda no PDV: catálogo (/api/pdv/products/search) com o saldo disponível (/api/inventory/balances). */
export interface PdvProduto {
  id: string;
  descricao: string;
  ncm: string | null;
  precoVenda: number;
  /** Saldo disponível; `null` quando o produto ainda não tem saldo no depósito. */
  estoque: number | null;
  /** Emoji do produto. */
  icone: string;
}

export interface ItemCarrinho {
  produto: PdvProduto;
  quantidade: number;
  desconto: number;
  subtotal: number;
}

const BALANCE_PAGE_SIZE = 200;
const SEARCH_DEBOUNCE_MS = 300;
const CPF_LENGTH = 11;
const PAYMENT_METHOD: Record<'dinheiro' | 'cartao' | 'pix', PaymentMethodType> = { dinheiro: 'CASH', cartao: 'CREDIT_CARD', pix: 'PIX' };

@Component({
  selector: 'app-pdv',
  standalone: true,
  imports: [BrlPipe, FormsModule],
  styleUrl: './pdv.component.scss',
  templateUrl: './pdv.component.html'
})
export class PdvComponent {
  private readonly productSearch = inject(ProductSearchService);
  private readonly inventory = inject(InventoryApiService);
  private readonly toast = inject(ToastService);
  private readonly sales = inject(PdvSalesService);

  search = signal('');
  produtos = signal<PdvProduto[]>([]);
  produtosStatus = signal<LoadStatus>('loading');
  cart = signal<ItemCarrinho[]>([]);
  cpfNota = signal('');
  showPayModal = signal(false);
  selectedMethod = signal<'dinheiro' | 'cartao' | 'pix'>('dinheiro');
  valorRecebido = signal(0);
  /** Venda sendo gravada, para não confirmar duas vezes. */
  confirmando = signal(false);

  private searchTimer: ReturnType<typeof setTimeout> | undefined;
  /** Descarta a resposta de uma busca que já foi superada por outra mais nova. */
  private searchSeq = 0;
  private stockByProduct = new Map<string, number>();

  constructor() {
    void this.loadStock().then(() => this.loadProdutos());
  }

  readonly cartTotal = computed(() =>
    this.cart().reduce((s, i) => s + i.subtotal, 0)
  );

  readonly troco = computed(() =>
    Math.max(0, this.valorRecebido() - this.cartTotal())
  );

  /** Digitar na busca consulta o servidor (código de barras, nome ou código interno) depois de uma breve pausa. */
  onSearch(value: string) {
    this.search.set(value);
    clearTimeout(this.searchTimer);
    this.searchTimer = setTimeout(() => void this.loadProdutos(), SEARCH_DEBOUNCE_MS);
  }

  reload() {
    void this.loadStock().then(() => this.loadProdutos());
  }

  /** Termo vazio devolve todos os produtos ativos. */
  async loadProdutos(): Promise<void> {
    const seq = ++this.searchSeq;
    this.produtosStatus.set('loading');
    try {
      const results = await this.productSearch.search(this.search().trim());
      if (seq !== this.searchSeq) return;
      this.produtos.set(results
        .filter(r => r.availableForSale)
        .map(r => ({
          id: r.productId, descricao: r.description, ncm: r.ncm, precoVenda: r.unitPrice,
          estoque: this.stockByProduct.get(r.productId) ?? null,
          icone: productIconEmoji(r.icon),
        })));
      this.produtosStatus.set('ready');
    } catch {
      if (seq === this.searchSeq) this.produtosStatus.set('error');
    }
  }

  /** Saldo disponível por produto, de todas as páginas; sem saldo, o card mostra o estoque como indisponível. */
  private async loadStock(): Promise<void> {
    try {
      const stock = new Map<string, number>();
      for (let page = 0, pages = 1; page < pages; page++) {
        const result = await this.inventory.balances(page, BALANCE_PAGE_SIZE);
        result.content.forEach(b => stock.set(b.productId, (stock.get(b.productId) ?? 0) + b.available));
        pages = result.totalPages;
      }
      this.stockByProduct = stock;
    } catch {
      this.stockByProduct = new Map();
    }
  }

  addToCart(p: PdvProduto) {
    this.cart.update(items => {
      const existing = items.find(i => i.produto.id === p.id);
      if (existing) {
        return items.map(i => i.produto.id === p.id
          ? { ...i, quantidade: i.quantidade + 1, subtotal: (i.quantidade + 1) * i.produto.precoVenda }
          : i
        );
      }
      return [...items, { produto: p, quantidade: 1, desconto: 0, subtotal: p.precoVenda }];
    });
  }

  changeQty(id: string, delta: number) {
    this.cart.update(items => items
      .map(i => {
        if (i.produto.id !== id) return i;
        const q = i.quantidade + delta;
        return q <= 0 ? null : { ...i, quantidade: q, subtotal: q * i.produto.precoVenda };
      })
      .filter((i): i is ItemCarrinho => i !== null)
    );
  }

  removeItem(id: string) {
    this.cart.update(items => items.filter(i => i.produto.id !== id));
  }

  clearCart() { this.cart.set([]); }

  openPayment() {
    if (this.cart().length === 0) {
      this.toast.warn('Adicione produtos ao carrinho primeiro.');
      return;
    }
    this.valorRecebido.set(this.cartTotal());
    this.showPayModal.set(true);
  }

  selectMethod(m: 'dinheiro' | 'cartao' | 'pix') { this.selectedMethod.set(m); }

  /** Grava a venda no backend e emite a NFC-e; só limpa o carrinho depois que a venda foi gravada. */
  async confirmSale() {
    if (this.confirmando()) return;
    const method = PAYMENT_METHOD[this.selectedMethod()];
    const total = this.cartTotal();
    const received = method === 'CASH' ? this.valorRecebido() : total;
    if (received < total) {
      this.toast.warn('O valor recebido não cobre o total da venda.');
      return;
    }
    const cpf = this.cpfNota().replace(/\D/g, '');
    if (cpf && cpf.length !== CPF_LENGTH) {
      this.toast.warn('CPF na nota inválido: informe os 11 dígitos ou deixe em branco.');
      return;
    }

    this.confirmando.set(true);
    try {
      const result = await this.sales.register({
        items: this.cart().map(i => ({
          productId: i.produto.id, quantity: i.quantidade, unitPrice: i.produto.precoVenda,
          ...(i.desconto > 0 ? { itemDiscount: i.desconto } : {}),
        })),
        payments: [{ method, amount: received }],
        ...(cpf ? { customerCpf: cpf } : {}),
      });
      this.showPayModal.set(false);
      this.clearCart();
      this.cpfNota.set('');
      this.announce(result);
      void this.loadStock().then(() => this.loadProdutos());
    } catch (error) {
      const detail = error instanceof HttpErrorResponse ? error.error?.message ?? error.error?.detail : null;
      this.toast.danger(detail ? `Venda não registrada: ${detail}` : 'Não foi possível registrar a venda. Tente novamente.');
    } finally {
      this.confirmando.set(false);
    }
  }

  private announce(result: PdvSaleResult) {
    if (result.status === 'AUTHORIZED') {
      this.toast.success('✓ Venda registrada e NFC-e autorizada!');
    } else if (result.status === 'PENDING_SYNC') {
      this.toast.warn('Venda registrada. A SEFAZ está indisponível: a NFC-e foi emitida em contingência e será transmitida depois.');
    } else {
      this.toast.warn(`Venda registrada, mas a NFC-e não foi emitida${result.issuanceError ? `: ${result.issuanceError}` : '.'}`);
    }
  }
}
