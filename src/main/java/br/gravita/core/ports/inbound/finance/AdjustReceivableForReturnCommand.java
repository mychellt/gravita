package br.gravita.core.ports.inbound.finance;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * @param returnedAmount the value of the goods returned, in the receivable's currency
 * @param salesReturnRef id of the M7 sales return that triggered the adjustment
 */
public record AdjustReceivableForReturnCommand(UUID receivableId, BigDecimal returnedAmount, UUID salesReturnRef) {

	public AdjustReceivableForReturnCommand {
		Objects.requireNonNull(receivableId, "receivableId is required");
		Objects.requireNonNull(returnedAmount, "returnedAmount is required");
		Objects.requireNonNull(salesReturnRef, "salesReturnRef is required");
	}
}
