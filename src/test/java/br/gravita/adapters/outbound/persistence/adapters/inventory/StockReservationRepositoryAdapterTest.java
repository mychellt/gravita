package br.gravita.adapters.outbound.persistence.adapters.inventory;

import br.gravita.adapters.outbound.persistence.entities.inventory.StockReservationJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.inventory.StockReservationPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.inventory.StockReservationJpaRepository;
import br.gravita.core.domain.inventory.StockReservation;
import br.gravita.core.domain.inventory.StockReservationId;
import br.gravita.core.domain.inventory.StockReservationStatus;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockReservationRepositoryAdapterTest {

	@Mock
	private StockReservationJpaRepository repository;

	@Mock
	private StockReservationPersistenceMapper mapper;

	@InjectMocks
	private StockReservationRepositoryAdapter adapter;

	@Test
	@DisplayName("Saves a new reservation marking its entity as new")
	void savesANewReservation() {
		final StockReservation reservation = buildReservation(UUID.randomUUID());
		final StockReservationJpaEntity entity = buildEntity(reservation.getId());
		final StockReservationJpaEntity saved = buildEntity(reservation.getId());
		when(mapper.map(reservation)).thenReturn(entity);
		when(repository.existsById(reservation.getId().value())).thenReturn(false);
		when(repository.save(entity)).thenReturn(saved);
		when(mapper.map(saved)).thenReturn(reservation);

		final StockReservation result = adapter.save(reservation);

		assertThat(result).isSameAs(reservation);
		assertThat(entity.isNew()).isTrue();
		verify(repository).save(entity);
	}

	@Test
	@DisplayName("Saves an existing reservation marking its entity as not new")
	void savesAnExistingReservationAsNotNew() {
		final StockReservation reservation = buildReservation(UUID.randomUUID());
		final StockReservationJpaEntity entity = buildEntity(reservation.getId());
		when(mapper.map(reservation)).thenReturn(entity);
		when(repository.existsById(reservation.getId().value())).thenReturn(true);
		when(repository.save(entity)).thenReturn(entity);
		when(mapper.map(entity)).thenReturn(reservation);

		adapter.save(reservation);

		assertThat(entity.isNew()).isFalse();
	}

	@Test
	@DisplayName("Finds a reservation by its id")
	void findsAReservationById() {
		final StockReservation reservation = buildReservation(UUID.randomUUID());
		final StockReservationJpaEntity entity = buildEntity(reservation.getId());
		when(repository.findById(reservation.getId().value())).thenReturn(Optional.of(entity));
		when(mapper.map(entity)).thenReturn(reservation);

		final Optional<StockReservation> result = adapter.findById(reservation.getId());

		assertThat(result).contains(reservation);
		verify(repository).findById(reservation.getId().value());
	}

	@Test
	@DisplayName("Returns empty when no reservation exists for the given id")
	void returnsEmptyWhenNoReservationExistsForThatId() {
		final StockReservationId id = StockReservationId.of(UUID.randomUUID());
		when(repository.findById(id.value())).thenReturn(Optional.empty());

		assertThat(adapter.findById(id)).isEmpty();
	}

	@Test
	@DisplayName("Finds the active reservations for a given order reference")
	void findsTheActiveReservationsForAGivenOrderRef() {
		final UUID orderRef = UUID.randomUUID();
		final StockReservation reservation = buildReservation(orderRef);
		final StockReservationJpaEntity entity = buildEntity(reservation.getId());
		when(repository.findByOrderRefAndStatus(orderRef, StockReservationStatus.ACTIVE)).thenReturn(List.of(entity));
		when(mapper.map(entity)).thenReturn(reservation);

		final List<StockReservation> result = adapter.findActiveByOrderRef(orderRef);

		assertThat(result).containsExactly(reservation);
		verify(repository).findByOrderRefAndStatus(orderRef, StockReservationStatus.ACTIVE);
	}

	private StockReservationJpaEntity buildEntity(final StockReservationId id) {
		return StockReservationJpaEntity.builder().id(id.value()).build();
	}

	private StockReservation buildReservation(final UUID orderRef) {
		return StockReservation.create(StockReservationId.of(UUID.randomUUID()), orderRef, UUID.randomUUID(),
				UUID.randomUUID(), new BigDecimal("15"));
	}
}
