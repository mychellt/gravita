package br.gravita.core.ports.inbound.inventory;

import br.gravita.core.domain.inventory.Lot;
import br.gravita.core.domain.inventory.SerialUnit;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A lot or a serial unit, flattened. A lot has an expiry date and a quantity but no status; a serial unit has a
 * status, is always a single unit and never expires.
 */
public record StockUnitView(ListLotsQuery.Kind kind, UUID id, UUID productId, String productCode, UUID warehouseId,
		String code, LocalDate expiryDate, BigDecimal quantity, String status) {

	public static StockUnitView from(final Lot lot, final String productCode) {
		return new StockUnitView(ListLotsQuery.Kind.LOT, lot.getId().value(), lot.getProductId(), productCode,
				lot.getWarehouseId(), lot.getCode(), lot.getExpiryDate(), lot.getQuantity(), null);
	}

	public static StockUnitView from(final SerialUnit unit, final String productCode) {
		return new StockUnitView(ListLotsQuery.Kind.SERIAL, unit.getId().value(), unit.getProductId(), productCode,
				unit.getWarehouseId(), unit.getSerialNumber(), null, BigDecimal.ONE, unit.getStatus().name());
	}
}
