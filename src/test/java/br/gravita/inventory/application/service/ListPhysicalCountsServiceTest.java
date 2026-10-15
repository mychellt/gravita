package br.gravita.inventory.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.inventory.DefaultWarehouse;
import br.gravita.core.domain.inventory.PhysicalCount;
import br.gravita.core.domain.inventory.PhysicalCountId;
import br.gravita.core.domain.inventory.PhysicalCountLine;
import br.gravita.core.domain.inventory.PhysicalCountScope;
import br.gravita.core.domain.inventory.PhysicalCountStatus;
import br.gravita.core.domain.shared.Page;
import br.gravita.core.domain.shared.PageQuery;
import br.gravita.core.ports.inbound.inventory.ListPhysicalCountsQuery;
import br.gravita.core.ports.inbound.inventory.PhysicalCountItemView;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.PhysicalCountRepositoryPort;
import br.gravita.core.usercases.inventory.ListPhysicalCountsService;
import java.math.BigDecimal;
import java.time.Instant;
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
class ListPhysicalCountsServiceTest {

	@Mock
	private PhysicalCountRepositoryPort physicalCountRepositoryPort;

	@Mock
	private ProductRepositoryPort productRepositoryPort;

	private ListPhysicalCountsService service;

	private final UUID productA = UUID.randomUUID();
	private final UUID productB = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		service = new ListPhysicalCountsService(physicalCountRepositoryPort, productRepositoryPort);
	}

	@Test
	@DisplayName("Looks the products of every count's lines up in one read, without repeating shared products")
	void looksTheProductsOfAllLinesUpInOneRead() {
		final ListPhysicalCountsQuery query = new ListPhysicalCountsQuery(PhysicalCountStatus.IN_PROGRESS,
				new PageQuery(0, 20));
		final PhysicalCount first = count(new PhysicalCountLine(productA, new BigDecimal("10")),
				new PhysicalCountLine(productB, new BigDecimal("5"), new BigDecimal("4")));
		final PhysicalCount second = count(new PhysicalCountLine(productA, new BigDecimal("10")));
		when(physicalCountRepositoryPort.search(DefaultWarehouse.ID, query))
				.thenReturn(new Page<>(List.of(first, second), 0, 20, 2));
		when(productRepositoryPort.findAllByIds(anyCollection())).thenReturn(List.of(
				ProductDomain.builder().id(productA).internalCode("SKU-A").build(),
				ProductDomain.builder().id(productB).internalCode("SKU-B").build()));

		final Page<PhysicalCountItemView> result = service.execute(query);

		@SuppressWarnings("unchecked")
		final ArgumentCaptor<java.util.Collection<UUID>> captor = ArgumentCaptor.forClass(java.util.Collection.class);
		verify(productRepositoryPort).findAllByIds(captor.capture());
		assertThat(captor.getValue()).containsExactlyInAnyOrder(productA, productB);
		assertThat(result.content()).hasSize(2);
		assertThat(result.content().get(0).count()).isSameAs(first);
		assertThat(result.content().get(0).productCodes()).containsEntry(productA, "SKU-A").containsEntry(productB,
				"SKU-B");
	}

	private PhysicalCount count(final PhysicalCountLine... lines) {
		return PhysicalCount.start(new PhysicalCountId(UUID.randomUUID()), PhysicalCountScope.TOTAL, null,
				DefaultWarehouse.ID, UUID.randomUUID(), Instant.parse("2026-10-14T08:00:00Z"), List.of(lines));
	}
}
