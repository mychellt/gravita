package br.gravita.core.ports.inbound.tax;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * What the conference screen needs to render a receipt, including the supplier/product resolution the backend
 * computed dynamically for this request (GRA-246 BR1-BR3) - never persisted on the {@code InboundNfe} itself.
 */
public record InboundNfeConferenceView(
		UUID id,
		String series,
		String number,
		String supplierName,
		boolean supplierResolved,
		UUID supplierId,
		List<InboundNfeConferenceItemView> items) {

	public record InboundNfeConferenceItemView(
			String supplierProductCode,
			String description,
			String ncm,
			String unit,
			BigDecimal quantity,
			BigDecimal unitValue,
			/** Null when no {@code SupplierProductCatalogLink} exists yet for this supplier+code - the item is unresolved. */
			UUID resolvedProductId,
			/** The resolved product's own catalog code, for the UI to show instead of a bare id; null when unresolved. */
			String resolvedProductLabel,
			/** The link's price from its last purchase before this NFe; null on a first-ever purchase of this item. */
			BigDecimal lastPurchaseUnitValue) {
	}
}
