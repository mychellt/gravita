import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { SESSION_TOKEN_KEY } from '../../core/services/auth.service';
import { SettingsComponent } from './settings.component';

describe('SettingsComponent — Usuários menu', () => {
  let http: HttpTestingController;
  const settle = async (f: { detectChanges(): void }) => { await new Promise(r => setTimeout(r)); f.detectChanges(); };

  async function open(profile: string) {
    sessionStorage.setItem(SESSION_TOKEN_KEY, 'tok');
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
    http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(SettingsComponent);
    fixture.detectChanges();
    http.expectOne('/api/auth/me').flush({ name: 'R', email: 'r@e.com', profile, companyId: 'co1' });
    await settle(fixture);
    http.expectOne('/api/companies/co1').flush({ id: 'co1', name: 'Acme', cnpj: '1', taxRegime: 'SIMPLES_NACIONAL' });
    await settle(fixture);
    return fixture;
  }
  const menuLabels = (el: HTMLElement) => Array.from(el.querySelectorAll('.settings-menu-item')).map(i => i.textContent!.trim());

  afterEach(() => {
    http.match('/api/plans').forEach(req => req.flush([]));
    http.verify(); sessionStorage.removeItem(SESSION_TOKEN_KEY);
  });

  it('hides the Usuários menu item from every profile but the Administrator', async () => {
    const fixture = await open('Salesperson');

    expect(menuLabels(fixture.nativeElement)).not.toContain('Usuários');
    expect(menuLabels(fixture.nativeElement)).toContain('Empresa');
  });

  it('shows the Usuários item to the Administrator and loads the real list when it is opened', async () => {
    const fixture = await open('Administrator');
    const el = fixture.nativeElement as HTMLElement;

    const item = Array.from(el.querySelectorAll('.settings-menu-item')).find(i => i.textContent!.includes('Usuários')) as HTMLElement;
    item.click();
    fixture.detectChanges();
    await settle(fixture);
    http.expectOne('/api/users').flush([{ id: 'u1', name: 'Real User', email: 'real@acme.com', profileId: 'p1',
      profileName: 'Administrator', twoFactorEnabled: true, status: 'ACTIVE' }]);
    http.expectOne('/api/profiles').flush([{ id: 'p1', name: 'Administrator' }]);
    await settle(fixture);

    expect(el.textContent).toContain('Real User');
    expect(el.textContent).not.toContain('Carlos Mendes');
    expect(el.textContent).toContain('Novo Usuário');
  });
});
