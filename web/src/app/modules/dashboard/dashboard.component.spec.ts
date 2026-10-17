import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { DataService } from '../../core/services/data.service';
import { NfeListItem } from '../../core/services/nfe.service';
import { DashboardComponent } from './dashboard.component';

const nfe = (id: string, daysAgo: number): NfeListItem => ({
  id, documentNumber: Number(id.replace(/\D/g, '')) || 1, documentSeries: '1', recipientName: `Cliente ${id}`, cfop: '5102',
  emittedAt: new Date(Date.now() - daysAgo * 86_400_000).toISOString(), totalValue: 100, icmsValue: 18, status: 'AUTHORIZED',
  cancellationJustification: null, cancelledAt: null, paymentMethodType: null,
});

describe('DashboardComponent — Últimas NF-e emitidas', () => {
  let http: HttpTestingController;

  async function create(response: NfeListItem[] | 'fail') {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
    http = TestBed.inject(HttpTestingController);
    const component = TestBed.runInInjectionContext(() => new DashboardComponent(TestBed.inject(DataService)));
    const request = http.expectOne('/api/nfe');
    response === 'fail' ? request.flush(null, { status: 500, statusText: 'Server Error' }) : request.flush(response);
    await new Promise(r => setTimeout(r));
    return component;
  }
  afterEach(() => http.verify());

  it('lists the 5 most recent issued NF-e from the backend, newest first', async () => {
    const c = await create([nfe('n1', 6), nfe('n2', 1), nfe('n3', 5), nfe('n4', 2), nfe('n5', 4), nfe('n6', 3), nfe('n7', 0)]);

    expect(c.nfeStatus()).toBe('ready');
    expect(c.recentNfes().map(n => n.id)).toEqual(['n7', 'n2', 'n4', 'n6', 'n5']);
  });

  it('reports a load failure so the card can offer a retry', async () => {
    const c = await create('fail');

    expect(c.nfeStatus()).toBe('error');
    expect(c.recentNfes()).toEqual([]);
    c.reloadNfes();
    http.expectOne('/api/nfe').flush([]);
  });
});
