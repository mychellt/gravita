package br.gravita.core.ports.inbound.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * @param originalReceivableIds the overdue titles being replaced
 * @param newInstallmentPlan    the agreed plan, in installment order
 */
public record RenegotiateTitleCommand(List<UUID> originalReceivableIds, List<Installment> newInstallmentPlan) {

	public RenegotiateTitleCommand {
		Objects.requireNonNull(originalReceivableIds, "originalReceivableIds is required");
		Objects.requireNonNull(newInstallmentPlan, "newInstallmentPlan is required");
		originalReceivableIds = List.copyOf(originalReceivableIds);
		newInstallmentPlan = List.copyOf(newInstallmentPlan);
	}

	public record Installment(LocalDate dueDate, BigDecimal amount) {

		public Installment {
			Objects.requireNonNull(dueDate, "dueDate is required");
			Objects.requireNonNull(amount, "amount is required");
		}
	}
}
