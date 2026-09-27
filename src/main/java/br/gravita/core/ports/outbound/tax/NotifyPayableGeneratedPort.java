package br.gravita.core.ports.outbound.tax;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * UC-M2-10's own outbound port into {@code finance} - named for the calling
 * module (tax), the same convention as purchasing's own {@code GeneratePayableFromReceiptPort}.
 * Kept independent from it on purpose (see {@link NotifyStockEntryPort}).
 * finance/M8 hasn't shipped a real payable-generation use case yet (GRA-14),
 * so - mirroring purchasing's own no-op stub until M8 lands - this is expected
 * to be backed by a stub adapter for now.
 */
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
