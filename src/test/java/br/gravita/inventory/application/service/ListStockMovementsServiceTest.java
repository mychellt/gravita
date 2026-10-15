package br.gravita.inventory.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.inventory.DefaultWarehouse;
import br.gravita.core.domain.inventory.StockMovement;
import br.gravita.core.domain.inventory.StockMovementId;
import br.gravita.core.domain.inventory.StockMovementType;
import br.gravita.core.domain.shared.Page;
import br.gravita.core.domain.shared.PageQuery;
import br.gravita.core.ports.inbound.inventory.ListStockMovementsQuery;
import br.gravita.core.ports.inbound.inventory.StockMovementItemView;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.StockMovementRepositoryPort;
import br.gravita.core.usercases.inventory.ListStockMovementsService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListStockMovementsServiceTest {

	@Mock
	private StockMovementRepositoryPort stockMovementRepositoryPort;

	@Mock
	private ProductRepositoryPort productRepositoryPort;

	private ListStockMovementsService service;

	private final UUID productId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		service = new ListStockMovementsService(stockMovementRepositoryPort, productRepositoryPort);
	}

	@Test
	@DisplayName("Passes every filter through, scoped to the default warehouse, and adds the product code")
	void passesEveryFilterThroughScopedToTheDefaultWarehouse() {
		final ListStockMovementsQuery query = new ListStockMovementsQuery(productId, StockMovementType.EXIT,
				Instant.parse("2026-10-01T00:00:00Z"), Instant.parse("2026-10-31T23:59:59Z"), new PageQuery(0, 20));
		final StockMovement movement = StockMovement.builder().id(new StockMovementId(UUID.randomUUID()))
				.type(StockMovementType.EXIT).productId(productId).warehouseId(DefaultWarehouse.ID)
				.quantity(new BigDecimal("5")).unitCost(new BigDecimal("3")).originReference("SO-1")
				.user(UUID.randomUUID()).timestamp(Instant.parse("2026-10-08T15:30:00Z")).build();
		when(stockMovementRepositoryPort.search(DefaultWarehouse.ID, query))
				.thenReturn(new Page<>(List.of(movement), 0, 20, 1));
		when(productRepositoryPort.findAllByIds(List.of(productId)))
				.thenReturn(List.of(ProductDomain.builder().id(productId).internalCode("SKU-9").build()));

		final Page<StockMovementItemView> result = service.execute(query);

		assertThat(result.totalElements()).isEqualTo(1);
		assertThat(result.content()).singleElement().satisfies(item -> {
			assertThat(item.movement()).isSameAs(movement);
			assertThat(item.productCode()).isEqualTo("SKU-9");
		});
	}
}
