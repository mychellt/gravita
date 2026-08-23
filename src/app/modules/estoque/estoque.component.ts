import { Component, computed, signal } from '@angular/core';
import { NgClass, DatePipe } from '@angular/common';
import { DataService } from '../../core/services/data.service';
import { ToastService } from '../../core/services/toast.service';
import { BadgeComponent } from '../../shared/components/badge/badge.component';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { BrlPipe } from '../../shared/pipes/brl.pipe';

@Component({
  selector: 'app-estoque',
  standalone: true,
  imports: [NgClass, DatePipe, BadgeComponent, PageHeaderComponent, BrlPipe],
  styleUrl: './estoque.component.scss',
  templateUrl: './estoque.component.html'
})
export class EstoqueComponent {
  constructor(public data: DataService, private toast: ToastService) {}

  filterTipo = signal<string>('');
  searchQuery = signal('');

  readonly movimentos = computed(() => {
    const q = this.searchQuery().toLowerCase();
    const t = this.filterTipo();
    return this.data.movimentos().filter(m =>
      (!t || m.tipo === t) &&
      (!q || m.produtoNome.toLowerCase().includes(q))
    );
  });

  readonly criticalProdutos = computed(() => this.data.produtosCriticos());
  readonly totalSKUs = computed(() => this.data.produtos().filter(p => p.status === 'ativo').length);

  solicitarCompra(nome: string) {
    this.toast.success(`Solicitação de compra criada para "${nome}"`);
  }
}
