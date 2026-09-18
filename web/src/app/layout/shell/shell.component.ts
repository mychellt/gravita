import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { SidebarComponent } from '../sidebar/sidebar.component';
import { TopbarComponent } from '../topbar/topbar.component';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, SidebarComponent, TopbarComponent],
  styleUrl: './shell.component.scss',
  template: `
    <div class="shell">
      <app-sidebar />
      <div class="shell-main">
        <app-topbar />
        <main class="shell-content">
          <router-outlet />
        </main>
      </div>
    </div>
  `
})
export class ShellComponent {}
