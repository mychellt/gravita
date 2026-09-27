package br.gravita.core.ports.outbound.sales;

import br.gravita.core.domain.sales.FiscalDocumentRef;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Delegates fiscal issuance to {@code tax} for one nature-group (products or
 * services) of a {@link br.gravita.core.domain.sales.SalesOrder}'s items
 * (UC-M7-06). Products are issued as an NFe; NFSe (M4) is not implemented
 * yet, so {@link #issueForServices} fails fast rather than blocking this
 * ticket on M4.
 */
public interface IssueFiscalDocumentPort {

	FiscalDocumentRef issueForProducts(IssueFiscalDocumentCommand command);

	FiscalDocumentRef issueForServices(IssueFiscalDocumentCommand command);

	/**
	 * Issues a return NFe (UC-M7-07), referencing the access key of the
	 * {@code originalDocument} previously issued by {@link #issueForProducts}
	 * for the same order.
	 */
	FiscalDocumentRef issueForReturn(IssueReturnFiscalDocumentCommand command);

	record IssueFiscalDocumentCommand(UUID orderId, UUID customerId, List<Item> items) {

		public IssueFiscalDocumentCommand {
			Objects.requireNonNull(orderId, "orderId is required");
			Objects.requireNonNull(customerId, "customerId is required");
			if (items == null || items.isEmpty()) {
				throw new IllegalArgumentException("items must not be empty");
			}
			items = List.copyOf(items);
		}

		public record Item(UUID productOrServiceId, String description, BigDecimal quantity, BigDecimal unitPrice,
				BigDecimal discount) {
		}
	}

	record IssueReturnFiscalDocumentCommand(UUID orderId, UUID customerId, FiscalDocumentRef originalDocument,
			List<IssueFiscalDocumentCommand.Item> items) {

		public IssueReturnFiscalDocumentCommand {
			Objects.requireNonNull(orderId, "orderId is required");
			Objects.requireNonNull(customerId, "customerId is required");
			Objects.requireNonNull(originalDocument, "originalDocument is required");
			if (items == null || items.isEmpty()) {
				throw new IllegalArgumentException("items must not be empty");
			}
			items = List.copyOf(items);
		}
	}
}
