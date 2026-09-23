package br.gravita.core.ports.outbound.persistence.purchasing;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Extension point for the finance context (M8), which owns accounts payable.
 * Mirrors {@link GeneratePayableFromReceiptPort} in reverse: UC-M6-09 calls
 * this once per return with the value of the returned items so the payable
 * generated on confirm is reduced accordingly once M8 exists. Until then this
 * is backed by a stub adapter, same as {@link GeneratePayableFromReceiptPort};
 * M8's own payable-generation use case (GRA-14) is expected to supply the
 * real adapter once it lands.
 */
public interface ReversePayableFromReturnPort {
	void reversePayable(ReversePayableFromReturnCommand command);

	record ReversePayableFromReturnCommand(UUID sourcePurchaseReturnId, UUID supplierId, BigDecimal amount) {

		public ReversePayableFromReturnCommand {
			Objects.requireNonNull(sourcePurchaseReturnId, "sourcePurchaseReturnId is required");
			Objects.requireNonNull(supplierId, "supplierId is required");
			Objects.requireNonNull(amount, "amount is required");
		}
	}
}
