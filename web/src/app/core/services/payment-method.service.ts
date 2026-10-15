import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import type { LoadStatus } from './customer.service';

/** Tipo da forma de pagamento (enum `PaymentMethodType` do backend). */
export type PaymentMethodType = 'CASH' | 'DEBIT_CARD' | 'CREDIT_CARD' | 'PIX' | 'BOLETO' | 'STORE_CREDIT' | 'VOUCHER';

/** Tipo de pagamento com a descrição em português, como devolvido por GET /api/auxiliary/payment-methods. */
export interface PaymentMethodTypeOption {
  code: PaymentMethodType;
  description: string;
}

/** Tipos de forma de pagamento (GET /api/auxiliary/payment-methods) — somente leitura. */
@Injectable({ providedIn: 'root' })
export class PaymentMethodService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/auxiliary/payment-methods`;

  private readonly types = signal<PaymentMethodTypeOption[]>([]);
  private readonly loadStatus = signal<LoadStatus>('loading');

  readonly paymentMethods = this.types.asReadonly();
  readonly status = this.loadStatus.asReadonly();

  /** Nunca rejeita: uma falha vira `status() === 'error'`. */
  async load(): Promise<void> {
    this.loadStatus.set('loading');
    try {
      this.types.set(await firstValueFrom(this.http.get<PaymentMethodTypeOption[]>(this.baseUrl)));
      this.loadStatus.set('ready');
    } catch {
      this.loadStatus.set('error');
    }
  }
}
