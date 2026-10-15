import { Component, EventEmitter, Input, Output } from '@angular/core';

/** Loading indicator or retry-capable error for a list fetch; render it instead of the table until the data is ready. */
@Component({
  selector: 'app-load-state',
  standalone: true,
  template: `
    @if (status === 'loading') {
      <div class="load-state" role="status"><i class="ti ti-loader-2 spin"></i> {{ loadingText }}</div>
    } @else if (status === 'error') {
      <div class="alert danger load-state" role="alert">
        <i class="ti ti-alert-triangle"></i>
        <span>{{ errorText }}</span>
        <button type="button" class="btn btn-ghost btn-sm" (click)="retry.emit()">
          <i class="ti ti-refresh"></i> Tentar novamente
        </button>
      </div>
    }
  `,
  styles: [`
    .load-state { display: flex; align-items: center; justify-content: center; gap: 8px; padding: 24px; margin: 0; font-size: 12px; color: var(--text3); }
    .alert.load-state { color: #7F1D1D; }
    .spin { animation: spin 1s linear infinite; color: var(--accent); }
    @keyframes spin { to { transform: rotate(360deg); } }
  `]
})
export class LoadStateComponent {
  @Input({ required: true }) status!: 'loading' | 'ready' | 'error';
  @Input() loadingText = 'Carregando…';
  @Input() errorText = 'Não foi possível carregar os dados.';
  @Output() retry = new EventEmitter<void>();
}
