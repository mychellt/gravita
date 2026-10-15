package br.gravita.inventory.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.inventory.DefaultWarehouse;
import br.gravita.core.domain.inventory.StockBalance;
import br.gravita.core.domain.inventory.StockBalanceId;
import br.gravita.core.domain.shared.Page;
import br.gravita.core.domain.shared.PageQuery;
import br.gravita.core.ports.inbound.inventory.ListStockBalancesQuery;
import br.gravita.core.ports.inbound.inventory.StockBalanceItemView;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.StockBalanceRepositoryPort;
import br.gravita.core.usercases.inventory.ListStockBalancesService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListStockBalancesServiceTest {

	@Mock
	private StockBalanceRepositoryPort stockBalanceRepositoryPort;

	@Mock
	private ProductRepositoryPort productRepositoryPort;

	private ListStockBalancesService service;

	private final UUID productId = UUID.randomUUID();
	private final PageQuery pageQuery = new PageQuery(1, 10);

	@BeforeEach
	void setUp() {
		service = new ListStockBalancesService(stockBalanceRepositoryPort, productRepositoryPort);
	}

	@Test
	@DisplayName("Lists the default warehouse's balances with the product code and keeps the page metadata")
	void listsTheDefaultWarehousesBalancesWithTheProductCode() {
		final StockBalance balance = StockBalance.of(new StockBalanceId(UUID.randomUUID()), productId, DefaultWarehouse.ID,
				new BigDecimal("10"), new BigDecimal("4"), new BigDecimal("1"), new BigDecimal("2.5"));
		when(stockBalanceRepositoryPort.search(DefaultWarehouse.ID, productId, pageQuery))
				.thenReturn(new Page<>(List.of(balance), 1, 10, 11));
		final ProductDomain product = ProductDomain.builder().id(productId).internalCode("SKU-1").build();
		when(productRepositoryPort.findAllByIds(List.of(productId))).thenReturn(List.of(product));

		final Page<StockBalanceItemView> result = service.execute(new ListStockBalancesQuery(productId, pageQuery));

		assertThat(result.page()).isEqualTo(1);
		assertThat(result.size()).isEqualTo(10);
		assertThat(result.totalElements()).isEqualTo(11);
		assertThat(result.content()).singleElement().satisfies(item -> {
			assertThat(item.productCode()).isEqualTo("SKU-1");
			assertThat(item.warehouseId()).isEqualTo(DefaultWarehouse.ID);
			assertThat(item.onHand()).isEqualByComparingTo("10");
			assertThat(item.available()).isEqualByComparingTo("6");
		});
	}

	@Test
	@DisplayName("An empty page returns no content and does not look products up")
	void emptyPageDoesNotLookProductsUp() {
		when(stockBalanceRepositoryPort.search(DefaultWarehouse.ID, null, pageQuery))
				.thenReturn(new Page<>(List.of(), 1, 10, 0));

		final Page<StockBalanceItemView> result = service.execute(new ListStockBalancesQuery(null, pageQuery));

		assertThat(result.content()).isEmpty();
		org.mockito.Mockito.verifyNoInteractions(productRepositoryPort);
	}
}
