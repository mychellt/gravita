package br.gravita.adapters.outbound.persistence.adapters.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.SalespersonTargetJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.sales.SalespersonTargetPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.sales.SalespersonTargetJpaRepository;
import br.gravita.core.domain.sales.SalespersonTarget;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalespersonTargetRepositoryAdapterTest {

	private static final YearMonth MONTH = YearMonth.of(2026, 1);

	@Mock
	private SalespersonTargetJpaRepository repository;

	@Mock
	private SalespersonTargetPersistenceMapper mapper;

	@InjectMocks
	private SalespersonTargetRepositoryAdapter adapter;

	@Test
	@DisplayName("Saves a target for a month that has none, generating a new id")
	void shouldSaveNewTargetGeneratingANewId() {
		final SalespersonTarget target = buildTarget(UUID.randomUUID(), MONTH);
		final SalespersonTargetJpaEntity entity = SalespersonTargetJpaEntity.builder().id(UUID.randomUUID()).build();
		final SalespersonTargetJpaEntity saved = SalespersonTargetJpaEntity.builder().id(entity.getId()).build();
		final ArgumentCaptor<UUID> generatedId = ArgumentCaptor.forClass(UUID.class);
		when(repository.findBySalespersonIdAndMonth(target.salespersonId(), "2026-01")).thenReturn(Optional.empty());
		when(mapper.map(eq(target), generatedId.capture())).thenReturn(entity);
		when(repository.existsById(any(UUID.class))).thenReturn(false);
		when(repository.save(entity)).thenReturn(saved);
		when(mapper.map(saved)).thenReturn(target);

		final SalespersonTarget result = adapter.save(target);

		assertThat(result).isSameAs(target);
		assertThat(entity.isNew()).isTrue();
		verify(repository).existsById(generatedId.getValue());
		verify(repository).save(entity);
	}

	@Test
	@DisplayName("Overwrites the existing target when one is set again for the same month")
	void settingATargetForAMonthThatAlreadyHasOneOverwritesIt() {
		final SalespersonTarget target = buildTarget(UUID.randomUUID(), MONTH);
		final SalespersonTargetJpaEntity existing = SalespersonTargetJpaEntity.builder().id(UUID.randomUUID()).build();
		final SalespersonTargetJpaEntity entity = SalespersonTargetJpaEntity.builder().id(existing.getId()).build();
		when(repository.findBySalespersonIdAndMonth(target.salespersonId(), "2026-01"))
				.thenReturn(Optional.of(existing));
		when(mapper.map(target, existing.getId())).thenReturn(entity);
		when(repository.existsById(existing.getId())).thenReturn(true);
		when(repository.save(entity)).thenReturn(entity);
		when(mapper.map(entity)).thenReturn(target);

		adapter.save(target);

		assertThat(entity.isNew()).isFalse();
		verify(repository).save(entity);
	}

	@Test
	@DisplayName("Finds the target of a salesperson for a month")
	void shouldFindBySalespersonAndMonth() {
		final UUID salespersonId = UUID.randomUUID();
		final SalespersonTarget target = buildTarget(salespersonId, MONTH);
		final SalespersonTargetJpaEntity entity = SalespersonTargetJpaEntity.builder().id(UUID.randomUUID()).build();
		when(repository.findBySalespersonIdAndMonth(salespersonId, "2026-01")).thenReturn(Optional.of(entity));
		when(mapper.map(entity)).thenReturn(target);

		final Optional<SalespersonTarget> result = adapter.findBySalespersonAndMonth(salespersonId, MONTH);

		assertThat(result).contains(target);
		verify(repository).findBySalespersonIdAndMonth(salespersonId, "2026-01");
	}

	@Test
	@DisplayName("Returns empty when no target exists for the salesperson and month")
	void findBySalespersonAndMonthReturnsEmptyWhenNoTargetExists() {
		final UUID salespersonId = UUID.randomUUID();
		when(repository.findBySalespersonIdAndMonth(salespersonId, "2026-01")).thenReturn(Optional.empty());

		assertThat(adapter.findBySalespersonAndMonth(salespersonId, MONTH)).isEmpty();
	}

	@Test
	@DisplayName("Returns every target of the requested month")
	void findByMonthReturnsEveryTargetOfThatMonth() {
		final SalespersonTarget ana = buildTarget(UUID.randomUUID(), MONTH);
		final SalespersonTarget bruno = buildTarget(UUID.randomUUID(), MONTH);
		final SalespersonTargetJpaEntity anaEntity = SalespersonTargetJpaEntity.builder().id(UUID.randomUUID()).build();
		final SalespersonTargetJpaEntity brunoEntity = SalespersonTargetJpaEntity.builder().id(UUID.randomUUID()).build();
		when(repository.findByMonth("2026-01")).thenReturn(List.of(anaEntity, brunoEntity));
		when(mapper.map(same(anaEntity))).thenReturn(ana);
		when(mapper.map(same(brunoEntity))).thenReturn(bruno);

		final List<SalespersonTarget> result = adapter.findByMonth(MONTH);

		assertThat(result).containsExactly(ana, bruno);
	}

	private SalespersonTarget buildTarget(final UUID salespersonId, final YearMonth month) {
		return new SalespersonTarget(salespersonId, month, new BigDecimal("15000.00"), 30);
	}
}
