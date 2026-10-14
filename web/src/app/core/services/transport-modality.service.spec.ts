import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TransportModalityOption, TransportModalityService, transportModalityFailureDetail } from './transport-modality.service';

const URL = '/api/auxiliary/transport-modalities';

const rows = (): TransportModalityOption[] => [
  { id: 'tm-cif', name: 'CIF — Por conta do emitente', code: 'CIF', active: true },
  { id: 'tm-none', name: 'Sem transporte', code: 'NONE', active: true },
];

describe('TransportModalityService', () => {
  let service: TransportModalityService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(TransportModalityService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads the dropdown options from GET /api/auxiliary/transport-modalities (active only, no includeInactive)', async () => {
    const loading = service.load();
    expect(service.status()).toBe('loading');
    const req = http.expectOne(URL);
    expect(req.request.params.has('includeInactive')).toBeFalse();
    req.flush(rows());
    await loading;

    expect(service.modalities()).toEqual(rows());
    expect(service.status()).toBe('ready');
  });

  it('turns a load failure into the error status without rejecting', async () => {
    const loading = service.load();
    http.expectOne(URL).flush(null, { status: 500, statusText: 'Server Error' });
    await loading;

    expect(service.status()).toBe('error');
    expect(service.modalities()).toEqual([]);
  });

  it('loadAll asks for inactive modalities too and keeps them apart from the dropdown options', async () => {
    const all = [...rows(), { id: 'tm-fob', name: 'FOB', code: 'FOB' as const, active: false }];

    const loading = service.loadAll();
    const req = http.expectOne(r => r.url === URL);
    expect(req.request.params.get('includeInactive')).toBe('true');
    req.flush(all);
    await loading;

    expect(service.managed()).toEqual(all);
    expect(service.managedStatus()).toBe('ready');
    expect(service.modalities()).toEqual([]);
  });

  it('create POSTs the input and then re-reads the full list', async () => {
    const creating = service.create({ name: 'FOB', code: 'FOB' });
    const post = http.expectOne(URL);
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual({ name: 'FOB', code: 'FOB' });
    post.flush({ id: 'tm-fob', name: 'FOB', code: 'FOB', active: true }, { status: 201, statusText: 'Created' });
    await Promise.resolve();
    http.expectOne(r => r.url === URL && r.method === 'GET').flush(rows());
    await creating;

    expect(service.managed()).toEqual(rows());
  });

  it('update PATCHes the row and then re-reads the list', async () => {
    const updating = service.update('tm-cif', { name: 'CIF renomeada', code: 'CIF', active: true });
    const patch = http.expectOne(`${URL}/tm-cif`);
    expect(patch.request.method).toBe('PATCH');
    expect(patch.request.body).toEqual({ name: 'CIF renomeada', code: 'CIF', active: true });
    patch.flush({});
    await Promise.resolve();
    http.expectOne(r => r.url === URL && r.method === 'GET').flush(rows());
    await updating;
  });

  it('deactivate DELETEs the row (soft delete on the server) and then re-reads the list', async () => {
    const deactivating = service.deactivate('tm-cif');
    const del = http.expectOne(`${URL}/tm-cif`);
    expect(del.request.method).toBe('DELETE');
    del.flush(null, { status: 204, statusText: 'No Content' });
    await Promise.resolve();
    http.expectOne(r => r.url === URL && r.method === 'GET').flush([{ ...rows()[0], active: false }, rows()[1]]);
    await deactivating;

    expect(service.managed()[0].active).toBeFalse();
  });

  it('a rejected write does not re-read the list and surfaces the HTTP error', async () => {
    const creating = service.create({ name: 'CIF', code: 'CIF' });
    http.expectOne(URL).flush('An active transport modality with code CIF already exists', { status: 409, statusText: 'Conflict' });

    await expectAsync(creating).toBeRejected();
  });

  describe('transportModalityFailureDetail', () => {
    it('reads a plain-text body, a {message} body, and ignores anything else', () => {
      expect(transportModalityFailureDetail(new HttpErrorResponse({ status: 409, error: ' already exists ' }))).toBe('already exists');
      expect(transportModalityFailureDetail(new HttpErrorResponse({ status: 400, error: { message: 'bad code' } }))).toBe('bad code');
      expect(transportModalityFailureDetail(new HttpErrorResponse({ status: 500, error: null }))).toBe('');
      expect(transportModalityFailureDetail(new Error('boom'))).toBe('');
    });
  });
});
