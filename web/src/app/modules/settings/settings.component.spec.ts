import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { SESSION_TOKEN_KEY } from '../../core/services/auth.service';
import { Company } from '../../core/services/company.service';
import { ToastService } from '../../core/services/toast.service';
import { SettingsComponent } from './settings.component';

const COMPANY_ID = '7c1b2d9e-0000-4000-8000-000000000001';

const company: Company = {
  id: COMPANY_ID, name: 'Mercado Moderno Ltda', cnpj: '12345678000190', ie: '123456789112', im: '98765',
  cnae: '4711-3/02', taxRegime: 'SIMPLES_NACIONAL', simplesOptante: true, address: 'Rua A, 1', state: 'SP',
  issuingEmail: 'nfe@mercado.com.br', phone: '1130001000', logoUrl: null,
};

describe('SettingsComponent — Empresa', () => {
  let fixture: ComponentFixture<SettingsComponent>;
  let http: HttpTestingController;
  let toast: ToastService;

  const el = () => fixture.nativeElement as HTMLElement;
  const settle = async () => { await new Promise(resolve => setTimeout(resolve)); fixture.detectChanges(); };
  const input = (label: string) =>
    Array.from(el().querySelectorAll('.form-field'))
      .find(f => f.querySelector('label')!.textContent!.trim() === label)!
      .querySelector('input, select') as HTMLInputElement;
  const buttonLabels = () => Array.from(el().querySelectorAll('button.btn')).map(b => b.textContent!.trim());
  const type = (label: string, value: string) => {
    const field = input(label);
    field.value = value;
    field.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  };
  const clickButton = (text: string) =>
    (Array.from(el().querySelectorAll('button.btn')).find(b => b.textContent!.includes(text)) as HTMLElement).click();

  async function open(profile = 'Administrator', companyId: string | null = COMPANY_ID) {
    sessionStorage.setItem(SESSION_TOKEN_KEY, 'tok');
    fixture = TestBed.createComponent(SettingsComponent);
    fixture.detectChanges();
    http.expectOne('/api/auth/me').flush({ name: 'Ricardo', email: 'r@e.com', profile, companyId });
    await settle();
    if (companyId) {
      http.expectOne(`/api/companies/${companyId}`).flush(company);
      await settle();
    }
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [SettingsComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    toast = TestBed.inject(ToastService);
  });

  afterEach(() => {
    http.match('/api/plans').forEach(req => req.flush([])); // PlatformConfigService loads plans when first injected
    http.verify(); sessionStorage.removeItem(SESSION_TOKEN_KEY); });

  it("fills the form from the current user's company", async () => {
    await open();

    expect(input('CNPJ').value).toBe('12345678000190');
    expect(input('Razão Social').value).toBe('Mercado Moderno Ltda');
    expect(input('Regime Tributário').value).toBe('SIMPLES_NACIONAL');
    expect(input('CNAE principal').value).toBe('4711-3/02');
    expect(input('Inscrição Estadual').value).toBe('123456789112');
  });

  it('keeps the CNPJ read-only', async () => {
    await open();
    expect(input('CNPJ').readOnly).toBeTrue();
  });

  it('PATCHes the edited fields, keeps the rest of the company and omits the CNPJ', async () => {
    await open();
    type('Razão Social', 'Mercado Novo Ltda');
    type('CNAE principal', '4712-1/00');

    clickButton('Salvar');
    const req = http.expectOne(`/api/companies/${COMPANY_ID}`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({
      name: 'Mercado Novo Ltda', ie: '123456789112', im: '98765', cnae: '4712-1/00', taxRegime: 'SIMPLES_NACIONAL',
      simplesOptante: true, address: 'Rua A, 1', state: 'SP', issuingEmail: 'nfe@mercado.com.br', phone: '1130001000',
      logoUrl: null,
    });
    req.flush({ ...company, name: 'Mercado Novo Ltda', cnae: '4712-1/00' });
    await settle();

    expect(toast.toasts().map(t => t.type)).toEqual(['success']);
  });

  it('shows an error toast and keeps the unsaved edits when the save is rejected', async () => {
    await open();
    type('Razão Social', 'Mercado Novo Ltda');

    clickButton('Salvar');
    http.expectOne(`/api/companies/${COMPANY_ID}`)
      .flush({ message: 'CNPJ cannot be changed' }, { status: 400, statusText: 'Bad Request' });
    await settle();

    expect(toast.toasts().map(t => t.type)).toEqual(['danger']);
    expect(toast.toasts()[0].message).toContain('CNPJ cannot be changed');
    expect(input('Razão Social').value).toBe('Mercado Novo Ltda');
  });

  it('shows an error toast on a server error', async () => {
    await open();
    type('Razão Social', 'Mercado Novo Ltda');

    clickButton('Salvar');
    http.expectOne(`/api/companies/${COMPANY_ID}`).flush('boom', { status: 500, statusText: 'Server Error' });
    await settle();

    expect(toast.toasts().map(t => t.type)).toEqual(['danger']);
    expect(input('Razão Social').value).toBe('Mercado Novo Ltda');
  });

  it('discards the edits on Cancelar', async () => {
    await open();
    type('Razão Social', 'Mercado Novo Ltda');

    clickButton('Cancelar');
    fixture.detectChanges();

    expect(input('Razão Social').value).toBe('Mercado Moderno Ltda');
  });

  it('is read-only, without Salvar/Cancelar, for a profile other than Administrator', async () => {
    await open('Salesperson');

    expect(input('Razão Social').disabled).toBeTrue();
    expect(input('Regime Tributário').disabled).toBeTrue();
    expect(input('CNAE principal').disabled).toBeTrue();
    expect(input('Inscrição Estadual').disabled).toBeTrue();
    expect(buttonLabels()).not.toContain('Salvar');
    expect(buttonLabels()).not.toContain('Cancelar');
  });

  it('offers editing and the buttons to an Administrator', async () => {
    await open();

    expect(input('Razão Social').disabled).toBeFalse();
    expect(buttonLabels()).toEqual(jasmine.arrayContaining(['Salvar', 'Cancelar']));
  });

  it('shows a load error when the user has no company', async () => {
    await open('Administrator', null);
    expect(el().textContent).toContain('Não foi possível carregar os dados da empresa');
  });

  it('shows a load error when the company request fails', async () => {
    sessionStorage.setItem(SESSION_TOKEN_KEY, 'tok');
    fixture = TestBed.createComponent(SettingsComponent);
    fixture.detectChanges();
    http.expectOne('/api/auth/me').flush({ name: 'R', email: 'r@e.com', profile: 'Administrator', companyId: COMPANY_ID });
    await settle();
    http.expectOne(`/api/companies/${COMPANY_ID}`).flush('', { status: 404, statusText: 'Not Found' });
    await settle();

    expect(el().textContent).toContain('Não foi possível carregar os dados da empresa');
  });
});
