import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { AuthService } from '../../../core/services/auth.service';
import { InboundNfeListItem, NfeService } from '../../../core/services/nfe.service';
import { ToastService } from '../../../core/services/toast.service';
import { ConfirmReceiptModalComponent } from './confirm-receipt-modal.component';
import { ImportXmlModalComponent } from './import-xml-modal.component';
import { ManifestationModalComponent } from './manifestation-modal.component';
import { ManualEntryModalComponent } from './manual-entry-modal.component';

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
    async function confirmModal(known: 'manual' | 'none') {
      if (known === 'manual') {
        const manual = TestBed.createComponent(ManualEntryModalComponent).componentInstance;
        manual.chave.set(KEY); manual.numero.set('777'); manual.emissao.set('2026-10-01T10:00');
        manual.cnpj.set('12345678000195'); manual.fornecedor.set('Fornecedor Alfa');
        manual.alterar(0, { description: 'Parafuso', quantity: 10, unitValue: 15 });
        manual.adicionar();
        manual.alterar(1, { description: 'Porca', quantity: 5, unitValue: 3 });
        const entering = manual.enviar();
        http.expectOne('/api/nfe/inbound').flush(summary());
        await flush();
        http.expectOne('/api/nfe/inbound').flush([row()]);
        await entering;
      }
      const fixture = TestBed.createComponent(ConfirmReceiptModalComponent);
      fixture.componentRef.setInput('nfe', row());
      fixture.detectChanges();
      return fixture.componentInstance;
    }

    it('lists the items of a manually entered note and sends one line per item, in order', async () => {
      const modal = await confirmModal('manual');
      expect(modal.linhas().map(l => l.description)).toEqual(['Parafuso', 'Porca']);
      modal.alterar(0, { productId: PRODUCT });
      modal.alterar(1, { productId: 'prod-2', receivedQty: 4 });

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

      expect(service.inboundNfes()[0].status).toBe('CONFIRMED');
    });

    it('blocks submission while an item has no product chosen (BR4)', async () => {
      const modal = await confirmModal('manual');
      modal.alterar(0, { productId: PRODUCT });

      await modal.confirmar();

      http.expectNone('/api/nfe/inbound/i1/confirm-receipt');
      expect(modal.tentouEnviar()).toBeTrue();
    });

    it('lets the user build the lines when the note items are unknown', async () => {
      const modal = await confirmModal('none');
      expect(modal.itensConhecidos()).toBeFalse();
      modal.adicionar();
      expect(modal.linhas().length).toBe(2);
      modal.remover(1);
      expect(modal.linhas().length).toBe(1);
    });

    it('keeps the modal open with the backend message when refused (size mismatch / already confirmed)', async () => {
      const modal = await confirmModal('none');
      const closed = jasmine.createSpy('closed');
      modal.closed.subscribe(closed);
      modal.alterar(0, { productId: PRODUCT, orderedQty: 10, receivedQty: 10 });

      const confirming = modal.confirmar();
      http.expectOne('/api/nfe/inbound/i1/confirm-receipt')
        .flush({ message: 'Conference result must have exactly one entry per NFe item (expected 2, got 1)' }, BAD_REQUEST);
      await confirming;

      expect(modal.erro()).toContain('expected 2, got 1');
      expect(modal.enviando()).toBeFalse();
      expect(closed).not.toHaveBeenCalled();
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
