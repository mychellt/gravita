package br.gravita.core.ports.outbound.sales;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Requests the receivable generated for the original invoice be reduced or
 * cancelled in {@code finance} for a customer return (UC-M7-07). Mirrors
 * {@link GenerateAccountsReceivablePort}: {@code finance} (M8) is not
 * implemented yet, so this is currently a fire-and-forget notification
 * rather than a call whose result feeds back into the sales return.
 */
public interface AdjustReceivableForReturnPort {

	void adjust(AdjustReceivableForReturnCommand command);

	record AdjustReceivableForReturnCommand(UUID sourceSalesReturnId, UUID customerId, BigDecimal amount) {

		public AdjustReceivableForReturnCommand {
			Objects.requireNonNull(sourceSalesReturnId, "sourceSalesReturnId is required");
			Objects.requireNonNull(customerId, "customerId is required");
			Objects.requireNonNull(amount, "amount is required");
		}
	}
}
