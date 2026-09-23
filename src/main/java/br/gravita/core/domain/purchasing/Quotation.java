package br.gravita.core.domain.purchasing;

import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.shared.BusinessRuleException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;

/**
 * Quotation aggregate (UC-M6-02/UC-M6-03). Created by {@link #send} with the
 * originating {@link PurchaseRequest}'s item list and the suppliers it was
 * sent to; starts with no responses. Each supplier's reply is recorded via
 * {@link #registerResponse}, which the buyer later compares side by side to
 * pick a winner before creating the {@code PurchaseOrder} (UC-M6-04) - that
 * comparison is a query over this aggregate's responses, not a separate use
 * case.
 */
@Getter
public final class Quotation {

	private final QuotationId id;
	private final PurchaseRequestId requestId;
	private final List<QuotationItem> items;
	private final List<SupplierId> suppliers;
	private final List<QuotationResponse> responses;

	private Quotation(QuotationId id, PurchaseRequestId requestId, List<QuotationItem> items,
			List<SupplierId> suppliers, List<QuotationResponse> responses) {
		this.id = Objects.requireNonNull(id, "QuotationId is required");
		this.requestId = Objects.requireNonNull(requestId, "requestId is required");
		this.items = requireNonEmptyItems(items);
		this.suppliers = requireNonEmptySuppliers(suppliers);
		this.responses = responses == null ? List.of() : List.copyOf(responses);
	}

	public static Quotation send(QuotationId id, PurchaseRequestId requestId, List<QuotationItem> items,
			List<SupplierId> suppliers) {
		return new Quotation(id, requestId, items, suppliers, List.of());
	}

	public static Quotation of(QuotationId id, PurchaseRequestId requestId, List<QuotationItem> items,
			List<SupplierId> suppliers, List<QuotationResponse> responses) {
		return new Quotation(id, requestId, items, suppliers, responses);
	}

	/**
	 * UC-M6-03: records a supplier's reply. The supplier must be one of the
	 * ones the quotation was originally sent to, and the reply must price
	 * every item on the quotation. A prior response from the same supplier is
	 * replaced rather than duplicated.
	 */
	public Quotation registerResponse(SupplierId supplierId, List<QuotationItemPrice> itemPrices, LocalDate deadline) {
		requireSupplierWasSentTheQuotation(supplierId);
		QuotationResponse response = new QuotationResponse(supplierId, itemPrices, deadline);
		requireItemPricesCoverEveryItem(response.itemPrices());

		List<QuotationResponse> updatedResponses = responses.stream()
				.filter(existing -> !existing.supplierId().equals(supplierId))
				.collect(Collectors.toCollection(ArrayList::new));
		updatedResponses.add(response);

		return new Quotation(id, requestId, items, suppliers, updatedResponses);
	}

	private void requireSupplierWasSentTheQuotation(SupplierId supplierId) {
		Objects.requireNonNull(supplierId, "supplierId is required");
		if (!suppliers.contains(supplierId)) {
			throw new BusinessRuleException(
					"Supplier " + supplierId.value() + " was not sent this quotation");
		}
	}

	private void requireItemPricesCoverEveryItem(List<QuotationItemPrice> itemPrices) {
		Set<UUID> requiredProductIds = items.stream().map(QuotationItem::productId).collect(Collectors.toSet());
		Set<UUID> pricedProductIds = itemPrices.stream().map(QuotationItemPrice::productId).collect(Collectors.toSet());
		if (!pricedProductIds.equals(requiredProductIds)) {
			throw new BusinessRuleException(
					"itemPrices must cover every item in the quotation: expected " + requiredProductIds
							+ ", got " + pricedProductIds);
		}
	}

	private static List<QuotationItem> requireNonEmptyItems(List<QuotationItem> items) {
		List<QuotationItem> copy = items == null ? List.of() : List.copyOf(items);
		if (copy.isEmpty()) {
			throw new BusinessRuleException("A quotation must carry at least one item");
		}
		return copy;
	}

	private static List<SupplierId> requireNonEmptySuppliers(List<SupplierId> suppliers) {
		List<SupplierId> copy = suppliers == null ? List.of() : List.copyOf(suppliers);
		if (copy.isEmpty()) {
			throw new BusinessRuleException("A quotation must be sent to at least one supplier");
		}
		return copy;
	}
}
