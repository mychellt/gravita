package br.gravita.adapters.dtos.response;

import br.gravita.core.domain.PlanDomain;
import br.gravita.core.domain.PlanTier;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PlanResponse(UUID id, String name, PlanTier tier, BigDecimal priceMonthly, BigDecimal priceAnnual, List<String> features) {

	public static PlanResponse from(PlanDomain domain) {
		return new PlanResponse(
				domain.getId(),
				domain.getName(),
				domain.getTier(),
				domain.getPriceMonthly(),
				domain.getPriceAnnual(),
				domain.getFeatures());
	}
}
