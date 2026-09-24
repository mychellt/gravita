package br.gravita.core.domain.inventory;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * One row per {@code product x warehouse}, per M5 module spec. Lot/serial
 * dimensions aren't modeled yet (UC-M5-01 only reads this projection).
 */
@Getter
public final class StockBalance {

	private final StockBalanceId id;
	private final UUID productId;
	private final UUID warehouseId;
	private final BigDecimal onHand;
	private final BigDecimal reserved;
	private final BigDecimal inTransit;
	private final BigDecimal averageCost;

	private StockBalance(StockBalanceId id, UUID productId, UUID warehouseId, BigDecimal onHand,
			BigDecimal reserved, BigDecimal inTransit, BigDecimal averageCost) {
		this.id = Objects.requireNonNull(id, "id is required");
		this.productId = Objects.requireNonNull(productId, "productId is required");
		this.warehouseId = Objects.requireNonNull(warehouseId, "warehouseId is required");
		this.onHand = Objects.requireNonNull(onHand, "onHand is required");
		this.reserved = Objects.requireNonNull(reserved, "reserved is required");
		this.inTransit = Objects.requireNonNull(inTransit, "inTransit is required");
		this.averageCost = Objects.requireNonNull(averageCost, "averageCost is required");
	}

	public static StockBalance of(StockBalanceId id, UUID productId, UUID warehouseId, BigDecimal onHand,
			BigDecimal reserved, BigDecimal inTransit, BigDecimal averageCost) {
		return new StockBalance(id, productId, warehouseId, onHand, reserved, inTransit, averageCost);
	}

	/**
	 * Derived per the {@code StockBalance} invariant (module spec §"Domain
	 * model"): {@code onHand - reserved}.
	 */
	public BigDecimal available() {
		return onHand.subtract(reserved);
	}
}
