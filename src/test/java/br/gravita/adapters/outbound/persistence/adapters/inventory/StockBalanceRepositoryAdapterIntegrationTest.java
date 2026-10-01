package br.gravita.adapters.outbound.persistence.adapters.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import br.gravita.adapters.outbound.persistence.entities.inventory.StockBalanceJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.inventory.StockBalancePersistenceMapperImpl;
import br.gravita.core.domain.inventory.StockBalance;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({StockBalanceRepositoryAdapter.class, StockBalancePersistenceMapperImpl.class})
class StockBalanceRepositoryAdapterIntegrationTest {

	@Autowired
	private StockBalanceRepositoryAdapter stockBalanceRepositoryAdapter;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	@DisplayName("Finds the balance row for a given product and warehouse")
	void findsTheRowForAGivenProductAndWarehouse() {
		UUID productId = UUID.randomUUID();
		UUID warehouseId = UUID.randomUUID();
		seed(productId, warehouseId, "50", "10", "0", "9.00");
		flushAndClear();

		Optional<StockBalance> found = stockBalanceRepositoryAdapter.findByProductIdAndWarehouseId(productId,
				warehouseId);

		assertThat(found).isPresent();
		assertThat(found.get().getOnHand()).isEqualByComparingTo("50");
		assertThat(found.get().available()).isEqualByComparingTo("40");
	}

	@Test
	@DisplayName("Returns empty when no balance row exists for the product and warehouse")
	void returnsEmptyWhenNoRowExistsForThatProductAndWarehouse() {
		Optional<StockBalance> found = stockBalanceRepositoryAdapter.findByProductIdAndWarehouseId(UUID.randomUUID(),
				UUID.randomUUID());

		assertThat(found).isEmpty();
	}

	@Test
	@DisplayName("Finds every warehouse balance row for a product")
	void findsEveryWarehouseRowForAProduct() {
		UUID productId = UUID.randomUUID();
		seed(productId, UUID.randomUUID(), "30", "0", "0", "8.00");
		seed(productId, UUID.randomUUID(), "70", "5", "0", "12.00");
		seed(UUID.randomUUID(), UUID.randomUUID(), "1000", "0", "0", "1.00");
		flushAndClear();

		List<StockBalance> found = stockBalanceRepositoryAdapter.findByProductId(productId);

		assertThat(found).hasSize(2);
		assertThat(found).extracting(StockBalance::getOnHand)
				.containsExactlyInAnyOrder(new BigDecimal("30.0000"), new BigDecimal("70.0000"));
	}

	@Test
	@DisplayName("Finds every balance row of a warehouse across products")
	void findsEveryRowForAGivenWarehouseAcrossProducts() {
		UUID warehouseId = UUID.randomUUID();
		seed(UUID.randomUUID(), warehouseId, "30", "0", "0", "8.00");
		seed(UUID.randomUUID(), warehouseId, "70", "5", "0", "12.00");
		seed(UUID.randomUUID(), UUID.randomUUID(), "1000", "0", "0", "1.00");
		flushAndClear();

		List<StockBalance> found = stockBalanceRepositoryAdapter.findByWarehouseId(warehouseId);

		assertThat(found).hasSize(2);
	}

	@Test
	@DisplayName("Finds every balance row across products and warehouses for a sweep")
	void findsEveryRowAcrossProductsAndWarehousesForASweep() {
		seed(UUID.randomUUID(), UUID.randomUUID(), "30", "0", "0", "8.00");
		seed(UUID.randomUUID(), UUID.randomUUID(), "70", "5", "0", "12.00");
		flushAndClear();

		List<StockBalance> found = stockBalanceRepositoryAdapter.findAll();

		assertThat(found).hasSize(2);
	}

	private void seed(UUID productId, UUID warehouseId, String onHand, String reserved, String inTransit,
			String averageCost) {
		entityManager.persist(StockBalanceJpaEntity.builder()
				.id(UUID.randomUUID())
				.productId(productId)
				.warehouseId(warehouseId)
				.onHand(new BigDecimal(onHand))
				.reserved(new BigDecimal(reserved))
				.inTransit(new BigDecimal(inTransit))
				.averageCost(new BigDecimal(averageCost))
				.build());
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
