package br.gravita.core.domain.finance;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import lombok.Getter;

/**
 * A transfer between the back office's {@link InternalCashBox} and the bank.
 * Unrelated to the PDV's sangria/suprimento, which belong to a {@code PosSession}
 * (M3). Immutable.
 */
@Getter
public final class CashMovement {

	private final CashMovementId id;
	private final InternalCashBoxId cashBoxId;
	private final CashMovementDirection direction;
	private final BigDecimal amount;
	private final String justification;
	private final Instant timestamp;

	public CashMovement(CashMovementId id, InternalCashBoxId cashBoxId, CashMovementDirection direction,
			BigDecimal amount, String justification, Instant timestamp) {
		this.id = Objects.requireNonNull(id, "id is required");
		this.cashBoxId = Objects.requireNonNull(cashBoxId, "cashBoxId is required");
		this.direction = Objects.requireNonNull(direction, "direction is required");
		this.amount = requirePositive(amount);
		this.justification = requireNonBlank(justification);
		this.timestamp = Objects.requireNonNull(timestamp, "timestamp is required");
	}

	public static CashMovement of(CashMovementId id, InternalCashBoxId cashBoxId, CashMovementDirection direction,
			BigDecimal amount, String justification, Instant timestamp) {
		return new CashMovement(id, cashBoxId, direction, amount, justification, timestamp);
	}

	/** The change this movement makes to the cash box balance: negative when cash goes to the bank. */
	public BigDecimal signedAmount() {
		return direction == CashMovementDirection.FROM_BANK ? amount : amount.negate();
	}

	private static BigDecimal requirePositive(BigDecimal amount) {
		if (amount == null) {
			throw new BusinessRuleException("amount is required");
		}
		if (amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessRuleException("amount must be positive: " + amount);
		}
		return amount;
	}

	private static String requireNonBlank(String justification) {
		if (justification == null || justification.isBlank()) {
			throw new BusinessRuleException("justification is required");
		}
		return justification;
	}
}
