import { Component, computed, signal } from '@angular/core';
import { DataService } from '../../core/services/data.service';
import { ToastService } from '../../core/services/toast.service';
import { Produto, ItemCarrinho } from '../../core/models';
import { BrlPipe } from '../../shared/pipes/brl.pipe';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-pdv',
  standalone: true,
  imports: [BrlPipe, FormsModule],
  styleUrl: './pdv.component.scss',
  templateUrl: './pdv.component.html'
})
export class PdvComponent {
  constructor(public data: DataService, private toast: ToastService) {}

  search = signal('');
  cart = signal<ItemCarrinho[]>([]);
  cpfNota = signal('');
  showPayModal = signal(false);
  selectedMethod = signal<'dinheiro' | 'cartao' | 'pix'>('dinheiro');
  valorRecebido = signal(0);

  readonly cartTotal = computed(() =>
    this.cart().reduce((s, i) => s + i.subtotal, 0)
  );

  readonly troco = computed(() =>
    Math.max(0, this.valorRecebido() - this.cartTotal())
  );

  readonly filteredProdutos = computed(() => {
    const q = this.search().toLowerCase();
    return this.data.produtos().filter(p =>
      p.status === 'ativo' && (
        !q ||
        p.descricao.toLowerCase().includes(q) ||
        p.codigo.includes(q) ||
        (p.codigoBarras ?? '').includes(q)
      )
    );
  });

  addToCart(p: Produto) {
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

  confirmSale() {
    this.showPayModal.set(false);
    this.clearCart();
    this.cpfNota.set('');
    this.toast.success('✓ NFC-e emitida e autorizada com sucesso!');
  }
}
