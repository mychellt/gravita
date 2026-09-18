import { Injectable, signal, computed } from '@angular/core';
import {
  Nfe, Nfse, Produto, Cliente, Fornecedor,
  PedidoCompra, Titulo, Oportunidade, MovimentoEstoque,
  ItemCarrinho, PedidoVenda
} from '../models';

@Injectable({ providedIn: 'root' })
export class DataService {

  // ── Produtos ─────────────────────────────────────────────
  readonly produtos = signal<Produto[]>([
    { id:'p1', codigo:'001', codigoBarras:'7891234560001', descricao:'Arroz Camil 5kg', tipo:'simples', ncm:'1006.30', cest:'', cfopPadrao:'5102', unidade:'UN', precoVenda:22.90, custoMedio:14.50, margem:36.7, estoque:48, estoqueMinimo:50, estoqueMaximo:300, grupo:'Alimentos', marca:'Camil', status:'ativo' },
    { id:'p2', codigo:'002', codigoBarras:'7891234560002', descricao:'Feijão Carioca 1kg', tipo:'simples', ncm:'0713.33', cest:'', cfopPadrao:'5102', unidade:'UN', precoVenda:7.49, custoMedio:4.80, margem:35.9, estoque:620, estoqueMinimo:100, estoqueMaximo:800, grupo:'Alimentos', marca:'Camil', status:'ativo' },
    { id:'p3', codigo:'003', codigoBarras:'7891234560003', descricao:'Leite Integral 1L', tipo:'simples', ncm:'0401.10', cest:'', cfopPadrao:'5102', unidade:'UN', precoVenda:4.89, custoMedio:3.20, margem:34.6, estoque:72, estoqueMinimo:60, estoqueMaximo:400, grupo:'Laticínios', marca:'Parmalat', status:'ativo' },
    { id:'p4', codigo:'004', codigoBarras:'7891234560004', descricao:'Açúcar Refinado 1kg', tipo:'simples', ncm:'1701.99', cest:'', cfopPadrao:'5102', unidade:'UN', precoVenda:3.99, custoMedio:2.60, margem:34.8, estoque:95, estoqueMinimo:80, estoqueMaximo:500, grupo:'Alimentos', marca:'União', status:'ativo' },
    { id:'p5', codigo:'005', codigoBarras:'7891234560005', descricao:'Óleo de Soja 900ml', tipo:'simples', ncm:'1507.90', cest:'', cfopPadrao:'5102', unidade:'UN', precoVenda:6.79, custoMedio:4.50, margem:33.7, estoque:60, estoqueMinimo:40, estoqueMaximo:300, grupo:'Alimentos', marca:'Soya', status:'ativo' },
    { id:'p6', codigo:'006', codigoBarras:'7891234560006', descricao:'Macarrão Grano Duro 500g', tipo:'simples', ncm:'1902.19', cest:'', cfopPadrao:'5102', unidade:'UN', precoVenda:3.29, custoMedio:2.10, margem:36.2, estoque:87, estoqueMinimo:60, estoqueMaximo:400, grupo:'Alimentos', marca:'Barilla', status:'ativo' },
    { id:'p7', codigo:'007', codigoBarras:'7891234560007', descricao:'Café Pilão 500g', tipo:'simples', ncm:'0901.21', cest:'', cfopPadrao:'5102', unidade:'UN', precoVenda:13.90, custoMedio:8.80, margem:36.7, estoque:8, estoqueMinimo:20, estoqueMaximo:200, grupo:'Bebidas', marca:'Pilão', status:'ativo' },
    { id:'p8', codigo:'008', codigoBarras:'7891234560008', descricao:'Sabão em Pó 1kg', tipo:'simples', ncm:'3401.20', cest:'', cfopPadrao:'5102', unidade:'UN', precoVenda:8.49, custoMedio:5.60, margem:34.0, estoque:28, estoqueMinimo:30, estoqueMaximo:200, grupo:'Limpeza', marca:'Omo', status:'ativo' },
    { id:'p9', codigo:'009', codigoBarras:'7891234560009', descricao:'Detergente 500ml', tipo:'simples', ncm:'3402.20', cest:'', cfopPadrao:'5102', unidade:'UN', precoVenda:2.49, custoMedio:1.50, margem:39.8, estoque:55, estoqueMinimo:40, estoqueMaximo:300, grupo:'Limpeza', marca:'Ypê', status:'ativo' },
  ]);

  // ── Clientes ─────────────────────────────────────────────
  readonly clientes = signal<Cliente[]>([
    { id:'c1', tipo:'pj', documento:'12.345.678/0001-90', nome:'Maria Distribuidora Ltda', ie:'123.456.789.000', indicadorIe:'contribuinte', consumidorFinal:false, enderecos:[{ cep:'01310-100', logradouro:'Av. Paulista', numero:'1000', bairro:'Bela Vista', municipio:'São Paulo', uf:'SP' }], email:'nf@mariadist.com.br', telefone:'(11) 3333-4444', whatsapp:'(11) 91234-5678', limiteCredito:120000, saldoDevedor:38400, status:'regular' },
    { id:'c2', tipo:'pj', documento:'98.765.432/0001-10', nome:'Atacadão SP Ltda', ie:'987.654.321.000', indicadorIe:'contribuinte', consumidorFinal:false, enderecos:[{ cep:'04578-000', logradouro:'Av. dos Bandeirantes', numero:'2000', bairro:'Vila Olímpia', municipio:'São Paulo', uf:'SP' }], email:'nfe@atacadao.com.br', telefone:'(11) 2222-3333', whatsapp:'(11) 98765-4321', limiteCredito:80000, saldoDevedor:22100, status:'regular' },
    { id:'c3', tipo:'pf', documento:'123.456.789-00', nome:'João Pedro Silva', indicadorIe:'nao_contribuinte', consumidorFinal:true, enderecos:[{ cep:'02040-000', logradouro:'Rua das Flores', numero:'45', bairro:'Vila Guilherme', municipio:'São Paulo', uf:'SP' }], email:'joao@email.com', telefone:'(11) 99999-8888', limiteCredito:5000, saldoDevedor:850, status:'inadimplente' },
    { id:'c4', tipo:'pj', documento:'45.678.901/0001-23', nome:'Supermercado Bom Preço', ie:'456.789.012.000', indicadorIe:'contribuinte', consumidorFinal:false, enderecos:[{ cep:'08040-000', logradouro:'Av. Aricanduva', numero:'500', bairro:'Aricanduva', municipio:'São Paulo', uf:'SP' }], email:'compras@bompreco.com.br', telefone:'(11) 4444-5555', limiteCredito:50000, saldoDevedor:12000, status:'regular' },
  ]);

  // ── Fornecedores ─────────────────────────────────────────
  readonly fornecedores = signal<Fornecedor[]>([
    { id:'f1', tipo:'pj', documento:'11.222.333/0001-44', nome:'Distribuidora Alves', endereco:{ cep:'07000-000', logradouro:'Av. Comercial', numero:'100', bairro:'Centro', municipio:'Guarulhos', uf:'SP' }, email:'pedidos@alves.com.br', telefone:'(11) 1111-2222', prazoEntregaDias:3, chavePix:'11222333000144' },
    { id:'f2', tipo:'pj', documento:'22.333.444/0001-55', nome:'Frigorífico Central', endereco:{ cep:'09000-000', logradouro:'Rua Industrial', numero:'200', bairro:'Distrito Industrial', municipio:'Santo André', uf:'SP' }, email:'vendas@frigcentral.com.br', telefone:'(11) 5555-6666', prazoEntregaDias:1, chavePix:'22333444000155' },
    { id:'f3', tipo:'pj', documento:'33.444.555/0001-66', nome:'Grãos & Cia', endereco:{ cep:'14000-000', logradouro:'Estrada da Fazenda', numero:'Km3', bairro:'Rural', municipio:'Ribeirão Preto', uf:'SP' }, email:'comercial@graoecia.com.br', telefone:'(16) 3333-4444', prazoEntregaDias:5, chavePix:'33444555000166' },
  ]);

  // ── NF-e ─────────────────────────────────────────────────
  readonly nfes = signal<Nfe[]>([
    { id:'nfe1', numero:234, serie:'001', natureza:'Venda de mercadoria adquirida ou recebida de terceiros', cfop:'5102', dataEmissao:new Date('2026-05-29T14:32:00'), clienteId:'c2', clienteNome:'Atacadão SP Ltda', clienteDoc:'98.765.432/0001-10', itens:[], valorProdutos:3713.10, valorFrete:106.90, valorDesconto:0, valorTotal:3820.00, baseIcms:3820.00, valorIcms:687.60, valorPis:24.83, valorCofins:114.60, status:'autorizada', chaveAcesso:'35260512345678000190550010000002341000012341', protocolo:'135200000001234' },
    { id:'nfe2', numero:233, serie:'001', natureza:'Venda de mercadoria adquirida ou recebida de terceiros', cfop:'5102', dataEmissao:new Date('2026-05-29T13:58:00'), clienteId:'c1', clienteNome:'Maria Distribuidora Ltda', clienteDoc:'12.345.678/0001-90', itens:[], valorProdutos:8150.00, valorFrete:0, valorDesconto:0, valorTotal:8150.00, baseIcms:8150.00, valorIcms:1467.00, valorPis:52.98, valorCofins:244.50, status:'autorizada', chaveAcesso:'35260512345678000190550010000002331000012331', protocolo:'135200000001233' },
    { id:'nfe3', numero:232, serie:'001', natureza:'Venda de mercadoria sujeita a ST', cfop:'5403', dataEmissao:new Date('2026-05-29T11:20:00'), clienteId:'c4', clienteNome:'Supermercado Bom Preço', clienteDoc:'45.678.901/0001-23', itens:[], valorProdutos:1290.50, valorFrete:0, valorDesconto:0, valorTotal:1290.50, baseIcms:0, valorIcms:0, valorPis:0, valorCofins:0, status:'em_fila' },
    { id:'nfe4', numero:231, serie:'001', natureza:'Venda de mercadoria adquirida ou recebida de terceiros', cfop:'5102', dataEmissao:new Date('2026-05-28T17:05:00'), clienteId:'c3', clienteNome:'Carlos Mendes ME', clienteDoc:'67.890.123/0001-00', itens:[], valorProdutos:522.00, valorFrete:0, valorDesconto:0, valorTotal:522.00, baseIcms:522.00, valorIcms:93.96, valorPis:3.39, valorCofins:15.66, status:'autorizada', chaveAcesso:'35260512345678000190550010000002311000012311', protocolo:'135200000001231' },
    { id:'nfe5', numero:230, serie:'001', natureza:'Venda de mercadoria adquirida ou recebida de terceiros', cfop:'5102', dataEmissao:new Date('2026-05-28T09:44:00'), clienteId:'c4', clienteNome:'Loja Central Eireli', clienteDoc:'56.789.012/0001-11', itens:[], valorProdutos:14600.00, valorFrete:0, valorDesconto:0, valorTotal:14600.00, baseIcms:0, valorIcms:0, valorPis:0, valorCofins:0, status:'cancelada' },
  ]);

  // ── NFS-e ─────────────────────────────────────────────────
  readonly nfses = signal<Nfse[]>([
    { id:'nfse1', numero:'NFS-0041', rpsNumero:'RPS-0041', tomadorNome:'Tech Corp Ltda', tomadorDoc:'71.234.567/0001-89', municipio:'São Paulo', uf:'SP', padrao:'abrasf', codigoServico:'1.07', discriminacao:'Serviços de suporte e manutenção de sistemas', valorServico:12000, aliquotaIss:2.0, valorIss:240, retencoes:0, dataEmissao:new Date('2026-05-28'), status:'autorizada' },
    { id:'nfse2', numero:'NFS-0040', rpsNumero:'RPS-0040', tomadorNome:'João Ferreira ME', tomadorDoc:'34.567.890/0001-12', municipio:'Campinas', uf:'SP', padrao:'nacional', codigoServico:'7.01', discriminacao:'Serviços de engenharia civil', valorServico:8500, aliquotaIss:2.5, valorIss:212.50, retencoes:85, dataEmissao:new Date('2026-05-27'), status:'autorizada' },
    { id:'nfse3', numero:'NFS-0039', rpsNumero:'RPS-0039', tomadorNome:'Indústria Alfa SA', tomadorDoc:'23.456.789/0001-01', municipio:'Santos', uf:'SP', padrao:'abrasf', codigoServico:'14.01', discriminacao:'Serviços de lubrificação, limpeza e conservação de máquinas', valorServico:3200, aliquotaIss:2.0, valorIss:64, retencoes:0, dataEmissao:new Date('2026-05-29'), status:'em_envio' },
  ]);

  // ── Movimentos estoque ───────────────────────────────────
  readonly movimentos = signal<MovimentoEstoque[]>([
    { id:'m1', dataHora:new Date('2026-05-29T14:32'), produtoId:'p1', produtoNome:'Arroz Camil 5kg', tipo:'saida', quantidade:-100, saldoApos:48, origem:'NF-e #000234', usuario:'Ricardo L.' },
    { id:'m2', dataHora:new Date('2026-05-29T10:15'), produtoId:'p2', produtoNome:'Feijão Carioca 1kg', tipo:'entrada', quantidade:500, saldoApos:620, origem:'Compra #089', usuario:'Ana P.' },
    { id:'m3', dataHora:new Date('2026-05-28T16:40'), produtoId:'p7', produtoNome:'Café Pilão 500g', tipo:'saida', quantidade:-24, saldoApos:8, origem:'PDV Caixa 01', usuario:'Operador' },
    { id:'m4', dataHora:new Date('2026-05-28T09:00'), produtoId:'p3', produtoNome:'Leite Integral 1L', tipo:'ajuste', quantidade:-3, saldoApos:72, origem:'Inventário', usuario:'Gerente' },
    { id:'m5', dataHora:new Date('2026-05-27T14:00'), produtoId:'p8', produtoNome:'Sabão em Pó 1kg', tipo:'saida', quantidade:-18, saldoApos:28, origem:'PDV Caixa 02', usuario:'Operador' },
  ]);

  // ── Pedidos de compra ─────────────────────────────────────
  readonly pedidosCompra = signal<PedidoCompra[]>([
    { id:'oc1', numero:'OC-0089', fornecedorId:'f1', fornecedorNome:'Distribuidora Alves', dataPedido:new Date('2026-05-29'), dataPrevisao:new Date('2026-06-01'), itens:[], valorTotal:18400, status:'aguarda_aprovacao' },
    { id:'oc2', numero:'OC-0088', fornecedorId:'f2', fornecedorNome:'Frigorífico Central', dataPedido:new Date('2026-05-28'), dataPrevisao:new Date('2026-05-30'), itens:[], valorTotal:9200, status:'em_transito' },
    { id:'oc3', numero:'OC-0087', fornecedorId:'f3', fornecedorNome:'Grãos & Cia', dataPedido:new Date('2026-05-26'), dataPrevisao:new Date('2026-05-29'), itens:[], valorTotal:14780, status:'encerrado' },
    { id:'oc4', numero:'OC-0086', fornecedorId:'f1', fornecedorNome:'Laticínios Norte', dataPedido:new Date('2026-05-24'), dataPrevisao:undefined, itens:[], valorTotal:3400, status:'cancelado' },
  ]);

  // ── Títulos financeiros ──────────────────────────────────
  readonly titulos = signal<Titulo[]>([
    { id:'t1', tipo:'receber', descricao:'NF-e #000234', parceiro:'Atacadão SP Ltda', vencimento:new Date('2026-05-29'), valor:3820, status:'aberto', formaPagamento:'Boleto', origem:'NF-e' },
    { id:'t2', tipo:'receber', descricao:'NF-e #000233', parceiro:'Maria Distribuidora Ltda', vencimento:new Date('2026-05-31'), valor:8150, status:'aberto', formaPagamento:'Boleto', origem:'NF-e' },
    { id:'t3', tipo:'receber', descricao:'Cobrança avulsa', parceiro:'João Pedro Silva', vencimento:new Date('2026-05-17'), valor:850, status:'vencido', formaPagamento:'PIX', origem:'Manual' },
    { id:'t4', tipo:'pagar', descricao:'OC-0089', parceiro:'Distribuidora Alves', vencimento:new Date('2026-05-30'), valor:18400, status:'aberto', formaPagamento:'PIX', origem:'Compra' },
    { id:'t5', tipo:'pagar', descricao:'Aluguel sede', parceiro:'Imobiliária Central', vencimento:new Date('2026-06-05'), valor:4800, status:'aberto', formaPagamento:'Débito automático', origem:'Manual', centoCusto:'Administrativo' },
    { id:'t6', tipo:'pagar', descricao:'Internet + Telefonia', parceiro:'TIM Business', vencimento:new Date('2026-06-08'), valor:680, status:'aberto', formaPagamento:'Boleto', origem:'Manual' },
    { id:'t7', tipo:'receber', descricao:'NF-e #000230', parceiro:'Supermercado Bom Preço', vencimento:new Date('2026-06-10'), valor:5400, status:'aberto', formaPagamento:'Boleto', origem:'NF-e' },
    { id:'t8', tipo:'pagar', descricao:'Energia elétrica', parceiro:'EDP São Paulo', vencimento:new Date('2026-06-12'), valor:1240, status:'aberto', formaPagamento:'Boleto', origem:'Manual' },
  ]);

  // ── Oportunidades CRM ─────────────────────────────────────
  readonly oportunidades = signal<Oportunidade[]>([
    { id:'op1', titulo:'Supermercado do Bairro', clienteNome:'Supermercado do Bairro', valor:15000, probabilidade:40, estagio:'prospeccao', responsavel:'Ana Paula' },
    { id:'op2', titulo:'Padaria São José', clienteNome:'Padaria São José', valor:4200, probabilidade:30, estagio:'prospeccao', responsavel:'Ricardo L.' },
    { id:'op3', titulo:'Hortifrúti Central', clienteNome:'Hortifrúti Central', valor:8700, probabilidade:25, estagio:'prospeccao', responsavel:'Ricardo L.' },
    { id:'op4', titulo:'Atacadão Regional', clienteNome:'Atacadão Regional', valor:48000, probabilidade:75, estagio:'proposta', responsavel:'Carlos M.' },
    { id:'op5', titulo:'Mini Mercado Flores', clienteNome:'Mini Mercado Flores', valor:9300, probabilidade:60, estagio:'proposta', responsavel:'Ana Paula' },
    { id:'op6', titulo:'Rede Compra Fácil', clienteNome:'Rede Compra Fácil', valor:95000, probabilidade:85, estagio:'negociacao', responsavel:'Ricardo L.' },
    { id:'op7', titulo:'Mercadão Mega', clienteNome:'Mercadão Mega', valor:22000, probabilidade:70, estagio:'negociacao', responsavel:'Carlos M.' },
    { id:'op8', titulo:'Maria Distribuidora', clienteNome:'Maria Distribuidora Ltda', valor:38000, probabilidade:100, estagio:'fechado', responsavel:'Ricardo L.' },
    { id:'op9', titulo:'Atacadão SP', clienteNome:'Atacadão SP Ltda', valor:12400, probabilidade:100, estagio:'fechado', responsavel:'Ana Paula' },
    { id:'op10', titulo:'Loja Central', clienteNome:'Loja Central Eireli', valor:7100, probabilidade:0, estagio:'perdido', responsavel:'Carlos M.' },
  ]);

  // ── Computed helpers ─────────────────────────────────────
  readonly produtosCriticos = computed(() =>
    this.produtos().filter(p => p.estoque <= p.estoqueMinimo)
  );

  readonly titulosVencidos = computed(() =>
    this.titulos().filter(t => t.status === 'vencido')
  );

  readonly totalReceber = computed(() =>
    this.titulos().filter(t => t.tipo === 'receber' && t.status === 'aberto')
      .reduce((s, t) => s + t.valor, 0)
  );

  readonly totalPagar = computed(() =>
    this.titulos().filter(t => t.tipo === 'pagar' && t.status === 'aberto')
      .reduce((s, t) => s + t.valor, 0)
  );

  // ── Mutations ─────────────────────────────────────────────
  addNfe(nfe: Nfe) { this.nfes.update(list => [nfe, ...list]); }

  updateNfeStatus(id: string, status: Nfe['status']) {
    this.nfes.update(list => list.map(n => n.id === id ? { ...n, status } : n));
  }

  updatePedidoCompraStatus(id: string, status: PedidoCompra['status']) {
    this.pedidosCompra.update(list => list.map(p => p.id === id ? { ...p, status } : p));
  }

  getProdutoById(id: string): Produto | undefined {
    return this.produtos().find(p => p.id === id);
  }

  getClienteById(id: string): Cliente | undefined {
    return this.clientes().find(c => c.id === id);
  }
}
