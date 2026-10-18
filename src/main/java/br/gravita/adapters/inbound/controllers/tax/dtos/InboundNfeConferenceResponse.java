package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.ports.inbound.tax.InboundNfeConference;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InboundNfeConferenceResponse(UUID inboundNfeId, String supplierDocument, String supplierName,
		boolean supplierResolved, UUID supplierId, List<Item> items) {

	public record Item(int index, String supplierProductCode, String description, String ncm, String unit, BigDecimal quantity,
			BigDecimal unitValue, boolean resolved, UUID productId, String productLabel, BigDecimal lastPurchaseUnitValue,
			Instant lastPurchaseAt, BigDecimal priceDelta, String barcode, String cest, UUID suggestedProductId,
			String suggestedProductLabel) {
	}

	public static InboundNfeConferenceResponse from(final InboundNfeConference conference) {
		return new InboundNfeConferenceResponse(conference.inboundNfeId(), conference.supplierDocument(),
				conference.supplierName(), conference.supplierResolved(), conference.supplierId(),
				conference.items().stream().map(InboundNfeConferenceResponse::toItem).toList());
	}

	private static Item toItem(final InboundNfeConference.Item item) {
		final InboundNfeConference.ProductSuggestion suggestion = item.suggestedProduct();
		return new Item(item.index(), item.supplierProductCode(), item.description(), item.ncm(), item.unit(),
				item.quantity(), item.unitValue(), item.resolved(), item.productId(), item.productLabel(),
				item.lastPurchaseUnitValue(), item.lastPurchaseAt(), item.priceDelta(), item.barcode(), item.cest(),
				suggestion == null ? null : suggestion.productId(), suggestion == null ? null : suggestion.productLabel());
	}
}
