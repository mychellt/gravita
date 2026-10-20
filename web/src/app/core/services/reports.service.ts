import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';

/** DRE gerencial de um mês, como devolvido por GET /api/reports/dre (ManagerialDre). */
export interface ManagerialDre {
  period: string;
  grossRevenue: number;
  deductions: number;
  cmv: number;
  totalExpenses: number;
  netResult: number;
}

/** Um produto (ou cliente) na Curva ABC, de GET /api/reports/abc-curve; as participações são percentuais. */
export interface AbcCurveEntry {
  entityId: string;
  revenue: number;
  revenueShare: number;
  cumulativeShare: number;
  abcClass: 'A' | 'B' | 'C';
}

export type ReportFormat = 'pdf' | 'xlsx';

/** Relatórios gerenciais (/api/reports/*). Os métodos rejeitam em caso de falha; a tela decide como mostrá-la. */
@Injectable({ providedIn: 'root' })
export class ReportsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/reports`;

  /** `period` no formato yyyy-MM. */
  dre(period: string, companyId?: string | null): Promise<ManagerialDre> {
    return firstValueFrom(this.http.get<ManagerialDre>(`${this.baseUrl}/dre`, { params: this.params({ period }, companyId) }));
  }

  abcCurve(type: 'product' | 'customer', period: string, companyId?: string | null): Promise<AbcCurveEntry[]> {
    return firstValueFrom(this.http.get<AbcCurveEntry[]>(`${this.baseUrl}/abc-curve`, { params: this.params({ type, period }, companyId) }));
  }

  /** GET /api/reports/{slug}/export — o arquivo do relatório. `extra` leva os parâmetros próprios dele (ex.: type da Curva ABC). */
  export(slug: string, format: ReportFormat, period: string, extra: Record<string, string> = {}, companyId?: string | null): Promise<Blob> {
    return firstValueFrom(this.http.get(`${this.baseUrl}/${slug}/export`, {
      params: this.params({ format, period, ...extra }, companyId), responseType: 'blob',
    }));
  }

  private params(values: Record<string, string>, companyId?: string | null): Record<string, string> {
    return companyId ? { ...values, companyId } : values;
  }
}
