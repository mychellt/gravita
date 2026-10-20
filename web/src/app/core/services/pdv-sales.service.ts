import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';

export type PaymentMethodType = 'CASH' | 'DEBIT_CARD' | 'CREDIT_CARD' | 'PIX' | 'BOLETO' | 'STORE_CREDIT' | 'VOUCHER';
export type NfceSaleStatus = 'DRAFT' | 'AUTHORIZED' | 'PENDING_SYNC' | 'CANCELLED' | 'VOIDED';

/** Venda do PDV, como aceita por POST /api/pdv/sales (RegisterNfceSaleRequest). */
export interface PdvSaleRequest {
  items: { productId: string; quantity: number; unitPrice: number; itemDiscount?: number }[];
  totalDiscount?: number;
  payments: { method: PaymentMethodType; amount: number }[];
  /** CPF na nota, só dígitos; omitido quando não informado. */
  customerCpf?: string;
}

/** Resposta de POST /api/pdv/sales: a venda foi gravada; `status` diz como ficou a NFC-e. */
export interface PdvSaleResult {
  id: string;
  status: NfceSaleStatus;
  accessKey: string | null;
  protocol: string | null;
  /** Presente quando a venda foi gravada mas a NFC-e não pôde ser emitida (status DRAFT). */
  issuanceError: string | null;
}

/** Vendas do PDV: abre (ou reaproveita) a sessão de caixa e grava a venda com a NFC-e (/api/pdv). */
@Injectable({ providedIn: 'root' })
export class PdvSalesService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/pdv`;

  private sessionId: string | null = null;

  /** Grava a venda e tenta emitir a NFC-e. Rejeita se a venda não foi gravada. */
  async register(sale: PdvSaleRequest): Promise<PdvSaleResult> {
    const sessionId = await this.ensureSession();
    try {
      return await firstValueFrom(this.http.post<PdvSaleResult>(`${this.baseUrl}/sales`, { sessionId, ...sale }));
    } catch (error) {
      this.sessionId = null; // a sessão pode ter sido encerrada; a próxima tentativa pede de novo
      throw error;
    }
  }

  /** POST /api/pdv/sessions/current — a sessão de caixa aberta da empresa do usuário, aberta sob demanda. */
  private async ensureSession(): Promise<string> {
    if (!this.sessionId) {
      this.sessionId = (await firstValueFrom(this.http.post<{ id: string }>(`${this.baseUrl}/sessions/current`, null))).id;
    }
    return this.sessionId;
  }
}
