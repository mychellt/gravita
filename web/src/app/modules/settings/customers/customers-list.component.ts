import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ADMINISTRATOR_PROFILE } from '../../../core/user-display';
import { statusBadge } from '../../../core/customer-display';
import { AuthService } from '../../../core/services/auth.service';
import { CustomerService } from '../../../core/services/customer.service';
import { BadgeComponent } from '../../../shared/components/badge/badge.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { BrlPipe } from '../../../shared/pipes/brl.pipe';

@Component({
  selector: 'app-customers-list',
  standalone: true,
  imports: [RouterLink, BadgeComponent, PageHeaderComponent, BrlPipe],
  styleUrl: './customers-list.component.scss',
  templateUrl: './customers-list.component.html'
})
export class CustomersListComponent implements OnInit {
  private readonly service = inject(CustomerService);
  private readonly auth = inject(AuthService);

  readonly status = this.service.status;
  readonly badge = statusBadge;

  /** A lista é visível a todos os perfis; só o Administrador cadastra. */
  readonly canCreate = computed(() => this.auth.currentUser()?.profile === ADMINISTRATOR_PROFILE);

  searchQuery = signal('');

  readonly customers = computed(() => {
    const q = this.searchQuery().trim().toLowerCase();
    const digits = q.replace(/\D/g, '');
    return this.service.customers().filter(c =>
      !q ||
      c.name.toLowerCase().includes(q) ||
      c.document.toLowerCase().includes(q) ||
      (digits !== '' && c.document.replace(/\D/g, '').includes(digits))
    );
  });

  ngOnInit() {
    void this.service.load();
  }

  reload() {
    void this.service.load();
  }
}
