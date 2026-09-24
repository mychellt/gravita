package br.gravita.inventory.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.inventory.StockBalance;
import br.gravita.core.domain.inventory.StockBalanceId;
import br.gravita.core.domain.inventory.StockReservation;
import br.gravita.core.domain.inventory.StockReservationStatus;
import br.gravita.core.ports.inbound.inventory.ReserveStockCommand;
import br.gravita.core.ports.outbound.persistence.inventory.StockBalanceRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.StockReservationRepositoryPort;
import br.gravita.core.usercases.inventory.ReserveStockService;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReserveStockServiceTest {

	@Mock
	private StockBalanceRepositoryPort stockBalanceRepositoryPort;

	@Mock
	private StockReservationRepositoryPort stockReservationRepositoryPort;

	private ReserveStockService service;

	private final UUID orderRef = UUID.randomUUID();
	private final UUID productId = UUID.randomUUID();
	private final UUID warehouseId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		service = new ReserveStockService(stockBalanceRepositoryPort, stockReservationRepositoryPort);
	}

	@Test
	void reservingMoreThanAvailableIsRejected() {
		StockBalance balance = StockBalance.of(StockBalanceId.of(UUID.randomUUID()), productId, warehouseId,
				new BigDecimal("10"), new BigDecimal("5"), BigDecimal.ZERO, new BigDecimal("2.00"));
		when(stockBalanceRepositoryPort.findByProductIdAndWarehouseId(productId, warehouseId))
				.thenReturn(Optional.of(balance));

		assertThatThrownBy(() -> service.execute(new ReserveStockCommand(orderRef, productId, warehouseId,
				new BigDecimal("6"))))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Insufficient available stock");

		verify(stockBalanceRepositoryPort, never()).save(any());
		verify(stockReservationRepositoryPort, never()).save(any());
	}

	@Test
	void reservingAgainstAProductWithNoTrackedBalanceIsRejected() {
		when(stockBalanceRepositoryPort.findByProductIdAndWarehouseId(productId, warehouseId))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new ReserveStockCommand(orderRef, productId, warehouseId,
				new BigDecimal("1"))))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void reservingIncreasesReservedAndLeavesOnHandUnchanged() {
		StockBalance balance = StockBalance.of(StockBalanceId.of(UUID.randomUUID()), productId, warehouseId,
				new BigDecimal("100"), new BigDecimal("10"), BigDecimal.ZERO, new BigDecimal("5.00"));
		when(stockBalanceRepositoryPort.findByProductIdAndWarehouseId(productId, warehouseId))
				.thenReturn(Optional.of(balance));
		when(stockReservationRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(new ReserveStockCommand(orderRef, productId, warehouseId, new BigDecimal("30")));

		ArgumentCaptor<StockBalance> savedBalance = ArgumentCaptor.forClass(StockBalance.class);
		verify(stockBalanceRepositoryPort).save(savedBalance.capture());
		assertThat(savedBalance.getValue().getOnHand()).isEqualByComparingTo("100");
		assertThat(savedBalance.getValue().getReserved()).isEqualByComparingTo("40");
		assertThat(savedBalance.getValue().available()).isEqualByComparingTo("60");
	}

	@Test
	void theCreatedReservationIsTraceableBackToItsOrderRefAndStartsActive() {
		StockBalance balance = StockBalance.of(StockBalanceId.of(UUID.randomUUID()), productId, warehouseId,
				new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("5.00"));
		when(stockBalanceRepositoryPort.findByProductIdAndWarehouseId(productId, warehouseId))
				.thenReturn(Optional.of(balance));
		when(stockReservationRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		StockReservation reservation = service.execute(
				new ReserveStockCommand(orderRef, productId, warehouseId, new BigDecimal("15")));

		assertThat(reservation.getOrderRef()).isEqualTo(orderRef);
		assertThat(reservation.getProductId()).isEqualTo(productId);
		assertThat(reservation.getWarehouseId()).isEqualTo(warehouseId);
		assertThat(reservation.getQuantity()).isEqualByComparingTo("15");
		assertThat(reservation.getStatus()).isEqualTo(StockReservationStatus.ACTIVE);
	}
}
