package br.gravita.core.domain.inventory;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Getter
public final class Lot {

	private final LotId id;
	private final UUID productId;
	private final UUID warehouseId;
	private final String code;
	private final LocalDate expiryDate;
	private final BigDecimal quantity;

	private Lot(LotId id, UUID productId, UUID warehouseId, String code, LocalDate expiryDate, BigDecimal quantity) {
		this.id = Objects.requireNonNull(id, "id is required");
		this.productId = Objects.requireNonNull(productId, "productId is required");
		this.warehouseId = Objects.requireNonNull(warehouseId, "warehouseId is required");
		this.code = Objects.requireNonNull(code, "code is required");
		this.expiryDate = Objects.requireNonNull(expiryDate, "expiryDate is required");
		this.quantity = Objects.requireNonNull(quantity, "quantity is required");
	}

	public static Lot of(LotId id, UUID productId, UUID warehouseId, String code, LocalDate expiryDate,
			BigDecimal quantity) {
		return new Lot(id, productId, warehouseId, code, expiryDate, quantity);
	}

	public Lot receive(BigDecimal additionalQuantity) {
		return new Lot(id, productId, warehouseId, code, expiryDate, quantity.add(additionalQuantity));
	}

	public boolean isExpired(LocalDate asOf) {
		return expiryDate.isBefore(asOf);
	}

	public Lot issue(BigDecimal quantity) {
		return new Lot(id, productId, warehouseId, code, expiryDate, this.quantity.subtract(quantity));
	}
}
