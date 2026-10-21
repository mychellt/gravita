import { Component, OnInit, computed, effect, inject, signal, untracked } from '@angular/core';
import { InventorySummaryService } from '../../core/services/inventory-summary.service';
import { InventoryAlertsService } from '../../core/services/inventory-alerts.service';
import { InventoryChangesService } from '../../core/services/inventory-changes.service';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { AdjustModalComponent } from './adjust-modal.component';
import { AlertsPanelComponent } from './alerts-panel.component';
import { BalancesTabComponent } from './balances-tab.component';
import { CountsTabComponent } from './counts-tab.component';
import { LotsTabComponent } from './lots-tab.component';
import { MovementsTabComponent } from './movements-tab.component';
import { ReorderTabComponent } from './reorder-tab.component';
import { ReservationsTabComponent } from './reservations-tab.component';

type AbaEstoque = 'movimentos' | 'saldos' | 'lotes' | 'reservas' | 'inventario' | 'reposicao';

@Component({
  selector: 'app-inventory',
  standalone: true,
  imports: [
    PageHeaderComponent, AdjustModalComponent, AlertsPanelComponent, BalancesTabComponent,
    CountsTabComponent, LotsTabComponent, MovementsTabComponent, ReorderTabComponent,
    ReservationsTabComponent
  ],
  styleUrl: './inventory.component.scss',
  templateUrl: './inventory.component.html'
})
export class InventoryComponent implements OnInit {
  private readonly summaryService = inject(InventorySummaryService);
  private readonly changes = inject(InventoryChangesService);
  readonly alertas = inject(InventoryAlertsService);

  readonly aba = signal<AbaEstoque>('movimentos');
  readonly ajusteAberto = signal(false);

  /** `null` enquanto carrega ou se falhar: o cabeçalho mostra "—" em vez de um número inventado. */
  readonly totalSKUs = computed(() => this.summaryService.summary()?.activeSkus ?? null);
  readonly abaixoDoMinimo = computed(() => this.summaryService.summary()?.belowMinimum ?? null);
  readonly abas = computed<{ id: AbaEstoque; rotulo: string; contador?: number }[]>(() => [
    { id: 'movimentos', rotulo: 'Movimentações' },
    { id: 'saldos', rotulo: 'Saldos' },
    { id: 'lotes', rotulo: 'Lotes e séries' },
    { id: 'reservas', rotulo: 'Reservas' },
    { id: 'inventario', rotulo: 'Inventário' },
    { id: 'reposicao', rotulo: 'Reposição', contador: this.alertas.lowStock().length },
  ]);

  constructor() {
    // Ajuste, contagem aprovada ou reserva liberada: os contadores e o painel de alertas vêm do servidor de novo.
    effect(() => {
      if (this.changes.version()) untracked(() => this.reload());
    });
  }

  /** Alertas de validade e de estoque baixo alimentam o painel, os contadores e as abas Lotes e Reposição. */
  ngOnInit(): void {
    this.reload();
  }

  private reload(): void {
    void this.alertas.loadAll();
    void this.summaryService.load();
  }

  abrirInventario(): void {
    this.aba.set('inventario');
  }
}
