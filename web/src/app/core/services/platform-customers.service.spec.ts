import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PlatformCustomersService } from './platform-customers.service';

const subscriber = (companyId: string, status: string, overrides: Record<string, unknown> = {}) => ({
  companyId, cnpj: '11222333000144', name: `Empresa ${companyId}`, email: null, phone: null, planTier: 'SILVER',
  planName: 'Silver', status, activationDate: '2026-03-10', expirationDate: '2026-04-10', ...overrides,
});

describe('PlatformCustomersService', () => {
  let http: HttpTestingController;
  let service: PlatformCustomersService;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    service = TestBed.inject(PlatformCustomersService);
  });
  afterEach(() => http.verify());

  it('maps subscribers to customers: formatted CNPJ, local activation date and status in Portuguese', async () => {
    const done = service.load();
    http.expectOne('/api/admin/customers').flush([
      subscriber('a', 'ACTIVE'), subscriber('b', 'PAYMENT_FAILURE', { email: 'x@y.com', phone: '(11) 1' }), subscriber('c', 'PENDING'),
    ]);
    await done;

    expect(service.status()).toBe('ready');
    expect(service.customers().map(c => c.status)).toEqual(['ativo', 'inadimplente', 'inativo']);
    expect(service.customers()[0].cnpj).toBe('11.222.333/0001-44');
    expect(service.customers()[0].assinaturaDesde.getDate()).toBe(10);
    expect(service.getById('b')?.email).toBe('x@y.com');
    expect(service.getById('a')?.email).toBe('');
  });

  it('flags a failed load', async () => {
    const done = service.load();
    http.expectOne('/api/admin/customers').flush(null, { status: 500, statusText: 'Server Error' });
    await done;

    expect(service.status()).toBe('error');
  });

  it('turns received payments into paid installments of the payment month', async () => {
    const done = service.payments('a');
    http.expectOne('/api/admin/customers/a/payments').flush([{ id: 'p1', amount: 297, paymentDate: '2026-09-10' }]);
    const payments = await done;

    expect(payments).toEqual([jasmine.objectContaining({ id: 'p1', customerId: 'a', valor: 297, status: 'pago' })]);
    expect(payments[0].competencia.getMonth()).toBe(8);
    expect(payments[0].competencia.getDate()).toBe(1);
  });
});
