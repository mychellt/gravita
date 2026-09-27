package br.gravita.core.ports.inbound.sales;

public interface ReturnSalesOrderUseCase {

	SalesReturnView execute(ReturnSalesOrderCommand command);
}
