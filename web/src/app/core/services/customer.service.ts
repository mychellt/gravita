import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';

export type CustomerType = 'INDIVIDUAL' | 'COMPANY';
export type IeIndicator = 'TAXPAYER' | 'EXEMPT' | 'NON_TAXPAYER';
export type CustomerStatus = 'REGULAR' | 'BLOCKED' | 'DELINQUENT';
export type AddressType = 'BILLING' | 'DELIVERY';
export type ContactType = 'EMAIL' | 'WHATSAPP' | 'PHONE';

export interface CustomerAddress {
  type: AddressType;
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
  type: ContactType;
  value: string;
}

export interface CustomerPriceTableLink {
  priceTableId: string;
  priority: number;
}

/** Cliente do tenant, como devolvido por GET /api/customers e GET /api/customers/{id}. */
export interface Customer {
  id: string;
  name: string;
  type: CustomerType;
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
  priceTables: CustomerPriceTableLink[];
}

/** Corpo do POST /api/customers (`RegisterCustomerRequest`). */
export interface RegisterCustomerRequest {
  type: CustomerType;
  document: string;
  name: string;
  email: string | null;
  ieIndicator: IeIndicator;
  finalConsumer: boolean;
  creditLimit: number;
  addresses: CustomerAddress[];
  contacts: CustomerContact[];
}

@Injectable({ providedIn: 'root' })
export class CustomerService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly baseUrl = `${environment.apiUrl}/customers`;

  /** Os endpoints de clientes identificam o chamador pelo token da sessão. */
  private get headers() {
    return { Authorization: `Bearer ${this.auth.sessionToken()}` };
  }

  list(): Promise<Customer[]> {
    return firstValueFrom(this.http.get<Customer[]>(this.baseUrl, { headers: this.headers }));
  }

  get(id: string): Promise<Customer> {
    return firstValueFrom(this.http.get<Customer>(`${this.baseUrl}/${id}`, { headers: this.headers }));
  }

  register(request: RegisterCustomerRequest): Promise<Customer> {
    return firstValueFrom(this.http.post<Customer>(this.baseUrl, request, { headers: this.headers }));
  }
}
