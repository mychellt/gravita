package br.gravita.adapters.outbound.persistence.adapters.sales;

import static org.assertj.core.api.Assertions.assertThat;

import br.gravita.adapters.outbound.persistence.mappers.sales.SalespersonTargetPersistenceMapperImpl;
import br.gravita.core.domain.sales.SalespersonTarget;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({SalespersonTargetRepositoryAdapter.class, SalespersonTargetPersistenceMapperImpl.class})
class SalespersonTargetRepositoryAdapterTest {

	@Autowired
	private SalespersonTargetRepositoryAdapter repositoryAdapter;

	@Test
	@DisplayName("Persists a salesperson target and reloads it intact")
	void shouldPersistAndReloadASalespersonTarget() {
		UUID salespersonId = UUID.randomUUID();
		YearMonth month = YearMonth.of(2026, 1);
		SalespersonTarget target = new SalespersonTarget(salespersonId, month, new BigDecimal("15000.00"), 30);

		repositoryAdapter.save(target);

		assertThat(repositoryAdapter.findBySalespersonAndMonth(salespersonId, month)).isPresent().get()
				.satisfies(found -> {
					assertThat(found.salespersonId()).isEqualTo(salespersonId);
					assertThat(found.month()).isEqualTo(month);
					assertThat(found.valueTarget()).isEqualByComparingTo("15000.00");
					assertThat(found.orderCountTarget()).isEqualTo(30);
				});
	}

	@Test
	@DisplayName("Overwrites the existing target when one is set again for the same month")
	void settingATargetForAMonthThatAlreadyHasOneOverwritesIt() {
		UUID salespersonId = UUID.randomUUID();
		YearMonth month = YearMonth.of(2026, 1);
		repositoryAdapter.save(new SalespersonTarget(salespersonId, month, new BigDecimal("10000.00"), 20));

		repositoryAdapter.save(new SalespersonTarget(salespersonId, month, new BigDecimal("20000.00"), 40));

		assertThat(repositoryAdapter.findBySalespersonAndMonth(salespersonId, month)).isPresent().get()
				.satisfies(found -> {
					assertThat(found.valueTarget()).isEqualByComparingTo("20000.00");
					assertThat(found.orderCountTarget()).isEqualTo(40);
				});
	}

	@Test
	@DisplayName("Returns empty when no target exists for the salesperson and month")
	void findBySalespersonAndMonthReturnsEmptyWhenNoTargetExists() {
		assertThat(repositoryAdapter.findBySalespersonAndMonth(UUID.randomUUID(), YearMonth.of(2026, 1))).isEmpty();
	}

	@Test
	@DisplayName("Returns every target of the requested month only")
	void findByMonthReturnsEveryTargetOfThatMonthOnly() {
		UUID ana = UUID.randomUUID();
		UUID bruno = UUID.randomUUID();
		YearMonth march = YearMonth.of(2026, 3);
		repositoryAdapter.save(new SalespersonTarget(ana, march, new BigDecimal("1000.00"), 1));
		repositoryAdapter.save(new SalespersonTarget(bruno, march, new BigDecimal("2000.00"), 2));
		repositoryAdapter.save(new SalespersonTarget(ana, YearMonth.of(2026, 4), new BigDecimal("3000.00"), 3));

		assertThat(repositoryAdapter.findByMonth(march)).extracting(SalespersonTarget::salespersonId)
				.containsExactlyInAnyOrder(ana, bruno);
		assertThat(repositoryAdapter.findByMonth(YearMonth.of(2026, 5))).isEmpty();
	}
}
