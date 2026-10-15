import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';

/** Página devolvida pelas listagens de estoque (PageResponse). `page` começa em 0. */
export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/** Saldo do produto no depósito padrão, como devolvido por GET /api/inventory/balances (StockBalanceItemResponse). */
export interface StockBalanceItem {
  productId: string;
  productCode: string | null;
  warehouseId: string;
  onHand: number;
  reserved: number;
  inTransit: number;
  available: number;
  averageCost: number;
}

export type StockMovementType = 'ENTRY' | 'EXIT' | 'ADJUSTMENT' | 'TRANSFER';

/** Movimentação, como devolvida por GET /api/inventory/movements (StockMovementItemResponse). */
export interface StockMovementItem {
  id: string;
  type: StockMovementType;
  productId: string;
  productCode: string | null;
  warehouseId: string;
  /** Saídas vêm em módulo; só o ajuste traz sinal. */
  quantity: number;
  unitCost: number | null;
  lotCode: string | null;
  serialNumbers: string[] | null;
  originReference: string | null;
  justification: string | null;
  user: string | null;
  /** Instante ISO-8601. */
  timestamp: string;
}

export type StockUnitKind = 'LOT' | 'SERIAL';
export type SerialStatus = 'IN_STOCK' | 'ISSUED';

/** Lote (com validade e quantidade) ou número de série (com status), como devolvido por GET /api/inventory/lots. */
export interface StockUnit {
  kind: StockUnitKind;
  id: string;
  productId: string;
  productCode: string | null;
  warehouseId: string;
  code: string;
  /** `yyyy-MM-dd`; só lotes têm validade. */
  expiryDate: string | null;
  quantity: number;
  status: SerialStatus | null;
}

/** Lote a vencer, como devolvido por GET /api/inventory/alerts/expiring-lots. */
export interface ExpiringLot {
  productId: string;
  warehouseId: string;
  lotCode: string;
  expiryDate: string;
  remainingQuantity: number;
}

/** Sugestão de reposição, como devolvida por GET /api/inventory/alerts/low-stock. */
export interface ReorderSuggestion {
  productId: string;
  warehouseId: string;
  available: number;
  reorderPoint: number;
  suggestedQuantity: number;
}

export interface MovementFilter {
  productId?: string;
  type?: StockMovementType;
  /** Instantes ISO-8601, ambos inclusivos. */
  from?: string;
  to?: string;
}

/** Mensagem do backend para uma falha (`{message}`), se houver. */
export function inventoryFailureDetail(error: unknown): string {
  if (!(error instanceof HttpErrorResponse)) return '';
  const message = error.error?.message;
  return typeof message === 'string' ? message.trim() : '';
}

/** O backend só devolve o código do produto (e nas listas de alerta, nem isso): sem código, usa o início do id. */
export function productLabel(productId: string, productCode?: string | null): string {
  return productCode?.trim() || productId.slice(0, 8);
}

/**
 * Cliente HTTP do estoque (`/api/inventory/**`). Há um único depósito implícito no servidor, então nenhuma chamada
 * envia `warehouseId`. Cada método rejeita se o servidor falha; quem chama decide como mostrar o erro.
 */
@Injectable({ providedIn: 'root' })
export class InventoryApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/inventory`;

  balances(page: number, size: number): Promise<Page<StockBalanceItem>> {
    return firstValueFrom(this.http.get<Page<StockBalanceItem>>(`${this.baseUrl}/balances`, {
      params: new HttpParams().set('page', page).set('size', size),
    }));
  }

  /** Mais recentes primeiro (ordem do servidor). Só os filtros preenchidos vão na query. */
  movements(filter: MovementFilter, page: number, size: number): Promise<Page<StockMovementItem>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (filter.productId) params = params.set('productId', filter.productId);
    if (filter.type) params = params.set('type', filter.type);
    if (filter.from) params = params.set('from', filter.from);
    if (filter.to) params = params.set('to', filter.to);
    return firstValueFrom(this.http.get<Page<StockMovementItem>>(`${this.baseUrl}/movements`, { params }));
  }

  units(kind: StockUnitKind, page: number, size: number): Promise<Page<StockUnit>> {
    return firstValueFrom(this.http.get<Page<StockUnit>>(`${this.baseUrl}/lots`, {
      params: new HttpParams().set('kind', kind).set('page', page).set('size', size),
    }));
  }

  expiringLots(withinDays: number): Promise<ExpiringLot[]> {
    return firstValueFrom(this.http.get<ExpiringLot[]>(`${this.baseUrl}/alerts/expiring-lots`, {
      params: new HttpParams().set('withinDays', withinDays),
    }));
  }

  lowStock(): Promise<ReorderSuggestion[]> {
    return firstValueFrom(this.http.get<ReorderSuggestion[]>(`${this.baseUrl}/alerts/low-stock`));
  }

  /** POST /api/purchasing/requests com origem `MIN_STOCK_TRIGGER`; devolve o id da requisição criada. */
  async createPurchaseRequest(productId: string, quantity: number): Promise<string> {
    const created = await firstValueFrom(this.http.post<{ id: string }>(`${environment.apiUrl}/purchasing/requests`, {
      origin: 'MIN_STOCK_TRIGGER',
      items: [{ productId, quantity }],
    }));
    return created.id;
  }
}
