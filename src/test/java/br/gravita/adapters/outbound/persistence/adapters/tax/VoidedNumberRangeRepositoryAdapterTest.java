package br.gravita.adapters.outbound.persistence.adapters.tax;

import static org.assertj.core.api.Assertions.assertThat;

import br.gravita.adapters.outbound.persistence.entities.tax.VoidedNumberRangeJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.tax.VoidedNumberRangePersistenceMapperImpl;
import br.gravita.adapters.outbound.persistence.repositories.tax.VoidedNumberRangeJpaRepository;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.FiscalDocumentType;
import br.gravita.core.domain.tax.VoidedNumberRange;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({VoidedNumberRangeRepositoryAdapter.class, VoidedNumberRangePersistenceMapperImpl.class})
class VoidedNumberRangeRepositoryAdapterTest {

	@Autowired
	private VoidedNumberRangeRepositoryAdapter repositoryAdapter;

	@Autowired
	private VoidedNumberRangeJpaRepository jpaRepository;

	@Test
	@DisplayName("Finds voided ranges by company, series and date range")
	void ac3_findsVoidedRangesByCompanySeriesAndDateRange() {
		UUID companyId = UUID.randomUUID();
		Instant now = Instant.now();

		save(companyId, "001", now.minus(10, ChronoUnit.DAYS));
		VoidedNumberRangeJpaEntity inRange = save(companyId, "001", now.minus(2, ChronoUnit.DAYS));
		save(companyId, "002", now.minus(2, ChronoUnit.DAYS));
		save(UUID.randomUUID(), "001", now.minus(2, ChronoUnit.DAYS));

		List<VoidedNumberRange> result = repositoryAdapter.findByCompanyIdAndSeriesAndVoidedAtBetween(
				CompanyId.of(companyId), "001", now.minus(5, ChronoUnit.DAYS), now);

		assertThat(result).extracting(VoidedNumberRange::getId)
				.containsExactly(br.gravita.core.domain.tax.VoidedNumberRangeId.of(inRange.getId()));
	}

	@Test
	@DisplayName("Finds the voided ranges of every series within a half-open period, oldest first")
	void findsTheRangesOfEverySeriesVoidedInAHalfOpenPeriodOldestFirst() {
		UUID companyId = UUID.randomUUID();
		Instant from = Instant.parse("2028-02-01T00:00:00Z");
		Instant to = Instant.parse("2028-03-01T00:00:00Z");

		save(companyId, "001", from.minusSeconds(1));
		VoidedNumberRangeJpaEntity late = save(companyId, "002", to.minusSeconds(1));
		VoidedNumberRangeJpaEntity early = save(companyId, "001", from);
		save(companyId, "001", to);
		save(UUID.randomUUID(), "001", from.plusSeconds(60));

		List<VoidedNumberRange> result = repositoryAdapter.findByCompanyIdAndVoidedAtBetween(
				CompanyId.of(companyId), from, to);

		assertThat(result).extracting(range -> range.getId().value()).containsExactly(early.getId(), late.getId());
	}

	private VoidedNumberRangeJpaEntity save(UUID companyId, String series, Instant voidedAt) {
		VoidedNumberRangeJpaEntity entity = VoidedNumberRangeJpaEntity.builder()
				.id(UUID.randomUUID())
				.companyId(companyId)
				.documentType(FiscalDocumentType.NFE)
				.series(series)
				.startNumber(100L)
				.endNumber(110L)
				.justification("test justification")
				.sefazProtocol("protocol-1")
				.voidedAt(voidedAt)
				.build();
		entity.setNew(true);
		return jpaRepository.save(entity);
	}
}
