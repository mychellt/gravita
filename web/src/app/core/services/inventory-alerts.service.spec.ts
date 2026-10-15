import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { InventoryAlertsService, daysUntil } from './inventory-alerts.service';

const suggestion = (productId: string, suggestedQuantity = 10) =>
  ({ productId, warehouseId: 'w1', available: 1, reorderPoint: 5, suggestedQuantity });

describe('InventoryAlertsService', () => {
  let service: InventoryAlertsService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(InventoryAlertsService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  async function loadLowStock(...rows: ReturnType<typeof suggestion>[]) {
    const loading = service.loadLowStock();
    http.expectOne('/api/inventory/alerts/low-stock').flush(rows);
    await loading;
  }

  it('loads both alerts, starting in the loading state', async () => {
    const loading = service.loadAll();
    expect(service.expiringStatus()).toBe('loading');
    expect(service.lowStockStatus()).toBe('loading');
    http.expectOne(r => r.url === '/api/inventory/alerts/expiring-lots').flush([
      { productId: 'p1', warehouseId: 'w1', lotCode: 'L1', expiryDate: '2026-01-01', remainingQuantity: 3 }]);
    http.expectOne('/api/inventory/alerts/low-stock').flush([suggestion('p1')]);
    await loading;

    expect(service.expiringStatus()).toBe('ready');
    expect(service.expiringLots().length).toBe(1);
    expect(service.lowStockStatus()).toBe('ready');
    expect(service.lowStock()[0].request).toBe('idle');
  });

  it('flags a failed load as an error instead of an empty list, and recovers on retry', async () => {
    const failing = service.loadLowStock();
    http.expectOne('/api/inventory/alerts/low-stock').flush(null, { status: 500, statusText: 'Server Error' });
    await failing;
    expect(service.lowStockStatus()).toBe('error');

    await loadLowStock(suggestion('p1'));
    expect(service.lowStockStatus()).toBe('ready');
  });

  it('reloads the expiring lots with the new window', async () => {
    const changing = service.changeExpiryWindow(30);
    const request = http.expectOne(r => r.url === '/api/inventory/alerts/expiring-lots');
    expect(request.request.params.get('withinDays')).toBe('30');
    request.flush([]);
    await changing;

    expect(service.expiryWindowDays()).toBe(30);
  });

  it('marks a row as requested only after the server answers', async () => {
    await loadLowStock(suggestion('p1', 25));

    const requesting = service.requestPurchase('p1');
    expect(service.lowStock()[0].request).toBe('sending');
    const post = http.expectOne('/api/purchasing/requests');
    expect(post.request.body.items).toEqual([{ productId: 'p1', quantity: 25 }]);
    post.flush({ id: 'r1' }, { status: 201, statusText: 'Created' });

    expect(await requesting).toEqual({ ok: true });
    expect(service.lowStock()[0].request).toBe('requested');
    expect(service.pendingRequests()).toEqual([]);
  });

  it('puts the row back to idle and reports the backend message when the request fails', async () => {
    await loadLowStock(suggestion('p1'));

    const requesting = service.requestPurchase('p1');
    http.expectOne('/api/purchasing/requests').flush({ message: 'Produto inativo' }, { status: 400, statusText: 'Bad Request' });

    expect(await requesting).toEqual({ ok: false, error: 'Produto inativo' });
    expect(service.lowStock()[0].request).toBe('idle');
  });

  it('does not send a second request for a row already requested or with nothing to order', async () => {
    await loadLowStock(suggestion('p1'), suggestion('p2', 0));
    const first = service.requestPurchase('p1');
    http.expectOne('/api/purchasing/requests').flush({ id: 'r1' }, { status: 201, statusText: 'Created' });
    await first;

    expect((await service.requestPurchase('p1')).ok).toBeFalse();
    expect((await service.requestPurchase('p2')).ok).toBeFalse();
    http.expectNone('/api/purchasing/requests');
  });

  it('requests every pending row and counts successes and failures', async () => {
    await loadLowStock(suggestion('p1'), suggestion('p2'));

    const all = service.requestAll();
    const posts = http.match('/api/purchasing/requests');
    expect(posts.length).toBe(2);
    posts[0].flush({ id: 'r1' }, { status: 201, statusText: 'Created' });
    posts[1].flush(null, { status: 500, statusText: 'Server Error' });

    expect(await all).toEqual({ created: 1, failed: 1 });
  });
});

describe('daysUntil', () => {
  const now = new Date(2026, 5, 10, 15, 30);

  it('counts calendar days from today', () => {
    expect(daysUntil('2026-06-10', now)).toBe(0);
    expect(daysUntil('2026-06-17', now)).toBe(7);
    expect(daysUntil('2026-06-09', now)).toBe(-1);
  });
});
