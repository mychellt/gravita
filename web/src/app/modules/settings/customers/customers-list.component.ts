import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { Customer, CustomerService } from '../../../core/services/customer.service';
import { ToastService } from '../../../core/services/toast.service';
import { ADMINISTRATOR_PROFILE } from '../../../core/user-display';
import { BadgeComponent } from '../../../shared/components/badge/badge.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { BrlPipe } from '../../../shared/pipes/brl.pipe';
import { STATUS_BADGES } from './customer-display';
import { CustomerFormComponent } from './customer-form.component';

@Component({
  selector: 'app-customers-list',
  standalone: true,
  imports: [RouterLink, BadgeComponent, PageHeaderComponent, BrlPipe, CustomerFormComponent],
  styleUrl: './customers-list.component.scss',
  templateUrl: './customers-list.component.html'
})
export class CustomersListComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly customerService = inject(CustomerService);
  private readonly toast = inject(ToastService);

  readonly statusBadges = STATUS_BADGES;

  searchQuery = signal('');
  readonly loadStatus = signal<'loading' | 'ready' | 'error'>('loading');
  readonly showForm = signal(false);
  private readonly allCustomers = signal<Customer[]>([]);

  /** Qualquer perfil vê a lista; só o Administrador cadastra. */
  readonly canRegister = computed(() => this.auth.currentUser()?.profile === ADMINISTRATOR_PROFILE);

  readonly customers = computed(() => {
    const q = this.searchQuery().trim().toLowerCase();
    const digits = q.replace(/\D/g, '');
    return this.allCustomers().filter(c =>
      !q ||
      c.name.toLowerCase().includes(q) ||
      c.document.toLowerCase().includes(q) ||
      (digits !== '' && c.document.replace(/\D/g, '').includes(digits))
    );
  });

  async ngOnInit() {
    if (!this.auth.currentUser()) void this.auth.loadCurrentUser();
    await this.load();
  }

  async load() {
    this.loadStatus.set('loading');
    try {
      this.allCustomers.set(await this.customerService.list());
      this.loadStatus.set('ready');
    } catch {
      this.loadStatus.set('error');
    }
  }

  onRegistered(customer: Customer) {
    this.allCustomers.update(all => [...all, customer]);
    this.showForm.set(false);
    this.toast.success('Cliente cadastrado com sucesso!');
  }
}
