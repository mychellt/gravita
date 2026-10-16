import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import type { PaymentMethodType } from './payment-method.service';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import type { LoadStatus } from './customer.service';

/** Ciclo de vida de uma NF-e emitida (enum `NfeDocumentStatus` do backend). */
export type NfeDocumentStatus = 'DRAFT' | 'QUEUED' | 'SENT' | 'AUTHORIZED' | 'REJECTED' | 'CANCELLED' | 'VOIDED';

/** Situação de uma NF-e de entrada (enum `InboundNfeStatus` do backend). */
export type InboundNfeStatus = 'PENDING_CONFERENCE' | 'CONFIRMED';

/** NF-e emitida, como devolvida por GET /api/nfe (NfeListItemResponse). */
export interface NfeListItem {
  id: string;
  documentNumber: number | null;
  documentSeries: string | null;
  recipientName: string;
  cfop: string;
  /** Instante da autorização; enquanto não autorizada, o da criação. */
  emittedAt: string;
  totalValue: number;
  icmsValue: number;
  status: NfeDocumentStatus;
  cancellationJustification: string | null;
  cancelledAt: string | null;
  /** Forma de pagamento escolhida na emissão (código de GET /api/auxiliary/payment-methods); nula quando não informada. */
  paymentMethodType: string | null;
}

/** NF-e de entrada, como devolvida por GET /api/nfe/inbound (InboundNfeListItemResponse). */
export interface InboundNfeListItem {
  id: string;
  series: string;
  number: string;
  supplierName: string;
  issuedAt: string;
  totalValue: number;
  status: InboundNfeStatus;
}

/** Corpo de resposta dos POST de /api/nfe/inbound (ImportSupplierNfeXmlResponse); não traz os itens, só a contagem. */
export interface InboundNfeSummary {
  id: string;
  accessKey: string;
  supplierName: string;
  itemCount: number;
  totalValue: number;
  status: InboundNfeStatus;
  /** GRA-246 AC1: se já existe um Fornecedor cadastrado para o CNPJ desta nota; não bloqueia o lançamento. */
  supplierResolved: boolean;
}

/** Um item da nota na tela de conferência, com a resolução fornecedor/produto já calculada pelo backend (GRA-246). */
export interface InboundNfeConferenceItem {
  supplierProductCode: string;
  description: string;
  ncm: string;
  unit: string;
  quantity: number;
  unitValue: number;
  /** Produto já vinculado a este código do fornecedor; nulo enquanto o item não é resolvido. */
  resolvedProductId: string | null;
  resolvedProductLabel: string | null;
  /** Preço da última compra registrada no vínculo; nulo numa primeira compra deste item. */
  lastPurchaseUnitValue: number | null;
}

/** Corpo de GET /api/nfe/inbound/{id}/conference (InboundNfeConferenceViewResponse) — fonte única dos itens da nota. */
export interface InboundNfeConferenceView {
  id: string;
  series: string;
  number: string;
  supplierName: string;
  supplierResolved: boolean;
  supplierId: string | null;
  items: InboundNfeConferenceItem[];
}

/** Corpo de POST .../items/{itemIndex}/link-new-product (RegisterProductRequest, só os campos usados por este fluxo). */
export interface RegisterProductFromItemRequest {
  internalCode: string;
  type: 'SIMPLE';
  ncm: string;
  origin: number;
  defaultCfopByOperation: Record<string, string>;
  cstCsosnByState: Record<string, string>;
  taxProfile: {
    icmsRate: number;
    ipiRate: number;
    pisRate: number;
    cofinsRate: number;
    icmsStRate: number;
    fcpRate: number;
  };
  averageCost: number;
  basePrice: number;
  purchaseUnit: string;
  saleUnit: string;
  conversionFactor: number;
}

/** Resposta de POST .../link-new-product (ProductResponse, só os campos usados pela tela). */
export interface RegisteredProduct {
  id: string;
  internalCode: string;
}

/** Item de uma NF-e de entrada lançada manualmente (EnterInboundNfeManuallyRequest.Item). */
export interface ManualInboundItem {
  supplierProductCode: string;
  description: string;
  ncm: string;
  cfop: string;
  unit: string;
  quantity: number;
  unitValue: number;
  totalValue: number;
  icmsValue: number;
  ipiValue: number;
  pisValue: number;
  cofinsValue: number;
}

/** Totais de uma NF-e de entrada lançada manualmente (EnterInboundNfeManuallyRequest.Totals). */
export interface ManualInboundTotals {
  productsValue: number;
  freightValue: number;
  insuranceValue: number;
  discountValue: number;
  otherExpensesValue: number;
  icmsValue: number;
  ipiValue: number;
  pisValue: number;
  cofinsValue: number;
  totalValue: number;
}

/** Corpo de POST /api/nfe/inbound (EnterInboundNfeManuallyRequest). A chave de acesso (44 dígitos) é obrigatória mesmo sem XML. */
export interface EnterInboundNfeRequest {
  companyId: string;
  accessKey: string;
  manualData: {
    accessKey: string;
    series: string;
    number: string;
    supplierCnpj: string;
    supplierName: string;
    issuedAt: string;
    items: ManualInboundItem[];
    totals: ManualInboundTotals;
  };
}

/**
 * Uma linha da conferência. `itemRef` é o UUID de um Produto do catálogo (o mesmo `productId` de /inventory),
 * não um id do item da nota; o backend casa a linha i com o item i da nota, por posição.
 */
export interface ConferenceLine {
  itemRef: string;
  orderedQty: number;
  receivedQty: number;
}

/** Tipo de manifestação do destinatário (enum `ManifestationType`). */
export type ManifestationType = 'CONFIRMED' | 'UNKNOWN' | 'OPERATION_NOT_PERFORMED';

/** Corpo de resposta de POST /api/nfe/inbound/manifestation (InboundManifestationResponse). */
export interface InboundManifestation {
  id: string;
  accessKey: string;
  type: ManifestationType;
  sefazProtocol: string | null;
  manifestedAt: string;
}

/** Natureza da operação aceita pelo backend (enum `NaturezaOperacao`). */
export type NaturezaOperacao = 'VENDA' | 'DEVOLUCAO' | 'NOTA_COMPLEMENTAR' | 'TRANSFERENCIA' | 'REMESSA' | 'OUTRA';

/** Modalidade do frete aceita pelo backend (enum `TransportModality`). */
export type TransportModality = 'CIF' | 'FOB';

/** Corpo de POST /api/nfe (IssueNfeRequest), só com os campos que a tela de emissão manual envia. */
export interface IssueNfeRequest {
  issuerCompanyId: string;
  naturezaOperacao: NaturezaOperacao;
  recipient: {
    personId: string;
    document: string;
    personType: 'INDIVIDUAL' | 'COMPANY';
    name: string;
    stateRegistration?: string;
    state: string;
  };
  items: { productId: string; description: string; quantity: number; unitPrice: number; discount: number }[];
  freight?: number;
  transport?: { modality: TransportModality; grossWeight?: number };
  /** Chave de acesso (44 dígitos) da nota original; obrigatória em devolução e nota complementar. */
  referencedAccessKey?: string;
  additionalInfo?: string;
  /** Forma de pagamento escolhida (código de GET /api/auxiliary/payment-methods); só informativa, não vai no XML. */
  paymentMethodType?: PaymentMethodType;
}

/** Corpo de resposta de POST /api/nfe (IssueNfeResponse): a nota nasce enfileirada, sem autorização ainda. */
export interface IssueNfeResponse {
  id: string;
  accessKey: string | null;
  documentSeries: string | null;
  documentNumber: number | null;
  status: NfeDocumentStatus;
}

/** Corpo de resposta de POST /api/nfe/{id}/cancel (CancelNfeResponse). */
interface CancelNfeResult {
  id: string;
  status: NfeDocumentStatus;
  cancellationJustification: string | null;
  cancelledAt: string | null;
}

/**
 * Mensagem do backend para uma falha. Cobre `{message}` (handlers locais dos controllers), texto puro
 * (`ApiExceptionHandler` genérico devolve `ResponseEntity<String>`, que o Angular embrulha em `{text}` quando
 * falha o parse como JSON) e Blob (downloads).
 */
export async function nfeFailureDetail(error: unknown): Promise<string> {
  if (!(error instanceof HttpErrorResponse)) return '';
  let body: unknown = error.error;
  if (body instanceof Blob) {
    try {
      body = JSON.parse(await body.text());
    } catch {
      return '';
    }
  }
  if (typeof body === 'string') return body.trim();
  const record = body as { message?: unknown; text?: unknown } | null;
  const message = record?.message ?? record?.text;
  return typeof message === 'string' ? message.trim() : '';
}

/** NF-e emitidas e de entrada da empresa (GET /api/nfe, GET /api/nfe/inbound) e as ações sobre uma NF-e emitida. */
@Injectable({ providedIn: 'root' })
export class NfeService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/nfe`;

  private readonly issued = signal<NfeListItem[]>([]);
  private readonly issuedStatus = signal<LoadStatus>('loading');
  private readonly inbound = signal<InboundNfeListItem[]>([]);
  private readonly inboundStatus = signal<LoadStatus>('loading');

  readonly nfes = this.issued.asReadonly();
  readonly status = this.issuedStatus.asReadonly();
  readonly inboundNfes = this.inbound.asReadonly();
  readonly inboundLoadStatus = this.inboundStatus.asReadonly();

  /** Recarrega as duas listas, cada uma com o seu status; nunca rejeita. */
  async load(): Promise<void> {
    await Promise.all([this.loadIssued(), this.loadInbound()]);
  }

  private async loadIssued(): Promise<void> {
    this.issuedStatus.set('loading');
    try {
      this.issued.set(await firstValueFrom(this.http.get<NfeListItem[]>(this.baseUrl)));
      this.issuedStatus.set('ready');
    } catch {
      this.issuedStatus.set('error');
    }
  }

  private async loadInbound(): Promise<void> {
    this.inboundStatus.set('loading');
    try {
      this.inbound.set(await firstValueFrom(this.http.get<InboundNfeListItem[]>(`${this.baseUrl}/inbound`)));
      this.inboundStatus.set('ready');
    } catch {
      this.inboundStatus.set('error');
    }
  }

  /** POST /api/nfe/inbound/import-xml — cria a nota de entrada a partir do XML e atualiza a lista. Rejeita se o backend recusa (400 com `{message}`). */
  async importInboundXml(companyId: string, xmlFile: File): Promise<InboundNfeSummary> {
    const form = new FormData();
    form.append('companyId', companyId);
    form.append('xmlFile', xmlFile);
    const created = await firstValueFrom(this.http.post<InboundNfeSummary>(`${this.baseUrl}/inbound/import-xml`, form));
    await this.refreshInbound();
    return created;
  }

  /** POST /api/nfe/inbound — lançamento manual; atualiza a lista. Rejeita se o backend recusa (400 com `{message}`). */
  async enterInboundManually(request: EnterInboundNfeRequest): Promise<InboundNfeSummary> {
    const created = await firstValueFrom(this.http.post<InboundNfeSummary>(`${this.baseUrl}/inbound`, request));
    await this.refreshInbound();
    return created;
  }

  /** GET /api/nfe/inbound/{id}/conference — fornecedor/itens com a resolução de produto já calculada (GRA-246). */
  getConferenceView(id: string): Promise<InboundNfeConferenceView> {
    return firstValueFrom(this.http.get<InboundNfeConferenceView>(`${this.baseUrl}/inbound/${id}/conference`));
  }

  /** POST .../items/{itemIndex}/link-new-product — cadastra o produto a partir do item e já cria o vínculo (BR4). */
  linkNewProduct(id: string, itemIndex: number, request: RegisterProductFromItemRequest): Promise<RegisteredProduct> {
    return firstValueFrom(
      this.http.post<RegisteredProduct>(`${this.baseUrl}/inbound/${id}/items/${itemIndex}/link-new-product`, request));
  }

  /** POST .../items/{itemIndex}/link-existing-product — vincula o item a um produto já existente do catálogo (BR5). */
  linkExistingProduct(id: string, itemIndex: number, productId: string): Promise<void> {
    return firstValueFrom(
      this.http.post<void>(`${this.baseUrl}/inbound/${id}/items/${itemIndex}/link-existing-product`, { productId }));
  }

  /** POST /api/nfe/inbound/{id}/confirm-receipt — a linha passa a refletir a resposta, sem recarregar a lista. Rejeita se o backend recusa. */
  async confirmInboundReceipt(id: string, conferenceResult: ConferenceLine[]): Promise<void> {
    const result = await firstValueFrom(
      this.http.post<InboundNfeSummary>(`${this.baseUrl}/inbound/${id}/confirm-receipt`, { conferenceResult }));
    this.inbound.update(list => list.map(n => n.id === id ? { ...n, status: result.status } : n));
  }

  /** POST /api/nfe/inbound/manifestation — manifestação do destinatário sobre a chave de acesso. Rejeita se o backend recusa. */
  manifestInbound(accessKey: string, type: ManifestationType): Promise<InboundManifestation> {
    return firstValueFrom(this.http.post<InboundManifestation>(`${this.baseUrl}/inbound/manifestation`, { accessKey, type }));
  }

  /** Relê as Entradas sem passar por "carregando", para a tabela não piscar depois de uma ação. */
  private async refreshInbound(): Promise<void> {
    try {
      this.inbound.set(await firstValueFrom(this.http.get<InboundNfeListItem[]>(`${this.baseUrl}/inbound`)));
      this.inboundStatus.set('ready');
    } catch {
      // A nota já foi criada; se a releitura falhar a lista antiga permanece e "Tentar novamente" segue disponível.
    }
  }

  /** POST /api/nfe — emite a NF-e. Rejeita se o backend recusa (400 com `{message}`; ver {@link nfeFailureDetail}). */
  issue(request: IssueNfeRequest): Promise<IssueNfeResponse> {
    return firstValueFrom(this.http.post<IssueNfeResponse>(this.baseUrl, request));
  }

  /** POST /api/nfe/{id}/cancel — a linha cancelada passa a refletir a resposta, sem recarregar a lista. Rejeita se o backend recusa. */
  async cancel(id: string, justification: string): Promise<void> {
    const result = await firstValueFrom(this.http.post<CancelNfeResult>(`${this.baseUrl}/${id}/cancel`, { justification }));
    this.issued.update(list =>
      list.map(n => n.id === id
        ? { ...n, status: result.status, cancellationJustification: result.cancellationJustification, cancelledAt: result.cancelledAt }
        : n));
  }

  /** GET /api/nfe/{id}/danfe — o PDF (retrato). */
  danfe(id: string): Promise<Blob> {
    return firstValueFrom(this.http.get(`${this.baseUrl}/${id}/danfe`, { params: { orientation: 'PORTRAIT' }, responseType: 'blob' }));
  }

  /** GET /api/nfe/{id}/xml — o XML autorizado. */
  xml(id: string): Promise<Blob> {
    return firstValueFrom(this.http.get(`${this.baseUrl}/${id}/xml`, { responseType: 'blob' }));
  }
}
