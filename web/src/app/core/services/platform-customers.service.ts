import { Injectable, signal } from '@angular/core';
import { PlatformCustomer, PlatformPayment, PlatformPaymentStatus } from '../models';

/**
 * Últimas mensalidades de um cliente, da mais antiga para a mais recente. Mês de referência e
 * vencimento (dia 10) são relativos a hoje para o histórico acompanhar o calendário.
 */
function mensalidades(customerId: string, valor: number, statuses: PlatformPaymentStatus[], forma: string): PlatformPayment[] {
  const now = new Date();
  return statuses.map((status, i) => {
    const offset = i - (statuses.length - 1);
    const competencia = new Date(now.getFullYear(), now.getMonth() + offset, 1);
    const vencimento = new Date(now.getFullYear(), now.getMonth() + offset, 10);
    return {
      id: `${customerId}-m${i + 1}`, customerId, competencia, vencimento, valor, status,
      ...(status === 'pago' ? { pagoEm: new Date(vencimento.getTime() - 2 * 86400000), formaPagamento: forma } : {}),
    };
  });
}

/**
 * Clientes da Gravita (tenants com plano). Mock em memória, como o `DataService`;
 * substituir por chamadas à API.
 */
@Injectable({ providedIn: 'root' })
export class PlatformCustomersService {
  readonly customers = signal<PlatformCustomer[]>([
    { id:'t1', cnpj:'11.222.333/0001-44', razaoSocial:'Mercado Moderno Ltda',       nomeFantasia:'Mercado Moderno',    email:'financeiro@mercadomoderno.com.br', planTier:'SILVER', status:'ativo',        assinaturaDesde:new Date('2025-03-10'), responsavel:'Carla Mendes', telefone:'(11) 3000-1001' },
    { id:'t2', cnpj:'22.333.444/0001-55', razaoSocial:'Rede Compra Fácil S.A.',     nomeFantasia:'Compra Fácil',       email:'ti@compraf.com.br',                planTier:'GOLD',   status:'ativo',        assinaturaDesde:new Date('2024-11-02'), responsavel:'Rafael Souza', telefone:'(21) 3000-2002' },
    { id:'t3', cnpj:'33.444.555/0001-66', razaoSocial:'Padaria São José ME',        nomeFantasia:'Padaria São José',   email:'contato@padariasaojose.com.br',    planTier:'BRONZE', status:'ativo',        assinaturaDesde:new Date('2026-01-20'), responsavel:'José Lima', telefone:'(11) 3000-3003' },
    { id:'t4', cnpj:'44.555.666/0001-77', razaoSocial:'Hortifrúti Central Eireli',  nomeFantasia:'Hortifrúti Central', email:'admin@hortcentral.com.br',         planTier:'BRONZE', status:'inadimplente', assinaturaDesde:new Date('2025-08-15'), responsavel:'Marta Alves', telefone:'(31) 3000-4004' },
    { id:'t5', cnpj:'55.666.777/0001-88', razaoSocial:'Mini Mercado Flores Ltda',   nomeFantasia:'Mini Mercado Flores', email:'flores@minimercado.com.br',       planTier:'SILVER', status:'bloqueado',    assinaturaDesde:new Date('2025-05-04'), responsavel:'Paulo Ramos', telefone:'(41) 3000-5005' },
    { id:'t6', cnpj:'66.777.888/0001-99', razaoSocial:'Loja Central Eireli',        nomeFantasia:'Loja Central',       email:'loja@central.com.br',              planTier:'BRONZE', status:'inativo',      assinaturaDesde:new Date('2024-06-30'), responsavel:'Sandra Dias', telefone:'(51) 3000-6006' },
  ]);

  readonly payments = signal<PlatformPayment[]>([
    ...mensalidades('t1', 597,  ['pago','pago','pago','pago','pago','aberto'], 'Cartão de crédito'),
    ...mensalidades('t2', 1197, ['pago','pago','pago','pago','pago','aberto'], 'Boleto'),
    ...mensalidades('t3', 297,  ['pago','pago','pago','pago','pago','aberto'], 'PIX'),
    ...mensalidades('t4', 297,  ['pago','pago','pago','pago','aberto','aberto'], 'Boleto'),
    ...mensalidades('t5', 597,  ['pago','pago','pago','aberto','aberto','aberto'], 'Cartão de crédito'),
    ...mensalidades('t6', 297,  ['pago','pago','pago','pago','cancelado','cancelado'], 'Boleto'),
  ]);

  getById(id: string): PlatformCustomer | undefined {
    return this.customers().find(c => c.id === id);
  }
}
