import { Component, computed, inject, signal } from '@angular/core';
import { DataService } from '../../core/services/data.service';
import { InventoryService } from '../../core/services/inventory.service';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { BrlPipe } from '../../shared/pipes/brl.pipe';
import { AdjustModalComponent } from './adjust-modal.component';
import { AlertsPanelComponent } from './alerts-panel.component';
import { BalancesTabComponent } from './balances-tab.component';
import { CountsTabComponent } from './counts-tab.component';
import { LotsTabComponent } from './lots-tab.component';
import { MovementsTabComponent } from './movements-tab.component';
import { ReorderTabComponent } from './reorder-tab.component';
import { ReservationsTabComponent } from './reservations-tab.component';
import { TransfersTabComponent } from './transfers-tab.component';

type AbaEstoque = 'movimentos' | 'saldos' | 'lotes' | 'transferencias' | 'reservas' | 'inventario' | 'reposicao';

@Component({
  selector: 'app-inventory',
  standalone: true,
  imports: [
    PageHeaderComponent, BrlPipe, AdjustModalComponent, AlertsPanelComponent, BalancesTabComponent,
    CountsTabComponent, LotsTabComponent, MovementsTabComponent, ReorderTabComponent,
    ReservationsTabComponent, TransfersTabComponent
  ],
  styleUrl: './inventory.component.scss',
  templateUrl: './inventory.component.html'
})
export class InventoryComponent {
  private readonly data = inject(DataService);
  readonly inventory = inject(InventoryService);

  readonly aba = signal<AbaEstoque>('movimentos');
  readonly ajusteAberto = signal(false);

  readonly totalSKUs = computed(() => this.data.produtos().filter(p => p.status === 'ativo').length);
  readonly abaixoDoMinimo = computed(() => this.data.produtosCriticos().length);
  readonly abas = computed<{ id: AbaEstoque; rotulo: string; contador?: number }[]>(() => [
    { id: 'movimentos', rotulo: 'Movimentações' },
    { id: 'saldos', rotulo: 'Saldos' },
    { id: 'lotes', rotulo: 'Lotes e séries' },
    { id: 'transferencias', rotulo: 'Transferências', contador: this.inventory.transferenciasPendentes().length },
    { id: 'reservas', rotulo: 'Reservas' },
    { id: 'inventario', rotulo: 'Inventário' },
    { id: 'reposicao', rotulo: 'Reposição', contador: this.inventory.sugestoesReposicao().length },
  ]);

  abrirInventario(): void {
    this.aba.set('inventario');
  }
}
