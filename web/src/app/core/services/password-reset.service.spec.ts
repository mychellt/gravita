import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PasswordResetService } from './password-reset.service';

describe('PasswordResetService', () => {
  let http: HttpTestingController;
  let service: PasswordResetService;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    service = TestBed.inject(PasswordResetService);
  });

  afterEach(() => http.verify());

  describe('request', () => {
    it('posts the e-mail and reports acceptance, whatever the body says', async () => {
      const result = service.request('ana@acme.com');
      const req = http.expectOne('/api/auth/password-reset');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ email: 'ana@acme.com' });
      req.flush({ message: 'qualquer coisa' }, { status: 202, statusText: 'Accepted' });

      expect(await result).toBe('accepted');
    });

    it('tells a malformed address (400) from an unreachable server', async () => {
      const malformed = service.request('x');
      http.expectOne('/api/auth/password-reset').flush({}, { status: 400, statusText: 'Bad Request' });
      expect(await malformed).toBe('invalid-email');

      const down = service.request('ana@acme.com');
      http.expectOne('/api/auth/password-reset').error(new ProgressEvent('error'));
      expect(await down).toBe('unavailable');

      const broken = service.request('ana@acme.com');
      http.expectOne('/api/auth/password-reset').flush('boom', { status: 500, statusText: 'Server Error' });
      expect(await broken).toBe('unavailable');
    });
  });

  describe('confirm', () => {
    function confirmWith(status: number, body: object | string = {}) {
      const result = service.confirm('tok-123', 'n3w-pass');
      const req = http.expectOne('/api/auth/password-reset/confirm');
      expect(req.request.method).toBe('POST');
      // O token e a senha vão no corpo: nunca na URL.
      expect(req.request.body).toEqual({ token: 'tok-123', newPassword: 'n3w-pass' });
      expect(req.request.urlWithParams).not.toContain('tok-123');
      req.flush(body, { status, statusText: String(status) });
      return result;
    }

    it('succeeds on 200', async () => {
      expect(await confirmWith(200, { message: 'ok' })).toBe('success');
    });

    it('maps 410 EXPIRED, 410 ALREADY_USED and 400 INVALID apart', async () => {
      expect(await confirmWith(410, { reason: 'EXPIRED' })).toBe('expired');
      expect(await confirmWith(410, { reason: 'ALREADY_USED' })).toBe('used');
      expect(await confirmWith(400, { reason: 'INVALID' })).toBe('invalid');
    });

    it('treats an unknown 410 reason as invalid and a 400 without reason as a refused password', async () => {
      expect(await confirmWith(410, {})).toBe('invalid');
      expect(await confirmWith(400, { message: 'Dados inválidos.' })).toBe('rejected');
    });

    it('reports server errors and connection failures as unavailable', async () => {
      expect(await confirmWith(500, 'boom')).toBe('unavailable');

      const result = service.confirm('tok-123', 'n3w-pass');
      http.expectOne('/api/auth/password-reset/confirm').error(new ProgressEvent('error'));
      expect(await result).toBe('unavailable');
    });
  });
});
