package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.domain.tax.VoidedNumberRange;
import java.time.Instant;
import java.util.UUID;

public record VoidNumberRangeResponse(UUID id, String series, Long startNumber, Long endNumber, String sefazProtocol,
		Instant voidedAt) {

	public static VoidNumberRangeResponse from(final VoidedNumberRange voidedNumberRange) {
		return new VoidNumberRangeResponse(voidedNumberRange.getId().value(), voidedNumberRange.getSeries(),
				voidedNumberRange.getStartNumber(), voidedNumberRange.getEndNumber(),
				voidedNumberRange.getSefazProtocol(), voidedNumberRange.getVoidedAt());
	}
}
