import { Component, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ToastService } from '../../core/services/toast.service';
import { PlatformConfigService } from '../../core/services/platform-config.service';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';

interface SettingsMenu { key: string; label: string; icon: string; route?: string; }

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [FormsModule, PageHeaderComponent],
  styleUrl: './settings.component.scss',
  templateUrl: './settings.component.html'
})
export class SettingsComponent {
  constructor(private toast: ToastService, private platform: PlatformConfigService, private router: Router) {}

  readonly compliance = computed(() => this.platform.config().compliance);

  activeMenu = signal('empresa');

  readonly menus: SettingsMenu[] = [
    { key: 'empresa',     label: 'Empresa',       icon: 'ti-building' },
    { key: 'usuarios',    label: 'Usuários',       icon: 'ti-users' },
    { key: 'clientes',    label: 'Clientes',       icon: 'ti-address-book', route: '/settings/clientes' },
    { key: 'permissoes',  label: 'Permissões',     icon: 'ti-shield-lock' },
    { key: 'certificado', label: 'Cert. Digital',  icon: 'ti-certificate' },
    { key: 'integracoes', label: 'Integrações',    icon: 'ti-plug' },
    { key: 'auditoria',   label: 'Auditoria',      icon: 'ti-history' },
    { key: 'backup',      label: 'Backup',         icon: 'ti-database-backup' },
  ];

  selectMenu(m: SettingsMenu) {
    if (m.route) this.router.navigateByUrl(m.route);
    else this.activeMenu.set(m.key);
  }

  activeMenuLabel() {
    return this.menus.find(m => m.key === this.activeMenu())?.label ?? '';
  }

  cnpj        = signal('12.345.678/0001-90');
  razaoSocial = signal('Mercado Moderno Ltda');
  regime      = signal('simples_nacional');
  cnae        = signal('4711-3/02');
  ie          = signal('123.456.789.112');

  ambienteProd    = signal(true);
  twoFa           = signal(true);
  darkModeAuto    = signal(true);
  estoqueNegativo = signal(false);
  backupAuto      = signal(true);

  save() { this.toast.success('Configurações salvas com sucesso!'); }
}
