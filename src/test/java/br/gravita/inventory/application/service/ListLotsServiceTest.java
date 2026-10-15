package br.gravita.inventory.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.inventory.DefaultWarehouse;
import br.gravita.core.domain.inventory.Lot;
import br.gravita.core.domain.inventory.LotId;
import br.gravita.core.domain.inventory.SerialUnit;
import br.gravita.core.domain.inventory.SerialUnitId;
import br.gravita.core.domain.inventory.SerialUnitStatus;
import br.gravita.core.domain.shared.Page;
import br.gravita.core.domain.shared.PageQuery;
import br.gravita.core.ports.inbound.inventory.ListLotsQuery;
import br.gravita.core.ports.inbound.inventory.StockUnitView;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.LotRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.SerialUnitRepositoryPort;
import br.gravita.core.usercases.inventory.ListLotsService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListLotsServiceTest {

	@Mock
	private LotRepositoryPort lotRepositoryPort;

	@Mock
	private SerialUnitRepositoryPort serialUnitRepositoryPort;

	@Mock
	private ProductRepositoryPort productRepositoryPort;

	private ListLotsService service;

	private final UUID productId = UUID.randomUUID();
	private final PageQuery pageQuery = new PageQuery(0, 20);

	@BeforeEach
	void setUp() {
		service = new ListLotsService(lotRepositoryPort, serialUnitRepositoryPort, productRepositoryPort);
	}

	@Test
	@DisplayName("Lists lots when no kind is given, leaving serial units alone")
	void listsLotsByDefault() {
		final Lot lot = Lot.of(new LotId(UUID.randomUUID()), productId, DefaultWarehouse.ID, "L-1",
				LocalDate.of(2027, 1, 15), new BigDecimal("100"));
		when(lotRepositoryPort.search(DefaultWarehouse.ID, productId, pageQuery))
				.thenReturn(new Page<>(List.of(lot), 0, 20, 1));
		when(productRepositoryPort.findAllByIds(List.of(productId)))
				.thenReturn(List.of(ProductDomain.builder().id(productId).internalCode("SKU-A").build()));

		final Page<StockUnitView> result = service.execute(new ListLotsQuery(productId, null, pageQuery));

		assertThat(result.content()).singleElement().satisfies(unit -> {
			assertThat(unit.kind()).isEqualTo(ListLotsQuery.Kind.LOT);
			assertThat(unit.productCode()).isEqualTo("SKU-A");
			assertThat(unit.code()).isEqualTo("L-1");
			assertThat(unit.expiryDate()).isEqualTo(LocalDate.of(2027, 1, 15));
			assertThat(unit.quantity()).isEqualByComparingTo("100");
			assertThat(unit.status()).isNull();
		});
		verifyNoInteractions(serialUnitRepositoryPort);
	}

	@Test
	@DisplayName("Lists serial units, one each and with their status, when the kind is SERIAL")
	void listsSerialUnitsWhenTheKindIsSerial() {
		final SerialUnit unit = new SerialUnit(new SerialUnitId(UUID.randomUUID()), productId, DefaultWarehouse.ID,
				"SN-1", SerialUnitStatus.ISSUED);
		when(serialUnitRepositoryPort.search(DefaultWarehouse.ID, null, pageQuery))
				.thenReturn(new Page<>(List.of(unit), 0, 20, 1));
		when(productRepositoryPort.findAllByIds(List.of(productId)))
				.thenReturn(List.of(ProductDomain.builder().id(productId).internalCode("SKU-B").build()));

		final Page<StockUnitView> result = service
				.execute(new ListLotsQuery(null, ListLotsQuery.Kind.SERIAL, pageQuery));

		assertThat(result.content()).singleElement().satisfies(view -> {
			assertThat(view.kind()).isEqualTo(ListLotsQuery.Kind.SERIAL);
			assertThat(view.code()).isEqualTo("SN-1");
			assertThat(view.quantity()).isEqualByComparingTo("1");
			assertThat(view.status()).isEqualTo("ISSUED");
			assertThat(view.expiryDate()).isNull();
		});
		verifyNoInteractions(lotRepositoryPort);
	}
}
