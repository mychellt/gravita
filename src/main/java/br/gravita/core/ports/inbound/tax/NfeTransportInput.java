package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.tax.TransportModality;
import java.math.BigDecimal;

public record NfeTransportInput(
		TransportModality modality,
		String carrier,
		String volume,
		BigDecimal grossWeight,
		BigDecimal netWeight,
		String rntrc) {
}
