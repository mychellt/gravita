import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PdvComponent } from './pdv.component';

const flush = () => new Promise<void>(r => setTimeout(r));

const found = (id: string, description: string, unitPrice: number, availableForSale = true) =>
  ({ productId: id, description, ncm: '73181600', unitPrice, availableForSale });

const balance = (productId: string, available: number) =>
  ({ productId, productCode: null, warehouseId: 'w', onHand: available, reserved: 0, inTransit: 0, available, averageCost: 1, allowNegativeStock: false });

describe('PdvComponent — Produtos', () => {
  let http: HttpTestingController;

  async function create(products: unknown[] | 'fail', balances: unknown[] = []) {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    const component = TestBed.runInInjectionContext(() => new PdvComponent());
    http.expectOne(r => r.url === '/api/inventory/balances').flush({ content: balances, page: 0, size: 200, totalElements: balances.length, totalPages: 1 });
    await flush();
    const search = http.expectOne(r => r.url === '/api/pdv/products/search');
    expect(search.request.params.get('q')).toBe('');
    products === 'fail' ? search.flush(null, { status: 500, statusText: 'Server Error' }) : search.flush(products);
    await flush();
    return component;
  }
  afterEach(() => http.verify());

  it('lists the active products from the backend with price and available stock', async () => {
    const c = await create([found('p1', 'Notebook', 6998.89), found('p2', 'Mouse', 50), found('p3', 'Inativo', 1, false)],
      [balance('p1', 3)]);

    expect(c.produtosStatus()).toBe('ready');
    expect(c.produtos().map(p => [p.id, p.precoVenda, p.estoque])).toEqual([['p1', 6998.89, 3], ['p2', 50, null]]);
  });

  it('searches on the server after typing, keeping only the newest answer', async () => {
    const c = await create([found('p1', 'Notebook', 10)]);

    c.search.set('mou');
    const first = c.loadProdutos();
    c.search.set('mouse');
    const second = c.loadProdutos();
    const [old, latest] = http.match(r => r.url === '/api/pdv/products/search');
    expect(latest.request.params.get('q')).toBe('mouse');
    latest.flush([found('p2', 'Mouse', 50)]);
    await second;
    old.flush([found('p9', 'Mouse antigo', 1)]);
    await first;

    expect(c.produtos().map(p => p.id)).toEqual(['p2']);
  });

  it('shows a retryable error when the catalog cannot be loaded', async () => {
    const c = await create('fail');

    expect(c.produtosStatus()).toBe('error');
    c.reload();
    http.expectOne(r => r.url === '/api/inventory/balances').flush({ content: [], page: 0, size: 200, totalElements: 0, totalPages: 1 });
    await flush();
    http.expectOne(r => r.url === '/api/pdv/products/search').flush([]);
    await flush();
    expect(c.produtosStatus()).toBe('ready');
  });

  it('still lists products when the stock balances fail to load', async () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    const c = TestBed.runInInjectionContext(() => new PdvComponent());
    http.expectOne(r => r.url === '/api/inventory/balances').flush(null, { status: 500, statusText: 'Server Error' });
    await flush();
    http.expectOne(r => r.url === '/api/pdv/products/search').flush([found('p1', 'Notebook', 10)]);
    await flush();

    expect(c.produtos().map(p => p.estoque)).toEqual([null]);
  });
});
