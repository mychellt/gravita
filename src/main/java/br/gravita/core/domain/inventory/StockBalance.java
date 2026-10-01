package br.gravita.core.domain.inventory;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

@Getter
public final class StockBalance {

	private final StockBalanceId id;
	private final UUID productId;
	private final UUID warehouseId;
	private final BigDecimal onHand;
	private final BigDecimal reserved;
	private final BigDecimal inTransit;
	private final BigDecimal averageCost;

	public StockBalance(StockBalanceId id, UUID productId, UUID warehouseId, BigDecimal onHand,
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

	public BigDecimal available() {
		return onHand.subtract(reserved);
	}

	public StockBalance reserve(BigDecimal quantity) {
		if (quantity == null || quantity.signum() <= 0) {
			throw new BusinessRuleException("Reservation quantity must be positive");
		}
		if (available().compareTo(quantity) < 0) {
			throw new BusinessRuleException(
					"Insufficient available stock to reserve: requested " + quantity + ", available " + available());
		}
		return new StockBalance(id, productId, warehouseId, onHand, reserved.add(quantity), inTransit, averageCost);
	}

	public StockBalance release(BigDecimal quantity) {
		if (quantity == null || quantity.signum() <= 0) {
			throw new BusinessRuleException("Release quantity must be positive");
		}
		return new StockBalance(id, productId, warehouseId, onHand, reserved.subtract(quantity), inTransit,
				averageCost);
	}

	public StockBalance receiveEntry(BigDecimal quantity, BigDecimal unitCost) {
		BigDecimal newOnHand = onHand.add(quantity);
		BigDecimal totalCost = onHand.multiply(averageCost).add(quantity.multiply(unitCost));
		BigDecimal newAverageCost = totalCost.divide(newOnHand, 4, RoundingMode.HALF_UP);
		return new StockBalance(id, productId, warehouseId, newOnHand, reserved, inTransit, newAverageCost);
	}

	public StockBalance exit(BigDecimal quantity) {
		if (quantity == null || quantity.signum() <= 0) {
			throw new BusinessRuleException("Exit quantity must be positive");
		}
		return new StockBalance(id, productId, warehouseId, onHand.subtract(quantity), reserved, inTransit,
				averageCost);
	}

	public StockBalance consumeReserved(BigDecimal quantity) {
		if (quantity == null || quantity.signum() <= 0) {
			throw new BusinessRuleException("Exit quantity must be positive");
		}
		return new StockBalance(id, productId, warehouseId, onHand.subtract(quantity), reserved.subtract(quantity),
				inTransit, averageCost);
	}

	public StockBalance applyAdjustment(BigDecimal delta) {
		if (delta == null || delta.signum() == 0) {
			throw new BusinessRuleException("Adjustment quantityDelta must not be zero");
		}
		return new StockBalance(id, productId, warehouseId, onHand.add(delta), reserved, inTransit, averageCost);
	}

	public StockBalance decreaseOnHandAndIncreaseInTransit(BigDecimal quantity) {
		if (quantity == null || quantity.signum() <= 0) {
			throw new BusinessRuleException("Transfer quantity must be positive");
		}
		if (available().compareTo(quantity) < 0) {
			throw new BusinessRuleException(
					"Insufficient available stock to transfer: requested " + quantity + ", available " + available());
		}
		return new StockBalance(id, productId, warehouseId, onHand.subtract(quantity), reserved,
				inTransit.add(quantity), averageCost);
	}

	public StockBalance releaseInTransit(BigDecimal quantity) {
		return new StockBalance(id, productId, warehouseId, onHand, reserved, inTransit.subtract(quantity),
				averageCost);
	}

	public StockBalance increaseOnHand(BigDecimal quantity) {
		return new StockBalance(id, productId, warehouseId, onHand.add(quantity), reserved, inTransit, averageCost);
	}
}
