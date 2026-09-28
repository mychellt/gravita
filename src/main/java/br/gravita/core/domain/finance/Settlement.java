package br.gravita.core.domain.finance;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import lombok.Getter;

/**
 * A baixa: a payment applied to one {@link Receivable}. {@code amount} is the
 * principal credited against the title; {@code interest}, {@code fine} and
 * {@code surcharge} are what was paid on top of it and {@code discount} is a
 * reduction granted on it. Immutable.
 */
@Getter
public final class Settlement {

	private final SettlementId id;
	private final ReceivableId receivableId;
	private final BigDecimal amount;
	private final BigDecimal interest;
	private final BigDecimal fine;
	private final BigDecimal discount;
	private final BigDecimal surcharge;
	private final SettlementMethod method;
	private final Instant timestamp;

	private Settlement(SettlementId id, ReceivableId receivableId, BigDecimal amount, BigDecimal interest,
			BigDecimal fine, BigDecimal discount, BigDecimal surcharge, SettlementMethod method, Instant timestamp) {
		this.id = Objects.requireNonNull(id, "id is required");
		this.receivableId = Objects.requireNonNull(receivableId, "receivableId is required");
		this.amount = requirePositive("amount", amount);
		this.interest = requireNotNegative("interest", interest);
		this.fine = requireNotNegative("fine", fine);
		this.discount = requireNotNegative("discount", discount);
		this.surcharge = requireNotNegative("surcharge", surcharge);
		this.method = Objects.requireNonNull(method, "method is required");
		this.timestamp = Objects.requireNonNull(timestamp, "timestamp is required");
	}

	/** A baixa created from a line of the bank's CNAB return file; {@code timestamp} is when the bank says it was paid. */
	public static Settlement automaticCnab(SettlementId id, ReceivableId receivableId, BigDecimal amount,
			BigDecimal interest, BigDecimal fine, BigDecimal discount, BigDecimal surcharge, Instant timestamp) {
		return new Settlement(id, receivableId, amount, interest, fine, discount, surcharge,
				SettlementMethod.AUTOMATIC_CNAB, timestamp);
	}

	/** A baixa entered by a user (e.g. a cash payment); {@code timestamp} is when it was recorded. */
	public static Settlement manual(SettlementId id, ReceivableId receivableId, BigDecimal amount,
			BigDecimal interest, BigDecimal fine, BigDecimal discount, BigDecimal surcharge, Instant timestamp) {
		return new Settlement(id, receivableId, amount, interest, fine, discount, surcharge,
				SettlementMethod.MANUAL, timestamp);
	}

	public static Settlement of(SettlementId id, ReceivableId receivableId, BigDecimal amount, BigDecimal interest,
			BigDecimal fine, BigDecimal discount, BigDecimal surcharge, SettlementMethod method, Instant timestamp) {
		return new Settlement(id, receivableId, amount, interest, fine, discount, surcharge, method, timestamp);
	}

	/** What this baixa clears of the title: the principal paid plus the discount granted. */
	public BigDecimal creditedAmount() {
		return amount.add(discount);
	}

	/**
	 * Whether {@code other} records the same bank payment - same title, method,
	 * principal and payment time - which is how a return file that is imported
	 * twice is recognised.
	 */
	public boolean isSamePaymentAs(Settlement other) {
		return receivableId.equals(other.receivableId) && method == other.method
				&& amount.compareTo(other.amount) == 0 && timestamp.equals(other.timestamp);
	}

	private static BigDecimal requirePositive(String field, BigDecimal value) {
		if (value == null) {
			throw new BusinessRuleException(field + " is required");
		}
		if (value.signum() <= 0) {
			throw new BusinessRuleException(field + " must be positive: " + value);
		}
		return value;
	}

	private static BigDecimal requireNotNegative(String field, BigDecimal value) {
		if (value == null) {
			return BigDecimal.ZERO;
		}
		if (value.signum() < 0) {
			throw new BusinessRuleException(field + " must not be negative: " + value);
		}
		return value;
	}
}
