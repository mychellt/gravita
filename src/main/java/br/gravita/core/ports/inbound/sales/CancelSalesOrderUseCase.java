package br.gravita.core.ports.inbound.sales;

public interface CancelSalesOrderUseCase {
	void execute(CancelSalesOrderCommand command);
}
