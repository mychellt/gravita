import { Injectable, computed, inject, signal } from '@angular/core';
import {
  Contagem, ContagemEscopo, CustoHistorico, LoteEstoque, MovimentoEstoque, MovimentoTipo, Produto,
  ReservaEstoque, SaldoEstoque, SerieEstoque, SugestaoReposicao, TransferenciaEstoque
} from '../models';
import { AuthService } from './auth.service';
import { DataService } from './data.service';
import {
  SEED_CONTAGENS, SEED_DEPOSITOS, SEED_LOTES, SEED_RESERVAS, SEED_SALDOS, SEED_SERIES, SEED_TRANSFERENCIAS,
  daysFromToday, seedCostHistory
} from './inventory.seed';

export type Resultado = { ok: true } | { ok: false; erro: string };

const OK: Resultado = { ok: true };
const falha = (erro: string): Resultado => ({ ok: false, erro });

export const JANELA_VALIDADE_PADRAO_DIAS = 7;

export interface AjusteEstoque {
  produtoId: string;
  depositoId: string;
  /** Positive adds stock, negative removes it. */
  variacao: number;
  justificativa: string;
}

export interface NovaTransferencia {
  produtoId: string;
  origemId: string;
  destinoId: string;
  quantidade: number;
  lote?: string;
}

export interface NovaContagem {
  escopo: ContagemEscopo;
  grupo?: string;
  depositoId: string;
}

/**
 * In-memory stand-in for the M5 inventory API (`/api/inventory/**`). It enforces the same rules as the
 * backend use cases, so swapping it for HTTP calls later only changes this class.
 * Product totals (`Produto.estoque`) stay in {@link DataService} because the dashboard and sidebar read them.
 */
@Injectable({ providedIn: 'root' })
export class InventoryService {
  private readonly data = inject(DataService);
  private readonly auth = inject(AuthService);
  private sequence = 0;

  readonly depositos = signal(SEED_DEPOSITOS).asReadonly();
  readonly saldos = signal<SaldoEstoque[]>(this.seedSaldos());
  readonly lotes = signal<LoteEstoque[]>(SEED_LOTES);
  readonly series = signal<SerieEstoque[]>(SEED_SERIES).asReadonly();
  readonly transferencias = signal<TransferenciaEstoque[]>(SEED_TRANSFERENCIAS);
  readonly reservas = signal<ReservaEstoque[]>(SEED_RESERVAS);
  readonly contagens = signal<Contagem[]>(SEED_CONTAGENS);
  readonly historicoCusto = signal<CustoHistorico[]>(seedCostHistory(this.data.produtos())).asReadonly();
  readonly janelaValidadeDias = signal(JANELA_VALIDADE_PADRAO_DIAS);
  private readonly requisicoesCriadas = signal<ReadonlySet<string>>(new Set());

  readonly lotesAVencer = computed(() => {
    const limite = daysFromToday(this.janelaValidadeDias()).getTime();
    return this.lotes().filter(l => l.quantidade > 0 && l.validade.getTime() <= limite);
  });

  readonly transferenciasPendentes = computed(() => this.transferencias().filter(t => t.status === 'pendente'));

  readonly sugestoesReposicao = computed<SugestaoReposicao[]>(() =>
    this.data.produtos().flatMap(produto => this.sugestaoPara(produto)));

  readonly valorEmEstoque = computed(() =>
    this.saldos().reduce((total, s) => total + s.fisico * s.custoMedio, 0));

  nomeDeposito(depositoId: string): string {
    return this.depositos().find(d => d.id === depositoId)?.nome ?? depositoId;
  }

  disponivel(saldo: SaldoEstoque): number {
    return saldo.fisico - saldo.reservado;
  }

  saldosDoProduto(produtoId: string): SaldoEstoque[] {
    return this.saldos().filter(s => s.produtoId === produtoId);
  }

  movimentosDoProduto(produtoId: string): MovimentoEstoque[] {
    return this.data.movimentos().filter(m => m.produtoId === produtoId);
  }

  statusValidade(lote: LoteEstoque): 'vencido' | 'a_vencer' | 'ok' {
    const hoje = daysFromToday(0).getTime();
    if (lote.validade.getTime() < hoje) return 'vencido';
    return this.lotesAVencer().includes(lote) ? 'a_vencer' : 'ok';
  }

  diasParaVencer(lote: LoteEstoque): number {
    return Math.round((lote.validade.getTime() - daysFromToday(0).getTime()) / 86_400_000);
  }

  jaRequisitado(sugestao: SugestaoReposicao): boolean {
    return this.requisicoesCriadas().has(this.chaveSugestao(sugestao));
  }

  ajustar(ajuste: AjusteEstoque): Resultado {
    const { produtoId, depositoId, variacao, justificativa } = ajuste;
    if (!justificativa.trim()) return falha('A justificativa é obrigatória para ajustes de estoque.');
    if (!variacao) return falha('Informe uma quantidade diferente de zero.');
    const bloqueio = this.verificarSaida(produtoId, depositoId, -variacao);
    if (bloqueio) return falha(bloqueio);

    this.mover(produtoId, depositoId, variacao);
    this.registrarMovimento({
      produtoId, depositoId, tipo: 'ajuste', quantidade: variacao, origem: 'Ajuste manual',
      justificativa: justificativa.trim(),
    });
    return OK;
  }

  iniciarTransferencia(nova: NovaTransferencia): Resultado {
    const { produtoId, origemId, destinoId, quantidade, lote } = nova;
    const produto = this.produto(produtoId);
    if (!produto) return falha('Produto não encontrado.');
    if (origemId === destinoId) return falha('Origem e destino devem ser depósitos diferentes.');
    if (quantidade <= 0) return falha('A quantidade deve ser maior que zero.');
    const erroLote = this.validarLoteDeSaida(produto, origemId, quantidade, lote);
    if (erroLote) return falha(erroLote);
    const bloqueio = this.verificarSaida(produtoId, origemId, quantidade);
    if (bloqueio) return falha(bloqueio);

    this.mover(produtoId, origemId, -quantidade);
    this.somarEmTransito(produtoId, destinoId, quantidade);
    if (lote) this.ajustarLote(produtoId, origemId, lote, -quantidade);
    this.registrarMovimento({
      produtoId, depositoId: origemId, tipo: 'transferencia', quantidade: -quantidade,
      origem: `Transferência → ${this.nomeDeposito(destinoId)}`, lote,
    });
    this.transferencias.update(lista => [
      { id: this.proximoId('tr'), produtoId, produtoNome: produto.descricao, origemId, destinoId, quantidade,
        lote, status: 'pendente', criadaEm: new Date() },
      ...lista,
    ]);
    return OK;
  }

  confirmarTransferencia(id: string): Resultado {
    const transferencia = this.transferencias().find(t => t.id === id);
    if (!transferencia) return falha('Transferência não encontrada.');
    if (transferencia.status !== 'pendente') return falha('Esta transferência já foi confirmada.');
    const { produtoId, destinoId, origemId, quantidade, lote } = transferencia;

    this.somarEmTransito(produtoId, destinoId, -quantidade);
    this.mover(produtoId, destinoId, quantidade);
    if (lote) this.receberLote(produtoId, origemId, destinoId, lote, quantidade);
    this.registrarMovimento({
      produtoId, depositoId: destinoId, tipo: 'transferencia', quantidade,
      origem: `Transferência ← ${this.nomeDeposito(origemId)}`, lote,
    });
    this.transferencias.update(lista => lista.map(t =>
      t.id === id ? { ...t, status: 'confirmada', confirmadaEm: new Date() } : t));
    return OK;
  }

  liberarReserva(id: string): Resultado {
    const reserva = this.reservas().find(r => r.id === id);
    if (!reserva) return falha('Reserva não encontrada.');
    if (reserva.status !== 'ativa') return falha('Só reservas ativas podem ser liberadas.');

    this.saldos.update(lista => lista.map(s =>
      this.mesmoSaldo(s, reserva.produtoId, reserva.depositoId)
        ? { ...s, reservado: s.reservado - reserva.quantidade } : s));
    this.reservas.update(lista => lista.map(r => r.id === id ? { ...r, status: 'liberada' } : r));
    return OK;
  }

  iniciarContagem(nova: NovaContagem): Resultado {
    if (nova.escopo === 'parcial' && !nova.grupo) return falha('Escolha o grupo de produtos da contagem parcial.');
    const linhas = this.data.produtos()
      .filter(p => p.status === 'ativo' && p.tipo !== 'servico')
      .filter(p => nova.escopo === 'total' || p.grupo === nova.grupo)
      .map(p => ({
        produtoId: p.id,
        produtoNome: p.descricao,
        qtdSistema: this.saldos().find(s => this.mesmoSaldo(s, p.id, nova.depositoId))?.fisico ?? 0,
      }));
    if (!linhas.length) return falha('Nenhum produto encontrado para esta contagem.');

    this.contagens.update(lista => [{
      id: this.proximoId('ct'), escopo: nova.escopo, grupo: nova.grupo, depositoId: nova.depositoId,
      status: 'em_andamento', iniciadaEm: new Date(), iniciadaPor: this.usuario(), linhas,
    }, ...lista]);
    return OK;
  }

  /** Cumulative: lines not mentioned keep their previous count; completes only once every line is counted. */
  enviarContagem(id: string, contadas: Record<string, number>): Resultado {
    const contagem = this.contagens().find(c => c.id === id);
    if (!contagem) return falha('Contagem não encontrada.');
    if (contagem.status !== 'em_andamento') return falha('Só contagens em andamento aceitam novas quantidades.');
    if (Object.values(contadas).some(qtd => qtd < 0)) return falha('A quantidade contada não pode ser negativa.');

    const linhas = contagem.linhas.map(l =>
      l.produtoId in contadas ? { ...l, qtdContada: contadas[l.produtoId] } : l);
    const completa = linhas.every(l => l.qtdContada !== undefined);
    this.contagens.update(lista => lista.map(c => c.id === id
      ? { ...c, linhas, status: completa ? 'aguardando_aprovacao' : 'em_andamento' } : c));
    return OK;
  }

  /** One adjustment per divergent line, each referencing the count in its justification. */
  aprovarContagem(id: string): Resultado {
    const contagem = this.contagens().find(c => c.id === id);
    if (!contagem) return falha('Contagem não encontrada.');
    if (contagem.status !== 'aguardando_aprovacao') return falha('Só contagens aguardando aprovação podem ser aprovadas.');

    contagem.linhas.filter(l => this.divergencia(l) !== 0).forEach(linha => {
      this.mover(linha.produtoId, contagem.depositoId, this.divergencia(linha));
      this.registrarMovimento({
        produtoId: linha.produtoId, depositoId: contagem.depositoId, tipo: 'ajuste',
        quantidade: this.divergencia(linha), origem: 'Inventário',
        justificativa: `Divergência da contagem ${contagem.id}`,
      });
    });
    this.contagens.update(lista => lista.map(c => c.id === id ? { ...c, status: 'aprovada' } : c));
    return OK;
  }

  divergencia(linha: { qtdSistema: number; qtdContada?: number }): number {
    return linha.qtdContada === undefined ? 0 : linha.qtdContada - linha.qtdSistema;
  }

  /** Stands in for M6's `CreatePurchaseRequestUseCase` with origin `MIN_STOCK_TRIGGER`. */
  solicitarCompra(sugestao: SugestaoReposicao): Resultado {
    if (this.jaRequisitado(sugestao)) return falha('Já existe uma requisição de compra para este item.');
    this.requisicoesCriadas.update(chaves => new Set(chaves).add(this.chaveSugestao(sugestao)));
    return OK;
  }

  definirPermiteEstoqueNegativo(produtoId: string, permite: boolean): void {
    this.data.produtos.update(lista => lista.map(p =>
      p.id === produtoId ? { ...p, permiteEstoqueNegativo: permite } : p));
  }

  private produto(produtoId: string): Produto | undefined {
    return this.data.produtos().find(p => p.id === produtoId);
  }

  private seedSaldos(): SaldoEstoque[] {
    return SEED_SALDOS.map(s => ({
      ...s, custoMedio: this.data.produtos().find(p => p.id === s.produtoId)?.custoMedio ?? 0,
    }));
  }

  /** Reorder parameters are per product, so the comparison uses the balance available across all warehouses. */
  private sugestaoPara(produto: Produto): SugestaoReposicao[] {
    if (produto.status !== 'ativo' || produto.tipo === 'servico') return [];
    const pontoReposicao = produto.pontoReposicao ?? produto.estoqueMinimo;
    const disponivel = this.saldosDoProduto(produto.id).reduce((total, s) => total + this.disponivel(s), 0);
    if (disponivel > pontoReposicao) return [];
    return [{
      produtoId: produto.id, produtoNome: produto.descricao, disponivel, pontoReposicao,
      quantidadeSugerida: Math.max(produto.estoqueMaximo - disponivel, 0),
    }];
  }

  private chaveSugestao(sugestao: SugestaoReposicao): string {
    return sugestao.produtoId;
  }

  private mesmoSaldo(saldo: SaldoEstoque, produtoId: string, depositoId: string): boolean {
    return saldo.produtoId === produtoId && saldo.depositoId === depositoId;
  }

  /** Returns why a stock reduction must be refused, or `null` when it can go ahead. */
  private verificarSaida(produtoId: string, depositoId: string, quantidade: number): string | null {
    if (quantidade <= 0) return null;
    const produto = this.produto(produtoId);
    if (!produto) return 'Produto não encontrado.';
    const saldo = this.saldos().find(s => this.mesmoSaldo(s, produtoId, depositoId));
    const disponivel = saldo ? this.disponivel(saldo) : 0;
    if (quantidade > disponivel && !produto.permiteEstoqueNegativo) {
      return `Saldo disponível insuficiente (${disponivel}). Este produto não permite estoque negativo.`;
    }
    return null;
  }

  private validarLoteDeSaida(produto: Produto, depositoId: string, quantidade: number, codigo?: string): string | null {
    if (!produto.controlaLote) return null;
    if (!codigo) return 'Selecione o lote — este produto tem controle por lote.';
    const lote = this.lotes().find(l => l.produtoId === produto.id && l.depositoId === depositoId && l.codigo === codigo);
    if (!lote) return 'Lote não encontrado neste depósito.';
    if (this.statusValidade(lote) === 'vencido') return `O lote ${codigo} está vencido e não pode ser movimentado.`;
    if (quantidade > lote.quantidade) return `O lote ${codigo} tem apenas ${lote.quantidade} no depósito.`;
    return null;
  }

  /** Moves on-hand of one product in one warehouse and keeps the product total in step. */
  private mover(produtoId: string, depositoId: string, variacao: number): void {
    const existe = this.saldos().some(s => this.mesmoSaldo(s, produtoId, depositoId));
    if (!existe) this.criarSaldo(produtoId, depositoId);
    this.saldos.update(lista => lista.map(s =>
      this.mesmoSaldo(s, produtoId, depositoId) ? { ...s, fisico: s.fisico + variacao } : s));
    this.data.produtos.update(lista => lista.map(p =>
      p.id === produtoId ? { ...p, estoque: p.estoque + variacao } : p));
  }

  private somarEmTransito(produtoId: string, depositoId: string, variacao: number): void {
    if (!this.saldos().some(s => this.mesmoSaldo(s, produtoId, depositoId))) this.criarSaldo(produtoId, depositoId);
    this.saldos.update(lista => lista.map(s =>
      this.mesmoSaldo(s, produtoId, depositoId) ? { ...s, emTransito: s.emTransito + variacao } : s));
  }

  private criarSaldo(produtoId: string, depositoId: string): void {
    const custoMedio = this.produto(produtoId)?.custoMedio ?? 0;
    this.saldos.update(lista => [...lista, { produtoId, depositoId, fisico: 0, reservado: 0, emTransito: 0, custoMedio }]);
  }

  private ajustarLote(produtoId: string, depositoId: string, codigo: string, variacao: number): void {
    this.lotes.update(lista => lista.map(l =>
      l.produtoId === produtoId && l.depositoId === depositoId && l.codigo === codigo
        ? { ...l, quantidade: l.quantidade + variacao } : l));
  }

  /** Lot arrives at the destination keeping its code and expiry; a new lot row is created when needed. */
  private receberLote(produtoId: string, origemId: string, destinoId: string, codigo: string, quantidade: number): void {
    const jaExiste = this.lotes().some(l => l.produtoId === produtoId && l.depositoId === destinoId && l.codigo === codigo);
    if (jaExiste) return this.ajustarLote(produtoId, destinoId, codigo, quantidade);
    const original = this.lotes().find(l => l.produtoId === produtoId && l.depositoId === origemId && l.codigo === codigo);
    if (!original) return;
    this.lotes.update(lista => [...lista, { ...original, depositoId: destinoId, quantidade }]);
  }

  private registrarMovimento(dados: {
    produtoId: string; depositoId: string; tipo: MovimentoTipo; quantidade: number; origem: string;
    lote?: string; justificativa?: string;
  }): void {
    const produto = this.produto(dados.produtoId);
    if (!produto) return;
    const movimento: MovimentoEstoque = {
      ...dados, id: this.proximoId('m'), dataHora: new Date(), produtoNome: produto.descricao,
      saldoApos: produto.estoque, usuario: this.usuario(), custoUnitario: produto.custoMedio,
    };
    this.data.movimentos.update(lista => [movimento, ...lista]);
  }

  private usuario(): string {
    return this.auth.currentUser()?.name ?? 'Usuário';
  }

  private proximoId(prefixo: string): string {
    this.sequence += 1;
    return `${prefixo}-novo-${this.sequence}`;
  }
}
