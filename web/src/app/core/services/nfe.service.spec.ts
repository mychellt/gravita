import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { InboundNfeListItem, NfeListItem, NfeService, nfeFailureDetail } from './nfe.service';

const nfe = (over: Partial<NfeListItem>): NfeListItem => ({
  id: 'n1', documentNumber: 1, documentSeries: '1', recipientName: 'Cliente A', cfop: '5102',
  emittedAt: '2026-10-14T10:00:00Z', totalValue: 100, icmsValue: 18, status: 'AUTHORIZED',
  cancellationJustification: null, cancelledAt: null, ...over,
});

const inbound = (over: Partial<InboundNfeListItem>): InboundNfeListItem => ({
  id: 'i1', series: '1', number: '12345', supplierName: 'Fornecedor', issuedAt: '2026-10-01T00:00:00Z',
  totalValue: 165, status: 'PENDING_CONFERENCE', ...over,
});

describe('NfeService', () => {
  let service: NfeService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(NfeService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('loads both lists, each with its own status', async () => {
    const loading = service.load();
    expect(service.status()).toBe('loading');
    http.expectOne('/api/nfe').flush([nfe({})]);
    http.expectOne('/api/nfe/inbound').flush([inbound({})]);
    await loading;

    expect(service.status()).toBe('ready');
    expect(service.inboundLoadStatus()).toBe('ready');
    expect(service.nfes().length).toBe(1);
    expect(service.inboundNfes()[0].supplierName).toBe('Fornecedor');
  });

  it('reports a failed list without affecting the other', async () => {
    const loading = service.load();
    http.expectOne('/api/nfe').flush(null, { status: 500, statusText: 'Server Error' });
    http.expectOne('/api/nfe/inbound').flush([]);
    await loading;

    expect(service.status()).toBe('error');
    expect(service.inboundLoadStatus()).toBe('ready');
  });

  it('cancels through POST /api/nfe/{id}/cancel and patches the row from the response', async () => {
    const loading = service.load();
    http.expectOne('/api/nfe').flush([nfe({ id: 'a' }), nfe({ id: 'b' })]);
    http.expectOne('/api/nfe/inbound').flush([]);
    await loading;

    const cancelling = service.cancel('a', 'Erro de digitação');
    const request = http.expectOne('/api/nfe/a/cancel');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ justification: 'Erro de digitação' });
    request.flush({ id: 'a', status: 'CANCELLED', cancellationJustification: 'Erro de digitação', cancelledAt: '2026-10-14T11:00:00Z' });
    await cancelling;

    expect(service.nfes().map(n => n.status)).toEqual(['CANCELLED', 'AUTHORIZED']);
    expect(service.nfes()[0].cancelledAt).toBe('2026-10-14T11:00:00Z');
  });

  it('rejects a refused cancellation and leaves the list untouched', async () => {
    const loading = service.load();
    http.expectOne('/api/nfe').flush([nfe({ id: 'a' })]);
    http.expectOne('/api/nfe/inbound').flush([]);
    await loading;

    const cancelling = service.cancel('a', 'x');
    http.expectOne('/api/nfe/a/cancel').flush({ message: 'Cancellation window has expired' }, { status: 400, statusText: 'Bad Request' });

    await expectAsync(cancelling).toBeRejected();
    expect(service.nfes()[0].status).toBe('AUTHORIZED');
  });

  it('fetches the DANFE (portrait) and the XML as blobs', async () => {
    const danfe = service.danfe('a');
    const danfeRequest = http.expectOne(r => r.url === '/api/nfe/a/danfe');
    expect(danfeRequest.request.params.get('orientation')).toBe('PORTRAIT');
    expect(danfeRequest.request.responseType).toBe('blob');
    danfeRequest.flush(new Blob(['%PDF']));
    expect((await danfe).size).toBe(4);

    const xml = service.xml('a');
    http.expectOne('/api/nfe/a/xml').flush(new Blob(['<nfe/>']));
    expect((await xml).size).toBe(6);
  });

  it('reads the backend message from a plain or a Blob error body', async () => {
    const plain = new HttpErrorResponse({ status: 400, error: { message: 'Prazo expirado' } });
    const blob = new HttpErrorResponse({ status: 400, error: new Blob([JSON.stringify({ message: 'NF-e não autorizada' })]) });

    expect(await nfeFailureDetail(plain)).toBe('Prazo expirado');
    expect(await nfeFailureDetail(blob)).toBe('NF-e não autorizada');
    expect(await nfeFailureDetail(new Error('x'))).toBe('');
  });
});
