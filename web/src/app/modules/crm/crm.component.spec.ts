import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { CrmComponent } from './crm.component';

const flush = () => new Promise<void>(r => setTimeout(r));

const opportunity = (id: string, stage: string, overrides: Record<string, unknown> = {}) => ({
  id, customerId: 'c1', customerName: 'Construtora Alfa', estimatedValue: 1000, probability: 50,
  expectedCloseDate: '2026-11-30', owner: 'u1', ownerName: 'Ricardo', stage, ...overrides,
});
const customer = (id: string, status: string | null, overrides: Record<string, unknown> = {}) => ({
  id, name: `Cliente ${id}`, document: '123', creditLimit: 500, currentBalance: 80, status, ...overrides,
});

describe('CrmComponent', () => {
  let http: HttpTestingController;

  async function create(opportunities: unknown[] | 'fail', customers: unknown[] = []) {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    const component = TestBed.runInInjectionContext(() => new CrmComponent());
    const request = http.expectOne('/api/crm/opportunities');
    opportunities === 'fail'
      ? request.flush(null, { status: 500, statusText: 'Server Error' })
      : request.flush(opportunities as object);
    http.expectOne('/api/customers').flush(customers);
    await flush();
    return component;
  }
  afterEach(() => http.verify());

  it('places each opportunity in the column of its stage', async () => {
    const c = await create([opportunity('o1', 'PROSPECTING'), opportunity('o2', 'NEGOTIATION'), opportunity('o3', 'PROSPECTING'), opportunity('o4', 'LOST')]);

    expect(c.opsByEstagio('PROSPECTING').map(o => o.id)).toEqual(['o1', 'o3']);
    expect(c.opsByEstagio('NEGOTIATION').map(o => o.id)).toEqual(['o2']);
    expect(c.opsByEstagio('PROPOSAL')).toEqual([]);
    expect(c.opsByEstagio('LOST').length).toBe(1);
  });

  it('flags a failed funnel load', async () => {
    const c = await create('fail');

    expect(c.funnelStatus()).toBe('error');
  });

  it('maps customers to rows with the badge of their status and a null limit kept as such', async () => {
    const c = await create([], [
      customer('a', 'REGULAR'), customer('b', 'DELINQUENT'), customer('c', 'BLOCKED'), customer('d', null, { creditLimit: null }),
    ]);

    expect(c.clientes().map(x => x.status)).toEqual(['ativo', 'inadimplente', 'bloqueado', 'ativo']);
    expect(c.clientes().map(x => x.inadimplente)).toEqual([false, true, false, false]);
    expect(c.clientes()[3].limiteCredito).toBeNull();
  });
});
