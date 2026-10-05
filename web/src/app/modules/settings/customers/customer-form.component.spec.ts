import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { AuthService, CurrentUser } from '../../../core/services/auth.service';
import { CustomerFormComponent } from './customer-form.component';

describe('CustomerFormComponent', () => {
  let http: HttpTestingController;

  const render = (profile: string | null) => {
    const currentUser = signal<CurrentUser | null>(profile ? { name: 'Ana', email: 'a@a.com', profile, companyId: 'co1' } : null);
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
        { provide: AuthService, useValue: { currentUser } }],
    });
    http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(CustomerFormComponent);
    fixture.detectChanges();
    return fixture;
  };

  const fillPerson = (c: CustomerFormComponent) => {
    c.setType('INDIVIDUAL');
    c.document.set('529.982.247-25');
    c.name.set('Ana Souza');
  };
  const fillAddress = (c: CustomerFormComponent) =>
    c.updateAddress(0, { street: 'Rua A', neighborhood: 'Centro', city: 'São Paulo', state: 'SP', zipCode: '01310-100' });

  afterEach(() => http.verify());

  it('keeps submit disabled until at least one address is filled in', () => {
    const fixture = render('Administrator');
    const form = fixture.componentInstance;
    const submit = () => (fixture.nativeElement.querySelector('button[type=submit]') as HTMLButtonElement);

    fillPerson(form);
    fixture.detectChanges();
    expect(submit().disabled).toBeTrue();
    expect(form.errors()['addresses']).toBe('Informe ao menos um endereço completo.');

    fillAddress(form);
    fixture.detectChanges();
    expect(submit().disabled).toBeFalse();
  });

  it('asks a company for its IE indicator and final-consumer flag', () => {
    const fixture = render('Administrator');
    const form = fixture.componentInstance;
    form.document.set('11.222.333/0001-81');
    form.name.set('Acme Ltda');
    fillAddress(form);

    expect(form.canSubmit()).toBeFalse();
    form.ieIndicator.set('TAXPAYER');
    form.finalConsumer.set('no');
    expect(form.canSubmit()).toBeTrue();
  });

  it('keeps one default address per type when the default is moved', () => {
    const form = render('Administrator').componentInstance;
    form.addAddress();
    form.updateAddress(1, { type: 'BILLING', isDefault: true });
    form.setDefault(1, true);

    expect(form.addresses().map(a => a.isDefault)).toEqual([false, true]);
  });

  it('posts the customer, adds it to the list and goes back to it', async () => {
    const fixture = render('Administrator');
    const form = fixture.componentInstance;
    const navigate = spyOn(TestBed.inject(Router), 'navigateByUrl').and.resolveTo(true);
    fillPerson(form);
    fillAddress(form);

    const submitting = form.submit();
    const request = http.expectOne('/api/customers');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(jasmine.objectContaining({
      type: 'INDIVIDUAL', document: '52998224725', name: 'Ana Souza',
      addresses: [jasmine.objectContaining({ street: 'Rua A', zipCode: '01310100', isDefault: true })],
    }));
    request.flush({ id: 'new', name: 'Ana Souza', type: 'INDIVIDUAL', document: '529.982.247-25', addresses: [], contacts: [], priceTables: [] });
    await submitting;

    expect(navigate).toHaveBeenCalledWith('/settings/customers');
  });

  it('shows the backend message when the registration is rejected and stays on the form', async () => {
    const form = render('Administrator').componentInstance;
    fillPerson(form);
    fillAddress(form);

    const submitting = form.submit();
    http.expectOne('/api/customers').flush({ message: 'Document already registered' }, { status: 400, statusText: 'Bad Request' });
    await submitting;

    expect(form.formError()).toContain('Document already registered');
    expect(form.submitting()).toBeFalse();
  });

  it('is closed to anyone but the Administrator', () => {
    const fixture = render('Salesperson');

    expect(fixture.nativeElement.textContent).toContain('Somente o perfil Administrador');
    expect(fixture.nativeElement.querySelector('form')).toBeNull();
  });
});
