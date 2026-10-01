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

const CONTATO_LABELS: Record<string, string> = {
  telefone: 'Telefone',
  whatsapp: 'WhatsApp',
  email: 'E-mail',
  outro: 'Outro',
};

@Component({
  selector: 'app-cliente-detail',
  standalone: true,
  imports: [RouterLink, BadgeComponent, PageHeaderComponent, BrlPipe],
  styleUrl: './cliente-detail.component.scss',
  templateUrl: './cliente-detail.component.html'
})
export class ClienteDetailComponent {
  private data = inject(DataService);

  /** Route param `:id` (withComponentInputBinding). */
  readonly id = input.required<string>();

  readonly cliente = computed(() => this.data.clientes().find(c => c.id === this.id()));

  readonly ieLabel = computed(() => {
    const c = this.cliente();
    return c ? IE_LABELS[c.indicadorIe] ?? '--' : '--';
  });

  readonly contatos = computed(() =>
    (this.cliente()?.contatos ?? []).map(ct => ({ tipo: CONTATO_LABELS[ct.tipo] ?? ct.tipo, valor: ct.valor || '--' }))
  );

  readonly tabelasPreco = computed(() =>
    [...(this.cliente()?.tabelasPreco ?? [])]
      .sort((a, b) => a.prioridade - b.prioridade)
      .map(t => ({ nome: this.data.getTabelaPrecoNome(t.tabelaPrecoId) ?? '--', prioridade: t.prioridade }))
  );

  formatEndereco(e: Endereco): string {
    const linha1 = [e.logradouro, e.numero].filter(Boolean).join(', ');
    const linha1c = e.complemento ? `${linha1} - ${e.complemento}` : linha1;
    const cidade = [e.municipio, e.uf].filter(Boolean).join('/');
    return [linha1c, e.bairro, cidade, e.cep].filter(Boolean).join(' · ') || '--';
  }
}
