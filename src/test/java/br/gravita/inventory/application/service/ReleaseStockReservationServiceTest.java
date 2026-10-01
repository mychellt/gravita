package br.gravita.inventory.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.inventory.StockBalance;
import br.gravita.core.domain.inventory.StockBalanceId;
import br.gravita.core.domain.inventory.StockReservation;
import br.gravita.core.domain.inventory.StockReservationId;
import br.gravita.core.domain.inventory.StockReservationStatus;
import br.gravita.core.ports.inbound.inventory.ReleaseStockReservationCommand;
import br.gravita.core.ports.outbound.persistence.inventory.StockBalanceRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.StockReservationRepositoryPort;
import br.gravita.core.usercases.inventory.ReleaseStockReservationService;
import java.math.BigDecimal;
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
class ReleaseStockReservationServiceTest {

	@Mock
	private StockBalanceRepositoryPort stockBalanceRepositoryPort;

	@Mock
	private StockReservationRepositoryPort stockReservationRepositoryPort;

	private ReleaseStockReservationService service;

	private final UUID orderRef = UUID.randomUUID();
	private final UUID productId = UUID.randomUUID();
	private final UUID warehouseId = UUID.randomUUID();
	private final StockReservationId reservationId = StockReservationId.of(UUID.randomUUID());

	@BeforeEach
	void setUp() {
		service = new ReleaseStockReservationService(stockBalanceRepositoryPort, stockReservationRepositoryPort);
	}

	@Test
	@DisplayName("Releasing restores exactly the reserved quantity to available and leaves on-hand unchanged")
	void releasingRestoresExactlyTheReservedQuantityToAvailableAndLeavesOnHandUnchanged() {
		StockReservation reservation = StockReservation.of(reservationId, orderRef, productId, warehouseId,
				new BigDecimal("30"), StockReservationStatus.ACTIVE);
		when(stockReservationRepositoryPort.findById(reservationId)).thenReturn(Optional.of(reservation));

		StockBalance balance = StockBalance.of(StockBalanceId.of(UUID.randomUUID()), productId, warehouseId,
				new BigDecimal("100"), new BigDecimal("40"), BigDecimal.ZERO, new BigDecimal("5.00"));
		when(stockBalanceRepositoryPort.findByProductIdAndWarehouseId(productId, warehouseId))
				.thenReturn(Optional.of(balance));

		service.execute(new ReleaseStockReservationCommand(reservationId.value()));

		ArgumentCaptor<StockBalance> savedBalance = ArgumentCaptor.forClass(StockBalance.class);
		verify(stockBalanceRepositoryPort).save(savedBalance.capture());
		assertThat(savedBalance.getValue().getOnHand()).isEqualByComparingTo("100");
		assertThat(savedBalance.getValue().getReserved()).isEqualByComparingTo("10");
		assertThat(savedBalance.getValue().available()).isEqualByComparingTo("90");

		ArgumentCaptor<StockReservation> savedReservation = ArgumentCaptor.forClass(StockReservation.class);
		verify(stockReservationRepositoryPort).save(savedReservation.capture());
		assertThat(savedReservation.getValue().getStatus()).isEqualTo(StockReservationStatus.RELEASED);
	}

	@Test
	@DisplayName("Releasing a reservation that was already released is rejected")
	void releasingAnAlreadyReleasedReservationIsRejected() {
		StockReservation reservation = StockReservation.of(reservationId, orderRef, productId, warehouseId,
				new BigDecimal("30"), StockReservationStatus.RELEASED);
		when(stockReservationRepositoryPort.findById(reservationId)).thenReturn(Optional.of(reservation));

		assertThatThrownBy(() -> service.execute(new ReleaseStockReservationCommand(reservationId.value())))
				.isInstanceOf(BusinessRuleException.class);

		verify(stockBalanceRepositoryPort, never()).save(any());
		verify(stockReservationRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Releasing a reservation that was already consumed is rejected")
	void releasingAnAlreadyConsumedReservationIsRejected() {
		StockReservation reservation = StockReservation.of(reservationId, orderRef, productId, warehouseId,
				new BigDecimal("30"), StockReservationStatus.CONSUMED);
		when(stockReservationRepositoryPort.findById(reservationId)).thenReturn(Optional.of(reservation));

		assertThatThrownBy(() -> service.execute(new ReleaseStockReservationCommand(reservationId.value())))
				.isInstanceOf(BusinessRuleException.class);

		verify(stockBalanceRepositoryPort, never()).save(any());
		verify(stockReservationRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Releasing a reservation that does not exist is rejected")
	void releasingAnUnknownReservationIsRejected() {
		when(stockReservationRepositoryPort.findById(reservationId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new ReleaseStockReservationCommand(reservationId.value())))
				.isInstanceOf(ResourceNotFoundException.class);

		verify(stockBalanceRepositoryPort, never()).save(any());
		verify(stockReservationRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Releasing by order reference releases every active reservation of that order")
	void releasingByOrderRefReleasesEveryActiveReservationForThatOrder() {
		UUID otherProductId = UUID.randomUUID();
		StockReservation firstReservation = StockReservation.of(reservationId, orderRef, productId, warehouseId,
				new BigDecimal("30"), StockReservationStatus.ACTIVE);
		StockReservation secondReservation = StockReservation.of(StockReservationId.of(UUID.randomUUID()), orderRef,
				otherProductId, warehouseId, new BigDecimal("5"), StockReservationStatus.ACTIVE);
		when(stockReservationRepositoryPort.findActiveByOrderRef(orderRef))
				.thenReturn(List.of(firstReservation, secondReservation));

		StockBalance firstBalance = StockBalance.of(StockBalanceId.of(UUID.randomUUID()), productId, warehouseId,
				new BigDecimal("100"), new BigDecimal("40"), BigDecimal.ZERO, new BigDecimal("5.00"));
		StockBalance secondBalance = StockBalance.of(StockBalanceId.of(UUID.randomUUID()), otherProductId,
				warehouseId, new BigDecimal("20"), new BigDecimal("5"), BigDecimal.ZERO, new BigDecimal("2.00"));
		when(stockBalanceRepositoryPort.findByProductIdAndWarehouseId(productId, warehouseId))
				.thenReturn(Optional.of(firstBalance));
		when(stockBalanceRepositoryPort.findByProductIdAndWarehouseId(otherProductId, warehouseId))
				.thenReturn(Optional.of(secondBalance));

		service.execute(ReleaseStockReservationCommand.byOrderRef(orderRef));

		verify(stockBalanceRepositoryPort, times(2)).save(any());
		ArgumentCaptor<StockReservation> savedReservations = ArgumentCaptor.forClass(StockReservation.class);
		verify(stockReservationRepositoryPort, times(2)).save(savedReservations.capture());
		assertThat(savedReservations.getAllValues())
				.allMatch(reservation -> reservation.getStatus() == StockReservationStatus.RELEASED);
	}

	@Test
	@DisplayName("Releasing by order reference does nothing when the order has no active reservations")
	void releasingByOrderRefWithNoActiveReservationsDoesNothing() {
		when(stockReservationRepositoryPort.findActiveByOrderRef(orderRef)).thenReturn(List.of());

		service.execute(ReleaseStockReservationCommand.byOrderRef(orderRef));

		verify(stockBalanceRepositoryPort, never()).save(any());
		verify(stockReservationRepositoryPort, never()).save(any());
	}
}
