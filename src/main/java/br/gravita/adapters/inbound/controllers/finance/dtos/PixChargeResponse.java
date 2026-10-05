package br.gravita.adapters.inbound.controllers.finance.dtos;

import br.gravita.core.domain.finance.PixCharge;
import br.gravita.core.domain.finance.PixChargeStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PixChargeResponse(UUID id, UUID receivableId, String dynamicQrPayload, BigDecimal amount,
		LocalDate dueDate, Instant expiresAt, PixChargeStatus status) {

	public static PixChargeResponse from(final PixCharge pixCharge) {
		return new PixChargeResponse(pixCharge.getId().value(), pixCharge.getReceivableId().value(),
				pixCharge.getDynamicQrPayload(), pixCharge.getAmount(), pixCharge.getDueDate(),
				pixCharge.getExpiresAt(), pixCharge.getStatus());
	}
}
