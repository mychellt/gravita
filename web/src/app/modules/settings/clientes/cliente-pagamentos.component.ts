import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';

/**
 * Placeholder for the customer "Pagamentos" page. The follow-up task replaces
 * this component's template/logic; the route and `id` input are already wired.
 */
@Component({
  selector: 'app-cliente-pagamentos',
  standalone: true,
  imports: [RouterLink, PageHeaderComponent],
  template: `
    <app-page-header title="Pagamentos" subtitle="Pagamentos do cliente">
      <a routerLink="/settings/clientes" class="btn btn-ghost"><i class="ti ti-arrow-left"></i> Voltar</a>
    </app-page-header>
    <div class="card" style="padding:40px;text-align:center;color:var(--text3)">
      <i class="ti ti-settings" style="font-size:32px;display:block;margin-bottom:8px"></i>
      Em desenvolvimento
    </div>
  `
})
export class ClientePagamentosComponent {
  readonly id = input.required<string>();
}
