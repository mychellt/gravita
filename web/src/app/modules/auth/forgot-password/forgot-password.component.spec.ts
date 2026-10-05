import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { PASSWORD_RESET_NEUTRAL_MESSAGE } from '../../../core/services/password-reset.service';
import { ForgotPasswordComponent, RESEND_COOLDOWN_MS } from './forgot-password.component';

describe('ForgotPasswordComponent — request step', () => {
  let fixture: ComponentFixture<ForgotPasswordComponent>;
  let http: HttpTestingController;
  let tracked: string[];
  const onTrack = (e: Event) => tracked.push((e as CustomEvent).detail.name);

  const el = () => fixture.nativeElement as HTMLElement;
  const input = () => el().querySelector<HTMLInputElement>('#forgot-email')!;
  const button = () => el().querySelector<HTMLButtonElement>('button[type=submit]')!;
  const status = () => el().querySelector('[role=status]')!.textContent!.trim();

  function type(value: string) {
    input().value = value;
    input().dispatchEvent(new Event('input'));
  }

  function submit() {
    el().querySelector('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  const settle = async () => { await new Promise(resolve => setTimeout(resolve)); fixture.detectChanges(); };

  beforeEach(() => {
    tracked = [];
    window.addEventListener('gravita:track', onTrack);
    TestBed.configureTestingModule({
      imports: [ForgotPasswordComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ForgotPasswordComponent);
    fixture.detectChanges();
  });

  afterEach(() => {
    window.removeEventListener('gravita:track', onTrack);
    http.verify();
  });

  it('asks for the e-mail and offers the way back to the login', () => {
    expect(el().querySelector('h1')!.textContent).toContain('Esqueci minha senha');
    expect(input().autocomplete).toBe('email');
    expect(el().querySelector('a')!.getAttribute('href')).toBe('/login');
    expect(status()).toBe('');
  });

  it('rejects an empty or malformed address on the client and sends nothing', () => {
    submit();
    expect(el().querySelector('#forgot-email-error')!.textContent).toContain('Informe um e-mail válido.');
    expect(input().getAttribute('aria-invalid')).toBe('true');

    type('not-an-email');
    submit();
    expect(el().querySelector('#forgot-email-error')!.textContent).toContain('Informe um e-mail válido.');
    http.expectNone('/api/auth/password-reset');
  });

  it('shows the neutral message on acceptance, and the same text whatever the response body says', async () => {
    type(' ana@acme.com ');
    submit();
    const req = http.expectOne('/api/auth/password-reset');
    expect(req.request.body).toEqual({ email: 'ana@acme.com' });
    req.flush({ message: 'um texto diferente do servidor' }, { status: 202, statusText: 'Accepted' });
    await settle();

    expect(status()).toBe(PASSWORD_RESET_NEUTRAL_MESSAGE);
    expect(status()).toBe('Se este e-mail tiver uma conta, enviaremos um link de redefinição em instantes.');
    expect(tracked).toEqual(['password_reset_requested']);
  });

  it('shows the very same message for an address nobody owns', async () => {
    type('ghost@acme.com');
    submit();
    http.expectOne('/api/auth/password-reset').flush({}, { status: 202, statusText: 'Accepted' });
    await settle();

    expect(status()).toBe(PASSWORD_RESET_NEUTRAL_MESSAGE);
  });

  it('locks the button as "Enviado!" for the 60 second cooldown, then frees it', fakeAsync(() => {
    type('ana@acme.com');
    submit();
    http.expectOne('/api/auth/password-reset').flush({}, { status: 202, statusText: 'Accepted' });
    tick();
    fixture.detectChanges();

    expect(button().textContent!.trim()).toBe('Enviado!');
    expect(button().disabled).toBeTrue();
    submit();
    http.expectNone('/api/auth/password-reset');

    tick(RESEND_COOLDOWN_MS);
    fixture.detectChanges();
    expect(button().disabled).toBeFalse();
    expect(button().textContent!.trim()).toBe('Enviar link');
  }));

  it('does not claim an e-mail was sent when the server cannot be reached', async () => {
    type('ana@acme.com');
    submit();
    http.expectOne('/api/auth/password-reset').error(new ProgressEvent('error'));
    await settle();

    expect(status()).toBe('');
    expect(el().querySelector('#forgot-send-error')!.textContent).toContain('Não foi possível enviar agora');
    expect(button().disabled).toBeFalse();
    expect(tracked).toEqual([]);
  });

  it('shows no HTTP status, stack trace or raw JSON on a server error', async () => {
    type('ana@acme.com');
    submit();
    http.expectOne('/api/auth/password-reset').flush({ trace: 'at Foo.bar(Foo.java:1)', status: 500 }, { status: 500, statusText: 'Server Error' });
    await settle();

    const text = el().textContent!;
    expect(text).not.toMatch(/500|Foo\.java|trace|\{|\}/);
  });
});
