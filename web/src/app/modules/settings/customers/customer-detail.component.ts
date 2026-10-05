import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import {
  ADDRESS_LABELS, CONTACT_LABELS, IE_INDICATOR_LABELS, documentLabel, formatAddress, statusBadge,
} from '../../../core/customer-display';
import { Customer, CustomerService } from '../../../core/services/customer.service';
import { BadgeComponent } from '../../../shared/components/badge/badge.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { BrlPipe } from '../../../shared/pipes/brl.pipe';

type LoadState = 'loading' | 'ready' | 'not-found' | 'error';

@Component({
  selector: 'app-customer-detail',
  standalone: true,
  imports: [RouterLink, BadgeComponent, PageHeaderComponent, BrlPipe],
  styleUrl: './customer-detail.component.scss',
  templateUrl: './customer-detail.component.html'
})
export class CustomerDetailComponent {
  private readonly service = inject(CustomerService);

  /** Route param `:id` (withComponentInputBinding). */
  readonly id = input.required<string>();

  readonly state = signal<LoadState>('loading');
  readonly customer = signal<Customer | null>(null);

  readonly documentLabel = computed(() => documentLabel(this.customer()?.type ?? 'COMPANY'));
  readonly badge = computed(() => statusBadge(this.customer()?.status));
  readonly ieLabel = computed(() => {
    const indicator = this.customer()?.ieIndicator;
    return indicator ? IE_INDICATOR_LABELS[indicator] : '--';
  });

  readonly addresses = computed(() =>
    (this.customer()?.addresses ?? []).map(a => ({
      kind: ADDRESS_LABELS[a.type] ?? a.type, isDefault: a.isDefault, text: formatAddress(a),
    })));

  readonly contacts = computed(() =>
    (this.customer()?.contacts ?? []).map(ct => ({ type: CONTACT_LABELS[ct.type] ?? ct.type, value: ct.value || '--' })));

  /** Não há endpoint para consultar tabelas de preço; mostramos a referência curta até existir. */
  readonly priceTables = computed(() =>
    [...(this.customer()?.priceTables ?? [])]
      .sort((a, b) => a.priority - b.priority)
      .map(t => ({ name: `Tabela ${t.priceTableId.slice(0, 8)}`, priority: t.priority })));

  constructor() {
    effect(() => {
      const id = this.id();
      untracked(() => void this.load(id));
    });
  }

  async load(id = this.id()) {
    this.state.set('loading');
    this.customer.set(null);
    try {
      this.customer.set(await this.service.get(id));
      this.state.set('ready');
    } catch (error) {
      this.state.set(error instanceof HttpErrorResponse && error.status === 404 ? 'not-found' : 'error');
    }
  }
}
