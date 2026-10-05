import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';

export type TaxRegime = 'SIMPLES_NACIONAL' | 'LUCRO_PRESUMIDO' | 'LUCRO_REAL';

/** Cadastro da empresa, como devolvido por GET/PATCH /api/companies/{id}. */
export interface Company {
  id: string;
  name: string;
  cnpj: string;
  ie: string;
  im: string;
  cnae: string;
  taxRegime: TaxRegime;
  simplesOptante: boolean;
  address: string;
  state: string;
  issuingEmail: string;
  phone: string;
  logoUrl: string | null;
}

/** Corpo do PATCH: o backend exige todos os campos do cadastro; o CNPJ não muda e por isso não é enviado. */
export type CompanyUpdate = Omit<Company, 'id' | 'cnpj'>;

/** Mensagem do backend para uma falha (`{message}` em regras de negócio, texto puro em outros casos), se houver. */
export function failureDetail(error: unknown): string {
  if (!(error instanceof HttpErrorResponse)) return '';
  const body = error.error;
  if (typeof body === 'string') return body.trim();
  return typeof body?.message === 'string' ? body.message.trim() : '';
}

@Injectable({ providedIn: 'root' })
export class CompanyService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/companies`;

  get(id: string): Promise<Company> {
    return firstValueFrom(this.http.get<Company>(`${this.baseUrl}/${id}`));
  }

  update(id: string, changes: CompanyUpdate): Promise<Company> {
    return firstValueFrom(this.http.patch<Company>(`${this.baseUrl}/${id}`, changes));
  }
}
