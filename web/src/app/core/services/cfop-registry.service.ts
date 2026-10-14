import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import type { LoadStatus } from './customer.service';
import type { NaturezaOperacao } from './nfe.service';

/** Linha do registro de CFOP, como devolvida por GET /api/auxiliary/cfop-registry (CfopRegistryEntryResponse). */
export interface CfopRegistryEntry {
  naturezaOperacao: NaturezaOperacao;
  cfop: string;
  description: string;
  /** Devolução e nota complementar só emitem referenciando a chave de acesso da nota original. */
  requiresReferencedAccessKey: boolean;
}

/** Registro de CFOP por natureza da operação (GET /api/auxiliary/cfop-registry) — somente leitura. */
@Injectable({ providedIn: 'root' })
export class CfopRegistryService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/auxiliary/cfop-registry`;

  private readonly registry = signal<CfopRegistryEntry[]>([]);
  private readonly loadStatus = signal<LoadStatus>('loading');

  readonly entries = this.registry.asReadonly();
  readonly status = this.loadStatus.asReadonly();

  /** Nunca rejeita: uma falha vira `status() === 'error'`. */
  async load(): Promise<void> {
    this.loadStatus.set('loading');
    try {
      this.registry.set(await firstValueFrom(this.http.get<CfopRegistryEntry[]>(this.baseUrl)));
      this.loadStatus.set('ready');
    } catch {
      this.loadStatus.set('error');
    }
  }
}
