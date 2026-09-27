package br.gravita.core.ports.inbound.sales;

public interface ConvertQuoteToOrderUseCase {
	SalesOrderView execute(ConvertQuoteToOrderCommand command);
}
