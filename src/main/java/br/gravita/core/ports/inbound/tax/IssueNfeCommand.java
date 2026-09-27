package br.gravita.core.ports.inbound.tax;

import br.gravita.core.domain.masterdata.CompanyId;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * UC-M2-01. {@code originSalesOrderId} is preserved but not otherwise acted
 * on here (M7's {@code InvoiceSalesOrderUseCase} caller doesn't exist yet);
 * {@code priceTableId} is the recipient's linked price table, used for AC5's
 * max-discount check (optional - an ad-hoc recipient may have none).
 */
public record IssueNfeCommand(
		CompanyId issuerCompanyId,
		UUID originSalesOrderId,
		String naturezaOperacao,
		NfeRecipientInput recipient,
		List<NfeItemInput> items,
		BigDecimal freight,
		BigDecimal insurance,
		BigDecimal otherExpenses,
		NfeTransportInput transport,
		String referencedAccessKey,
		String additionalInfo,
		UUID priceTableId) {

	public IssueNfeCommand {
		Objects.requireNonNull(issuerCompanyId, "issuerCompanyId");
		Objects.requireNonNull(naturezaOperacao, "naturezaOperacao");
		Objects.requireNonNull(recipient, "recipient");
		items = items == null ? List.of() : List.copyOf(items);
	}
}
