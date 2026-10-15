package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.domain.PaymentMethodType;
import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.domain.tax.NfeDocumentStatus;
import br.gravita.core.domain.tax.TaxType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** A row of the NF-e list. {@code emittedAt} is the authorization instant, or the creation instant while not yet authorized. */
public record NfeListItemResponse(UUID id, Long documentNumber, String documentSeries, String recipientName, String cfop,
		Instant emittedAt, BigDecimal totalValue, BigDecimal icmsValue, NfeDocumentStatus status,
		String cancellationJustification, Instant cancelledAt, PaymentMethodType paymentMethodType) {

	public static NfeListItemResponse from(final NfeDocument document) {
		final Instant emittedAt = document.getAuthorizedAt() != null ? document.getAuthorizedAt() : document.getCreatedAt();
		return new NfeListItemResponse(document.getId().value(), document.getDocumentNumber(),
				document.getDocumentSeries(), document.getRecipient().name(), document.getCfop().code(), emittedAt,
				document.getDocumentTotal(), document.getTaxTotals().byTaxType().getOrDefault(TaxType.ICMS, BigDecimal.ZERO),
				document.getStatus(), document.getCancellationJustification(), document.getCancelledAt(), document.getPaymentMethodType());
	}
}
