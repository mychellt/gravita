import {
  Contagem, CustoHistorico, Deposito, LoteEstoque, Produto, ReservaEstoque, SerieEstoque,
  TransferenciaEstoque
} from '../models';

export const DEPOSITO_MATRIZ = 'd1';
export const DEPOSITO_FILIAL = 'd2';

/** Local-midnight date `n` days from today, so lot expiry in the mock data ages with the calendar. */
export function daysFromToday(n: number): Date {
  const d = new Date();
  d.setHours(0, 0, 0, 0);
  d.setDate(d.getDate() + n);
  return d;
}

export const SEED_DEPOSITOS: Deposito[] = [
  { id: DEPOSITO_MATRIZ, nome: 'Matriz — Depósito Central' },
  { id: DEPOSITO_FILIAL, nome: 'Filial 02' },
];

/** On-hand per product/warehouse; sums per product match `Produto.estoque`. */
export const SEED_SALDOS = [
  { produtoId: 'p1',  depositoId: DEPOSITO_MATRIZ, fisico: 48,  reservado: 20, emTransito: 0 },
  { produtoId: 'p2',  depositoId: DEPOSITO_MATRIZ, fisico: 520, reservado: 0,  emTransito: 0 },
  { produtoId: 'p2',  depositoId: DEPOSITO_FILIAL, fisico: 100, reservado: 0,  emTransito: 0 },
  { produtoId: 'p3',  depositoId: DEPOSITO_MATRIZ, fisico: 72,  reservado: 8,  emTransito: 0 },
  { produtoId: 'p4',  depositoId: DEPOSITO_MATRIZ, fisico: 95,  reservado: 0,  emTransito: 0 },
  { produtoId: 'p5',  depositoId: DEPOSITO_MATRIZ, fisico: 60,  reservado: 0,  emTransito: 0 },
  { produtoId: 'p5',  depositoId: DEPOSITO_FILIAL, fisico: 0,   reservado: 0,  emTransito: 50 },
  { produtoId: 'p6',  depositoId: DEPOSITO_MATRIZ, fisico: 87,  reservado: 0,  emTransito: 0 },
  { produtoId: 'p7',  depositoId: DEPOSITO_MATRIZ, fisico: 8,   reservado: 5,  emTransito: 0 },
  { produtoId: 'p8',  depositoId: DEPOSITO_MATRIZ, fisico: 28,  reservado: 0,  emTransito: 0 },
  { produtoId: 'p9',  depositoId: DEPOSITO_MATRIZ, fisico: 55,  reservado: 0,  emTransito: 0 },
  { produtoId: 'p10', depositoId: DEPOSITO_MATRIZ, fisico: 45,  reservado: 0,  emTransito: 0 },
  { produtoId: 'p11', depositoId: DEPOSITO_MATRIZ, fisico: 120, reservado: 0,  emTransito: 0 },
  { produtoId: 'p12', depositoId: DEPOSITO_MATRIZ, fisico: 6,   reservado: 0,  emTransito: 0 },
];

export const SEED_LOTES: LoteEstoque[] = [
  { produtoId: 'p10', depositoId: DEPOSITO_MATRIZ, codigo: 'L-2409X', validade: daysFromToday(-3), quantidade: 5 },
  { produtoId: 'p10', depositoId: DEPOSITO_MATRIZ, codigo: 'L-2410A', validade: daysFromToday(4),  quantidade: 15 },
  { produtoId: 'p10', depositoId: DEPOSITO_MATRIZ, codigo: 'L-2411B', validade: daysFromToday(40), quantidade: 25 },
  { produtoId: 'p11', depositoId: DEPOSITO_MATRIZ, codigo: 'L-IG01',  validade: daysFromToday(6),  quantidade: 30 },
  { produtoId: 'p11', depositoId: DEPOSITO_MATRIZ, codigo: 'L-IG02',  validade: daysFromToday(25), quantidade: 90 },
];

export const SEED_SERIES: SerieEstoque[] = [
  ...[1, 2, 3, 4, 5, 6].map(n => serie(n, 'em_estoque')),
  ...[7, 8].map(n => serie(n, 'emitida')),
];

function serie(n: number, status: SerieEstoque['status']): SerieEstoque {
  return { numero: `SM-A15-${String(n).padStart(4, '0')}`, produtoId: 'p12', depositoId: DEPOSITO_MATRIZ, status };
}

export const SEED_TRANSFERENCIAS: TransferenciaEstoque[] = [
  {
    id: 'tr1', produtoId: 'p5', produtoNome: 'Óleo de Soja 900ml', origemId: DEPOSITO_MATRIZ,
    destinoId: DEPOSITO_FILIAL, quantidade: 50, status: 'pendente', criadaEm: daysFromToday(-1),
  },
];

export const SEED_RESERVAS: ReservaEstoque[] = [
  { id: 'rs1', pedidoRef: 'PV-0412', produtoId: 'p1', produtoNome: 'Arroz Camil 5kg', depositoId: DEPOSITO_MATRIZ, quantidade: 20, status: 'ativa' },
  { id: 'rs2', pedidoRef: 'PV-0415', produtoId: 'p3', produtoNome: 'Leite Integral 1L', depositoId: DEPOSITO_MATRIZ, quantidade: 8,  status: 'ativa' },
  { id: 'rs3', pedidoRef: 'PV-0418', produtoId: 'p7', produtoNome: 'Café Pilão 500g', depositoId: DEPOSITO_MATRIZ, quantidade: 5, status: 'ativa' },
  { id: 'rs4', pedidoRef: 'PV-0409', produtoId: 'p1', produtoNome: 'Arroz Camil 5kg', depositoId: DEPOSITO_MATRIZ, quantidade: 30, status: 'consumida' },
  { id: 'rs5', pedidoRef: 'PV-0407', produtoId: 'p4', produtoNome: 'Açúcar Refinado 1kg', depositoId: DEPOSITO_MATRIZ, quantidade: 10, status: 'liberada' },
];

export const SEED_CONTAGENS: Contagem[] = [
  {
    id: 'ct1', escopo: 'parcial', grupo: 'Laticínios', depositoId: DEPOSITO_MATRIZ, status: 'em_andamento',
    iniciadaEm: daysFromToday(-1), iniciadaPor: 'Gerente',
    linhas: [
      { produtoId: 'p3',  produtoNome: 'Leite Integral 1L',    qtdSistema: 72,  qtdContada: 70 },
      { produtoId: 'p10', produtoNome: 'Queijo Mussarela kg',  qtdSistema: 45 },
      { produtoId: 'p11', produtoNome: 'Iogurte Natural 170g', qtdSistema: 120 },
    ],
  },
];

const COST_TREND: { diasAtras: number; fator: number }[] = [
  { diasAtras: 90, fator: 0.94 }, { diasAtras: 60, fator: 0.97 },
  { diasAtras: 30, fator: 0.99 }, { diasAtras: 0, fator: 1 },
];

/** Average cost (CMV) trail ending at the product's current cost, so the history chart has something to draw. */
export function seedCostHistory(produtos: Produto[]): CustoHistorico[] {
  return produtos.flatMap(p => COST_TREND.map(({ diasAtras, fator }) => ({
    produtoId: p.id,
    data: daysFromToday(-diasAtras),
    custoMedio: Math.round(p.custoMedio * fator * 100) / 100,
  })));
}
