import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { SESSION_TOKEN_KEY } from '../../../core/services/auth.service';
import { LOGIN_REJECTED_MESSAGE, LOGIN_UNAVAILABLE_MESSAGE, LoginComponent, TOTP_REJECTED_MESSAGE } from './login.component';

@Component({ standalone: true, template: 'DASHBOARD' })
class DashboardStub {}

describe('LoginComponent (UC-M10-15)', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let http: HttpTestingController;
  let router: Router;

  const el = () => fixture.nativeElement as HTMLElement;
  const q = <T extends HTMLElement>(selector: string) => el().querySelector<T>(selector)!;
  const email = () => q<HTMLInputElement>('#login-email');
  const password = () => q<HTMLInputElement>('#login-password');
  const code = () => q<HTMLInputElement>('#login-code');
  const formError = () => q('#login-form-error').textContent!.trim();

  function fill(input: HTMLInputElement, value: string) {
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  async function settle() {
    await new Promise(resolve => setTimeout(resolve));
    fixture.detectChanges();
  }

  async function submit(formEmail = 'maria@acme.com', formPassword = 'secret') {
    fill(email(), formEmail);
    fill(password(), formPassword);
    q('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  async function submitCode(value: string) {
    fill(code(), value);
    q('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  /** Leva o componente até a etapa do TOTP, como o administrador `joao@acme.com`. */
  async function reachTotpStep() {
    await submit('joao@acme.com', 'secret');
    http.expectOne('/api/auth/login').flush({ status: 'TOTP_REQUIRED', sessionToken: null });
    await settle();
  }

  beforeEach(() => {
    sessionStorage.removeItem(SESSION_TOKEN_KEY);
    TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideHttpClient(), provideHttpClientTesting(),
        provideRouter([{ path: 'dashboard', component: DashboardStub }]),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    fixture = TestBed.createComponent(LoginComponent);
    document.body.appendChild(fixture.nativeElement); // foco só é observável com o elemento no documento
    fixture.detectChanges();
  });

  afterEach(() => {
    http.verify();
    fixture.nativeElement.remove();
    sessionStorage.removeItem(SESSION_TOKEN_KEY);
  });

  describe('login', () => {
    it('1. correct credentials without 2FA store the session and land on /dashboard', async () => {
      const navigate = spyOn(router, 'navigateByUrl').and.callThrough();
      await submit();
      const req = http.expectOne('/api/auth/login');
      expect(req.request.body).toEqual({ email: 'maria@acme.com', password: 'secret' });
      req.flush({ status: 'AUTHENTICATED', sessionToken: '7f1e' });
      await settle();

      expect(sessionStorage.getItem(SESSION_TOKEN_KEY)).toBe('7f1e');
      expect(navigate).toHaveBeenCalledOnceWith('/dashboard', { replaceUrl: true });
      await fixture.whenStable();
      expect(router.url).toBe('/dashboard');
    });

    it('2. with 2FA the TOTP step appears and nothing redirects or is stored until a valid code is sent', async () => {
      const navigate = spyOn(router, 'navigateByUrl').and.callThrough();
      await reachTotpStep();

      expect(code()).not.toBeNull();
      expect(email()).toBeNull();
      expect(sessionStorage.getItem(SESSION_TOKEN_KEY)).toBeNull();
      expect(navigate).not.toHaveBeenCalled();

      await submitCode('482913');
      const req = http.expectOne('/api/auth/2fa/verify');
      // E-mail e senha seguem da primeira etapa, sem o usuário digitar de novo.
      expect(req.request.body).toEqual({ email: 'joao@acme.com', password: 'secret', totpCode: '482913' });
      req.flush({ status: 'AUTHENTICATED', sessionToken: '9ab3' });
      await settle();

      expect(sessionStorage.getItem(SESSION_TOKEN_KEY)).toBe('9ab3');
      expect(navigate).toHaveBeenCalledOnceWith('/dashboard', { replaceUrl: true });
    });

    it('3. wrong password, unknown e-mail, inactive and pending accounts all show the same single message', async () => {
      // O backend responde 401 {status: REJECTED} nos quatro casos; a tela não tem como distingui-los.
      const messages: string[] = [];
      for (const account of ['maria@acme.com', 'ghost@acme.com', 'inactive@acme.com', 'pending@acme.com']) {
        await submit(account, 'whatever');
        http.expectOne('/api/auth/login').flush({ status: 'REJECTED' }, { status: 401, statusText: 'Unauthorized' });
        await settle();
        messages.push(formError());
        expect(router.url).toBe('/');
        expect(q('h1').textContent).toContain('Entrar');
      }

      expect(new Set(messages)).toEqual(new Set([LOGIN_REJECTED_MESSAGE]));
      expect(LOGIN_REJECTED_MESSAGE).toContain('E-mail ou senha incorretos.');
      expect(sessionStorage.getItem(SESSION_TOKEN_KEY)).toBeNull();
    });

    it('3. after a rejection the e-mail stays, the password is cleared and focus returns to the e-mail', async () => {
      await submit('maria@acme.com', 'wrong');
      http.expectOne('/api/auth/login').flush({ status: 'REJECTED' }, { status: 401, statusText: 'Unauthorized' });
      await settle();

      expect(email().value).toBe('maria@acme.com');
      expect(password().value).toBe('');
      expect(document.activeElement).toBe(email());
      expect(el().innerHTML).not.toContain('wrong');
    });

    it('4. an invalid code shows a code-specific error and allows a retry without re-entering e-mail/password', async () => {
      await reachTotpStep();
      await submitCode('000000');
      http.expectOne('/api/auth/2fa/verify').flush({ status: 'REJECTED' }, { status: 401, statusText: 'Unauthorized' });
      await settle();

      expect(q('#login-code-error').textContent).toContain(TOTP_REJECTED_MESSAGE);
      expect(TOTP_REJECTED_MESSAGE).toContain('Código de verificação inválido.');
      expect(q('h1').textContent).toContain('Verificação em duas etapas');
      expect(code().value).toBe('');
      expect(document.activeElement).toBe(code());

      await submitCode('482913');
      const retry = http.expectOne('/api/auth/2fa/verify');
      expect(retry.request.body).toEqual({ email: 'joao@acme.com', password: 'secret', totpCode: '482913' });
      retry.flush({ status: 'AUTHENTICATED', sessionToken: '9ab3' });
      await settle();
      expect(sessionStorage.getItem(SESSION_TOKEN_KEY)).toBe('9ab3');
    });

    it('4. a malformed code is caught on the client and nothing is sent', async () => {
      await reachTotpStep();
      await submitCode('12ab');

      expect(q('#login-code-error').textContent).toContain('Informe o código de 6 dígitos.');
      http.expectNone('/api/auth/2fa/verify');
    });

    it('5. the dashboard is the only redirect target, whatever page was requested before', async () => {
      await router.navigateByUrl('/dashboard').catch(() => undefined);
      const navigate = spyOn(router, 'navigateByUrl').and.callThrough();
      await submit();
      http.expectOne('/api/auth/login').flush({ status: 'AUTHENTICATED', sessionToken: 'tok' });
      await settle();

      expect(navigate.calls.allArgs().map(args => args[0])).toEqual(['/dashboard']);
    });

    it('does not call the API when e-mail or password is missing', async () => {
      await submit('', '');
      expect(q('#login-email-error').textContent).toContain('Informe um e-mail válido.');
      expect(q('#login-password-error').textContent).toContain('Informe sua senha.');
      http.expectNone('/api/auth/login');
    });

    it('the TOTP step can go back to e-mail/password', async () => {
      await reachTotpStep();
      q<HTMLButtonElement>('.auth-link-btn').click();
      fixture.detectChanges();

      expect(q('h1').textContent).toContain('Entrar');
      expect(password().value).toBe('');
    });
  });

  describe('forgot password entry point', () => {
    it('8. shows a visibly labeled "Esqueci minha senha" link to the reset flow', () => {
      const link = Array.from(el().querySelectorAll('a')).find(a => a.textContent!.trim() === 'Esqueci minha senha')!;
      expect(link).toBeDefined();
      expect(link.getAttribute('href')).toBe('/forgot-password');
    });
  });

  describe('quality', () => {
    const technical = /\b(4\d\d|5\d\d)\b|status|unauthorized|error|exception|stack|at \w+\.|\{|\}|REJECTED|HttpErrorResponse/i;

    it('9. a 401 body, a 500 and a network failure never leak a status code, stack trace or raw JSON', async () => {
      const failures: Array<() => void> = [
        () => http.expectOne('/api/auth/login').flush({ status: 'REJECTED', trace: 'java.lang.Foo at Bar.baz' }, { status: 401, statusText: 'Unauthorized' }),
        () => http.expectOne('/api/auth/login').flush('<html>Internal Server Error</html>', { status: 500, statusText: 'Internal Server Error' }),
        () => http.expectOne('/api/auth/login').error(new ProgressEvent('error')),
      ];
      for (const fail of failures) {
        await submit();
        fail();
        await settle();
        expect(formError()).not.toBe('');
        expect(formError()).not.toMatch(technical);
      }
    });

    it('9. an unreachable server says so in plain language, not as a wrong password', async () => {
      await submit();
      http.expectOne('/api/auth/login').flush('boom', { status: 503, statusText: 'Service Unavailable' });
      await settle();

      expect(formError()).toBe(LOGIN_UNAVAILABLE_MESSAGE);
      expect(formError()).not.toContain('incorretos');
    });

    it('9. the TOTP step is clean of technical details too', async () => {
      await reachTotpStep();
      await submitCode('123456');
      http.expectOne('/api/auth/2fa/verify').flush({ status: 'REJECTED' }, { status: 401, statusText: 'Unauthorized' });
      await settle();

      expect(q('#login-code-error').textContent).not.toMatch(technical);
    });

    it('10. fits a 360px viewport without horizontal scroll and keeps the submit button reachable', () => {
      const host = fixture.nativeElement as HTMLElement;
      host.style.cssText = 'display:block;width:360px;overflow:hidden;';
      const button = q<HTMLButtonElement>('button[type=submit]');

      expect(host.scrollWidth).toBeLessThanOrEqual(360);
      expect(button.getBoundingClientRect().right).toBeLessThanOrEqual(360);
      expect(button.getBoundingClientRect().height).toBeGreaterThanOrEqual(44);
      host.style.cssText = '';
    });

    it('10. ships a dark-mode override driven by the OS preference', () => {
      const darkRules = Array.from(document.styleSheets).flatMap(sheet => {
        try { return Array.from(sheet.cssRules); } catch { return []; }
      }).filter(rule => rule instanceof CSSMediaRule && rule.conditionText.includes('prefers-color-scheme: dark'));

      expect(darkRules.length).toBeGreaterThan(0);
    });

    it('never echoes the password into the DOM', async () => {
      await submit('maria@acme.com', 'sup3r-secret');
      http.expectOne('/api/auth/login').flush({ status: 'REJECTED' }, { status: 401, statusText: 'Unauthorized' });
      await settle();

      expect(el().innerHTML).not.toContain('sup3r-secret');
      expect(password().value).toBe('');
    });
  });
});
