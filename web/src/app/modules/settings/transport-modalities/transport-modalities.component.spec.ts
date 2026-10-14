import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { AuthService, CurrentUser } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { TransportModalityOption } from '../../../core/services/transport-modality.service';
import { TransportModalitiesComponent } from './transport-modalities.component';

const URL = '/api/auxiliary/transport-modalities';

const seed = (): TransportModalityOption[] => [
  { id: 'tm-cif', name: 'CIF — Por conta do emitente', code: 'CIF', active: true },
  { id: 'tm-fob', name: 'FOB — Por conta do destinatário', code: 'FOB', active: true },
  { id: 'tm-none', name: 'Sem transporte', code: 'NONE', active: true },
];

describe('TransportModalitiesComponent', () => {
  let http: HttpTestingController;
  let toast: ToastService;

  const settle = async (fixture: { detectChanges(): void }) => { await new Promise(r => setTimeout(r)); fixture.detectChanges(); };
  const isList = (r: { url: string; method: string }) => r.url === URL && r.method === 'GET';

  async function render(profile: string, rowsToShow: TransportModalityOption[] = seed()) {
    const currentUser = signal<CurrentUser | null>({ name: 'Ricardo', email: 'ricardo@acme.com', profile, companyId: 'co1' });
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: AuthService, useValue: { currentUser } }],
    });
    http = TestBed.inject(HttpTestingController);
    toast = TestBed.inject(ToastService);
    const fixture = TestBed.createComponent(TransportModalitiesComponent);
    fixture.detectChanges();
    const list = http.expectOne(isList);
    expect(list.request.params.get('includeInactive')).toBe('true');
    list.flush(rowsToShow);
    await settle(fixture);
    return { fixture, el: fixture.nativeElement as HTMLElement };
  }

  const rows = (el: HTMLElement) => Array.from(el.querySelectorAll('tbody tr'));
  const type = (el: HTMLElement, id: string, value: string) => {
    const field = el.querySelector(`#${id}`) as HTMLInputElement | HTMLSelectElement;
    field.value = value;
    field.dispatchEvent(new Event('input'));
    field.dispatchEvent(new Event('change'));
  };
  const submitButton = (el: HTMLElement) => el.querySelector('form button[type=submit]') as HTMLButtonElement;
  const buttonNamed = (el: HTMLElement, label: string) =>
    Array.from(el.querySelectorAll('button')).find(b => b.getAttribute('aria-label') === label) as HTMLButtonElement;
  const openNew = (el: HTMLElement) =>
    (Array.from(el.querySelectorAll('button')).find(b => b.textContent!.includes('Nova modalidade')) as HTMLButtonElement).click();

  afterEach(() => http.verify());

  it('lists every modality from the API with its code and status', async () => {
    const { el } = await render('Administrator', [...seed().slice(0, 2), { ...seed()[2], active: false }]);

    expect(rows(el).length).toBe(3);
    expect(rows(el)[0].textContent).toContain('CIF — Por conta do emitente');
    expect(rows(el)[0].textContent).toContain('CIF');
    expect(rows(el)[0].textContent).toContain('Ativo');
    expect(rows(el)[2].textContent).toContain('Sem transporte');
    expect(rows(el)[2].textContent).toContain('Inativo');
  });

  it('shows a retry when the list cannot be loaded', async () => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(),
        { provide: AuthService, useValue: { currentUser: signal({ profile: 'Administrator' }) } }],
    });
    http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(TransportModalitiesComponent);
    fixture.detectChanges();
    http.expectOne(isList).flush(null, { status: 500, statusText: 'Server Error' });
    await settle(fixture);
    const el = fixture.nativeElement as HTMLElement;

    expect(el.querySelector('[role=alert]')!.textContent).toContain('Não foi possível carregar');
    (el.querySelector('[role=alert] button') as HTMLButtonElement).click();
    http.expectOne(isList).flush(seed());
    await settle(fixture);
    expect(rows(el).length).toBe(3);
  });

  it('creates a modality: POSTs name and code, re-reads the list and closes the form', async () => {
    const { fixture, el } = await render('Administrator', seed().slice(0, 1));
    const success = spyOn(toast, 'success');
    expect(el.querySelector('form')).toBeNull();

    openNew(el);
    fixture.detectChanges();
    expect(submitButton(el).disabled).toBeTrue();
    expect(Array.from(el.querySelectorAll('#tm-code option')).map(o => (o as HTMLOptionElement).value)).toEqual(['', 'CIF', 'FOB', 'NONE']);
    expect(el.querySelector('#tm-active')).toBeNull();
    type(el, 'tm-name', '  FOB — Por conta do destinatário ');
    type(el, 'tm-code', 'FOB');
    fixture.detectChanges();
    expect(submitButton(el).disabled).toBeFalse();

    submitButton(el).click();
    const post = http.expectOne(URL);
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual({ name: 'FOB — Por conta do destinatário', code: 'FOB' });
    post.flush({ ...seed()[1] }, { status: 201, statusText: 'Created' });
    await settle(fixture);
    http.expectOne(isList).flush(seed().slice(0, 2));
    await settle(fixture);

    expect(rows(el).length).toBe(2);
    expect(el.querySelector('form')).toBeNull();
    expect(success).toHaveBeenCalled();
  });

  it('keeps the form and shows the server message inline when the create is refused (409)', async () => {
    const { fixture, el } = await render('Administrator');

    openNew(el);
    fixture.detectChanges();
    type(el, 'tm-name', 'CIF outra');
    type(el, 'tm-code', 'CIF');
    fixture.detectChanges();
    submitButton(el).click();
    http.expectOne(URL).flush('An active transport modality with code CIF already exists', { status: 409, statusText: 'Conflict' });
    await settle(fixture);

    expect(el.querySelector('form .form-error')!.textContent).toContain('already exists');
    expect((el.querySelector('#tm-name') as HTMLInputElement).value).toBe('CIF outra');
  });

  it('edits a modality: pre-fills the form, PATCHes name, code and active, then re-reads the list', async () => {
    const { fixture, el } = await render('Administrator');

    buttonNamed(el, 'Editar CIF — Por conta do emitente').click();
    fixture.detectChanges();
    expect((el.querySelector('#tm-name') as HTMLInputElement).value).toBe('CIF — Por conta do emitente');
    expect((el.querySelector('#tm-code') as HTMLSelectElement).value).toBe('CIF');
    expect((el.querySelector('#tm-active') as HTMLInputElement).checked).toBeTrue();

    type(el, 'tm-name', 'CIF — frete do emitente');
    fixture.detectChanges();
    submitButton(el).click();
    const patch = http.expectOne(`${URL}/tm-cif`);
    expect(patch.request.method).toBe('PATCH');
    expect(patch.request.body).toEqual({ name: 'CIF — frete do emitente', code: 'CIF', active: true });
    patch.flush({});
    await settle(fixture);
    http.expectOne(isList).flush([{ ...seed()[0], name: 'CIF — frete do emitente' }, ...seed().slice(1)]);
    await settle(fixture);

    expect(rows(el)[0].textContent).toContain('CIF — frete do emitente');
    expect(el.querySelector('form')).toBeNull();
  });

  it('deactivates after confirming and does nothing if the confirmation is declined', async () => {
    const { fixture, el } = await render('Administrator');
    const deactivateFob = () => buttonNamed(el, 'Desativar FOB — Por conta do destinatário');

    spyOn(window, 'confirm').and.returnValue(false);
    deactivateFob().click();
    http.expectNone(`${URL}/tm-fob`);

    (window.confirm as jasmine.Spy).and.returnValue(true);
    deactivateFob().click();
    const del = http.expectOne(`${URL}/tm-fob`);
    expect(del.request.method).toBe('DELETE');
    del.flush(null, { status: 204, statusText: 'No Content' });
    await settle(fixture);
    http.expectOne(isList).flush([seed()[0], { ...seed()[1], active: false }, seed()[2]]);
    await settle(fixture);

    expect(rows(el).length).toBe(3);
    expect(rows(el)[1].textContent).toContain('Inativo');
    expect(buttonNamed(el, 'Reativar FOB — Por conta do destinatário')).toBeTruthy();
  });

  it('reactivates an inactive modality by PATCHing active=true', async () => {
    const { fixture, el } = await render('Administrator', [seed()[0], { ...seed()[1], active: false }]);

    buttonNamed(el, 'Reativar FOB — Por conta do destinatário').click();
    const patch = http.expectOne(`${URL}/tm-fob`);
    expect(patch.request.body).toEqual({ name: 'FOB — Por conta do destinatário', code: 'FOB', active: true });
    patch.flush({});
    await settle(fixture);
    http.expectOne(isList).flush(seed().slice(0, 2));
    await settle(fixture);

    expect(rows(el)[1].textContent).toContain('Ativo');
  });

  it('is read-only for non-administrators: the list is visible but there are no create/edit/deactivate controls', async () => {
    const { el } = await render('Salesperson');

    expect(rows(el).length).toBe(3);
    expect(el.textContent).not.toContain('Nova modalidade');
    expect(el.querySelector('th.td-actions')).toBeNull();
    expect(el.querySelectorAll('tbody button').length).toBe(0);
  });
});
