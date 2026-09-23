package br.gravita.core.ports.inbound.purchasing;

import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.purchasing.QuotationId;
import br.gravita.core.domain.purchasing.QuotationItemPrice;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record RegisterQuotationResponseCommand(
		QuotationId quotationId,
		SupplierId supplierId,
		List<QuotationItemPrice> itemPrices,
		LocalDate deadline) {

	public RegisterQuotationResponseCommand {
		Objects.requireNonNull(quotationId, "quotationId is required");
		Objects.requireNonNull(supplierId, "supplierId is required");
		itemPrices = itemPrices == null ? List.of() : List.copyOf(itemPrices);
	}
}
