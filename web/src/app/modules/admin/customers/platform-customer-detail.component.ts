import { Component, computed, inject, input } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { PlatformConfigService } from '../../../core/services/platform-config.service';
import { PlatformCustomersService } from '../../../core/services/platform-customers.service';
import { BadgeComponent } from '../../../shared/components/badge/badge.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { BrlPipe } from '../../../shared/pipes/brl.pipe';

@Component({
  selector: 'app-platform-customer-detail',
  standalone: true,
  imports: [DatePipe, RouterLink, BadgeComponent, PageHeaderComponent, BrlPipe],
  styleUrl: '../../settings/customers/customer-detail.component.scss',
  templateUrl: './platform-customer-detail.component.html'
})
export class PlatformCustomerDetailComponent {
  private customers = inject(PlatformCustomersService);
  private platform = inject(PlatformConfigService);

  /** Route param `:id` (withComponentInputBinding). */
  readonly id = input.required<string>();

  readonly customer = computed(() => this.customers.getById(this.id()));
  readonly plan = computed(() => this.platform.config().plans.find(p => p.tier === this.customer()?.planTier));

  readonly limits = computed(() => {
    const l = this.plan()?.limits;
    if (!l) return [];
    const fmt = (n: number | null) => (n == null ? 'Ilimitado' : String(n));
    return [
      { label: 'CNPJs', value: fmt(l.cnpjs) },
      { label: 'Filiais', value: fmt(l.filiais) },
      { label: 'Caixas PDV', value: fmt(l.caixasPdv) },
      { label: 'Usuários', value: fmt(l.usuarios) },
    ];
  });
}
