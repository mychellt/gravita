import { Customer, CustomerAddress, CustomerStatus, ContactKind, IeIndicator } from './services/customer.service';

/** Valores que o `<app-badge>` conhece (os mesmos do protótipo). */
export type CustomerBadge = 'regular' | 'bloqueado' | 'inadimplente';

const STATUS_BADGES: Record<CustomerStatus, CustomerBadge> = {
  REGULAR: 'regular',
  BLOCKED: 'bloqueado',
  DELINQUENT: 'inadimplente',
};

export const IE_INDICATOR_LABELS: Record<IeIndicator, string> = {
  TAXPAYER: 'Contribuinte',
  EXEMPT: 'Isento',
  NON_TAXPAYER: 'Não contribuinte',
};

export const CONTACT_LABELS: Record<ContactKind, string> = {
  PHONE: 'Telefone',
  WHATSAPP: 'WhatsApp',
  EMAIL: 'E-mail',
};

export const ADDRESS_LABELS = { BILLING: 'Cobrança', DELIVERY: 'Entrega' } as const;

export function statusBadge(status: CustomerStatus | null | undefined): CustomerBadge | null {
  return status ? STATUS_BADGES[status] ?? null : null;
}

export const documentLabel = (type: Customer['type']) => (type === 'INDIVIDUAL' ? 'CPF' : 'CNPJ');

/** "Rua A, 10 - sala 2 · Centro · São Paulo/SP · 01310-100" */
export function formatAddress(address: CustomerAddress): string {
  const line = [address.street, address.number].filter(Boolean).join(', ');
  const withComplement = address.complement ? `${line} - ${address.complement}` : line;
  const city = [address.city, address.state].filter(Boolean).join('/');
  return [withComplement, address.neighborhood, city, address.zipCode].filter(Boolean).join(' · ') || '--';
}
