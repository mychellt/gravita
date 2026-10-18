import { Component, Input } from '@angular/core';

export type BadgeType = 'auth' | 'pend' | 'canc' | 'rascunho' | 'proc' | 'inativo';

const LABELS: Record<string, string> = {
  autorizada:        'Autorizada',
  em_fila:           'Em fila',
  cancelada:         'Cancelada',
  rascunho:          'Rascunho',
  inutilizada:       'Inutilizada',
  aberto:            'Aberto',
  pago:              'Pago',
  vencido:           'Vencido',
  em_envio:          'Em envio',
  aguarda_aprovacao: 'Aguarda aprovação',
  em_transito:       'Em trânsito',
  encerrado:         'Encerrado',
  aprovado:          'Aprovado',
  faturado:          'Faturado',
  regular:           'Regular',
  bloqueado:         'Bloqueado',
  inadimplente:      'Inadimplente',
  ativo:             'Ativo',
  inativo:           'Inativo',
  aguardando_ativacao: 'Aguardando ativação',
  parcial:           'Parcialmente baixado',
  liquidado:         'Liquidado',
  renegociado:       'Renegociado',
  cancelado:         'Cancelado',
  // NF-e emitida (NfeDocumentStatus) e de entrada (InboundNfeStatus), como vêm do backend
  RPS:                'RPS',
  DRAFT:              'Rascunho',
  QUEUED:             'Em fila',
  SENT:               'Enviada',
  AUTHORIZED:         'Autorizada',
  REJECTED:           'Rejeitada',
  CANCELLED:          'Cancelada',
  VOIDED:             'Inutilizada',
  PENDING_CONFERENCE: 'Aguardando conferência',
  CONFIRMED:          'Confirmada',
};

const CLASSES: Record<string, BadgeType> = {
  autorizada:        'auth',
  pago:              'auth',
  encerrado:         'auth',
  faturado:          'auth',
  aprovado:          'auth',
  regular:           'auth',
  ativo:             'auth',
  em_fila:           'pend',
  aguardando_ativacao: 'pend',
  aberto:            'pend',
  aguarda_aprovacao: 'pend',
  inadimplente:      'pend',
  cancelada:         'canc',
  vencido:           'canc',
  bloqueado:         'canc',
  inativo:           'inativo',
  parcial:           'proc',
  liquidado:         'auth',
  renegociado:       'rascunho',
  cancelado:         'inativo',
  rascunho:          'rascunho',
  em_envio:          'proc',
  em_transito:       'proc',
  RPS:                'rascunho',
  DRAFT:              'rascunho',
  QUEUED:             'pend',
  SENT:               'proc',
  AUTHORIZED:         'auth',
  REJECTED:           'canc',
  CANCELLED:          'canc',
  VOIDED:             'inativo',
  PENDING_CONFERENCE: 'pend',
  CONFIRMED:          'auth',
};

@Component({
  selector: 'app-badge',
  standalone: true,
  template: `
    <span class="badge {{ badgeClass }}">
      <span class="badge-dot"></span>
      {{ badgeLabel }}
    </span>
  `
})
export class BadgeComponent {
  @Input() set value(v: string) {
    this.badgeClass  = CLASSES[v]  ?? 'rascunho';
    this.badgeLabel  = LABELS[v]   ?? v;
  }
  badgeClass: BadgeType | string = 'rascunho';
  badgeLabel = '';
}
