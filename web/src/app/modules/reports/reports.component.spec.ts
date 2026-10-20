import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ReportsComponent } from './reports.component';

const flush = () => new Promise<void>(r => setTimeout(r));

const dre = { period: '2026-09', grossRevenue: 1000, deductions: 120, cmv: 500, totalExpenses: 200, netResult: 180 };
const entry = (id: string, share: number, abcClass: 'A' | 'B' | 'C') =>
  ({ entityId: id, revenue: share * 10, revenueShare: share, cumulativeShare: share, abcClass });

describe('ReportsComponent', () => {
  let http: HttpTestingController;

  async function create(responses: { dre?: unknown | 'fail'; abc?: unknown[] | 'fail' } = {}) {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    const component = TestBed.runInInjectionContext(() => new ReportsComponent());
    await flush();
    const answer = (url: string, body: unknown | undefined, fallback: unknown) => {
      const request = http.expectOne(r => r.url === url);
      body === 'fail' ? request.flush(null, { status: 500, statusText: 'Server Error' }) : request.flush((body ?? fallback) as object);
      return request.request;
    };
    const dreRequest = answer('/api/reports/dre', responses.dre, dre);
    const abcRequest = answer('/api/reports/abc-curve', responses.abc, []);
    expect(dreRequest.params.get('period')).toBe(component.period);
    expect(abcRequest.params.get('type')).toBe('product');
    await flush();
    if (responses.abc !== 'fail') {
      http.expectOne(r => r.url === '/api/pdv/products/search').flush([
        { productId: 'p1', description: 'Tinta Acrílica 18L', ncm: null, unitPrice: 1, availableForSale: true },
      ]);
      await flush();
    }
    return component;
  }
  afterEach(() => http.verify());

  it('builds the DRE from the backend, with the shares over gross revenue', async () => {
    const c = await create();

    expect(c.dreStatus()).toBe('ready');
    const rows = c.dreRows()!;
    expect(rows.grossProfit).toBe(380);
    expect(rows.deductionsShare).toBe(12);
    expect(rows.cmvShare).toBe(50);
    expect(rows.grossProfitShare).toBe(38);
    expect(rows.netShare).toBe(18);
  });

  it('shows no shares when the month had no revenue', async () => {
    const c = await create({ dre: { ...dre, grossRevenue: 0, deductions: 0, cmv: 0, totalExpenses: 0, netResult: 0 } });

    expect(c.dreRows()!.netShare).toBeNull();
    expect(c.pct(null)).toBe('—');
  });

  it('reports a DRE failure so the card can offer a retry', async () => {
    const c = await create({ dre: 'fail' });

    expect(c.dreStatus()).toBe('error');
    expect(c.dreRows()).toBeNull();
  });

  it('shows the top 10 products of the ABC curve with their names, scaled to the best seller', async () => {
    const curve = [entry('p1', 40, 'A'), entry('p2', 20, 'A'), entry('p3', 15, 'B'), ...Array.from({ length: 10 }, (_, i) => entry(`x${i}`, 2.5, 'C'))];
    const c = await create({ abc: curve });

    expect(c.abcData().length).toBe(10);
    expect(c.abcData()[0]).toEqual(jasmine.objectContaining({ name: 'Tinta Acrílica 18L', label: 'Tinta A…', cls: 'a', height: 90 }));
    expect(c.abcData()[1]).toEqual(jasmine.objectContaining({ name: 'Produto', height: 45 }));
    expect(c.abcShares()).toEqual({ a: 60, b: 15, c: 25 });
  });

  it('reports an ABC curve failure', async () => {
    const c = await create({ abc: 'fail' });

    expect(c.abcStatus()).toBe('error');
    expect(c.abcData()).toEqual([]);
  });

  it('exports a report in the chosen format for the current month', async () => {
    const c = await create();
    const saved: string[] = [];
    (c as unknown as { saveFile: (b: Blob, n: string) => void }).saveFile = (_b, name) => saved.push(name);
    c.format.set('xlsx');

    const exporting = c.generate(c.reportCards[1]);
    const request = http.expectOne(r => r.url === '/api/reports/abc-curve/export');
    expect(request.request.params.get('format')).toBe('xlsx');
    expect(request.request.params.get('period')).toBe(c.period);
    expect(request.request.params.get('type')).toBe('product');
    request.flush(new Blob(['x']));
    await exporting;

    expect(saved).toEqual([`abc-curve-${c.period}.xlsx`]);
    expect(c.exporting()).toBeNull();
  });

  it('tells the user when their profile cannot export the report', async () => {
    const c = await create();

    const exporting = c.generate(c.reportCards[0]);
    http.expectOne(r => r.url === '/api/reports/dre/export').flush(null, { status: 403, statusText: 'Forbidden' });
    await exporting;

    expect(c.exporting()).toBeNull();
  });
});
