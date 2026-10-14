import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { CfopRegistryEntry, CfopRegistryService } from './cfop-registry.service';

describe('CfopRegistryService', () => {
  let service: CfopRegistryService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(CfopRegistryService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads the registry from GET /api/auxiliary/cfop-registry', async () => {
    const rows: CfopRegistryEntry[] = [
      { naturezaOperacao: 'VENDA', cfop: '5102', description: 'Venda de mercadoria', requiresReferencedAccessKey: false },
    ];

    const loading = service.load();
    expect(service.status()).toBe('loading');
    http.expectOne('/api/auxiliary/cfop-registry').flush(rows);
    await loading;

    expect(service.entries()).toEqual(rows);
    expect(service.status()).toBe('ready');
  });

  it('turns a failure into the error status without rejecting', async () => {
    const loading = service.load();
    http.expectOne('/api/auxiliary/cfop-registry').flush(null, { status: 500, statusText: 'Server Error' });
    await loading;

    expect(service.status()).toBe('error');
    expect(service.entries()).toEqual([]);
  });
});
