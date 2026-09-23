package br.gravita.core.domain.purchasing;

import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.shared.BusinessRuleException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * One supplier's reply to a {@link Quotation} (UC-M6-03): their price per
 * item and delivery deadline. A quotation holds at most one response per
 * supplier - re-submission replaces the prior one, see
 * {@link Quotation#registerResponse}.
 */
public record QuotationResponse(SupplierId supplierId, List<QuotationItemPrice> itemPrices, LocalDate deadline) {

	public QuotationResponse {
		Objects.requireNonNull(supplierId, "supplierId is required");
		Objects.requireNonNull(deadline, "deadline is required");
		itemPrices = itemPrices == null ? List.of() : List.copyOf(itemPrices);
		if (itemPrices.isEmpty()) {
			throw new BusinessRuleException("A quotation response must price at least one item");
		}
	}
}
