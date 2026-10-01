import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
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
  constructor(public titleSvc: TitleService) {}

  readonly nav: AdminNavItem[] = [
    { label: 'Configurações gerais', icon: 'ti-adjustments-horizontal', route: '/admin/configuracoes' },
    { label: 'Clientes Gravita', icon: 'ti-building-store', route: '/admin/clientes' },
  ];
}
