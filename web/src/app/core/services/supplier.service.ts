import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';

/** Endereço, como POST /api/suppliers (RegisterSupplierRequest.AddressRequest) e GET /api/lookup/* o trocam. */
export interface SupplierAddress {
  street: string;
  number?: string | null;
  complement?: string | null;
  neighborhood: string;
  city: string;
  state: string;
  zipCode: string;
}

/** Corpo de POST /api/suppliers (RegisterSupplierRequest, só o que a conferência da NF-e envia). */
export interface RegisterSupplierRequest {
  document: string;
  name: string;
  addresses: SupplierAddress[];
}

/** Fornecedor já cadastrado, como GET /api/suppliers/by-document/{cnpj} devolve (SupplierSummaryResponse). */
export interface SupplierSummary {
  id: string;
  name: string;
  document: string;
}

/** GET /api/lookup/cnpj/{cnpj} e /cep/{cep} (PersonLookupResponse); campos nulos quando nada foi encontrado. */
export interface PersonLookup {
  name: string | null;
  address: Partial<SupplierAddress> | null;
}

/** Cadastro de fornecedores (UC-M1-09) e as consultas de CNPJ/CEP que o pré-preenchem. */
@Injectable({ providedIn: 'root' })
export class SupplierService {
  private readonly http = inject(HttpClient);

  /** POST /api/suppliers. Rejeita se o backend recusa (400/409 com `{message}`; ver `nfeFailureDetail`). */
  register(request: RegisterSupplierRequest): Promise<{ id: string }> {
    return firstValueFrom(this.http.post<{ id: string }>(`${environment.apiUrl}/suppliers`, request));
  }

  /** O fornecedor cadastrado com este CNPJ, ou `null` se não há (404) ou a consulta falha; só ajuda a preencher formulários. */
  async findByDocument(cnpj: string): Promise<SupplierSummary | null> {
    try {
      return await firstValueFrom(this.http.get<SupplierSummary>(`${environment.apiUrl}/suppliers/by-document/${cnpj.replace(/\D/g, '')}`));
    } catch {
      return null;
    }
  }

  /** Só ajuda a preencher o formulário: qualquer falha da consulta vira `null`, nunca um erro. */
  lookupCnpj(cnpj: string): Promise<PersonLookup | null> {
    return this.lookup(`${environment.apiUrl}/lookup/cnpj/${cnpj.replace(/\D/g, '')}`);
  }

  lookupCep(cep: string): Promise<PersonLookup | null> {
    return this.lookup(`${environment.apiUrl}/lookup/cep/${cep.replace(/\D/g, '')}`);
  }

  private async lookup(url: string): Promise<PersonLookup | null> {
    try {
      return await firstValueFrom(this.http.get<PersonLookup>(url));
    } catch {
      return null;
    }
  }
}
