package br.gravita.core.ports.inbound.sales;

public interface ApproveSalesOrderUseCase {
	SalesOrderView execute(ApproveSalesOrderCommand command);
}
