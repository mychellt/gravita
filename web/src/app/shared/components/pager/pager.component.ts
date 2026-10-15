import { Component, EventEmitter, Input, Output } from '@angular/core';

/** Previous/next controls for a server-paginated list; `page` is zero-based, as the API's. */
@Component({
  selector: 'app-pager',
  standalone: true,
  template: `
    @if (totalPages > 1) {
      <div class="pager">
        <button type="button" class="btn btn-ghost btn-sm" [disabled]="page <= 0" (click)="pageChange.emit(page - 1)">
          <i class="ti ti-chevron-left"></i> Anterior
        </button>
        <span class="pager-info">Página {{ page + 1 }} de {{ totalPages }} · {{ totalElements }} registros</span>
        <button type="button" class="btn btn-ghost btn-sm" [disabled]="page >= totalPages - 1" (click)="pageChange.emit(page + 1)">
          Próxima <i class="ti ti-chevron-right"></i>
        </button>
      </div>
    }
  `,
  styles: [`
    .pager { display: flex; align-items: center; justify-content: flex-end; gap: 12px; padding: 10px 14px; border-top: 1px solid var(--border); }
    .pager-info { font-size: 11px; color: var(--text3); }
  `]
})
export class PagerComponent {
  @Input({ required: true }) page!: number;
  @Input({ required: true }) totalPages!: number;
  @Input() totalElements = 0;
  @Output() pageChange = new EventEmitter<number>();
}
