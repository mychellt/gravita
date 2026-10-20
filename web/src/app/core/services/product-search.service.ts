import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';

/** Produto encontrado para venda, como devolvido por GET /api/pdv/products/search (ProductSearchResultResponse). */
export interface ProductSearchResult {
  productId: string;
  description: string;
  ncm: string | null;
  /** Preço já resolvido para o cliente informado (tabela de preço dele, senão o preço-base). */
  unitPrice: number;
  availableForSale: boolean;
  /** Chave semântica do ícone (ProductIcon); ausente em backends antigos. */
  icon?: string;
}

/** Busca de produtos por código/código de barras, com o preço do cliente (GET /api/pdv/products/search). */
@Injectable({ providedIn: 'root' })
export class ProductSearchService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/pdv/products`;

  /** Rejeita se o servidor falha; sem resultado, devolve lista vazia. */
  search(query: string, customerId?: string): Promise<ProductSearchResult[]> {
    const params: Record<string, string> = { q: query };
    if (customerId) params['customerId'] = customerId;
    return firstValueFrom(this.http.get<ProductSearchResult[]>(`${this.baseUrl}/search`, { params }));
  }
}
