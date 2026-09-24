package br.gravita.core.domain.purchasing;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * UC-M6-07: reconciles a {@link PurchaseReceipt} against its {@link PurchaseOrder}
 * and the supplier's just-imported NFe. Per-product ordered-vs-received
 * quantity divergence is read straight off the receipt (each
 * {@link PurchaseReceiptItem} already carries both quantities); the
 * NF-invoiced amount is only compared at the document total, not per line,
 * because there's no supplier-SKU-to-catalog-product crosswalk yet (see
 * {@code InboundNfeItem}'s javadoc - that mapping is still an open concern for
 * UC-M2-10/UC-M6-07).
 */
public record ConferenceResult(List<ConferenceLine> lines, BigDecimal orderedValue, BigDecimal invoicedValue) {

	public ConferenceResult {
		lines = lines == null ? List.of() : List.copyOf(lines);
		Objects.requireNonNull(orderedValue, "orderedValue is required");
		Objects.requireNonNull(invoicedValue, "invoicedValue is required");
	}

	public boolean hasDivergences() {
		return lines.stream().anyMatch(ConferenceLine::isDivergent) || orderedValue.compareTo(invoicedValue) != 0;
	}

	public record ConferenceLine(UUID productId, BigDecimal orderedQty, BigDecimal receivedQty) {

		public boolean isDivergent() {
			return orderedQty.compareTo(receivedQty) != 0;
		}
	}
}
