package br.gravita.core.domain.finance;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.time.LocalDate;
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

	private Receivable(ReceivableId id, UUID customerId, ReceivableOrigin origin, BigDecimal amount,
			LocalDate dueDate, Integer installments, ReceivableStatus status, UUID originDocumentRef,
			Integer installmentNumber) {
		this.id = Objects.requireNonNull(id, "id is required");
		this.customerId = Objects.requireNonNull(customerId, "customerId is required");
		this.origin = Objects.requireNonNull(origin, "origin is required");
		this.amount = requirePositive(amount);
		this.dueDate = Objects.requireNonNull(dueDate, "dueDate is required");
		this.installments = requireValidInstallments(installments);
		this.status = Objects.requireNonNull(status, "status is required");
		this.originDocumentRef = originDocumentRef;
		this.installmentNumber = installmentNumber;
	}

	public static Receivable createManual(ReceivableId id, UUID customerId, BigDecimal amount, LocalDate dueDate,
			Integer installments) {
		return new Receivable(id, customerId, ReceivableOrigin.MANUAL, amount, dueDate, installments,
				ReceivableStatus.OPEN, null, null);
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
				ReceivableStatus.OPEN, originDocumentRef, installmentNumber);
	}

	public static Receivable of(ReceivableId id, UUID customerId, ReceivableOrigin origin, BigDecimal amount,
			LocalDate dueDate, Integer installments, ReceivableStatus status, UUID originDocumentRef,
			Integer installmentNumber) {
		return new Receivable(id, customerId, origin, amount, dueDate, installments, status, originDocumentRef,
				installmentNumber);
	}

	/** A boleto (or any charge) can only be issued against a title that is still {@code OPEN}. */
	public void requireOpen() {
		if (status != ReceivableStatus.OPEN) {
			throw new BusinessRuleException("Receivable " + id.value() + " is not OPEN: " + status);
		}
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
				originDocumentRef, installmentNumber);
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
