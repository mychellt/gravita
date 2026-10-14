package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.domain.tax.InboundNfeStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InboundNfeListItemResponse(UUID id, String series, String number, String supplierName, Instant issuedAt,
		BigDecimal totalValue, InboundNfeStatus status) {

	public static InboundNfeListItemResponse from(final InboundNfe inboundNfe) {
		return new InboundNfeListItemResponse(inboundNfe.getId().value(), inboundNfe.getSeries(), inboundNfe.getNumber(),
				inboundNfe.getSupplierName(), inboundNfe.getIssuedAt(), inboundNfe.getTotals().totalValue(),
				inboundNfe.getStatus());
	}
}
