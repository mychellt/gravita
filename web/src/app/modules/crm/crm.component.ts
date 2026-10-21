import { Component, computed, inject, signal } from '@angular/core';
import { CustomerService, CustomerStatus } from '../../core/services/customer.service';
import { Opportunity, OpportunitiesService, OpportunityStage } from '../../core/services/opportunities.service';
import { BadgeComponent } from '../../shared/components/badge/badge.component';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { BrlPipe } from '../../shared/pipes/brl.pipe';

type CrmTab = 'funil' | 'pedidos' | 'clientes' | 'metas';

const CUSTOMER_BADGE: Record<CustomerStatus, string> = {
  REGULAR: 'ativo', BLOCKED: 'bloqueado', DELINQUENT: 'inadimplente',
};
const AVATAR_COLORS = ['#4F6EF7', '#7C3AED', '#D97706', '#059669', '#DB2777'];

@Component({
  selector: 'app-crm',
  standalone: true,
  imports: [BadgeComponent, PageHeaderComponent, BrlPipe],
  styleUrl: './crm.component.scss',
  templateUrl: './crm.component.html'
})
export class CrmComponent {
  private readonly opportunityService = inject(OpportunitiesService);
  private readonly customerService = inject(CustomerService);

  constructor() {
    void this.opportunityService.load();
    void this.customerService.load();
  }

  activeTab = signal<CrmTab>('funil');

  readonly estagios: { key: OpportunityStage; label: string; color?: string }[] = [
    { key: 'PROSPECTING', label: 'Prospecção' },
    { key: 'PROPOSAL',    label: 'Proposta' },
    { key: 'NEGOTIATION', label: 'Negociação' },
    { key: 'CLOSED',      label: 'Fechado ✓', color: 'var(--success)' },
    { key: 'LOST',        label: 'Perdido',   color: 'var(--danger)' },
  ];

  readonly funnelStatus = this.opportunityService.status;
  readonly clientsStatus = this.customerService.status;

  readonly byStage = computed(() => {
    const groups = new Map<OpportunityStage, Opportunity[]>(this.estagios.map(e => [e.key, []]));
    this.opportunityService.opportunities().forEach(o => groups.get(o.stage)?.push(o));
    return groups;
  });

  readonly clientes = computed(() => this.customerService.customers().map(c => ({
    id: c.id,
    nome: c.name,
    documento: c.document,
    limiteCredito: c.creditLimit,
    saldoDevedor: c.currentBalance ?? 0,
    status: CUSTOMER_BADGE[c.status ?? 'REGULAR'],
    inadimplente: c.status === 'DELINQUENT',
    cor: AVATAR_COLORS[this.colorIndex(c.id)],
  })));

  opsByEstagio(estagio: OpportunityStage): Opportunity[] {
    return this.byStage().get(estagio) ?? [];
  }

  reload() {
    void this.opportunityService.load();
    void this.customerService.load();
  }

  private colorIndex(id: string): number {
    return [...id].reduce((sum, ch) => sum + ch.charCodeAt(0), 0) % AVATAR_COLORS.length;
  }
}
