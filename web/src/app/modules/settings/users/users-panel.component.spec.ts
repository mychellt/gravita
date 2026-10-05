import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { AuthService, CurrentUser } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { UserSummary } from '../../../core/services/user.service';
import { DUPLICATE_EMAIL_MESSAGE, SELF_DEACTIVATION_HINT, UsersPanelComponent } from './users-panel.component';

const user = (over: Partial<UserSummary>): UserSummary => ({
  id: 'u1', name: 'Ricardo Lima', email: 'ricardo@acme.com', profileId: 'p1', profileName: 'Administrator',
  twoFactorEnabled: true, status: 'ACTIVE', ...over,
});
const PROFILES = [{ id: 'p1', name: 'Administrator' }, { id: 'p2', name: 'Salesperson' }];

describe('UsersPanelComponent', () => {
  let http: HttpTestingController;
  let toast: ToastService;

  const settle = async (fixture: { detectChanges(): void }) => { await new Promise(r => setTimeout(r)); fixture.detectChanges(); };

  async function render(profile: string, users: UserSummary[]) {
    const currentUser = signal<CurrentUser | null>({ name: 'Ricardo', email: 'ricardo@acme.com', profile, companyId: 'co1' });
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: AuthService, useValue: { currentUser } }],
    });
    http = TestBed.inject(HttpTestingController);
    toast = TestBed.inject(ToastService);
    const fixture = TestBed.createComponent(UsersPanelComponent);
    fixture.detectChanges();
    await settle(fixture);
    if (profile === 'Administrator') {
      http.expectOne('/api/users').flush(users);
      http.expectOne('/api/profiles').flush(PROFILES);
      await settle(fixture);
    }
    return { fixture, el: fixture.nativeElement as HTMLElement, panel: fixture.componentInstance };
  }

  const type = (el: HTMLElement, id: string, value: string) => {
    const field = el.querySelector(`#${id}`) as HTMLInputElement;
    field.value = value;
    field.dispatchEvent(new Event('input'));
    field.dispatchEvent(new Event('change'));
  };
  const fillValid = (el: HTMLElement) => {
    type(el, 'nu-name', 'Bia Souza'); type(el, 'nu-email', 'bia@acme.com'); type(el, 'nu-password', 's3cret'); type(el, 'nu-profile', 'p2');
  };
  const submitButton = (el: HTMLElement) => el.querySelector('form button[type=submit]') as HTMLButtonElement;
  const rows = (el: HTMLElement) => Array.from(el.querySelectorAll('tbody tr'));

  afterEach(() => http.verify());

  it('lists the users from the API with profile, 2FA and status', async () => {
    const { el } = await render('Administrator', [user({}), user({ id: 'u2', name: 'Ana Paula', email: 'ana@acme.com',
      profileName: 'Salesperson', twoFactorEnabled: false, status: 'INACTIVE' })]);

    expect(rows(el).length).toBe(2);
    expect(rows(el)[0].textContent).toContain('Administrador');
    expect(rows(el)[0].querySelector('.ti-check')).not.toBeNull();
    expect(rows(el)[1].textContent).toContain('Vendedor');
    expect(rows(el)[1].querySelector('.ti-x')).not.toBeNull();
    expect(rows(el)[1].textContent).toContain('Inativo');
  });

  it('keeps the form closed until "Novo Usuário" and disables submit until it is complete', async () => {
    const { fixture, el } = await render('Administrator', []);
    expect(el.querySelector('form')).toBeNull();

    (el.querySelector('.panel-actions button') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(submitButton(el).disabled).toBeTrue();
    expect(Array.from(el.querySelectorAll('#nu-profile option')).map(o => o.textContent!.trim()))
      .toEqual(['Selecione…', 'Administrador', 'Vendedor']);

    fillValid(el);
    fixture.detectChanges();
    expect(submitButton(el).disabled).toBeFalse();
  });

  it('registers the user, shows it in the list without a reload and closes the form', async () => {
    const { fixture, el } = await render('Administrator', [user({})]);
    const success = spyOn(toast, 'success');
    (el.querySelector('.panel-actions button') as HTMLButtonElement).click();
    fixture.detectChanges();
    fillValid(el);
    fixture.detectChanges();

    submitButton(el).click();
    const post = http.expectOne('/api/users');
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual({ name: 'Bia Souza', email: 'bia@acme.com', password: 's3cret', profileId: 'p2' });
    post.flush({ id: 'u2' });
    await settle(fixture);
    http.expectOne('/api/users').flush([user({}), user({ id: 'u2', name: 'Bia Souza', email: 'bia@acme.com', profileName: 'Salesperson', twoFactorEnabled: false })]);
    await settle(fixture);

    expect(rows(el).length).toBe(2);
    expect(rows(el)[1].textContent).toContain('Bia Souza');
    expect(el.querySelector('form')).toBeNull();
    expect(success).toHaveBeenCalled();
  });

  it('shows a duplicate e-mail inline on the form, keeps what was typed, and clears it when the e-mail changes', async () => {
    const { fixture, el } = await render('Administrator', [user({})]);
    const danger = spyOn(toast, 'danger');
    (el.querySelector('.panel-actions button') as HTMLButtonElement).click();
    fixture.detectChanges();
    fillValid(el);
    fixture.detectChanges();

    submitButton(el).click();
    http.expectOne('/api/users').flush({ message: 'Email already registered: bia@acme.com' }, { status: 400, statusText: 'Bad Request' });
    await settle(fixture);

    expect(el.querySelector('#nu-email-error')!.textContent).toContain(DUPLICATE_EMAIL_MESSAGE);
    expect((el.querySelector('#nu-name') as HTMLInputElement).value).toBe('Bia Souza');
    expect(danger).not.toHaveBeenCalled();

    type(el, 'nu-email', 'bia2@acme.com');
    fixture.detectChanges();
    expect(el.querySelector('#nu-email-error')!.textContent!.trim()).toBe('');
  });

  it('inactivates after confirming, flips the row in place, and does nothing if the confirmation is declined', async () => {
    const { fixture, el } = await render('Administrator', [user({}), user({ id: 'u2', name: 'Ana Paula', email: 'ana@acme.com' })]);
    const inactivate = () => (rows(el)[1].querySelector('button.danger') as HTMLButtonElement);

    spyOn(window, 'confirm').and.returnValue(false);
    inactivate().click();
    http.expectNone('/api/users/u2');

    (window.confirm as jasmine.Spy).and.returnValue(true);
    inactivate().click();
    const patch = http.expectOne('/api/users/u2');
    expect(patch.request.method).toBe('PATCH');
    expect(patch.request.body).toEqual({ status: 'INACTIVE' });
    patch.flush(null, { status: 204, statusText: 'No Content' });
    await settle(fixture);

    expect(rows(el)[1].textContent).toContain('Inativo');
    expect(rows(el)[1].querySelector('button.danger')).toBeNull();
    expect(rows(el)[0].textContent).toContain('Ativo');
  });

  it('only offers "Inativar" on active rows', async () => {
    const { el } = await render('Administrator', [user({ id: 'u2', email: 'ana@acme.com', status: 'INACTIVE' }),
      user({ id: 'u3', email: 'bia@acme.com', status: 'PENDING_ACTIVATION' })]);

    expect(el.querySelectorAll('button.danger').length).toBe(0);
  });

  it("disables 'Inativar' on the logged-in user's own row, with an explanation", async () => {
    const { el } = await render('Administrator', [user({ email: 'Ricardo@Acme.com' })]);
    const button = el.querySelector('button.danger') as HTMLButtonElement;
    spyOn(window, 'confirm');

    expect(button.disabled).toBeTrue();
    expect(button.getAttribute('title')).toBe(SELF_DEACTIVATION_HINT);
    button.click();
    expect(window.confirm).not.toHaveBeenCalled();
  });

  it('says so when the users cannot be loaded and retries on request', async () => {
    const currentUser = signal<CurrentUser | null>({ name: 'R', email: 'r@a.com', profile: 'Administrator', companyId: 'c' });
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: AuthService, useValue: { currentUser } }],
    });
    http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(UsersPanelComponent);
    fixture.detectChanges();
    await settle(fixture);
    http.expectOne('/api/users').flush(null, { status: 500, statusText: 'Server Error' });
    http.expectOne('/api/profiles').flush([]);
    await settle(fixture);
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Não foi possível carregar os usuários.');

    (el.querySelector('tbody button') as HTMLButtonElement).click();
    http.expectOne('/api/users').flush([user({})]);
    http.expectOne('/api/profiles').flush(PROFILES);
    await settle(fixture);
    expect(rows(el).length).toBe(1);
  });
});
