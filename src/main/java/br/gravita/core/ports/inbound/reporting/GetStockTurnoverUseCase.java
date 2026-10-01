package br.gravita.core.ports.inbound.reporting;

import java.util.List;

public interface GetStockTurnoverUseCase {
	List<StockTurnoverEntry> execute(StockTurnoverQuery query);
}
