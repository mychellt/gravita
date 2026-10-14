import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { AuthService, CurrentUser } from '../../../core/services/auth.service';
import { CfopRegistryEntry } from '../../../core/services/cfop-registry.service';
import { Customer } from '../../../core/services/customer.service';
import { PaymentMethod } from '../../../core/services/payment-method.service';
import { IssueNfeRequest } from '../../../core/services/nfe.service';
import { ProductSearchResult } from '../../../core/services/product-search.service';
import { TransportModalityOption } from '../../../core/services/transport-modality.service';
import { NfeFormComponent, NfeStepNumber, STEP_PANEL_IDS } from './nfe-form.component';

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

const registry = (): CfopRegistryEntry[] => [
  { naturezaOperacao: 'VENDA', cfop: '5102', description: 'Venda de mercadoria', requiresReferencedAccessKey: false },
  { naturezaOperacao: 'DEVOLUCAO', cfop: '5202', description: 'Devolução', requiresReferencedAccessKey: true },
  { naturezaOperacao: 'NOTA_COMPLEMENTAR', cfop: '5124', description: 'Nota complementar', requiresReferencedAccessKey: true },
];

const paymentMethods = (): PaymentMethod[] => [
  { id: 'pm-pix', name: 'PIX', type: 'PIX' },
  { id: 'pm-boleto', name: 'Boleto bancário', type: 'BOLETO' },
];

const transportModalities = (): TransportModalityOption[] => [
  { id: 'tm-cif', name: 'CIF — Por conta do emitente', code: 'CIF', active: true },
  { id: 'tm-fob', name: 'FOB — Por conta do destinatário', code: 'FOB', active: true },
  { id: 'tm-none', name: 'Sem transporte', code: 'NONE', active: true },
];

const ACCESS_KEY = '35240111222333000181550010000012345123456789';

describe('NfeFormComponent', () => {
  let http: HttpTestingController;

  /** Monta o componente (já rodando ngOnInit) e responde GET /api/customers, GET /api/auxiliary/cfop-registry GET /api/auxiliary/payment-methods e GET /api/auxiliary/transport-modalities. */
  async function setup(customers: Customer[] = [customer()]) {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
    http = TestBed.inject(HttpTestingController);
    spyOn(TestBed.inject(AuthService), 'currentUser').and.returnValue({ companyId: 'company-1' } as CurrentUser);
    const fixture = TestBed.createComponent(NfeFormComponent);
    fixture.detectChanges();
    http.expectOne('/api/customers').flush(customers);
    http.expectOne('/api/auxiliary/cfop-registry').flush(registry());
    http.expectOne('/api/auxiliary/payment-methods').flush(paymentMethods());
    http.expectOne('/api/auxiliary/transport-modalities').flush(transportModalities());
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }

  const create = async (customers?: Customer[]) => (await setup(customers)).componentInstance;
  afterEach(() => http.verify());

  /** Preenche tudo que o "Emitir" exige. */
  async function fillAll(c: NfeFormComponent) {
    c.naturezaSel.set('VENDA');
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

  describe('natureza da operação', () => {
    it('populates the select from the CFOP registry, not from a hard-coded list', async () => {
      const fixture = await setup([]);

      const options = Array.from(fixture.nativeElement.querySelectorAll('[data-testid="natureza-select"] option') as NodeListOf<HTMLOptionElement>);
      expect(options.map(o => o.value)).toEqual(['', 'VENDA', 'DEVOLUCAO', 'NOTA_COMPLEMENTAR']);
      expect(options[1].textContent).toContain('5102 — Venda de mercadoria');
    });

    it('offers a retry when the registry cannot be loaded', async () => {
      TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
      http = TestBed.inject(HttpTestingController);
      const fixture = TestBed.createComponent(NfeFormComponent);
      fixture.detectChanges();
      http.expectOne('/api/customers').flush([]);
      http.expectOne('/api/auxiliary/cfop-registry').flush(null, { status: 500, statusText: 'Server Error' });
      http.expectOne('/api/auxiliary/payment-methods').flush(paymentMethods());
      http.expectOne('/api/auxiliary/transport-modalities').flush(transportModalities());
      await fixture.whenStable();
      fixture.detectChanges();

      expect(fixture.nativeElement.querySelector('[data-testid="registry-error"]')).not.toBeNull();
      fixture.componentInstance.reloadRegistry();
      http.expectOne('/api/auxiliary/cfop-registry').flush(registry());
      await fixture.whenStable();
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('[data-testid="registry-error"]')).toBeNull();
      expect(fixture.componentInstance.naturezaOptions().length).toBe(3);
    });

    it('pre-fills newly-added lines with the CFOP of the selected natureza and keeps the CFOP editable per line', async () => {
      const c = await create();

      c.naturezaSel.set('VENDA');
      c.addProduct(product({ productId: 'a' }));
      c.naturezaSel.set('DEVOLUCAO');
      c.addProduct(product({ productId: 'b' }));

      expect(c.items().map(i => i.cfop)).toEqual(['5102', '5202']);
      c.updateItem(0, { cfop: '6102' });
      expect(c.items().map(i => i.cfop)).toEqual(['6102', '5202']);
    });

    it('does not ask for the referenced access key on a regular sale', async () => {
      const fixture = await setup([]);
      fixture.componentInstance.naturezaSel.set('VENDA');
      fixture.detectChanges();

      expect(fixture.nativeElement.querySelector('[data-testid="referenced-access-key"]')).toBeNull();
    });

    for (const natureza of ['DEVOLUCAO', 'NOTA_COMPLEMENTAR'] as const) {
      it(`requires the referenced access key for ${natureza} before Emitir is enabled, and sends it`, async () => {
        const fixture = await setup();
        const c = fixture.componentInstance;
        await fillAll(c);
        c.naturezaSel.set(natureza);
        fixture.detectChanges();

        expect(fixture.nativeElement.querySelector('[data-testid="referenced-access-key"]')).not.toBeNull();
        expect(fixture.nativeElement.querySelector('[data-testid="referenced-access-key-error"]')).not.toBeNull();
        expect(c.canEmit()).toBeFalse();

        c.referencedAccessKey.set('123');
        expect(c.canEmit()).toBeFalse();

        c.referencedAccessKey.set(ACCESS_KEY.replace(/(\d{4})/g, '$1 '));
        fixture.detectChanges();
        expect(fixture.nativeElement.querySelector('[data-testid="referenced-access-key-error"]')).toBeNull();
        expect(c.canEmit()).toBeTrue();

        const emitting = c.emit();
        const req = http.expectOne('/api/nfe');
        expect(req.request.body.naturezaOperacao).toBe(natureza);
        expect(req.request.body.referencedAccessKey).toBe(ACCESS_KEY);
        req.flush({ id: 'n1', accessKey: null, documentSeries: null, documentNumber: null, status: 'QUEUED' });
        await emitting;
      });
    }

    it('drops the referenced access key from the payload when switching back to a sale', async () => {
      const c = await create();
      await fillAll(c);
      c.naturezaSel.set('DEVOLUCAO');
      c.referencedAccessKey.set(ACCESS_KEY);
      c.naturezaSel.set('VENDA');

      const emitting = c.emit();
      const req = http.expectOne('/api/nfe');
      expect(req.request.body.referencedAccessKey).toBeUndefined();
      req.flush({ id: 'n1', accessKey: null, documentSeries: null, documentNumber: null, status: 'QUEUED' });
      await emitting;
    });
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
        { productId: 'p1', descricao: 'SKU-ARROZ', ncm: '1006.30', quantidade: 1, valorUnitario: 20, desconto: 0, cfop: '' },
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
      c.naturezaSel.set('VENDA');
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

  describe('trilha → painéis', () => {
    const PANEL_BY_STEP: [NfeStepNumber, string][] = [
      [1, 'nfe-panel-natureza'], [2, 'nfe-panel-destinatario'], [3, 'nfe-panel-itens'],
      [4, 'nfe-panel-transporte'], [5, 'nfe-panel-revisao'],
    ];
    const stepButtons = (el: HTMLElement) => Array.from(el.querySelectorAll<HTMLElement>('.steps-bar .step-item'));
    const q = (el: HTMLElement, testId: string) => el.querySelector<HTMLElement>(`[data-testid="${testId}"]`);
    const emitButton = (el: HTMLElement) => Array.from(el.querySelectorAll<HTMLButtonElement>('app-page-header button'))
      .find(b => b.textContent?.includes('Emitir NF-e'))!;

    let scrolled: jasmine.Spy;
    beforeEach(() => { scrolled = spyOn(Element.prototype, 'scrollIntoView'); });

    it('maps each step to a fixed panel id, regardless of its column', () => {
      expect(Object.entries(STEP_PANEL_IDS).map(([n, id]) => [+n, id])).toEqual(PANEL_BY_STEP);
    });

    for (const [step, panelId] of PANEL_BY_STEP) {
      it(`clicking step ${step} scrolls to #${panelId} and highlights it`, async () => {
        const fixture = await setup();
        stepButtons(fixture.nativeElement)[step - 1].click();
        fixture.detectChanges();

        const panel = fixture.nativeElement.querySelector(`#${panelId}`) as HTMLElement;
        expect(panel).not.toBeNull();
        expect(scrolled).toHaveBeenCalledTimes(1);
        expect(scrolled.calls.mostRecent().object).toBe(panel);
        expect(panel.classList).toContain('panel-highlight');
        expect(fixture.nativeElement.querySelectorAll('.panel-highlight').length).toBe(1);
        expect(fixture.componentInstance.currentStep()).toBe(step);
      });
    }

    it('navigates even when the step is not done (no guard)', async () => {
      const fixture = await setup();
      expect(fixture.componentInstance.stepDone()[3]).toBeFalse();

      stepButtons(fixture.nativeElement)[2].click();
      fixture.detectChanges();

      expect(scrolled).toHaveBeenCalledTimes(1);
      expect(fixture.nativeElement.querySelector('#nfe-panel-itens').classList).toContain('panel-highlight');
    });

    it('drops the highlight after about 2 seconds', async () => {
      const fixture = await setup();
      jasmine.clock().install();
      try {
        fixture.componentInstance.goToStep(2);
        fixture.detectChanges();
        expect(fixture.nativeElement.querySelector('.panel-highlight')).not.toBeNull();

        jasmine.clock().tick(2001);
        fixture.detectChanges();
        expect(fixture.nativeElement.querySelector('.panel-highlight')).toBeNull();
      } finally {
        jasmine.clock().uninstall();
      }
    });

    it('does not change the done state of the steps', async () => {
      const c = await create();
      c.modalidade.set('FOB');
      c.goToStep(1);
      c.goToStep(5);
      expect(c.stepDone()).toEqual({ 1: false, 2: false, 3: false, 4: true, 5: false });
    });

    describe('revisão', () => {
      it('is always rendered, after Transporte, and shows no confirm control until reached', async () => {
        const fixture = await setup();
        const el: HTMLElement = fixture.nativeElement;
        const ids = Array.from(el.querySelectorAll('.nfe-form-area .form-section')).map(n => n.id);

        expect(ids).toEqual(['nfe-panel-destinatario', 'nfe-panel-itens', 'nfe-panel-transporte', 'nfe-panel-revisao']);
        expect(q(el, 'confirm-emit')).toBeNull();
      });

      it('summarizes natureza, recipient, items, totals, transport, payment and tributos', async () => {
        const fixture = await setup();
        const c = fixture.componentInstance;
        await fillAll(c);
        c.addProduct(product({ productId: 'p2', description: 'SKU-FEIJAO', unitPrice: 7.5 }));
        c.updateItem(0, { quantidade: 10, desconto: 5 });
        c.updateItem(1, { quantidade: 2 });
        c.paymentMethodId.set('pm-pix');
        fixture.detectChanges();

        const el: HTMLElement = fixture.nativeElement;
        const text = (id: string) => q(el, id)!.textContent!.trim();
        expect(text('review-natureza')).toBe('5102 — Venda de mercadoria');
        expect(text('review-recipient')).toBe('Atacadão SP Ltda · 11.222.333/0001-81');
        expect(text('review-recipient-state')).toBe('SP');
        expect(text('review-recipient-address')).toContain('São Paulo/SP');
        expect(text('review-modalidade')).toBe('CIF — Por conta do emitente');
        expect(text('review-payment')).toBe('PIX');
        const rows = Array.from(el.querySelectorAll('[data-testid="review-item"]')).map(r => r.textContent!.replace(/\s+/g, ' '));
        expect(rows.length).toBe(2);
        expect(rows[0]).toContain('SKU-ARROZ');
        expect(rows[0]).toContain('10');
        expect(rows[0]).toMatch(/20,00.*190,00/);
        expect(rows[1]).toMatch(/SKU-FEIJAO.*2.*7,50.*15,00/);
        expect(text('review-grand-total')).toContain('205,00');
        expect(el.querySelector('.review-tributos')!.textContent).toContain('ICMS (18%)');
      });

      it('shows the referenced access key when the natureza requires it', async () => {
        const fixture = await setup();
        const c = fixture.componentInstance;
        c.naturezaSel.set('DEVOLUCAO');
        c.referencedAccessKey.set(ACCESS_KEY);
        fixture.detectChanges();

        expect(q(fixture.nativeElement, 'review-access-key')!.textContent).toContain(ACCESS_KEY);
      });

      it('shows placeholders for an empty form instead of failing', async () => {
        const fixture = await setup();
        const el: HTMLElement = fixture.nativeElement;

        expect(q(el, 'review-recipient')!.textContent!.trim()).toBe('—');
        expect(q(el, 'review-payment')!.textContent!.trim()).toBe('Não informada');
        expect(el.querySelectorAll('[data-testid="review-item"]').length).toBe(0);
      });

      it('updates the summary live when an earlier field is edited after reaching it', async () => {
        const fixture = await setup();
        const c = fixture.componentInstance;
        await fillAll(c);
        c.goToStep(5);
        fixture.detectChanges();
        const el: HTMLElement = fixture.nativeElement;
        expect(q(el, 'review-grand-total')!.textContent).toContain('20,00');

        c.updateItem(0, { quantidade: 3 });
        c.modalidade.set('FOB');
        c.selectCustomer(customer({ name: 'Beta Ltda' }));
        c.naturezaSel.set('DEVOLUCAO');
        c.paymentMethodId.set('pm-boleto');
        fixture.detectChanges();

        expect(q(el, 'review-grand-total')!.textContent).toContain('60,00');
        expect(q(el, 'review-modalidade')!.textContent).toContain('FOB');
        expect(q(el, 'review-recipient')!.textContent).toContain('Beta Ltda');
        expect(q(el, 'review-natureza')!.textContent).toContain('5202');
        expect(q(el, 'review-payment')!.textContent).toContain('Boleto');
      });

      it('"Emitir NF-e" goes to the review panel and does not post until "Confirmar emissão"', async () => {
        const fixture = await setup();
        const c = fixture.componentInstance;
        await fillAll(c);
        fixture.detectChanges();
        const el: HTMLElement = fixture.nativeElement;

        emitButton(el).click();
        fixture.detectChanges();

        http.expectNone('/api/nfe');
        expect(scrolled.calls.mostRecent().object).toBe(el.querySelector('#nfe-panel-revisao') as Element);
        expect(el.querySelector('#nfe-panel-revisao')!.classList).toContain('panel-highlight');
        expect(c.submitting()).toBeFalse();
        expect(q(el, 'confirm-emit')).not.toBeNull();
      });

      it('"Emitir NF-e" stays gated by canEmit()', async () => {
        const fixture = await setup();
        fixture.componentInstance.requestEmit();
        fixture.detectChanges();

        expect(emitButton(fixture.nativeElement).disabled).toBeTrue();
        expect(scrolled).not.toHaveBeenCalled();
        expect(q(fixture.nativeElement, 'confirm-emit')).toBeNull();
      });

      it('"Confirmar emissão" posts the same payload as before', async () => {
        const fixture = await setup();
        const c = fixture.componentInstance;
        await fillAll(c);
        c.updateItem(0, { quantidade: 10, desconto: 5 });
        c.freight.set(15);
        c.grossWeight.set(42);
        c.infoAdicionais.set('  Entregar pela manhã  ');
        c.paymentMethodId.set('pm-pix');
        fixture.detectChanges();
        const el: HTMLElement = fixture.nativeElement;

        emitButton(el).click();
        fixture.detectChanges();
        http.expectNone('/api/nfe');
        q(el, 'confirm-emit')!.click();

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
          paymentMethodId: 'pm-pix',
        });
        req.flush({ id: 'n1', accessKey: null, documentSeries: '1', documentNumber: 77, status: 'QUEUED' });
        await fixture.whenStable();
        fixture.detectChanges();

        expect(c.issued()).not.toBeNull();
        expect(q(el, 'issued')).not.toBeNull();
        expect(q(el, 'confirm-emit')).toBeNull();
      });

      it('clicking the Revisão step directly reaches the same state, with confirm disabled until the form is complete', async () => {
        const fixture = await setup();
        const c = fixture.componentInstance;
        const el: HTMLElement = fixture.nativeElement;

        stepButtons(el)[4].click();
        fixture.detectChanges();
        expect(q(el, 'confirm-emit')!.hasAttribute('disabled')).toBeTrue();
        expect(q(el, 'review-incomplete')).not.toBeNull();

        await fillAll(c);
        fixture.detectChanges();
        expect(scrolled.calls.mostRecent().object).toBe(el.querySelector('#nfe-panel-revisao') as Element);
        expect(q(el, 'confirm-emit')!.hasAttribute('disabled')).toBeFalse();
        q(el, 'confirm-emit')!.click();
        http.expectOne('/api/nfe').flush({ id: 'n1', accessKey: null, documentSeries: '1', documentNumber: 1, status: 'QUEUED' });
        await fixture.whenStable();
      });

      it('shows a failed confirmation inside the review panel and keeps the confirm control for a retry', async () => {
        const fixture = await setup();
        const c = fixture.componentInstance;
        await fillAll(c);
        c.requestEmit();
        fixture.detectChanges();

        const emitting = c.emit();
        http.expectOne('/api/nfe').flush({ message: 'Recipient IE is required' }, { status: 400, statusText: 'Bad Request' });
        await emitting;
        fixture.detectChanges();

        expect(q(fixture.nativeElement, 'review-emit-error')!.textContent).toContain('Recipient IE is required');
        expect(q(fixture.nativeElement, 'confirm-emit')).not.toBeNull();
      });
    });
  });

  describe('forma de pagamento', () => {
    it('populates the select from the payment-methods endpoint, not from a hard-coded list', async () => {
      const fixture = await setup([]);

      const options = Array.from(fixture.nativeElement.querySelectorAll('[data-testid="payment-method-select"] option') as NodeListOf<HTMLOptionElement>);
      expect(options.map(o => o.value)).toEqual(['', 'pm-pix', 'pm-boleto']);
      expect(options.map(o => o.textContent?.trim())).toEqual(['Não informada', 'PIX', 'Boleto bancário']);
    });

    it('sends paymentMethodId in the POST /api/nfe payload when a method is selected', async () => {
      const c = await create();
      await fillAll(c);
      c.paymentMethodId.set('pm-pix');

      const emitting = c.emit();
      const req = http.expectOne('/api/nfe');
      expect((req.request.body as IssueNfeRequest).paymentMethodId).toBe('pm-pix');
      req.flush({ id: 'n1', accessKey: null, documentSeries: '1', documentNumber: 1, status: 'QUEUED' });
      await emitting;
    });

    it('omits paymentMethodId when no method is selected', async () => {
      const c = await create();
      await fillAll(c);

      const emitting = c.emit();
      const req = http.expectOne('/api/nfe');
      expect('paymentMethodId' in req.request.body).toBeFalse();
      req.flush({ id: 'n1', accessKey: null, documentSeries: '1', documentNumber: 1, status: 'QUEUED' });
      await emitting;
    });

    it('offers a retry when the payment methods cannot be loaded', async () => {
      TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
      http = TestBed.inject(HttpTestingController);
      const fixture = TestBed.createComponent(NfeFormComponent);
      fixture.detectChanges();
      http.expectOne('/api/customers').flush([]);
      http.expectOne('/api/auxiliary/cfop-registry').flush(registry());
      http.expectOne('/api/auxiliary/payment-methods').flush(null, { status: 500, statusText: 'Server Error' });
      http.expectOne('/api/auxiliary/transport-modalities').flush(transportModalities());
      await fixture.whenStable();
      fixture.detectChanges();

      expect(fixture.nativeElement.querySelector('[data-testid="payment-methods-error"]')).not.toBeNull();
      fixture.componentInstance.reloadPaymentMethods();
      http.expectOne('/api/auxiliary/payment-methods').flush(paymentMethods());
      await fixture.whenStable();
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('[data-testid="payment-methods-error"]')).toBeNull();
      expect(fixture.componentInstance.paymentMethods().length).toBe(2);
    });
  });

  describe('transporte', () => {
    it('populates the Modalidade select from the transport-modalities endpoint, including "Sem transporte"', async () => {
      const fixture = await setup([]);

      const options = Array.from(fixture.nativeElement.querySelectorAll('[data-testid="modalidade-select"] option') as NodeListOf<HTMLOptionElement>);
      expect(options.map(o => o.value)).toEqual(['', 'CIF', 'FOB', 'NONE']);
      expect(options.map(o => o.textContent?.trim())).toEqual(
        ['Selecione...', 'CIF — Por conta do emitente', 'FOB — Por conta do destinatário', 'Sem transporte']);
    });

    it('shows only what the registry returns: a deactivated modality is not offered', async () => {
      TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
      http = TestBed.inject(HttpTestingController);
      const fixture = TestBed.createComponent(NfeFormComponent);
      fixture.detectChanges();
      http.expectOne('/api/customers').flush([]);
      http.expectOne('/api/auxiliary/cfop-registry').flush(registry());
      http.expectOne('/api/auxiliary/payment-methods').flush(paymentMethods());
      http.expectOne('/api/auxiliary/transport-modalities').flush(transportModalities().filter(m => m.code !== 'FOB'));
      await fixture.whenStable();
      fixture.detectChanges();

      const options = Array.from(fixture.nativeElement.querySelectorAll('[data-testid="modalidade-select"] option') as NodeListOf<HTMLOptionElement>);
      expect(options.map(o => o.value)).toEqual(['', 'CIF', 'NONE']);
    });

    it('selecting an option in the DOM sets the registry code as the modalidade', async () => {
      const fixture = await setup([]);
      const select: HTMLSelectElement = fixture.nativeElement.querySelector('[data-testid="modalidade-select"]');

      select.value = 'FOB';
      select.dispatchEvent(new Event('change'));

      expect(fixture.componentInstance.modalidade()).toBe('FOB');
    });

    it('offers a retry when the modalities cannot be loaded', async () => {
      TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
      http = TestBed.inject(HttpTestingController);
      const fixture = TestBed.createComponent(NfeFormComponent);
      fixture.detectChanges();
      http.expectOne('/api/customers').flush([]);
      http.expectOne('/api/auxiliary/cfop-registry').flush(registry());
      http.expectOne('/api/auxiliary/payment-methods').flush(paymentMethods());
      http.expectOne('/api/auxiliary/transport-modalities').flush(null, { status: 500, statusText: 'Server Error' });
      await fixture.whenStable();
      fixture.detectChanges();

      expect(fixture.nativeElement.querySelector('[data-testid="modalidades-error"]')).not.toBeNull();
      fixture.componentInstance.reloadTransportModalities();
      http.expectOne('/api/auxiliary/transport-modalities').flush(transportModalities());
      await fixture.whenStable();
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('[data-testid="modalidades-error"]')).toBeNull();
      expect(fixture.componentInstance.modalidadeOptions().length).toBe(3);
    });

    it('maps the selected CIF/FOB code to transport.modality, with freight and weight', async () => {
      const c = await create();
      await fillAll(c);
      c.modalidade.set('FOB');
      c.freight.set(15);
      c.grossWeight.set(8.5);

      const emitting = c.emit();
      const req = http.expectOne('/api/nfe');
      expect(req.request.body.transport).toEqual({ modality: 'FOB', grossWeight: 8.5 });
      expect(req.request.body.freight).toBe(15);
      req.flush({ id: 'n1', accessKey: null, documentSeries: null, documentNumber: null, status: 'QUEUED' });
      await emitting;
    });

    it('"Sem transporte" does not require freight or weight and leaves the form emittable', async () => {
      const fixture = await setup();
      const c = fixture.componentInstance;
      await fillAll(c);
      c.modalidade.set('NONE');
      fixture.detectChanges();
      await fixture.whenStable();
      fixture.detectChanges();

      expect(c.freight()).toBeNull();
      expect(c.grossWeight()).toBeNull();
      expect(c.canEmit()).toBeTrue();
      expect(fixture.nativeElement.querySelector('input[name="freight"]').disabled).toBeTrue();
      expect(fixture.nativeElement.querySelector('input[name="grossWeight"]').disabled).toBeTrue();
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

    it('shows the chosen payment method name on the issued panel', async () => {
      const fixture = await setup();
      const c = fixture.componentInstance;
      await fillAll(c);
      c.paymentMethodId.set('pm-pix');

      const emitting = c.emit();
      http.expectOne('/api/nfe').flush({ id: 'n1', accessKey: null, documentSeries: '1', documentNumber: 77, status: 'QUEUED' });
      await emitting;
      fixture.detectChanges();

      const shown = fixture.nativeElement.querySelector('[data-testid="issued-payment-method"]') as HTMLElement;
      expect(shown.textContent?.trim()).toBe(paymentMethods().find(m => m.id === 'pm-pix')!.name);
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
