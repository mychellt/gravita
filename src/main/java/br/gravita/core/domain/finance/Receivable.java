package br.gravita.core.domain.finance;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Getter
public final class Receivable {

	private final ReceivableId id;
	private final UUID customerId;
	private final ReceivableOrigin origin;
	private final BigDecimal amount;
	private final LocalDate dueDate;
	private final Integer installments;
	private final ReceivableStatus status;
	private final UUID originDocumentRef;
	private final Integer installmentNumber;
	private final LedgerScope scope;

	private Receivable(ReceivableId id, UUID customerId, ReceivableOrigin origin, BigDecimal amount,
			LocalDate dueDate, Integer installments, ReceivableStatus status, UUID originDocumentRef,
			Integer installmentNumber, LedgerScope scope) {
		this.id = Objects.requireNonNull(id, "id is required");
		this.customerId = Objects.requireNonNull(customerId, "customerId is required");
		this.origin = Objects.requireNonNull(origin, "origin is required");
		this.amount = requirePositive(amount);
		this.dueDate = Objects.requireNonNull(dueDate, "dueDate is required");
		this.installments = requireValidInstallments(installments);
		this.status = Objects.requireNonNull(status, "status is required");
		this.originDocumentRef = originDocumentRef;
		this.installmentNumber = installmentNumber;
		this.scope = scope == null ? LedgerScope.NONE : scope;
	}

	public static Receivable createManual(ReceivableId id, UUID customerId, BigDecimal amount, LocalDate dueDate,
			Integer installments) {
		return new Receivable(id, customerId, ReceivableOrigin.MANUAL, amount, dueDate, installments,
				ReceivableStatus.OPEN, null, null, LedgerScope.NONE);
	}

	/**
	 * One installment of an invoice's payment terms. {@code installments} is the
	 * total number of installments of the invoice, {@code installmentNumber}
	 * (1-based) is this title's position among them.
	 */
	public static Receivable createFromInvoicing(ReceivableId id, UUID customerId, UUID originDocumentRef,
			BigDecimal amount, LocalDate dueDate, int installmentNumber, int installments) {
		Objects.requireNonNull(originDocumentRef, "originDocumentRef is required");
		if (installmentNumber < 1 || installmentNumber > installments) {
			throw new BusinessRuleException(
					"installmentNumber must be between 1 and " + installments + ": " + installmentNumber);
		}
		return new Receivable(id, customerId, ReceivableOrigin.INVOICING, amount, dueDate, installments,
				ReceivableStatus.OPEN, originDocumentRef, installmentNumber, LedgerScope.NONE);
	}

	/**
	 * One installment of the plan agreed in a {@link Renegotiation}.
	 * {@code installments} is the plan's total number of installments,
	 * {@code installmentNumber} (1-based) this title's position in it.
	 */
	public static Receivable createFromRenegotiation(ReceivableId id, UUID customerId, BigDecimal amount,
			LocalDate dueDate, int installmentNumber, int installments) {
		if (installmentNumber < 1 || installmentNumber > installments) {
			throw new BusinessRuleException(
					"installmentNumber must be between 1 and " + installments + ": " + installmentNumber);
		}
		return new Receivable(id, customerId, ReceivableOrigin.RENEGOTIATION, amount, dueDate, installments,
				ReceivableStatus.OPEN, null, installmentNumber, LedgerScope.NONE);
	}

	public static Receivable of(ReceivableId id, UUID customerId, ReceivableOrigin origin, BigDecimal amount,
			LocalDate dueDate, Integer installments, ReceivableStatus status, UUID originDocumentRef,
			Integer installmentNumber) {
		return of(id, customerId, origin, amount, dueDate, installments, status, originDocumentRef,
				installmentNumber, LedgerScope.NONE);
	}

	public static Receivable of(ReceivableId id, UUID customerId, ReceivableOrigin origin, BigDecimal amount,
			LocalDate dueDate, Integer installments, ReceivableStatus status, UUID originDocumentRef,
			Integer installmentNumber, LedgerScope scope) {
		return new Receivable(id, customerId, origin, amount, dueDate, installments, status, originDocumentRef,
				installmentNumber, scope);
	}

	/** The same title placed in {@code scope} (company, branch and bank account). */
	public Receivable inScope(LedgerScope scope) {
		return new Receivable(id, customerId, origin, amount, dueDate, installments, status, originDocumentRef,
				installmentNumber, scope);
	}

	/** A boleto (or any charge) can only be issued against a title that is still {@code OPEN}. */
	public void requireOpen() {
		if (status != ReceivableStatus.OPEN) {
			throw new BusinessRuleException("Receivable " + id.value() + " is not OPEN: " + status);
		}
	}

	/** Still owed by the customer: {@code OPEN} or {@code PARTIALLY_SETTLED}. */
	public boolean isOutstanding() {
		return status == ReceivableStatus.OPEN || status == ReceivableStatus.PARTIALLY_SETTLED;
	}

	/** Outstanding and past its due date as of {@code today}. */
	public boolean isOverdue(LocalDate today) {
		return isOutstanding() && dueDate.isBefore(today);
	}

	/**
	 * Replaces this title by a renegotiated installment plan. Only an overdue
	 * title (see {@link #isOverdue}) can be renegotiated; it leaves the aging
	 * and every open-balance view as {@code RENEGOTIATED}.
	 */
	public Receivable renegotiate(LocalDate today) {
		if (!isOverdue(today)) {
			throw new BusinessRuleException("Receivable " + id.value() + " is not overdue: " + status + ", due "
					+ dueDate);
		}
		return new Receivable(id, customerId, origin, amount, dueDate, installments, ReceivableStatus.RENEGOTIATED,
				originDocumentRef, installmentNumber, scope);
	}

	/**
	 * Full settlement (e.g. a confirmed PIX payment). Only a title that is still
	 * {@code OPEN} or {@code PARTIALLY_SETTLED} can be settled.
	 */
	public Receivable settle() {
		if (status != ReceivableStatus.OPEN && status != ReceivableStatus.PARTIALLY_SETTLED) {
			throw new BusinessRuleException("Receivable " + id.value() + " cannot be settled: " + status);
		}
		return new Receivable(id, customerId, origin, amount, dueDate, installments, ReceivableStatus.SETTLED,
				originDocumentRef, installmentNumber, scope);
	}

	/**
	 * Applies the total credited to this title so far (its settlements'
	 * principal plus discounts): {@code SETTLED} once that covers the title's
	 * amount, {@code PARTIALLY_SETTLED} until then. Only a title that is still
	 * {@code OPEN} or {@code PARTIALLY_SETTLED} can take a payment.
	 */
	public Receivable applyCreditedTotal(BigDecimal totalCredited) {
		if (totalCredited == null || totalCredited.signum() <= 0) {
			throw new BusinessRuleException("totalCredited must be positive: " + totalCredited);
		}
		if (totalCredited.compareTo(amount) >= 0) {
			return settle();
		}
		if (status != ReceivableStatus.OPEN && status != ReceivableStatus.PARTIALLY_SETTLED) {
			throw new BusinessRuleException("Receivable " + id.value() + " cannot be settled: " + status);
		}
		return new Receivable(id, customerId, origin, amount, dueDate, installments,
				ReceivableStatus.PARTIALLY_SETTLED, originDocumentRef, installmentNumber, scope);
	}

	/**
	 * Reverses {@code returnedAmount} of what is still owed on this title for a
	 * customer return. A return covering the whole remaining balance cancels the
	 * title; a smaller one lowers its amount by the returned value and leaves the
	 * status untouched ({@code OPEN} or {@code PARTIALLY_SETTLED}). Only the
	 * unsettled portion is adjustable: what {@code settlements} already credited
	 * is never clawed back, and a return above the remaining balance is rejected.
	 */
	public Receivable adjustForReturn(BigDecimal returnedAmount, Collection<Settlement> settlements) {
		if (returnedAmount == null || returnedAmount.signum() <= 0) {
			throw new BusinessRuleException("returnedAmount must be positive: " + returnedAmount);
		}
		if (!isOutstanding()) {
			throw new BusinessRuleException("Receivable " + id.value() + " cannot be adjusted: " + status);
		}
		BigDecimal remaining = remainingBalance(settlements);
		int comparison = returnedAmount.compareTo(remaining);
		if (comparison > 0) {
			throw new BusinessRuleException("Returned amount of " + returnedAmount
					+ " exceeds the open balance of " + remaining);
		}
		if (comparison == 0) {
			return new Receivable(id, customerId, origin, amount, dueDate, installments, ReceivableStatus.CANCELLED,
					originDocumentRef, installmentNumber, scope);
		}
		return new Receivable(id, customerId, origin, amount.subtract(returnedAmount), dueDate, installments, status,
				originDocumentRef, installmentNumber, scope);
	}

	/** What is still to be credited to this title once {@code settlements} (its baixas so far) are applied. */
	public BigDecimal remainingBalance(Collection<Settlement> settlements) {
		return settlements.stream().map(Settlement::creditedAmount).reduce(amount, BigDecimal::subtract);
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

	private static Integer requireValidInstallments(Integer installments) {
		if (installments != null && installments < 1) {
			throw new BusinessRuleException("installments must be at least 1: " + installments);
		}
		return installments;
	}
}
