package br.gravita.adapters.dtos.response;

import br.gravita.core.domain.PlanDomain;
import br.gravita.core.domain.PlanFeature;
import br.gravita.core.domain.PlanLimits;
import br.gravita.core.domain.PlanSupport;
import br.gravita.core.domain.PlanTier;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record PlanResponse(
		UUID id,
		String name,
		String description,
		PlanTier tier,
		BigDecimal priceMonthly,
		BigDecimal priceAnnual,
		boolean active,
		boolean featured,
		PlanLimits limits,
		List<FeatureResponse> features,
		PlanSupport support) {

	public record FeatureResponse(String label, boolean included) {
	}

	public static PlanResponse from(PlanDomain domain) {
		return new PlanResponse(
				domain.getId(),
				domain.getName(),
				domain.getDescription(),
				domain.getTier(),
				domain.getPriceMonthly(),
				domain.getPriceAnnual(),
				domain.isActivePlan(),
				domain.isFeatured(),
				domain.getLimits(),
				orderedFeatures(domain.getFeatures()),
				domain.getSupport());
	}

	private static List<FeatureResponse> orderedFeatures(List<PlanFeature> features) {
		return features.stream()
				.sorted(Comparator.comparingInt(PlanFeature::displayOrder))
				.map(feature -> new FeatureResponse(feature.label(), feature.included()))
				.toList();
	}
}
