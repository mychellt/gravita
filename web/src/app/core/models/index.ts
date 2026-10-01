export type NFeStatus = 'autorizada' | 'em_fila' | 'cancelada' | 'rascunho' | 'inutilizada';
export type NFSeStatus = 'autorizada' | 'em_envio' | 'cancelada' | 'rascunho';
export type PedidoStatus = 'rascunho' | 'aprovado' | 'em_separacao' | 'faturado' | 'cancelado';
export type PedidoCompraStatus = 'aberto' | 'parcial' | 'encerrado' | 'cancelado' | 'aguarda_aprovacao' | 'em_transito';
export type TituloStatus = 'aberto' | 'pago' | 'vencido' | 'cancelado';
export type FunilEstagio = 'prospeccao' | 'proposta' | 'negociacao' | 'fechado' | 'perdido';
export type RegimeTributario = 'simples_nacional' | 'lucro_presumido' | 'lucro_real';
export type MovimentoTipo = 'entrada' | 'saida' | 'ajuste' | 'transferencia';

export interface Empresa {
  id: string;
  cnpj: string;
  razaoSocial: string;
  nomeFantasia: string;
  ie: string;
  im: string;
  cnae: string;
  regime: RegimeTributario;
  ambiente: 'producao' | 'homologacao';
  logotipo?: string;
}

export interface Endereco {
  cep: string;
  logradouro: string;
  numero: string;
  complemento?: string;
  bairro: string;
  municipio: string;
  uf: string;
  codigoIbge?: string;
}

export interface ContatoCliente {
  tipo: 'telefone' | 'whatsapp' | 'email' | 'outro';
  valor: string;
}

export interface TabelaPreco {
  id: string;
  nome: string;
}

export interface TabelaPrecoVinculo {
  tabelaPrecoId: string;
  prioridade: number;
}

export interface Cliente {
  id: string;
  tipo: 'pf' | 'pj';
  documento: string;
  nome: string;
  ie?: string;
  indicadorIe: 'contribuinte' | 'isento' | 'nao_contribuinte';
  consumidorFinal: boolean;
  enderecos: Endereco[];
  email?: string;
  telefone?: string;
  whatsapp?: string;
  limiteCredito: number;
  saldoDevedor: number;
  status: 'regular' | 'bloqueado' | 'inadimplente';
  tabelaPrecoId?: string;
  contatos?: ContatoCliente[];
  tabelasPreco?: TabelaPrecoVinculo[];
}

export interface Fornecedor {
  id: string;
  tipo: 'pf' | 'pj';
  documento: string;
  nome: string;
  endereco: Endereco;
  email?: string;
  telefone?: string;
  prazoEntregaDias: number;
  chavePix?: string;
  banco?: string;
  agencia?: string;
  conta?: string;
}

export interface Produto {
  id: string;
  codigo: string;
  codigoBarras?: string;
  descricao: string;
  tipo: 'simples' | 'grade' | 'kit' | 'servico';
  ncm: string;
  cest?: string;
  cfopPadrao: string;
  unidade: string;
  precoVenda: number;
  custoMedio: number;
  margem: number;
  estoque: number;
  estoqueMinimo: number;
  estoqueMaximo: number;
  grupo?: string;
  marca?: string;
  status: 'ativo' | 'inativo';
  imagem?: string;
}

export interface ItemNfe {
  seq: number;
  produtoId: string;
  descricao: string;
  ncm: string;
  cfop: string;
  unidade: string;
  quantidade: number;
  valorUnitario: number;
  desconto: number;
  valorTotal: number;
  icmsBase: number;
  icmsAliq: number;
  icmsValor: number;
  pisValor: number;
  cofinsValor: number;
}

export interface Nfe {
  id: string;
  numero: number;
  serie: string;
  natureza: string;
  cfop: string;
  dataEmissao: Date;
  clienteId: string;
  clienteNome: string;
  clienteDoc: string;
  itens: ItemNfe[];
  valorProdutos: number;
  valorFrete: number;
  valorDesconto: number;
  valorTotal: number;
  baseIcms: number;
  valorIcms: number;
  valorPis: number;
  valorCofins: number;
  status: NFeStatus;
  chaveAcesso?: string;
  protocolo?: string;
  xmlPath?: string;
}

export interface Nfse {
  id: string;
  numero: string;
  rpsNumero: string;
  tomadorNome: string;
  tomadorDoc: string;
  municipio: string;
  uf: string;
  padrao: 'abrasf' | 'nacional' | 'issnet' | 'betha';
  codigoServico: string;
  discriminacao: string;
  valorServico: number;
  aliquotaIss: number;
  valorIss: number;
  retencoes: number;
  dataEmissao: Date;
  status: NFSeStatus;
}

export interface MovimentoEstoque {
  id: string;
  dataHora: Date;
  produtoId: string;
  produtoNome: string;
  tipo: MovimentoTipo;
  quantidade: number;
  saldoApos: number;
  origem: string;
  usuario: string;
  lote?: string;
  serie?: string;
}

export interface ItemPedidoCompra {
  produtoId: string;
  descricao: string;
  quantidade: number;
  valorUnitario: number;
  valorTotal: number;
}

export interface PedidoCompra {
  id: string;
  numero: string;
  fornecedorId: string;
  fornecedorNome: string;
  dataPedido: Date;
  dataPrevisao?: Date;
  itens: ItemPedidoCompra[];
  valorTotal: number;
  status: PedidoCompraStatus;
  observacao?: string;
}

export interface Oportunidade {
  id: string;
  titulo: string;
  clienteNome: string;
  valor: number;
  probabilidade: number;
  estagio: FunilEstagio;
  responsavel: string;
  dataFechamento?: Date;
}

export interface PedidoVenda {
  id: string;
  numero: string;
  clienteId: string;
  clienteNome: string;
  vendedor: string;
  dataPedido: Date;
  itens: ItemNfe[];
  valorTotal: number;
  status: PedidoStatus;
}

export interface Titulo {
  id: string;
  tipo: 'receber' | 'pagar';
  descricao: string;
  parceiro: string;
  parceiroDoc?: string;
  vencimento: Date;
  valor: number;
  valorPago?: number;
  formaPagamento?: string;
  status: TituloStatus;
  origem?: string;
  centoCusto?: string;
}

export type RecebivelStatus = 'aberto' | 'parcial' | 'liquidado' | 'renegociado' | 'cancelado';
export type RecebivelOrigem = 'faturamento' | 'manual' | 'renegociacao';

/** Customer receivable (backend `Receivable`: origin, amount, dueDate, installmentNumber/installments, status). */
export interface Recebivel {
  id: string;
  clienteId: string;
  origem: RecebivelOrigem;
  valor: number;
  vencimento: Date;
  /** Position of this title among the installments (1-based); absent for manual titles. */
  parcela?: number;
  totalParcelas?: number;
  status: RecebivelStatus;
}

export interface ItemCarrinho {
  produto: Produto;
  quantidade: number;
  desconto: number;
  subtotal: number;
}

export interface VendaPdv {
  itens: ItemCarrinho[];
  total: number;
  formaPagamento?: string;
  valorRecebido?: number;
  troco?: number;
  cpfNota?: string;
}

// ── Administração da plataforma (configuração comum a todos os clientes) ──

export type PlanTier = 'BRONZE' | 'SILVER' | 'GOLD';

export interface PlanFeature {
  label: string;
  included: boolean;
}

/** `null` = ilimitado */
export interface PlanLimits {
  cnpjs: number | null;
  filiais: number | null;
  caixasPdv: number | null;
  usuarios: number | null;
}

export interface PlanSupport {
  email: boolean;
  chat: boolean;
  telefone: boolean;
  slaHoras: number;
  horarioComercial: boolean;
  gerenteDedicado: boolean;
}

/** Espelha `PlanDomain` do backend (`/api/plans`), com os campos extras exibidos no site. */
export interface Plan {
  id: string;
  tier: PlanTier;
  name: string;
  description: string;
  priceMonthly: number;
  /** Valor mensal equivalente na cobrança anual */
  priceAnnual: number;
  featured: boolean;
  active: boolean;
  limits: PlanLimits;
  features: PlanFeature[];
  support: PlanSupport;
}

export interface BillingSettings {
  trialDias: number;
  trialExigeCartao: boolean;
  taxaImplantacao: number;
  fidelidadeMeses: number;
  mesesGratisAnual: number;
}

export type IntegracaoCategoria = 'banco' | 'ecommerce' | 'mensageria';

export interface IntegracaoCatalogo {
  id: string;
  nome: string;
  categoria: IntegracaoCategoria;
  icon: string;
  ativa: boolean;
  planoMinimo: PlanTier;
}

export interface ComplianceSettings {
  retencaoXmlAnos: number;
  retencaoBackupDias: number;
  horarioBackup: string;
  exigir2faAdmin: boolean;
  senhaMinCaracteres: number;
  uptimeSla: number;
}

export interface InstitucionalSettings {
  razaoSocial: string;
  cnpj: string;
  cidadeUf: string;
  emailContato: string;
  urlCentralAjuda: string;
  urlDocsApi: string;
  urlStatus: string;
  urlPrivacidade: string;
  urlTermos: string;
  urlLgpd: string;
}

export interface SiteStat {
  valor: string;
  legenda: string;
}

export interface FaqItem {
  pergunta: string;
  resposta: string;
}

/** Cliente da Gravita (tenant) com plano contratado — não confundir com `Cliente`, que é cliente do tenant. */
export interface PlatformCustomer {
  id: string;
  cnpj: string;
  razaoSocial: string;
  nomeFantasia: string;
  email: string;
  planTier: PlanTier;
  status: 'ativo' | 'inadimplente' | 'bloqueado' | 'inativo';
  assinaturaDesde: Date;
  responsavel?: string;
  telefone?: string;
}

export type PlatformPaymentStatus = 'aberto' | 'pago' | 'cancelado';

/** Mensalidade da assinatura de um cliente da Gravita. */
export interface PlatformPayment {
  id: string;
  customerId: string;
  /** Mês de referência (dia 1). */
  competencia: Date;
  vencimento: Date;
  valor: number;
  status: PlatformPaymentStatus;
  pagoEm?: Date;
  formaPagamento?: string;
}

export interface PlatformConfig {
  plans: Plan[];
  billing: BillingSettings;
  integracoes: IntegracaoCatalogo[];
  compliance: ComplianceSettings;
  institucional: InstitucionalSettings;
  siteStats: SiteStat[];
  faq: FaqItem[];
}
