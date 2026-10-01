import { Component, computed, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DataService } from '../../../core/services/data.service';
import { BadgeComponent } from '../../../shared/components/badge/badge.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { BrlPipe } from '../../../shared/pipes/brl.pipe';

@Component({
  selector: 'app-customers-list',
  standalone: true,
  imports: [RouterLink, BadgeComponent, PageHeaderComponent, BrlPipe],
  styleUrl: './customers-list.component.scss',
  templateUrl: './customers-list.component.html'
})
export class CustomersListComponent {
  constructor(private data: DataService) {}

  searchQuery = signal('');

  readonly customers = computed(() => {
    const q = this.searchQuery().trim().toLowerCase();
    const digits = q.replace(/\D/g, '');
    return this.data.clientes().filter(c =>
      !q ||
      c.nome.toLowerCase().includes(q) ||
      c.documento.toLowerCase().includes(q) ||
      (digits !== '' && c.documento.replace(/\D/g, '').includes(digits))
    );
  });
}
