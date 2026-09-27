package br.gravita.core.domain.tax;

import java.math.BigDecimal;

/**
 * Transport data (module spec §3.1): modality (CIF/FOB), carrier, volume,
 * gross/net weight, RNTRC. Optional on {@link NfeDocument} - not every
 * operation moves physical goods needing a carrier.
 */
public record NfeTransport(
		TransportModality modality,
		String carrier,
		String volume,
		BigDecimal grossWeight,
		BigDecimal netWeight,
		String rntrc) {
}
