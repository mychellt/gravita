package br.gravita.inventory.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.inventory.DefaultWarehouse;
import br.gravita.core.domain.inventory.StockReservation;
import br.gravita.core.domain.inventory.StockReservationId;
import br.gravita.core.domain.inventory.StockReservationStatus;
import br.gravita.core.domain.shared.Page;
import br.gravita.core.domain.shared.PageQuery;
import br.gravita.core.ports.inbound.inventory.ListStockReservationsQuery;
import br.gravita.core.ports.inbound.inventory.StockReservationItemView;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.StockReservationRepositoryPort;
import br.gravita.core.usercases.inventory.ListStockReservationsService;
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
class ListStockReservationsServiceTest {

	@Mock
	private StockReservationRepositoryPort stockReservationRepositoryPort;

	@Mock
	private ProductRepositoryPort productRepositoryPort;

	private ListStockReservationsService service;

	private final UUID productId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		service = new ListStockReservationsService(stockReservationRepositoryPort, productRepositoryPort);
	}

	@Test
	@DisplayName("Lists the default warehouse's reservations matching the filters, with the product code")
	void listsTheDefaultWarehousesReservationsWithTheProductCode() {
		final ListStockReservationsQuery query = new ListStockReservationsQuery(StockReservationStatus.ACTIVE,
				productId, new PageQuery(0, 20));
		final StockReservation reservation = StockReservation.create(new StockReservationId(UUID.randomUUID()),
				UUID.randomUUID(), productId, DefaultWarehouse.ID, new BigDecimal("4"));
		when(stockReservationRepositoryPort.search(DefaultWarehouse.ID, query))
				.thenReturn(new Page<>(List.of(reservation), 0, 20, 1));
		when(productRepositoryPort.findAllByIds(List.of(productId)))
				.thenReturn(List.of(ProductDomain.builder().id(productId).internalCode("SKU-C").build()));

		final Page<StockReservationItemView> result = service.execute(query);

		assertThat(result.content()).singleElement().satisfies(item -> {
			assertThat(item.reservation()).isSameAs(reservation);
			assertThat(item.productCode()).isEqualTo("SKU-C");
		});
	}
}
