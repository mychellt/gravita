import { AddressType, ContactType, CustomerAddress, CustomerStatus, IeIndicator } from '../../../core/services/customer.service';

export const IE_LABELS: Record<IeIndicator, string> = {
  TAXPAYER: 'Contribuinte',
  EXEMPT: 'Isento',
  NON_TAXPAYER: 'Não contribuinte',
};

export const CONTACT_LABELS: Record<ContactType, string> = {
  PHONE: 'Telefone',
  WHATSAPP: 'WhatsApp',
  EMAIL: 'E-mail',
};

export const ADDRESS_TYPE_LABELS: Record<AddressType, string> = {
  BILLING: 'Cobrança',
  DELIVERY: 'Entrega',
};

/** Chave de `app-badge` para o status de crédito do cliente. */
export const STATUS_BADGES: Record<CustomerStatus, string> = {
  REGULAR: 'regular',
  BLOCKED: 'bloqueado',
  DELINQUENT: 'inadimplente',
};

export function formatAddress(a: CustomerAddress): string {
  const line1 = [a.street, a.number].filter(Boolean).join(', ');
  const line1WithComplement = a.complement ? `${line1} - ${a.complement}` : line1;
  const city = [a.city, a.state].filter(Boolean).join('/');
  return [line1WithComplement, a.neighborhood, city, a.zipCode].filter(Boolean).join(' · ') || '--';
}
