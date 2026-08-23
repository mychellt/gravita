import { Component, computed } from '@angular/core';
import { RouterLink, RouterModule } from '@angular/router';
import { DecimalPipe, DatePipe } from '@angular/common';
import { DataService } from '../../core/services/data.service';
import { BadgeComponent } from '../../shared/components/badge/badge.component';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { BrlPipe } from '../../shared/pipes/brl.pipe';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, RouterModule, DecimalPipe, DatePipe, BadgeComponent, PageHeaderComponent, BrlPipe],
  styleUrl: './dashboard.component.scss',
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent {
  constructor(public data: DataService) {}

  readonly faturamentoHoje = 18420;
  readonly nfCount = 47;
  readonly receber30d = computed(() => this.data.totalReceber());
  readonly estoqueCritico = computed(() => this.data.produtosCriticos().length);
  readonly recentNfes = computed(() => this.data.nfes().slice(0, 5));

  readonly chartBars = [
    { day: 'Seg', value: 12400, height: 48,  highlight: false },
    { day: 'Ter', value: 16100, height: 62,  highlight: false },
    { day: 'Qua', value: 14000, height: 54,  highlight: false },
    { day: 'Qui', value: 20700, height: 80,  highlight: false },
    { day: 'Sex', value: 18600, height: 72,  highlight: false },
    { day: 'Sáb', value: 23200, height: 90,  highlight: true  },
    { day: 'Hoje',value: 18420, height: 70,  highlight: false },
  ];

  readonly activity = [
    { icon: 'ti-file-check',   type: 'nfe',   title: 'NF-e #000234 autorizada — Atacadão SP',   time: 'há 4 min',  value: 'R$ 3.820,00' },
    { icon: 'ti-device-desktop',type: 'pdv',  title: 'NFC-e PDV Caixa 01 — 8 itens',            time: 'há 8 min',  value: 'R$ 247,90'   },
    { icon: 'ti-cash',         type: 'cp',    title: 'Pagamento recebido — João Silva',          time: 'há 15 min', value: 'R$ 1.450,00' },
    { icon: 'ti-alert-circle', type: 'alert', title: 'Estoque mínimo atingido — Arroz Camil 5kg',time: 'há 22 min', value: '12 un restantes'},
    { icon: 'ti-file-check',   type: 'nfe',   title: 'NF-e #000233 autorizada — Maria Ltda',    time: 'há 31 min', value: 'R$ 8.150,00' },
  ];
}
