import { Component, computed, inject, output, signal } from '@angular/core';
import {
  AddressType, ContactType, Customer, CustomerAddress, CustomerService, CustomerType, IeIndicator,
  RegisterCustomerRequest,
} from '../../../core/services/customer.service';
import { failureDetail } from '../../../core/services/company.service';
import { ToastService } from '../../../core/services/toast.service';
import { ADDRESS_TYPE_LABELS, CONTACT_LABELS, IE_LABELS } from './customer-display';

interface AddressDraft {
  type: AddressType;
  street: string;
  number: string;
  complement: string;
  neighborhood: string;
  city: string;
  state: string;
  zipCode: string;
  isDefault: boolean;
}

interface ContactDraft {
  type: ContactType;
  value: string;
}

const emptyAddress = (isDefault: boolean): AddressDraft => ({
  type: 'BILLING', street: '', number: '', complement: '', neighborhood: '', city: '', state: '', zipCode: '', isDefault,
});

const isBlank = (a: AddressDraft) =>
  [a.street, a.number, a.complement, a.neighborhood, a.city, a.state, a.zipCode].every(v => !v.trim());

/** The backend's required address fields (`RegisterCustomerRequest.AddressRequest`). */
const isComplete = (a: AddressDraft) =>
  [a.street, a.neighborhood, a.city, a.state, a.zipCode].every(v => v.trim());

const DOCUMENT_LENGTH: Record<CustomerType, number> = { INDIVIDUAL: 11, COMPANY: 14 };

/** Formulário de cadastro de cliente (`POST /api/customers`). */
@Component({
  selector: 'app-customer-form',
  standalone: true,
  templateUrl: './customer-form.component.html',
  styleUrl: './customer-form.component.scss'
})
export class CustomerFormComponent {
  private readonly customers = inject(CustomerService);
  private readonly toast = inject(ToastService);

  readonly registered = output<Customer>();
  readonly cancelled = output<void>();

  readonly ieOptions = Object.entries(IE_LABELS) as [IeIndicator, string][];
  readonly addressTypeOptions = Object.entries(ADDRESS_TYPE_LABELS) as [AddressType, string][];
  readonly contactTypeOptions = Object.entries(CONTACT_LABELS) as [ContactType, string][];

  readonly type = signal<CustomerType>('COMPANY');
  readonly document = signal('');
  readonly name = signal('');
  readonly email = signal('');
  readonly ieIndicator = signal<IeIndicator>('TAXPAYER');
  readonly finalConsumer = signal(false);
  readonly creditLimit = signal('');
  readonly addresses = signal<AddressDraft[]>([emptyAddress(true)]);
  readonly contacts = signal<ContactDraft[]>([]);
  readonly saving = signal(false);

  readonly documentLabel = computed(() => (this.type() === 'INDIVIDUAL' ? 'CPF' : 'CNPJ'));

  private readonly filledAddresses = computed(() => this.addresses().filter(a => !isBlank(a)));

  /** Types with more than one address must have exactly one default (the backend's `validateAddresses`). */
  readonly defaultRuleViolation = computed(() => {
    const filled = this.filledAddresses();
    return this.addressTypeOptions.some(([type]) => {
      const ofType = filled.filter(a => a.type === type);
      return ofType.length > 1 && ofType.filter(a => a.isDefault).length !== 1;
    });
  });

  private readonly creditLimitValue = computed(() => {
    const raw = this.creditLimit().trim().replace(',', '.');
    return raw === '' ? 0 : Number(raw);
  });

  /** At least one address, every filled one complete — mirrors `CustomerDomain.validateForRegistration`. */
  readonly addressesValid = computed(() => {
    const filled = this.filledAddresses();
    return filled.length > 0 && filled.every(isComplete) && !this.defaultRuleViolation();
  });

  readonly canSubmit = computed(() =>
    !this.saving() &&
    !!this.name().trim() &&
    this.document().replace(/\D/g, '').length === DOCUMENT_LENGTH[this.type()] &&
    Number.isFinite(this.creditLimitValue()) && this.creditLimitValue() >= 0 &&
    this.addressesValid());

  setType(type: CustomerType) {
    this.type.set(type);
  }

  addAddress() {
    this.addresses.update(list => [...list, emptyAddress(false)]);
  }

  removeAddress(index: number) {
    this.addresses.update(list => list.filter((_, i) => i !== index));
  }

  patchAddress(index: number, patch: Partial<AddressDraft>) {
    this.addresses.update(list => list.map((a, i) => (i === index ? { ...a, ...patch } : a)));
  }

  /** Marking an address as the default of its type unmarks the others of that type. */
  setDefault(index: number, isDefault: boolean) {
    this.addresses.update(list => {
      const type = list[index].type;
      return list.map((a, i) => (i === index ? { ...a, isDefault } : isDefault && a.type === type ? { ...a, isDefault: false } : a));
    });
  }

  addContact() {
    this.contacts.update(list => [...list, { type: 'PHONE', value: '' }]);
  }

  removeContact(index: number) {
    this.contacts.update(list => list.filter((_, i) => i !== index));
  }

  patchContact(index: number, patch: Partial<ContactDraft>) {
    this.contacts.update(list => list.map((c, i) => (i === index ? { ...c, ...patch } : c)));
  }

  async submit() {
    if (!this.canSubmit()) return;
    this.saving.set(true);
    try {
      this.registered.emit(await this.customers.register(this.toRequest()));
    } catch (error) {
      // A falha deixa o formulário como está para o usuário corrigir e reenviar.
      const detail = failureDetail(error);
      this.toast.danger(detail ? `Não foi possível cadastrar: ${detail}` : 'Não foi possível cadastrar o cliente.');
    } finally {
      this.saving.set(false);
    }
  }

  private toRequest(): RegisterCustomerRequest {
    const filled = this.filledAddresses();
    return {
      type: this.type(),
      document: this.document().replace(/\D/g, ''),
      name: this.name().trim(),
      email: this.email().trim() || null,
      ieIndicator: this.ieIndicator(),
      finalConsumer: this.finalConsumer(),
      creditLimit: this.creditLimitValue(),
      // The only address of its type is that type's default, whatever the checkbox says.
      addresses: filled.map(a => this.toAddress(a, filled.filter(o => o.type === a.type).length === 1)),
      contacts: this.contacts().filter(c => c.value.trim()).map(c => ({ type: c.type, value: c.value.trim() })),
    };
  }

  private toAddress(a: AddressDraft, onlyOfType: boolean): CustomerAddress {
    return {
      type: a.type,
      street: a.street.trim(),
      number: a.number.trim() || null,
      complement: a.complement.trim() || null,
      neighborhood: a.neighborhood.trim(),
      city: a.city.trim(),
      state: a.state.trim().toUpperCase(),
      zipCode: a.zipCode.trim(),
      isDefault: onlyOfType || a.isDefault,
    };
  }
}
