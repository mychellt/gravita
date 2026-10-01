import { Component, signal } from '@angular/core';
import { ToastService } from '../../core/services/toast.service';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { BrlPipe } from '../../shared/pipes/brl.pipe';

interface ReportCard {
  icon: string; iconBg: string; iconColor: string; title: string; desc: string;
}

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [PageHeaderComponent, BrlPipe],
  styleUrl: './reports.component.scss',
  templateUrl: './reports.component.html'
})
export class ReportsComponent {
  constructor(private toast: ToastService) {}

  readonly reportCards: ReportCard[] = [
    { icon: 'ti-chart-line',              iconBg: '#EFF6FF', iconColor: '#2563EB', title: 'DRE Gerencial',    desc: 'Receita bruta, CMV, despesas e resultado líquido' },
    { icon: 'ti-sort-descending-letters', iconBg: '#FFFBEB', iconColor: '#D97706', title: 'Curva ABC',        desc: 'Produtos e clientes por representatividade' },
    { icon: 'ti-report-money',            iconBg: '#ECFDF5', iconColor: '#059669', title: 'Comissões',        desc: 'Por vendedor, produto e período' },
    { icon: 'ti-file-text',               iconBg: '#FEF2F2', iconColor: '#DC2626', title: 'Livros Fiscais',   desc: 'Entradas, Saídas e Apuração ICMS' },
    { icon: 'ti-package',                 iconBg: '#EDE9FE', iconColor: '#7C3AED', title: 'Giro de Estoque',  desc: 'Rotatividade e itens parados' },
    { icon: 'ti-calculator',              iconBg: '#F0F9FF', iconColor: '#0284C7', title: 'Tributos Apurados',desc: 'ICMS, IPI, PIS, COFINS, ISS por período' },
  ];

  readonly abcData = [
    { label: 'Arroz',  pct: 28, cls: 'a' },
    { label: 'Feijão', pct: 22, cls: 'a' },
    { label: 'Leite',  pct: 16, cls: 'a' },
    { label: 'Café',   pct: 11, cls: 'b' },
    { label: 'Óleo',   pct: 8,  cls: 'b' },
    { label: 'Macar.', pct: 5,  cls: 'c' },
    { label: 'Açúcar', pct: 4,  cls: 'c' },
    { label: 'Sabão',  pct: 3,  cls: 'c' },
    { label: 'Det.',   pct: 2,  cls: 'c' },
    { label: 'Outros', pct: 1,  cls: 'c' },
  ];

  readonly maxPct = 28;

  generate(title: string) { this.toast.success(`Relatório "${title}" gerado com sucesso!`); }
}
