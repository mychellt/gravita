package br.gravita.core.domain;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static br.gravita.core.domain.PlanFixtures.plan;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlanDomainTest {

	@Test
	@DisplayName("A complete plan is valid")
	void acceptsCompletePlan() {
		assertThatCode(() -> plan().build().validate()).doesNotThrowAnyException();
	}

	@ParameterizedTest(name = "rejects {0}")
	@MethodSource("br.gravita.core.domain.PlanFixtures#invalidPlans")
	void rejectsPlanBreakingARule(final String ignoredRule, final PlanDomain plan) {
		assertThatThrownBy(plan::validate).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Accepts an annual price equal to the monthly price and unlimited limits")
	void acceptsBoundaryValues() {
		final PlanDomain plan = plan()
				.priceAnnual(new BigDecimal("297"))
				.limits(new PlanLimits(null, null, null, null))
				.support(new PlanSupport(false, false, true, 72, false, true))
				.build();

		assertThatCode(plan::validate).doesNotThrowAnyException();
	}

	@ParameterizedTest(name = "accepts sla of {0} hours")
	@ValueSource(ints = {1, 72})
	void acceptsSlaBounds(final int hours) {
		final PlanDomain plan = plan().support(new PlanSupport(true, false, false, hours, true, false)).build();

		assertThatCode(plan::validate).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("Rejects a plan without the active flag, limits or support")
	void rejectsMissingParts() {
		assertThatThrownBy(() -> plan().active(null).build().validate()).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> plan().limits(null).build().validate()).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> plan().support(null).build().validate()).isInstanceOf(BusinessRuleException.class);
	}
}
