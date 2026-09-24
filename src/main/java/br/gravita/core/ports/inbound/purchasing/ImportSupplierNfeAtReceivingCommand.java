package br.gravita.core.ports.inbound.purchasing;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.purchasing.PurchaseOrderId;
import br.gravita.core.domain.purchasing.PurchaseReceiptId;
import java.util.Objects;

/**
 * {@code companyId} isn't in the UC-M6-07 spec's 3-field command list, but M2's
 * {@code ImportSupplierNfeXmlUseCase} (the delegated collaborator) requires
 * one and neither {@code PurchaseOrder} nor {@code Supplier} carry a company
 * reference yet - so it's taken as an explicit input here, the same way other
 * commands needing a {@code CompanyId} do (e.g. {@code ConfigureDocumentSeriesCommand}).
 */
public record ImportSupplierNfeAtReceivingCommand(PurchaseOrderId orderId, PurchaseReceiptId receiptId,
		CompanyId companyId, byte[] xmlFile) {

	public ImportSupplierNfeAtReceivingCommand {
		Objects.requireNonNull(orderId, "orderId is required");
		Objects.requireNonNull(receiptId, "receiptId is required");
		Objects.requireNonNull(companyId, "companyId is required");
		Objects.requireNonNull(xmlFile, "xmlFile is required");
	}
}
