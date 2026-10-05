import { Component, OnInit, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AUTH_PATHS } from '../../modules/auth/auth-paths';
import { AuthService } from '../../core/services/auth.service';
import { DataService } from '../../core/services/data.service';
import { initialsOf, profileLabel } from '../../core/user-display';

interface NavItem {
  label: string;
  icon: string;
  route: string;
  exact?: boolean;
  badge?: number | string;
  badgeWarn?: boolean;
  live?: boolean;
}

interface NavSection {
  title: string;
  items: NavItem[];
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  styleUrl: './sidebar.component.scss',
  templateUrl: './sidebar.component.html'
})
export class SidebarComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  constructor(private data: DataService) {}

  /** O usuário logado, no rodapé da barra lateral (nada é mostrado até a sessão ser confirmada). */
  readonly user = computed(() => {
    const current = this.auth.currentUser();
    return current ? { name: current.name, initials: initialsOf(current.name), role: profileLabel(current.profile) } : null;
  });

  async ngOnInit() {
    await this.auth.loadCurrentUser();
    // Uma sessão que o servidor não reconhece mais volta para o login.
    if (!this.auth.isAuthenticated()) await this.router.navigateByUrl(AUTH_PATHS.login);
  }

  readonly criticalCount = computed(() => this.data.produtosCriticos().length);

  get sections(): NavSection[] {
    return [
      {
        title: 'Principal',
        items: [
          { label: 'Dashboard',    icon: 'ti-layout-dashboard', route: '/dashboard', exact: true }
        ]
      },
      {
        title: 'Fiscal',
        items: [
          { label: 'NF-e (M55)',   icon: 'ti-file-invoice',   route: '/nfe',   badge: 3 },
          { label: 'PDV / NFC-e',  icon: 'ti-device-desktop', route: '/pdv',   live: true },
          { label: 'NFS-e',        icon: 'ti-receipt',        route: '/nfse' },
        ]
      },
      {
        title: 'Operação',
        items: [
          { label: 'Estoque',      icon: 'ti-box',            route: '/inventory', badge: this.criticalCount(), badgeWarn: true },
          { label: 'Compras',      icon: 'ti-shopping-cart',  route: '/purchasing' },
          { label: 'Vendas & CRM', icon: 'ti-users',          route: '/crm' },
        ]
      },
      {
        title: 'Financeiro',
        items: [
          { label: 'Financeiro',   icon: 'ti-cash',           route: '/finance' },
        ]
      },
      {
        title: 'Visibilidade',
        items: [
          { label: 'Relatórios & BI', icon: 'ti-chart-bar',   route: '/reports' },
        ]
      },
      {
        title: 'Sistema',
        items: [
          { label: 'Configurações', icon: 'ti-settings',      route: '/settings' },
        ]
      },
    ];
  }
}
