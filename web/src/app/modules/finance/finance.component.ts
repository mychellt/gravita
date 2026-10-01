import { Component, computed, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { DataService } from '../../core/services/data.service';
import { ToastService } from '../../core/services/toast.service';
import { BadgeComponent } from '../../shared/components/badge/badge.component';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { BrlPipe } from '../../shared/pipes/brl.pipe';

@Component({
  selector: 'app-finance',
  standalone: true,
  imports: [DatePipe, BadgeComponent, PageHeaderComponent, BrlPipe],
  styleUrl: './finance.component.scss',
  templateUrl: './finance.component.html'
})
export class FinanceComponent {
  constructor(public data: DataService, private toast: ToastService) {}

  activeTab = signal<'receivable' | 'payable' | 'cashFlow' | 'reconciliation'>('receivable');

  readonly receivables = computed(() => this.data.titulos().filter(t => t.tipo === 'receber'));
  readonly payables    = computed(() => this.data.titulos().filter(t => t.tipo === 'pagar'));

  readonly totalReceivable = computed(() =>
    this.receivables().filter(t => t.status === 'aberto').reduce((s,t) => s + t.valor, 0)
  );
  readonly totalPayable = computed(() =>
    this.payables().filter(t => t.status === 'aberto').reduce((s,t) => s + t.valor, 0)
  );
  readonly bankBalance = 42310;
  readonly totalOverdue = computed(() =>
    this.receivables().filter(t => t.status === 'vencido').reduce((s,t) => s + t.valor, 0)
  );
  readonly overdueCount = computed(() => 
    this.receivables().filter(t => t.status === 'vencido').length
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

  settleTitle(id: string) { this.toast.success('Baixa registrada com sucesso!'); }
}
