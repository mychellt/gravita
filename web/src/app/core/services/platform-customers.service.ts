import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PlanTier, PlatformCustomer, PlatformPayment } from '../models';
import type { LoadStatus } from './customer.service';

type SubscriptionStatus = 'PENDING' | 'ACTIVE' | 'PAYMENT_FAILURE';

/** Cliente da plataforma, como devolvido por GET /api/admin/customers (PlatformCustomerView). */
interface PlatformCustomerResponse {
  companyId: string;
  cnpj: string;
  name: string;
  email: string | null;
  phone: string | null;
  planTier: PlanTier;
  planName: string;
  status: SubscriptionStatus;
  activationDate: string;
  expirationDate: string;
}

/** Pagamento recebido, como devolvido por GET /api/admin/customers/{id}/payments (PlatformPaymentView). */
interface PlatformPaymentResponse {
  id: string;
  amount: number;
  paymentDate: string;
}

const STATUS: Record<SubscriptionStatus, PlatformCustomer['status']> = {
  ACTIVE: 'ativo', PAYMENT_FAILURE: 'inadimplente', PENDING: 'inativo',
};

/** `yyyy-MM-dd` vira data local (e não UTC, que recuaria um dia no Brasil). */
const localDate = (iso: string) => {
  const [year, month, day] = iso.split('-').map(Number);
  return new Date(year, month - 1, day);
};

const formatCnpj = (digits: string) =>
  digits.length === 14 ? digits.replace(/^(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})$/, '$1.$2.$3/$4-$5') : digits;

/** Clientes da Gravita (empresas com assinatura): GET /api/admin/customers, somente leitura. */
@Injectable({ providedIn: 'root' })
export class PlatformCustomersService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/admin/customers`;

  private readonly list = signal<PlatformCustomer[]>([]);
  private readonly loadState = signal<LoadStatus>('loading');

  readonly customers = this.list.asReadonly();
  readonly status = this.loadState.asReadonly();

  /** Recarrega os clientes; nunca rejeita, o resultado fica em `status`. */
  async load(): Promise<void> {
    this.loadState.set('loading');
    try {
      const response = await firstValueFrom(this.http.get<PlatformCustomerResponse[]>(this.baseUrl));
      this.list.set(response.map(c => ({
        id: c.companyId,
        cnpj: formatCnpj(c.cnpj),
        razaoSocial: c.name,
        nomeFantasia: '',
        email: c.email ?? '',
        planTier: c.planTier,
        status: STATUS[c.status],
        assinaturaDesde: localDate(c.activationDate),
        telefone: c.phone ?? undefined,
      })));
      this.loadState.set('ready');
    } catch {
      this.loadState.set('error');
    }
  }

  getById(id: string): PlatformCustomer | undefined {
    return this.list().find(c => c.id === id);
  }

  /** Pagamentos já recebidos da assinatura. Rejeita se o servidor falha. */
  async payments(customerId: string): Promise<PlatformPayment[]> {
    const response = await firstValueFrom(this.http.get<PlatformPaymentResponse[]>(`${this.baseUrl}/${customerId}/payments`));
    return response.map(p => {
      const paidAt = localDate(p.paymentDate);
      return {
        id: p.id, customerId, competencia: new Date(paidAt.getFullYear(), paidAt.getMonth(), 1), vencimento: paidAt,
        valor: p.amount, status: 'pago' as const, pagoEm: paidAt,
      };
    });
  }
}
