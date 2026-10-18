package br.gravita.core.ports.inbound.tax;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * What the conference screen needs about an inbound NFe: whether its supplier is registered and, per item, the
 * imported snapshot next to the catalog Product it resolves to (if any) and how its price compares with the last
 * purchase. {@code supplierDocument}/{@code supplierName} are the NFe's own snapshot, so an unregistered supplier can
 * be registered from the screen.
 */
public record InboundNfeConference(UUID inboundNfeId, String supplierDocument, String supplierName, boolean supplierResolved,
		UUID supplierId, List<Item> items) {

	public InboundNfeConference {
		items = List.copyOf(items);
	}

	/**
	 * {@code productId} and the price fields are {@code null} while the item has no link. {@code suggestions} are the
	 * catalog products the item probably stands for, best first, offered only to unlinked items; none becomes a link by itself.
	 */
	public record Item(int index, String supplierProductCode, String description, String ncm, String unit, BigDecimal quantity,
			BigDecimal unitValue, UUID productId, String productLabel, BigDecimal lastPurchaseUnitValue,
			Instant lastPurchaseAt, BigDecimal priceDelta, String barcode, String cest, List<ProductSuggestion> suggestions) {

		public Item {
			suggestions = suggestions == null ? List.of() : List.copyOf(suggestions);
		}

		/** An item without a GTIN-based suggestion. */
		public Item(final int index, final String supplierProductCode, final String description, final String ncm,
				final String unit, final BigDecimal quantity, final BigDecimal unitValue, final UUID productId,
				final String productLabel, final BigDecimal lastPurchaseUnitValue, final Instant lastPurchaseAt,
				final BigDecimal priceDelta, final String barcode, final String cest) {
			this(index, supplierProductCode, description, ncm, unit, quantity, unitValue, productId, productLabel,
					lastPurchaseUnitValue, lastPurchaseAt, priceDelta, barcode, cest, List.of());
		}

		/** An item whose XML did not state a GTIN/CEST. */
		public Item(final int index, final String supplierProductCode, final String description, final String ncm,
				final String unit, final BigDecimal quantity, final BigDecimal unitValue, final UUID productId,
				final String productLabel, final BigDecimal lastPurchaseUnitValue, final Instant lastPurchaseAt,
				final BigDecimal priceDelta) {
			this(index, supplierProductCode, description, ncm, unit, quantity, unitValue, productId, productLabel,
					lastPurchaseUnitValue, lastPurchaseAt, priceDelta, null, null, List.of());
		}

		public boolean resolved() {
			return productId != null;
		}
	}

	/** A catalog product offered for an unlinked item, and why. */
	public record ProductSuggestion(UUID productId, String productLabel, MatchReason reason) {
	}

	/** {@code GTIN}: the item and the product share a barcode. {@code SIMILAR_DESCRIPTION}: same NCM and alike wording. */
	public enum MatchReason {
		GTIN, SIMILAR_DESCRIPTION
	}
}
