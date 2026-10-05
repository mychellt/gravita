package br.gravita.inventory.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.ClassificationDomain;
import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.ProductStatus;
import br.gravita.core.domain.ProductType;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.inventory.PhysicalCount;
import br.gravita.core.domain.inventory.PhysicalCountId;
import br.gravita.core.domain.inventory.PhysicalCountScope;
import br.gravita.core.domain.inventory.PhysicalCountStatus;
import br.gravita.core.domain.inventory.StockBalance;
import br.gravita.core.domain.inventory.StockBalanceId;
import br.gravita.core.ports.inbound.inventory.StartPhysicalCountCommand;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.PhysicalCountRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.StockBalanceRepositoryPort;
import br.gravita.core.usercases.inventory.StartPhysicalCountService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StartPhysicalCountServiceTest {

	@Mock
	private StockBalanceRepositoryPort stockBalanceRepositoryPort;

	@Mock
	private ProductRepositoryPort productRepositoryPort;

	@Mock
	private PhysicalCountRepositoryPort physicalCountRepositoryPort;

	private StartPhysicalCountService service;

	private final UUID warehouseId = UUID.randomUUID();
	private final UUID user = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		service = new StartPhysicalCountService(stockBalanceRepositoryPort, productRepositoryPort,
				physicalCountRepositoryPort);
	}

	@Test
	@DisplayName("A TOTAL count snapshots every product in the warehouse")
	void totalScopeSnapshotsEveryProductInTheWarehouse() {
		final UUID productA = UUID.randomUUID();
		final UUID productB = UUID.randomUUID();
		when(stockBalanceRepositoryPort.findByWarehouseId(warehouseId)).thenReturn(List.of(
				balance(productA, "30"),
				balance(productB, "70")));
		when(physicalCountRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final PhysicalCount result = service
				.execute(new StartPhysicalCountCommand(PhysicalCountScope.TOTAL, null, warehouseId, user));

		assertThat(result.getStatus()).isEqualTo(PhysicalCountStatus.IN_PROGRESS);
		assertThat(result.getLines()).extracting(line -> line.productId()).containsExactlyInAnyOrder(productA,
				productB);
		verifyNoInteractions(productRepositoryPort);
	}

	@Test
	@DisplayName("A PARTIAL_BY_GROUP count snapshots only the products of that group")
	void partialByGroupScopeOnlySnapshotsProductsInThatGroup() {
		final UUID productInGroup = UUID.randomUUID();
		final UUID productOutsideGroup = UUID.randomUUID();
		when(stockBalanceRepositoryPort.findByWarehouseId(warehouseId)).thenReturn(List.of(
				balance(productInGroup, "15"),
				balance(productOutsideGroup, "25")));
		when(productRepositoryPort.findAll()).thenReturn(List.of(
				productIn(productInGroup, "Beverages"),
				productIn(productOutsideGroup, "Snacks")));
		when(physicalCountRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final PhysicalCount result = service.execute(
				new StartPhysicalCountCommand(PhysicalCountScope.PARTIAL_BY_GROUP, "Beverages", warehouseId, user));

		assertThat(result.getLines()).hasSize(1);
		assertThat(result.getLines().get(0).productId()).isEqualTo(productInGroup);
		assertThat(result.getLines().get(0).systemQuantity()).isEqualByComparingTo("15");
	}

	@Test
	@DisplayName("A PARTIAL_BY_GROUP count without a product group id is rejected")
	void partialByGroupWithoutAProductGroupIdIsRejected() {
		assertThatThrownBy(() -> service
				.execute(new StartPhysicalCountCommand(PhysicalCountScope.PARTIAL_BY_GROUP, null, warehouseId, user)))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("The saved count always starts IN_PROGRESS")
	void savedCountAlwaysStartsInProgress() {
		when(stockBalanceRepositoryPort.findByWarehouseId(warehouseId)).thenReturn(List.of());
		final ArgumentCaptor<PhysicalCount> captor = ArgumentCaptor.forClass(PhysicalCount.class);
		when(physicalCountRepositoryPort.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(new StartPhysicalCountCommand(PhysicalCountScope.TOTAL, null, warehouseId, user));

		verify(physicalCountRepositoryPort).save(any());
		assertThat(captor.getValue().getStatus()).isEqualTo(PhysicalCountStatus.IN_PROGRESS);
		assertThat(captor.getValue().getId()).isEqualTo(PhysicalCountId.of(captor.getValue().getId().value()));
	}

	private StockBalance balance(final UUID productId, final String onHand) {
		return StockBalance.of(StockBalanceId.of(UUID.randomUUID()), productId, warehouseId, new BigDecimal(onHand),
				BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
	}

	private ProductDomain productIn(final UUID productId, final String group) {
		return ProductDomain.builder()
				.id(productId)
				.internalCode("SKU-" + productId)
				.type(ProductType.SIMPLE)
				.status(ProductStatus.ACTIVE)
				.classification(new ClassificationDomain(group, null, null, null))
				.build();
	}
}
