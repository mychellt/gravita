package br.gravita.adapters.dtos.request;

import br.gravita.core.domain.PlanDomain;
import br.gravita.core.domain.PlanTier;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PlanRequest(
		@NotBlank String name,
		@NotNull PlanTier tier,
		@NotNull @PositiveOrZero BigDecimal priceMonthly,
		@NotNull @PositiveOrZero BigDecimal priceAnnual,
		@NotEmpty List<@NotBlank String> features) {

	public PlanDomain toDomain(UUID id) {
		return PlanDomain.builder()
				.id(id)
				.name(name)
				.tier(tier)
				.priceMonthly(priceMonthly)
				.priceAnnual(priceAnnual)
				.features(features)
				.build();
	}
}
