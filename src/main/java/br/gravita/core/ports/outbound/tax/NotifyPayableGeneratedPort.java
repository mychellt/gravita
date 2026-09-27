package br.gravita.core.ports.outbound.tax;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public interface NotifyPayableGeneratedPort {
	void notifyGenerated(NotifyPayableGeneratedCommand command);

	record NotifyPayableGeneratedCommand(UUID sourceInboundNfeId, String supplierDocument,
			List<Installment> installments) {
		public NotifyPayableGeneratedCommand {
			Objects.requireNonNull(sourceInboundNfeId, "sourceInboundNfeId is required");
			Objects.requireNonNull(supplierDocument, "supplierDocument is required");
			installments = installments == null ? List.of() : List.copyOf(installments);
		}

		public record Installment(BigDecimal amount, LocalDate dueDate) {
			public Installment {
				Objects.requireNonNull(amount, "amount is required");
				Objects.requireNonNull(dueDate, "dueDate is required");
			}
		}
	}
}
