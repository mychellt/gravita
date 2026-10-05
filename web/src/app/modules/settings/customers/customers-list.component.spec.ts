import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { SESSION_TOKEN_KEY } from '../../../core/services/auth.service';
import { Customer } from '../../../core/services/customer.service';
import { ToastService } from '../../../core/services/toast.service';
import { CustomersListComponent } from './customers-list.component';

const address = {
  type: 'BILLING' as const, street: 'Rua A', number: '10', complement: null, neighborhood: 'Centro',
  city: 'São Paulo', state: 'SP', zipCode: '01000-000', isDefault: true,
};

const customer = (id: string, name: string, document: string, overrides: Partial<Customer> = {}): Customer => ({
  id, name, type: 'COMPANY', document, email: null, ieIndicator: 'TAXPAYER', finalConsumer: false,
  creditLimit: 1000, currentBalance: 250, status: 'REGULAR', companyId: null, addresses: [address], contacts: [],
  priceTables: [], ...overrides,
});

const ACME = customer('c1', 'Acme Ltda', '11.222.333/0001-81');
const BETA = customer('c2', 'Beta Comércio', '52.998.224/0001-02', { status: 'DELINQUENT' });

describe('CustomersListComponent', () => {
  let fixture: ComponentFixture<CustomersListComponent>;
  let http: HttpTestingController;
  let toast: ToastService;

  const el = () => fixture.nativeElement as HTMLElement;
  const settle = async () => { await new Promise(resolve => setTimeout(resolve)); fixture.detectChanges(); };
  const rows = () => Array.from(el().querySelectorAll('tbody tr'));
  const field = (label: string, scope: ParentNode = el()) =>
    Array.from(scope.querySelectorAll('.form-field'))
      .find(f => f.querySelector('label')!.textContent!.trim() === label)!
      .querySelector('input, select') as HTMLInputElement;
  const fill = (label: string, value: string, scope: ParentNode = el()) => {
    const input = field(label, scope);
    input.value = value;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  };
  const button = (text: string) =>
    Array.from(el().querySelectorAll('button')).find(b => b.textContent!.includes(text)) as HTMLButtonElement | undefined;

  async function open(profile = 'Administrator', customers: Customer[] = [ACME, BETA]) {
    sessionStorage.setItem(SESSION_TOKEN_KEY, 'tok');
    fixture = TestBed.createComponent(CustomersListComponent);
    fixture.detectChanges();
    http.expectOne('/api/auth/me').flush({ name: 'Ricardo', email: 'r@e.com', profile, companyId: null });
    const req = http.expectOne('/api/customers');
    expect(req.request.method).toBe('GET');
    expect(req.request.headers.get('Authorization')).toBe('Bearer tok');
    req.flush(customers);
    await settle();
  }

  function fillValidForm() {
    fill('CNPJ', '11.222.333/0001-81');
    fill('Nome', 'Nova Empresa Ltda');
    fill('CEP', '01310-100');
    fill('Logradouro', 'Av. Paulista');
    fill('Bairro', 'Bela Vista');
    fill('Cidade', 'São Paulo');
    fill('UF', 'sp');
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [CustomersListComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    toast = TestBed.inject(ToastService);
  });

  afterEach(() => { http.verify(); sessionStorage.removeItem(SESSION_TOKEN_KEY); });

  it('lists the customers returned by GET /api/customers', async () => {
    await open();
    expect(rows().length).toBe(2);
    expect(rows()[0].textContent).toContain('Acme Ltda');
    expect(rows()[0].textContent).toContain('11.222.333/0001-81');
    expect(rows()[1].textContent).toContain('Inadimplente');
  });

  it('filters by name or by document digits', async () => {
    await open();
    const search = el().querySelector('.filter-input') as HTMLInputElement;
    const type = (value: string) => { search.value = value; search.dispatchEvent(new Event('input')); fixture.detectChanges(); };

    type('beta');
    expect(rows().length).toBe(1);
    expect(rows()[0].textContent).toContain('Beta Comércio');

    type('11222333');
    expect(rows().length).toBe(1);
    expect(rows()[0].textContent).toContain('Acme Ltda');

    type('zzz');
    expect(rows()[0].textContent).toContain('Nenhum cliente encontrado');
  });

  it('shows an eye icon to the customer data and keeps the Pagamentos link', async () => {
    await open();
    const links = Array.from(rows()[0].querySelectorAll('a'));
    const eye = links.find(a => a.querySelector('i.ti-eye'))!;
    expect(eye.getAttribute('href')).toBe('/settings/customers/c1');
    expect(eye.textContent!.trim()).toBe('');
    expect(links.map(a => a.textContent!.trim())).not.toContain('Dados');
    expect(links.find(a => a.textContent!.trim() === 'Pagamentos')!.getAttribute('href')).toBe('/settings/customers/c1/payments');
  });

  it('offers a retry when the list cannot be loaded', async () => {
    sessionStorage.setItem(SESSION_TOKEN_KEY, 'tok');
    fixture = TestBed.createComponent(CustomersListComponent);
    fixture.detectChanges();
    http.expectOne('/api/auth/me').flush({ name: 'R', email: 'r@e.com', profile: 'Administrator', companyId: null });
    http.expectOne('/api/customers').flush('boom', { status: 500, statusText: 'Server Error' });
    await settle();
    expect(el().textContent).toContain('Não foi possível carregar os clientes');

    button('Tentar novamente')!.click();
    http.expectOne('/api/customers').flush([ACME]);
    await settle();
    expect(rows().length).toBe(1);
  });

  it('shows "Novo Cliente" to the Administrator only', async () => {
    await open('Administrator');
    expect(button('Novo Cliente')).toBeDefined();
  });

  it('hides "Novo Cliente" from other profiles but still lists customers', async () => {
    await open('Salesperson');
    expect(button('Novo Cliente')).toBeUndefined();
    expect(rows().length).toBe(2);
  });

  describe('registration form', () => {
    beforeEach(async () => {
      await open();
      button('Novo Cliente')!.click();
      fixture.detectChanges();
    });

    const submit = () => el().querySelector('button[type=submit]') as HTMLButtonElement;

    it('keeps submit disabled until a complete address is filled in', () => {
      fill('CNPJ', '11.222.333/0001-81');
      fill('Nome', 'Nova Empresa Ltda');
      expect(submit().disabled).toBeTrue();
      expect(el().textContent).toContain('Informe ao menos um endereço completo');

      fill('CEP', '01310-100');
      fill('Logradouro', 'Av. Paulista');
      fill('Bairro', 'Bela Vista');
      fill('Cidade', 'São Paulo');
      expect(submit().disabled).toBeTrue(); // UF still missing

      fill('UF', 'SP');
      expect(submit().disabled).toBeFalse();
    });

    it('keeps submit disabled while the document does not have the right length for the person type', () => {
      fillValidForm();
      fill('CNPJ', '11.222.333');
      expect(submit().disabled).toBeTrue();
    });

    it('requires exactly one default among several addresses of the same type', () => {
      fillValidForm();
      (button('Adicionar endereço')!).click();
      fixture.detectChanges();
      const second = el().querySelectorAll('.repeat-item')[1];
      fill('CEP', '20000-000', second);
      fill('Logradouro', 'Rua B', second);
      fill('Bairro', 'Centro', second);
      fill('Cidade', 'Rio de Janeiro', second);
      fill('UF', 'RJ', second);
      expect(submit().disabled).toBeFalse(); // first is the default, second is not

      const defaults = Array.from(el().querySelectorAll('.repeat-item input[type=checkbox]')) as HTMLInputElement[];
      defaults[0].checked = false;
      defaults[0].dispatchEvent(new Event('change'));
      fixture.detectChanges();
      expect(submit().disabled).toBeTrue();
      expect(el().textContent).toContain('exatamente um endereço padrão');

      defaults[1].checked = true;
      defaults[1].dispatchEvent(new Event('change'));
      fixture.detectChanges();
      expect(submit().disabled).toBeFalse();
    });

    it('POSTs the customer and adds it to the list without reloading', async () => {
      fillValidForm();
      fill('Limite de crédito (R$)', '1500,50');
      (button('Adicionar contato')!).click();
      fixture.detectChanges();
      fill('Valor', '(11) 99999-0000');

      submit().click();
      const req = http.expectOne('/api/customers');
      expect(req.request.method).toBe('POST');
      expect(req.request.headers.get('Authorization')).toBe('Bearer tok');
      expect(req.request.body).toEqual({
        type: 'COMPANY', document: '11222333000181', name: 'Nova Empresa Ltda', email: null,
        ieIndicator: 'TAXPAYER', finalConsumer: false, creditLimit: 1500.5,
        addresses: [{
          type: 'BILLING', street: 'Av. Paulista', number: null, complement: null, neighborhood: 'Bela Vista',
          city: 'São Paulo', state: 'SP', zipCode: '01310-100', isDefault: true,
        }],
        contacts: [{ type: 'PHONE', value: '(11) 99999-0000' }],
      });
      req.flush(customer('c3', 'Nova Empresa Ltda', '11.222.333/0001-81'), { status: 201, statusText: 'Created' });
      await settle();

      expect(rows().length).toBe(3);
      expect(rows()[2].textContent).toContain('Nova Empresa Ltda');
      expect(el().querySelector('app-customer-form')).toBeNull();
      expect(toast.toasts().some(t => t.type === 'success')).toBeTrue();
    });

    it('keeps the form and shows the backend message when the registration is rejected', async () => {
      fillValidForm();
      submit().click();
      http.expectOne('/api/customers').flush({ message: 'Invalid CNPJ: 11.222.333/0001-81' }, { status: 400, statusText: 'Bad Request' });
      await settle();

      expect(el().querySelector('app-customer-form')).not.toBeNull();
      expect(field('Nome').value).toBe('Nova Empresa Ltda');
      expect(rows().length).toBe(2);
      expect(toast.toasts().some(t => t.type === 'danger' && t.message.includes('Invalid CNPJ'))).toBeTrue();
      expect(submit().disabled).toBeFalse();
    });

    it('closes the form on Cancelar without calling the API', () => {
      button('Cancelar')!.click();
      fixture.detectChanges();
      expect(el().querySelector('app-customer-form')).toBeNull();
    });
  });
});
