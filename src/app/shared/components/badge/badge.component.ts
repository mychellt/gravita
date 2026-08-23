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
  aberto:            'pend',
  aguarda_aprovacao: 'pend',
  inadimplente:      'pend',
  cancelada:         'canc',
  vencido:           'canc',
  bloqueado:         'canc',
  inativo:           'inativo',
  rascunho:          'rascunho',
  em_envio:          'proc',
  em_transito:       'proc',
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
