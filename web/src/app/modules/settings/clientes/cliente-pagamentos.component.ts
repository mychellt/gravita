import { Component, computed, inject, input } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { DataService } from '../../../core/services/data.service';
import { Recebivel, RecebivelOrigem } from '../../../core/models';
import { BadgeComponent } from '../../../shared/components/badge/badge.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { BrlPipe } from '../../../shared/pipes/brl.pipe';

const ORIGEM_LABELS: Record<RecebivelOrigem, string> = {
  faturamento: 'Faturamento',
  manual: 'Manual',
  renegociacao: 'Renegociação',
};

/** Still owed by the customer (backend `Receivable.isOutstanding`). */
const isOutstanding = (r: Recebivel) => r.status === 'aberto' || r.status === 'parcial';

/** Outstanding and past its due date (backend `Receivable.isOverdue`). */
const isOverdue = (r: Recebivel, today: Date) => isOutstanding(r) && r.vencimento < today;

@Component({
  selector: 'app-cliente-pagamentos',
  standalone: true,
  imports: [DatePipe, RouterLink, BadgeComponent, PageHeaderComponent, BrlPipe],
  styleUrl: './cliente-pagamentos.component.scss',
  templateUrl: './cliente-pagamentos.component.html'
})
export class ClientePagamentosComponent {
  private data = inject(DataService);

  /** Route param `:id` (withComponentInputBinding). */
  readonly id = input.required<string>();

  readonly cliente = computed(() => this.data.clientes().find(c => c.id === this.id()));

  readonly titulos = computed(() => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return this.data.recebiveis()
      .filter(r => r.clienteId === this.id())
      .sort((a, b) => a.vencimento.getTime() - b.vencimento.getTime())
      .map(r => ({
        ...r,
        origemLabel: ORIGEM_LABELS[r.origem] ?? r.origem,
        parcelaLabel: r.parcela != null && r.totalParcelas != null ? `${r.parcela}/${r.totalParcelas}` : '--',
        vencido: isOverdue(r, today),
      }));
  });

  readonly vencidos = computed(() => this.titulos().filter(t => t.vencido));
  readonly totalVencido = computed(() => this.vencidos().reduce((s, t) => s + t.valor, 0));
}
