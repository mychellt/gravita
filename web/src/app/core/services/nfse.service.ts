import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import type { LoadStatus } from './customer.service';

export type NfseStatus = 'RPS' | 'DRAFT' | 'SENT' | 'AUTHORIZED' | 'CANCELLED';

/** RPS/NFS-e da empresa, como devolvida por GET /api/nfse (NfseListItemResponse). */
export interface NfseListItem {
  id: string;
  status: NfseStatus;
  /** Série/número da NFS-e; nulos enquanto o documento ainda é um RPS. */
  series: string | null;
  number: number | null;
  rpsSeries: string;
  rpsNumber: number;
  tomadorName: string;
  tomadorDocument: string;
  municipalityIbgeCode: string;
  /** Nome e UF do município do ISS; nulos se o código IBGE não está cadastrado. */
  municipalityName: string | null;
  municipalityState: string | null;
  serviceCode: string;
  discrimination: string;
  serviceAmount: number;
  issRate: number;
  issAmount: number;
  withheldAmount: number;
  /** Autorização da prefeitura, ou a criação do RPS enquanto não autorizada. */
  issuedAt: string;
}

/** RPS/NFS-e emitidos pela empresa (GET /api/nfse). */
@Injectable({ providedIn: 'root' })
export class NfseService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/nfse`;

  private readonly items = signal<NfseListItem[]>([]);
  private readonly loadState = signal<LoadStatus>('loading');

  readonly nfses = this.items.asReadonly();
  readonly status = this.loadState.asReadonly();

  /** Recarrega a lista; nunca rejeita, o resultado fica em `status`. */
  async load(): Promise<void> {
    this.loadState.set('loading');
    try {
      this.items.set(await firstValueFrom(this.http.get<NfseListItem[]>(this.baseUrl)));
      this.loadState.set('ready');
    } catch {
      this.loadState.set('error');
    }
  }
}
