import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-page-header',
  standalone: true,
  styleUrl: './page-header.component.scss',
  template: `
    <div class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">{{ title }}</h2>
        @if (subtitle) {
          <p class="page-sub">{{ subtitle }}</p>
        }
      </div>
      <div class="page-header-actions">
        <ng-content />
      </div>
    </div>
  `
})
export class PageHeaderComponent {
  @Input() title = '';
  @Input() subtitle = '';
}
