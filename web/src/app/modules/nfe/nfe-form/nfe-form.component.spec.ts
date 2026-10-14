import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { AuthService, CurrentUser } from '../../../core/services/auth.service';
import { Customer } from '../../../core/services/customer.service';
import { IssueNfeRequest } from '../../../core/services/nfe.service';
import { ProductSearchResult } from '../../../core/services/product-search.service';
import { NfeFormComponent } from './nfe-form.component';

const customer = (over: Partial<Customer> = {}): Customer => ({
  id: 'c1', name: 'Atacadão SP Ltda', type: 'COMPANY', document: '11.222.333/0001-81', email: 'nfe@atacadao.com.br',
  ieIndicator: 'EXEMPT', finalConsumer: false, creditLimit: 1000, currentBalance: 0, status: 'REGULAR', companyId: null,
  addresses: [{ type: 'BILLING', street: 'Av. dos Bandeirantes', number: '2000', complement: null, neighborhood: 'Moema',
    city: 'São Paulo', state: 'SP', zipCode: '04578-000', isDefault: true }],
  contacts: [], priceTables: [], ...over,
});

const product = (over: Partial<ProductSearchResult> = {}): ProductSearchResult => ({
  productId: 'p1', description: 'SKU-ARROZ', ncm: '1006.30', unitPrice: 20, availableForSale: true, ...over,
});

describe('NfeFormComponent', () => {
  let http: HttpTestingController;

  /** Monta o componente (já rodando ngOnInit) e responde GET /api/customers. */
  async function setup(customers: Customer[] = [customer()]) {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
    http = TestBed.inject(HttpTestingController);
    spyOn(TestBed.inject(AuthService), 'currentUser').and.returnValue({ companyId: 'company-1' } as CurrentUser);
    const fixture = TestBed.createComponent(NfeFormComponent);
    fixture.detectChanges();
    http.expectOne('/api/customers').flush(customers);
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }

  const create = async (customers?: Customer[]) => (await setup(customers)).componentInstance;
  afterEach(() => http.verify());

  /** Preenche tudo que o "Emitir" exige. */
  async function fillAll(c: NfeFormComponent) {
    c.cfopSel.set('5102');
    c.selectCustomer(customer());
    c.addProduct(product());
    c.modalidade.set('CIF');
  }

  it('renders no hard-coded recipient, demo items or document number', async () => {
    const fixture = await setup([]);

    const text: string = fixture.nativeElement.textContent;
    expect(text).not.toContain('000235');
    expect(text).not.toContain('Atacadão');
    expect(text).not.toContain('98.765.432');
    expect(text).toContain('atribuídos na emissão');
    expect(fixture.componentInstance.items()).toEqual([]);
    expect(fixture.nativeElement.querySelector('[data-testid="tax-estimate-tag"]').textContent).toContain('Estimativa');
  });

  describe('destinatário', () => {
    it('filters the registered customers by name or document', async () => {
      const c = await create([customer(), customer({ id: 'c2', name: 'Beta Ltda', document: '99.888.777/0001-00' })]);

      c.customerQuery.set('beta');
      expect(c.customerMatches().map(m => m.id)).toEqual(['c2']);
      c.customerQuery.set('11222333');
      expect(c.customerMatches().map(m => m.id)).toEqual(['c1']);
    });

    it('selecting a customer fills the recipient from its record, read-only', async () => {
      const fixture = await setup([customer()]);

      fixture.componentInstance.selectCustomer(customer());
      fixture.detectChanges();

      const el: HTMLElement = fixture.nativeElement;
      const field = (id: string) => el.querySelector<HTMLInputElement>(`[data-testid="${id}"]`)!;
      expect(field('recipient-document').value).toBe('11.222.333/0001-81');
      expect(field('recipient-name').value).toBe('Atacadão SP Ltda');
      expect(field('recipient-address').value).toContain('São Paulo/SP');
      expect(field('recipient-document').readOnly).toBeTrue();
      expect(field('recipient-name').readOnly).toBeTrue();
      expect(field('customer-search')).toBeNull();
    });

    it('points to Settings › Clientes when nothing matches', async () => {
      const fixture = await setup([customer()]);

      fixture.componentInstance.customerQuery.set('inexistente');
      fixture.detectChanges();

      const hint: HTMLElement = fixture.nativeElement.querySelector('[data-testid="customer-none"]');
      expect(hint.querySelector('a')!.getAttribute('href')).toBe('/settings/customers');
    });

    it('blocks emission for a customer without a state (UF)', async () => {
      const c = await create();
      await fillAll(c);
      expect(c.canEmit()).toBeTrue();

      c.selectCustomer(customer({ addresses: [] }));
      expect(c.recipientIncomplete()).toBeTrue();
      expect(c.canEmit()).toBeFalse();
    });
  });

  describe('itens', () => {
    it('searches with the term and the selected customer, then adds the result as a line', async () => {
      const c = await create();
      c.selectCustomer(customer());
      c.productQuery.set('SKU');

      const searching = c.searchProducts();
      const req = http.expectOne(r => r.url === '/api/pdv/products/search');
      expect(req.request.params.get('q')).toBe('SKU');
      expect(req.request.params.get('customerId')).toBe('c1');
      req.flush([product()]);
      await searching;

      c.addProduct(c.productResults()[0]);
      expect(c.items()).toEqual([
        { productId: 'p1', descricao: 'SKU-ARROZ', ncm: '1006.30', quantidade: 1, valorUnitario: 20, desconto: 0 },
      ]);
    });

    it('never adds a product that is not available for sale, and disables it in the list', async () => {
      const fixture = await setup([]);
      const c = fixture.componentInstance;
      c.productQuery.set('SKU');
      const searching = c.searchProducts();
      http.expectOne(r => r.url === '/api/pdv/products/search')
        .flush([product({ productId: 'ok' }), product({ productId: 'off', availableForSale: false })]);
      await searching;
      fixture.detectChanges();

      const buttons = fixture.nativeElement.querySelectorAll('[data-testid="product-results"] button') as NodeListOf<HTMLButtonElement>;
      expect(buttons[0].disabled).toBeFalse();
      expect(buttons[1].disabled).toBeTrue();
      c.addProduct(c.productResults()[1]);
      expect(c.items()).toEqual([]);
    });

    it('adding the same product again raises its quantity instead of duplicating the line', async () => {
      const c = await create();
      c.addProduct(product());
      c.addProduct(product());

      expect(c.items().length).toBe(1);
      expect(c.items()[0].quantidade).toBe(2);
    });

    it('computes each line total with its discount percentage and keeps the discount in 0-100', async () => {
      const c = await create();
      c.addProduct(product({ unitPrice: 20 }));
      c.updateItem(0, { quantidade: 10, desconto: 5 });

      expect(c.baseCalculo()).toBe(190);
      c.updateItem(0, { desconto: 250 });
      expect(c.items()[0].desconto).toBe(100);
    });
  });

  describe('trilha de passos', () => {
    it('marks each step from the form state, regardless of order or clicked step', async () => {
      const c = await create();
      expect(c.stepDone()).toEqual({ 1: false, 2: false, 3: false, 4: false, 5: false });

      c.modalidade.set('FOB');
      expect(c.stepDone()).toEqual({ 1: false, 2: false, 3: false, 4: true, 5: false });

      c.currentStep.set(5);
      expect(c.stepDone()[4]).toBeTrue();
      expect(c.stepDone()[1]).toBeFalse();

      c.selectCustomer(customer());
      expect(c.stepDone()).toEqual({ 1: false, 2: true, 3: false, 4: true, 5: false });

      c.addProduct(product());
      c.cfopSel.set('5102');
      expect(c.stepDone()).toEqual({ 1: true, 2: true, 3: true, 4: true, 5: true });

      c.removeItem(0);
      expect(c.stepDone()).toEqual({ 1: true, 2: true, 3: false, 4: true, 5: false });
    });

    it('enables Emitir NF-e only when every prior step is done', async () => {
      const c = await create();
      expect(c.canEmit()).toBeFalse();
      await fillAll(c);
      expect(c.canEmit()).toBeTrue();
      c.clearCustomer();
      expect(c.canEmit()).toBeFalse();
    });
  });

  describe('emissão', () => {
    it('posts the real payload and shows the real values from the response', async () => {
      const c = await create();
      await fillAll(c);
      c.updateItem(0, { quantidade: 10, desconto: 5 });
      c.freight.set(15);
      c.grossWeight.set(42);
      c.infoAdicionais.set('  Entregar pela manhã  ');

      const emitting = c.emit();
      const req = http.expectOne('/api/nfe');
      expect(req.request.method).toBe('POST');
      expect(req.request.body as IssueNfeRequest).toEqual({
        issuerCompanyId: 'company-1',
        naturezaOperacao: 'VENDA',
        recipient: { personId: 'c1', document: '11.222.333/0001-81', personType: 'COMPANY', name: 'Atacadão SP Ltda',
          stateRegistration: 'ISENTO', state: 'SP' },
        items: [{ productId: 'p1', description: 'SKU-ARROZ', quantity: 10, unitPrice: 20, discount: 10 }],
        freight: 15,
        transport: { modality: 'CIF', grossWeight: 42 },
        additionalInfo: 'Entregar pela manhã',
      });
      req.flush({ id: 'n1', accessKey: '3526'.padEnd(44, '1'), documentSeries: '1', documentNumber: 77, status: 'QUEUED' });
      await emitting;

      expect(c.issued()).toEqual(jasmine.objectContaining({ documentNumber: 77, documentSeries: '1', status: 'QUEUED' }));
      expect(c.errorMessage()).toBeNull();
      expect(c.canEmit()).toBeFalse();
    });

    it('omits transport and freight when the modality is "sem transporte"', async () => {
      const c = await create();
      await fillAll(c);
      c.modalidade.set('NONE');
      c.freight.set(15);

      const emitting = c.emit();
      const body = http.expectOne('/api/nfe');
      expect(body.request.body.transport).toBeUndefined();
      expect(body.request.body.freight).toBeUndefined();
      body.flush({ id: 'n1', accessKey: null, documentSeries: null, documentNumber: null, status: 'QUEUED' });
      await emitting;
    });

    it('shows the backend message inline on a 400', async () => {
      const c = await create();
      await fillAll(c);

      const emitting = c.emit();
      http.expectOne('/api/nfe').flush({ message: 'Recipient IE is required for a company (CNPJ) recipient' },
        { status: 400, statusText: 'Bad Request' });
      await emitting;

      expect(c.errorMessage()).toBe('Recipient IE is required for a company (CNPJ) recipient');
      expect(c.issued()).toBeNull();
      expect(c.canEmit()).toBeTrue();
    });

    it('falls back to a generic message when the failure has no backend message', async () => {
      const c = await create();
      await fillAll(c);

      const emitting = c.emit();
      http.expectOne('/api/nfe').flush(null, { status: 500, statusText: 'Server Error' });
      await emitting;

      expect(c.errorMessage()).toContain('Não foi possível emitir');
    });

    it('does nothing while the form is incomplete', async () => {
      const c = await create();
      await c.emit();
      http.expectNone('/api/nfe');
      expect(c.issued()).toBeNull();
    });
  });
});
