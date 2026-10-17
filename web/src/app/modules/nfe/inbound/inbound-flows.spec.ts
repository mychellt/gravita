import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { AuthService } from '../../../core/services/auth.service';
import { InboundConference, InboundConferenceItem, InboundNfeListItem, NfeService } from '../../../core/services/nfe.service';
import { ToastService } from '../../../core/services/toast.service';
import { ConfirmReceiptModalComponent } from './confirm-receipt-modal.component';
import { ImportXmlModalComponent } from './import-xml-modal.component';
import { ManifestationModalComponent } from './manifestation-modal.component';
import { ManualEntryModalComponent } from './manual-entry-modal.component';
import { RegisterProductModalComponent } from './register-product-modal.component';

const COMPANY = 'company-1';
const KEY = '3'.repeat(44);
const PRODUCT = '0a1b2c3d-1111-2222-3333-444444444444';
const BAD_REQUEST = { status: 400, statusText: 'Bad Request' };
const flush = () => new Promise(resolve => setTimeout(resolve));

const row = (over: Partial<InboundNfeListItem> = {}): InboundNfeListItem => ({
  id: 'i1', series: '1', number: '777', supplierName: 'Fornecedor Alfa', issuedAt: '2026-10-01T00:00:00Z',
  totalValue: 165, status: 'PENDING_CONFERENCE', ...over,
});
const summary = (over = {}) => ({ id: 'i1', accessKey: KEY, supplierName: 'Fornecedor Alfa', itemCount: 2, totalValue: 165, status: 'PENDING_CONFERENCE', ...over });

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

    it('tells the user the NF-e cannot be imported when its access key already exists', async () => {
      const modal = TestBed.createComponent(ImportXmlModalComponent).componentInstance;
      const closed = jasmine.createSpy('closed');
      modal.closed.subscribe(closed);
      modal.arquivo.set(new File(['x'], 'repetida.xml'));

      const sending = modal.enviar();
      http.expectOne('/api/nfe/inbound/import-xml').flush(
        { message: 'An NFe with access key 3524 has already been imported', code: 'NFE_ALREADY_IMPORTED' },
        { status: 409, statusText: 'Conflict' });
      await sending;

      expect(modal.erro()).toBe('Não é possível importar esta NF-e: já existe uma NF-e importada com a mesma chave de acesso.');
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

  describe('confirm receipt', () => {
    const resolvedItem = (over: Partial<InboundConferenceItem> = {}): InboundConferenceItem => ({
      index: 0, supplierProductCode: 'SKU-1', description: 'Parafuso', ncm: '73181500', unit: 'UN', quantity: 10, unitValue: 1.5,
      resolved: true, productId: PRODUCT, productLabel: 'PARAF-M8', lastPurchaseUnitValue: 1.5, lastPurchaseAt: '2026-09-01T00:00:00Z',
      priceDelta: 0, ...over,
    });
    const unresolvedItem = (over: Partial<InboundConferenceItem> = {}): InboundConferenceItem => resolvedItem({
      index: 1, supplierProductCode: 'SKU-2', description: 'Porca', ncm: '73181600', unit: 'PC', quantity: 5, unitValue: 3,
      resolved: false, productId: null, productLabel: null, lastPurchaseUnitValue: null, lastPurchaseAt: null, priceDelta: null, ...over,
    });
    const conference = (items: InboundConferenceItem[], supplierResolved = true): InboundConference =>
      ({ inboundNfeId: 'i1', supplierResolved, supplierId: supplierResolved ? 'sup-1' : null, items });

    async function open(items: InboundConferenceItem[], supplierResolved = true) {
      const fixture = TestBed.createComponent(ConfirmReceiptModalComponent);
      fixture.componentRef.setInput('nfe', row());
      fixture.detectChanges();
      http.expectOne('/api/nfe/inbound/i1/conference').flush(conference(items, supplierResolved));
      await flush();
      return fixture.componentInstance;
    }

    it('pre-fills the product of an item that already has a link, with no search (BR3)', async () => {
      const modal = await open([resolvedItem()]);

      expect(modal.linhas().map(l => l.item.productId)).toEqual([PRODUCT]);
      expect(modal.linhas()[0].orderedQty).toBe(10);
    });

    it('sends one line per item, in order, each with the linked product', async () => {
      const second = resolvedItem({ index: 1, productId: 'prod-2', productLabel: 'PORCA', quantity: 5 });
      const modal = await open([resolvedItem(), second]);
      modal.alterar(1, { receivedQty: 4 });

      const confirming = modal.confirmar();
      const post = http.expectOne('/api/nfe/inbound/i1/confirm-receipt');
      expect(post.request.body).toEqual({
        conferenceResult: [
          { itemRef: PRODUCT, orderedQty: 10, receivedQty: 10 },
          { itemRef: 'prod-2', orderedQty: 5, receivedQty: 4 },
        ],
      });
      post.flush(summary({ status: 'CONFIRMED' }));
      await confirming;

      expect(toast.success).toHaveBeenCalled();
    });

    it('shows both prices and the delta when the NF-e price differs from the last purchase (BR7)', async () => {
      const modal = await open([resolvedItem({ unitValue: 1.8, lastPurchaseUnitValue: 1.5, priceDelta: 0.3 })]);

      expect(modal.houveVariacao(modal.linhas()[0].item)).toBeTrue();
      expect(modal.variacao(0.3)).toContain('0,30');
      expect(modal.variacao(-1)).toMatch(/^−/);
      expect(modal.valor(1.5)).toContain('1,50');
    });

    it('shows no variance when the price is unchanged', async () => {
      const modal = await open([resolvedItem()]);

      expect(modal.houveVariacao(modal.linhas()[0].item)).toBeFalse();
    });

    it('blocks submission while an item has no linked product (BR2)', async () => {
      const modal = await open([resolvedItem(), unresolvedItem()]);

      await modal.confirmar();

      http.expectNone('/api/nfe/inbound/i1/confirm-receipt');
      expect(modal.tentouEnviar()).toBeTrue();
    });

    it('blocks submission while the supplier is not registered (BR1)', async () => {
      const modal = await open([resolvedItem()], false);

      await modal.confirmar();

      http.expectNone('/api/nfe/inbound/i1/confirm-receipt');
      expect(modal.conferencia()!.supplierResolved).toBeFalse();
    });

    it('links an existing product picked for an unresolved item and reloads the resolution (BR5)', async () => {
      const modal = await open([unresolvedItem()]);
      modal.alterar(1, { pick: 'prod-9' });

      const linking = modal.vincular(modal.linhas()[0]);
      const post = http.expectOne('/api/nfe/inbound/i1/items/1/link-product');
      expect(post.request.body).toEqual({ productId: 'prod-9' });
      post.flush({});
      await flush();
      http.expectOne('/api/nfe/inbound/i1/conference')
        .flush(conference([unresolvedItem({ resolved: true, productId: 'prod-9', productLabel: 'PRODUTO-9' })]));
      await linking;
      await flush();

      expect(modal.linhas()[0].item.productId).toBe('prod-9');
      expect(modal.linhas()[0].pick).toBe('');
    });

    it('keeps the quantities the operator typed when the resolution is reloaded', async () => {
      const modal = await open([unresolvedItem()]);
      modal.alterar(1, { pick: 'prod-9', receivedQty: 3 });

      const linking = modal.vincular(modal.linhas()[0]);
      http.expectOne('/api/nfe/inbound/i1/items/1/link-product').flush({});
      await flush();
      http.expectOne('/api/nfe/inbound/i1/conference')
        .flush(conference([unresolvedItem({ resolved: true, productId: 'prod-9', productLabel: 'PRODUTO-9' })]));
      await linking;
      await flush();

      expect(modal.linhas()[0].receivedQty).toBe(3);
    });

    it('shows the backend message when linking is refused', async () => {
      const modal = await open([unresolvedItem()]);
      modal.alterar(1, { pick: 'prod-9' });

      const linking = modal.vincular(modal.linhas()[0]);
      http.expectOne('/api/nfe/inbound/i1/items/1/link-product')
        .flush({ message: 'Item 2 is already linked to a product' }, BAD_REQUEST);
      await linking;

      expect(modal.registrandoErro()).toContain('already linked');
    });

    it('keeps the modal open with the backend message when refused (supplier / item / size mismatch)', async () => {
      const modal = await open([resolvedItem()]);
      const closed = jasmine.createSpy('closed');
      modal.closed.subscribe(closed);

      const confirming = modal.confirmar();
      http.expectOne('/api/nfe/inbound/i1/confirm-receipt')
        .flush({ message: 'Link a product to every NF-e item before confirming the receipt; unresolved: item 2 (SKU-2)' }, BAD_REQUEST);
      await confirming;

      expect(modal.erro()).toContain('unresolved: item 2');
      expect(modal.enviando()).toBeFalse();
      expect(closed).not.toHaveBeenCalled();
    });

    it('offers a retry when the conference cannot be loaded', async () => {
      const fixture = TestBed.createComponent(ConfirmReceiptModalComponent);
      fixture.componentRef.setInput('nfe', row());
      fixture.detectChanges();
      http.expectOne('/api/nfe/inbound/i1/conference').flush({}, { status: 500, statusText: 'Server Error' });
      await flush();

      expect(fixture.componentInstance.carregamento()).toBe('error');
    });
  });

  describe('register product from an NF-e item', () => {
    const item: InboundConferenceItem = {
      index: 1, supplierProductCode: 'SKU-2', description: 'Porca Sextavada M8', ncm: '73181600', unit: 'PC', quantity: 5, unitValue: 3,
      resolved: false, productId: null, productLabel: null, lastPurchaseUnitValue: null, lastPurchaseAt: null, priceDelta: null,
    };

    function open() {
      const fixture = TestBed.createComponent(RegisterProductModalComponent);
      fixture.componentRef.setInput('inboundNfeId', 'i1');
      fixture.componentRef.setInput('item', item);
      fixture.detectChanges();
      return fixture.componentInstance;
    }

    function completed() {
      const modal = open();
      modal.unidadeVenda.set('PC');
      modal.origem.set(0);
      modal.cfopVenda.set('5102');
      modal.precoVenda.set(6.9);
      modal.uf.set('sp');
      modal.cstCsosn.set('102');
      return modal;
    }

    it('pre-fills description, NCM and purchase unit from the NF-e item, leaving fiscal and pricing fields empty (BR4)', () => {
      const modal = open();

      expect(modal.codigo()).toBe('Porca Sextavada M8');
      expect(modal.ncm()).toBe('73181600');
      expect(modal.unidadeCompra()).toBe('PC');
      expect(modal.origem()).toBeNull();
      expect(modal.cfopVenda()).toBe('');
      expect(modal.cstCsosn()).toBe('');
      expect(modal.precoVenda()).toBeNull();
    });

    it('requires the fiscal-output and pricing fields before posting', async () => {
      const modal = open();

      await modal.salvar();

      http.expectNone('/api/nfe/inbound/i1/items/1/register-product');
      expect(modal.tentouEnviar()).toBeTrue();
      expect(modal.origemValida()).toBeFalse();
      expect(modal.cfopValido()).toBeFalse();
      expect(modal.cstValido()).toBeFalse();
      expect(modal.precoValido()).toBeFalse();
    });

    it('posts the product to the item endpoint so the backend also creates the link, then emits registered', async () => {
      const modal = completed();
      const registered = jasmine.createSpy('registered');
      modal.registered.subscribe(registered);

      const saving = modal.salvar();
      const post = http.expectOne('/api/nfe/inbound/i1/items/1/register-product');
      expect(post.request.body).toEqual({
        internalCode: 'Porca Sextavada M8', type: 'SIMPLE', ncm: '73181600', origin: 0,
        defaultCfopByOperation: { VENDA: '5102' }, cstCsosnByState: { SP: '102' },
        basePrice: 6.9, purchaseUnit: 'PC', saleUnit: 'PC',
      });
      post.flush({});
      await saving;

      expect(registered).toHaveBeenCalled();
      expect(toast.success).toHaveBeenCalled();
    });

    it('keeps the form open with the backend message when refused', async () => {
      const modal = completed();
      const registered = jasmine.createSpy('registered');
      modal.registered.subscribe(registered);

      const saving = modal.salvar();
      http.expectOne('/api/nfe/inbound/i1/items/1/register-product')
        .flush({ message: 'Barcode is already registered' }, BAD_REQUEST);
      await saving;

      expect(modal.erro()).toContain('Barcode');
      expect(modal.enviando()).toBeFalse();
      expect(registered).not.toHaveBeenCalled();
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
