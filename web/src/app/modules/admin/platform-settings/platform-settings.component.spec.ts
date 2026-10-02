import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, TestRequest, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { Plan } from '../../../core/models';
import { ToastService } from '../../../core/services/toast.service';
import { aPlan } from '../../../core/testing/plan.fixture';
import { PlatformSettingsComponent } from './platform-settings.component';

const bronze = aPlan();
const silver = aPlan({ id: 'silver-id', tier: 'SILVER', name: 'Silver', priceMonthly: 597, priceAnnual: 497, featured: true });

describe('PlatformSettingsComponent — plans', () => {
  let fixture: ComponentFixture<PlatformSettingsComponent>;
  let component: PlatformSettingsComponent;
  let http: HttpTestingController;
  let toast: ToastService;

  const el = () => fixture.nativeElement as HTMLElement;
  const settle = async () => { await new Promise(resolve => setTimeout(resolve)); fixture.detectChanges(); };

  async function open(respond: (req: TestRequest) => void) {
    fixture = TestBed.createComponent(PlatformSettingsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    respond(http.expectOne('/api/plans'));
    await settle();
  }

  const openWithPlans = (plans: Plan[] = [silver, bronze]) => open(req => req.flush(plans));

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [PlatformSettingsComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    toast = TestBed.inject(ToastService);
  });

  afterEach(() => http.verify());

  it('shows a loading state until the plans arrive', async () => {
    fixture = TestBed.createComponent(PlatformSettingsComponent);
    fixture.detectChanges();
    expect(el().textContent).toContain('Carregando planos');

    http.expectOne('/api/plans').flush([bronze]);
    await settle();
    expect(el().textContent).not.toContain('Carregando planos');
  });

  it('renders the plans fetched from the API', async () => {
    await openWithPlans();

    const tabs = Array.from(el().querySelectorAll('.plan-tab-name')).map(t => t.textContent!.trim());
    expect(tabs.some(t => t.startsWith('Bronze'))).toBeTrue();
    expect(tabs.some(t => t.startsWith('Silver'))).toBeTrue();
    expect(component.draft.plans.map(p => p.name)).toEqual(['Bronze', 'Silver']);
    expect(component.selectedPlanId()).toBe('silver-id'); // the featured one
    expect(component.dirty).toBeFalse();
  });

  it('shows an error with a retry button when the fetch fails, then recovers', async () => {
    await open(req => req.flush('boom', { status: 500, statusText: 'Server Error' }));

    expect(el().querySelector('.alert.danger')?.textContent).toContain('Não foi possível carregar os planos');
    expect(el().querySelector('.plan-tab')).toBeNull();
    expect(component.errorTotal).toBe(0); // an empty, unloaded list is not a validation error

    const retry = el().querySelector<HTMLButtonElement>('.alert.danger button')!;
    retry.click();
    http.expectOne('/api/plans').flush([bronze]);
    await settle();

    expect(el().querySelector('.alert.danger')).toBeNull();
    expect(el().querySelector('.plan-tab')).not.toBeNull();
  });

  it('saves an edited plan with PUT and reflects the server response', async () => {
    await openWithPlans();
    spyOn(toast, 'success');
    component.draft.plans.find(p => p.id === bronze.id)!.priceMonthly = 320;
    fixture.detectChanges();
    expect(component.dirty).toBeTrue();

    const saving = component.save();
    const req = http.expectOne('/api/plans/' + bronze.id);
    expect(req.request.method).toBe('PUT');
    req.flush({ ...req.request.body, id: bronze.id, priceMonthly: 320.5 });
    await saving;
    fixture.detectChanges();

    expect(component.draft.plans.find(p => p.id === bronze.id)!.priceMonthly).toBe(320.5);
    expect(component.dirty).toBeFalse();
    expect(toast.success).toHaveBeenCalled();
  });

  it('surfaces a backend validation error and keeps the draft', async () => {
    await openWithPlans();
    spyOn(toast, 'danger');
    component.draft.plans.find(p => p.id === bronze.id)!.priceMonthly = 320;
    fixture.detectChanges();

    const saving = component.save();
    http.expectOne('/api/plans/' + bronze.id)
      .flush('Plan priceAnnual cannot be greater than priceMonthly', { status: 409, statusText: 'Conflict' });
    await saving;
    fixture.detectChanges();

    expect(toast.danger).toHaveBeenCalledWith(jasmine.stringMatching(/priceAnnual cannot be greater than priceMonthly/));
    expect(component.draft.plans.find(p => p.id === bronze.id)!.priceMonthly).toBe(320);
    expect(component.dirty).toBeTrue();
    expect(component.saving()).toBeFalse();
  });

  it('does not call the API while the form has validation errors', async () => {
    await openWithPlans();
    spyOn(toast, 'danger');
    component.draft.plans.find(p => p.id === bronze.id)!.priceAnnual = 9999; // above the monthly price
    fixture.detectChanges();

    await component.save();

    expect(toast.danger).toHaveBeenCalled();
    http.expectNone(() => true);
  });
});
