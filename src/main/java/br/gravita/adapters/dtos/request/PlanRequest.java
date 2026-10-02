package br.gravita.adapters.dtos.request;

import br.gravita.core.domain.PlanDomain;
import br.gravita.core.domain.PlanFeature;
import br.gravita.core.domain.PlanLimits;
import br.gravita.core.domain.PlanSupport;
import br.gravita.core.domain.PlanTier;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

public record PlanRequest(
		@NotBlank String name,
		@NotBlank String description,
		@NotNull PlanTier tier,
		@NotNull @PositiveOrZero BigDecimal priceMonthly,
		@NotNull @PositiveOrZero BigDecimal priceAnnual,
		@NotNull Boolean active,
		boolean featured,
		@NotNull PlanLimits limits,
		@NotEmpty List<@NotNull @Valid FeatureRequest> features,
		@NotNull PlanSupport support) {

	/** A feature line; its position in the list becomes its display order. */
	public record FeatureRequest(@NotBlank String label, boolean included) {
	}

	public PlanDomain toDomain(UUID id) {
		return PlanDomain.builder()
				.id(id)
				.name(name)
				.description(description)
				.tier(tier)
				.priceMonthly(priceMonthly)
				.priceAnnual(priceAnnual)
				.active(active)
				.featured(featured)
				.limits(limits)
				.features(toFeatures())
				.support(support)
				.build();
	}

	private List<PlanFeature> toFeatures() {
		return IntStream.range(0, features.size())
				.mapToObj(index -> new PlanFeature(features.get(index).label(), features.get(index).included(), index))
				.toList();
	}
}
