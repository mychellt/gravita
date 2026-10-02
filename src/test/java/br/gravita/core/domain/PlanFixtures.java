package br.gravita.core.domain;

import org.junit.jupiter.params.provider.Arguments;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/** Builders for plan test data: a valid plan to tweak, and the invalid variants every write path must reject. */
public final class PlanFixtures {

	private PlanFixtures() {
	}

	public static PlanDomain.PlanDomainBuilder<?, ?> aPlan() {
		return PlanDomain.builder()
				.id(UUID.randomUUID())
				.name("Bronze")
				.description("Para quem está começando")
				.tier(PlanTier.BRONZE)
				.priceMonthly(new BigDecimal("297"))
				.priceAnnual(new BigDecimal("247"))
				.active(true)
				.featured(false)
				.limits(new PlanLimits(1, 1, 1, 3))
				.features(List.of(new PlanFeature("1 CNPJ · 1 filial", true, 0), new PlanFeature("API", false, 1)))
				.support(new PlanSupport(true, false, false, 24, true, false));
	}

	/** Plans that break exactly one per-plan rule (acceptance criteria 4 and 7 to 11). */
	public static Stream<Arguments> invalidPlans() {
		return Stream.of(
				Arguments.of("featured while inactive", aPlan().featured(true).active(false).build()),
				Arguments.of("annual price above monthly", aPlan().priceAnnual(new BigDecimal("298")).build()),
				Arguments.of("limit of zero", aPlan().limits(new PlanLimits(0, null, null, null)).build()),
				Arguments.of("negative limit", aPlan().limits(new PlanLimits(null, null, null, -5)).build()),
				Arguments.of("no features", aPlan().features(List.of()).build()),
				Arguments.of("no included feature", aPlan().features(List.of(new PlanFeature("API", false, 0))).build()),
				Arguments.of("blank description", aPlan().description(" ").build()),
				Arguments.of("no support channel", aPlan().support(new PlanSupport(false, false, false, 24, true, false)).build()),
				Arguments.of("sla below one hour", aPlan().support(new PlanSupport(true, false, false, 0, true, false)).build()),
				Arguments.of("sla above 72 hours", aPlan().support(new PlanSupport(true, false, false, 73, true, false)).build()));
	}
}
