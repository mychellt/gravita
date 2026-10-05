import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { SESSION_TOKEN_KEY } from '../../../core/services/auth.service';
import { Customer } from '../../../core/services/customer.service';
import { CustomerDetailComponent } from './customer-detail.component';

const customer: Customer = {
  id: 'c1', name: 'Acme Ltda', type: 'COMPANY', document: '11.222.333/0001-81', email: 'acme@acme.com',
  ieIndicator: 'EXEMPT', finalConsumer: true, creditLimit: 1000, currentBalance: 250, status: 'BLOCKED', companyId: null,
  addresses: [{
    type: 'DELIVERY', street: 'Rua A', number: '10', complement: 'Sala 2', neighborhood: 'Centro', city: 'São Paulo',
    state: 'SP', zipCode: '01000-000', isDefault: true,
  }],
  contacts: [{ type: 'WHATSAPP', value: '11999990000' }],
  priceTables: [],
};

describe('CustomerDetailComponent', () => {
  let fixture: ComponentFixture<CustomerDetailComponent>;
  let http: HttpTestingController;

  const el = () => fixture.nativeElement as HTMLElement;
  const settle = async () => { await new Promise(resolve => setTimeout(resolve)); fixture.detectChanges(); };

  async function open(id: string) {
    sessionStorage.setItem(SESSION_TOKEN_KEY, 'tok');
    fixture = TestBed.createComponent(CustomerDetailComponent);
    fixture.componentRef.setInput('id', id);
    fixture.detectChanges();
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [CustomerDetailComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => { http.verify(); sessionStorage.removeItem(SESSION_TOKEN_KEY); });

  it('shows the customer returned by GET /api/customers/{id}', async () => {
    await open('c1');
    const req = http.expectOne('/api/customers/c1');
    expect(req.request.headers.get('Authorization')).toBe('Bearer tok');
    req.flush(customer);
    await settle();

    const text = el().textContent!;
    for (const expected of ['Acme Ltda', 'CNPJ', '11.222.333/0001-81', 'Bloqueado', 'Isento', 'acme@acme.com', 'Sim',
      'Rua A, 10 - Sala 2', 'Entrega', 'WhatsApp', '11999990000']) {
      expect(text).toContain(expected);
    }
  });

  it('says so when the customer does not exist', async () => {
    await open('missing');
    http.expectOne('/api/customers/missing').flush({ message: 'Customer not found' }, { status: 404, statusText: 'Not Found' });
    await settle();
    expect(el().textContent).toContain('Cliente não encontrado');
  });

  it('offers a retry on other failures', async () => {
    await open('c1');
    http.expectOne('/api/customers/c1').flush('boom', { status: 500, statusText: 'Server Error' });
    await settle();
    expect(el().textContent).toContain('Não foi possível carregar');

    (Array.from(el().querySelectorAll('button')).find(b => b.textContent!.includes('Tentar novamente')) as HTMLElement).click();
    http.expectOne('/api/customers/c1').flush(customer);
    await settle();
    expect(el().textContent).toContain('Acme Ltda');
  });
});
