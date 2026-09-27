package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.domain.tax.VoidedNumberRange;
import java.time.Instant;
import java.util.UUID;

public record VoidedNumberRangeResponse(UUID id, UUID companyId, String series, Long startNumber, Long endNumber,
		String justification, String protocol, Instant voidedAt) {

	public static VoidedNumberRangeResponse from(VoidedNumberRange voidedNumberRange) {
		return new VoidedNumberRangeResponse(voidedNumberRange.getId().value(),
				voidedNumberRange.getCompanyId().value(), voidedNumberRange.getSeries(),
				voidedNumberRange.getStartNumber(), voidedNumberRange.getEndNumber(),
				voidedNumberRange.getJustification(), voidedNumberRange.getProtocol(),
				voidedNumberRange.getVoidedAt());
	}
}
