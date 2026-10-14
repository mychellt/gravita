import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { SESSION_TOKEN_KEY } from '../../core/services/auth.service';
import { SettingsComponent } from './settings.component';

@Component({ standalone: true, template: '<p id="stub">lista de clientes</p>' })
class CustomersStubComponent {}

@Component({ standalone: true, template: '<p id="modalities-stub">modalidades de frete</p>' })
class ModalitiesStubComponent {}

describe('SettingsComponent — Clientes no menu lateral', () => {
  let http: HttpTestingController;
  const settle = async (f: { detectChanges(): void }) => { await new Promise(r => setTimeout(r)); f.detectChanges(); };

  async function open(url: string) {
    sessionStorage.setItem(SESSION_TOKEN_KEY, 'tok');
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(),
        provideRouter([{ path: 'settings', component: SettingsComponent, children: [
          { path: 'customers', component: CustomersStubComponent },
          { path: 'transport-modalities', component: ModalitiesStubComponent },
        ] }])],
    });
    http = TestBed.inject(HttpTestingController);
    const router = TestBed.inject(Router);
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl(url);
    const fixture = harness.fixture;
    http.expectOne('/api/auth/me').flush({ name: 'R', email: 'r@e.com', profile: 'Administrator', companyId: 'co1' });
    await settle(fixture);
    http.expectOne('/api/companies/co1').flush({ id: 'co1', name: 'Acme', cnpj: '1', taxRegime: 'SIMPLES_NACIONAL' });
    await settle(fixture);
    return { fixture, router, el: fixture.nativeElement as HTMLElement };
  }
  const active = (el: HTMLElement) => el.querySelector('.settings-menu-item.active')?.textContent?.trim();
  const item = (el: HTMLElement, label: string) =>
    Array.from(el.querySelectorAll('.settings-menu-item')).find(i => i.textContent!.includes(label)) as HTMLElement;

  afterEach(() => {
    http.match('/api/plans').forEach(req => req.flush([]));
    http.verify(); sessionStorage.removeItem(SESSION_TOKEN_KEY);
  });

  it('highlights Clientes in the menu when the customers route is open', async () => {
    const { el } = await open('/settings/customers');

    expect(active(el)).toBe('Clientes');
    expect(el.querySelector('.settings-menu')).not.toBeNull();
  });

  it('goes back to the settings page when another menu item is picked', async () => {
    const { fixture, router, el } = await open('/settings/customers');

    item(el, 'Integrações').click();
    await settle(fixture);

    expect(router.url).toBe('/settings');
    expect(active(el)).toBe('Integrações');
  });

  it('opens the customers route from the Clientes menu item', async () => {
    const { fixture, router, el } = await open('/settings');

    item(el, 'Clientes').click();
    await settle(fixture);

    expect(router.url).toBe('/settings/customers');
    expect(active(el)).toBe('Clientes');
    expect(el.querySelector('#stub')).not.toBeNull();
  });

  it('opens the transport-modalities route from the Modalidades de frete menu item, and highlights it on a deep link', async () => {
    const { fixture, router, el } = await open('/settings');

    item(el, 'Modalidades de frete').click();
    await settle(fixture);

    expect(router.url).toBe('/settings/transport-modalities');
    expect(active(el)).toBe('Modalidades de frete');
    expect(el.querySelector('#modalities-stub')).not.toBeNull();
    expect(el.textContent).not.toContain('Em desenvolvimento');
  });

  it('highlights Modalidades de frete when its route is opened directly and leaves it for another menu item', async () => {
    const { fixture, router, el } = await open('/settings/transport-modalities');

    expect(active(el)).toBe('Modalidades de frete');

    item(el, 'Clientes').click();
    await settle(fixture);
    expect(router.url).toBe('/settings/customers');
    expect(active(el)).toBe('Clientes');
  });
});
