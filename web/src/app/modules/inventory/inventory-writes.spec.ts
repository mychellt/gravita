import { HttpRequest, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AuthService } from '../../core/services/auth.service';
import { InventoryChangesService } from '../../core/services/inventory-changes.service';
import { DEFAULT_WAREHOUSE_ID } from '../../core/services/inventory-api.service';
import { ToastService } from '../../core/services/toast.service';
import { AdjustModalComponent } from './adjust-modal.component';
import { CountsTabComponent } from './counts-tab.component';
import { InventoryComponent } from './inventory.component';
import { ProductDetailModalComponent } from './product-detail-modal.component';
import { ReservationsTabComponent } from './reservations-tab.component';

const PRODUCT = '0a1b2c3d-1111-2222-3333-444444444444';
const USER = '9f9f9f9f-0000-0000-0000-000000000001';
const page = <T>(content: T[], over = {}) => ({ content, page: 0, size: 20, totalElements: content.length, totalPages: 1, ...over });
const text = (fixture: ComponentFixture<unknown>) => (fixture.nativeElement as HTMLElement).textContent ?? '';
const byUrl = (url: string) => (r: HttpRequest<unknown>) => r.url === url;
const button = (fixture: ComponentFixture<unknown>, label: string) =>
  Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('button')).find(b => b.textContent?.includes(label))!;
const FAIL = { status: 500, statusText: 'Server Error' };

describe('Inventory writes backed by the API', () => {
  let http: HttpTestingController;
  let toast: ToastService;
  let changes: InventoryChangesService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(), provideHttpClientTesting(),
        { provide: AuthService, useValue: { currentUser: signal({ name: 'Ana', email: 'Ana@Empresa.com', profile: null, companyId: null }) } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    toast = TestBed.inject(ToastService);
    changes = TestBed.inject(InventoryChangesService);
    spyOn(toast, 'danger');
    spyOn(toast, 'success');
  });
  afterEach(() => http.verify());

  async function settle(fixture: ComponentFixture<unknown>) {
    await fixture.whenStable();
    await new Promise(resolve => setTimeout(resolve));
    fixture.detectChanges();
  }

  /** Answers the lookup that turns the logged e-mail into the user id the write endpoints require. */
  async function answerUserLookup() {
    await new Promise(resolve => setTimeout(resolve));
    http.expectOne('/api/users').flush([
      { id: 'other', email: 'x@y.com' }, { id: USER, email: 'ana@empresa.com' }]);
    await new Promise(resolve => setTimeout(resolve));
  }

  describe('reservations-tab', () => {
    const reservation = (over = {}) => ({
      id: 'r1', orderRef: 'abcdef12-0000-0000-0000-000000000000', productId: PRODUCT, productCode: 'SKU-9',
      warehouseId: DEFAULT_WAREHOUSE_ID, quantity: 4, status: 'ACTIVE', ...over });

    it('loads active reservations from the server and shows a loading indicator first', async () => {
      const fixture = TestBed.createComponent(ReservationsTabComponent);
      fixture.detectChanges();
      expect(text(fixture)).toContain('Carregando reservas');

      const request = http.expectOne(byUrl('/api/inventory/reservations'));
      expect(request.request.params.get('status')).toBe('ACTIVE');
      request.flush(page([reservation()]));
      await settle(fixture);

      expect(text(fixture)).toContain('SKU-9');
      expect(text(fixture)).toContain('abcdef12');
    });

    it('keeps the row until DELETE succeeds, then refetches so it disappears', async () => {
      const fixture = TestBed.createComponent(ReservationsTabComponent);
      fixture.detectChanges();
      http.expectOne(byUrl('/api/inventory/reservations')).flush(page([reservation()]));
      await settle(fixture);

      button(fixture, 'Liberar').click();
      const del = http.expectOne('/api/inventory/reservations/r1');
      expect(del.request.method).toBe('DELETE');
      await settle(fixture);
      expect(text(fixture)).toContain('SKU-9');
      expect(text(fixture)).toContain('Liberando');

      del.flush(null, { status: 204, statusText: 'No Content' });
      await new Promise(resolve => setTimeout(resolve));
      fixture.detectChanges();
      http.expectOne(byUrl('/api/inventory/reservations')).flush(page([]));
      await settle(fixture);

      expect(text(fixture)).not.toContain('SKU-9');
      expect(text(fixture)).toContain('Nenhuma reserva');
      expect(toast.success).toHaveBeenCalled();
    });

    it('keeps the row and reports the server message when DELETE fails', async () => {
      const fixture = TestBed.createComponent(ReservationsTabComponent);
      fixture.detectChanges();
      http.expectOne(byUrl('/api/inventory/reservations')).flush(page([reservation()]));
      await settle(fixture);

      button(fixture, 'Liberar').click();
      http.expectOne('/api/inventory/reservations/r1')
        .flush({ message: 'Reservation already consumed' }, { status: 422, statusText: 'Unprocessable' });
      await settle(fixture);

      expect(text(fixture)).toContain('SKU-9');
      expect(toast.danger).toHaveBeenCalledWith('Reservation already consumed');
      expect(button(fixture, 'Liberar').disabled).toBeFalse();
    });

    it('shows an error with retry instead of an empty table when the list fails', async () => {
      const fixture = TestBed.createComponent(ReservationsTabComponent);
      fixture.detectChanges();
      http.expectOne(byUrl('/api/inventory/reservations')).flush(null, FAIL);
      await settle(fixture);

      expect(text(fixture)).toContain('Não foi possível carregar as reservas');
      expect(text(fixture)).not.toContain('Nenhuma reserva');
      button(fixture, 'Tentar novamente').click();
      http.expectOne(byUrl('/api/inventory/reservations')).flush(page([]));
      await settle(fixture);
      expect(text(fixture)).toContain('Nenhuma reserva');
    });

    it('refetches when stock changes elsewhere', async () => {
      const fixture = TestBed.createComponent(ReservationsTabComponent);
      fixture.detectChanges();
      http.expectOne(byUrl('/api/inventory/reservations')).flush(page([]));
      await settle(fixture);

      changes.stockChanged();
      fixture.detectChanges();
      http.expectOne(byUrl('/api/inventory/reservations')).flush(page([]));
    });
  });

  describe('counts-tab', () => {
    const count = (status: string, lines: object[], over = {}) => ({
      id: 'c1234567-0000-0000-0000-000000000000', scope: 'PARTIAL_BY_GROUP', productGroupId: 'Bebidas',
      warehouseId: DEFAULT_WAREHOUSE_ID, status, startedBy: USER, startedAt: '2026-06-10T12:00:00Z', lines, ...over });
    const line = (counted: number | null, system = 10) => ({
      productId: PRODUCT, productCode: 'SKU-9', systemQuantity: system, countedQuantity: counted,
      divergence: counted === null ? null : counted - system });

    async function open(status: string, lines: object[]) {
      const fixture = TestBed.createComponent(CountsTabComponent);
      fixture.detectChanges();
      http.expectOne(byUrl('/api/inventory/counts')).flush(page([count(status, lines)]));
      await settle(fixture);
      return fixture;
    }

    it('shows a loading indicator, then the counts from the server', async () => {
      const fixture = TestBed.createComponent(CountsTabComponent);
      fixture.detectChanges();
      expect(text(fixture)).toContain('Carregando contagens');
      http.expectOne(byUrl('/api/inventory/counts')).flush(page([count('IN_PROGRESS', [line(null)])]));
      await settle(fixture);

      expect(text(fixture)).toContain('Parcial — Bebidas');
      expect(text(fixture)).toContain('0 / 1');
    });

    it('shows an error with retry when the list fails', async () => {
      const fixture = TestBed.createComponent(CountsTabComponent);
      fixture.detectChanges();
      http.expectOne(byUrl('/api/inventory/counts')).flush(null, FAIL);
      await settle(fixture);

      expect(text(fixture)).toContain('Não foi possível carregar as contagens');
      expect(text(fixture)).not.toContain('Nenhuma contagem aberta');
    });

    it('starts a count in the default warehouse as the logged user, then refetches', async () => {
      const fixture = await open('APPROVED', []);
      const tab = fixture.componentInstance;
      tab.abrirNova.set(true);
      tab.grupo.set(' Bebidas ');
      fixture.detectChanges();

      const starting = tab.iniciar();
      await answerUserLookup();
      const post = http.expectOne('/api/inventory/counts');
      expect(post.request.method).toBe('POST');
      expect(post.request.body).toEqual({
        scope: 'PARTIAL_BY_GROUP', productGroupId: 'Bebidas', warehouseId: DEFAULT_WAREHOUSE_ID, user: USER });
      expect(tab.abrirNova()).toBeTrue();
      post.flush({ id: 'c2' }, { status: 201, statusText: 'Created' });
      await starting;

      expect(tab.abrirNova()).toBeFalse();
      http.expectOne(byUrl('/api/inventory/counts')).flush(page([]));
    });

    it('requires a group for a partial count and sends none for a total one', async () => {
      const fixture = await open('APPROVED', []);
      const tab = fixture.componentInstance;

      await tab.iniciar();
      http.expectNone('/api/inventory/counts');
      expect(tab.tentouIniciar()).toBeTrue();

      tab.escopo.set('TOTAL');
      const starting = tab.iniciar();
      await answerUserLookup();
      const post = http.expectOne('/api/inventory/counts');
      expect(post.request.body.scope).toBe('TOTAL');
      expect(post.request.body.productGroupId).toBeNull();
      post.flush({ id: 'c2' }, { status: 201, statusText: 'Created' });
      await starting;
      http.expectOne(byUrl('/api/inventory/counts')).flush(page([]));
    });

    it('keeps the new-count dialog open and reports the server message when starting fails', async () => {
      const fixture = await open('APPROVED', []);
      const tab = fixture.componentInstance;
      tab.grupo.set('Bebidas');

      const starting = tab.iniciar();
      await answerUserLookup();
      http.expectOne('/api/inventory/counts').flush({ message: 'No stock' }, { status: 422, statusText: 'Unprocessable' });
      await starting;

      expect(toast.danger).toHaveBeenCalledWith('No stock');
    });

    it('submits only the typed quantities to /submit and refetches', async () => {
      const fixture = await open('IN_PROGRESS', [line(null)]);
      const tab = fixture.componentInstance;
      tab.abrir(tab.pagina().content[0]);
      tab.registrar(PRODUCT, 7);
      fixture.detectChanges();

      const saving = tab.salvar(tab.pagina().content[0]);
      await answerUserLookup();
      const post = http.expectOne('/api/inventory/counts/c1234567-0000-0000-0000-000000000000/submit');
      expect(post.request.body).toEqual({ countedLines: [{ productId: PRODUCT, countedQuantity: 7 }], submittedBy: USER });
      post.flush({});
      await saving;

      http.expectOne(byUrl('/api/inventory/counts')).flush(page([count('PENDING_APPROVAL', [line(7)])]));
      await settle(fixture);
      expect(text(fixture)).toContain('Aprovar e ajustar');
      expect(text(fixture)).toContain('-3');
    });

    it('does not call /submit when nothing was typed', async () => {
      const fixture = await open('IN_PROGRESS', [line(null)]);
      const tab = fixture.componentInstance;
      await tab.salvar(tab.pagina().content[0]);

      http.expectNone('/api/inventory/counts/c1234567-0000-0000-0000-000000000000/submit');
      expect(toast.danger).toHaveBeenCalled();
    });

    it('approves via /approve, then tells the other tabs the stock changed and refetches the counts', async () => {
      const fixture = await open('PENDING_APPROVAL', [line(7)]);
      const tab = fixture.componentInstance;
      const before = changes.version();

      const approving = tab.aprovar(tab.pagina().content[0]);
      await answerUserLookup();
      const post = http.expectOne('/api/inventory/counts/c1234567-0000-0000-0000-000000000000/approve');
      expect(post.request.body).toEqual({ approvedBy: USER });
      expect(changes.version()).toBe(before);
      post.flush({});
      await approving;

      expect(changes.version()).toBe(before + 1);
      http.expectOne(byUrl('/api/inventory/counts')).flush(page([count('APPROVED', [line(7)])]));
    });

    it('does not announce a stock change when approval fails', async () => {
      const fixture = await open('PENDING_APPROVAL', [line(7)]);
      const tab = fixture.componentInstance;
      const before = changes.version();

      const approving = tab.aprovar(tab.pagina().content[0]);
      await answerUserLookup();
      http.expectOne('/api/inventory/counts/c1234567-0000-0000-0000-000000000000/approve')
        .flush({ message: 'Not pending' }, { status: 422, statusText: 'Unprocessable' });
      await approving;

      expect(changes.version()).toBe(before);
      expect(toast.danger).toHaveBeenCalledWith('Not pending');
    });
  });

  describe('adjust-modal', () => {
    async function pickProduct(fixture: ComponentFixture<AdjustModalComponent>) {
      const modal = fixture.componentInstance;
      modal.termo.set('SKU-9');
      const searching = modal.buscar();
      http.expectOne(r => r.url === '/api/pdv/products/search').flush([
        { productId: PRODUCT, description: 'Água 500ml', ncm: null, unitPrice: 2, availableForSale: true }]);
      await searching;
      http.expectOne(`/api/inventory/products/${PRODUCT}/balance`).flush({
        productId: PRODUCT, warehouseId: DEFAULT_WAREHOUSE_ID, onHand: 12, reserved: 0, inTransit: 0, available: 12, averageCost: 1 });
      await settle(fixture);
    }

    it('shows the server balance and the balance after the adjustment', async () => {
      const fixture = TestBed.createComponent(AdjustModalComponent);
      fixture.detectChanges();
      await pickProduct(fixture);
      fixture.componentInstance.sentido.set('saida');
      fixture.componentInstance.quantidade.set(5);
      fixture.detectChanges();

      expect(text(fixture)).toContain('Saldo atual: 12 · depois: 7');
    });

    it('POSTs a signed delta for the default warehouse, then announces the change and closes', async () => {
      const fixture = TestBed.createComponent(AdjustModalComponent);
      const closed = jasmine.createSpy('closed');
      fixture.componentInstance.closed.subscribe(closed);
      fixture.detectChanges();
      await pickProduct(fixture);
      const modal = fixture.componentInstance;
      modal.sentido.set('saida');
      modal.quantidade.set(5);
      modal.justificativa.set('  avaria  ');
      const before = changes.version();

      const saving = modal.salvar();
      await answerUserLookup();
      const post = http.expectOne('/api/inventory/adjustments');
      expect(post.request.body).toEqual({
        productId: PRODUCT, quantityDelta: -5, justification: 'avaria', warehouseId: DEFAULT_WAREHOUSE_ID, user: USER });
      expect(closed).not.toHaveBeenCalled();
      post.flush({ id: 'm1' }, { status: 201, statusText: 'Created' });
      await saving;

      expect(changes.version()).toBe(before + 1);
      expect(closed).toHaveBeenCalled();
    });

    it('stays open and reports the server message when the adjustment is refused', async () => {
      const fixture = TestBed.createComponent(AdjustModalComponent);
      const closed = jasmine.createSpy('closed');
      fixture.componentInstance.closed.subscribe(closed);
      fixture.detectChanges();
      await pickProduct(fixture);
      const modal = fixture.componentInstance;
      modal.quantidade.set(5);
      modal.justificativa.set('x');
      const before = changes.version();

      const saving = modal.salvar();
      await answerUserLookup();
      http.expectOne('/api/inventory/adjustments').flush({ message: 'Insufficient stock' }, { status: 422, statusText: 'Unprocessable' });
      await saving;

      expect(toast.danger).toHaveBeenCalledWith('Insufficient stock');
      expect(closed).not.toHaveBeenCalled();
      expect(changes.version()).toBe(before);
      expect(modal.enviando()).toBeFalse();
    });

    it('sends nothing without a product, a quantity and a justification', async () => {
      const fixture = TestBed.createComponent(AdjustModalComponent);
      fixture.detectChanges();
      await fixture.componentInstance.salvar();

      http.expectNone('/api/inventory/adjustments');
      http.expectNone('/api/users');
      expect(fixture.componentInstance.tentouSalvar()).toBeTrue();
    });

    it('does not post when the logged user cannot be identified', async () => {
      const fixture = TestBed.createComponent(AdjustModalComponent);
      fixture.detectChanges();
      await pickProduct(fixture);
      const modal = fixture.componentInstance;
      modal.quantidade.set(5);
      modal.justificativa.set('x');

      const saving = modal.salvar();
      await new Promise(resolve => setTimeout(resolve));
      http.expectOne('/api/users').flush([{ id: 'other', email: 'x@y.com' }]);
      await saving;

      http.expectNone('/api/inventory/adjustments');
      expect(toast.danger).toHaveBeenCalledWith('Não foi possível identificar o usuário logado.');
    });

    it('offers retry when the balance cannot be loaded', async () => {
      const fixture = TestBed.createComponent(AdjustModalComponent);
      fixture.detectChanges();
      const modal = fixture.componentInstance;
      modal.termo.set('SKU-9');
      const searching = modal.buscar();
      http.expectOne(r => r.url === '/api/pdv/products/search').flush([
        { productId: PRODUCT, description: 'Água', ncm: null, unitPrice: 2, availableForSale: true }]);
      await searching;
      http.expectOne(`/api/inventory/products/${PRODUCT}/balance`).flush(null, FAIL);
      await settle(fixture);

      expect(text(fixture)).toContain('Não foi possível carregar o saldo');
      button(fixture, 'Tentar novamente').click();
      http.expectOne(`/api/inventory/products/${PRODUCT}/balance`).flush(null, FAIL);
    });
  });

  describe('product-detail-modal', () => {
    function create() {
      const fixture = TestBed.createComponent(ProductDetailModalComponent);
      fixture.componentInstance.produtoId = PRODUCT;
      fixture.componentInstance.nome = 'Água 500ml';
      fixture.detectChanges();
      return fixture;
    }
    const movement = (id: string, unitCost: number | null, day: number) => ({
      id, type: 'ENTRY', productId: PRODUCT, productCode: 'SKU-9', warehouseId: 'w', quantity: 3, unitCost,
      lotCode: null, serialNumbers: null, originReference: 'NF-1', justification: null, user: null,
      timestamp: `2026-06-${10 + day}T12:00:00Z` });

    it('reads balance, movements and lots from the shared endpoints, filtered by product', async () => {
      const fixture = create();
      expect(text(fixture)).toContain('Carregando saldo');

      http.expectOne(`/api/inventory/products/${PRODUCT}/balance`).flush({
        productId: PRODUCT, warehouseId: 'w', onHand: 12, reserved: 2, inTransit: 0, available: 10, averageCost: 5 });
      const movements = http.expectOne(byUrl('/api/inventory/movements'));
      expect(movements.request.params.get('productId')).toBe(PRODUCT);
      movements.flush(page([movement('m2', 6, 2), movement('m1', 4, 1), movement('m0', null, 0)]));
      const lots = http.expectOne(byUrl('/api/inventory/lots'));
      expect(lots.request.params.get('productId')).toBe(PRODUCT);
      expect(lots.request.params.get('kind')).toBe('LOT');
      lots.flush(page([{ kind: 'LOT', id: 'l1', productId: PRODUCT, productCode: 'SKU-9', warehouseId: 'w', code: 'L-77',
        expiryDate: '2027-01-31', quantity: 4, status: null }]));
      await settle(fixture);

      expect(text(fixture)).toContain('Água 500ml');
      expect(text(fixture)).toContain('L-77');
      expect(text(fixture)).toContain('NF-1');
      expect(fixture.componentInstance.custos().map(c => c.unitCost)).toEqual([4, 6]);
      expect((fixture.nativeElement as HTMLElement).querySelector('svg polyline')).not.toBeNull();
    });

    it('shows an error with retry in the section that failed, leaving the others intact', async () => {
      const fixture = create();
      http.expectOne(`/api/inventory/products/${PRODUCT}/balance`).flush(null, FAIL);
      http.expectOne(byUrl('/api/inventory/movements')).flush(page([]));
      http.expectOne(byUrl('/api/inventory/lots')).flush(page([]));
      await settle(fixture);

      expect(text(fixture)).toContain('Não foi possível carregar o saldo');
      expect(text(fixture)).toContain('Sem movimentações');
      button(fixture, 'Tentar novamente').click();
      http.expectOne(`/api/inventory/products/${PRODUCT}/balance`).flush({
        productId: PRODUCT, warehouseId: 'w', onHand: 1, reserved: 0, inTransit: 0, available: 1, averageCost: 1 });
      await settle(fixture);
      expect(text(fixture)).not.toContain('Não foi possível carregar o saldo');
    });
  });

  describe('inventory page', () => {
    it('has no Transfers tab or in-transit KPI', async () => {
      const fixture = TestBed.createComponent(InventoryComponent);
      fixture.detectChanges();
      http.expectOne(byUrl('/api/inventory/alerts/expiring-lots')).flush([]);
      http.expectOne(byUrl('/api/inventory/alerts/low-stock')).flush([]);
      http.expectOne(byUrl('/api/inventory/summary')).flush({ activeSkus: 0, belowMinimum: 0 });
      http.expectOne(byUrl('/api/inventory/movements')).flush(page([]));
      await settle(fixture);

      const labels = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('.tab')).map(t => t.textContent?.trim() ?? '');
      expect(labels.some(l => l.includes('Transferências'))).toBeFalse();
      expect(text(fixture)).not.toContain('em trânsito');
      expect(labels.length).toBe(6);
    });

    it('shows the SKU, below-minimum and expiring-lot figures from the backend', async () => {
      const fixture = TestBed.createComponent(InventoryComponent);
      fixture.detectChanges();
      http.expectOne(byUrl('/api/inventory/alerts/expiring-lots')).flush([
        { productId: PRODUCT, warehouseId: 'w', lotCode: 'L1', expiryDate: '2026-12-31', quantity: 3 },
        { productId: PRODUCT, warehouseId: 'w', lotCode: 'L2', expiryDate: '2026-12-31', quantity: 1 },
      ]);
      http.expectOne(byUrl('/api/inventory/alerts/low-stock')).flush([]);
      http.expectOne(byUrl('/api/inventory/summary')).flush({ activeSkus: 12, belowMinimum: 3 });
      http.expectOne(byUrl('/api/inventory/movements')).flush(page([]));
      await settle(fixture);

      const kpis = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('.stock-kpi'))
        .map(k => (k.textContent ?? '').replace(/\s+/g, ' ').trim());
      expect(kpis[0]).toMatch(/12\s*SKUs ativos/);
      expect(kpis[1]).toMatch(/3\s*Abaixo do mínimo/);
      expect(kpis[2]).toMatch(/2\s*Lotes a vencer/);
    });

    it('shows a dash instead of an invented number when the summary cannot be loaded', async () => {
      const fixture = TestBed.createComponent(InventoryComponent);
      fixture.detectChanges();
      http.expectOne(byUrl('/api/inventory/alerts/expiring-lots')).flush([]);
      http.expectOne(byUrl('/api/inventory/alerts/low-stock')).flush([]);
      http.expectOne(byUrl('/api/inventory/summary')).flush(null, FAIL);
      http.expectOne(byUrl('/api/inventory/movements')).flush(page([]));
      await settle(fixture);

      const kpis = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('.stock-kpi'))
        .map(k => (k.textContent ?? '').replace(/\s+/g, ' ').trim());
      expect(kpis[0]).toMatch(/—\s*SKUs ativos/);
      expect(kpis[1]).toMatch(/—\s*Abaixo do mínimo/);
    });
  });
});
