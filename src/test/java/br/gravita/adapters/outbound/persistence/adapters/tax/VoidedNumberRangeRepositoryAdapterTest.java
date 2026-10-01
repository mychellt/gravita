package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.VoidedNumberRangeJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.tax.VoidedNumberRangePersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.tax.VoidedNumberRangeJpaRepository;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.FiscalDocumentType;
import br.gravita.core.domain.tax.VoidedNumberRange;
import br.gravita.core.domain.tax.VoidedNumberRangeId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoidedNumberRangeRepositoryAdapterTest {

	private static final Instant FROM = Instant.parse("2028-02-01T00:00:00Z");
	private static final Instant TO = Instant.parse("2028-03-01T00:00:00Z");

	@Mock
	private VoidedNumberRangeJpaRepository repository;

	@Mock
	private VoidedNumberRangePersistenceMapper mapper;

	@InjectMocks
	private VoidedNumberRangeRepositoryAdapter adapter;

	@Test
	@DisplayName("Saves a voided number range")
	void shouldSaveVoidedNumberRange() {
		final VoidedNumberRange range = buildRange(CompanyId.of(UUID.randomUUID()), "001");
		final VoidedNumberRangeJpaEntity entity = buildEntity(range.getId());
		final VoidedNumberRangeJpaEntity saved = buildEntity(range.getId());
		when(mapper.map(range)).thenReturn(entity);
		when(repository.save(entity)).thenReturn(saved);
		when(mapper.map(saved)).thenReturn(range);

		final VoidedNumberRange result = adapter.save(range);

		assertThat(result).isSameAs(range);
		verify(repository).save(entity);
	}

	@Test
	@DisplayName("Finds the voided ranges of a company")
	void shouldFindByCompanyId() {
		final CompanyId companyId = CompanyId.of(UUID.randomUUID());
		final VoidedNumberRange range = buildRange(companyId, "001");
		final VoidedNumberRangeJpaEntity entity = buildEntity(range.getId());
		when(repository.findByCompanyId(companyId.value())).thenReturn(List.of(entity));
		when(mapper.map(entity)).thenReturn(range);

		final List<VoidedNumberRange> result = adapter.findByCompanyId(companyId);

		assertThat(result).containsExactly(range);
		verify(repository).findByCompanyId(companyId.value());
	}

	@Test
	@DisplayName("Finds voided ranges by company, series and date range")
	void findsVoidedRangesByCompanySeriesAndDateRange() {
		final CompanyId companyId = CompanyId.of(UUID.randomUUID());
		final VoidedNumberRange range = buildRange(companyId, "001");
		final VoidedNumberRangeJpaEntity entity = buildEntity(range.getId());
		when(repository.findByCompanyIdAndSeriesAndVoidedAtBetween(companyId.value(), "001", FROM, TO))
				.thenReturn(List.of(entity));
		when(mapper.map(entity)).thenReturn(range);

		final List<VoidedNumberRange> result = adapter.findByCompanyIdAndSeriesAndVoidedAtBetween(companyId, "001",
				FROM, TO);

		assertThat(result).containsExactly(range);
		verify(repository).findByCompanyIdAndSeriesAndVoidedAtBetween(companyId.value(), "001", FROM, TO);
	}

	@Test
	@DisplayName("Finds the voided ranges of every series within a period, oldest first")
	void findsTheRangesOfEverySeriesVoidedInAPeriod() {
		final CompanyId companyId = CompanyId.of(UUID.randomUUID());
		final VoidedNumberRange range = buildRange(companyId, "002");
		final VoidedNumberRangeJpaEntity entity = buildEntity(range.getId());
		when(repository.findByCompanyIdAndVoidedAtGreaterThanEqualAndVoidedAtLessThanOrderByVoidedAtAscSeriesAscStartNumberAsc(
				companyId.value(), FROM, TO)).thenReturn(List.of(entity));
		when(mapper.map(entity)).thenReturn(range);

		final List<VoidedNumberRange> result = adapter.findByCompanyIdAndVoidedAtBetween(companyId, FROM, TO);

		assertThat(result).containsExactly(range);
	}

	private VoidedNumberRangeJpaEntity buildEntity(final VoidedNumberRangeId id) {
		return VoidedNumberRangeJpaEntity.builder().id(id.value()).build();
	}

	private VoidedNumberRange buildRange(final CompanyId companyId, final String series) {
		return VoidedNumberRange.of(VoidedNumberRangeId.of(UUID.randomUUID()), companyId, FiscalDocumentType.NFE,
				series, 100L, 110L, "test justification", "protocol-1", Instant.parse("2028-02-10T00:00:00Z"));
	}
}
