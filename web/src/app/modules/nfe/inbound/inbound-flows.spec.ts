import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { AuthService } from '../../../core/services/auth.service';
import { InboundNfeConferenceView, InboundNfeListItem, NfeService } from '../../../core/services/nfe.service';
import { ToastService } from '../../../core/services/toast.service';
import { ConfirmReceiptModalComponent } from './confirm-receipt-modal.component';
import { ImportXmlModalComponent } from './import-xml-modal.component';
import { ManifestationModalComponent } from './manifestation-modal.component';
import { ManualEntryModalComponent } from './manual-entry-modal.component';
import { RegisterProductFromItemModalComponent } from './register-product-from-item-modal.component';

const COMPANY = 'company-1';
const KEY = '3'.repeat(44);
const PRODUCT = '0a1b2c3d-1111-2222-3333-444444444444';
const BAD_REQUEST = { status: 400, statusText: 'Bad Request' };
const flush = () => new Promise(resolve => setTimeout(resolve));

const row = (over: Partial<InboundNfeListItem> = {}): InboundNfeListItem => ({
  id: 'i1', series: '1', number: '777', supplierName: 'Fornecedor Alfa', issuedAt: '2026-10-01T00:00:00Z',
  totalValue: 165, status: 'PENDING_CONFERENCE', ...over,
});
const summary = (over = {}) => ({
  id: 'i1', accessKey: KEY, supplierName: 'Fornecedor Alfa', itemCount: 2, totalValue: 165, status: 'PENDING_CONFERENCE',
  supplierResolved: true, ...over,
});
const conferenceView = (over: Partial<InboundNfeConferenceView> = {}): InboundNfeConferenceView => ({
  id: 'i1', series: '1', number: '777', supplierName: 'Fornecedor Alfa', supplierResolved: true, supplierId: 'sup-1',
  items: [
    { supplierProductCode: 'SKU-1', description: 'Parafuso', ncm: '73181500', unit: 'UN', quantity: 10, unitValue: 15,
      resolvedProductId: PRODUCT, resolvedProductLabel: 'PRF-001', lastPurchaseUnitValue: 15 },
    { supplierProductCode: 'SKU-2', description: 'Porca', ncm: '73181500', unit: 'UN', quantity: 5, unitValue: 3,
      resolvedProductId: null, resolvedProductLabel: null, lastPurchaseUnitValue: null },
  ],
  ...over,
});

describe('Inbound NF-e flows', () => {
  let http: HttpTestingController;
  let service: NfeService;
  let toast: ToastService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(), provideHttpClientTesting(),
        { provide: AuthService, useValue: { currentUser: signal({ companyId: COMPANY }) } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    service = TestBed.inject(NfeService);
    toast = TestBed.inject(ToastService);
    spyOn(toast, 'success');
  });
  afterEach(() => http.verify());

  describe('import XML', () => {
    it('posts the file with the company id, refreshes the list and closes', async () => {
      const modal = TestBed.createComponent(ImportXmlModalComponent).componentInstance;
      const closed = jasmine.createSpy('closed');
      modal.closed.subscribe(closed);
      const file = new File(['<nfeProc/>'], 'nota.xml', { type: 'text/xml' });
      modal.arquivo.set(file);

      const sending = modal.enviar();
      const post = http.expectOne('/api/nfe/inbound/import-xml');
      const body = post.request.body as FormData;
      expect(body.get('companyId')).toBe(COMPANY);
      expect((body.get('xmlFile') as File).name).toBe('nota.xml');
      post.flush(summary());
      await flush();
      http.expectOne('/api/nfe/inbound').flush([row()]);
      await sending;

      expect(service.inboundNfes().map(n => n.id)).toEqual(['i1']);
      expect(closed).toHaveBeenCalled();
    });

    it('keeps the modal open and shows the backend message for a malformed XML', async () => {
      const modal = TestBed.createComponent(ImportXmlModalComponent).componentInstance;
      const closed = jasmine.createSpy('closed');
      modal.closed.subscribe(closed);
      modal.arquivo.set(new File(['x'], 'ruim.xml'));

      const sending = modal.enviar();
      http.expectOne('/api/nfe/inbound/import-xml').flush({ message: 'Malformed NFe XML' }, BAD_REQUEST);
      await sending;

      expect(modal.erro()).toBe('Malformed NFe XML');
      expect(modal.enviando()).toBeFalse();
      expect(closed).not.toHaveBeenCalled();
    });

    it('does not post without a file', async () => {
      const modal = TestBed.createComponent(ImportXmlModalComponent).componentInstance;

      await modal.enviar();

      http.expectNone('/api/nfe/inbound/import-xml');
      expect(modal.tentouEnviar()).toBeTrue();
    });
  });

  describe('manual entry', () => {
    function filled() {
      const modal = TestBed.createComponent(ManualEntryModalComponent).componentInstance;
      modal.chave.set(KEY);
      modal.numero.set('777');
      modal.emissao.set('2026-10-01T10:00');
      modal.cnpj.set('12.345.678/0001-95');
      modal.fornecedor.set('Fornecedor Alfa');
      modal.alterar(0, { description: 'Parafuso', quantity: 10, unitValue: 15, ncm: '73181500' });
      modal.frete.set(15);
      return modal;
    }

    it('requires the 44-digit access key even though there is no XML (BR1)', async () => {
      const modal = filled();
      modal.chave.set('123');

      await modal.enviar();

      http.expectNone('/api/nfe/inbound');
      expect(modal.chaveValida()).toBeFalse();
    });

    it('posts accessKey at both levels with digits-only CNPJ and computed totals', async () => {
      const modal = filled();
      const closed = jasmine.createSpy('closed');
      modal.closed.subscribe(closed);

      const sending = modal.enviar();
      const post = http.expectOne('/api/nfe/inbound');
      const body = post.request.body;
      expect(body.companyId).toBe(COMPANY);
      expect(body.accessKey).toBe(KEY);
      expect(body.manualData.accessKey).toBe(KEY);
      expect(body.manualData.supplierCnpj).toBe('12345678000195');
      expect(body.manualData.items[0]).toEqual(jasmine.objectContaining({ description: 'Parafuso', quantity: 10, unitValue: 15, totalValue: 150 }));
      expect(body.manualData.totals).toEqual(jasmine.objectContaining({ productsValue: 150, freightValue: 15, totalValue: 165 }));
      post.flush(summary({ itemCount: 1 }));
      await flush();
      http.expectOne('/api/nfe/inbound').flush([row()]);
      await sending;

      expect(closed).toHaveBeenCalled();
      expect(service.inboundNfes().length).toBe(1);
    });

    it('keeps the form and shows the backend message when refused', async () => {
      const modal = filled();
      const closed = jasmine.createSpy('closed');
      modal.closed.subscribe(closed);

      const sending = modal.enviar();
      http.expectOne('/api/nfe/inbound').flush({ message: 'Invalid CNPJ' }, BAD_REQUEST);
      await sending;

      expect(modal.erro()).toBe('Invalid CNPJ');
      expect(closed).not.toHaveBeenCalled();
    });
  });

  describe('confirm receipt (GRA-246)', () => {
    async function confirmModal(vista: InboundNfeConferenceView = conferenceView()) {
      const loading = service.load();
      http.expectOne('/api/nfe').flush([]);
      http.expectOne('/api/nfe/inbound').flush([row()]);
      await loading;

      const fixture = TestBed.createComponent(ConfirmReceiptModalComponent);
      fixture.componentRef.setInput('nfe', row());
      fixture.detectChanges();
      http.expectOne('/api/nfe/inbound/i1/conference').flush(vista);
      await flush();
      return fixture.componentInstance;
    }

    it('loads the conference view and sends one line per item, in order', async () => {
      const modal = await confirmModal();
      expect(modal.linhas().map(l => l.description)).toEqual(['Parafuso', 'Porca']);
      expect(modal.linhas()[0].resolvedProductId).toBe(PRODUCT);

      const linking = modal.vincularExistente(1, { productId: 'prod-2', description: 'Porca Sextavada', ncm: null, unitPrice: 3, availableForSale: true });
      http.expectOne('/api/nfe/inbound/i1/items/1/link-existing-product').flush(null);
      await linking;
      expect(modal.linhas()[1].resolvedProductId).toBe('prod-2');

      const confirming = modal.confirmar();
      const post = http.expectOne('/api/nfe/inbound/i1/confirm-receipt');
      expect(post.request.body).toEqual({
        conferenceResult: [
          { itemRef: PRODUCT, orderedQty: 10, receivedQty: 10 },
          { itemRef: 'prod-2', orderedQty: 5, receivedQty: 5 },
        ],
      });
      post.flush(summary({ status: 'CONFIRMED' }));
      await confirming;

      expect(service.inboundNfes()[0].status).toBe('CONFIRMED');
    });

    it('blocks confirmation while an item has no product linked yet (AC3)', async () => {
      const modal = await confirmModal();

      await modal.confirmar();

      http.expectNone('/api/nfe/inbound/i1/confirm-receipt');
      expect(modal.tentouEnviar()).toBeTrue();
    });

    it('blocks confirmation when the supplier is not registered yet (AC2)', async () => {
      const modal = await confirmModal(conferenceView({ supplierResolved: false, supplierId: null }));

      await modal.confirmar();

      http.expectNone('/api/nfe/inbound/i1/confirm-receipt');
      expect(modal.tentouEnviar()).toBeTrue();
    });

    it('resolves a line in place once a new product is registered and linked (BR4)', async () => {
      const modal = await confirmModal();

      modal.onNewProductLinked(1, { id: 'prod-3', internalCode: 'Porca Nova' });

      expect(modal.linhas()[1].resolvedProductId).toBe('prod-3');
      expect(modal.linhas()[1].resolvedProductLabel).toBe('Porca Nova');
    });

    it('flags the price variance against the link\'s last purchase price, but never blocks confirmation (AC8/BR7)', async () => {
      const modal = await confirmModal();

      expect(modal.variancia(modal.linhas()[0])).toBeNull();

      modal.alterar(0, { unitValue: 18 });
      expect(modal.variancia(modal.linhas()[0])).toBe(3);

      modal.onNewProductLinked(1, { id: 'prod-3', internalCode: 'Porca Nova' });
      expect(modal.variancia(modal.linhas()[1])).toBeNull();
    });

    it('keeps the modal open with the backend message when refused', async () => {
      const vista = conferenceView({
        items: [
          conferenceView().items[0],
          { ...conferenceView().items[1], resolvedProductId: 'prod-2', resolvedProductLabel: 'Porca', lastPurchaseUnitValue: 3 },
        ],
      });
      const modal = await confirmModal(vista);
      const closed = jasmine.createSpy('closed');
      modal.closed.subscribe(closed);

      const confirming = modal.confirmar();
      http.expectOne('/api/nfe/inbound/i1/confirm-receipt')
        .flush({ message: 'Item is not linked to a catalog product' }, BAD_REQUEST);
      await confirming;

      expect(modal.erro()).toContain('not linked');
      expect(modal.enviando()).toBeFalse();
      expect(closed).not.toHaveBeenCalled();
    });
  });

  describe('register product from an unresolved item (GRA-246 BR4)', () => {
    function modal() {
      const fixture = TestBed.createComponent(RegisterProductFromItemModalComponent);
      fixture.componentRef.setInput('item', conferenceView().items[1]);
      fixture.componentRef.setInput('inboundNfeId', 'i1');
      fixture.componentRef.setInput('itemIndex', 1);
      fixture.detectChanges();
      return fixture.componentInstance;
    }

    it('pre-fills identification fields from the item', () => {
      const c = modal();
      expect(c.internalCode()).toBe('Porca');
      expect(c.ncm()).toBe('73181500');
      expect(c.purchaseUnit()).toBe('UN');
      expect(c.averageCost()).toBe(3);
    });

    it('requires the fiscal-output and pricing fields before saving (UC-M1-11)', async () => {
      const c = modal();
      await c.salvar();
      http.expectNone('/api/nfe/inbound/i1/items/1/link-new-product');
      expect(c.tentouEnviar()).toBeTrue();
    });

    it('registers the product and links it, carrying every completed field', async () => {
      const c = modal();
      c.cfopVenda.set('5102');
      c.ufCst.set('SP');
      c.cstCsosn.set('000');
      c.icmsRate.set(18);
      c.basePrice.set(10);
      const linked = jasmine.createSpy('linked');
      c.linked.subscribe(linked);

      const saving = c.salvar();
      const post = http.expectOne('/api/nfe/inbound/i1/items/1/link-new-product');
      expect(post.request.body).toEqual(jasmine.objectContaining({
        internalCode: 'Porca', type: 'SIMPLE', ncm: '73181500',
        defaultCfopByOperation: { VENDA: '5102' }, cstCsosnByState: { SP: '000' },
        basePrice: 10, averageCost: 3,
      }));
      post.flush({ id: 'prod-3', internalCode: 'Porca' });
      await saving;

      expect(linked).toHaveBeenCalledWith({ id: 'prod-3', internalCode: 'Porca' });
    });
  });

  describe('manifestation', () => {
    it('posts the access key and type; rejects a short key without calling the API', async () => {
      const modal = TestBed.createComponent(ManifestationModalComponent).componentInstance;
      modal.chave.set('123');
      await modal.enviar();
      http.expectNone('/api/nfe/inbound/manifestation');

      modal.chave.set(KEY);
      modal.tipo.set('UNKNOWN');
      const sending = modal.enviar();
      const post = http.expectOne('/api/nfe/inbound/manifestation');
      expect(post.request.body).toEqual({ accessKey: KEY, type: 'UNKNOWN' });
      post.flush({ id: 'm1', accessKey: KEY, type: 'UNKNOWN', sefazProtocol: null, manifestedAt: '2026-10-01T00:00:00Z' });
      await sending;

      expect(toast.success).toHaveBeenCalled();
    });
  });
});
