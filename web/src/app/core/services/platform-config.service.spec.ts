import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { aPlan } from '../testing/plan.fixture';
import { PlatformConfigService } from './platform-config.service';

const bronze = aPlan();
const silver = aPlan({ id: 'silver-id', tier: 'SILVER', name: 'Silver', priceMonthly: 597, priceAnnual: 497, featured: true });
const gold = aPlan({ id: 'gold-id', tier: 'GOLD', name: 'Gold', priceMonthly: 1197, priceAnnual: 997 });

describe('PlatformConfigService', () => {
  let http: HttpTestingController;
  let service: PlatformConfigService;

  /** Creates the service (which starts loading plans) and answers GET /api/plans. */
  function createWithPlans(plans = [silver, gold, bronze]) {
    service = TestBed.inject(PlatformConfigService);
    http.expectOne('/api/plans').flush(plans);
  }

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  describe('loading plans', () => {
    it('starts loading on creation and exposes what the API returns, cheapest first', async () => {
      service = TestBed.inject(PlatformConfigService);
      expect(service.plansStatus()).toBe('loading');
      http.expectOne('/api/plans').flush([silver, gold, bronze]);
      await Promise.resolve();

      expect(service.plansStatus()).toBe('ready');
      expect(service.config().plans.map(p => p.name)).toEqual(['Bronze', 'Silver', 'Gold']);
    });

    it('reports a connection failure and can retry', async () => {
      service = TestBed.inject(PlatformConfigService);
      http.expectOne('/api/plans').error(new ProgressEvent('error'));
      await service.loadPlans();

      expect(service.plansStatus()).toBe('error');
      expect(service.plansError()).toContain('sem conexão com o servidor');
      expect(service.config().plans).toEqual([]);

      const retry = service.loadPlans();
      http.expectOne('/api/plans').flush([bronze]);
      await retry;
      expect(service.plansStatus()).toBe('ready');
      expect(service.plansError()).toBe('');
    });

    it('reports a server error with its status', async () => {
      service = TestBed.inject(PlatformConfigService);
      http.expectOne('/api/plans').flush('boom', { status: 503, statusText: 'Service Unavailable' });
      await service.loadPlans();

      expect(service.plansStatus()).toBe('error');
      expect(service.plansError()).toContain('erro 503');
    });

    it('shares one request between concurrent loads', async () => {
      service = TestBed.inject(PlatformConfigService);
      const second = service.loadPlans();
      http.expectOne('/api/plans').flush([bronze]);
      await second;
      expect(service.config().plans.length).toBe(1);
    });
  });

  describe('saving plans', () => {
    beforeEach(async () => {
      createWithPlans();
      await Promise.resolve();
    });

    it('sends a PUT only for the plan that changed and keeps the server response', async () => {
      const draft = service.snapshot();
      draft.plans[0].priceMonthly = 310;

      const saving = service.save(draft, 'Ana');
      const req = http.expectOne('/api/plans/' + bronze.id);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body.priceMonthly).toBe(310);
      req.flush({ ...draft.plans[0], priceMonthly: 310.5 });
      await saving;

      expect(service.config().plans[0].priceMonthly).toBe(310.5);
      expect(service.lastSaved()?.by).toBe('Ana');
    });

    it('makes no plan request when no plan changed', async () => {
      await service.save(service.snapshot(), 'Ana');

      http.expectNone(() => true);
      expect(service.lastSaved()?.by).toBe('Ana');
    });

    it('POSTs a new plan without its client id and adopts the server-issued id', async () => {
      const draft = service.snapshot();
      draft.plans.push(aPlan({ id: 'tmp-1', name: 'Platinum', priceMonthly: 2000, priceAnnual: 1800 }));

      const saving = service.save(draft, 'Ana');
      const req = http.expectOne('/api/plans');
      expect(req.request.method).toBe('POST');
      expect('id' in req.request.body).toBeFalse();
      req.flush({ ...draft.plans[3], id: 'server-uuid' }, { status: 201, statusText: 'Created' });
      await saving;

      expect(service.config().plans.map(p => p.id)).toContain('server-uuid');
      expect(service.config().plans.map(p => p.id)).not.toContain('tmp-1');
    });

    it('DELETEs a plan removed from the draft', async () => {
      const draft = service.snapshot();
      draft.plans = draft.plans.filter(p => p.tier !== 'GOLD');

      const saving = service.save(draft, 'Ana');
      const req = http.expectOne('/api/plans/gold-id');
      expect(req.request.method).toBe('DELETE');
      req.flush(null, { status: 204, statusText: 'No Content' });
      await saving;

      expect(service.config().plans.map(p => p.tier)).toEqual(['BRONZE', 'SILVER']);
    });

    it('rejects with the backend validation message and leaves the saved plans untouched', async () => {
      const draft = service.snapshot();
      draft.plans[0].priceAnnual = 999;

      const saving = service.save(draft, 'Ana');
      http.expectOne('/api/plans/' + bronze.id)
        .flush('Plan priceAnnual cannot be greater than priceMonthly', { status: 409, statusText: 'Conflict' });

      await expectAsync(saving).toBeRejectedWithError(/priceAnnual cannot be greater than priceMonthly/);
      expect(service.config().plans[0].priceAnnual).toBe(247);
      expect(service.lastSaved()).toBeNull();
    });

    it('keeps what was already saved when a later call fails', async () => {
      const draft = service.snapshot();
      draft.plans[0].priceMonthly = 300;
      draft.plans[1].priceMonthly = 650;

      const saving = service.save(draft, 'Ana');
      http.expectOne('/api/plans/' + bronze.id).flush({ ...draft.plans[0] });
      await Promise.resolve();
      await Promise.resolve();
      http.expectOne('/api/plans/silver-id').flush('Plan name taken', { status: 409, statusText: 'Conflict' });

      await expectAsync(saving).toBeRejected();
      expect(service.config().plans[0].priceMonthly).toBe(300);
      expect(service.config().plans[1].priceMonthly).toBe(597);
    });
  });
});
