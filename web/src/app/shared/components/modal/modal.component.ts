import { Component, EventEmitter, HostListener, Input, Output } from '@angular/core';

/** Centered dialog. Project `<div footer>…</div>` for the action buttons. */
@Component({
  selector: 'app-modal',
  standalone: true,
  styles: [`
    .overlay { position: fixed; inset: 0; background: rgba(0,0,0,.45); display: flex; align-items: center; justify-content: center; z-index: 1000; padding: 16px; }
    .dialog { background: var(--surface); border-radius: var(--r16); max-width: 100%; max-height: calc(100vh - 32px); display: flex; flex-direction: column; overflow: hidden; box-shadow: 0 20px 60px rgba(0,0,0,.25); }
    .head { padding: 14px 20px; border-bottom: 1px solid var(--border); display: flex; align-items: center; justify-content: space-between; }
    .title { font-size: 15px; font-weight: 600; }
    .body { padding: 16px 20px; overflow-y: auto; }
    .foot { padding: 12px 20px; border-top: 1px solid var(--border); display: flex; justify-content: flex-end; gap: 8px; }
  `],
  template: `
    <div class="overlay" (click)="closed.emit()">
      <div class="dialog" role="dialog" aria-modal="true" [attr.aria-label]="title"
           [style.width.px]="width" (click)="$event.stopPropagation()">
        <div class="head">
          <span class="title">{{ title }}</span>
          <button class="btn-icon" type="button" aria-label="Fechar" (click)="closed.emit()">
            <i class="ti ti-x"></i>
          </button>
        </div>
        <div class="body"><ng-content /></div>
        <div class="foot"><ng-content select="[footer]" /></div>
      </div>
    </div>
  `
})
export class ModalComponent {
  @Input() title = '';
  @Input() width = 480;
  @Output() closed = new EventEmitter<void>();

  @HostListener('document:keydown.escape')
  closeOnEscape(): void {
    this.closed.emit();
  }
}
