package br.gravita.inventory.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.inventory.Lot;
import br.gravita.core.domain.inventory.LotId;
import br.gravita.core.domain.inventory.StockBalance;
import br.gravita.core.domain.inventory.StockBalanceId;
import br.gravita.core.domain.inventory.StockMovement;
import br.gravita.core.domain.inventory.StockReservation;
import br.gravita.core.domain.inventory.StockReservationId;
import br.gravita.core.domain.inventory.StockReservationStatus;
import br.gravita.core.ports.inbound.inventory.RegisterStockExitCommand;
import br.gravita.core.ports.outbound.persistence.inventory.LotRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.SerialUnitRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.StockBalanceRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.StockMovementRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.StockReservationRepositoryPort;
import br.gravita.core.usercases.inventory.LowStockReorderTrigger;
import br.gravita.core.usercases.inventory.RegisterStockExitService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegisterStockExitServiceTest {

	@Mock
	private StockBalanceRepositoryPort stockBalanceRepositoryPort;

	@Mock
	private StockMovementRepositoryPort stockMovementRepositoryPort;

	@Mock
	private LotRepositoryPort lotRepositoryPort;

	@Mock
	private SerialUnitRepositoryPort serialUnitRepositoryPort;

	@Mock
	private StockReservationRepositoryPort stockReservationRepositoryPort;

	@Mock
	private LowStockReorderTrigger lowStockReorderTrigger;

	private RegisterStockExitService service;

	private final UUID productId = UUID.randomUUID();
	private final UUID warehouseId = UUID.randomUUID();
	private final UUID userId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		service = new RegisterStockExitService(stockBalanceRepositoryPort, stockMovementRepositoryPort,
				lotRepositoryPort, serialUnitRepositoryPort, stockReservationRepositoryPort, lowStockReorderTrigger);
	}

	private RegisterStockExitCommand exitCommand(final BigDecimal quantity) {
		return new RegisterStockExitCommand(productId, warehouseId, quantity, null, List.of(), null, false,
				"SALE:1", userId);
	}

	private StockBalance balance(final BigDecimal onHand, final BigDecimal reserved) {
		return StockBalance.of(StockBalanceId.of(UUID.randomUUID()), productId, warehouseId, onHand, reserved,
				BigDecimal.ZERO, new BigDecimal("10.00"));
	}

	@Test
	@DisplayName("AC2: Blocks the exit when available stock is insufficient and negative stock is not allowed")
	void ac2BlocksTheExitWhenAvailableIsInsufficientAndNegativeStockIsNotAllowed() {
		when(stockBalanceRepositoryPort.findByProductIdAndWarehouseId(productId, warehouseId))
				.thenReturn(Optional.of(balance(new BigDecimal("10"), BigDecimal.ZERO)));

		assertThatThrownBy(() -> service.execute(exitCommand(new BigDecimal("20"))))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Insufficient available stock");

		verify(stockBalanceRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("AC2: Allows stock to go negative when explicitly permitted")
	void ac2AllowsGoingNegativeWhenExplicitlyPermitted() {
		when(stockBalanceRepositoryPort.findByProductIdAndWarehouseId(productId, warehouseId))
				.thenReturn(Optional.of(balance(new BigDecimal("10"), BigDecimal.ZERO)));
		when(stockMovementRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final RegisterStockExitCommand command = new RegisterStockExitCommand(productId, warehouseId,
				new BigDecimal("20"), null, List.of(), null, true, "SALE:1", userId);
		service.execute(command);

		final ArgumentCaptor<StockBalance> savedBalance = ArgumentCaptor.forClass(StockBalance.class);
		verify(stockBalanceRepositoryPort).save(savedBalance.capture());
		assertThat(savedBalance.getValue().getOnHand()).isEqualByComparingTo("-10");
	}

	@Test
	@DisplayName("AC1: Rejects allocating stock from an expired lot")
	void ac1RejectsAllocationFromAnExpiredLot() {
		when(stockBalanceRepositoryPort.findByProductIdAndWarehouseId(productId, warehouseId))
				.thenReturn(Optional.of(balance(new BigDecimal("100"), BigDecimal.ZERO)));
		final Lot expiredLot = Lot.of(LotId.of(UUID.randomUUID()), productId, warehouseId, "LOT-1",
				LocalDate.now().minusDays(1), new BigDecimal("50"));
		when(lotRepositoryPort.findByProductIdAndWarehouseIdAndCode(productId, warehouseId, "LOT-1"))
				.thenReturn(Optional.of(expiredLot));

		final RegisterStockExitCommand command = new RegisterStockExitCommand(productId, warehouseId, new BigDecimal("10"),
				new RegisterStockExitCommand.LotRef("LOT-1"), List.of(), null, false, "SALE:1", userId);

		assertThatThrownBy(() -> service.execute(command))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("expired");
		verify(lotRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("AC4: Fulfilling a reservation consumes it and leaves available stock unchanged")
	void ac4FulfillingAReservationConsumesItAndLeavesAvailableUnchanged() {
		final StockBalance current = balance(new BigDecimal("100"), new BigDecimal("30"));
		when(stockBalanceRepositoryPort.findByProductIdAndWarehouseId(productId, warehouseId))
				.thenReturn(Optional.of(current));
		final StockReservationId reservationId = StockReservationId.of(UUID.randomUUID());
		final StockReservation reservation = StockReservation.of(reservationId, UUID.randomUUID(), productId, warehouseId,
				new BigDecimal("30"), StockReservationStatus.ACTIVE);
		when(stockReservationRepositoryPort.findById(reservationId)).thenReturn(Optional.of(reservation));
		when(stockMovementRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final RegisterStockExitCommand command = new RegisterStockExitCommand(productId, warehouseId, new BigDecimal("30"),
				null, List.of(), reservationId.value(), false, "SALE:1", userId);
		final StockMovement movement = service.execute(command);

		final ArgumentCaptor<StockBalance> savedBalance = ArgumentCaptor.forClass(StockBalance.class);
		verify(stockBalanceRepositoryPort).save(savedBalance.capture());
		assertThat(savedBalance.getValue().getOnHand()).isEqualByComparingTo("70");
		assertThat(savedBalance.getValue().getReserved()).isEqualByComparingTo("0");
		assertThat(savedBalance.getValue().available()).isEqualByComparingTo(current.available());

		final ArgumentCaptor<StockReservation> savedReservation = ArgumentCaptor.forClass(StockReservation.class);
		verify(stockReservationRepositoryPort).save(savedReservation.capture());
		assertThat(savedReservation.getValue().getStatus()).isEqualTo(StockReservationStatus.CONSUMED);
		assertThat(movement.getQuantity()).isEqualByComparingTo("30");
	}

	@Test
	@DisplayName("Fulfilling an unknown reservation is rejected")
	void anUnknownReservationIsRejected() {
		when(stockBalanceRepositoryPort.findByProductIdAndWarehouseId(productId, warehouseId))
				.thenReturn(Optional.of(balance(new BigDecimal("100"), BigDecimal.ZERO)));
		final UUID reservationId = UUID.randomUUID();
		when(stockReservationRepositoryPort.findById(StockReservationId.of(reservationId)))
				.thenReturn(Optional.empty());

		final RegisterStockExitCommand command = new RegisterStockExitCommand(productId, warehouseId, new BigDecimal("10"),
				null, List.of(), reservationId, false, "SALE:1", userId);

		assertThatThrownBy(() -> service.execute(command)).isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	@DisplayName("AC3: The resulting movement is for exactly the exited quantity")
	void ac3TheResultingMovementIsForTheExactExitedQuantity() {
		when(stockBalanceRepositoryPort.findByProductIdAndWarehouseId(productId, warehouseId))
				.thenReturn(Optional.of(balance(new BigDecimal("100"), BigDecimal.ZERO)));
		when(stockMovementRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final StockMovement movement = service.execute(exitCommand(new BigDecimal("25")));

		assertThat(movement.getQuantity()).isEqualByComparingTo("25");
		assertThat(movement.getType().name()).isEqualTo("EXIT");
	}

	@Test
	@DisplayName("AC5: Evaluates low-stock reorder for the affected warehouse after a successful exit")
	void ac5EvaluatesLowStockReorderForTheAffectedWarehouseAfterASuccessfulExit() {
		when(stockBalanceRepositoryPort.findByProductIdAndWarehouseId(productId, warehouseId))
				.thenReturn(Optional.of(balance(new BigDecimal("100"), BigDecimal.ZERO)));
		when(stockMovementRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(exitCommand(new BigDecimal("25")));

		verify(lowStockReorderTrigger).evaluate(warehouseId);
	}

	@Test
	@DisplayName("Rejects an exit with a zero or negative quantity")
	void rejectsAZeroOrNegativeQuantity() {
		assertThatThrownBy(() -> service.execute(exitCommand(BigDecimal.ZERO)))
				.isInstanceOf(BusinessRuleException.class);
	}
}
