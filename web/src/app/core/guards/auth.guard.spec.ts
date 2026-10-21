import { Component, Injector, runInInjectionContext } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRouteSnapshot, Route, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { routes } from '../../app.routes';
import { AuthService, SESSION_TOKEN_KEY } from '../services/auth.service';
import { HttpTestingController } from '@angular/common/http/testing';
import { adminGuard, authGuard, guestGuard } from './auth.guard';

/** Rotas do critério 6 do UC-M10-15 (as `/admin/*` vêm de `ADMIN_PATHS`). */
const AUTHENTICATED_PATHS = [
  '/', '/dashboard', '/pdv', '/nfe', '/nfe/nova', '/nfse', '/inventory', '/purchasing',
  '/crm', '/finance', '/reports', '/settings', '/settings/customers', '/settings/customers/1',
  '/settings/customers/1/payments',
];
const ADMIN_PATHS = [
  '/admin', '/admin/settings/planos', '/admin/customers', '/admin/customers/1', '/admin/customers/1/payments',
];

@Component({ standalone: true, template: 'LOGIN-PAGE' })
class LoginStub {}

@Component({ standalone: true, template: 'DASHBOARD' })
class DashboardStub {}

function setup(token?: string) {
  sessionStorage.removeItem(SESSION_TOKEN_KEY);
  if (token) sessionStorage.setItem(SESSION_TOKEN_KEY, token);
}

describe('route guards (UC-M10-15)', () => {
  beforeEach(() => setup());
  afterEach(() => sessionStorage.removeItem(SESSION_TOKEN_KEY));

  describe('guard unit', () => {
    function run(guard: typeof authGuard) {
      TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
      return runInInjectionContext(TestBed.inject(Injector), () =>
        guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot));
    }

    it('authGuard sends an anonymous visitor to /login', () => {
      const result = run(authGuard) as UrlTree;
      expect(TestBed.inject(Router).serializeUrl(result)).toBe('/login');
    });

    it('authGuard lets a session through', () => {
      setup('token');
      expect(run(authGuard)).toBeTrue();
    });

    it('guestGuard lets an anonymous visitor see the login', () => {
      expect(run(guestGuard)).toBeTrue();
    });

    it('guestGuard sends an active session straight to /dashboard', () => {
      setup('token');
      const result = run(guestGuard) as UrlTree;
      expect(TestBed.inject(Router).serializeUrl(result)).toBe('/dashboard');
    });
  });

  describe('adminGuard', () => {
    function setupAdmin() {
      TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
      const http = TestBed.inject(HttpTestingController);
      const run = () => runInInjectionContext(TestBed.inject(Injector), () =>
        adminGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)) as Promise<boolean | UrlTree>;
      return { http, run, url: (r: boolean | UrlTree) => TestBed.inject(Router).serializeUrl(r as UrlTree) };
    }
    const me = (profile: string | null) => ({ name: 'Ana', email: 'a@b.c', profile, companyId: null });

    it('sends an anonymous visitor to /login', async () => {
      const { run, url } = setupAdmin();
      expect(url(await run())).toBe('/login');
    });

    it('lets a GRAVITA_ADMINISTRATOR in', async () => {
      setup('token');
      const { http, run } = setupAdmin();
      const result = run();
      http.expectOne('/api/auth/me').flush(me('GRAVITA_ADMINISTRATOR'));
      expect(await result).toBeTrue();
    });

    it('sends any other profile back to /dashboard, even a tenant Administrator', async () => {
      setup('token');
      const { http, run, url } = setupAdmin();
      const result = run();
      http.expectOne('/api/auth/me').flush(me('Administrator'));
      expect(url(await result)).toBe('/dashboard');
    });

    it('sends the visitor to /login when the server no longer knows the session', async () => {
      setup('token');
      const { http, run, url } = setupAdmin();
      const result = run();
      http.expectOne('/api/auth/me').flush(null, { status: 401, statusText: 'Unauthorized' });
      expect(url(await result)).toBe('/login');
    });
  });

  describe('route table', () => {
    const protectedRoots = (): Route[] => routes.filter(r => r.path === 'admin' || r.path === '');

    it('protects both authenticated shells with authGuard on the route and its children', () => {
      expect(protectedRoots().length).toBe(2);
      for (const route of protectedRoots()) {
        // A área administrativa troca o authGuard pelo adminGuard, que também exige sessão.
        expect(route.canActivate).toContain(route.path === 'admin' ? adminGuard : authGuard);
        expect(route.canActivateChild).toContain(authGuard);
      }
    });

    it('keeps the auth pages public and guards only the login with guestGuard', () => {
      const find = (path: string) => routes.find(r => r.path === path)!;
      expect(find('login').canActivate).toEqual([guestGuard]);
      expect(find('forgot-password').canActivate).toBeUndefined();
      expect(find('reset-password').canActivate).toBeUndefined();
    });
  });

  describe('router without a session', () => {
    let harness: RouterTestingHarness;

    beforeEach(async () => {
      TestBed.configureTestingModule({
        providers: [
          provideHttpClient(), provideHttpClientTesting(),
          // Rotas reais; só o destino do redirecionamento (o login) é trocado por um stub.
          provideRouter(routes.map(r => (r.path === 'login' ? { ...r, loadComponent: () => LoginStub } : r))),
        ],
      });
      harness = await RouterTestingHarness.create();
    });

    for (const path of [...AUTHENTICATED_PATHS, ...ADMIN_PATHS]) {
      it(`blocks ${path} and never renders the protected view`, async () => {
        await harness.navigateByUrl(path);

        expect(TestBed.inject(Router).url).toBe('/login');
        const html = (harness.routeNativeElement as HTMLElement).outerHTML;
        expect(html).toContain('LOGIN-PAGE');
        expect(document.querySelector('app-shell, app-sidebar, app-admin-shell')).toBeNull();
      });
    }

    it('blocks an unknown URL too (wildcard goes through the guarded root)', async () => {
      await harness.navigateByUrl('/does-not-exist');
      expect(TestBed.inject(Router).url).toBe('/login');
    });
  });

  describe('router with a session', () => {
    it('sends a visitor from /login straight to /dashboard instead of showing the form', async () => {
      setup('token');
      TestBed.configureTestingModule({
        providers: [
          provideHttpClient(), provideHttpClientTesting(),
          provideRouter([
            { path: 'login', canActivate: [guestGuard], component: LoginStub },
            { path: 'dashboard', component: DashboardStub },
          ]),
        ],
      });
      const harness = await RouterTestingHarness.create();
      await harness.navigateByUrl('/login');

      expect(TestBed.inject(Router).url).toBe('/dashboard');
      expect((harness.routeNativeElement as HTMLElement).textContent).toContain('DASHBOARD');
    });
  });
});
