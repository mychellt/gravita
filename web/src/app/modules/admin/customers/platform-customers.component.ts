import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { PlatformConfigService } from '../../../core/services/platform-config.service';
import { PlatformCustomersService } from '../../../core/services/platform-customers.service';
import { BadgeComponent } from '../../../shared/components/badge/badge.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { BrlPipe } from '../../../shared/pipes/brl.pipe';

@Component({
  selector: 'app-platform-customers',
  standalone: true,
  imports: [DatePipe, RouterLink, BadgeComponent, PageHeaderComponent, BrlPipe],
  styleUrl: './platform-customers.component.scss',
  templateUrl: './platform-customers.component.html'
})
export class PlatformCustomersComponent {
  private customersSvc = inject(PlatformCustomersService);
  private platform = inject(PlatformConfigService);

  readonly searchQuery = signal('');

  readonly customers = computed(() => {
    const q = this.searchQuery().trim().toLowerCase();
    const digits = q.replace(/\D/g, '');
    const plans = this.platform.config().plans;
    return this.customersSvc.customers()
      .filter(c =>
        !q ||
        c.razaoSocial.toLowerCase().includes(q) ||
        c.nomeFantasia.toLowerCase().includes(q) ||
        (digits !== '' && c.cnpj.replace(/\D/g, '').includes(digits))
      )
      .map(c => {
        const plan = plans.find(p => p.tier === c.planTier);
        return { ...c, planName: plan?.name ?? c.planTier, planPrice: plan?.priceMonthly };
      });
  });
}
