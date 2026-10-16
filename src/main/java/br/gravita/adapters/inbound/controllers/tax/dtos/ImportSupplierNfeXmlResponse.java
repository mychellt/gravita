package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.domain.tax.InboundNfe;
import java.math.BigDecimal;
import java.util.UUID;

public record ImportSupplierNfeXmlResponse(
		UUID id,
		String accessKey,
		String supplierName,
		int itemCount,
		BigDecimal totalValue,
		String status,
		/** GRA-246 AC1: whether a Supplier is already registered for this NFe's CNPJ; never blocks the import either way. */
		boolean supplierResolved) {

	public static ImportSupplierNfeXmlResponse from(final InboundNfe inboundNfe, final boolean supplierResolved) {
		return new ImportSupplierNfeXmlResponse(
				inboundNfe.getId().value(),
				inboundNfe.getAccessKey(),
				inboundNfe.getSupplierName(),
				inboundNfe.getItems().size(),
				inboundNfe.getTotals().totalValue(),
				inboundNfe.getStatus().name(),
				supplierResolved);
	}
}
