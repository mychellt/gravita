import { Component, computed, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DataService } from '../../../core/services/data.service';
import { Cliente, Endereco } from '../../../core/models';
import { BadgeComponent } from '../../../shared/components/badge/badge.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { BrlPipe } from '../../../shared/pipes/brl.pipe';

const IE_LABELS: Record<Cliente['indicadorIe'], string> = {
  contribuinte: 'Contribuinte',
  isento: 'Isento',
  nao_contribuinte: 'Não contribuinte',
};

const CONTACT_LABELS: Record<string, string> = {
  telefone: 'Telefone',
  whatsapp: 'WhatsApp',
  email: 'E-mail',
  outro: 'Outro',
};

@Component({
  selector: 'app-customer-detail',
  standalone: true,
  imports: [RouterLink, BadgeComponent, PageHeaderComponent, BrlPipe],
  styleUrl: './customer-detail.component.scss',
  templateUrl: './customer-detail.component.html'
})
export class CustomerDetailComponent {
  private data = inject(DataService);

  /** Route param `:id` (withComponentInputBinding). */
  readonly id = input.required<string>();

  readonly customer = computed(() => this.data.clientes().find(c => c.id === this.id()));

  readonly ieLabel = computed(() => {
    const c = this.customer();
    return c ? IE_LABELS[c.indicadorIe] ?? '--' : '--';
  });

  readonly contacts = computed(() =>
    (this.customer()?.contatos ?? []).map(ct => ({ type: CONTACT_LABELS[ct.tipo] ?? ct.tipo, value: ct.valor || '--' }))
  );

  readonly priceTables = computed(() =>
    [...(this.customer()?.tabelasPreco ?? [])]
      .sort((a, b) => a.prioridade - b.prioridade)
      .map(t => ({ name: this.data.getTabelaPrecoNome(t.tabelaPrecoId) ?? '--', priority: t.prioridade }))
  );

  formatAddress(e: Endereco): string {
    const line1 = [e.logradouro, e.numero].filter(Boolean).join(', ');
    const line1WithComplement = e.complemento ? `${line1} - ${e.complemento}` : line1;
    const city = [e.municipio, e.uf].filter(Boolean).join('/');
    return [line1WithComplement, e.bairro, city, e.cep].filter(Boolean).join(' · ') || '--';
  }
}
