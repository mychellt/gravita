package br.gravita.core.usercases.inventory;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.inventory.Lot;
import br.gravita.core.domain.inventory.LotId;
import br.gravita.core.domain.inventory.SerialUnit;
import br.gravita.core.domain.inventory.SerialUnitId;
import br.gravita.core.domain.inventory.StockBalance;
import br.gravita.core.domain.inventory.StockBalanceId;
import br.gravita.core.domain.inventory.StockMovement;
import br.gravita.core.domain.inventory.StockMovementId;
import br.gravita.core.domain.inventory.StockMovementType;
import br.gravita.core.ports.inbound.inventory.RegisterStockEntryCommand;
import br.gravita.core.ports.inbound.inventory.RegisterStockEntryUseCase;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.LotRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.SerialUnitRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.StockBalanceRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.StockMovementRepositoryPort;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@UseCase
public class RegisterStockEntryService implements RegisterStockEntryUseCase {

	private final StockBalanceRepositoryPort stockBalanceRepositoryPort;
	private final StockMovementRepositoryPort stockMovementRepositoryPort;
	private final LotRepositoryPort lotRepositoryPort;
	private final SerialUnitRepositoryPort serialUnitRepositoryPort;
	private final ProductRepositoryPort productRepositoryPort;

	public RegisterStockEntryService(StockBalanceRepositoryPort stockBalanceRepositoryPort,
			StockMovementRepositoryPort stockMovementRepositoryPort, LotRepositoryPort lotRepositoryPort,
			SerialUnitRepositoryPort serialUnitRepositoryPort, ProductRepositoryPort productRepositoryPort) {
		this.stockBalanceRepositoryPort = stockBalanceRepositoryPort;
		this.stockMovementRepositoryPort = stockMovementRepositoryPort;
		this.lotRepositoryPort = lotRepositoryPort;
		this.serialUnitRepositoryPort = serialUnitRepositoryPort;
		this.productRepositoryPort = productRepositoryPort;
	}

	@Override
	public StockMovement execute(RegisterStockEntryCommand command) {
		if (command.quantity().compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessRuleException("Entry quantity must be greater than zero");
		}

		ProductDomain product = productRepositoryPort.get(command.productId())
				.orElseThrow(() -> new ResourceNotFoundException("Product not found: " + command.productId()));

		boolean lotControl = Boolean.TRUE.equals(product.getLotControl());
		boolean serialControl = Boolean.TRUE.equals(product.getSerialControl());
		validateTraceability(command, lotControl, serialControl);

		StockBalance current = stockBalanceRepositoryPort
				.findByProductIdAndWarehouseId(command.productId(), command.warehouseId())
				.orElseGet(() -> StockBalance.of(StockBalanceId.of(UUID.randomUUID()), command.productId(),
						command.warehouseId(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
		stockBalanceRepositoryPort.save(current.receiveEntry(command.quantity(), command.unitCost()));

		if (lotControl) {
			registerLot(command);
		}
		if (serialControl) {
			registerSerials(command);
		}

		StockMovement movement = StockMovement.of(StockMovementId.of(UUID.randomUUID()), StockMovementType.ENTRY,
				command.productId(), command.warehouseId(), command.quantity(), command.unitCost(),
				lotControl ? command.lot().code() : null, serialControl ? command.serials() : List.of(),
				command.originReference(), null, command.user(), Instant.now());
		return stockMovementRepositoryPort.save(movement);
	}

	private void validateTraceability(RegisterStockEntryCommand command, boolean lotControl, boolean serialControl) {
		if (lotControl && (command.lot() == null || command.lot().expiryDate() == null)) {
			throw new BusinessRuleException(
					"Product " + command.productId() + " has lot control active; lot code and expiryDate are required");
		}
		if (serialControl
				&& command.quantity().compareTo(BigDecimal.valueOf(command.serials().size())) != 0) {
			throw new BusinessRuleException("Product " + command.productId()
					+ " has serial control active; exactly " + command.quantity() + " serial number(s) are required");
		}
	}

	private void registerLot(RegisterStockEntryCommand command) {
		Lot lot = lotRepositoryPort
				.findByProductIdAndWarehouseIdAndCode(command.productId(), command.warehouseId(), command.lot().code())
				.map(existing -> existing.receive(command.quantity()))
				.orElseGet(() -> Lot.of(LotId.of(UUID.randomUUID()), command.productId(), command.warehouseId(),
						command.lot().code(), command.lot().expiryDate(), command.quantity()));
		lotRepositoryPort.save(lot);
	}

	private void registerSerials(RegisterStockEntryCommand command) {
		List<SerialUnit> serialUnits = command.serials().stream()
				.map(serialNumber -> SerialUnit.received(SerialUnitId.of(UUID.randomUUID()), command.productId(),
						command.warehouseId(), serialNumber))
				.toList();
		serialUnitRepositoryPort.saveAll(serialUnits);
	}
}
