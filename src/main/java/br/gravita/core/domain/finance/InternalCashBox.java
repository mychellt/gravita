package br.gravita.core.domain.finance;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.util.Objects;
import lombok.Getter;

/**
 * The back office's physical cash box (§9.3), separate from any PDV register:
 * its balance is moved only by {@link CashMovement}s recorded against it, never
 * by a {@code PosSession}. Immutable — {@link #apply} returns the updated box.
 */
@Getter
public final class InternalCashBox {

	private final InternalCashBoxId id;
	private final BigDecimal balance;

	public InternalCashBox(InternalCashBoxId id, BigDecimal balance) {
		this.id = Objects.requireNonNull(id, "id is required");
		this.balance = Objects.requireNonNull(balance, "balance is required");
	}

	public static InternalCashBox of(InternalCashBoxId id, BigDecimal balance) {
		return new InternalCashBox(id, balance);
	}

	/** The box after {@code movement}: {@code FROM_BANK} adds to the balance, {@code TO_BANK} subtracts from it. */
	public InternalCashBox apply(CashMovement movement) {
		if (!id.equals(movement.getCashBoxId())) {
			throw new BusinessRuleException("Movement " + movement.getId().value()
					+ " belongs to another cash box: " + movement.getCashBoxId().value());
		}
		return new InternalCashBox(id, balance.add(movement.signedAmount()));
	}
}
