package br.gravita.core.usercases.inventory;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.inventory.Lot;
import br.gravita.core.domain.inventory.SerialUnit;
import br.gravita.core.domain.inventory.StockBalance;
import br.gravita.core.domain.inventory.StockMovement;
import br.gravita.core.domain.inventory.StockMovementId;
import br.gravita.core.domain.inventory.StockMovementType;
import br.gravita.core.domain.inventory.StockReservation;
import br.gravita.core.domain.inventory.StockReservationId;
import br.gravita.core.domain.inventory.StockReservationStatus;
import br.gravita.core.ports.inbound.inventory.RegisterStockExitCommand;
import br.gravita.core.ports.inbound.inventory.RegisterStockExitUseCase;
import br.gravita.core.ports.outbound.persistence.inventory.LotRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.SerialUnitRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.StockBalanceRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.StockMovementRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.StockReservationRepositoryPort;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@UseCase
public class RegisterStockExitService implements RegisterStockExitUseCase {

	private final StockBalanceRepositoryPort stockBalanceRepositoryPort;
	private final StockMovementRepositoryPort stockMovementRepositoryPort;
	private final LotRepositoryPort lotRepositoryPort;
	private final SerialUnitRepositoryPort serialUnitRepositoryPort;
	private final StockReservationRepositoryPort stockReservationRepositoryPort;
	private final LowStockReorderTrigger lowStockReorderTrigger;

	public RegisterStockExitService(final StockBalanceRepositoryPort stockBalanceRepositoryPort,
			final StockMovementRepositoryPort stockMovementRepositoryPort, final LotRepositoryPort lotRepositoryPort,
			final SerialUnitRepositoryPort serialUnitRepositoryPort,
			final StockReservationRepositoryPort stockReservationRepositoryPort,
			final LowStockReorderTrigger lowStockReorderTrigger) {
		this.stockBalanceRepositoryPort = stockBalanceRepositoryPort;
		this.stockMovementRepositoryPort = stockMovementRepositoryPort;
		this.lotRepositoryPort = lotRepositoryPort;
		this.serialUnitRepositoryPort = serialUnitRepositoryPort;
		this.stockReservationRepositoryPort = stockReservationRepositoryPort;
		this.lowStockReorderTrigger = lowStockReorderTrigger;
	}

	@Override
	public StockMovement execute(final RegisterStockExitCommand command) {
		if (command.quantity().signum() <= 0) {
			throw new BusinessRuleException("Exit quantity must be greater than zero");
		}

		final StockBalance balance = stockBalanceRepositoryPort
				.findByProductIdAndWarehouseId(command.productId(), command.warehouseId())
				.orElseThrow(() -> new BusinessRuleException("Insufficient available stock to exit: requested "
						+ command.quantity() + ", available 0"));

		final StockReservation reservation = command.reservationId() == null ? null : resolveReservation(command);

		if (reservation == null && !command.allowNegativeStock()
				&& balance.available().compareTo(command.quantity()) < 0) {
			throw new BusinessRuleException("Insufficient available stock to exit: requested " + command.quantity()
					+ ", available " + balance.available());
		}

		if (command.lot() != null) {
			allocateLot(command);
		}
		if (!command.serials().isEmpty()) {
			allocateSerials(command);
		}

		final StockBalance updated = reservation != null ? balance.consumeReserved(command.quantity())
				: balance.exit(command.quantity());
		stockBalanceRepositoryPort.save(updated);
		if (reservation != null) {
			stockReservationRepositoryPort.save(reservation);
		}

		final StockMovement movement = StockMovement.builder()
				.id(StockMovementId.of(UUID.randomUUID()))
				.type(StockMovementType.EXIT)
				.productId(command.productId())
				.warehouseId(command.warehouseId())
				.quantity(command.quantity())
				.unitCost(balance.getAverageCost())
				.lotCode(command.lot() != null ? command.lot().code() : null)
				.serialNumbers(command.serials())
				.originReference(command.originReference())
				.justification(null)
				.user(command.user())
				.timestamp(Instant.now())
				.build();
		final StockMovement saved = stockMovementRepositoryPort.save(movement);
		lowStockReorderTrigger.evaluate(command.warehouseId());
		return saved;
	}

	private StockReservation resolveReservation(final RegisterStockExitCommand command) {
		final StockReservation reservation = stockReservationRepositoryPort
				.findById(StockReservationId.of(command.reservationId()))
				.orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + command.reservationId()));
		if (reservation.getStatus() != StockReservationStatus.ACTIVE) {
			throw new BusinessRuleException("Reservation is not active: " + command.reservationId());
		}
		if (!reservation.getProductId().equals(command.productId())
				|| !reservation.getWarehouseId().equals(command.warehouseId())) {
			throw new BusinessRuleException(
					"Reservation " + command.reservationId() + " does not match the requested product/warehouse");
		}
		if (reservation.getQuantity().compareTo(command.quantity()) != 0) {
			throw new BusinessRuleException("Exit quantity must match the reserved quantity: reserved "
					+ reservation.getQuantity() + ", requested " + command.quantity());
		}
		return reservation.consume();
	}

	private void allocateLot(final RegisterStockExitCommand command) {
		final Lot lot = lotRepositoryPort
				.findByProductIdAndWarehouseIdAndCode(command.productId(), command.warehouseId(),
						command.lot().code())
				.orElseThrow(() -> new ResourceNotFoundException("Lot not found: " + command.lot().code()));
		if (lot.isExpired(LocalDate.now())) {
			throw new BusinessRuleException("Cannot allocate expired lot: " + command.lot().code());
		}
		lotRepositoryPort.save(lot.issue(command.quantity()));
	}

	private void allocateSerials(final RegisterStockExitCommand command) {
		final List<SerialUnit> found = serialUnitRepositoryPort.findByProductIdAndWarehouseIdAndSerialNumberIn(
				command.productId(), command.warehouseId(), command.serials());
		if (found.size() != command.serials().size()) {
			throw new ResourceNotFoundException("One or more serial units not found for product "
					+ command.productId() + " at warehouse " + command.warehouseId());
		}
		serialUnitRepositoryPort.saveAll(found.stream().map(SerialUnit::issue).toList());
	}
}
