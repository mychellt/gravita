import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Customer, CustomerService, NewCustomer } from './customer.service';

const customer = (over: Partial<Customer>): Customer => ({
  id: 'c1', name: 'Beta Ltda', type: 'COMPANY', document: '11.222.333/0001-81', email: null, ieIndicator: 'TAXPAYER',
  finalConsumer: false, creditLimit: 1000, currentBalance: 0, status: 'REGULAR', companyId: null,
  addresses: [], contacts: [], priceTables: [], ...over,
});

describe('CustomerService', () => {
  let service: CustomerService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(CustomerService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('loads the list from GET /api/customers, sorted by name', async () => {
    const loading = service.load();
    expect(service.status()).toBe('loading');
    http.expectOne('/api/customers').flush([customer({ id: 'b', name: 'Beta' }), customer({ id: 'a', name: 'Alfa' })]);
    await loading;

    expect(service.status()).toBe('ready');
    expect(service.customers().map(c => c.name)).toEqual(['Alfa', 'Beta']);
  });

  it('reports a failed load without rejecting', async () => {
    const loading = service.load();
    http.expectOne('/api/customers').flush(null, { status: 500, statusText: 'Server Error' });
    await loading;

    expect(service.status()).toBe('error');
  });

  it('reads one customer from GET /api/customers/{id}', async () => {
    const reading = service.get('c9');
    http.expectOne('/api/customers/c9').flush(customer({ id: 'c9', name: 'Gama' }));

    expect((await reading).name).toBe('Gama');
  });

  it('rejects when the customer does not exist', async () => {
    const reading = service.get('nope');
    http.expectOne('/api/customers/nope').flush({ message: 'not found' }, { status: 404, statusText: 'Not Found' });

    await expectAsync(reading).toBeRejected();
  });

  it('posts a new customer and adds it to the loaded list without a reload', async () => {
    const loading = service.load();
    http.expectOne('/api/customers').flush([customer({ id: 'b', name: 'Beta' })]);
    await loading;

    const body: NewCustomer = { type: 'COMPANY', document: '11222333000181', name: 'Alfa', addresses: [] };
    const registering = service.register(body);
    const request = http.expectOne('/api/customers');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(body);
    request.flush(customer({ id: 'a', name: 'Alfa' }));
    await registering;

    expect(service.customers().map(c => c.name)).toEqual(['Alfa', 'Beta']);
  });
});
