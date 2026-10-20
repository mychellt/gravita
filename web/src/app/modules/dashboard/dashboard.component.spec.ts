import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ExecutiveDashboard } from '../../core/services/dashboard.service';
import { NfeListItem } from '../../core/services/nfe.service';
import { DashboardComponent } from './dashboard.component';

const nfe = (id: string, daysAgo: number): NfeListItem => ({
  id, documentNumber: Number(id.replace(/\D/g, '')) || 1, documentSeries: '1', recipientName: `Cliente ${id}`, cfop: '5102',
  emittedAt: new Date(Date.now() - daysAgo * 86_400_000).toISOString(), totalValue: 100, icmsValue: 18, status: 'AUTHORIZED',
  cancellationJustification: null, cancelledAt: null, paymentMethodType: null,
});

const comparison = (current: number, previous: number, variationPercent: number | null) => ({ current, previous, variationPercent });

const dashboardView = (overrides: Partial<ExecutiveDashboard> = {}): ExecutiveDashboard => ({
  period: 'MONTH',
  revenue: { day: comparison(150, 100, 50), week: comparison(0, 0, null), month: comparison(0, 0, null) },
  margin: { grossMarginPercent: 38.2 },
  delinquency: { totalOverdue: 4200, titleCount: 3 },
  criticalStock: [{ productId: 'p1', reason: 'BELOW_MINIMUM' }, { productId: 'p2', reason: 'NEAR_EXPIRY' }],
  lastSevenDays: ['2026-09-10', '2026-09-11', '2026-09-12', '2026-09-13', '2026-09-14', '2026-09-15', '2026-09-16']
    .map((date, i) => ({ date, value: [0, 100, 0, 50, 200, 0, 150][i] })),
  ...overrides,
});

describe('DashboardComponent', () => {
  let http: HttpTestingController;

  async function create(nfes: NfeListItem[] | 'fail', dashboard: ExecutiveDashboard | 'fail' = dashboardView()) {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
    http = TestBed.inject(HttpTestingController);
    const component = TestBed.runInInjectionContext(() => new DashboardComponent());
    const request = http.expectOne('/api/nfe');
    nfes === 'fail' ? request.flush(null, { status: 500, statusText: 'Server Error' }) : request.flush(nfes);
    await new Promise(r => setTimeout(r));
    const indicators = http.expectOne(r => r.url === '/api/reports/dashboard');
    expect(indicators.request.params.get('period')).toBe('MONTH');
    dashboard === 'fail' ? indicators.flush(null, { status: 500, statusText: 'Server Error' }) : indicators.flush(dashboard);
    await new Promise(r => setTimeout(r));
    return component;
  }
  afterEach(() => http.verify());

  describe('Últimas NF-e emitidas', () => {
    it('lists the 5 most recent issued NF-e from the backend, newest first', async () => {
      const c = await create([nfe('n1', 6), nfe('n2', 1), nfe('n3', 5), nfe('n4', 2), nfe('n5', 4), nfe('n6', 3), nfe('n7', 0)]);

      expect(c.nfeStatus()).toBe('ready');
      expect(c.recentNfes().map(n => n.id)).toEqual(['n7', 'n2', 'n4', 'n6', 'n5']);
    });

    it('reports a load failure so the card can offer a retry', async () => {
      const c = await create('fail');

      expect(c.nfeStatus()).toBe('error');
      expect(c.recentNfes()).toEqual([]);
      c.reloadNfes();
      http.expectOne('/api/nfe').flush([]);
    });
  });

  describe('Indicadores', () => {
    it('shows the backend figures: revenue today, overdue receivables and critical stock', async () => {
      const c = await create([]);

      expect(c.dashboardStatus()).toBe('ready');
      expect(c.faturamentoHoje()).toBe(150);
      expect(c.variacaoDia()).toBe(50);
      expect(c.vencido()).toBe(4200);
      expect(c.titulosVencidos()).toBe(3);
      expect(c.estoqueCritico()).toBe(2);
      expect(c.margemBruta()).toBe(38.2);
    });

    it('builds the 7-day chart from the backend series, scaled to the best day', async () => {
      const c = await create([]);

      expect(c.chartBars().map(b => b.day)).toEqual(['Qui', 'Sex', 'Sáb', 'Dom', 'Seg', 'Ter', 'Hoje']);
      expect(c.chartBars().map(b => b.height)).toEqual([4, 45, 4, 23, 90, 4, 68]);
      expect(c.chartBars().filter(b => b.highlight).map(b => b.value)).toEqual([200]);
      expect(c.totalSemana()).toBe(500);
    });

    it('has no day comparison when yesterday had no revenue', async () => {
      const c = await create([], dashboardView({ revenue: { day: comparison(10, 0, null), week: comparison(0, 0, null), month: comparison(0, 0, null) } }));

      expect(c.variacaoDia()).toBeNull();
    });

    it('offers a retry when the indicators cannot be loaded', async () => {
      const c = await create([], 'fail');

      expect(c.dashboardStatus()).toBe('error');
      expect(c.faturamentoHoje()).toBe(0);
      c.reloadDashboard();
      await new Promise(r => setTimeout(r));
      http.expectOne(r => r.url === '/api/reports/dashboard').flush(dashboardView());
    });

    it('counts this month\'s issued NF-e, ignoring cancelled ones', async () => {
      const c = await create([nfe('n1', 0), { ...nfe('n2', 0), status: 'CANCELLED' }]);

      expect(c.nfeDoMes()).toEqual({ count: 1, total: 100 });
    });
  });
});
