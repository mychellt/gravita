import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';

export type PersonKind = 'INDIVIDUAL' | 'COMPANY';
export type IeIndicator = 'TAXPAYER' | 'EXEMPT' | 'NON_TAXPAYER';
export type AddressKind = 'BILLING' | 'DELIVERY';
export type ContactKind = 'EMAIL' | 'WHATSAPP' | 'PHONE';
export type CustomerStatus = 'REGULAR' | 'BLOCKED' | 'DELINQUENT';

export interface CustomerAddress {
  type: AddressKind;
  street: string;
  number: string | null;
  complement: string | null;
  neighborhood: string;
  city: string;
  state: string;
  zipCode: string;
  isDefault: boolean;
}

export interface CustomerContact {
  type: ContactKind;
  value: string;
}

export interface CustomerPriceTable {
  priceTableId: string;
  priority: number;
}

/** Cliente, como devolvido por GET /api/customers e GET /api/customers/{id}. */
export interface Customer {
  id: string;
  name: string;
  type: PersonKind;
  document: string;
  email: string | null;
  ieIndicator: IeIndicator | null;
  finalConsumer: boolean | null;
  creditLimit: number | null;
  currentBalance: number | null;
  status: CustomerStatus | null;
  companyId: string | null;
  addresses: CustomerAddress[];
  contacts: CustomerContact[];
  priceTables: CustomerPriceTable[];
}

/** Corpo do POST /api/customers (RegisterCustomerRequest). */
export interface NewCustomer {
  type: PersonKind;
  document: string;
  name: string;
  email?: string;
  ieIndicator?: IeIndicator;
  finalConsumer?: boolean;
  creditLimit?: number;
  addresses: CustomerAddress[];
  contacts?: CustomerContact[];
}

export type LoadStatus = 'loading' | 'ready' | 'error';

/** Mensagem do backend para uma falha (`{message}`), se houver. */
export function customerFailureDetail(error: unknown): string {
  if (!(error instanceof HttpErrorResponse)) return '';
  const message = error.error?.message;
  return typeof message === 'string' ? message.trim() : '';
}

const byName = (a: Customer, b: Customer) => a.name.localeCompare(b.name, 'pt-BR');

/** Clientes do cadastro (GET/POST /api/customers). Mantém a lista em memória para a tela refletir um novo cadastro sem recarregar. */
@Injectable({ providedIn: 'root' })
export class CustomerService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/customers`;

  private readonly list = signal<Customer[]>([]);
  private readonly loadStatus = signal<LoadStatus>('loading');

  readonly customers = this.list.asReadonly();
  readonly status = this.loadStatus.asReadonly();

  /** GET /api/customers — nunca rejeita: o resultado fica em `status`. */
  async load(): Promise<void> {
    this.loadStatus.set('loading');
    try {
      const customers = await firstValueFrom(this.http.get<Customer[]>(this.baseUrl));
      this.list.set([...customers].sort(byName));
      this.loadStatus.set('ready');
    } catch {
      this.loadStatus.set('error');
    }
  }

  /** GET /api/customers/{id} — rejeita se o cliente não existe (404) ou o servidor falha. */
  get(id: string): Promise<Customer> {
    return firstValueFrom(this.http.get<Customer>(`${this.baseUrl}/${id}`));
  }

  /** POST /api/customers — o cliente criado entra na lista já carregada. */
  async register(customer: NewCustomer): Promise<Customer> {
    const created = await firstValueFrom(this.http.post<Customer>(this.baseUrl, customer));
    this.list.update(current => [...current.filter(c => c.id !== created.id), created].sort(byName));
    return created;
  }
}
