import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import type { LoadStatus } from './customer.service';

export type OpportunityStage = 'PROSPECTING' | 'PROPOSAL' | 'NEGOTIATION' | 'CLOSED' | 'LOST';

/** Oportunidade do funil, como devolvida por GET /api/crm/opportunities (OpportunityListItemResponse). */
export interface Opportunity {
  id: string;
  customerId: string;
  customerName: string | null;
  estimatedValue: number;
  probability: number;
  expectedCloseDate: string;
  owner: string;
  ownerName: string | null;
  stage: OpportunityStage;
}

/** Funil de vendas (/api/crm/opportunities). */
@Injectable({ providedIn: 'root' })
export class OpportunitiesService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/crm/opportunities`;

  private readonly items = signal<Opportunity[]>([]);
  private readonly loadState = signal<LoadStatus>('loading');

  readonly opportunities = this.items.asReadonly();
  readonly status = this.loadState.asReadonly();

  /** Recarrega o funil; nunca rejeita, o resultado fica em `status`. */
  async load(): Promise<void> {
    this.loadState.set('loading');
    try {
      this.items.set(await firstValueFrom(this.http.get<Opportunity[]>(this.baseUrl)));
      this.loadState.set('ready');
    } catch {
      this.loadState.set('error');
    }
  }
}
