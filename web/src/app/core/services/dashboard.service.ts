import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import type { LoadStatus } from './customer.service';

export interface DashboardPeriodComparison {
  current: number;
  previous: number;
  /** Nulo quando o período anterior não teve faturamento para comparar. */
  variationPercent: number | null;
}

export interface DashboardCriticalStockItem {
  productId: string;
  reason: 'BELOW_MINIMUM' | 'NEAR_EXPIRY';
}

/** Painel executivo, como devolvido por GET /api/reports/dashboard (ExecutiveDashboardView); só os campos que a tela usa. */
export interface ExecutiveDashboard {
  period: 'DAY' | 'WEEK' | 'MONTH';
  revenue: { day: DashboardPeriodComparison; week: DashboardPeriodComparison; month: DashboardPeriodComparison };
  margin: { grossMarginPercent: number | null };
  delinquency: { totalOverdue: number; titleCount: number };
  criticalStock: DashboardCriticalStockItem[];
  /** Faturamento dos últimos 7 dias, do mais antigo ao de hoje. */
  lastSevenDays: { date: string; value: number }[];
}

/** Indicadores do dashboard (GET /api/reports/dashboard). */
@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/reports/dashboard`;

  private readonly current = signal<ExecutiveDashboard | null>(null);
  private readonly loadState = signal<LoadStatus>('loading');

  readonly dashboard = this.current.asReadonly();
  readonly status = this.loadState.asReadonly();

  /** Recarrega o painel (do mês, da empresa informada); nunca rejeita, o resultado fica em `status`. */
  async load(companyId?: string | null): Promise<void> {
    this.loadState.set('loading');
    try {
      const params: Record<string, string> = { period: 'MONTH' };
      if (companyId) params['companyId'] = companyId;
      this.current.set(await firstValueFrom(this.http.get<ExecutiveDashboard>(this.baseUrl, { params })));
      this.loadState.set('ready');
    } catch {
      this.loadState.set('error');
    }
  }
}
