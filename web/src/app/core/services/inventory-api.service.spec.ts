import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { InventoryApiService, productLabel } from './inventory-api.service';

describe('InventoryApiService', () => {
  let service: InventoryApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(InventoryApiService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('pages balances without sending a warehouse', async () => {
    const reading = service.balances(2, 20);
    const request = http.expectOne(r => r.url === '/api/inventory/balances');
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('size')).toBe('20');
    expect(request.request.params.has('warehouseId')).toBeFalse();
    request.flush({ content: [], page: 2, size: 20, totalElements: 0, totalPages: 0 });

    expect((await reading).page).toBe(2);
  });

  it('sends only the movement filters that were set', async () => {
    const reading = service.movements({ productId: 'p1', type: 'EXIT', from: '2026-01-01T00:00:00.000Z' }, 0, 20);
    const request = http.expectOne(r => r.url === '/api/inventory/movements');
    expect(request.request.params.get('productId')).toBe('p1');
    expect(request.request.params.get('type')).toBe('EXIT');
    expect(request.request.params.get('from')).toBe('2026-01-01T00:00:00.000Z');
    expect(request.request.params.has('to')).toBeFalse();
    request.flush({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
    await reading;
  });

  it('lists lots or serial units by kind', async () => {
    const reading = service.units('SERIAL', 0, 20);
    const request = http.expectOne(r => r.url === '/api/inventory/lots');
    expect(request.request.params.get('kind')).toBe('SERIAL');
    request.flush({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
    await reading;
  });

  it('reads the expiring lots within the window', async () => {
    const reading = service.expiringLots(15);
    const request = http.expectOne(r => r.url === '/api/inventory/alerts/expiring-lots');
    expect(request.request.params.get('withinDays')).toBe('15');
    request.flush([]);
    expect(await reading).toEqual([]);
  });

  it('reads the low-stock suggestions', async () => {
    const reading = service.lowStock();
    http.expectOne('/api/inventory/alerts/low-stock').flush([]);
    expect(await reading).toEqual([]);
  });

  it('creates a purchase request for the suggested quantity and returns its id', async () => {
    const creating = service.createPurchaseRequest('p1', 30);
    const request = http.expectOne('/api/purchasing/requests');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ origin: 'MIN_STOCK_TRIGGER', items: [{ productId: 'p1', quantity: 30 }] });
    request.flush({ id: 'req-1' }, { status: 201, statusText: 'Created' });

    expect(await creating).toBe('req-1');
  });

  it('rejects when the server fails', async () => {
    const reading = service.lowStock();
    http.expectOne('/api/inventory/alerts/low-stock').flush(null, { status: 500, statusText: 'Server Error' });

    await expectAsync(reading).toBeRejected();
  });
});

describe('productLabel', () => {
  it('prefers the product code and falls back to the start of the id', () => {
    expect(productLabel('0a1b2c3d-0000-0000-0000-000000000000', 'SKU-1')).toBe('SKU-1');
    expect(productLabel('0a1b2c3d-0000-0000-0000-000000000000', null)).toBe('0a1b2c3d');
    expect(productLabel('0a1b2c3d-0000-0000-0000-000000000000', '  ')).toBe('0a1b2c3d');
  });
});
