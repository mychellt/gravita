import { Component, computed, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { DataService } from '../../core/services/data.service';
import { ToastService } from '../../core/services/toast.service';
import { BadgeComponent } from '../../shared/components/badge/badge.component';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { BrlPipe } from '../../shared/pipes/brl.pipe';

@Component({
  selector: 'app-financeiro',
  standalone: true,
  imports: [DatePipe, BadgeComponent, PageHeaderComponent, BrlPipe],
  styleUrl: './financeiro.component.scss',
  templateUrl: './financeiro.component.html'
})
export class FinanceiroComponent {
  constructor(public data: DataService, private toast: ToastService) {}

  activeTab = signal<'receber' | 'pagar' | 'fluxo' | 'conciliacao'>('receber');

  readonly receber = computed(() => this.data.titulos().filter(t => t.tipo === 'receber'));
  readonly pagar   = computed(() => this.data.titulos().filter(t => t.tipo === 'pagar'));

  readonly totalReceber = computed(() =>
    this.receber().filter(t => t.status === 'aberto').reduce((s,t) => s + t.valor, 0)
  );
  readonly totalPagar = computed(() =>
    this.pagar().filter(t => t.status === 'aberto').reduce((s,t) => s + t.valor, 0)
  );
  readonly saldoBancario = 42310;
  readonly totalVencido = computed(() =>
    this.receber().filter(t => t.status === 'vencido').reduce((s,t) => s + t.valor, 0)
  );
  readonly vencidosCount = computed(() => 
    this.receber().filter(t => t.status === 'vencido').length
  );

  readonly cashflowBars = [
    { day: 'Hoje', inH: 60, outH: 30 },
    { day: 'Dom',  inH: 20, outH: 55 },
    { day: 'Seg',  inH: 70, outH: 20 },
    { day: 'Ter',  inH: 45, outH: 68 },
    { day: 'Qua',  inH: 80, outH: 35 },
    { day: 'Qui',  inH: 40, outH: 25 },
    { day: 'Sex',  inH: 55, outH: 45 },
  ];

  baixarTitulo(id: string) { this.toast.success('Baixa registrada com sucesso!'); }
}
