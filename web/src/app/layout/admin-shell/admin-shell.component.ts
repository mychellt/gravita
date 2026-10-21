import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { TitleService } from '../../core/services/title.service';

interface AdminNavItem { label: string; icon: string; route: string; }

@Component({
  selector: 'app-admin-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  styleUrls: [
    '../shell/shell.component.scss',
    '../sidebar/sidebar.component.scss',
    '../topbar/topbar.component.scss',
    './admin-shell.component.scss',
  ],
  templateUrl: './admin-shell.component.html'
})
export class AdminShellComponent {
  private readonly auth = inject(AuthService);
  readonly user = this.auth.currentUser;

  constructor(public titleSvc: TitleService) {}

  /** Iniciais do nome para o avatar. */
  readonly initials = () => (this.user()?.name ?? '').split(/\s+/).filter(Boolean).slice(0, 2).map(w => w[0]).join('').toUpperCase();

  async logout() {
    await this.auth.logout();
    // A página pública (marketing) é estática, fora do Angular: precisa de navegação completa.
    window.location.assign('/');
  }

  readonly nav: AdminNavItem[] = [
    { label: 'Configurações gerais', icon: 'ti-adjustments-horizontal', route: '/admin/settings' },
    { label: 'Clientes Gravita', icon: 'ti-building-store', route: '/admin/customers' },
  ];
}
