package br.gravita.core.ports.inbound.purchasing;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.purchasing.PurchaseOrderId;
import br.gravita.core.domain.purchasing.PurchaseReceiptId;
import java.util.Objects;

public record ImportSupplierNfeAtReceivingCommand(PurchaseOrderId orderId, PurchaseReceiptId receiptId,
		CompanyId companyId, byte[] xmlFile) {

	public ImportSupplierNfeAtReceivingCommand {
		Objects.requireNonNull(orderId, "orderId is required");
		Objects.requireNonNull(receiptId, "receiptId is required");
		Objects.requireNonNull(companyId, "companyId is required");
		Objects.requireNonNull(xmlFile, "xmlFile is required");
	}
}
