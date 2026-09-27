package br.gravita.core.domain.tax;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record InboundNfeConferenceItem(UUID itemRef, BigDecimal orderedQty, BigDecimal receivedQty) {

	public InboundNfeConferenceItem {
		Objects.requireNonNull(itemRef, "itemRef is required");
		Objects.requireNonNull(orderedQty, "orderedQty is required");
		Objects.requireNonNull(receivedQty, "receivedQty is required");
		if (orderedQty.compareTo(BigDecimal.ZERO) < 0) {
			throw new BusinessRuleException("orderedQty cannot be negative: " + orderedQty);
		}
		if (receivedQty.compareTo(BigDecimal.ZERO) < 0) {
			throw new BusinessRuleException("receivedQty cannot be negative: " + receivedQty);
		}
	}
}
