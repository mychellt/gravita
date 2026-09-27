package br.gravita.inventory.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.inventory.StockBalance;
import br.gravita.core.domain.inventory.StockBalanceId;
import br.gravita.core.ports.inbound.inventory.GetStockBalanceQuery;
import br.gravita.core.ports.inbound.inventory.StockBalanceView;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.StockBalanceRepositoryPort;
import br.gravita.core.usercases.inventory.GetStockBalanceService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetStockBalanceServiceTest {

	@Mock
	private StockBalanceRepositoryPort stockBalanceRepositoryPort;

	@Mock
	private ProductRepositoryPort productRepositoryPort;

	private GetStockBalanceService service;

	private final UUID productId = UUID.randomUUID();
	private final UUID warehouseId = UUID.randomUUID();

	@org.junit.jupiter.api.BeforeEach
	void setUp() {
		service = new GetStockBalanceService(stockBalanceRepositoryPort, productRepositoryPort);
	}

	@Test
	void rejectsAnUnknownProductWithNotFoundInsteadOfAZeroedBalance() {
		when(productRepositoryPort.get(productId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new GetStockBalanceQuery(productId, null)))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining(productId.toString());
	}

	@Test
	void returnsAvailableAsOnHandMinusReservedForAKnownWarehouse() {
		when(productRepositoryPort.get(productId)).thenReturn(Optional.of(knownProduct()));
		StockBalance balance = StockBalance.of(StockBalanceId.of(UUID.randomUUID()), productId, warehouseId,
				new BigDecimal("50"), new BigDecimal("20"), new BigDecimal("5"), new BigDecimal("10.00"));
		when(stockBalanceRepositoryPort.findByProductIdAndWarehouseId(productId, warehouseId))
				.thenReturn(Optional.of(balance));

		StockBalanceView view = service.execute(new GetStockBalanceQuery(productId, warehouseId));

		assertThat(view.warehouseId()).isEqualTo(warehouseId);
		assertThat(view.onHand()).isEqualByComparingTo("50");
		assertThat(view.reserved()).isEqualByComparingTo("20");
		assertThat(view.inTransit()).isEqualByComparingTo("5");
		assertThat(view.available()).isEqualByComparingTo("30");
		assertThat(view.averageCost()).isEqualByComparingTo("10.00");
	}

	@Test
	void aKnownProductWithNoTrackedBalanceInAWarehouseReadsAsZeroNotNotFound() {
		when(productRepositoryPort.get(productId)).thenReturn(Optional.of(knownProduct()));
		when(stockBalanceRepositoryPort.findByProductIdAndWarehouseId(productId, warehouseId))
				.thenReturn(Optional.empty());

		StockBalanceView view = service.execute(new GetStockBalanceQuery(productId, warehouseId));

		assertThat(view.onHand()).isEqualByComparingTo("0");
		assertThat(view.available()).isEqualByComparingTo("0");
	}

	@Test
	void omittingWarehouseIdAggregatesAcrossEveryWarehouse() {
		when(productRepositoryPort.get(productId)).thenReturn(Optional.of(knownProduct()));
		UUID warehouseA = UUID.randomUUID();
		UUID warehouseB = UUID.randomUUID();
		StockBalance balanceA = StockBalance.of(StockBalanceId.of(UUID.randomUUID()), productId, warehouseA,
				new BigDecimal("30"), new BigDecimal("10"), BigDecimal.ZERO, new BigDecimal("8.00"));
		StockBalance balanceB = StockBalance.of(StockBalanceId.of(UUID.randomUUID()), productId, warehouseB,
				new BigDecimal("70"), new BigDecimal("0"), new BigDecimal("5"), new BigDecimal("12.00"));
		when(stockBalanceRepositoryPort.findByProductId(productId)).thenReturn(List.of(balanceA, balanceB));

		StockBalanceView view = service.execute(new GetStockBalanceQuery(productId, null));

		assertThat(view.warehouseId()).isNull();
		assertThat(view.onHand()).isEqualByComparingTo("100");
		assertThat(view.reserved()).isEqualByComparingTo("10");
		assertThat(view.inTransit()).isEqualByComparingTo("5");
		assertThat(view.available()).isEqualByComparingTo("90");
		assertThat(view.averageCost()).isEqualByComparingTo("10.80");
	}

	@Test
	void omittingWarehouseIdForAKnownProductWithNoBalanceRowsReadsAsZero() {
		when(productRepositoryPort.get(productId)).thenReturn(Optional.of(knownProduct()));
		when(stockBalanceRepositoryPort.findByProductId(productId)).thenReturn(List.of());

		StockBalanceView view = service.execute(new GetStockBalanceQuery(productId, null));

		assertThat(view.onHand()).isEqualByComparingTo("0");
		assertThat(view.available()).isEqualByComparingTo("0");
	}

	private ProductDomain knownProduct() {
		return ProductDomain.builder().id(productId).build();
	}
}
