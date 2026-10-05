import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { AuthService, CurrentUser } from '../../../core/services/auth.service';
import { Customer } from '../../../core/services/customer.service';
import { CustomersListComponent } from './customers-list.component';

const customer = (over: Partial<Customer>): Customer => ({
  id: 'c1', name: 'Alfa Ltda', type: 'COMPANY', document: '11.222.333/0001-81', email: null, ieIndicator: 'TAXPAYER',
  finalConsumer: false, creditLimit: 1000, currentBalance: 250, status: 'REGULAR', companyId: null,
  addresses: [], contacts: [], priceTables: [], ...over,
});

describe('CustomersListComponent', () => {
  let http: HttpTestingController;

  const render = (profile: string | null) => {
    const currentUser = signal<CurrentUser | null>(profile ? { name: 'Ana', email: 'a@a.com', profile, companyId: 'co1' } : null);
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
        { provide: AuthService, useValue: { currentUser } }],
    });
    http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(CustomersListComponent);
    fixture.detectChanges();
    return fixture;
  };

  const flushList = (fixture: ReturnType<typeof render>, customers: Customer[]) => {
    http.expectOne('/api/customers').flush(customers);
    return fixture.whenStable().then(() => { fixture.detectChanges(); return fixture.nativeElement as HTMLElement; });
  };

  afterEach(() => http.verify());

  it('lists the customers returned by the API, with an eye icon to their data', async () => {
    const fixture = render('Salesperson');
    const el = await flushList(fixture, [customer({ id: 'c1', name: 'Alfa Ltda' }), customer({ id: 'c2', name: 'Beta SA' })]);

    expect(el.querySelectorAll('tbody tr').length).toBe(2);
    const eye = el.querySelector('a[title="Ver dados do cliente"]') as HTMLAnchorElement;
    expect(eye.querySelector('i.ti-eye')).not.toBeNull();
    expect(eye.getAttribute('href')).toBe('/settings/customers/c1');
    expect(el.textContent).toContain('Pagamentos');
    expect(el.textContent).not.toContain('Dados');
  });

  it('filters by name or document as the user types', async () => {
    const fixture = render('Administrator');
    const el = await flushList(fixture, [customer({ id: 'c1', name: 'Alfa Ltda' }),
      customer({ id: 'c2', name: 'Beta SA', document: '529.982.247-25', type: 'INDIVIDUAL' })]);

    fixture.componentInstance.searchQuery.set('beta');
    fixture.detectChanges();
    expect(el.querySelectorAll('tbody tr').length).toBe(1);

    fixture.componentInstance.searchQuery.set('52998224725');
    fixture.detectChanges();
    expect(el.querySelector('tbody')!.textContent).toContain('Beta SA');
    expect(el.querySelector('tbody')!.textContent).not.toContain('Alfa');
  });

  it('shows the "Novo Cliente" button to the Administrator only', async () => {
    const admin = render('Administrator');
    expect((await flushList(admin, [])).textContent).toContain('Novo Cliente');
    TestBed.resetTestingModule();

    const other = render('Financial');
    expect((await flushList(other, [])).textContent).not.toContain('Novo Cliente');
  });

  it('says so when the list cannot be loaded and retries on request', async () => {
    const fixture = render('Administrator');
    http.expectOne('/api/customers').flush(null, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;

    expect(el.textContent).toContain('Não foi possível carregar os clientes.');
    (el.querySelector('tbody button') as HTMLButtonElement).click();
    await flushList(fixture, [customer({})]);
    expect(el.querySelectorAll('tbody tr').length).toBe(1);
  });
});
