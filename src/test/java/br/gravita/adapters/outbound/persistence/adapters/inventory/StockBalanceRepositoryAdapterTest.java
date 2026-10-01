package br.gravita.adapters.outbound.persistence.adapters.inventory;

import br.gravita.adapters.outbound.persistence.entities.inventory.StockBalanceJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.inventory.StockBalancePersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.inventory.StockBalanceJpaRepository;
import br.gravita.core.domain.inventory.StockBalance;
import br.gravita.core.domain.inventory.StockBalanceId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockBalanceRepositoryAdapterTest {

	@Mock
	private StockBalanceJpaRepository repository;

	@Mock
	private StockBalancePersistenceMapper mapper;

	@InjectMocks
	private StockBalanceRepositoryAdapter adapter;

	@Test
	@DisplayName("Finds the balance row for a given product and warehouse")
	void findsTheRowForAGivenProductAndWarehouse() {
		final UUID productId = UUID.randomUUID();
		final UUID warehouseId = UUID.randomUUID();
		final StockBalance balance = buildBalance(productId, warehouseId);
		final StockBalanceJpaEntity entity = buildEntity(balance.getId());
		when(repository.findByProductIdAndWarehouseId(productId, warehouseId)).thenReturn(Optional.of(entity));
		when(mapper.map(entity)).thenReturn(balance);

		final Optional<StockBalance> result = adapter.findByProductIdAndWarehouseId(productId, warehouseId);

		assertThat(result).contains(balance);
		verify(repository).findByProductIdAndWarehouseId(productId, warehouseId);
	}

	@Test
	@DisplayName("Returns empty when no balance row exists for the product and warehouse")
	void returnsEmptyWhenNoRowExistsForThatProductAndWarehouse() {
		final UUID productId = UUID.randomUUID();
		final UUID warehouseId = UUID.randomUUID();
		when(repository.findByProductIdAndWarehouseId(productId, warehouseId)).thenReturn(Optional.empty());

		assertThat(adapter.findByProductIdAndWarehouseId(productId, warehouseId)).isEmpty();
	}

	@Test
	@DisplayName("Finds every warehouse balance row for a product")
	void findsEveryWarehouseRowForAProduct() {
		final UUID productId = UUID.randomUUID();
		final StockBalance first = buildBalance(productId, UUID.randomUUID());
		final StockBalance second = buildBalance(productId, UUID.randomUUID());
		final StockBalanceJpaEntity firstEntity = buildEntity(first.getId());
		final StockBalanceJpaEntity secondEntity = buildEntity(second.getId());
		when(repository.findByProductId(productId)).thenReturn(List.of(firstEntity, secondEntity));
		when(mapper.map(same(firstEntity))).thenReturn(first);
		when(mapper.map(same(secondEntity))).thenReturn(second);

		final List<StockBalance> result = adapter.findByProductId(productId);

		assertThat(result).containsExactly(first, second);
	}

	@Test
	@DisplayName("Finds every balance row of a warehouse across products")
	void findsEveryRowForAGivenWarehouseAcrossProducts() {
		final UUID warehouseId = UUID.randomUUID();
		final StockBalance first = buildBalance(UUID.randomUUID(), warehouseId);
		final StockBalance second = buildBalance(UUID.randomUUID(), warehouseId);
		final StockBalanceJpaEntity firstEntity = buildEntity(first.getId());
		final StockBalanceJpaEntity secondEntity = buildEntity(second.getId());
		when(repository.findByWarehouseId(warehouseId)).thenReturn(List.of(firstEntity, secondEntity));
		when(mapper.map(same(firstEntity))).thenReturn(first);
		when(mapper.map(same(secondEntity))).thenReturn(second);

		final List<StockBalance> result = adapter.findByWarehouseId(warehouseId);

		assertThat(result).containsExactly(first, second);
	}

	@Test
	@DisplayName("Finds every balance row across products and warehouses for a sweep")
	void findsEveryRowAcrossProductsAndWarehousesForASweep() {
		final StockBalance first = buildBalance(UUID.randomUUID(), UUID.randomUUID());
		final StockBalance second = buildBalance(UUID.randomUUID(), UUID.randomUUID());
		final StockBalanceJpaEntity firstEntity = buildEntity(first.getId());
		final StockBalanceJpaEntity secondEntity = buildEntity(second.getId());
		when(repository.findAll()).thenReturn(List.of(firstEntity, secondEntity));
		when(mapper.map(same(firstEntity))).thenReturn(first);
		when(mapper.map(same(secondEntity))).thenReturn(second);

		final List<StockBalance> result = adapter.findAll();

		assertThat(result).containsExactly(first, second);
	}

	@Test
	@DisplayName("Saves a new balance marking its entity as new")
	void shouldSaveNewBalance() {
		final StockBalance balance = buildBalance(UUID.randomUUID(), UUID.randomUUID());
		final StockBalanceJpaEntity entity = buildEntity(balance.getId());
		final StockBalanceJpaEntity saved = buildEntity(balance.getId());
		when(mapper.map(balance)).thenReturn(entity);
		when(repository.existsById(entity.getId())).thenReturn(false);
		when(repository.save(entity)).thenReturn(saved);
		when(mapper.map(saved)).thenReturn(balance);

		final StockBalance result = adapter.save(balance);

		assertThat(result).isSameAs(balance);
		assertThat(entity.isNew()).isTrue();
		verify(repository).save(entity);
	}

	@Test
	@DisplayName("Saves an existing balance marking its entity as not new")
	void shouldSaveExistingBalanceAsNotNew() {
		final StockBalance balance = buildBalance(UUID.randomUUID(), UUID.randomUUID());
		final StockBalanceJpaEntity entity = buildEntity(balance.getId());
		when(mapper.map(balance)).thenReturn(entity);
		when(repository.existsById(entity.getId())).thenReturn(true);
		when(repository.save(entity)).thenReturn(entity);
		when(mapper.map(entity)).thenReturn(balance);

		adapter.save(balance);

		assertThat(entity.isNew()).isFalse();
	}

	private StockBalanceJpaEntity buildEntity(final StockBalanceId id) {
		return StockBalanceJpaEntity.builder().id(id.value()).build();
	}

	private StockBalance buildBalance(final UUID productId, final UUID warehouseId) {
		return StockBalance.of(StockBalanceId.of(UUID.randomUUID()), productId, warehouseId, new BigDecimal("50"),
				new BigDecimal("10"), BigDecimal.ZERO, new BigDecimal("9.00"));
	}
}
