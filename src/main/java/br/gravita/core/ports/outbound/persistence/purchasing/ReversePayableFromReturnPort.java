package br.gravita.core.ports.outbound.persistence.purchasing;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

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
