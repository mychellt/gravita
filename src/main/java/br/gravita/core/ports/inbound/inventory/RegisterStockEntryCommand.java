package br.gravita.core.ports.inbound.inventory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * {@code lot} and {@code serials} are mutually exclusive traceability
 * dimensions, selected per {@code ProductDomain.lotControl}/{@code serialControl}
 * (UC-M5-02, AC3/AC4). Business validation - quantity sign, lot/serial
 * requirement per the product's traceability flags - happens in the service,
 * not here, so it surfaces as a {@code BusinessRuleException} rather than an
 * NPE.
 */
public record RegisterStockEntryCommand(UUID productId, UUID warehouseId, BigDecimal quantity, BigDecimal unitCost,
		LotDetails lot, List<String> serials, String originReference, UUID user) {

	public RegisterStockEntryCommand {
		Objects.requireNonNull(productId, "productId is required");
		Objects.requireNonNull(warehouseId, "warehouseId is required");
		Objects.requireNonNull(quantity, "quantity is required");
		Objects.requireNonNull(unitCost, "unitCost is required");
		Objects.requireNonNull(originReference, "originReference is required");
		Objects.requireNonNull(user, "user is required");
		serials = serials == null ? List.of() : List.copyOf(serials);
	}

	public record LotDetails(String code, LocalDate expiryDate) {
		public LotDetails {
			Objects.requireNonNull(code, "lot code is required");
		}
	}
}
