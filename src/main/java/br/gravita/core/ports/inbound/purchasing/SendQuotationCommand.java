package br.gravita.core.ports.inbound.purchasing;

import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.purchasing.PurchaseRequestId;
import java.util.List;
import java.util.Objects;

public record SendQuotationCommand(
		PurchaseRequestId requestId,
		List<SupplierId> suppliers) {

	public SendQuotationCommand {
		Objects.requireNonNull(requestId, "requestId is required");
		suppliers = suppliers == null ? List.of() : List.copyOf(suppliers);
	}
}
