import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import type { LoadStatus } from './customer.service';

export type ReceivableStatus = 'OPEN' | 'PARTIALLY_SETTLED' | 'SETTLED' | 'RENEGOTIATED' | 'CANCELLED';
export type PayableStatus = 'OPEN' | 'APPROVED' | 'PAID' | 'CANCELLED';

/** Conta a receber, como devolvida por GET /api/finance/receivables (ReceivableResponse). */
export interface ReceivableItem {
  id: string;
  customerId: string;
  customerName: string | null;
  origin: 'INVOICING' | 'MANUAL' | 'RENEGOTIATION';
  amount: number;
  /** O que ainda falta receber: o valor menos as baixas já feitas. */
  remainingBalance: number;
  dueDate: string;
  installments: number | null;
  installmentNumber: number | null;
  status: ReceivableStatus;
}

/** Conta a pagar, como devolvida por GET /api/finance/payables (PayableResponse). */
export interface PayableItem {
  id: string;
  supplierId: string | null;
  supplierName: string | null;
  origin: 'PURCHASE_RECEIPT' | 'MANUAL';
  amount: number;
  dueDate: string;
  status: PayableStatus;
}

export interface CashFlowBucket {
  periodStart: string;
  realizedInflow: number;
  realizedOutflow: number;
  projectedInflow: number;
  projectedOutflow: number;
}

/** Fluxo de caixa de GET /api/finance/cash-flow (CashFlowResponse); só o que a tela usa. */
export interface CashFlow {
  closingBalance: number;
  buckets: CashFlowBucket[];
}

const CASH_FLOW_DAYS = 7;
const isoDate = (date: Date) =>
  `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;

/** Contas a receber, a pagar e fluxo de caixa (/api/finance/*). */
@Injectable({ providedIn: 'root' })
export class FinanceService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/finance`;

  private readonly receivableItems = signal<ReceivableItem[]>([]);
  private readonly payableItems = signal<PayableItem[]>([]);
  private readonly cashFlowState = signal<CashFlow | null>(null);
  private readonly receivablesState = signal<LoadStatus>('loading');
  private readonly payablesState = signal<LoadStatus>('loading');
  private readonly cashFlowStatus = signal<LoadStatus>('loading');

  readonly receivables = this.receivableItems.asReadonly();
  readonly payables = this.payableItems.asReadonly();
  readonly cashFlow = this.cashFlowState.asReadonly();
  readonly receivablesStatus = this.receivablesState.asReadonly();
  readonly payablesStatus = this.payablesState.asReadonly();
  readonly cashFlowLoadStatus = this.cashFlowStatus.asReadonly();

  /** Recarrega as três visões; nunca rejeita, cada resultado fica no seu status. */
  async loadAll(): Promise<void> {
    await Promise.all([this.loadReceivables(), this.loadPayables(), this.loadCashFlow()]);
  }

  async loadReceivables(): Promise<void> {
    this.receivablesState.set('loading');
    try {
      this.receivableItems.set(await firstValueFrom(this.http.get<ReceivableItem[]>(`${this.baseUrl}/receivables`)));
      this.receivablesState.set('ready');
    } catch {
      this.receivablesState.set('error');
    }
  }

  async loadPayables(): Promise<void> {
    this.payablesState.set('loading');
    try {
      this.payableItems.set(await firstValueFrom(this.http.get<PayableItem[]>(`${this.baseUrl}/payables`)));
      this.payablesState.set('ready');
    } catch {
      this.payablesState.set('error');
    }
  }

  /** Hoje e os próximos 6 dias, dia a dia. */
  async loadCashFlow(): Promise<void> {
    this.cashFlowStatus.set('loading');
    const from = new Date();
    const to = new Date();
    to.setDate(to.getDate() + CASH_FLOW_DAYS - 1);
    try {
      this.cashFlowState.set(await firstValueFrom(this.http.get<CashFlow>(`${this.baseUrl}/cash-flow`, {
        params: { granularity: 'DAILY', from: isoDate(from), to: isoDate(to) },
      })));
      this.cashFlowStatus.set('ready');
    } catch {
      this.cashFlowStatus.set('error');
    }
  }

  /** POST /api/finance/receivables/{id}/settle — baixa integral do que falta. Rejeita se o servidor recusa. */
  async settleReceivable(id: string, amount: number): Promise<void> {
    await firstValueFrom(this.http.post(`${this.baseUrl}/receivables/${id}/settle`, { amount, partial: false }));
    await this.loadReceivables();
  }
}
