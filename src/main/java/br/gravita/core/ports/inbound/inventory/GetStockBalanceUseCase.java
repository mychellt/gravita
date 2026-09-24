package br.gravita.core.ports.inbound.inventory;

public interface GetStockBalanceUseCase {
	StockBalanceView execute(GetStockBalanceQuery query);
}
