import { Injectable, computed, inject, signal } from '@angular/core';
import { LoadStatus } from './customer.service';
import { ExpiringLot, InventoryApiService, ReorderSuggestion, inventoryFailureDetail } from './inventory-api.service';

/** Pedido de compra de uma sugestão: `requested` só depois da resposta do servidor. */
export type PurchaseRequestState = 'idle' | 'sending' | 'requested';

export interface LowStockRow extends ReorderSuggestion {
  request: PurchaseRequestState;
}

export type Outcome = { ok: true } | { ok: false; error: string };

export const DEFAULT_EXPIRY_WINDOW_DAYS = 7;

const MS_PER_DAY = 86_400_000;
const FAILED_REQUEST = 'Não foi possível criar a requisição de compra.';

/** Dias de hoje (00:00 local) até a data `yyyy-MM-dd`; negativo se já passou. */
export function daysUntil(isoDate: string, now: Date = new Date()): number {
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
  return Math.round((new Date(`${isoDate}T00:00:00`).getTime() - today) / MS_PER_DAY);
}

/** Chave que identifica um lote tanto na listagem de lotes quanto no alerta de vencimento. */
export function lotKey(productId: string, lotCode: string): string {
  return `${productId}|${lotCode}`;
}

/**
 * Alertas de estoque vindos do backend, compartilhados pelo painel de alertas, pela aba Lotes, pela aba Reposição e
 * pelos contadores do cabeçalho — assim as telas nunca divergem entre si.
 */
@Injectable({ providedIn: 'root' })
export class InventoryAlertsService {
  private readonly api = inject(InventoryApiService);
  private expiringLoads = 0;
  private lowStockLoads = 0;

  readonly expiryWindowDays = signal(DEFAULT_EXPIRY_WINDOW_DAYS);

  private readonly expiring = signal<ExpiringLot[]>([]);
  private readonly expiringState = signal<LoadStatus>('loading');
  private readonly lowStockRows = signal<LowStockRow[]>([]);
  private readonly lowStockState = signal<LoadStatus>('loading');

  readonly expiringLots = this.expiring.asReadonly();
  readonly expiringStatus = this.expiringState.asReadonly();
  readonly lowStock = this.lowStockRows.asReadonly();
  readonly lowStockStatus = this.lowStockState.asReadonly();

  /** Sugestões ainda sem requisição e com quantidade a pedir. */
  readonly pendingRequests = computed(() =>
    this.lowStockRows().filter(row => row.request === 'idle' && row.suggestedQuantity > 0));

  /** Recarrega os dois alertas; nunca rejeita — o resultado fica nos status. */
  loadAll(): Promise<void> {
    return Promise.all([this.loadExpiring(), this.loadLowStock()]).then(() => undefined);
  }

  async loadExpiring(): Promise<void> {
    const load = ++this.expiringLoads;
    this.expiringState.set('loading');
    try {
      const lots = await this.api.expiringLots(this.expiryWindowDays());
      if (load !== this.expiringLoads) return;
      this.expiring.set(lots);
      this.expiringState.set('ready');
    } catch {
      if (load === this.expiringLoads) this.expiringState.set('error');
    }
  }

  async loadLowStock(): Promise<void> {
    const load = ++this.lowStockLoads;
    this.lowStockState.set('loading');
    try {
      const suggestions = await this.api.lowStock();
      if (load !== this.lowStockLoads) return;
      this.lowStockRows.set(suggestions.map(s => ({ ...s, request: 'idle' })));
      this.lowStockState.set('ready');
    } catch {
      if (load === this.lowStockLoads) this.lowStockState.set('error');
    }
  }

  changeExpiryWindow(days: number): Promise<void> {
    this.expiryWindowDays.set(days);
    return this.loadExpiring();
  }

  /** POST /api/purchasing/requests; a linha só passa a "requisitada" quando o servidor responde com sucesso. */
  async requestPurchase(productId: string): Promise<Outcome> {
    const row = this.lowStockRows().find(r => r.productId === productId);
    if (!row || row.request !== 'idle') return { ok: false, error: 'Já existe uma requisição para este item.' };
    if (row.suggestedQuantity <= 0) return { ok: false, error: 'Não há quantidade a pedir para este item.' };

    this.setRequestState(productId, 'sending');
    try {
      await this.api.createPurchaseRequest(productId, row.suggestedQuantity);
      this.setRequestState(productId, 'requested');
      return { ok: true };
    } catch (error) {
      this.setRequestState(productId, 'idle');
      return { ok: false, error: inventoryFailureDetail(error) || FAILED_REQUEST };
    }
  }

  /** Pede todas as pendentes; devolve quantas foram criadas e quantas falharam. */
  async requestAll(): Promise<{ created: number; failed: number }> {
    const outcomes = await Promise.all(this.pendingRequests().map(row => this.requestPurchase(row.productId)));
    const created = outcomes.filter(outcome => outcome.ok).length;
    return { created, failed: outcomes.length - created };
  }

  private setRequestState(productId: string, request: PurchaseRequestState): void {
    this.lowStockRows.update(rows => rows.map(row => row.productId === productId ? { ...row, request } : row));
  }
}
