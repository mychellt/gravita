import { Component, OnInit, computed, effect, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { Company, CompanyService, TaxRegime, failureDetail } from '../../core/services/company.service';
import { ToastService } from '../../core/services/toast.service';
import { ADMINISTRATOR_PROFILE } from '../../core/user-display';
import { PlatformConfigService } from '../../core/services/platform-config.service';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { UsersPanelComponent } from './users/users-panel.component';

const CUSTOMERS_MENU = 'clientes';
const CUSTOMERS_ROUTE = '/settings/customers';

interface SettingsMenu { key: string; label: string; icon: string; route?: string; adminOnly?: boolean; }

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [FormsModule, RouterOutlet, PageHeaderComponent, UsersPanelComponent],
  styleUrl: './settings.component.scss',
  templateUrl: './settings.component.html'
})
export class SettingsComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly companies = inject(CompanyService);

  constructor(private toast: ToastService, private platform: PlatformConfigService, private router: Router) {
    // Clientes é uma rota filha: o menu acompanha a URL (inclusive voltar/avançar do navegador).
    effect(() => {
      if (this.inCustomers()) this.activeMenu.set(CUSTOMERS_MENU);
      else if (this.activeMenu() === CUSTOMERS_MENU) this.activeMenu.set('empresa');
    }, { allowSignalWrites: true });
  }

  private readonly url = toSignal(
    this.router.events.pipe(filter(e => e instanceof NavigationEnd), map(() => this.router.url)),
    { initialValue: this.router.url });
  private readonly inCustomers = computed(() => this.url().split('?')[0].startsWith(CUSTOMERS_ROUTE));

  readonly compliance = computed(() => this.platform.config().compliance);

  activeMenu = signal('empresa');

  readonly menus: SettingsMenu[] = [
    { key: 'empresa',     label: 'Empresa',       icon: 'ti-building' },
    { key: 'usuarios',    label: 'Usuários',       icon: 'ti-users', adminOnly: true },
    { key: 'clientes',    label: 'Clientes',       icon: 'ti-address-book', route: '/settings/customers' },
    { key: 'permissoes',  label: 'Permissões',     icon: 'ti-shield-lock' },
    { key: 'certificado', label: 'Cert. Digital',  icon: 'ti-certificate' },
    { key: 'integracoes', label: 'Integrações',    icon: 'ti-plug' },
    { key: 'auditoria',   label: 'Auditoria',      icon: 'ti-history' },
    { key: 'backup',      label: 'Backup',         icon: 'ti-database-backup' },
  ];

  /** Usuários é do Administrador: os demais perfis nem veem o item no menu. */
  readonly visibleMenus = computed(() => this.menus.filter(m => !m.adminOnly || this.canEdit()));

  selectMenu(m: SettingsMenu) {
    if (m.route) {
      this.router.navigateByUrl(m.route);
      return;
    }
    this.activeMenu.set(m.key);
    if (this.inCustomers()) this.router.navigateByUrl('/settings');
  }

  activeMenuLabel() {
    return this.menus.find(m => m.key === this.activeMenu())?.label ?? '';
  }

  readonly companyStatus = signal<'loading' | 'ready' | 'error'>('loading');
  readonly saving = signal(false);
  /** A empresa como gravada no backend; "Cancelar" volta o formulário para ela. */
  private readonly company = signal<Company | null>(null);

  /** Só o Administrador edita os dados da empresa; os demais perfis veem o formulário somente leitura. */
  readonly canEdit = computed(() => this.auth.currentUser()?.profile === ADMINISTRATOR_PROFILE);
  readonly companyName = computed(() => this.company()?.name ?? '');

  cnpj        = signal('');
  razaoSocial = signal('');
  regime      = signal<TaxRegime>('SIMPLES_NACIONAL');
  cnae        = signal('');
  ie          = signal('');

  async ngOnInit() {
    await this.loadCompany();
  }

  async loadCompany() {
    this.companyStatus.set('loading');
    try {
      await this.auth.loadCurrentUser();
      const companyId = this.auth.currentUser()?.companyId;
      if (!companyId) throw new Error('current user has no company');
      const company = await this.companies.get(companyId);
      this.company.set(company);
      this.fillForm(company);
      this.companyStatus.set('ready');
    } catch {
      this.companyStatus.set('error');
    }
  }

  private fillForm(company: Company) {
    this.cnpj.set(company.cnpj);
    this.razaoSocial.set(company.name);
    this.regime.set(company.taxRegime);
    this.cnae.set(company.cnae ?? '');
    this.ie.set(company.ie ?? '');
    this.ambienteProd.set(company.sefazEnvironment === 'PRODUCTION');
  }

  cancel() {
    const company = this.company();
    if (company) this.fillForm(company);
  }

  ambienteProd    = signal(true);
  twoFa           = signal(true);
  darkModeAuto    = signal(true);
  blockNegativeStock = signal(false);
  backupAuto      = signal(true);

  async save() {
    const company = this.company();
    if (!company || !this.canEdit() || this.saving()) return;
    this.saving.set(true);
    try {
      const { id, cnpj, sefazEnvironment, ...current } = company;
      let saved = await this.companies.update(id, {
        ...current, name: this.razaoSocial(), taxRegime: this.regime(), cnae: this.cnae(), ie: this.ie(),
      });
      const wanted = this.ambienteProd() ? 'PRODUCTION' : 'HOMOLOGATION';
      if (wanted !== saved.sefazEnvironment) saved = await this.companies.switchSefazEnvironment(id, wanted);
      this.company.set(saved);
      this.fillForm(saved);
      this.toast.success('Configurações salvas com sucesso!');
    } catch (error) {
      // Falha: nenhum sinal do formulário é tocado, então as edições do usuário continuam na tela.
      const detail = failureDetail(error);
      this.toast.danger(detail ? `Não foi possível salvar: ${detail}` : 'Não foi possível salvar as configurações.');
    } finally {
      this.saving.set(false);
    }
  }
}
