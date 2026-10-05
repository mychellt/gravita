package br.gravita.core.usercases.tax;

import br.gravita.core.domain.tax.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public final class TaxEngine {

	private static final int MONEY_SCALE = 2;
	private static final int CALC_SCALE = 10;
	private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
	private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

	public ItemTaxBreakdown calculate(final ItemTaxInput input, final List<TaxOverrideInput> overridesForItem) {
		final BigDecimal grossAmount = input.grossAmount();
		final Map<TaxType, TaxRateRule> rulesByType = new EnumMap<>(TaxType.class);
		for (final TaxRateRule rule : input.applicableRates()) {
			rulesByType.putIfAbsent(rule.taxType(), rule);
		}

		BigDecimal icmsAmount = BigDecimal.ZERO;
		final TaxRateRule icmsRule = rulesByType.get(TaxType.ICMS);
		if (icmsRule != null) {
			final BigDecimal base = effectiveBase(grossAmount, icmsRule.baseReductionPercentage());
			icmsAmount = percentageOf(base, icmsRule.ratePercentage());
		}

		final List<TaxLineBreakdown> lines = new ArrayList<>();
		for (final TaxRateRule rule : rulesByType.values()) {
			final BigDecimal base;
			final BigDecimal amount;
			if (rule.taxType() == TaxType.ICMS) {
				base = effectiveBase(grossAmount, rule.baseReductionPercentage());
				amount = icmsAmount;
			} else if (rule.taxType() == TaxType.ICMS_ST) {
				final BigDecimal mvaFactor = BigDecimal.ONE.add(rule.mvaPercentage().divide(HUNDRED, CALC_SCALE, ROUNDING));
				base = effectiveBase(grossAmount, rule.baseReductionPercentage()).multiply(mvaFactor);
				final BigDecimal grossIcmsSt = percentageOf(base, rule.ratePercentage());
				amount = grossIcmsSt.subtract(icmsAmount).max(BigDecimal.ZERO);
			} else {
				base = effectiveBase(grossAmount, rule.baseReductionPercentage());
				amount = percentageOf(base, rule.ratePercentage());
			}
			lines.add(buildLine(rule.taxType(), base, rule.ratePercentage(), amount, input.itemIndex(), overridesForItem));
		}

		lines.sort(Comparator.comparing(TaxLineBreakdown::taxType));
		return new ItemTaxBreakdown(input.itemIndex(), input.productRef(), lines);
	}

	private TaxLineBreakdown buildLine(final TaxType taxType, final BigDecimal base, final BigDecimal ratePercentage,
			final BigDecimal computedAmount, final int itemIndex, final List<TaxOverrideInput> overrides) {
		final BigDecimal roundedComputed = computedAmount.setScale(MONEY_SCALE, ROUNDING);
		final TaxOverrideInput override = findOverride(overrides, itemIndex, taxType);
		if (override != null) {
			return new TaxLineBreakdown(taxType, base, ratePercentage, roundedComputed,
					override.value().setScale(MONEY_SCALE, ROUNDING), true, override.justification());
		}
		return new TaxLineBreakdown(taxType, base, ratePercentage, roundedComputed, roundedComputed, false, null);
	}

	private TaxOverrideInput findOverride(final List<TaxOverrideInput> overrides, final int itemIndex, final TaxType taxType) {
		for (final TaxOverrideInput override : overrides) {
			if (override.itemIndex() == itemIndex && override.taxType() == taxType) {
				return override;
			}
		}
		return null;
	}

	private BigDecimal effectiveBase(final BigDecimal grossAmount, final BigDecimal reductionPercentage) {
		if (reductionPercentage.signum() == 0) {
			return grossAmount;
		}
		final BigDecimal factor = BigDecimal.ONE.subtract(reductionPercentage.divide(HUNDRED, CALC_SCALE, ROUNDING));
		return grossAmount.multiply(factor);
	}

	private BigDecimal percentageOf(final BigDecimal base, final BigDecimal ratePercentage) {
		return base.multiply(ratePercentage).divide(HUNDRED, CALC_SCALE, ROUNDING);
	}
}
