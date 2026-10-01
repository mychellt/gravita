import { Injectable, signal } from '@angular/core';
import { PlatformCustomer } from '../models';

/**
 * Clientes da Gravita (tenants com plano). Mock em memória, como o `DataService`;
 * substituir por chamadas à API.
 */
@Injectable({ providedIn: 'root' })
export class PlatformCustomersService {
  readonly customers = signal<PlatformCustomer[]>([
    { id:'t1', cnpj:'11.222.333/0001-44', razaoSocial:'Mercado Moderno Ltda',       nomeFantasia:'Mercado Moderno',    email:'financeiro@mercadomoderno.com.br', planTier:'SILVER', status:'ativo',        assinaturaDesde:new Date('2025-03-10') },
    { id:'t2', cnpj:'22.333.444/0001-55', razaoSocial:'Rede Compra Fácil S.A.',     nomeFantasia:'Compra Fácil',       email:'ti@compraf.com.br',                planTier:'GOLD',   status:'ativo',        assinaturaDesde:new Date('2024-11-02') },
    { id:'t3', cnpj:'33.444.555/0001-66', razaoSocial:'Padaria São José ME',        nomeFantasia:'Padaria São José',   email:'contato@padariasaojose.com.br',    planTier:'BRONZE', status:'ativo',        assinaturaDesde:new Date('2026-01-20') },
    { id:'t4', cnpj:'44.555.666/0001-77', razaoSocial:'Hortifrúti Central Eireli',  nomeFantasia:'Hortifrúti Central', email:'admin@hortcentral.com.br',         planTier:'BRONZE', status:'inadimplente', assinaturaDesde:new Date('2025-08-15') },
    { id:'t5', cnpj:'55.666.777/0001-88', razaoSocial:'Mini Mercado Flores Ltda',   nomeFantasia:'Mini Mercado Flores', email:'flores@minimercado.com.br',       planTier:'SILVER', status:'bloqueado',    assinaturaDesde:new Date('2025-05-04') },
    { id:'t6', cnpj:'66.777.888/0001-99', razaoSocial:'Loja Central Eireli',        nomeFantasia:'Loja Central',       email:'loja@central.com.br',              planTier:'BRONZE', status:'inativo',      assinaturaDesde:new Date('2024-06-30') },
  ]);
}
