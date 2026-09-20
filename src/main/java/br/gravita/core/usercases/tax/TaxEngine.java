package br.gravita.core.usercases.tax;

import br.gravita.core.domain.tax.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Computes ICMS (normal and ST), IPI, PIS, COFINS and FCP for one item from
 * whatever {@link TaxRateRule} rows were resolved for it. The rows already
 * encode NCM/UF/regime/operation — this class never branches on regime or
 * operation type, so a new combination is handled entirely by rate-table
 * data (doc §13).
 */
public final class TaxEngine {

	private static final int MONEY_SCALE = 2;
	private static final int CALC_SCALE = 10;
	private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
	private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

	public ItemTaxBreakdown calculate(ItemTaxInput input, List<TaxOverrideInput> overridesForItem) {
		BigDecimal grossAmount = input.grossAmount();
		Map<TaxType, TaxRateRule> rulesByType = new EnumMap<>(TaxType.class);
		for (TaxRateRule rule : input.applicableRates()) {
			rulesByType.putIfAbsent(rule.taxType(), rule);
		}

		BigDecimal icmsAmount = BigDecimal.ZERO;
		TaxRateRule icmsRule = rulesByType.get(TaxType.ICMS);
		if (icmsRule != null) {
			BigDecimal base = effectiveBase(grossAmount, icmsRule.baseReductionPercentage());
			icmsAmount = percentageOf(base, icmsRule.ratePercentage());
		}

		List<TaxLineBreakdown> lines = new ArrayList<>();
		for (TaxRateRule rule : rulesByType.values()) {
			BigDecimal base;
			BigDecimal amount;
			if (rule.taxType() == TaxType.ICMS) {
				base = effectiveBase(grossAmount, rule.baseReductionPercentage());
				amount = icmsAmount;
			} else if (rule.taxType() == TaxType.ICMS_ST) {
				BigDecimal mvaFactor = BigDecimal.ONE.add(rule.mvaPercentage().divide(HUNDRED, CALC_SCALE, ROUNDING));
				base = effectiveBase(grossAmount, rule.baseReductionPercentage()).multiply(mvaFactor);
				BigDecimal grossIcmsSt = percentageOf(base, rule.ratePercentage());
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

	private TaxLineBreakdown buildLine(TaxType taxType, BigDecimal base, BigDecimal ratePercentage,
			BigDecimal computedAmount, int itemIndex, List<TaxOverrideInput> overrides) {
		BigDecimal roundedComputed = computedAmount.setScale(MONEY_SCALE, ROUNDING);
		TaxOverrideInput override = findOverride(overrides, itemIndex, taxType);
		if (override != null) {
			return new TaxLineBreakdown(taxType, base, ratePercentage, roundedComputed,
					override.value().setScale(MONEY_SCALE, ROUNDING), true, override.justification());
		}
		return new TaxLineBreakdown(taxType, base, ratePercentage, roundedComputed, roundedComputed, false, null);
	}

	private TaxOverrideInput findOverride(List<TaxOverrideInput> overrides, int itemIndex, TaxType taxType) {
		for (TaxOverrideInput override : overrides) {
			if (override.itemIndex() == itemIndex && override.taxType() == taxType) {
				return override;
			}
		}
		return null;
	}

	private BigDecimal effectiveBase(BigDecimal grossAmount, BigDecimal reductionPercentage) {
		if (reductionPercentage.signum() == 0) {
			return grossAmount;
		}
		BigDecimal factor = BigDecimal.ONE.subtract(reductionPercentage.divide(HUNDRED, CALC_SCALE, ROUNDING));
		return grossAmount.multiply(factor);
	}

	private BigDecimal percentageOf(BigDecimal base, BigDecimal ratePercentage) {
		return base.multiply(ratePercentage).divide(HUNDRED, CALC_SCALE, ROUNDING);
	}
}
