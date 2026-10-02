package br.gravita.core.domain;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static br.gravita.core.domain.PlanFixtures.aPlan;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlanDomainTest {

	@Test
	@DisplayName("A complete plan is valid")
	void acceptsCompletePlan() {
		assertThatCode(() -> aPlan().build().validate()).doesNotThrowAnyException();
	}

	@ParameterizedTest(name = "rejects {0}")
	@MethodSource("br.gravita.core.domain.PlanFixtures#invalidPlans")
	void rejectsPlanBreakingARule(String ignoredRule, PlanDomain plan) {
		assertThatThrownBy(plan::validate).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Accepts an annual price equal to the monthly price and unlimited limits")
	void acceptsBoundaryValues() {
		PlanDomain plan = aPlan()
				.priceAnnual(new BigDecimal("297"))
				.limits(new PlanLimits(null, null, null, null))
				.support(new PlanSupport(false, false, true, 72, false, true))
				.build();

		assertThatCode(plan::validate).doesNotThrowAnyException();
	}

	@ParameterizedTest(name = "accepts sla of {0} hours")
	@ValueSource(ints = {1, 72})
	void acceptsSlaBounds(int hours) {
		PlanDomain plan = aPlan().support(new PlanSupport(true, false, false, hours, true, false)).build();

		assertThatCode(plan::validate).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("Rejects a plan without the active flag, limits or support")
	void rejectsMissingParts() {
		assertThatThrownBy(() -> aPlan().active(null).build().validate()).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> aPlan().limits(null).build().validate()).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> aPlan().support(null).build().validate()).isInstanceOf(BusinessRuleException.class);
	}
}
