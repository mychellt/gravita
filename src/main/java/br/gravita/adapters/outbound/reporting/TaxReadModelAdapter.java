package br.gravita.adapters.outbound.reporting;

import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.sales.SalesInvoiceStatus;
import br.gravita.core.domain.sales.SalesOrder;
import br.gravita.core.ports.outbound.persistence.sales.SalesInvoiceRepositoryPort;
import br.gravita.core.ports.outbound.persistence.sales.SalesOrderRepositoryPort;
import br.gravita.core.ports.outbound.reporting.TaxReadModelPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/** Sales orders carry no company, so {@code companyId} cannot narrow this read yet. */
@PersistenceAdapter
class TaxReadModelAdapter implements TaxReadModelPort {

	private final SalesOrderRepositoryPort salesOrderRepositoryPort;
	private final SalesInvoiceRepositoryPort salesInvoiceRepositoryPort;

	TaxReadModelAdapter(SalesOrderRepositoryPort salesOrderRepositoryPort,
			SalesInvoiceRepositoryPort salesInvoiceRepositoryPort) {
		this.salesOrderRepositoryPort = salesOrderRepositoryPort;
		this.salesInvoiceRepositoryPort = salesInvoiceRepositoryPort;
	}

	@Override
	@Transactional(readOnly = true)
	public BigDecimal invoicedTotal(LocalDate from, LocalDate to, UUID companyId) {
		return salesOrderRepositoryPort.findInvoicedByPeriod(from, to).stream()
				.filter(order -> hasIssuedFiscalDocument(order))
				.map(SalesOrder::totalValue).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private boolean hasIssuedFiscalDocument(SalesOrder order) {
		return salesInvoiceRepositoryPort.findByOrderId(order.getId())
				.filter(invoice -> invoice.getStatus() == SalesInvoiceStatus.ISSUED).isPresent();
	}
}
