import { provideHttpClient } from '@angular/common/http';
import { HttpRequest } from '@angular/common/http';
import { HttpTestingController, TestRequest, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { InventoryAlertsService } from '../../core/services/inventory-alerts.service';
import { AlertsPanelComponent } from './alerts-panel.component';
import { BalancesTabComponent } from './balances-tab.component';
import { LotsTabComponent } from './lots-tab.component';
import { MovementsTabComponent } from './movements-tab.component';
import { ReorderTabComponent } from './reorder-tab.component';

const PRODUCT = '0a1b2c3d-1111-2222-3333-444444444444';
const page = <T>(content: T[], over = {}) => ({ content, page: 0, size: 20, totalElements: content.length, totalPages: 1, ...over });
const isoDay = (offset: number) => {
  const d = new Date(); d.setDate(d.getDate() + offset);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
};
const text = (fixture: ComponentFixture<unknown>) => (fixture.nativeElement as HTMLElement).textContent ?? '';
const byUrl = (url: string) => (r: HttpRequest<unknown>) => r.url === url;

describe('Inventory tabs backed by the API', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  async function settle<T>(fixture: ComponentFixture<T>) {
    await fixture.whenStable();
    await new Promise(resolve => setTimeout(resolve));
    fixture.detectChanges();
  }

  describe('balances-tab', () => {
    it('shows a loading indicator, then the rows from the server', async () => {
      const fixture = TestBed.createComponent(BalancesTabComponent);
      fixture.detectChanges();
      expect(text(fixture)).toContain('Carregando saldos');

      http.expectOne(byUrl('/api/inventory/balances')).flush(page([{
        productId: PRODUCT, productCode: 'SKU-9', warehouseId: 'w', onHand: 12, reserved: 2, inTransit: 0, available: 10,
        averageCost: 5, allowNegativeStock: false }]));
      await settle(fixture);

      expect(text(fixture)).toContain('SKU-9');
      expect(text(fixture)).not.toContain('Carregando');
    });

    it('shows an error with retry, never an empty table, and recovers on retry', async () => {
      const fixture = TestBed.createComponent(BalancesTabComponent);
      fixture.detectChanges();
      http.expectOne(byUrl('/api/inventory/balances')).flush(null, { status: 500, statusText: 'Server Error' });
      await settle(fixture);

      expect(text(fixture)).toContain('Não foi possível carregar os saldos');
      expect(text(fixture)).not.toContain('Nenhum saldo encontrado');
      (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('button')!.click();
      http.expectOne(byUrl('/api/inventory/balances')).flush(page([]));
      await settle(fixture);

      expect(text(fixture)).toContain('Nenhum saldo encontrado');
    });

    it('toggles allow-negative-stock through the server and reflects the persisted value', async () => {
      const fixture = TestBed.createComponent(BalancesTabComponent);
      fixture.detectChanges();
      http.expectOne(byUrl('/api/inventory/balances')).flush(page([{
        productId: PRODUCT, productCode: 'SKU-9', warehouseId: 'w', onHand: 12, reserved: 2, inTransit: 0, available: 10,
        averageCost: 5, allowNegativeStock: false }]));
      await settle(fixture);
      const switchEl = (fixture.nativeElement as HTMLElement).querySelector<HTMLElement>('[role="switch"]')!;
      expect(switchEl.getAttribute('aria-checked')).toBe('false');

      switchEl.click();
      const request = http.expectOne(byUrl(`/api/inventory/products/${PRODUCT}/allow-negative-stock`));
      expect(request.request.method).toBe('PATCH');
      expect(request.request.body).toEqual({ allowNegativeStock: true });
      request.flush({ productId: PRODUCT, allowNegativeStock: true });
      await settle(fixture);

      expect(switchEl.getAttribute('aria-checked')).toBe('true');
    });

    it('keeps the previous value and shows a toast when the toggle fails', async () => {
      const fixture = TestBed.createComponent(BalancesTabComponent);
      fixture.detectChanges();
      http.expectOne(byUrl('/api/inventory/balances')).flush(page([{
        productId: PRODUCT, productCode: 'SKU-9', warehouseId: 'w', onHand: 12, reserved: 2, inTransit: 0, available: 10,
        averageCost: 5, allowNegativeStock: false }]));
      await settle(fixture);
      const switchEl = (fixture.nativeElement as HTMLElement).querySelector<HTMLElement>('[role="switch"]')!;

      switchEl.click();
      http.expectOne(byUrl(`/api/inventory/products/${PRODUCT}/allow-negative-stock`))
        .flush(null, { status: 500, statusText: 'Server Error' });
      await settle(fixture);

      expect(switchEl.getAttribute('aria-checked')).toBe('false');
    });
  });

  describe('movements-tab', () => {
    const movement = (over = {}) => ({
      id: 'm1', type: 'EXIT', productId: PRODUCT, productCode: 'SKU-9', warehouseId: 'w', quantity: 3, unitCost: 1,
      lotCode: null, serialNumbers: null, originReference: 'NF-1', justification: null, user: null,
      timestamp: '2026-06-10T12:00:00Z', ...over });

    it('sends type, product and date range to the server instead of filtering in memory', async () => {
      const fixture = TestBed.createComponent(MovementsTabComponent);
      fixture.detectChanges();
      http.expectOne(byUrl('/api/inventory/movements')).flush(page([movement()]));
      await settle(fixture);
      const component = fixture.componentInstance;

      component.definirTipo('EXIT');
      let request: TestRequest = http.expectOne(byUrl('/api/inventory/movements'));
      expect(request.request.params.get('type')).toBe('EXIT');
      request.flush(page([]));

      component.definirProduto(PRODUCT);
      request = http.expectOne(byUrl('/api/inventory/movements'));
      expect(request.request.params.get('productId')).toBe(PRODUCT);
      request.flush(page([]));

      component.definirDe('2026-06-01');
      component.definirAte('2026-06-10');
      const pending = http.match(byUrl('/api/inventory/movements'));
      expect(pending.length).toBe(2);
      request = pending[1];
      expect(new Date(request.request.params.get('from')!).getTime()).toBe(new Date('2026-06-01T00:00:00').getTime());
      expect(new Date(request.request.params.get('to')!).getTime()).toBe(new Date('2026-06-10T23:59:59.999').getTime());
      request.flush(page([]));
      pending[0].flush(page([]));
    });

    it('does not call the server for a malformed product id', async () => {
      const fixture = TestBed.createComponent(MovementsTabComponent);
      fixture.detectChanges();
      http.expectOne(byUrl('/api/inventory/movements')).flush(page([]));
      await settle(fixture);

      fixture.componentInstance.definirProduto('not-a-uuid');
      await settle(fixture);

      http.expectNone(byUrl('/api/inventory/movements'));
      expect(text(fixture)).toContain('formato UUID');
    });

    it('requests another page and shows the exit as negative', async () => {
      const fixture = TestBed.createComponent(MovementsTabComponent);
      fixture.detectChanges();
      http.expectOne(byUrl('/api/inventory/movements')).flush(page([movement()], { totalPages: 3, totalElements: 41 }));
      await settle(fixture);
      expect(text(fixture)).toContain('−3');

      fixture.componentInstance.load(1);
      const request = http.expectOne(byUrl('/api/inventory/movements'));
      expect(request.request.params.get('page')).toBe('1');
      request.flush(page([]));
    });

    it('shows an error with retry when the request fails', async () => {
      const fixture = TestBed.createComponent(MovementsTabComponent);
      fixture.detectChanges();
      http.expectOne(byUrl('/api/inventory/movements')).flush(null, { status: 500, statusText: 'Server Error' });
      await settle(fixture);

      expect(text(fixture)).toContain('Não foi possível carregar as movimentações');
      expect(text(fixture)).not.toContain('Nenhuma movimentação encontrada');
    });
  });

  describe('lots-tab', () => {
    const lot = (code: string, expiryDate: string) =>
      ({ kind: 'LOT', id: code, productId: PRODUCT, productCode: 'SKU-9', warehouseId: 'w', code, expiryDate, quantity: 4, status: null });

    it('marks as "a vencer" exactly the lots the expiring-lots alert returns, and expired ones as vencidos', async () => {
      const alertas = TestBed.inject(InventoryAlertsService);
      const loadingAlerts = alertas.loadExpiring();
      http.expectOne(byUrl('/api/inventory/alerts/expiring-lots')).flush([
        { productId: PRODUCT, warehouseId: 'w', lotCode: 'SOON', expiryDate: isoDay(3), remainingQuantity: 4 },
        { productId: PRODUCT, warehouseId: 'w', lotCode: 'OLD', expiryDate: isoDay(-2), remainingQuantity: 4 }]);
      await loadingAlerts;

      const fixture = TestBed.createComponent(LotsTabComponent);
      fixture.detectChanges();
      const lots = http.match(byUrl('/api/inventory/lots'));
      expect(lots.map(r => r.request.params.get('kind'))).toEqual(['LOT', 'SERIAL']);
      lots[0].flush(page([lot('OLD', isoDay(-2)), lot('SOON', isoDay(3)), lot('LATER', isoDay(90))]));
      lots[1].flush(page([]));
      await settle(fixture);

      const rows = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('tbody tr')).map(r => r.textContent ?? '');
      expect(rows[0]).toContain('Vencido');
      expect(rows[1]).toContain('A vencer');
      expect(rows[2]).toContain('Dentro da validade');
    });

    it('shows an error with retry when the lots request fails', async () => {
      const fixture = TestBed.createComponent(LotsTabComponent);
      fixture.detectChanges();
      const lots = http.match(byUrl('/api/inventory/lots'));
      lots[0].flush(null, { status: 500, statusText: 'Server Error' });
      lots[1].flush(page([]));
      await settle(fixture);

      expect(text(fixture)).toContain('Não foi possível carregar os lotes');
      expect(text(fixture)).not.toContain('Nenhum lote cadastrado');
    });
  });

  describe('reorder-tab and alerts-panel', () => {
    async function load(alertas: InventoryAlertsService) {
      const loading = alertas.loadAll();
      http.expectOne(byUrl('/api/inventory/alerts/expiring-lots')).flush([]);
      http.expectOne(byUrl('/api/inventory/alerts/low-stock')).flush([
        { productId: PRODUCT, warehouseId: 'w', available: 1, reorderPoint: 5, suggestedQuantity: 20 }]);
      await loading;
    }

    it('creates the purchase request and then shows it as requested in both surfaces', async () => {
      const alertas = TestBed.inject(InventoryAlertsService);
      await load(alertas);
      const tab = TestBed.createComponent(ReorderTabComponent);
      const panel = TestBed.createComponent(AlertsPanelComponent);
      tab.detectChanges();
      panel.detectChanges();
      expect(text(tab)).toContain('Criar requisição');

      const button = Array.from((tab.nativeElement as HTMLElement).querySelectorAll('button'))
        .find(b => b.textContent?.includes('Criar requisição') && !b.textContent.includes('('))!;
      button.click();
      const post = http.expectOne('/api/purchasing/requests');
      expect(post.request.body).toEqual({ origin: 'MIN_STOCK_TRIGGER', items: [{ productId: PRODUCT, quantity: 20 }] });
      post.flush({ id: 'r1' }, { status: 201, statusText: 'Created' });
      await settle(tab);
      panel.detectChanges();

      expect(text(tab)).toContain('Requisição criada');
      expect(text(panel)).toContain('Pedido');
    });

    it('keeps the button available when the request fails', async () => {
      const alertas = TestBed.inject(InventoryAlertsService);
      await load(alertas);
      const panel = TestBed.createComponent(AlertsPanelComponent);
      panel.detectChanges();

      const button = Array.from((panel.nativeElement as HTMLElement).querySelectorAll('button')).find(b => b.textContent?.includes('Pedir'))!;
      button.click();
      http.expectOne('/api/purchasing/requests').flush({ message: 'x' }, { status: 400, statusText: 'Bad Request' });
      await settle(panel);

      expect(text(panel)).toContain('Pedir');
      expect(text(panel)).not.toContain('Pedido');
    });

    it('shows an error with retry for each alert endpoint that fails', async () => {
      const alertas = TestBed.inject(InventoryAlertsService);
      const loading = alertas.loadAll();
      http.expectOne(byUrl('/api/inventory/alerts/expiring-lots')).flush(null, { status: 500, statusText: 'Server Error' });
      http.expectOne(byUrl('/api/inventory/alerts/low-stock')).flush(null, { status: 500, statusText: 'Server Error' });
      await loading;
      const panel = TestBed.createComponent(AlertsPanelComponent);
      const tab = TestBed.createComponent(ReorderTabComponent);
      panel.detectChanges();
      tab.detectChanges();

      expect(text(panel)).toContain('alertas de estoque baixo');
      expect(text(panel)).toContain('alertas de validade');
      expect(text(panel)).not.toContain('Nenhum alerta no momento');
      expect(text(tab)).toContain('Não foi possível carregar as sugestões de reposição');
    });
  });
});
