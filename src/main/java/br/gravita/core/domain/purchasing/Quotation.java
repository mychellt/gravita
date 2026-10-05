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

@Getter
public final class Quotation {

	private final QuotationId id;
	private final PurchaseRequestId requestId;
	private final List<QuotationItem> items;
	private final List<SupplierId> suppliers;
	private final List<QuotationResponse> responses;

	public Quotation(final QuotationId id, final PurchaseRequestId requestId, final List<QuotationItem> items,
			final List<SupplierId> suppliers, final List<QuotationResponse> responses) {
		this.id = Objects.requireNonNull(id, "QuotationId is required");
		this.requestId = Objects.requireNonNull(requestId, "requestId is required");
		this.items = requireNonEmptyItems(items);
		this.suppliers = requireNonEmptySuppliers(suppliers);
		this.responses = responses == null ? List.of() : List.copyOf(responses);
	}

	public static Quotation send(final QuotationId id, final PurchaseRequestId requestId, final List<QuotationItem> items,
			final List<SupplierId> suppliers) {
		return new Quotation(id, requestId, items, suppliers, List.of());
	}

	public static Quotation of(final QuotationId id, final PurchaseRequestId requestId, final List<QuotationItem> items,
			final List<SupplierId> suppliers, final List<QuotationResponse> responses) {
		return new Quotation(id, requestId, items, suppliers, responses);
	}

	public Quotation registerResponse(final SupplierId supplierId, final List<QuotationItemPrice> itemPrices, final LocalDate deadline) {
		requireSupplierWasSentTheQuotation(supplierId);
		final QuotationResponse response = new QuotationResponse(supplierId, itemPrices, deadline);
		requireItemPricesCoverEveryItem(response.itemPrices());

		final List<QuotationResponse> updatedResponses = responses.stream()
				.filter(existing -> !existing.supplierId().equals(supplierId))
				.collect(Collectors.toCollection(ArrayList::new));
		updatedResponses.add(response);

		return new Quotation(id, requestId, items, suppliers, updatedResponses);
	}

	private void requireSupplierWasSentTheQuotation(final SupplierId supplierId) {
		Objects.requireNonNull(supplierId, "supplierId is required");
		if (!suppliers.contains(supplierId)) {
			throw new BusinessRuleException(
					"Supplier " + supplierId.value() + " was not sent this quotation");
		}
	}

	private void requireItemPricesCoverEveryItem(final List<QuotationItemPrice> itemPrices) {
		final Set<UUID> requiredProductIds = items.stream().map(QuotationItem::productId).collect(Collectors.toSet());
		final Set<UUID> pricedProductIds = itemPrices.stream().map(QuotationItemPrice::productId).collect(Collectors.toSet());
		if (!pricedProductIds.equals(requiredProductIds)) {
			throw new BusinessRuleException(
					"itemPrices must cover every item in the quotation: expected " + requiredProductIds
							+ ", got " + pricedProductIds);
		}
	}

	private static List<QuotationItem> requireNonEmptyItems(final List<QuotationItem> items) {
		final List<QuotationItem> copy = items == null ? List.of() : List.copyOf(items);
		if (copy.isEmpty()) {
			throw new BusinessRuleException("A quotation must carry at least one item");
		}
		return copy;
	}

	private static List<SupplierId> requireNonEmptySuppliers(final List<SupplierId> suppliers) {
		final List<SupplierId> copy = suppliers == null ? List.of() : List.copyOf(suppliers);
		if (copy.isEmpty()) {
			throw new BusinessRuleException("A quotation must be sent to at least one supplier");
		}
		return copy;
	}
}
