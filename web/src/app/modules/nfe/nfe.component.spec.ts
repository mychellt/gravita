import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { InboundNfeListItem, NfeListItem } from '../../core/services/nfe.service';
import { ToastService } from '../../core/services/toast.service';
import { NfeComponent } from './nfe.component';

const iso = (offsetDays = 0) => new Date(Date.now() - offsetDays * 86_400_000).toISOString();

const nfe = (over: Partial<NfeListItem>): NfeListItem => ({
  id: 'n1', documentNumber: 1, documentSeries: '1', recipientName: 'Cliente A', cfop: '5102',
  emittedAt: iso(), totalValue: 100, icmsValue: 18, status: 'AUTHORIZED',
  cancellationJustification: null, cancelledAt: null, ...over,
});

const inbound = (over: Partial<InboundNfeListItem>): InboundNfeListItem => ({
  id: 'i1', series: '1', number: '777', supplierName: 'Fornecedor Alfa', issuedAt: iso(3),
  totalValue: 165, status: 'PENDING_CONFERENCE', ...over,
});

class TestNfeComponent extends NfeComponent {
  justification: string | null = 'Erro de digitação';
  saved: string[] = [];
  protected override askJustification() { return this.justification; }
  protected override saveFile(_blob: Blob, fileName: string) { this.saved.push(fileName); }
}

describe('NfeComponent', () => {
  let http: HttpTestingController;
  let toast: ToastService;

  async function create(issued: NfeListItem[] | 'fail', inboundList: InboundNfeListItem[] = []) {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    toast = TestBed.inject(ToastService);
    const component = TestBed.runInInjectionContext(() => new TestNfeComponent());
    const request = http.expectOne('/api/nfe');
    issued === 'fail' ? request.flush(null, { status: 500, statusText: 'Server Error' }) : request.flush(issued);
    http.expectOne('/api/nfe/inbound').flush(inboundList);
    await Promise.resolve();
    await new Promise(r => setTimeout(r));
    return component;
  }
  afterEach(() => http.verify());

  it('splits Emitidas (any non-cancelled status) from Canceladas, never showing a document on both', async () => {
    const c = await create([
      nfe({ id: 'a', status: 'AUTHORIZED' }), nfe({ id: 'q', status: 'QUEUED' }), nfe({ id: 'x', status: 'CANCELLED' }),
    ]);

    expect(c.filtered().map(n => n.id)).toEqual(['a', 'q']);
    c.activeTab.set('canceladas');
    expect(c.filtered().map(n => n.id)).toEqual(['x']);
  });

  it('keeps Entradas on its own data source', async () => {
    const c = await create([nfe({ id: 'a' })], [inbound({ id: 'i1' }), inbound({ id: 'i2', supplierName: 'Beta', number: '9' })]);
    c.activeTab.set('entradas');

    expect(c.filteredInbound().map(n => n.id)).toEqual(['i1', 'i2']);
    c.searchQuery.set('beta');
    expect(c.filteredInbound().map(n => n.id)).toEqual(['i2']);
  });

  it('searches by recipient, number or CFOP', async () => {
    const c = await create([
      nfe({ id: 'a', recipientName: 'Maria', documentNumber: 10, cfop: '5102' }),
      nfe({ id: 'b', recipientName: 'João', documentNumber: 20, cfop: '6102' }),
    ]);

    c.searchQuery.set('mar');
    expect(c.filtered().map(n => n.id)).toEqual(['a']);
    c.searchQuery.set('20');
    expect(c.filtered().map(n => n.id)).toEqual(['b']);
    c.searchQuery.set('6102');
    expect(c.filtered().map(n => n.id)).toEqual(['b']);
  });

  it('exposes a load error instead of an empty table', async () => {
    const c = await create('fail');

    expect(c.loadStatus()).toBe('error');
  });

  it('computes the KPIs from the fetched data', async () => {
    const c = await create([
      nfe({ id: 'a', status: 'AUTHORIZED', totalValue: 100 }),
      nfe({ id: 'b', status: 'AUTHORIZED', totalValue: 50 }),
      nfe({ id: 'q', status: 'QUEUED', totalValue: 10 }),
      nfe({ id: 'x', status: 'CANCELLED', totalValue: 70, cancelledAt: iso() }),
    ]);

    expect(c.kpis().authorizedToday).toEqual({ count: 2, total: 150 });
    expect(c.kpis().pending.count).toBe(1);
    expect(c.kpis().cancelledThisMonth).toEqual({ count: 1, total: 70 });
    expect(c.kpis().issuedThisMonth).toEqual({ count: 3, total: 160 });
  });

  it('cancels with the typed justification and moves the row to Canceladas', async () => {
    const c = await create([nfe({ id: 'a' })]);

    const cancelling = c.cancelNfe(c.filtered()[0]);
    const request = http.expectOne('/api/nfe/a/cancel');
    expect(request.request.body).toEqual({ justification: 'Erro de digitação' });
    request.flush({ id: 'a', status: 'CANCELLED', cancellationJustification: 'Erro de digitação', cancelledAt: iso() });
    await cancelling;

    expect(c.filtered()).toEqual([]);
    c.activeTab.set('canceladas');
    expect(c.filtered().map(n => n.id)).toEqual(['a']);
  });

  it('does nothing when the justification prompt is dismissed', async () => {
    const c = await create([nfe({ id: 'a' })]);
    c.justification = null;

    await c.cancelNfe(c.filtered()[0]);

    http.expectNone('/api/nfe/a/cancel');
  });

  it("shows the backend's message and keeps the row when the cancellation is refused", async () => {
    const c = await create([nfe({ id: 'a' })]);

    const cancelling = c.cancelNfe(c.filtered()[0]);
    http.expectOne('/api/nfe/a/cancel').flush({ message: 'Cancellation window has expired' }, { status: 400, statusText: 'Bad Request' });
    await cancelling;

    expect(toast.toasts().map(t => t.message)).toEqual(['Cancellation window has expired']);
    expect(c.filtered().map(n => n.status)).toEqual(['AUTHORIZED']);
  });

  it('downloads the DANFE and the XML through the real endpoints', async () => {
    const c = await create([nfe({ id: 'a', documentSeries: '1', documentNumber: 42 })]);
    const row = c.filtered()[0];

    const danfe = c.downloadDanfe(row);
    http.expectOne(r => r.url === '/api/nfe/a/danfe').flush(new Blob(['%PDF']));
    await danfe;
    const xml = c.downloadXml(row);
    http.expectOne('/api/nfe/a/xml').flush(new Blob(['<nfe/>']));
    await xml;

    expect(c.saved).toEqual(['nfe-1-42.pdf', 'nfe-1-42.xml']);
  });

  it('reports a failed download with the backend message', async () => {
    const c = await create([nfe({ id: 'a' })]);

    const xml = c.downloadXml(c.filtered()[0]);
    http.expectOne('/api/nfe/a/xml').flush(new Blob([JSON.stringify({ message: 'não autorizada' })]), { status: 400, statusText: 'Bad Request' });
    await xml;

    expect(c.saved).toEqual([]);
    expect(toast.toasts().map(t => t.message)).toEqual(['não autorizada']);
  });
});
