import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PaymentMethod, PaymentMethodService } from './payment-method.service';

describe('PaymentMethodService', () => {
  let service: PaymentMethodService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(PaymentMethodService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads the methods from GET /api/auxiliary/payment-methods', async () => {
    const rows: PaymentMethod[] = [{ id: 'pm1', name: 'PIX', type: 'PIX' }];

    const loading = service.load();
    expect(service.status()).toBe('loading');
    http.expectOne('/api/auxiliary/payment-methods').flush(rows);
    await loading;

    expect(service.paymentMethods()).toEqual(rows);
    expect(service.status()).toBe('ready');
  });

  it('turns a failure into the error status without rejecting', async () => {
    const loading = service.load();
    http.expectOne('/api/auxiliary/payment-methods').flush(null, { status: 500, statusText: 'Server Error' });
    await loading;

    expect(service.status()).toBe('error');
    expect(service.paymentMethods()).toEqual([]);
  });
});
