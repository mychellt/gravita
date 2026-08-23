import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ToastService } from '../../core/services/toast.service';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';

interface SettingsMenu { key: string; label: string; icon: string; }

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [FormsModule, PageHeaderComponent],
  styleUrl: './settings.component.scss',
  templateUrl: './settings.component.html'
})
export class SettingsComponent {
  constructor(private toast: ToastService) {}

  activeMenu = signal('empresa');

  readonly menus: SettingsMenu[] = [
    { key: 'empresa',     label: 'Empresa',       icon: 'ti-building' },
    { key: 'usuarios',    label: 'Usuários',       icon: 'ti-users' },
    { key: 'permissoes',  label: 'Permissões',     icon: 'ti-shield-lock' },
    { key: 'certificado', label: 'Cert. Digital',  icon: 'ti-certificate' },
    { key: 'integracoes', label: 'Integrações',    icon: 'ti-plug' },
    { key: 'auditoria',   label: 'Auditoria',      icon: 'ti-history' },
    { key: 'backup',      label: 'Backup',         icon: 'ti-database-backup' },
  ];

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
