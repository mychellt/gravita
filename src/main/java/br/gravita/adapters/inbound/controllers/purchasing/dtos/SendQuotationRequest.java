package br.gravita.adapters.inbound.controllers.purchasing.dtos;

import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.purchasing.PurchaseRequestId;
import br.gravita.core.ports.inbound.purchasing.SendQuotationCommand;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record SendQuotationRequest(@NotEmpty List<UUID> suppliers) {

	public SendQuotationCommand toCommand(UUID requestId) {
		return new SendQuotationCommand(
				PurchaseRequestId.of(requestId),
				suppliers.stream().map(SupplierId::of).toList());
	}
}
