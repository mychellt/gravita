package br.gravita.adapters.outbound.persistence.adapters.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import br.gravita.adapters.outbound.persistence.mappers.inventory.StockReservationPersistenceMapperImpl;
import br.gravita.core.domain.inventory.StockReservation;
import br.gravita.core.domain.inventory.StockReservationId;
import br.gravita.core.domain.inventory.StockReservationStatus;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({StockReservationRepositoryAdapter.class, StockReservationPersistenceMapperImpl.class})
class StockReservationRepositoryAdapterIntegrationTest {

	@Autowired
	private StockReservationRepositoryAdapter stockReservationRepositoryAdapter;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void savesANewReservationTraceableByOrderRef() {
		StockReservationId id = StockReservationId.of(UUID.randomUUID());
		UUID orderRef = UUID.randomUUID();
		StockReservation reservation = StockReservation.create(id, orderRef, UUID.randomUUID(), UUID.randomUUID(),
				new BigDecimal("15"));

		StockReservation saved = stockReservationRepositoryAdapter.save(reservation);
		entityManager.flush();
		entityManager.clear();

		Optional<StockReservation> found = stockReservationRepositoryAdapter.findById(id);
		assertThat(found).isPresent();
		assertThat(found.get().getOrderRef()).isEqualTo(orderRef);
		assertThat(found.get().getQuantity()).isEqualByComparingTo("15");
		assertThat(found.get().getStatus()).isEqualTo(StockReservationStatus.ACTIVE);
		assertThat(saved.getOrderRef()).isEqualTo(orderRef);
	}

	@Test
	void returnsEmptyWhenNoReservationExistsForThatId() {
		Optional<StockReservation> found = stockReservationRepositoryAdapter.findById(
				StockReservationId.of(UUID.randomUUID()));

		assertThat(found).isEmpty();
	}
}
