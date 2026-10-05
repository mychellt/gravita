import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { Customer } from '../../../core/services/customer.service';
import { CustomerDetailComponent } from './customer-detail.component';

const customer: Customer = {
  id: 'c1', name: 'Alfa Ltda', type: 'COMPANY', document: '11.222.333/0001-81', email: 'vendas@alfa.com', ieIndicator: 'EXEMPT',
  finalConsumer: true, creditLimit: 1000, currentBalance: 250, status: 'DELINQUENT', companyId: null,
  addresses: [{ type: 'BILLING', street: 'Rua A', number: '10', complement: null, neighborhood: 'Centro', city: 'São Paulo',
    state: 'SP', zipCode: '01310-100', isDefault: true }],
  contacts: [{ type: 'WHATSAPP', value: '(11) 91234-5678' }],
  priceTables: [{ priceTableId: 'abcdef12-0000-0000-0000-000000000000', priority: 1 }],
};

describe('CustomerDetailComponent', () => {
  let http: HttpTestingController;

  const render = async (id: string, respond: (req: ReturnType<HttpTestingController['expectOne']>) => void) => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
    http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(CustomerDetailComponent);
    fixture.componentRef.setInput('id', id);
    fixture.detectChanges();
    respond(http.expectOne(`/api/customers/${id}`));
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  };

  afterEach(() => http.verify());

  it('shows the customer returned by GET /api/customers/{id}', async () => {
    const el = await render('c1', req => req.flush(customer));

    for (const text of ['Alfa Ltda', 'CNPJ', '11.222.333/0001-81', 'Isento', 'Sim', 'vendas@alfa.com', 'Inadimplente',
      'Rua A, 10', 'Cobrança · padrão', 'WhatsApp', '(11) 91234-5678', 'Tabela abcdef12']) {
      expect(el.textContent).withContext(text).toContain(text);
    }
  });

  it('says the customer was not found on a 404', async () => {
    const el = await render('nope', req => req.flush({ message: 'x' }, { status: 404, statusText: 'Not Found' }));

    expect(el.textContent).toContain('Cliente não encontrado');
  });

  it('offers a retry when the server fails', async () => {
    const el = await render('c1', req => req.flush(null, { status: 500, statusText: 'Server Error' }));

    expect(el.textContent).toContain('Não foi possível carregar o cliente');
    expect(el.textContent).toContain('Tentar novamente');
  });
});
