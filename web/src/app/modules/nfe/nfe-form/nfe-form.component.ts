import { Component, signal, computed } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DataService } from '../../../core/services/data.service';
import { ToastService } from '../../../core/services/toast.service';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { BrlPipe } from '../../../shared/pipes/brl.pipe';

interface FormItem {
  produtoId: string;
  descricao: string;
  ncm: string;
  cfop: string;
  quantidade: number;
  valorUnitario: number;
  desconto: number;
  total: number;
}

@Component({
  selector: 'app-nfe-form',
  standalone: true,
  imports: [FormsModule, PageHeaderComponent, BrlPipe],
  styleUrl: './nfe-form.component.scss',
  templateUrl: './nfe-form.component.html'
})
export class NfeFormComponent {
  constructor(private router: Router, private data: DataService, private toast: ToastService) {
    this.addDefaultItems();
  }

  currentStep = signal(2);

  cnpj          = '98.765.432/0001-10';
  razaoSocial   = 'Atacadão SP Ltda';
  ie            = '987.654.321.000';
  indicadorIe   = 'contribuinte';
  consumidorFinal = 'nao';
  cep           = '04578-000';
  logradouro    = 'Av. dos Bandeirantes, 2000';
  municipio     = 'São Paulo';
  uf            = 'SP';
  email         = 'nfe@atacadao.com.br';
  cfopSel       = '5102';
  formaPagamento = 'boleto30';
  infoAdicionais = '';

  items = signal<FormItem[]>([]);

  readonly baseCalculo = computed(() => this.items().reduce((s, i) => s + i.total, 0));
  readonly icms        = computed(() => this.baseCalculo() * 0.18);
  readonly pis         = computed(() => this.baseCalculo() * 0.0065);
  readonly cofins      = computed(() => this.baseCalculo() * 0.03);
  readonly totalNota   = computed(() => this.baseCalculo());

  addDefaultItems() {
    const prods = this.data.produtos();
    if (prods.length >= 2) {
      this.items.set([
        { produtoId: prods[0].id, descricao: prods[0].descricao, ncm: prods[0].ncm, cfop: '5102', quantidade: 100, valorUnitario: prods[0].precoVenda, desconto: 0,   total: 100 * prods[0].precoVenda },
        { produtoId: prods[1].id, descricao: prods[1].descricao, ncm: prods[1].ncm, cfop: '5102', quantidade: 200, valorUnitario: prods[1].precoVenda, desconto: 5,   total: 200 * prods[1].precoVenda * 0.95 },
      ]);
    }
  }

  removeItem(i: number) {
    this.items.update(list => list.filter((_, idx) => idx !== i));
  }

  addItem() {
    const p = this.data.produtos()[0];
    this.items.update(list => [...list, {
      produtoId: p.id, descricao: p.descricao, ncm: p.ncm,
      cfop: '5102', quantidade: 1, valorUnitario: p.precoVenda,
      desconto: 0, total: p.precoVenda
    }]);
  }

  emit() {
    this.toast.success('NF-e #000235 transmitida e autorizada com sucesso!');
    this.router.navigate(['/nfe']);
  }

  cancel() { this.router.navigate(['/nfe']); }

  readonly steps = [
    { n: 1, label: 'Natureza' },
    { n: 2, label: 'Destinatário' },
    { n: 3, label: 'Itens' },
    { n: 4, label: 'Transporte' },
    { n: 5, label: 'Revisão' },
  ];
}
