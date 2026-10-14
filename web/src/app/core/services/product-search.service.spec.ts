import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ProductSearchService } from './product-search.service';

describe('ProductSearchService', () => {
  let service: ProductSearchService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(ProductSearchService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('searches by term and customer', async () => {
    const searching = service.search('cola', 'c1');
    const req = http.expectOne(r => r.url === '/api/pdv/products/search');
    expect(req.request.params.get('q')).toBe('cola');
    expect(req.request.params.get('customerId')).toBe('c1');
    req.flush([{ productId: 'p', description: 'COLA', ncm: '2202', unitPrice: 5, availableForSale: true }]);

    expect((await searching)[0].ncm).toBe('2202');
  });

  it('omits customerId when there is none', async () => {
    const searching = service.search('cola');
    const req = http.expectOne(r => r.url === '/api/pdv/products/search');
    expect(req.request.params.has('customerId')).toBeFalse();
    req.flush([]);
    await searching;
  });
});
