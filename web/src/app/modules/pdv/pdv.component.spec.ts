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

  it('maps the product icon key to an emoji, falling back to the generic one', async () => {
    const c = await create([{ ...found('p1', 'Tinta', 10), icon: 'PAINT' }, { ...found('p2', 'Piso', 10), icon: 'FLOORING' },
      found('p3', 'Sem ícone', 10), { ...found('p4', 'Desconhecido', 10), icon: 'NOVO' }]);

    expect(c.produtos().map(p => p.icone)).toEqual(['🪣', '🔲', '📦', '📦']);
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

  describe('Confirmar venda', () => {
    async function ready(): Promise<PdvComponent> {
      const c = await create([found('p1', 'Tinta', 25), found('p2', 'Piso', 10)]);
      c.addToCart(c.produtos()[0]);
      c.addToCart(c.produtos()[0]);
      c.addToCart(c.produtos()[1]);
      c.openPayment();
      return c;
    }

    /** Responde à abertura da sessão de caixa e devolve a requisição da venda. */
    async function sessionThenSale() {
      await flush();
      http.expectOne('/api/pdv/sessions/current').flush({ id: 'sess-1' });
      await flush();
      return http.expectOne('/api/pdv/sales');
    }

    /** Depois de uma venda gravada a tela recarrega saldo e produtos. */
    async function answerReload() {
      await flush();
      http.expectOne(r => r.url === '/api/inventory/balances').flush({ content: [], page: 0, size: 200, totalElements: 0, totalPages: 1 });
      await flush();
      http.expectOne(r => r.url === '/api/pdv/products/search').flush([]);
      await flush();
    }

    it('stores the sale with the cart, the payment and the CPF, then clears the cart', async () => {
      const c = await ready();
      c.cpfNota.set('529.982.247-25');
      c.valorRecebido.set(100);

      const confirming = c.confirmSale();
      const sale = await sessionThenSale();
      expect(sale.request.body).toEqual({
        sessionId: 'sess-1',
        items: [{ productId: 'p1', quantity: 2, unitPrice: 25 }, { productId: 'p2', quantity: 1, unitPrice: 10 }],
        payments: [{ method: 'CASH', amount: 100 }],
        customerCpf: '52998224725',
      });
      sale.flush({ id: 's1', status: 'AUTHORIZED', accessKey: 'K', protocol: 'P', issuanceError: null });
      await confirming;

      expect(c.cart()).toEqual([]);
      expect(c.showPayModal()).toBeFalse();
      expect(c.confirmando()).toBeFalse();
      await answerReload();
    });

    it('pays non-cash methods with the exact total and maps the method', async () => {
      const c = await ready();
      c.selectMethod('pix');
      c.valorRecebido.set(999);

      const confirming = c.confirmSale();
      const sale = await sessionThenSale();
      expect(sale.request.body.payments).toEqual([{ method: 'PIX', amount: 60 }]);
      expect(sale.request.body.customerCpf).toBeUndefined();
      sale.flush({ id: 's1', status: 'AUTHORIZED', accessKey: 'K', protocol: 'P', issuanceError: null });
      await confirming;
      await answerReload();
    });

    it('keeps the sale stored but says so when the NFC-e could not be issued', async () => {
      const c = await ready();

      const confirming = c.confirmSale();
      (await sessionThenSale()).flush({ id: 's1', status: 'DRAFT', accessKey: null, protocol: null, issuanceError: 'Company state is required' });
      await confirming;

      expect(c.cart()).toEqual([]);
      await answerReload();
    });

    it('keeps the cart and the modal when the sale is refused, and asks for a new session next time', async () => {
      const c = await ready();

      const confirming = c.confirmSale();
      (await sessionThenSale()).flush({ message: 'PosSession is not open' }, { status: 409, statusText: 'Conflict' });
      await confirming;

      expect(c.cart().length).toBe(2);
      expect(c.showPayModal()).toBeTrue();
      expect(c.confirmando()).toBeFalse();

      const again = c.confirmSale();
      await flush();
      http.expectOne('/api/pdv/sessions/current').flush({ id: 'sess-2' });
      await flush();
      http.expectOne('/api/pdv/sales').flush({ message: 'still refused' }, { status: 409, statusText: 'Conflict' });
      await again;
    });

    it('does not call the backend when the cash received does not cover the total', async () => {
      const c = await ready();
      c.valorRecebido.set(10);

      await c.confirmSale();

      expect(c.cart().length).toBe(2);
      http.expectNone('/api/pdv/sessions/current');
    });

    it('does not call the backend for an incomplete CPF', async () => {
      const c = await ready();
      c.cpfNota.set('123');

      await c.confirmSale();

      expect(c.cart().length).toBe(2);
      http.expectNone('/api/pdv/sessions/current');
    });
  });
});
