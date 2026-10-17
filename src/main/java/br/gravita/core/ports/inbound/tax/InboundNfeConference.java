package br.gravita.core.ports.inbound.tax;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * What the conference screen needs about an inbound NFe: whether its supplier is registered and, per item, the
 * imported snapshot next to the catalog Product it resolves to (if any) and how its price compares with the last
 * purchase.
 */
public record InboundNfeConference(UUID inboundNfeId, boolean supplierResolved, UUID supplierId, List<Item> items) {

	public InboundNfeConference {
		items = List.copyOf(items);
	}

	/** {@code productId} and the price fields are {@code null} while the item has no link. */
	public record Item(int index, String supplierProductCode, String description, String ncm, String unit, BigDecimal quantity,
			BigDecimal unitValue, UUID productId, String productLabel, BigDecimal lastPurchaseUnitValue,
			Instant lastPurchaseAt, BigDecimal priceDelta) {

		public boolean resolved() {
			return productId != null;
		}
	}
}
