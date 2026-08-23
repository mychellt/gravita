import { Component } from '@angular/core';
import { ToastService } from '../../../core/services/toast.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-toast-container',
  standalone: true,
  imports: [CommonModule],
  styleUrl: './toast-container.component.scss',
  template: `
    <div class="toast-container">
      @for (toast of toastSvc.toasts(); track toast.id) {
        <div class="toast {{ toast.type }}" (click)="toastSvc.remove(toast.id)">
          <i class="ti"
            [class.ti-check]="toast.type === 'success'"
            [class.ti-alert-triangle]="toast.type === 'warn'"
            [class.ti-x]="toast.type === 'danger'"
            [class.ti-info-circle]="toast.type === 'info'"></i>
          {{ toast.message }}
        </div>
      }
    </div>
  `
})
export class ToastContainerComponent {
  constructor(public toastSvc: ToastService) {}
}
