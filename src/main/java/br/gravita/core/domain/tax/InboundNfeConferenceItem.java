package br.gravita.core.domain.tax;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * One line of UC-M2-10's three-way conference. {@code itemRef} is the
 * resolved masterdata product id for the corresponding {@link InboundNfeItem}
 * (conference entries line up positionally with {@link InboundNfe#getItems()});
 * matching the supplier's own {@code supplierProductCode} to a catalog
 * product happens before this use case is invoked (per {@link InboundNfeItem}'s
 * javadoc), not here. {@code orderedQty}/{@code receivedQty} complete the
 * comparison against what the NF itself states ({@link InboundNfeItem#quantity()}).
 */
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
