import { Component, computed, signal } from '@angular/core';
import { DataService } from '../../core/services/data.service';
import { ToastService } from '../../core/services/toast.service';
import { BadgeComponent } from '../../shared/components/badge/badge.component';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { BrlPipe } from '../../shared/pipes/brl.pipe';
import { FunilEstagio, Oportunidade } from '../../core/models';

@Component({
  selector: 'app-crm',
  standalone: true,
  imports: [BadgeComponent, PageHeaderComponent, BrlPipe],
  styleUrl: './crm.component.scss',
  templateUrl: './crm.component.html'
})
export class CrmComponent {
  constructor(public data: DataService, private toast: ToastService) {}

  activeTab = signal<'funil' | 'pedidos' | 'clientes' | 'metas'>('funil');

  readonly estagios: { key: FunilEstagio; label: string; color?: string }[] = [
    { key: 'prospeccao', label: 'Prospecção' },
    { key: 'proposta',   label: 'Proposta' },
    { key: 'negociacao', label: 'Negociação' },
    { key: 'fechado',    label: 'Fechado ✓', color: 'var(--success)' },
    { key: 'perdido',    label: 'Perdido',   color: 'var(--danger)' },
  ];

  opsByEstagio(estagio: FunilEstagio): Oportunidade[] {
    return this.data.oportunidades().filter(o => o.estagio === estagio);
  }

  readonly clientes = computed(() => this.data.clientes());
}
