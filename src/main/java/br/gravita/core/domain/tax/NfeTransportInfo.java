package br.gravita.core.domain.tax;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import java.math.BigDecimal;

/**
 * Transport section of an NFe (doc §3.1). Optional on the document as a
 * whole — plenty of operations (e.g. a same-city complementary note) carry no
 * transport data at all.
 */
public record NfeTransportInfo(TransportModality modality, String carrier, Integer volume, BigDecimal grossWeight,
		BigDecimal netWeight, String rntrc) {

	public NfeTransportInfo {
		if (grossWeight != null && grossWeight.compareTo(BigDecimal.ZERO) < 0) {
			throw new BusinessRuleException("grossWeight cannot be negative: " + grossWeight);
		}
		if (netWeight != null && netWeight.compareTo(BigDecimal.ZERO) < 0) {
			throw new BusinessRuleException("netWeight cannot be negative: " + netWeight);
		}
		if (volume != null && volume < 0) {
			throw new BusinessRuleException("volume cannot be negative: " + volume);
		}
	}
}
