package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.ports.inbound.tax.InboundNfeConference;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InboundNfeConferenceResponse(UUID inboundNfeId, boolean supplierResolved, UUID supplierId, List<Item> items) {

	public record Item(int index, String supplierProductCode, String description, String ncm, String unit, BigDecimal quantity,
			BigDecimal unitValue, boolean resolved, UUID productId, String productLabel, BigDecimal lastPurchaseUnitValue,
			Instant lastPurchaseAt, BigDecimal priceDelta) {
	}

	public static InboundNfeConferenceResponse from(final InboundNfeConference conference) {
		return new InboundNfeConferenceResponse(conference.inboundNfeId(), conference.supplierResolved(),
				conference.supplierId(), conference.items().stream().map(InboundNfeConferenceResponse::toItem).toList());
	}

	private static Item toItem(final InboundNfeConference.Item item) {
		return new Item(item.index(), item.supplierProductCode(), item.description(), item.ncm(), item.unit(),
				item.quantity(), item.unitValue(), item.resolved(), item.productId(), item.productLabel(),
				item.lastPurchaseUnitValue(), item.lastPurchaseAt(), item.priceDelta());
	}
}
