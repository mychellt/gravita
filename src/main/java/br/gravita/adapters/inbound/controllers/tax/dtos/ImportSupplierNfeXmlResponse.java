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
		String status) {

	public static ImportSupplierNfeXmlResponse from(InboundNfe inboundNfe) {
		return new ImportSupplierNfeXmlResponse(
				inboundNfe.getId().value(),
				inboundNfe.getAccessKey(),
				inboundNfe.getSupplierName(),
				inboundNfe.getItems().size(),
				inboundNfe.getTotals().totalValue(),
				inboundNfe.getStatus().name());
	}
}
