package br.gravita.core.ports.inbound.sales;

public interface InvoiceSalesOrderUseCase {

	SalesInvoiceView execute(InvoiceSalesOrderCommand command);
}
