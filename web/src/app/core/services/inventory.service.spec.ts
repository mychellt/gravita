import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { DataService } from './data.service';
import { InventoryService, Resultado } from './inventory.service';
import { DEPOSITO_FILIAL, DEPOSITO_MATRIZ } from './inventory.seed';

const PRODUTO_ARROZ = 'p1';       // 48 on hand, 20 reserved → 28 available
const PRODUTO_QUEIJO = 'p10';     // lot-controlled; L-2409X is expired, L-2410A has 15
const PRODUTO_OLEO = 'p5';        // 50 already in transit to the branch

describe('InventoryService', () => {
  let service: InventoryService;
  let data: DataService;

  const fisico = (produtoId: string, depositoId: string) =>
    service.saldosDoProduto(produtoId).find(s => s.depositoId === depositoId)?.fisico ?? 0;
  const erro = (resultado: Resultado) => (resultado.ok ? '' : resultado.erro);

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(InventoryService);
    data = TestBed.inject(DataService);
  });

  describe('adjustments', () => {
    it('rejects an adjustment without justification', () => {
      const resultado = service.ajustar({ produtoId: PRODUTO_ARROZ, depositoId: DEPOSITO_MATRIZ, variacao: 5, justificativa: '  ' });

      expect(erro(resultado)).toContain('justificativa');
    });

    it('applies a positive adjustment and logs it with the justification', () => {
      const antes = data.movimentos().length;

      const resultado = service.ajustar({ produtoId: PRODUTO_ARROZ, depositoId: DEPOSITO_MATRIZ, variacao: 10, justificativa: 'Achado na contagem' });

      expect(resultado.ok).toBeTrue();
      expect(fisico(PRODUTO_ARROZ, DEPOSITO_MATRIZ)).toBe(58);
      expect(data.produtos().find(p => p.id === PRODUTO_ARROZ)?.estoque).toBe(58);
      expect(data.movimentos().length).toBe(antes + 1);
      expect(data.movimentos()[0]).toEqual(jasmine.objectContaining({
        tipo: 'ajuste', quantidade: 10, saldoApos: 58, justificativa: 'Achado na contagem',
      }));
    });

    it('blocks a negative adjustment above the available balance', () => {
      const resultado = service.ajustar({ produtoId: PRODUTO_ARROZ, depositoId: DEPOSITO_MATRIZ, variacao: -30, justificativa: 'Avaria' });

      expect(erro(resultado)).toContain('não permite estoque negativo');
      expect(fisico(PRODUTO_ARROZ, DEPOSITO_MATRIZ)).toBe(48);
    });

    it('allows going negative once the product is configured to', () => {
      service.definirPermiteEstoqueNegativo(PRODUTO_ARROZ, true);

      const resultado = service.ajustar({ produtoId: PRODUTO_ARROZ, depositoId: DEPOSITO_MATRIZ, variacao: -30, justificativa: 'Avaria' });

      expect(resultado.ok).toBeTrue();
      expect(fisico(PRODUTO_ARROZ, DEPOSITO_MATRIZ)).toBe(18);
    });
  });

  describe('transfers', () => {
    it('moves stock to in-transit at the destination until confirmed', () => {
      const resultado = service.iniciarTransferencia({ produtoId: PRODUTO_ARROZ, origemId: DEPOSITO_MATRIZ, destinoId: DEPOSITO_FILIAL, quantidade: 10 });

      const destino = service.saldosDoProduto(PRODUTO_ARROZ).find(s => s.depositoId === DEPOSITO_FILIAL);
      expect(resultado.ok).toBeTrue();
      expect(fisico(PRODUTO_ARROZ, DEPOSITO_MATRIZ)).toBe(38);
      expect(destino?.emTransito).toBe(10);
      expect(destino?.fisico).toBe(0);
    });

    it('refuses to send more than the available balance', () => {
      const resultado = service.iniciarTransferencia({ produtoId: PRODUTO_ARROZ, origemId: DEPOSITO_MATRIZ, destinoId: DEPOSITO_FILIAL, quantidade: 29 });

      expect(erro(resultado)).toContain('insuficiente');
    });

    it('refuses the same warehouse as origin and destination', () => {
      const resultado = service.iniciarTransferencia({ produtoId: PRODUTO_ARROZ, origemId: DEPOSITO_MATRIZ, destinoId: DEPOSITO_MATRIZ, quantidade: 1 });

      expect(erro(resultado)).toContain('diferentes');
    });

    it('credits the destination on confirmation and rejects a second confirmation', () => {
      const pendente = service.transferenciasPendentes()[0];

      expect(service.confirmarTransferencia(pendente.id).ok).toBeTrue();

      const destino = service.saldosDoProduto(PRODUTO_OLEO).find(s => s.depositoId === DEPOSITO_FILIAL);
      expect(destino?.fisico).toBe(50);
      expect(destino?.emTransito).toBe(0);
      expect(erro(service.confirmarTransferencia(pendente.id))).toContain('já foi confirmada');
    });

    it('requires a lot for lot-controlled products and rejects an expired one', () => {
      const base = { produtoId: PRODUTO_QUEIJO, origemId: DEPOSITO_MATRIZ, destinoId: DEPOSITO_FILIAL, quantidade: 2 };

      expect(erro(service.iniciarTransferencia(base))).toContain('Selecione o lote');
      expect(erro(service.iniciarTransferencia({ ...base, lote: 'L-2409X' }))).toContain('vencido');
    });

    it('carries the lot to the destination when the transfer is confirmed', () => {
      service.iniciarTransferencia({ produtoId: PRODUTO_QUEIJO, origemId: DEPOSITO_MATRIZ, destinoId: DEPOSITO_FILIAL, quantidade: 5, lote: 'L-2410A' });
      const transferencia = service.transferenciasPendentes().find(t => t.produtoId === PRODUTO_QUEIJO)!;

      service.confirmarTransferencia(transferencia.id);

      const lotes = service.lotes().filter(l => l.codigo === 'L-2410A');
      expect(lotes.find(l => l.depositoId === DEPOSITO_MATRIZ)?.quantidade).toBe(10);
      expect(lotes.find(l => l.depositoId === DEPOSITO_FILIAL)?.quantidade).toBe(5);
    });
  });

  describe('reservations', () => {
    it('releases an active reservation back to the available balance', () => {
      const reserva = service.reservas().find(r => r.status === 'ativa' && r.produtoId === PRODUTO_ARROZ)!;

      expect(service.liberarReserva(reserva.id).ok).toBeTrue();

      const saldo = service.saldosDoProduto(PRODUTO_ARROZ)[0];
      expect(service.disponivel(saldo)).toBe(48);
      expect(erro(service.liberarReserva(reserva.id))).toContain('ativas');
    });
  });

  describe('expiry', () => {
    it('flags expired lots and lots inside the alert window', () => {
      const codigos = service.lotesAVencer().map(l => l.codigo);

      expect(codigos).toContain('L-2409X');
      expect(codigos).toContain('L-2410A');
      expect(codigos).not.toContain('L-2411B');
    });

    it('widens the window when configured', () => {
      service.janelaValidadeDias.set(60);

      expect(service.lotesAVencer().map(l => l.codigo)).toContain('L-2411B');
    });
  });

  describe('physical count', () => {
    const abrirContagemTotalParcial = () => {
      service.iniciarContagem({ escopo: 'parcial', grupo: 'Limpeza', depositoId: DEPOSITO_MATRIZ });
      return service.contagens()[0];
    };

    it('requires a group for a partial count', () => {
      expect(erro(service.iniciarContagem({ escopo: 'parcial', depositoId: DEPOSITO_MATRIZ }))).toContain('grupo');
    });

    it('snapshots the system quantity of every product in the group', () => {
      const contagem = abrirContagemTotalParcial();

      expect(contagem.status).toBe('em_andamento');
      expect(contagem.linhas.map(l => l.produtoNome)).toEqual(['Sabão em Pó 1kg', 'Detergente 500ml']);
      expect(contagem.linhas.map(l => l.qtdSistema)).toEqual([28, 55]);
    });

    it('stays in progress on a partial submission and becomes pending approval once complete', () => {
      const { id } = abrirContagemTotalParcial();

      service.enviarContagem(id, { p8: 27 });
      expect(service.contagens()[0].status).toBe('em_andamento');

      service.enviarContagem(id, { p9: 55 });
      expect(service.contagens()[0].status).toBe('aguardando_aprovacao');
      expect(service.contagens()[0].linhas[0].qtdContada).toBe(27);
    });

    it('rejects negative counts and submissions to a count that is no longer in progress', () => {
      const { id } = abrirContagemTotalParcial();

      expect(erro(service.enviarContagem(id, { p8: -1 }))).toContain('negativa');

      service.enviarContagem(id, { p8: 28, p9: 55 });
      expect(erro(service.enviarContagem(id, { p8: 1 }))).toContain('em andamento');
    });

    it('only approves a count that is pending approval', () => {
      const { id } = abrirContagemTotalParcial();

      expect(erro(service.aprovarContagem(id))).toContain('aguardando aprovação');
    });

    it('posts one adjustment per divergent line on approval and none for matching lines', () => {
      const { id } = abrirContagemTotalParcial();
      service.enviarContagem(id, { p8: 25, p9: 55 });
      const movimentosAntes = data.movimentos().length;

      expect(service.aprovarContagem(id).ok).toBeTrue();

      expect(data.movimentos().length).toBe(movimentosAntes + 1);
      expect(data.movimentos()[0]).toEqual(jasmine.objectContaining({
        produtoId: 'p8', quantidade: -3, tipo: 'ajuste', justificativa: `Divergência da contagem ${id}`,
      }));
      expect(fisico('p8', DEPOSITO_MATRIZ)).toBe(25);
      expect(service.contagens()[0].status).toBe('aprovada');
    });
  });

  describe('reorder suggestions', () => {
    it('suggests items at or below the reorder point, refilling up to the maximum', () => {
      const sugestao = service.sugestoesReposicao().find(s => s.produtoId === 'p7')!;

      expect(sugestao.disponivel).toBe(3);          // Café: 8 on hand − 5 reserved
      expect(sugestao.pontoReposicao).toBe(20);
      expect(sugestao.quantidadeSugerida).toBe(197); // maximum 200 − 3
    });

    it('does not suggest items above the reorder point', () => {
      expect(service.sugestoesReposicao().some(s => s.produtoId === 'p2')).toBeFalse();
    });

    it('creates a purchase request once per item', () => {
      const sugestao = service.sugestoesReposicao()[0];

      expect(service.solicitarCompra(sugestao).ok).toBeTrue();
      expect(service.jaRequisitado(sugestao)).toBeTrue();
      expect(erro(service.solicitarCompra(sugestao))).toContain('Já existe');
    });
  });
});
