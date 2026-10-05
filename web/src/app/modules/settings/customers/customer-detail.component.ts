import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Customer, CustomerService } from '../../../core/services/customer.service';
import { BadgeComponent } from '../../../shared/components/badge/badge.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { BrlPipe } from '../../../shared/pipes/brl.pipe';
import { ADDRESS_TYPE_LABELS, CONTACT_LABELS, IE_LABELS, STATUS_BADGES, formatAddress } from './customer-display';

@Component({
  selector: 'app-customer-detail',
  standalone: true,
  imports: [RouterLink, BadgeComponent, PageHeaderComponent, BrlPipe],
  styleUrl: './customer-detail.component.scss',
  templateUrl: './customer-detail.component.html'
})
export class CustomerDetailComponent {
  private readonly customerService = inject(CustomerService);

  /** Route param `:id` (withComponentInputBinding). */
  readonly id = input.required<string>();

  readonly statusBadges = STATUS_BADGES;
  readonly loadStatus = signal<'loading' | 'ready' | 'not-found' | 'error'>('loading');
  readonly customer = signal<Customer | null>(null);

  constructor() {
    effect(() => {
      const id = this.id();
      untracked(() => void this.load(id));
    });
  }

  async load(id = this.id()) {
    this.loadStatus.set('loading');
    this.customer.set(null);
    try {
      const customer = await this.customerService.get(id);
      if (id !== this.id()) return; // a rota mudou enquanto a resposta vinha
      this.customer.set(customer);
      this.loadStatus.set('ready');
    } catch (error) {
      if (id !== this.id()) return;
      this.loadStatus.set((error as { status?: number })?.status === 404 ? 'not-found' : 'error');
    }
  }

  readonly ieLabel = computed(() => {
    const ie = this.customer()?.ieIndicator;
    return ie ? IE_LABELS[ie] ?? '--' : '--';
  });

  readonly contacts = computed(() =>
    (this.customer()?.contacts ?? []).map(ct => ({ type: CONTACT_LABELS[ct.type] ?? ct.type, value: ct.value || '--' }))
  );

  readonly addresses = computed(() =>
    (this.customer()?.addresses ?? []).map(a => ({
      type: ADDRESS_TYPE_LABELS[a.type] ?? a.type,
      isDefault: a.isDefault,
      text: formatAddress(a),
    }))
  );

  readonly priceTables = computed(() =>
    [...(this.customer()?.priceTables ?? [])]
      .sort((a, b) => a.priority - b.priority)
      .map(t => ({ id: t.priceTableId, priority: t.priority }))
  );
}
