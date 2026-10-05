package br.gravita.core.domain.tax;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Getter
public final class CashMovement {

	private final CashMovementId id;
	private final PosSessionId sessionId;
	private final CashMovementType type;
	private final BigDecimal amount;
	private final String justification;
	private final Instant timestamp;

	public CashMovement(final CashMovementId id, final PosSessionId sessionId, final CashMovementType type, final BigDecimal amount,
			final String justification, final Instant timestamp) {
		this.id = Objects.requireNonNull(id, "id is required");
		this.sessionId = Objects.requireNonNull(sessionId, "sessionId is required");
		this.type = Objects.requireNonNull(type, "type is required");
		this.amount = requirePositive(amount);
		this.justification = requireNonBlank(justification);
		this.timestamp = Objects.requireNonNull(timestamp, "timestamp is required");
	}

	public static CashMovement of(final CashMovementId id, final PosSessionId sessionId, final CashMovementType type,
			final BigDecimal amount, final String justification, final Instant timestamp) {
		return new CashMovement(id, sessionId, type, amount, justification, timestamp);
	}

	private static BigDecimal requirePositive(final BigDecimal amount) {
		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessRuleException("amount must be positive");
		}
		return amount;
	}

	private static String requireNonBlank(final String justification) {
		if (justification == null || justification.isBlank()) {
			throw new BusinessRuleException("justification is required");
		}
		return justification;
	}
}
