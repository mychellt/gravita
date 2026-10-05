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
import org.junit.jupiter.api.DisplayName;
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
	@DisplayName("An unknown product raises not-found instead of returning a zeroed balance")
	void rejectsAnUnknownProductWithNotFoundInsteadOfAZeroedBalance() {
		when(productRepositoryPort.get(productId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new GetStockBalanceQuery(productId, null)))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining(productId.toString());
	}

	@Test
	@DisplayName("Returns available as on-hand minus reserved for a known warehouse")
	void returnsAvailableAsOnHandMinusReservedForAKnownWarehouse() {
		when(productRepositoryPort.get(productId)).thenReturn(Optional.of(knownProduct()));
		final StockBalance balance = StockBalance.of(StockBalanceId.of(UUID.randomUUID()), productId, warehouseId,
				new BigDecimal("50"), new BigDecimal("20"), new BigDecimal("5"), new BigDecimal("10.00"));
		when(stockBalanceRepositoryPort.findByProductIdAndWarehouseId(productId, warehouseId))
				.thenReturn(Optional.of(balance));

		final StockBalanceView view = service.execute(new GetStockBalanceQuery(productId, warehouseId));

		assertThat(view.warehouseId()).isEqualTo(warehouseId);
		assertThat(view.onHand()).isEqualByComparingTo("50");
		assertThat(view.reserved()).isEqualByComparingTo("20");
		assertThat(view.inTransit()).isEqualByComparingTo("5");
		assertThat(view.available()).isEqualByComparingTo("30");
		assertThat(view.averageCost()).isEqualByComparingTo("10.00");
	}

	@Test
	@DisplayName("A known product with no balance in the warehouse reads as zero rather than not-found")
	void knownProductWithNoTrackedBalanceInAWarehouseReadsAsZeroNotNotFound() {
		when(productRepositoryPort.get(productId)).thenReturn(Optional.of(knownProduct()));
		when(stockBalanceRepositoryPort.findByProductIdAndWarehouseId(productId, warehouseId))
				.thenReturn(Optional.empty());

		final StockBalanceView view = service.execute(new GetStockBalanceQuery(productId, warehouseId));

		assertThat(view.onHand()).isEqualByComparingTo("0");
		assertThat(view.available()).isEqualByComparingTo("0");
	}

	@Test
	@DisplayName("Omitting the warehouse id sums the balances of every warehouse")
	void omittingWarehouseIdAggregatesAcrossEveryWarehouse() {
		when(productRepositoryPort.get(productId)).thenReturn(Optional.of(knownProduct()));
		final UUID warehouseA = UUID.randomUUID();
		final UUID warehouseB = UUID.randomUUID();
		final StockBalance balanceA = StockBalance.of(StockBalanceId.of(UUID.randomUUID()), productId, warehouseA,
				new BigDecimal("30"), new BigDecimal("10"), BigDecimal.ZERO, new BigDecimal("8.00"));
		final StockBalance balanceB = StockBalance.of(StockBalanceId.of(UUID.randomUUID()), productId, warehouseB,
				new BigDecimal("70"), new BigDecimal("0"), new BigDecimal("5"), new BigDecimal("12.00"));
		when(stockBalanceRepositoryPort.findByProductId(productId)).thenReturn(List.of(balanceA, balanceB));

		final StockBalanceView view = service.execute(new GetStockBalanceQuery(productId, null));

		assertThat(view.warehouseId()).isNull();
		assertThat(view.onHand()).isEqualByComparingTo("100");
		assertThat(view.reserved()).isEqualByComparingTo("10");
		assertThat(view.inTransit()).isEqualByComparingTo("5");
		assertThat(view.available()).isEqualByComparingTo("90");
		assertThat(view.averageCost()).isEqualByComparingTo("10.80");
	}

	@Test
	@DisplayName("Omitting the warehouse id for a known product with no balance rows reads as zero")
	void omittingWarehouseIdForAKnownProductWithNoBalanceRowsReadsAsZero() {
		when(productRepositoryPort.get(productId)).thenReturn(Optional.of(knownProduct()));
		when(stockBalanceRepositoryPort.findByProductId(productId)).thenReturn(List.of());

		final StockBalanceView view = service.execute(new GetStockBalanceQuery(productId, null));

		assertThat(view.onHand()).isEqualByComparingTo("0");
		assertThat(view.available()).isEqualByComparingTo("0");
	}

	private ProductDomain knownProduct() {
		return ProductDomain.builder().id(productId).build();
	}
}
