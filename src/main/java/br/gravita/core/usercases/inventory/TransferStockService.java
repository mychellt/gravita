package br.gravita.core.usercases.inventory;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.inventory.StockBalance;
import br.gravita.core.domain.inventory.StockBalanceId;
import br.gravita.core.domain.inventory.StockMovement;
import br.gravita.core.domain.inventory.StockMovementId;
import br.gravita.core.domain.inventory.StockMovementType;
import br.gravita.core.domain.inventory.StockTransfer;
import br.gravita.core.domain.inventory.StockTransferId;
import br.gravita.core.ports.inbound.inventory.ConfirmTransferCommand;
import br.gravita.core.ports.inbound.inventory.InitiateTransferCommand;
import br.gravita.core.ports.inbound.inventory.TransferStockUseCase;
import br.gravita.core.ports.outbound.persistence.inventory.StockBalanceRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.StockMovementRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.StockTransferRepositoryPort;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@UseCase
public class TransferStockService implements TransferStockUseCase {

	private final StockBalanceRepositoryPort stockBalanceRepositoryPort;
	private final StockMovementRepositoryPort stockMovementRepositoryPort;
	private final StockTransferRepositoryPort stockTransferRepositoryPort;

	public TransferStockService(final StockBalanceRepositoryPort stockBalanceRepositoryPort,
			final StockMovementRepositoryPort stockMovementRepositoryPort,
			final StockTransferRepositoryPort stockTransferRepositoryPort) {
		this.stockBalanceRepositoryPort = stockBalanceRepositoryPort;
		this.stockMovementRepositoryPort = stockMovementRepositoryPort;
		this.stockTransferRepositoryPort = stockTransferRepositoryPort;
	}

	@Override
	public StockMovement initiate(final InitiateTransferCommand command) {
		if (command.quantity().signum() <= 0) {
			throw new BusinessRuleException("Transfer quantity must be greater than zero");
		}

		final StockBalance source = stockBalanceRepositoryPort
				.findByProductIdAndWarehouseId(command.productId(), command.sourceWarehouseId())
				.orElseThrow(() -> new BusinessRuleException("Insufficient available stock to transfer: requested "
						+ command.quantity() + ", available 0"));
		stockBalanceRepositoryPort.save(source.decreaseOnHandAndIncreaseInTransit(command.quantity()));

		final UUID transferId = UUID.randomUUID();
		final StockTransfer transfer = StockTransfer.initiate(StockTransferId.of(transferId), command.productId(),
				command.sourceWarehouseId(), command.destinationWarehouseId(), command.quantity());
		stockTransferRepositoryPort.save(transfer);

		final StockMovement movement = StockMovement.builder()
				.id(StockMovementId.of(transferId))
				.type(StockMovementType.TRANSFER)
				.productId(command.productId())
				.warehouseId(command.sourceWarehouseId())
				.quantity(command.quantity())
				.unitCost(source.getAverageCost())
				.lotCode(command.lotCode())
				.serialNumbers(command.serials())
				.originReference("Transfer to warehouse " + command.destinationWarehouseId())
				.justification(null)
				.user(command.user())
				.timestamp(Instant.now())
				.build();
		return stockMovementRepositoryPort.save(movement);
	}

	@Override
	public StockMovement confirm(final ConfirmTransferCommand command) {
		final StockTransfer transfer = stockTransferRepositoryPort.findById(StockTransferId.of(command.transferMovementId()))
				.orElseThrow(
						() -> new ResourceNotFoundException("Transfer not found: " + command.transferMovementId()));
		stockTransferRepositoryPort.save(transfer.confirm());

		final StockBalance source = stockBalanceRepositoryPort
				.findByProductIdAndWarehouseId(transfer.getProductId(), transfer.getSourceWarehouseId())
				.orElseThrow(() -> new ResourceNotFoundException(
						"Source stock balance not found for transfer: " + command.transferMovementId()));
		stockBalanceRepositoryPort.save(source.releaseInTransit(transfer.getQuantity()));

		final StockBalance destination = stockBalanceRepositoryPort
				.findByProductIdAndWarehouseId(transfer.getProductId(), transfer.getDestinationWarehouseId())
				.orElseGet(() -> StockBalance.of(StockBalanceId.of(UUID.randomUUID()), transfer.getProductId(),
						transfer.getDestinationWarehouseId(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
						BigDecimal.ZERO));
		stockBalanceRepositoryPort.save(destination.increaseOnHand(transfer.getQuantity()));

		final StockMovement movement = StockMovement.builder()
				.id(StockMovementId.of(UUID.randomUUID()))
				.type(StockMovementType.TRANSFER)
				.productId(transfer.getProductId())
				.warehouseId(transfer.getDestinationWarehouseId())
				.quantity(transfer.getQuantity())
				.unitCost(destination.getAverageCost())
				.lotCode(null)
				.serialNumbers(List.of())
				.originReference("Transfer confirmation for " + transfer.getId().value())
				.justification(null)
				.user(command.user())
				.timestamp(Instant.now())
				.build();
		return stockMovementRepositoryPort.save(movement);
	}
}
