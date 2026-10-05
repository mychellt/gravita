package br.gravita.core.domain.finance;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import lombok.Getter;

/**
 * One entry of an imported OFX/CSV bank statement. {@code amount} is signed the
 * way the bank shows it: positive for a credit (money in), negative for a debit
 * (money out). {@code lineNumber} says where the entry is in the statement (the
 * file line for a CSV, the n-th transaction for an OFX). Once reconciled it
 * points at the {@link Settlement} or the {@link CashMovement} it was matched
 * to; a line with neither still needs a manual review. Immutable.
 */
@Getter
public final class BankStatementLine {

	private final int lineNumber;
	private final LocalDate postedOn;
	private final BigDecimal amount;
	private final String description;
	private final String reference;
	private final SettlementId settlementId;
	private final CashMovementId cashMovementId;

	private BankStatementLine(final int lineNumber, final LocalDate postedOn, final BigDecimal amount, final String description,
			final String reference, final SettlementId settlementId, final CashMovementId cashMovementId) {
		if (lineNumber < 1) {
			throw new BusinessRuleException("lineNumber must be positive: " + lineNumber);
		}
		this.lineNumber = lineNumber;
		this.postedOn = Objects.requireNonNull(postedOn, "postedOn is required");
		this.amount = Objects.requireNonNull(amount, "amount is required");
		this.description = description == null ? "" : description;
		this.reference = reference;
		this.settlementId = settlementId;
		this.cashMovementId = cashMovementId;
	}

	/** A line as read from the statement, not matched to anything yet; {@code reference} is the bank's id for the entry, if it has one. */
	public static BankStatementLine unmatched(final int lineNumber, final LocalDate postedOn, final BigDecimal amount,
			final String description, final String reference) {
		return new BankStatementLine(lineNumber, postedOn, amount, description, reference, null, null);
	}

	public BankStatementLine matchedTo(final SettlementId settlementId) {
		Objects.requireNonNull(settlementId, "settlementId is required");
		return new BankStatementLine(lineNumber, postedOn, amount, description, reference, settlementId, null);
	}

	public BankStatementLine matchedTo(final CashMovementId cashMovementId) {
		Objects.requireNonNull(cashMovementId, "cashMovementId is required");
		return new BankStatementLine(lineNumber, postedOn, amount, description, reference, null, cashMovementId);
	}

	public boolean isMatched() {
		return settlementId != null || cashMovementId != null;
	}

	/** Whether money came into the account. */
	public boolean isCredit() {
		return amount.signum() > 0;
	}
}
