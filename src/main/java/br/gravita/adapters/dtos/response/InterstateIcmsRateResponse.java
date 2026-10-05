package br.gravita.adapters.dtos.response;

import br.gravita.core.domain.InterstateIcmsRateDomain;

import java.math.BigDecimal;
import java.util.UUID;

public record InterstateIcmsRateResponse(UUID id, String originState, String destinationState, BigDecimal ratePercent) {

	public static InterstateIcmsRateResponse from(final InterstateIcmsRateDomain domain) {
		return new InterstateIcmsRateResponse(domain.getId(), domain.getOriginState(), domain.getDestinationState(), domain.getRatePercent());
	}
}
