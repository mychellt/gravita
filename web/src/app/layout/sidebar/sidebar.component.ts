import { Component, computed } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { DataService } from '../../core/services/data.service';

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
export class SidebarComponent {
  constructor(private data: DataService) {}

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
