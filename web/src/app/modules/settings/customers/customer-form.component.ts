import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ADDRESS_LABELS, CONTACT_LABELS, IE_INDICATOR_LABELS } from '../../../core/customer-display';
import { AuthService } from '../../../core/services/auth.service';
import {
  AddressKind, ContactKind, CustomerService, IeIndicator, PersonKind, customerFailureDetail,
} from '../../../core/services/customer.service';
import { ToastService } from '../../../core/services/toast.service';
import { ADMINISTRATOR_PROFILE } from '../../../core/user-display';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import {
  AddressDraft, ContactDraft, CustomerDraft, emptyAddress, toNewCustomer, validateCustomerDraft,
} from './customer-form.validation';

@Component({
  selector: 'app-customer-form',
  standalone: true,
  imports: [RouterLink, PageHeaderComponent],
  styleUrl: './customer-form.component.scss',
  templateUrl: './customer-form.component.html'
})
export class CustomerFormComponent {
  private readonly service = inject(CustomerService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  readonly ieOptions = Object.entries(IE_INDICATOR_LABELS) as [IeIndicator, string][];
  readonly addressKinds = Object.entries(ADDRESS_LABELS) as [AddressKind, string][];
  readonly contactKinds = Object.entries(CONTACT_LABELS) as [ContactKind, string][];

  /** Só o Administrador cadastra clientes (a lista continua visível a todos). */
  readonly profileKnown = computed(() => this.auth.currentUser() !== null);
  readonly isAdministrator = computed(() => this.auth.currentUser()?.profile === ADMINISTRATOR_PROFILE);

  readonly type = signal<PersonKind>('COMPANY');
  readonly document = signal('');
  readonly name = signal('');
  readonly email = signal('');
  readonly ieIndicator = signal<IeIndicator | ''>('');
  readonly finalConsumer = signal<'yes' | 'no' | ''>('');
  readonly creditLimit = signal('');
  readonly addresses = signal<AddressDraft[]>([emptyAddress('BILLING', true)]);
  readonly contacts = signal<ContactDraft[]>([]);

  readonly submitting = signal(false);
  readonly formError = signal('');
  private readonly touched = signal<ReadonlySet<string>>(new Set());

  readonly draft = computed<CustomerDraft>(() => ({
    type: this.type(), document: this.document(), name: this.name(), email: this.email(),
    ieIndicator: this.ieIndicator(), finalConsumer: this.finalConsumer(), creditLimit: this.creditLimit(),
    addresses: this.addresses(), contacts: this.contacts(),
  }));

  readonly errors = computed(() => validateCustomerDraft(this.draft()));
  readonly canSubmit = computed(() => Object.keys(this.errors()).length === 0 && !this.submitting());

  /** Erro de um campo, mostrado só depois de o usuário passar por ele. */
  error(key: string): string {
    return this.touched().has(key) ? this.errors()[key] ?? '' : '';
  }

  touch(key: string) {
    this.touched.update(set => new Set(set).add(key));
  }

  setType(type: PersonKind) {
    this.type.set(type);
    if (type === 'INDIVIDUAL') { this.ieIndicator.set(''); this.finalConsumer.set(''); }
  }

  updateAddress(index: number, patch: Partial<AddressDraft>) {
    this.addresses.update(list => list.map((a, i) => (i === index ? { ...a, ...patch } : a)));
  }

  /** O endereço padrão é único por tipo: marcar um desmarca os outros do mesmo tipo. */
  setDefault(index: number, isDefault: boolean) {
    this.addresses.update(list => {
      const type = list[index].type;
      return list.map((a, i) => (i === index ? { ...a, isDefault } : isDefault && a.type === type ? { ...a, isDefault: false } : a));
    });
  }

  addAddress() {
    const hasDelivery = this.addresses().some(a => a.type === 'DELIVERY');
    this.addresses.update(list => [...list, emptyAddress('DELIVERY', !hasDelivery)]);
  }

  removeAddress(index: number) {
    this.addresses.update(list => list.filter((_, i) => i !== index));
  }

  addContact() {
    this.contacts.update(list => [...list, { type: 'PHONE', value: '' }]);
  }

  updateContact(index: number, patch: Partial<ContactDraft>) {
    this.contacts.update(list => list.map((c, i) => (i === index ? { ...c, ...patch } : c)));
  }

  removeContact(index: number) {
    this.contacts.update(list => list.filter((_, i) => i !== index));
  }

  async submit(event?: Event) {
    event?.preventDefault();
    if (!this.canSubmit() || !this.isAdministrator()) return;

    this.formError.set('');
    this.submitting.set(true);
    try {
      const created = await this.service.register(toNewCustomer(this.draft()));
      this.toast.success(`Cliente ${created.name} cadastrado.`);
      await this.router.navigateByUrl('/settings/customers');
    } catch (error) {
      const detail = customerFailureDetail(error);
      this.formError.set(detail ? `Não foi possível cadastrar: ${detail}` : 'Não foi possível cadastrar o cliente agora. Tente novamente.');
    } finally {
      this.submitting.set(false);
    }
  }
}
