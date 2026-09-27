package br.gravita.core.domain.inventory;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Getter
public final class StockTransfer {

	private final StockTransferId id;
	private final UUID productId;
	private final UUID sourceWarehouseId;
	private final UUID destinationWarehouseId;
	private final BigDecimal quantity;
	private final StockTransferStatus status;

	private StockTransfer(StockTransferId id, UUID productId, UUID sourceWarehouseId, UUID destinationWarehouseId,
			BigDecimal quantity, StockTransferStatus status) {
		this.id = Objects.requireNonNull(id, "id is required");
		this.productId = Objects.requireNonNull(productId, "productId is required");
		this.sourceWarehouseId = Objects.requireNonNull(sourceWarehouseId, "sourceWarehouseId is required");
		this.destinationWarehouseId = Objects.requireNonNull(destinationWarehouseId,
				"destinationWarehouseId is required");
		this.quantity = Objects.requireNonNull(quantity, "quantity is required");
		this.status = Objects.requireNonNull(status, "status is required");
	}

	public static StockTransfer of(StockTransferId id, UUID productId, UUID sourceWarehouseId,
			UUID destinationWarehouseId, BigDecimal quantity, StockTransferStatus status) {
		return new StockTransfer(id, productId, sourceWarehouseId, destinationWarehouseId, quantity, status);
	}

	public static StockTransfer initiate(StockTransferId id, UUID productId, UUID sourceWarehouseId,
			UUID destinationWarehouseId, BigDecimal quantity) {
		return new StockTransfer(id, productId, sourceWarehouseId, destinationWarehouseId, quantity,
				StockTransferStatus.PENDING);
	}

	public StockTransfer confirm() {
		if (status != StockTransferStatus.PENDING) {
			throw new BusinessRuleException("Transfer " + id.value() + " has already been confirmed");
		}
		return new StockTransfer(id, productId, sourceWarehouseId, destinationWarehouseId, quantity,
				StockTransferStatus.CONFIRMED);
	}
}
