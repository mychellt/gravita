import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import type { LoadStatus } from './customer.service';

/** Números do cabeçalho do estoque, de GET /api/inventory/summary (InventorySummary). */
export interface InventorySummary {
  /** Produtos ativos (SKUs). */
  activeSkus: number;
  /** Produtos ativos com saldo disponível, somando os depósitos, abaixo do mínimo cadastrado. */
  belowMinimum: number;
}

/** Totais do cabeçalho da tela de estoque (GET /api/inventory/summary). */
@Injectable({ providedIn: 'root' })
export class InventorySummaryService {
  private readonly http = inject(HttpClient);

  private readonly current = signal<InventorySummary | null>(null);
  private readonly loadState = signal<LoadStatus>('loading');

  readonly summary = this.current.asReadonly();
  readonly status = this.loadState.asReadonly();

  /** Recarrega os totais; nunca rejeita, o resultado fica em `status`. */
  async load(): Promise<void> {
    this.loadState.set('loading');
    try {
      this.current.set(await firstValueFrom(this.http.get<InventorySummary>(`${environment.apiUrl}/inventory/summary`)));
      this.loadState.set('ready');
    } catch {
      this.loadState.set('error');
    }
  }
}
