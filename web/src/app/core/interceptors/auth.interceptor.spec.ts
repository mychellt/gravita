import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { SESSION_TOKEN_KEY } from '../services/auth.service';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let controller: HttpTestingController;

  const setup = (token: string | null) => {
    if (token) sessionStorage.setItem(SESSION_TOKEN_KEY, token);
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
  };

  afterEach(() => { controller.verify(); sessionStorage.removeItem(SESSION_TOKEN_KEY); });

  it('sends the session token as a bearer credential on API calls', () => {
    setup('abc');
    http.get('/api/customers').subscribe();
    expect(controller.expectOne('/api/customers').request.headers.get('Authorization')).toBe('Bearer abc');
  });

  it('adds nothing without a session', () => {
    setup(null);
    http.get('/api/customers').subscribe();
    expect(controller.expectOne('/api/customers').request.headers.has('Authorization')).toBeFalse();
  });

  it('keeps login and 2FA anonymous even when an old session exists', () => {
    setup('stale');
    http.post('/api/auth/login', {}).subscribe();
    http.post('/api/auth/2fa/verify', {}).subscribe();
    expect(controller.expectOne('/api/auth/login').request.headers.has('Authorization')).toBeFalse();
    expect(controller.expectOne('/api/auth/2fa/verify').request.headers.has('Authorization')).toBeFalse();
  });

  it('does not touch other origins or a header that is already set', () => {
    setup('abc');
    http.get('https://example.com/data').subscribe();
    http.get('/api/auth/me', { headers: { Authorization: 'Bearer own' } }).subscribe();
    expect(controller.expectOne('https://example.com/data').request.headers.has('Authorization')).toBeFalse();
    expect(controller.expectOne('/api/auth/me').request.headers.get('Authorization')).toBe('Bearer own');
  });
});
