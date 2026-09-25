package br.gravita.core.domain.inventory;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

	/**
	 * Reserves {@code quantity} against this balance (UC-M5-06): moves it
	 * from {@code available} into {@code reserved} without touching
	 * {@code onHand}. Rejects the reservation when not enough is available,
	 * per the {@code StockBalance} invariant (module spec §"Domain model").
	 */
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

	/**
	 * Applies a stock entry: increases {@code onHand} and recalculates
	 * {@code averageCost} (CMV) as a weighted average of the existing balance
	 * and the new entry (UC-M5-02, AC1).
	 */
	public StockBalance receiveEntry(BigDecimal quantity, BigDecimal unitCost) {
		BigDecimal newOnHand = onHand.add(quantity);
		BigDecimal totalCost = onHand.multiply(averageCost).add(quantity.multiply(unitCost));
		BigDecimal newAverageCost = totalCost.divide(newOnHand, 4, RoundingMode.HALF_UP);
		return new StockBalance(id, productId, warehouseId, newOnHand, reserved, inTransit, newAverageCost);
	}

	/**
	 * Applies a plain stock exit (UC-M5-03): decreases {@code onHand} only.
	 * Availability against the negative-stock setting is enforced by the
	 * caller, which decides whether to allow going negative.
	 */
	public StockBalance exit(BigDecimal quantity) {
		if (quantity == null || quantity.signum() <= 0) {
			throw new BusinessRuleException("Exit quantity must be positive");
		}
		return new StockBalance(id, productId, warehouseId, onHand.subtract(quantity), reserved, inTransit,
				averageCost);
	}

	/**
	 * Fulfills an exit against an existing {@link StockReservation} (UC-M5-03,
	 * AC4): decreases {@code onHand} and {@code reserved} together, so
	 * {@code available} is unaffected - the quantity was already carved out of
	 * it when the reservation was created.
	 */
	public StockBalance consumeReserved(BigDecimal quantity) {
		if (quantity == null || quantity.signum() <= 0) {
			throw new BusinessRuleException("Exit quantity must be positive");
		}
		return new StockBalance(id, productId, warehouseId, onHand.subtract(quantity), reserved.subtract(quantity),
				inTransit, averageCost);
	}

	/**
	 * Applies a manual correction (UC-M5-04, AC2): {@code onHand} reflects
	 * {@code delta} exactly, positive or negative.
	 */
	public StockBalance applyAdjustment(BigDecimal delta) {
		if (delta == null || delta.signum() == 0) {
			throw new BusinessRuleException("Adjustment quantityDelta must not be zero");
		}
		return new StockBalance(id, productId, warehouseId, onHand.add(delta), reserved, inTransit, averageCost);
	}

	/**
	 * Initiates a transfer's outbound leg (UC-M5-05, AC1): moves {@code quantity}
	 * out of {@code onHand} into {@code inTransit} without touching
	 * {@code reserved}, so {@code available} ({@code onHand - reserved}) drops
	 * immediately while the destination hasn't received anything yet (AC2).
	 */
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

	/**
	 * Clears the pending leg on the source balance once the transfer is
	 * confirmed at the destination (UC-M5-05).
	 */
	public StockBalance releaseInTransit(BigDecimal quantity) {
		return new StockBalance(id, productId, warehouseId, onHand, reserved, inTransit.subtract(quantity),
				averageCost);
	}

	/**
	 * Receives a confirmed transfer's inbound leg (UC-M5-05): increases
	 * {@code onHand} at the destination.
	 */
	public StockBalance increaseOnHand(BigDecimal quantity) {
		return new StockBalance(id, productId, warehouseId, onHand.add(quantity), reserved, inTransit, averageCost);
	}
}
