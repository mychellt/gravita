package br.gravita.core.domain.inventory;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import lombok.Getter;

import java.util.Objects;
import java.util.UUID;

/** Tracked individually rather than by quantity, per module spec (UC-M5-02, AC4). */
@Getter
public final class SerialUnit {

	private final SerialUnitId id;
	private final UUID productId;
	private final UUID warehouseId;
	private final String serialNumber;
	private final SerialUnitStatus status;

	private SerialUnit(SerialUnitId id, UUID productId, UUID warehouseId, String serialNumber,
			SerialUnitStatus status) {
		this.id = Objects.requireNonNull(id, "id is required");
		this.productId = Objects.requireNonNull(productId, "productId is required");
		this.warehouseId = Objects.requireNonNull(warehouseId, "warehouseId is required");
		this.serialNumber = Objects.requireNonNull(serialNumber, "serialNumber is required");
		this.status = Objects.requireNonNull(status, "status is required");
	}

	public static SerialUnit received(SerialUnitId id, UUID productId, UUID warehouseId, String serialNumber) {
		return new SerialUnit(id, productId, warehouseId, serialNumber, SerialUnitStatus.IN_STOCK);
	}

	/** Allocates this unit to an exit (UC-M5-03, AC1); rejects units already issued. */
	public SerialUnit issue() {
		if (status != SerialUnitStatus.IN_STOCK) {
			throw new BusinessRuleException("Serial unit " + serialNumber + " is not in stock: " + status);
		}
		return new SerialUnit(id, productId, warehouseId, serialNumber, SerialUnitStatus.ISSUED);
	}
}
