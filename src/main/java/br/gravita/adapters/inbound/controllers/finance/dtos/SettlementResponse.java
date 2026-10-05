package br.gravita.adapters.inbound.controllers.finance.dtos;

import br.gravita.core.domain.finance.Settlement;
import br.gravita.core.domain.finance.SettlementMethod;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SettlementResponse(UUID id, UUID receivableId, BigDecimal amount, BigDecimal interest,
		BigDecimal fine, BigDecimal discount, BigDecimal surcharge, SettlementMethod method, Instant timestamp) {

	public static SettlementResponse from(final Settlement settlement) {
		return new SettlementResponse(settlement.getId().value(), settlement.getReceivableId().value(),
				settlement.getAmount(), settlement.getInterest(), settlement.getFine(), settlement.getDiscount(),
				settlement.getSurcharge(), settlement.getMethod(), settlement.getTimestamp());
	}
}
