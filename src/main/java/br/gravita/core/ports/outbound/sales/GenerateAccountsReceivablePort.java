package br.gravita.core.ports.outbound.sales;

import br.gravita.core.domain.sales.FiscalDocumentRef;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Requests an accounts-receivable title in {@code finance} for a fiscal
 * document issued by {@link IssueFiscalDocumentPort} (UC-M7-06). Mirrors
 * {@code tax}'s {@code NotifyPayableGeneratedPort}: {@code finance} (M8) is
 * not implemented yet, so this is currently a fire-and-forget notification
 * rather than a call whose result feeds back into the sales invoice.
 */
public interface GenerateAccountsReceivablePort {

	void generate(GenerateAccountsReceivableCommand command);

	record GenerateAccountsReceivableCommand(UUID customerId, FiscalDocumentRef originDocument, BigDecimal amount,
			LocalDate dueDate) {

		public GenerateAccountsReceivableCommand {
			Objects.requireNonNull(customerId, "customerId is required");
			Objects.requireNonNull(originDocument, "originDocument is required");
			Objects.requireNonNull(amount, "amount is required");
			Objects.requireNonNull(dueDate, "dueDate is required");
		}
	}
}
