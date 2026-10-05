import { documentLabel, formatAddress, statusBadge } from './customer-display';

describe('customer display helpers', () => {
  it('maps the backend status to the badge values', () => {
    expect(statusBadge('REGULAR')).toBe('regular');
    expect(statusBadge('BLOCKED')).toBe('bloqueado');
    expect(statusBadge('DELINQUENT')).toBe('inadimplente');
    expect(statusBadge(null)).toBeNull();
  });

  it('labels the document by person type', () => {
    expect(documentLabel('INDIVIDUAL')).toBe('CPF');
    expect(documentLabel('COMPANY')).toBe('CNPJ');
  });

  it('formats an address on one line, skipping what is missing', () => {
    expect(formatAddress({ type: 'BILLING', street: 'Rua A', number: '10', complement: 'sala 2', neighborhood: 'Centro',
      city: 'São Paulo', state: 'SP', zipCode: '01310-100', isDefault: true }))
      .toBe('Rua A, 10 - sala 2 · Centro · São Paulo/SP · 01310-100');
    expect(formatAddress({ type: 'DELIVERY', street: 'Rua B', number: null, complement: null, neighborhood: 'Vila',
      city: 'Campinas', state: 'SP', zipCode: '13000-000', isDefault: false }))
      .toBe('Rua B · Vila · Campinas/SP · 13000-000');
  });
});
