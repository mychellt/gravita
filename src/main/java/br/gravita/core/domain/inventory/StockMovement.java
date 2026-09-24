package br.gravita.core.domain.inventory;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Append-only per module spec (§"Domain model") - {@code StockMovementRepositoryPort}
 * intentionally exposes no update/delete method (UC-M5-02, AC2).
 */
@Getter
public final class StockMovement {

	private final StockMovementId id;
	private final StockMovementType type;
	private final UUID productId;
	private final UUID warehouseId;
	private final BigDecimal quantity;
	private final BigDecimal unitCost;
	private final String lotCode;
	private final List<String> serialNumbers;
	private final String originReference;
	private final UUID user;
	private final Instant timestamp;

	private StockMovement(StockMovementId id, StockMovementType type, UUID productId, UUID warehouseId,
			BigDecimal quantity, BigDecimal unitCost, String lotCode, List<String> serialNumbers,
			String originReference, UUID user, Instant timestamp) {
		this.id = Objects.requireNonNull(id, "id is required");
		this.type = Objects.requireNonNull(type, "type is required");
		this.productId = Objects.requireNonNull(productId, "productId is required");
		this.warehouseId = Objects.requireNonNull(warehouseId, "warehouseId is required");
		this.quantity = Objects.requireNonNull(quantity, "quantity is required");
		this.unitCost = Objects.requireNonNull(unitCost, "unitCost is required");
		this.lotCode = lotCode;
		this.serialNumbers = serialNumbers == null ? List.of() : List.copyOf(serialNumbers);
		this.originReference = Objects.requireNonNull(originReference, "originReference is required");
		this.user = Objects.requireNonNull(user, "user is required");
		this.timestamp = Objects.requireNonNull(timestamp, "timestamp is required");
	}

	public static StockMovement of(StockMovementId id, StockMovementType type, UUID productId, UUID warehouseId,
			BigDecimal quantity, BigDecimal unitCost, String lotCode, List<String> serialNumbers,
			String originReference, UUID user, Instant timestamp) {
		return new StockMovement(id, type, productId, warehouseId, quantity, unitCost, lotCode, serialNumbers,
				originReference, user, timestamp);
	}
}
