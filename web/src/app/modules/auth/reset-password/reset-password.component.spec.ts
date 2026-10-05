import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { ResetPasswordComponent } from './reset-password.component';

const URL = '/api/auth/password-reset/confirm';

describe('ResetPasswordComponent', () => {
  let fixture: ComponentFixture<ResetPasswordComponent>;
  let http: HttpTestingController;
  let router: Router;
  let navigate: jasmine.Spy;
  let tracked: string[];
  const onTrack = (e: Event) => tracked.push((e as CustomEvent).detail.name);

  const el = () => fixture.nativeElement as HTMLElement;
  const settle = async () => { await new Promise(resolve => setTimeout(resolve)); fixture.detectChanges(); };
  const field = (id: string) => el().querySelector<HTMLInputElement>(`#${id}`)!;
  const title = () => el().querySelector('h1')!.textContent!.trim();
  const link = (text: string) => Array.from(el().querySelectorAll('a')).find(a => a.textContent!.includes(text));

  function type(id: string, value: string) {
    field(id).value = value;
    field(id).dispatchEvent(new Event('input'));
  }

  function fillAndSubmit(password = 'n3w-pass', confirmation = password) {
    type('reset-password', password);
    type('reset-confirmation', confirmation);
    el().querySelector('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  function open(token: string | null) {
    TestBed.configureTestingModule({
      imports: [ResetPasswordComponent],
      providers: [
        provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap(token === null ? {} : { token }) } } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    navigate = spyOn(router, 'navigate').and.resolveTo(true);
    fixture = TestBed.createComponent(ResetPasswordComponent);
    fixture.detectChanges();
  }

  beforeEach(() => {
    tracked = [];
    window.addEventListener('gravita:track', onTrack);
  });

  afterEach(() => {
    window.removeEventListener('gravita:track', onTrack);
    http.verify();
  });

  describe('reading the link', () => {
    it('takes the token out of the address bar as soon as it has read it', () => {
      open('tok-123');

      expect(navigate).toHaveBeenCalledWith([], jasmine.objectContaining({
        queryParams: { token: null }, queryParamsHandling: 'merge', replaceUrl: true,
      }));
    });

    it('shows the new-password form, with a confirmation field, when there is a token', () => {
      open('tok-123');

      expect(title()).toBe('Redefinir senha');
      expect(field('reset-password').type).toBe('password');
      expect(field('reset-password').autocomplete).toBe('new-password');
      expect(field('reset-confirmation').type).toBe('password');
    });

    it('shows the invalid state straight away when the link has no token', () => {
      open(null);

      expect(title()).toBe('Link inválido');
      expect(el().querySelector('form')).toBeNull();
      http.expectNone(URL);
    });
  });

  describe('client-side checks', () => {
    beforeEach(() => open('tok-123'));

    it('refuses to submit when the confirmation does not match', () => {
      fillAndSubmit('n3w-pass', 'other-pass');

      expect(el().querySelector('#reset-confirmation-error')!.textContent).toContain('As senhas não conferem.');
      expect(field('reset-confirmation').getAttribute('aria-invalid')).toBe('true');
      http.expectNone(URL);
    });

    it('refuses to submit an empty password', () => {
      fillAndSubmit('', '');

      expect(el().querySelector('#reset-password-error')!.textContent).toContain('Informe a nova senha.');
      http.expectNone(URL);
    });

    it('does not impose a strength policy: a short password goes through', async () => {
      fillAndSubmit('abc');

      const req = http.expectOne(URL);
      expect(req.request.body).toEqual({ token: 'tok-123', newPassword: 'abc' });
      req.flush({}, { status: 200, statusText: 'OK' });
      await settle();
    });
  });

  describe('success', () => {
    it('sends token and password in the body and offers the way to the login (no session is created)', async () => {
      open('tok-123');
      fillAndSubmit('n3w-pass');

      const req = http.expectOne(URL);
      expect(req.request.body).toEqual({ token: 'tok-123', newPassword: 'n3w-pass' });
      expect(req.request.urlWithParams).not.toContain('tok-123');
      req.flush({ message: 'ok' }, { status: 200, statusText: 'OK' });
      await settle();

      expect(title()).toBe('Senha redefinida');
      expect(el().textContent).toContain('Sua senha foi alterada');
      expect(link('Ir para o login')!.getAttribute('href')).toBe('/login');
      expect(el().querySelector('form')).toBeNull();
      expect(tracked).toEqual(['password_reset_succeeded']);
    });

    it('moves focus to the title so screen readers announce the result', async () => {
      open('tok-123');
      fillAndSubmit('n3w-pass');
      http.expectOne(URL).flush({}, { status: 200, statusText: 'OK' });
      await settle();

      expect(document.activeElement).toBe(el().querySelector('h1'));
      expect(el().querySelector('[role=status]')).not.toBeNull();
    });
  });

  describe('expired link', () => {
    it('says it expired — not "inválido" — and offers a new link', async () => {
      open('tok-123');
      fillAndSubmit();
      http.expectOne(URL).flush({ reason: 'EXPIRED', message: 'Este link de redefinição expirou.' }, { status: 410, statusText: 'Gone' });
      await settle();

      expect(title()).toBe('Link expirado');
      expect(el().textContent).toContain('expirou');
      expect(el().textContent).not.toContain('inválido');
      expect(link('Pedir novo link')!.getAttribute('href')).toBe('/forgot-password');
      expect(el().querySelector('form')).toBeNull();
      expect(link('Ir para o login')).toBeUndefined();
      expect(tracked).toEqual(['password_reset_link_expired']);
    });
  });

  describe('invalid or already used link', () => {
    it('shows the invalid state for an unknown token (400)', async () => {
      open('tok-123');
      fillAndSubmit();
      http.expectOne(URL).flush({ reason: 'INVALID', message: 'Link de redefinição inválido.' }, { status: 400, statusText: 'Bad Request' });
      await settle();

      expect(title()).toBe('Link inválido');
      expect(link('Pedir novo link')!.getAttribute('href')).toBe('/forgot-password');
      expect(tracked).toEqual(['password_reset_link_invalid']);
    });

    it('shows the invalid state for an already used token (410), tracked as used', async () => {
      open('tok-123');
      fillAndSubmit();
      http.expectOne(URL).flush({ reason: 'ALREADY_USED' }, { status: 410, statusText: 'Gone' });
      await settle();

      expect(title()).toBe('Link inválido');
      expect(el().textContent).toContain('já foi utilizado');
      expect(tracked).toEqual(['password_reset_link_used']);
    });

    it('never offers the form again once the link is spent', async () => {
      open('tok-123');
      fillAndSubmit();
      http.expectOne(URL).flush({ reason: 'ALREADY_USED' }, { status: 410, statusText: 'Gone' });
      await settle();

      expect(el().querySelector('input')).toBeNull();
    });
  });

  describe('failures that say nothing about the link', () => {
    it('keeps the form and what was typed when the server is unreachable, so the person can retry', async () => {
      open('tok-123');
      fillAndSubmit('n3w-pass');
      http.expectOne(URL).error(new ProgressEvent('error'));
      await settle();

      expect(title()).toBe('Redefinir senha');
      expect(el().querySelector('#reset-submit-error')!.textContent).toContain('Não foi possível redefinir agora');
      expect(field('reset-password').value).toBe('n3w-pass');

      el().querySelector('form')!.dispatchEvent(new Event('submit'));
      fixture.detectChanges();
      http.expectOne(URL).flush({}, { status: 200, statusText: 'OK' });
      await settle();
      expect(title()).toBe('Senha redefinida');
    });

    it('keeps the form when the server refuses the password (400 without a link reason)', async () => {
      open('tok-123');
      fillAndSubmit('n3w-pass');
      http.expectOne(URL).flush({ message: 'Dados inválidos.' }, { status: 400, statusText: 'Bad Request' });
      await settle();

      expect(title()).toBe('Redefinir senha');
      expect(el().querySelector('#reset-submit-error')!.textContent).toContain('Escolha outra');
    });

    it('shows no HTTP status, stack trace or raw JSON, in any state', async () => {
      open('tok-123');
      fillAndSubmit();
      http.expectOne(URL).flush({ trace: 'at Foo.bar(Foo.java:1)', reason: 'EXPIRED' }, { status: 410, statusText: 'Gone' });
      await settle();

      expect(el().textContent).not.toMatch(/410|Foo\.java|trace|reason|\{|\}|EXPIRED/);
    });
  });
});
