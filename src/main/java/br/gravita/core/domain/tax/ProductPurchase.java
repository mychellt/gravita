package br.gravita.core.domain.tax;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One purchase of a catalog product, as a confirmed inbound NFe states it: who sold it, on which note, when, how many
 * and at what unit price. A product bought from several suppliers has one entry per supplier per note.
 */
public record ProductPurchase(UUID inboundNfeId, String series, String number, String supplierDocument, String supplierName,
		Instant issuedAt, BigDecimal quantity, BigDecimal unitValue) {
}
