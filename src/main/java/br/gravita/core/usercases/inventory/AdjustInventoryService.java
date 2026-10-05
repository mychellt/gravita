package br.gravita.core.usercases.inventory;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.inventory.StockBalance;
import br.gravita.core.domain.inventory.StockBalanceId;
import br.gravita.core.domain.inventory.StockMovement;
import br.gravita.core.domain.inventory.StockMovementId;
import br.gravita.core.domain.inventory.StockMovementType;
import br.gravita.core.ports.inbound.inventory.AdjustInventoryCommand;
import br.gravita.core.ports.inbound.inventory.AdjustInventoryUseCase;
import br.gravita.core.ports.outbound.persistence.inventory.PostAdjustmentAccountingEntryPort;
import br.gravita.core.ports.outbound.persistence.inventory.PostAdjustmentAccountingEntryPort.PostAdjustmentAccountingEntryCommand;
import br.gravita.core.ports.outbound.persistence.inventory.StockBalanceRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.StockMovementRepositoryPort;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@UseCase
public class AdjustInventoryService implements AdjustInventoryUseCase {

	private final StockBalanceRepositoryPort stockBalanceRepositoryPort;
	private final StockMovementRepositoryPort stockMovementRepositoryPort;
	private final PostAdjustmentAccountingEntryPort postAdjustmentAccountingEntryPort;

	public AdjustInventoryService(final StockBalanceRepositoryPort stockBalanceRepositoryPort,
			final StockMovementRepositoryPort stockMovementRepositoryPort,
			final PostAdjustmentAccountingEntryPort postAdjustmentAccountingEntryPort) {
		this.stockBalanceRepositoryPort = stockBalanceRepositoryPort;
		this.stockMovementRepositoryPort = stockMovementRepositoryPort;
		this.postAdjustmentAccountingEntryPort = postAdjustmentAccountingEntryPort;
	}

	@Override
	public StockMovement execute(final AdjustInventoryCommand command) {
		if (command.justification() == null || command.justification().isBlank()) {
			throw new BusinessRuleException("Adjustment justification is required");
		}

		final StockBalance current = stockBalanceRepositoryPort
				.findByProductIdAndWarehouseId(command.productId(), command.warehouseId())
				.orElseGet(() -> StockBalance.of(StockBalanceId.of(UUID.randomUUID()), command.productId(),
						command.warehouseId(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
		final StockBalance updated = current.applyAdjustment(command.quantityDelta());
		stockBalanceRepositoryPort.save(updated);

		postAdjustmentAccountingEntryPort.postAdjustmentEntry(new PostAdjustmentAccountingEntryCommand(
				command.productId(), command.warehouseId(), command.quantityDelta(), current.getAverageCost(),
				command.justification(), command.user()));

		final StockMovement movement = StockMovement.builder()
				.id(StockMovementId.of(UUID.randomUUID()))
				.type(StockMovementType.ADJUSTMENT)
				.productId(command.productId())
				.warehouseId(command.warehouseId())
				.quantity(command.quantityDelta())
				.unitCost(current.getAverageCost())
				.lotCode(null)
				.serialNumbers(null)
				.originReference("MANUAL_ADJUSTMENT")
				.justification(command.justification())
				.user(command.user())
				.timestamp(Instant.now())
				.build();
		return stockMovementRepositoryPort.save(movement);
	}
}
