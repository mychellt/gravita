import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import type { LoadStatus } from './customer.service';

export type PurchaseOrderStatus = 'OPEN' | 'PARTIALLY_RECEIVED' | 'CLOSED' | 'CANCELLED';

/** Pedido de compra, como devolvido por GET /api/purchasing/orders (PurchaseOrderListItemResponse). */
export interface PurchaseOrderItem {
  id: string;
  supplierId: string;
  supplierName: string | null;
  status: PurchaseOrderStatus;
  /** Aguardando aprovação enquanto verdadeiro. */
  approvalRequired: boolean;
  approvedBy: string | null;
  createdAt: string;
  totalValue: number;
  items: { productId: string; quantity: number; unitPrice: number }[];
}

/** Pedidos de compra (/api/purchasing/orders). */
@Injectable({ providedIn: 'root' })
export class PurchasingService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/purchasing/orders`;

  private readonly items = signal<PurchaseOrderItem[]>([]);
  private readonly loadState = signal<LoadStatus>('loading');

  readonly orders = this.items.asReadonly();
  readonly status = this.loadState.asReadonly();

  /** Recarrega os pedidos; nunca rejeita, o resultado fica em `status`. */
  async load(): Promise<void> {
    this.loadState.set('loading');
    try {
      this.items.set(await firstValueFrom(this.http.get<PurchaseOrderItem[]>(this.baseUrl)));
      this.loadState.set('ready');
    } catch {
      this.loadState.set('error');
    }
  }

  /** POST /api/purchasing/orders/{id}/approve — o aprovador é o usuário logado. Rejeita se o servidor recusa. */
  async decide(id: string, decision: 'APPROVE' | 'REJECT'): Promise<void> {
    await firstValueFrom(this.http.post(`${this.baseUrl}/${id}/approve`, { decision }));
    await this.load();
  }

  /** POST /api/purchasing/orders/{id}/receipts — abre o recebimento de todos os itens do pedido, a conferir. */
  async openReceipt(order: PurchaseOrderItem): Promise<void> {
    await firstValueFrom(this.http.post(`${this.baseUrl}/${order.id}/receipts`, {
      receivedItems: order.items.map(i => ({ productId: i.productId, receivedQty: i.quantity })),
    }));
    await this.load();
  }
}
