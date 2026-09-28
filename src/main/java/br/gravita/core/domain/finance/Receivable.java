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

	private Receivable(ReceivableId id, UUID customerId, ReceivableOrigin origin, BigDecimal amount,
			LocalDate dueDate, Integer installments, ReceivableStatus status) {
		this.id = Objects.requireNonNull(id, "id is required");
		this.customerId = Objects.requireNonNull(customerId, "customerId is required");
		this.origin = Objects.requireNonNull(origin, "origin is required");
		this.amount = requirePositive(amount);
		this.dueDate = Objects.requireNonNull(dueDate, "dueDate is required");
		this.installments = requireValidInstallments(installments);
		this.status = Objects.requireNonNull(status, "status is required");
	}

	public static Receivable createManual(ReceivableId id, UUID customerId, BigDecimal amount, LocalDate dueDate,
			Integer installments) {
		return new Receivable(id, customerId, ReceivableOrigin.MANUAL, amount, dueDate, installments,
				ReceivableStatus.OPEN);
	}

	public static Receivable of(ReceivableId id, UUID customerId, ReceivableOrigin origin, BigDecimal amount,
			LocalDate dueDate, Integer installments, ReceivableStatus status) {
		return new Receivable(id, customerId, origin, amount, dueDate, installments, status);
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
