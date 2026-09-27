package br.gravita.adapters.outbound.persistence.adapters.tax;

import static org.assertj.core.api.Assertions.assertThat;

import br.gravita.adapters.outbound.persistence.mappers.tax.VoidedNumberRangePersistenceMapperImpl;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.tax.VoidedNumberRange;
import br.gravita.core.domain.tax.VoidedNumberRangeId;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({VoidedNumberRangeRepositoryAdapter.class, VoidedNumberRangePersistenceMapperImpl.class})
class VoidedNumberRangeRepositoryAdapterTest {

	@Autowired
	private VoidedNumberRangeRepositoryAdapter repositoryAdapter;

	@Test
	void shouldPersistAVoidedNumberRange() {
		VoidedNumberRange voidedNumberRange = voidedNumberRange(CompanyId.of(UUID.randomUUID()), "001", 100L, 110L,
				Instant.now());

		VoidedNumberRange saved = repositoryAdapter.save(voidedNumberRange);

		assertThat(saved.getSeries()).isEqualTo("001");
		assertThat(saved.getStartNumber()).isEqualTo(100L);
		assertThat(saved.getEndNumber()).isEqualTo(110L);
		assertThat(saved.getJustification()).isEqualTo(voidedNumberRange.getJustification());
		assertThat(saved.getProtocol()).isEqualTo(voidedNumberRange.getProtocol());
	}

	@Test
	void shouldFilterByCompanySeriesAndDateRange() {
		CompanyId companyId = CompanyId.of(UUID.randomUUID());
		Instant now = Instant.now();
		VoidedNumberRange matching = repositoryAdapter.save(voidedNumberRange(companyId, "001", 1L, 10L, now));
		repositoryAdapter.save(voidedNumberRange(companyId, "002", 1L, 10L, now));
		repositoryAdapter.save(voidedNumberRange(companyId, "001", 1L, 10L, now.minus(30, ChronoUnit.DAYS)));
		repositoryAdapter.save(voidedNumberRange(CompanyId.of(UUID.randomUUID()), "001", 1L, 10L, now));

		var result = repositoryAdapter.findByCompanyIdAndSeriesAndDateRange(companyId, "001",
				now.minus(1, ChronoUnit.DAYS), now.plus(1, ChronoUnit.DAYS));

		assertThat(result).extracting(VoidedNumberRange::getId).containsExactly(matching.getId());
	}

	@Test
	void aNullSeriesMatchesAnySeriesOfTheCompany() {
		CompanyId companyId = CompanyId.of(UUID.randomUUID());
		Instant now = Instant.now();
		VoidedNumberRange first = repositoryAdapter.save(voidedNumberRange(companyId, "001", 1L, 10L, now));
		VoidedNumberRange second = repositoryAdapter.save(voidedNumberRange(companyId, "002", 1L, 10L, now));

		var result = repositoryAdapter.findByCompanyIdAndSeriesAndDateRange(companyId, null, null, null);

		assertThat(result).extracting(VoidedNumberRange::getId).containsExactlyInAnyOrder(first.getId(),
				second.getId());
	}

	private VoidedNumberRange voidedNumberRange(CompanyId companyId, String series, Long startNumber, Long endNumber,
			Instant voidedAt) {
		return VoidedNumberRange.of(VoidedNumberRangeId.of(UUID.randomUUID()), companyId, series, startNumber,
				endNumber, "justification for " + series, "protocol-" + UUID.randomUUID(), voidedAt);
	}
}
