import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { AuthService } from '../../../core/services/auth.service';
import { InboundConference, InboundConferenceItem, InboundNfeListItem, NfeService } from '../../../core/services/nfe.service';
import { ToastService } from '../../../core/services/toast.service';
import { ConfirmReceiptModalComponent } from './confirm-receipt-modal.component';
import { InboundDetailsModalComponent } from './inbound-details-modal.component';
import { ImportXmlModalComponent } from './import-xml-modal.component';
import { ManifestationModalComponent } from './manifestation-modal.component';
import { ManualEntryModalComponent } from './manual-entry-modal.component';
import { RegisterProductModalComponent } from './register-product-modal.component';
import { RegisterSupplierModalComponent } from './register-supplier-modal.component';

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
      priceDelta: 0, barcode: null, cest: null, ...over,
    });
    const unresolvedItem = (over: Partial<InboundConferenceItem> = {}): InboundConferenceItem => resolvedItem({
      index: 1, supplierProductCode: 'SKU-2', description: 'Porca', ncm: '73181600', unit: 'PC', quantity: 5, unitValue: 3,
      resolved: false, productId: null, productLabel: null, lastPurchaseUnitValue: null, lastPurchaseAt: null, priceDelta: null, barcode: null, cest: null, ...over,
    });
    const conference = (items: InboundConferenceItem[], supplierResolved = true): InboundConference =>
      ({ inboundNfeId: 'i1', supplierDocument: '11222333000181', supplierName: 'Fornecedor Alfa', supplierResolved, supplierId: supplierResolved ? 'sup-1' : null, items });

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

    it('loads the purchase history of the linked product once and toggles it', async () => {
      const modal = await open([resolvedItem()]);
      const purchase = (id: string, supplierName: string, unitValue: number) =>
        ({ inboundNfeId: id, series: '1', number: id, supplierDocument: '1', supplierName, issuedAt: '2026-01-10T12:00:00Z', quantity: 1, unitValue });

      const opening = modal.alternarHistorico(PRODUCT);
      expect(modal.historicoAberto(PRODUCT)).toBeTrue();
      expect(modal.historicoStatus(PRODUCT)).toBe('loading');
      const request = http.expectOne(r => r.url === '/api/nfe/inbound/purchase-history');
      expect(request.request.params.get('productId')).toBe(PRODUCT);
      request.flush([purchase('a', 'Fornecedor A', 7000), purchase('b', 'Fornecedor B', 6500)]);
      await opening;

      expect(modal.historicoStatus(PRODUCT)).toBe('ready');
      expect(modal.historico(PRODUCT).map(p => p.supplierName)).toEqual(['Fornecedor A', 'Fornecedor B']);
      expect(modal.menorValor(PRODUCT)).toBe(6500);
      await modal.alternarHistorico(PRODUCT);
      expect(modal.historicoAberto(PRODUCT)).toBeFalse();
      http.expectNone(r => r.url === '/api/nfe/inbound/purchase-history');
    });

    it('lets the operator retry the purchase history after a failure', async () => {
      const modal = await open([resolvedItem()]);

      const first = modal.alternarHistorico(PRODUCT);
      http.expectOne(r => r.url === '/api/nfe/inbound/purchase-history').flush(null, { status: 500, statusText: 'Server Error' });
      await first;
      expect(modal.historicoStatus(PRODUCT)).toBe('error');

      const retry = modal.alternarHistorico(PRODUCT);
      http.expectOne(r => r.url === '/api/nfe/inbound/purchase-history').flush([]);
      await retry;
      expect(modal.historicoStatus(PRODUCT)).toBe('ready');
      expect(modal.historico(PRODUCT)).toEqual([]);
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
      expect(modal.erro()).toBe('Vincule o item sem produto a um produto do catálogo antes de confirmar.');
    });

    it('blocks submission while the supplier is not registered (BR1)', async () => {
      const modal = await open([resolvedItem()], false);

      await modal.confirmar();

      http.expectNone('/api/nfe/inbound/i1/confirm-receipt');
      expect(modal.conferencia()!.supplierResolved).toBeFalse();
      expect(modal.erro()).toBe('Cadastre o fornecedor desta NF-e (botão "Cadastrar fornecedor") antes de confirmar o recebimento.');
    });

    it('blocks submission and says so when a quantity is empty', async () => {
      const modal = await open([resolvedItem()]);
      modal.alterar(0, { receivedQty: null });

      await modal.confirmar();

      http.expectNone('/api/nfe/inbound/i1/confirm-receipt');
      expect(modal.erro()).toBe('Informe as quantidades da nota e recebida de todos os itens (zero ou mais).');
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

  describe('register the supplier during the confirmation', () => {
    const item = (over: Partial<InboundConferenceItem> = {}): InboundConferenceItem => ({
      index: 0, supplierProductCode: 'SKU-1', description: 'Parafuso', ncm: '73181500', unit: 'UN', quantity: 10, unitValue: 1.5,
      resolved: false, productId: null, productLabel: null, lastPurchaseUnitValue: null, lastPurchaseAt: null, priceDelta: null, barcode: null, cest: null, ...over,
    });
    const conf = (supplierResolved: boolean): InboundConference => ({
      inboundNfeId: 'i1', supplierDocument: '11222333000181', supplierName: 'Fornecedor Alfa', supplierResolved,
      supplierId: supplierResolved ? 'sup-1' : null, items: [item()],
    });

    async function open(supplierResolved: boolean) {
      const fixture = TestBed.createComponent(ConfirmReceiptModalComponent);
      fixture.componentRef.setInput('nfe', row());
      fixture.detectChanges();
      http.expectOne('/api/nfe/inbound/i1/conference').flush(conf(supplierResolved));
      await flush();
      fixture.detectChanges();
      return fixture;
    }
    const q = (fixture: { nativeElement: HTMLElement }, id: string) => fixture.nativeElement.querySelector<HTMLElement>(`[data-testid="${id}"]`);

    it('tells the user, up front, that the supplier and the products can be registered here', async () => {
      const fixture = await open(false);

      expect(fixture.nativeElement.textContent).toContain('pode ser cadastrado aqui mesmo');
      expect(q(fixture, 'check-supplier')!.textContent).toContain('ainda não está cadastrado');
      expect(q(fixture, 'check-supplier')!.textContent).toContain('11.222.333/0001-81');
      expect(q(fixture, 'register-supplier')).not.toBeNull();
      expect(q(fixture, 'check-items')!.textContent).toContain('0 de 1 itens vinculados');
      expect(q(fixture, 'item-needs-supplier')).not.toBeNull();
    });

    it('does not offer the action when the supplier is already registered', async () => {
      const fixture = await open(true);

      expect(q(fixture, 'register-supplier')).toBeNull();
      expect(q(fixture, 'check-supplier')!.textContent).toContain('Fornecedor cadastrado');
      expect(q(fixture, 'item-needs-supplier')).toBeNull();
    });

    it('opens the supplier form with the CNPJ and name of the NF-e; the CNPJ is read-only', async () => {
      const fixture = await open(false);

      q(fixture, 'register-supplier')!.click();
      fixture.detectChanges();
      http.expectOne('/api/lookup/cnpj/11222333000181').flush(null, { status: 404, statusText: 'Not Found' });
      await flush();
      fixture.detectChanges();

      const cnpj = (fixture.nativeElement as HTMLElement).querySelector<HTMLInputElement>('#sup-cnpj')!;
      expect(cnpj.readOnly).toBeTrue();
      expect(cnpj.value).toBe('11.222.333/0001-81');
      expect((fixture.nativeElement as HTMLElement).querySelector<HTMLInputElement>('#sup-nome')!.value).toBe('Fornecedor Alfa');
    });

    it('posts the supplier with the NF-e CNPJ and one address, then reloads the conference', async () => {
      const fixture = await open(false);
      fixture.componentInstance.cadastrandoFornecedor.set(true);
      fixture.detectChanges();
      http.expectOne('/api/lookup/cnpj/11222333000181').flush({ name: null, address: null });
      await flush();
      const modal = fixture.debugElement.query(d => d.componentInstance instanceof RegisterSupplierModalComponent).componentInstance as RegisterSupplierModalComponent;
      modal.cep.set('01001-000');
      modal.rua.set('Praça da Sé');
      modal.bairro.set('Sé');
      modal.cidade.set('São Paulo');
      modal.uf.set('sp');

      const saving = modal.salvar();
      const post = http.expectOne('/api/suppliers');
      expect(post.request.body).toEqual({
        document: '11222333000181', name: 'Fornecedor Alfa',
        addresses: [{ street: 'Praça da Sé', number: null, complement: null, neighborhood: 'Sé', city: 'São Paulo', state: 'SP', zipCode: '01001000' }],
      });
      post.flush({ id: 'sup-1' });
      await saving;
      http.expectOne('/api/nfe/inbound/i1/conference').flush(conf(true));
      await flush();
      fixture.detectChanges();

      expect(fixture.componentInstance.cadastrandoFornecedor()).toBeFalse();
      expect(fixture.componentInstance.conferencia()!.supplierResolved).toBeTrue();
      expect(q(fixture, 'register-supplier')).toBeNull();
    });

    it('requires the address before posting', async () => {
      const fixture = await open(false);
      fixture.componentInstance.cadastrandoFornecedor.set(true);
      fixture.detectChanges();
      http.expectOne('/api/lookup/cnpj/11222333000181').flush({ name: null, address: null });
      await flush();
      const modal = fixture.debugElement.query(d => d.componentInstance instanceof RegisterSupplierModalComponent).componentInstance as RegisterSupplierModalComponent;

      await modal.salvar();

      http.expectNone('/api/suppliers');
      expect(modal.tentouEnviar()).toBeTrue();
    });

    it('keeps the form open with the backend message when the registration is refused', async () => {
      const fixture = await open(false);
      fixture.componentInstance.cadastrandoFornecedor.set(true);
      fixture.detectChanges();
      http.expectOne('/api/lookup/cnpj/11222333000181').flush({ name: null, address: null });
      await flush();
      const modal = fixture.debugElement.query(d => d.componentInstance instanceof RegisterSupplierModalComponent).componentInstance as RegisterSupplierModalComponent;
      Object.entries({ cep: '01001000', rua: 'Rua A', bairro: 'Centro', cidade: 'Recife', uf: 'PE' }).forEach(([k, v]) =>
        (modal as unknown as Record<string, { set(v: string): void }>)[k].set(v));

      const saving = modal.salvar();
      http.expectOne('/api/suppliers').flush({ message: 'Supplier already registered' }, { status: 400, statusText: 'Bad Request' });
      await saving;

      expect(modal.erro()).toBe('Supplier already registered');
      expect(modal.enviando()).toBeFalse();
      expect(fixture.componentInstance.cadastrandoFornecedor()).toBeTrue();
    });

    it('pre-fills the address from the CNPJ lookup without overwriting what the operator typed', async () => {
      const fixture = await open(false);
      fixture.componentInstance.cadastrandoFornecedor.set(true);
      fixture.detectChanges();
      const modal = fixture.debugElement.query(d => d.componentInstance instanceof RegisterSupplierModalComponent).componentInstance as RegisterSupplierModalComponent;
      modal.cidade.set('Digitada');
      http.expectOne('/api/lookup/cnpj/11222333000181').flush({
        name: 'Razão Social Alfa', address: { street: 'Rua X', number: '10', neighborhood: 'Centro', city: 'Recife', state: 'PE', zipCode: '50000000' },
      });
      await flush();

      expect(modal.rua()).toBe('Rua X');
      expect(modal.uf()).toBe('PE');
      expect(modal.cidade()).toBe('Digitada');
      expect(modal.nome()).toBe('Fornecedor Alfa');
    });
  });

  describe('register product from an NF-e item', () => {
    const item: InboundConferenceItem = {
      index: 1, supplierProductCode: 'SKU-2', description: 'Porca Sextavada M8', ncm: '73181600', unit: 'PC', quantity: 5, unitValue: 3,
      resolved: false, productId: null, productLabel: null, lastPurchaseUnitValue: null, lastPurchaseAt: null, priceDelta: null,
      barcode: '7891234567895', cest: '2100100',
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

    it('pre-fills description, NCM and units from the NF-e item, leaving fiscal and pricing fields empty (BR4)', () => {
      const modal = open();

      expect(modal.codigo()).toBe('Porca Sextavada M8');
      expect(modal.ncm()).toBe('73181600');
      expect(modal.unidadeCompra()).toBe('PC');
      expect(modal.unidadeVenda()).toBe('PC');
      expect(modal.origem()).toBeNull();
      expect(modal.cfopVenda()).toBe('');
      expect(modal.cstCsosn()).toBe('');
      expect(modal.precoVenda()).toBeNull();
    });

    it('requires only the sale unit and price; fiscal-output fields are optional (managerial sale)', async () => {
      const modal = open();

      await modal.salvar();

      http.expectNone('/api/nfe/inbound/i1/items/1/register-product');
      expect(modal.tentouEnviar()).toBeTrue();
      expect(modal.origemValida()).toBeTrue();
      expect(modal.cfopValido()).toBeTrue();
      expect(modal.cstValido()).toBeTrue();
      expect(modal.precoValido()).toBeFalse();
    });

    it('pre-fills the GTIN and CEST stated in the XML and sends them with the product', async () => {
      const modal = open();
      modal.unidadeVenda.set('PC');
      modal.precoVenda.set(6.9);

      expect(modal.barcode()).toBe('7891234567895');
      expect(modal.cest()).toBe('2100100');
      const saving = modal.salvar();
      const post = http.expectOne('/api/nfe/inbound/i1/items/1/register-product');
      expect(post.request.body.barcodes).toEqual(['7891234567895']);
      expect(post.request.body.cest).toBe('2100100');
      post.flush({});
      await saving;
    });

    it('rejects a malformed GTIN or CEST but accepts them empty', () => {
      const modal = open();

      modal.barcode.set('123');
      modal.cest.set('12');
      expect(modal.barcodeValido()).toBeFalse();
      expect(modal.cestValido()).toBeFalse();
      modal.barcode.set('');
      modal.cest.set('');
      expect(modal.barcodeValido()).toBeTrue();
      expect(modal.cestValido()).toBeTrue();
    });

    it('suggests the sale price from the invoice cost and the margin', () => {
      const modal = open();

      modal.aoMudarMargem(50);

      expect(modal.precoVenda()).toBe(Math.round(Number(modal.item.unitValue) * 1.5 * 100) / 100);
    });

    it('validates fiscal fields only when filled, and CST needs its state', () => {
      const modal = open();

      modal.cfopVenda.set('51');
      modal.cstCsosn.set('102');

      expect(modal.cfopValido()).toBeFalse();
      expect(modal.ufValida()).toBeFalse();
      modal.uf.set('SP');
      expect(modal.ufValida()).toBeTrue();
    });

    it('posts without fiscal-output data when the operator leaves it empty', async () => {
      const modal = open();
      modal.unidadeVenda.set('PC');
      modal.precoVenda.set(6.9);

      const saving = modal.salvar();
      const post = http.expectOne('/api/nfe/inbound/i1/items/1/register-product');
      expect(post.request.body).toEqual({
        internalCode: 'Porca Sextavada M8', type: 'SIMPLE', ncm: '73181600', cest: '2100100', barcodes: ['7891234567895'],
        basePrice: 6.9, purchaseUnit: 'PC', saleUnit: 'PC',
      });
      post.flush({});
      await saving;
    });

    it('posts the product to the item endpoint so the backend also creates the link, then emits registered', async () => {
      const modal = completed();
      const registered = jasmine.createSpy('registered');
      modal.registered.subscribe(registered);

      const saving = modal.salvar();
      const post = http.expectOne('/api/nfe/inbound/i1/items/1/register-product');
      expect(post.request.body).toEqual({
        internalCode: 'Porca Sextavada M8', type: 'SIMPLE', ncm: '73181600', cest: '2100100', barcodes: ['7891234567895'], origin: 0,
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

  describe('details of an inbound NF-e', () => {
    const items: InboundConferenceItem[] = [{
      index: 0, supplierProductCode: 'SKU-1', description: 'Parafuso', ncm: '73181500', unit: 'UN', quantity: 10, unitValue: 2.5,
      resolved: true, productId: PRODUCT, productLabel: 'PARAF-01', lastPurchaseUnitValue: null, lastPurchaseAt: null, priceDelta: null,
      barcode: null, cest: null,
    }];

    it('loads the items of a confirmed note and shows them read-only', async () => {
      const fixture = TestBed.createComponent(InboundDetailsModalComponent);
      fixture.componentRef.setInput('nfe', row({ status: 'CONFIRMED' }));
      fixture.detectChanges();
      http.expectOne('/api/nfe/inbound/i1/conference').flush(
        { inboundNfeId: 'i1', supplierDocument: '11222333000181', supplierName: 'Fornecedor Alfa', supplierResolved: true, supplierId: 'sup-1', items });
      await flush();
      fixture.detectChanges();

      const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
      expect(fixture.componentInstance.carregamento()).toBe('ready');
      expect(text).toContain('Parafuso');
      expect(text).toContain('11.222.333/0001-81');
      expect(text).toContain('PARAF-01');
    });

    it('offers a retry when the items cannot be loaded', async () => {
      const fixture = TestBed.createComponent(InboundDetailsModalComponent);
      fixture.componentRef.setInput('nfe', row());
      fixture.detectChanges();
      http.expectOne('/api/nfe/inbound/i1/conference').flush({}, { status: 500, statusText: 'Server Error' });
      await flush();

      expect(fixture.componentInstance.carregamento()).toBe('error');
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
