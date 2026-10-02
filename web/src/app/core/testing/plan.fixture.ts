import { Plan } from '../models';

/** A valid plan as `GET /api/plans` returns it; override only what a test cares about. */
export function aPlan(overrides: Partial<Plan> = {}): Plan {
  return {
    id: '8f3a83e9-5fe9-4743-b588-bb60c28d8b23',
    tier: 'BRONZE',
    name: 'Bronze',
    description: 'Para pequenos varejos.',
    priceMonthly: 297,
    priceAnnual: 247,
    featured: false,
    active: true,
    limits: { cnpjs: 1, filiais: 1, caixasPdv: 1, usuarios: 3 },
    features: [
      { label: 'NF-e e NFC-e ilimitadas', included: true },
      { label: 'NFS-e (serviços)', included: false },
    ],
    support: { email: true, chat: true, telefone: false, slaHoras: 8, horarioComercial: true, gerenteDedicado: false },
    ...overrides,
  };
}
