package br.gravita.core.domain;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class PlanDomain extends AbstractDomain {
	private String name;
	private String description;
	private PlanTier tier;
	private BigDecimal priceMonthly;
	private BigDecimal priceAnnual;
	private boolean featured;
	private PlanLimits limits;
	private List<PlanFeature> features;
	private PlanSupport support;

	public boolean isActivePlan() {
		return Boolean.TRUE.equals(getActive());
	}

	/** Checks the rules that depend on this plan alone; rules across plans live in the use cases. */
	public void validate() {
		requireText(name, "name");
		requireText(description, "description");
		requireNonNegative(priceMonthly, "priceMonthly");
		requireNonNegative(priceAnnual, "priceAnnual");
		if (priceAnnual.compareTo(priceMonthly) > 0) {
			throw new BusinessRuleException("Plan priceAnnual cannot be greater than priceMonthly");
		}
		if (getActive() == null) {
			throw new BusinessRuleException("Plan active flag is required");
		}
		if (featured && !isActivePlan()) {
			throw new BusinessRuleException("An inactive plan cannot be featured");
		}
		requirePresent(limits, "limits").validate();
		validateFeatures();
		requirePresent(support, "support").validate();
	}

	private void validateFeatures() {
		if (features == null || features.isEmpty()) {
			throw new BusinessRuleException("Plan must have at least one feature");
		}
		if (features.stream().anyMatch(feature -> feature.label() == null || feature.label().isBlank())) {
			throw new BusinessRuleException("Plan feature labels cannot be blank");
		}
		if (features.stream().noneMatch(PlanFeature::included)) {
			throw new BusinessRuleException("Plan must have at least one included feature");
		}
	}

	private static void requireText(final String value, final String field) {
		if (value == null || value.isBlank()) {
			throw new BusinessRuleException("Plan " + field + " is required");
		}
	}

	private static void requireNonNegative(final BigDecimal value, final String field) {
		if (value == null || value.signum() < 0) {
			throw new BusinessRuleException("Plan " + field + " must be zero or greater");
		}
	}

	private static <T> T requirePresent(final T value, final String field) {
		if (value == null) {
			throw new BusinessRuleException("Plan " + field + " is required");
		}
		return value;
	}
}
