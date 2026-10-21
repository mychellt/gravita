import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { PlatformPayment } from '../../../core/models';
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
  styleUrl: '../../settings/customers/customer-payments.component.scss',
  templateUrl: './platform-customer-payments.component.html'
})
export class PlatformCustomerPaymentsComponent {
  private customers = inject(PlatformCustomersService);

  /** Route param `:id` (withComponentInputBinding). */
  readonly id = input.required<string>();

  readonly status = this.customers.status;
  readonly customer = computed(() => this.customers.getById(this.id()));
  private readonly received = signal<PlatformPayment[]>([]);
  readonly paymentsStatus = signal<'loading' | 'ready' | 'error'>('loading');

  constructor() {
    void this.customers.load();
    effect(() => { void this.loadPayments(this.id()); }, { allowSignalWrites: true });
  }

  private async loadPayments(id: string) {
    this.paymentsStatus.set('loading');
    try {
      this.received.set(await this.customers.payments(id));
      this.paymentsStatus.set('ready');
    } catch {
      this.paymentsStatus.set('error');
    }
  }

  /** Most recent first; an unpaid invoice past its due date is shown as overdue. */
  readonly payments = computed(() => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return [...this.received()]
      .sort((a, b) => b.vencimento.getTime() - a.vencimento.getTime())
      .map(p => ({ ...p, vencido: p.status === 'aberto' && p.vencimento < today }));
  });

  readonly totalPago = computed(() => this.payments().filter(p => p.status === 'pago').reduce((s, p) => s + p.valor, 0));
  readonly vencidos = computed(() => this.payments().filter(p => p.vencido));
  readonly totalVencido = computed(() => this.vencidos().reduce((s, p) => s + p.valor, 0));
}
