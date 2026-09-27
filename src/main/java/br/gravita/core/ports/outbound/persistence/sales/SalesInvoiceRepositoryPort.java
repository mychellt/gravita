package br.gravita.core.ports.outbound.persistence.sales;

import br.gravita.core.domain.sales.SalesInvoice;
import br.gravita.core.domain.sales.SalesInvoiceId;
import java.util.Optional;

public interface SalesInvoiceRepositoryPort {
	SalesInvoice save(SalesInvoice invoice);
	Optional<SalesInvoice> findById(SalesInvoiceId id);
}
