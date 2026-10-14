import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import type { LoadStatus } from './customer.service';

/** Ciclo de vida de uma NF-e emitida (enum `NfeDocumentStatus` do backend). */
export type NfeDocumentStatus = 'DRAFT' | 'QUEUED' | 'SENT' | 'AUTHORIZED' | 'REJECTED' | 'CANCELLED' | 'VOIDED';

/** Situação de uma NF-e de entrada (enum `InboundNfeStatus` do backend). */
export type InboundNfeStatus = 'PENDING_CONFERENCE' | 'CONFIRMED';

/** NF-e emitida, como devolvida por GET /api/nfe (NfeListItemResponse). */
export interface NfeListItem {
  id: string;
  documentNumber: number | null;
  documentSeries: string | null;
  recipientName: string;
  cfop: string;
  /** Instante da autorização; enquanto não autorizada, o da criação. */
  emittedAt: string;
  totalValue: number;
  icmsValue: number;
  status: NfeDocumentStatus;
  cancellationJustification: string | null;
  cancelledAt: string | null;
}

/** NF-e de entrada, como devolvida por GET /api/nfe/inbound (InboundNfeListItemResponse). */
export interface InboundNfeListItem {
  id: string;
  series: string;
  number: string;
  supplierName: string;
  issuedAt: string;
  totalValue: number;
  status: InboundNfeStatus;
}

/** Corpo de resposta de POST /api/nfe/{id}/cancel (CancelNfeResponse). */
interface CancelNfeResult {
  id: string;
  status: NfeDocumentStatus;
  cancellationJustification: string | null;
  cancelledAt: string | null;
}

/** Mensagem do backend (`{message}`) para uma falha, inclusive quando o corpo veio como Blob (downloads). */
export async function nfeFailureDetail(error: unknown): Promise<string> {
  if (!(error instanceof HttpErrorResponse)) return '';
  let body: unknown = error.error;
  if (body instanceof Blob) {
    try {
      body = JSON.parse(await body.text());
    } catch {
      return '';
    }
  }
  const message = (body as { message?: unknown } | null)?.message;
  return typeof message === 'string' ? message.trim() : '';
}

/** NF-e emitidas e de entrada da empresa (GET /api/nfe, GET /api/nfe/inbound) e as ações sobre uma NF-e emitida. */
@Injectable({ providedIn: 'root' })
export class NfeService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/nfe`;

  private readonly issued = signal<NfeListItem[]>([]);
  private readonly issuedStatus = signal<LoadStatus>('loading');
  private readonly inbound = signal<InboundNfeListItem[]>([]);
  private readonly inboundStatus = signal<LoadStatus>('loading');

  readonly nfes = this.issued.asReadonly();
  readonly status = this.issuedStatus.asReadonly();
  readonly inboundNfes = this.inbound.asReadonly();
  readonly inboundLoadStatus = this.inboundStatus.asReadonly();

  /** Recarrega as duas listas, cada uma com o seu status; nunca rejeita. */
  async load(): Promise<void> {
    await Promise.all([this.loadIssued(), this.loadInbound()]);
  }

  private async loadIssued(): Promise<void> {
    this.issuedStatus.set('loading');
    try {
      this.issued.set(await firstValueFrom(this.http.get<NfeListItem[]>(this.baseUrl)));
      this.issuedStatus.set('ready');
    } catch {
      this.issuedStatus.set('error');
    }
  }

  private async loadInbound(): Promise<void> {
    this.inboundStatus.set('loading');
    try {
      this.inbound.set(await firstValueFrom(this.http.get<InboundNfeListItem[]>(`${this.baseUrl}/inbound`)));
      this.inboundStatus.set('ready');
    } catch {
      this.inboundStatus.set('error');
    }
  }

  /** POST /api/nfe/{id}/cancel — a linha cancelada passa a refletir a resposta, sem recarregar a lista. Rejeita se o backend recusa. */
  async cancel(id: string, justification: string): Promise<void> {
    const result = await firstValueFrom(this.http.post<CancelNfeResult>(`${this.baseUrl}/${id}/cancel`, { justification }));
    this.issued.update(list =>
      list.map(n => n.id === id
        ? { ...n, status: result.status, cancellationJustification: result.cancellationJustification, cancelledAt: result.cancelledAt }
        : n));
  }

  /** GET /api/nfe/{id}/danfe — o PDF (retrato). */
  danfe(id: string): Promise<Blob> {
    return firstValueFrom(this.http.get(`${this.baseUrl}/${id}/danfe`, { params: { orientation: 'PORTRAIT' }, responseType: 'blob' }));
  }

  /** GET /api/nfe/{id}/xml — o XML autorizado. */
  xml(id: string): Promise<Blob> {
    return firstValueFrom(this.http.get(`${this.baseUrl}/${id}/xml`, { responseType: 'blob' }));
  }
}
