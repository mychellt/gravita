import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import type { LoadStatus } from './customer.service';

/** Código da modalidade: `CIF`/`FOB` são o enum `TransportModality` do backend; `NONE` = "Sem transporte" (nada é enviado). */
export type TransportModalityCode = 'CIF' | 'FOB' | 'NONE';

/** Linha do cadastro de modalidades, como devolvida por GET /api/auxiliary/transport-modalities (TransportModalityResponse). */
export interface TransportModalityOption {
  id: string;
  name: string;
  code: TransportModalityCode;
  active: boolean;
}

/** Corpo do POST/PATCH. `active` omitido: o cadastro nasce ativo e a edição mantém o valor gravado. */
export interface TransportModalityInput {
  name: string;
  code: TransportModalityCode;
  active?: boolean;
}

/** Mensagem do backend para uma falha (texto simples ou `{message}`), se houver. */
export function transportModalityFailureDetail(error: unknown): string {
  if (!(error instanceof HttpErrorResponse)) return '';
  const body = error.error;
  if (typeof body === 'string') return body.trim();
  return typeof body?.message === 'string' ? body.message.trim() : '';
}

/**
 * Cadastro de modalidades de transporte (/api/auxiliary/transport-modalities).
 * `modalities` = só as ativas (as opções do select de Transporte da NF-e); `managed` = todas, para a tela de cadastro.
 */
@Injectable({ providedIn: 'root' })
export class TransportModalityService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/auxiliary/transport-modalities`;

  private readonly activeList = signal<TransportModalityOption[]>([]);
  private readonly activeStatus = signal<LoadStatus>('loading');
  private readonly allList = signal<TransportModalityOption[]>([]);
  private readonly allStatus = signal<LoadStatus>('loading');

  readonly modalities = this.activeList.asReadonly();
  readonly status = this.activeStatus.asReadonly();
  readonly managed = this.allList.asReadonly();
  readonly managedStatus = this.allStatus.asReadonly();

  /** Nunca rejeita: uma falha vira `status() === 'error'`. */
  async load(): Promise<void> {
    this.activeStatus.set('loading');
    try {
      this.activeList.set(await firstValueFrom(this.http.get<TransportModalityOption[]>(this.baseUrl)));
      this.activeStatus.set('ready');
    } catch {
      this.activeStatus.set('error');
    }
  }

  /** Todas as modalidades, inclusive as inativas. Nunca rejeita: uma falha vira `managedStatus() === 'error'`. */
  async loadAll(): Promise<void> {
    this.allStatus.set('loading');
    try {
      this.allList.set(await firstValueFrom(this.http.get<TransportModalityOption[]>(this.baseUrl, { params: { includeInactive: true } })));
      this.allStatus.set('ready');
    } catch {
      this.allStatus.set('error');
    }
  }

  /** Rejeita com o erro HTTP (ex.: 409 quando já existe uma modalidade ativa com o mesmo código); a lista só é relida no sucesso. */
  async create(input: TransportModalityInput): Promise<void> {
    await firstValueFrom(this.http.post<TransportModalityOption>(this.baseUrl, input));
    await this.loadAll();
  }

  async update(id: string, input: TransportModalityInput): Promise<void> {
    await firstValueFrom(this.http.patch<TransportModalityOption>(`${this.baseUrl}/${id}`, input));
    await this.loadAll();
  }

  /** DELETE desativa (exclusão lógica): a linha continua na lista, inativa, e as NF-e já emitidas não mudam. */
  async deactivate(id: string): Promise<void> {
    await firstValueFrom(this.http.delete<void>(`${this.baseUrl}/${id}`));
    await this.loadAll();
  }
}
