package br.gravita.adapters.outbound.persistence.adapters.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.adapters.outbound.persistence.entities.inventory.StockMovementJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.inventory.StockMovementPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.inventory.StockMovementJpaRepository;
import br.gravita.core.domain.inventory.DefaultWarehouse;
import br.gravita.core.domain.inventory.StockMovement;
import br.gravita.core.domain.inventory.StockMovementType;
import br.gravita.core.domain.shared.Page;
import br.gravita.core.domain.shared.PageQuery;
import br.gravita.core.ports.inbound.inventory.ListStockMovementsQuery;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class StockMovementRepositoryAdapterSearchTest {

	@Mock
	private StockMovementJpaRepository repository;

	@Mock
	private StockMovementPersistenceMapper mapper;

	@InjectMocks
	private StockMovementRepositoryAdapter adapter;

	@Test
	@DisplayName("Searches newest first, with the filters and the requested page, and returns the page metadata")
	void searchesNewestFirstWithTheFiltersAndTheRequestedPage() {
		final UUID productId = UUID.randomUUID();
		final Instant from = Instant.parse("2026-10-01T00:00:00Z");
		final StockMovementJpaEntity entity = new StockMovementJpaEntity();
		final StockMovement movement = org.mockito.Mockito.mock(StockMovement.class);
		when(repository.search(eq(DefaultWarehouse.ID), eq(productId), eq(StockMovementType.ENTRY), eq(from),
				eq(null), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(entity), PageRequest.of(2, 5), 11));
		when(mapper.map(entity)).thenReturn(movement);

		final Page<StockMovement> result = adapter.search(DefaultWarehouse.ID, new ListStockMovementsQuery(productId,
				StockMovementType.ENTRY, from, null, new PageQuery(2, 5)));

		final ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(repository).search(eq(DefaultWarehouse.ID), eq(productId), eq(StockMovementType.ENTRY), eq(from),
				eq(null), pageable.capture());
		assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
		assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
		assertThat(pageable.getValue().getSort().getOrderFor("timestamp").isDescending()).isTrue();
		assertThat(result.content()).containsExactly(movement);
		assertThat(result.page()).isEqualTo(2);
		assertThat(result.size()).isEqualTo(5);
		assertThat(result.totalElements()).isEqualTo(11);
	}
}
