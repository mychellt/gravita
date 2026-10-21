import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PurchasingComponent } from './purchasing.component';

const flush = () => new Promise<void>(r => setTimeout(r));

const order = (id: string, overrides: Record<string, unknown> = {}) => ({
  id, supplierId: 's1', supplierName: 'Fornecedor Beta', status: 'OPEN', approvalRequired: false, approvedBy: null,
  createdAt: new Date().toISOString(), totalValue: 100,
  items: [{ productId: 'p1', quantity: 2, unitPrice: 30 }, { productId: 'p2', quantity: 1, unitPrice: 40 }], ...overrides,
});

describe('PurchasingComponent', () => {
  let http: HttpTestingController;

  async function create(orders: unknown[] | 'fail') {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    const component = TestBed.runInInjectionContext(() => new PurchasingComponent());
    const request = http.expectOne('/api/purchasing/orders');
    orders === 'fail' ? request.flush(null, { status: 500, statusText: 'Server Error' }) : request.flush(orders as object);
    await flush();
    return component;
  }
  afterEach(() => http.verify());

  it('maps each order to a row with its number, supplier, total and status in Portuguese', async () => {
    const c = await create([
      order('aaaaaaaa-1', { approvalRequired: true }), order('bbbbbbbb-2'), order('cccccccc-3', { status: 'PARTIALLY_RECEIVED' }),
      order('dddddddd-4', { status: 'CLOSED' }), order('eeeeeeee-5', { status: 'CANCELLED' }), order('ffffffff-6', { supplierName: null }),
    ]);

    expect(c.orders().map(o => o.status)).toEqual(['aguarda_aprovacao', 'aberto', 'recebido_parcial', 'encerrado', 'cancelado', 'aberto']);
    expect(c.orders()[0].numero).toBe('PC-AAAAAAAA');
    expect(c.orders()[5].fornecedorNome).toBe('—');
    expect(c.orders().map(o => o.aguardaAprovacao)).toEqual([true, false, false, false, false, false]);
    expect(c.orders().map(o => o.recebivel)).toEqual([false, true, false, false, false, true]);
  });

  it('computes the KPIs from the orders: open, awaiting delivery, closed this month and open value', async () => {
    const lastYear = new Date(); lastYear.setFullYear(lastYear.getFullYear() - 1);
    const c = await create([
      order('o1', { approvalRequired: true, totalValue: 10 }), order('o2', { totalValue: 20 }),
      order('o3', { status: 'PARTIALLY_RECEIVED', totalValue: 30 }), order('o4', { status: 'CLOSED' }),
      order('o5', { status: 'CLOSED', createdAt: lastYear.toISOString() }), order('o6', { status: 'CANCELLED' }),
    ]);

    expect(c.totals()).toEqual({ open: 3, awaitingDelivery: 2, closedThisMonth: 1, openValue: 60 });
  });

  it('offers a retry when the orders cannot be loaded', async () => {
    const c = await create('fail');

    expect(c.loadStatus()).toBe('error');
    c.reload();
    http.expectOne('/api/purchasing/orders').flush([]);
  });

  it('approves an order awaiting approval and reloads the list', async () => {
    const c = await create([order('o1', { approvalRequired: true })]);

    const deciding = c.decide(c.orders()[0], 'APPROVE');
    const request = http.expectOne('/api/purchasing/orders/o1/approve');
    expect(request.request.body).toEqual({ decision: 'APPROVE' });
    request.flush(null, { status: 204, statusText: 'No Content' });
    await flush();
    http.expectOne('/api/purchasing/orders').flush([order('o1')]);
    await deciding;

    expect(c.orders()[0].status).toBe('aberto');
    expect(c.busyId()).toBeNull();
  });

  it('opens the receipt of every item of the order', async () => {
    const c = await create([order('o1')]);

    const receiving = c.receive(c.orders()[0]);
    const request = http.expectOne('/api/purchasing/orders/o1/receipts');
    expect(request.request.body).toEqual({ receivedItems: [{ productId: 'p1', receivedQty: 2 }, { productId: 'p2', receivedQty: 1 }] });
    request.flush({ id: 'r1' }, { status: 201, statusText: 'Created' });
    await flush();
    http.expectOne('/api/purchasing/orders').flush([order('o1')]);
    await receiving;
  });

  it('keeps the order and frees the button when the backend refuses the action', async () => {
    const c = await create([order('o1')]);

    const receiving = c.receive(c.orders()[0]);
    http.expectOne('/api/purchasing/orders/o1/receipts').flush({ message: 'order pending approval' }, { status: 409, statusText: 'Conflict' });
    await receiving;

    expect(c.busyId()).toBeNull();
    expect(c.orders().length).toBe(1);
  });
});
