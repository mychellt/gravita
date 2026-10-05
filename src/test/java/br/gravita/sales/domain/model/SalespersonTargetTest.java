package br.gravita.sales.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gravita.core.domain.sales.SalespersonTarget;
import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SalespersonTargetTest {

	@Test
	@DisplayName("Creates a target when all fields are valid")
	void createsATargetWithValidFields() {
		final UUID salespersonId = UUID.randomUUID();
		final YearMonth month = YearMonth.of(2026, 1);

		final SalespersonTarget target = new SalespersonTarget(salespersonId, month, new BigDecimal("10000.00"), 20);

		assertThat(target.salespersonId()).isEqualTo(salespersonId);
		assertThat(target.month()).isEqualTo(month);
		assertThat(target.valueTarget()).isEqualByComparingTo("10000.00");
		assertThat(target.orderCountTarget()).isEqualTo(20);
	}

	@Test
	@DisplayName("Allows targets whose value and order count are zero")
	void allowsZeroValuedTargets() {
		final SalespersonTarget target =
				new SalespersonTarget(UUID.randomUUID(), YearMonth.of(2026, 1), BigDecimal.ZERO, 0);

		assertThat(target.valueTarget()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(target.orderCountTarget()).isZero();
	}

	@Test
	@DisplayName("Rejects a target with a negative value")
	void rejectsNegativeValueTarget() {
		assertThatThrownBy(() -> new SalespersonTarget(UUID.randomUUID(), YearMonth.of(2026, 1),
				new BigDecimal("-1"), 10)).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Value target");
	}

	@Test
	@DisplayName("Rejects a target with a negative order count")
	void rejectsNegativeOrderCountTarget() {
		assertThatThrownBy(() -> new SalespersonTarget(UUID.randomUUID(), YearMonth.of(2026, 1),
				new BigDecimal("100"), -1)).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Order count target");
	}

	@Test
	@DisplayName("Rejects a target without a salesperson id")
	void rejectsNullSalespersonId() {
		assertThatThrownBy(
				() -> new SalespersonTarget(null, YearMonth.of(2026, 1), new BigDecimal("100"), 10))
				.isInstanceOf(NullPointerException.class);
	}

	@Test
	@DisplayName("Rejects a target without a month")
	void rejectsNullMonth() {
		assertThatThrownBy(() -> new SalespersonTarget(UUID.randomUUID(), null, new BigDecimal("100"), 10))
				.isInstanceOf(NullPointerException.class);
	}
}
