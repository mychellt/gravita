import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { FinanceComponent } from './finance.component';

const flush = () => new Promise<void>(r => setTimeout(r));

const dayOffset = (days: number) => {
  const d = new Date();
  d.setDate(d.getDate() + days);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
};

const receivable = (id: string, overrides: Record<string, unknown> = {}) => ({
  id, customerId: 'c1', customerName: 'Cliente Alfa', origin: 'INVOICING', amount: 100, remainingBalance: 100,
  dueDate: dayOffset(5), installments: 1, installmentNumber: 1, status: 'OPEN', ...overrides,
});
const payable = (id: string, overrides: Record<string, unknown> = {}) => ({
  id, supplierId: 's1', supplierName: 'Fornecedor Beta', origin: 'PURCHASE_RECEIPT', amount: 40, dueDate: dayOffset(3), status: 'OPEN', ...overrides,
});
const bucket = (periodStart: string, inflow: number, outflow: number) =>
  ({ periodStart, realizedInflow: 0, realizedOutflow: 0, projectedInflow: inflow, projectedOutflow: outflow });

describe('FinanceComponent', () => {
  let http: HttpTestingController;

  async function create(data: { receivables?: unknown[] | 'fail'; payables?: unknown[]; cashFlow?: unknown | 'fail' } = {}) {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    const component = TestBed.runInInjectionContext(() => new FinanceComponent());
    const answer = (url: string, body: unknown) => {
      const request = http.expectOne(r => r.url === url);
      body === 'fail' ? request.flush(null, { status: 500, statusText: 'Server Error' }) : request.flush(body as object);
      return request.request;
    };
    answer('/api/finance/receivables', data.receivables ?? []);
    answer('/api/finance/payables', data.payables ?? []);
    const cashFlow = answer('/api/finance/cash-flow', data.cashFlow ?? { closingBalance: 0, buckets: [] });
    expect(cashFlow.params.get('granularity')).toBe('DAILY');
    await flush();
    return component;
  }
  afterEach(() => http.verify());

  it('totals what is still to receive and to pay, and the overdue receivables', async () => {
    const c = await create({
      receivables: [
        receivable('r1'), receivable('r2', { dueDate: dayOffset(-3), remainingBalance: 60 }),
        receivable('r3', { status: 'SETTLED', remainingBalance: 0 }), receivable('r4', { status: 'PARTIALLY_SETTLED', remainingBalance: 25 }),
      ],
      payables: [payable('p1'), payable('p2', { status: 'PAID' })],
    });

    expect(c.totalReceivable()).toBe(185);
    expect(c.openReceivableCount()).toBe(3);
    expect(c.totalOverdue()).toBe(60);
    expect(c.overdueCount()).toBe(1);
    expect(c.totalPayable()).toBe(40);
    expect(c.openPayableCount()).toBe(1);
  });

  it('shows the badge status in Portuguese: overdue, partial, settled', async () => {
    const c = await create({
      receivables: [
        receivable('r1'), receivable('r2', { dueDate: dayOffset(-1) }), receivable('r3', { status: 'PARTIALLY_SETTLED', remainingBalance: 25 }),
        receivable('r4', { status: 'SETTLED' }), receivable('r5', { status: 'RENEGOTIATED' }),
      ],
    });

    expect(c.receivables().map(r => r.status)).toEqual(['aberto', 'vencido', 'parcial', 'liquidado', 'renegociado']);
    expect(c.receivables()[2].valor).toBe(25);
    expect(c.receivables().map(r => r.aberto)).toEqual([true, true, true, false, false]);
  });

  it('labels installments and resolves partner names', async () => {
    const c = await create({
      receivables: [receivable('r1', { installments: 3, installmentNumber: 2 }), receivable('r2', { customerName: null })],
      payables: [payable('p1', { supplierName: null })],
    });

    expect(c.receivables()[0].descricao).toBe('Faturamento 2/3');
    expect(c.receivables()[1].parceiro).toBe('—');
    expect(c.payables()[0].parceiro).toBe('—');
  });

  it('draws the 7-day cash flow with both bars on the same scale', async () => {
    const c = await create({ cashFlow: { closingBalance: 70, buckets: [bucket('2026-10-19', 100, 50), bucket('2026-10-20', 0, 0), bucket('2026-10-21', 20, 100)] } });

    expect(c.projectedBalance()).toBe(70);
    expect(c.cashflowBars().map(b => [b.inH, b.outH])).toEqual([[80, 40], [0, 0], [16, 80]]);
    expect(c.cashflowBars()[0].day).toBe('Hoje');
    expect(c.cashflowBars()[2].day).toBe('Qua');
  });

  it('offers a retry when receivables cannot be loaded', async () => {
    const c = await create({ receivables: 'fail' });

    expect(c.receivablesStatus()).toBe('error');
    c.reloadReceivables();
    http.expectOne('/api/finance/receivables').flush([]);
  });

  it('settles the whole remaining balance of a receivable and reloads the list', async () => {
    const c = await create({ receivables: [receivable('r1', { status: 'PARTIALLY_SETTLED', remainingBalance: 25 })] });

    const settling = c.settleReceivable(c.receivables()[0]);
    const request = http.expectOne('/api/finance/receivables/r1/settle');
    expect(request.request.body).toEqual({ amount: 25, partial: false });
    request.flush({});
    await flush();
    http.expectOne('/api/finance/receivables').flush([receivable('r1', { status: 'SETTLED', remainingBalance: 0 })]);
    await settling;
    http.expectOne(r => r.url === '/api/finance/cash-flow').flush({ closingBalance: 0, buckets: [] });

    expect(c.receivables()[0].status).toBe('liquidado');
    expect(c.busyId()).toBeNull();
  });

  it('keeps the title and frees the button when the settlement is refused', async () => {
    const c = await create({ receivables: [receivable('r1')] });

    const settling = c.settleReceivable(c.receivables()[0]);
    http.expectOne('/api/finance/receivables/r1/settle').flush({ message: 'cannot be settled' }, { status: 409, statusText: 'Conflict' });
    await settling;

    expect(c.busyId()).toBeNull();
    expect(c.receivables()[0].aberto).toBeTrue();
  });
});
