import { Component, computed, inject, input } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { PlatformCustomersService } from '../../../core/services/platform-customers.service';
import { BadgeComponent } from '../../../shared/components/badge/badge.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { BrlPipe } from '../../../shared/pipes/brl.pipe';

@Component({
  selector: 'app-platform-customer-payments',
  standalone: true,
  imports: [DatePipe, RouterLink, BadgeComponent, PageHeaderComponent, BrlPipe],
  styleUrl: '../../settings/clientes/cliente-pagamentos.component.scss',
  templateUrl: './platform-customer-payments.component.html'
})
export class PlatformCustomerPaymentsComponent {
  private customers = inject(PlatformCustomersService);

  /** Route param `:id` (withComponentInputBinding). */
  readonly id = input.required<string>();

  readonly customer = computed(() => this.customers.getById(this.id()));

  /** Most recent first; an unpaid invoice past its due date is shown as overdue. */
  readonly payments = computed(() => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return this.customers.payments()
      .filter(p => p.customerId === this.id())
      .sort((a, b) => b.vencimento.getTime() - a.vencimento.getTime())
      .map(p => ({ ...p, vencido: p.status === 'aberto' && p.vencimento < today }));
  });

  readonly totalPago = computed(() => this.payments().filter(p => p.status === 'pago').reduce((s, p) => s + p.valor, 0));
  readonly vencidos = computed(() => this.payments().filter(p => p.vencido));
  readonly totalVencido = computed(() => this.vencidos().reduce((s, p) => s + p.valor, 0));
}
