import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService, SESSION_TOKEN_KEY } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  const create = () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  };

  beforeEach(() => sessionStorage.removeItem(SESSION_TOKEN_KEY));
  afterEach(() => { http.verify(); sessionStorage.removeItem(SESSION_TOKEN_KEY); });

  it('starts without a session', () => {
    create();
    expect(service.isAuthenticated()).toBeFalse();
  });

  it('stores the session token on AUTHENTICATED', async () => {
    create();
    const result = service.login('maria@acme.com', 'secret');
    const req = http.expectOne('/api/auth/login');
    expect(req.request.body).toEqual({ email: 'maria@acme.com', password: 'secret' });
    req.flush({ status: 'AUTHENTICATED', sessionToken: '7f1e' });

    expect(await result).toBe('authenticated');
    expect(service.isAuthenticated()).toBeTrue();
    expect(sessionStorage.getItem(SESSION_TOKEN_KEY)).toBe('7f1e');
  });

  it('restores an existing session from storage', () => {
    sessionStorage.setItem(SESSION_TOKEN_KEY, 'abc');
    create();
    expect(service.isAuthenticated()).toBeTrue();
  });

  it('reports TOTP_REQUIRED without creating a session', async () => {
    create();
    const result = service.login('joao@acme.com', 'secret');
    http.expectOne('/api/auth/login').flush({ status: 'TOTP_REQUIRED', sessionToken: null });

    expect(await result).toBe('totp-required');
    expect(service.isAuthenticated()).toBeFalse();
  });

  it('sends e-mail, password and code to the 2FA endpoint', async () => {
    create();
    const result = service.verifyTotp('joao@acme.com', 'secret', '482913');
    const req = http.expectOne('/api/auth/2fa/verify');
    expect(req.request.body).toEqual({ email: 'joao@acme.com', password: 'secret', totpCode: '482913' });
    req.flush({ status: 'AUTHENTICATED', sessionToken: '9ab3' });

    expect(await result).toBe('authenticated');
    expect(service.isAuthenticated()).toBeTrue();
  });

  it('maps 401 REJECTED to rejected, with no session', async () => {
    create();
    const result = service.login('x@acme.com', 'bad');
    http.expectOne('/api/auth/login').flush({ status: 'REJECTED' }, { status: 401, statusText: 'Unauthorized' });

    expect(await result).toBe('rejected');
    expect(service.isAuthenticated()).toBeFalse();
  });

  it('maps server/network failures to unavailable, never to rejected', async () => {
    create();
    const result = service.login('x@acme.com', 'pw');
    http.expectOne('/api/auth/login').flush('boom', { status: 500, statusText: 'Server Error' });
    expect(await result).toBe('unavailable');
  });

  it('does not treat AUTHENTICATED without a token as a session', async () => {
    create();
    const result = service.login('x@acme.com', 'pw');
    http.expectOne('/api/auth/login').flush({ status: 'AUTHENTICATED', sessionToken: null });

    expect(await result).toBe('unavailable');
    expect(service.isAuthenticated()).toBeFalse();
  });
  describe('current user', () => {
    it('loads the logged user with the session token as a bearer credential', async () => {
      sessionStorage.setItem(SESSION_TOKEN_KEY, 'abc');
      create();
      const loading = service.loadCurrentUser();
      const req = http.expectOne('/api/auth/me');
      expect(req.request.headers.get('Authorization')).toBe('Bearer abc');
      req.flush({ name: 'Ana Souza', email: 'ana@acme.com', profile: 'Administrator' });
      await loading;

      expect(service.currentUser()).toEqual({ name: 'Ana Souza', email: 'ana@acme.com', profile: 'Administrator' });
    });

    it('has no user and makes no request without a session', async () => {
      create();
      await service.loadCurrentUser();

      expect(service.currentUser()).toBeNull();
      http.expectNone('/api/auth/me');
    });

    it('ends a session the server no longer recognises', async () => {
      sessionStorage.setItem(SESSION_TOKEN_KEY, 'stale');
      create();
      const loading = service.loadCurrentUser();
      http.expectOne('/api/auth/me').flush(null, { status: 401, statusText: 'Unauthorized' });
      await loading;

      expect(service.isAuthenticated()).toBeFalse();
      expect(service.currentUser()).toBeNull();
      expect(sessionStorage.getItem(SESSION_TOKEN_KEY)).toBeNull();
    });

    it('keeps the session when the server is merely unavailable', async () => {
      sessionStorage.setItem(SESSION_TOKEN_KEY, 'abc');
      create();
      const loading = service.loadCurrentUser();
      http.expectOne('/api/auth/me').flush(null, { status: 503, statusText: 'Service Unavailable' });
      await loading;

      expect(service.isAuthenticated()).toBeTrue();
      expect(service.currentUser()).toBeNull();
    });
  });
});
