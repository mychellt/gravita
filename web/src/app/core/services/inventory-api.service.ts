import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';

/** O único depósito implícito do servidor (`DefaultWarehouse.ID`); os comandos de escrita exigem o id. */
export const DEFAULT_WAREHOUSE_ID = '00000000-0000-0000-0000-000000000001';

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
  allowNegativeStock: boolean;
}

/** Saldo agregado de um produto, como devolvido por GET /api/inventory/products/{id}/balance. */
export interface ProductBalance {
  productId: string;
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

export type ReservationStatus = 'ACTIVE' | 'CONSUMED' | 'RELEASED';

/** Reserva de estoque, como devolvida por GET /api/inventory/reservations (StockReservationItemResponse). */
export interface StockReservationItem {
  id: string;
  orderRef: string;
  productId: string;
  productCode: string | null;
  warehouseId: string;
  quantity: number;
  status: ReservationStatus;
}

export type CountScope = 'PARTIAL_BY_GROUP' | 'TOTAL';
export type CountStatus = 'IN_PROGRESS' | 'PENDING_APPROVAL' | 'APPROVED';

/** Linha de contagem; `countedQuantity` e `divergence` ficam nulos até a linha ser contada. */
export interface PhysicalCountLine {
  productId: string;
  productCode: string | null;
  systemQuantity: number;
  countedQuantity: number | null;
  divergence: number | null;
}

/** Contagem física com suas linhas, como devolvida por GET /api/inventory/counts (PhysicalCountItemResponse). */
export interface PhysicalCountItem {
  id: string;
  scope: CountScope;
  productGroupId: string | null;
  warehouseId: string;
  status: CountStatus;
  startedBy: string;
  /** Instante ISO-8601. */
  startedAt: string;
  lines: PhysicalCountLine[];
}

export interface CountedLine {
  productId: string;
  countedQuantity: number;
}

export interface StockAdjustment {
  productId: string;
  /** Positivo acrescenta, negativo retira. */
  quantityDelta: number;
  justification: string;
}

export interface MovementFilter {
  productId?: string;
  type?: StockMovementType;
  /** Instantes ISO-8601, ambos inclusivos. */
  from?: string;
  to?: string;
}

/** O usuário logado não pôde ser resolvido para o id que as escritas exigem; a mensagem é para o usuário. */
export class InventoryActorError extends Error {}

/** Mensagem do backend para uma falha (`{message}`), se houver. */
export function inventoryFailureDetail(error: unknown): string {
  if (error instanceof InventoryActorError) return error.message;
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
  private readonly auth = inject(AuthService);
  private readonly baseUrl = `${environment.apiUrl}/inventory`;
  private actor: { email: string; id: Promise<string> } | null = null;

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

  /** GET /api/inventory/products/{id}/balance — saldo do produto no depósito padrão (zerado se nunca movimentado). */
  productBalance(productId: string): Promise<ProductBalance> {
    return firstValueFrom(this.http.get<ProductBalance>(`${this.baseUrl}/products/${productId}/balance`));
  }

  /** PATCH /api/inventory/products/{id}/allow-negative-stock — devolve o valor persistido. */
  async setAllowNegativeStock(productId: string, allowNegativeStock: boolean): Promise<boolean> {
    const updated = await firstValueFrom(this.http.patch<{ allowNegativeStock: boolean }>(
      `${this.baseUrl}/products/${productId}/allow-negative-stock`, { allowNegativeStock }));
    return updated.allowNegativeStock;
  }

  units(kind: StockUnitKind, page: number, size: number, productId?: string): Promise<Page<StockUnit>> {
    let params = new HttpParams().set('kind', kind).set('page', page).set('size', size);
    if (productId) params = params.set('productId', productId);
    return firstValueFrom(this.http.get<Page<StockUnit>>(`${this.baseUrl}/lots`, { params }));
  }

  reservations(status: ReservationStatus | null, page: number, size: number): Promise<Page<StockReservationItem>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    return firstValueFrom(this.http.get<Page<StockReservationItem>>(`${this.baseUrl}/reservations`, { params }));
  }

  /** DELETE /api/inventory/reservations/{id}: o servidor marca a reserva como liberada e devolve o saldo. */
  releaseReservation(id: string): Promise<void> {
    return firstValueFrom(this.http.delete<void>(`${this.baseUrl}/reservations/${id}`));
  }

  /** Mais recentes primeiro (ordem do servidor), com as linhas de cada contagem. */
  counts(page: number, size: number): Promise<Page<PhysicalCountItem>> {
    return firstValueFrom(this.http.get<Page<PhysicalCountItem>>(`${this.baseUrl}/counts`, {
      params: new HttpParams().set('page', page).set('size', size),
    }));
  }

  /** POST /api/inventory/counts; devolve o id da contagem aberta. `productGroupId` só vale para a parcial. */
  async startCount(scope: CountScope, productGroupId: string | null): Promise<string> {
    const user = await this.currentUserId();
    const created = await firstValueFrom(this.http.post<{ id: string }>(`${this.baseUrl}/counts`, {
      scope, productGroupId: scope === 'PARTIAL_BY_GROUP' ? productGroupId : null, warehouseId: DEFAULT_WAREHOUSE_ID, user,
    }));
    return created.id;
  }

  /** POST /api/inventory/counts/{id}/submit — acumulativo: linhas não enviadas mantêm a contagem anterior. */
  async submitCount(id: string, countedLines: CountedLine[]): Promise<void> {
    const submittedBy = await this.currentUserId();
    await firstValueFrom(this.http.post(`${this.baseUrl}/counts/${id}/submit`, { countedLines, submittedBy }));
  }

  /** POST /api/inventory/counts/{id}/approve — o servidor gera um ajuste por linha divergente. */
  async approveCount(id: string): Promise<void> {
    const approvedBy = await this.currentUserId();
    await firstValueFrom(this.http.post(`${this.baseUrl}/counts/${id}/approve`, { approvedBy }));
  }

  /** POST /api/inventory/adjustments no depósito padrão; devolve o movimento gerado. */
  async adjust(adjustment: StockAdjustment): Promise<StockMovementItem> {
    const user = await this.currentUserId();
    return firstValueFrom(this.http.post<StockMovementItem>(`${this.baseUrl}/adjustments`, {
      ...adjustment, justification: adjustment.justification.trim(), warehouseId: DEFAULT_WAREHOUSE_ID, user,
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

  /**
   * As escritas do estoque pedem o id do usuário, que `/api/auth/me` ainda não devolve: acha-se o usuário logado
   * pelo e-mail em GET /api/users. O resultado fica em memória por e-mail; uma falha não é guardada.
   */
  private currentUserId(): Promise<string> {
    const email = this.auth.currentUser()?.email?.trim().toLowerCase();
    if (!email) return Promise.reject(new InventoryActorError('Não foi possível identificar o usuário logado.'));
    if (this.actor?.email !== email) {
      const id = firstValueFrom(this.http.get<{ id: string; email: string }[]>(`${environment.apiUrl}/users`))
        .then(users => {
          const self = users.find(user => user.email.trim().toLowerCase() === email);
          if (!self) throw new InventoryActorError('Não foi possível identificar o usuário logado.');
          return self.id;
        });
      this.actor = { email, id };
      id.catch(() => { if (this.actor?.id === id) this.actor = null; });
    }
    return this.actor.id;
  }
}
