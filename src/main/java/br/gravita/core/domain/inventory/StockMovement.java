package br.gravita.core.domain.inventory;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

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
	private final String justification;
	private final UUID user;
	private final Instant timestamp;

	@Builder
	public StockMovement(final StockMovementId id, final StockMovementType type, final UUID productId, final UUID warehouseId,
			final BigDecimal quantity, final BigDecimal unitCost, final String lotCode, final List<String> serialNumbers,
			final String originReference, final String justification, final UUID user, final Instant timestamp) {
		this.id = Objects.requireNonNull(id, "id is required");
		this.type = Objects.requireNonNull(type, "type is required");
		this.productId = Objects.requireNonNull(productId, "productId is required");
		this.warehouseId = Objects.requireNonNull(warehouseId, "warehouseId is required");
		this.quantity = Objects.requireNonNull(quantity, "quantity is required");
		this.unitCost = Objects.requireNonNull(unitCost, "unitCost is required");
		this.lotCode = lotCode;
		this.serialNumbers = serialNumbers == null ? List.of() : List.copyOf(serialNumbers);
		this.originReference = Objects.requireNonNull(originReference, "originReference is required");
		this.justification = justification;
		this.user = Objects.requireNonNull(user, "user is required");
		this.timestamp = Objects.requireNonNull(timestamp, "timestamp is required");
	}

}
