package br.gravita.core.usercases.tax;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.tax.ItemTaxBreakdown;
import br.gravita.core.domain.tax.ItemTaxInput;
import br.gravita.core.domain.tax.NfseTomador;
import br.gravita.core.domain.tax.NfseWithholding;
import br.gravita.core.domain.tax.ServiceTaxRule;
import br.gravita.core.domain.tax.TaxLineBreakdown;
import br.gravita.core.domain.tax.TaxRateRule;
import br.gravita.core.domain.tax.TaxRegime;
import br.gravita.core.domain.tax.TaxType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves the ISS rate and the withholdings of one service from the parameterized service-tax table. The
 * percentage-of-base arithmetic is the shared {@link TaxEngine} (the single tax-calculation implementation, doc §13);
 * this class only decides which rules apply and which of the resulting taxes are withheld at source.
 */
final class ServiceTaxCalculator {

	/** The only taxes a service can carry; other types in the table (ICMS, IPI...) are goods taxes and ignored. */
	private static final Set<TaxType> SERVICE_TAXES = EnumSet.of(TaxType.ISS, TaxType.PIS, TaxType.COFINS,
			TaxType.CSLL, TaxType.IRPJ, TaxType.INSS);
	private static final String OPERATION_TYPE = "SERVICE";

	private final TaxEngine taxEngine;

	ServiceTaxCalculator(final TaxEngine taxEngine) {
		this.taxEngine = taxEngine;
	}

	record Result(BigDecimal issRate, BigDecimal issAmount, List<NfseWithholding> withholdings) {
	}

	/**
	 * @param issRateOverride a manual ISS rate that replaces the configured one (its justification is validated by the
	 *                        caller), or {@code null} to use the configured rate
	 */
	Result calculate(final List<ServiceTaxRule> candidates, final String serviceCode, final String issMunicipalityIbgeCode,
			final TaxRegime regime, final String providerState, final NfseTomador tomador, final BigDecimal serviceAmount,
			final BigDecimal issRateOverride) {
		final Map<TaxType, ServiceTaxRule> selected = selectMostSpecific(candidates, issMunicipalityIbgeCode, regime);

		if (issRateOverride != null) {
			final ServiceTaxRule configured = selected.get(TaxType.ISS);
			selected.put(TaxType.ISS, new ServiceTaxRule(serviceCode, issMunicipalityIbgeCode, regime, TaxType.ISS,
					issRateOverride, configured == null ? null : configured.withholding()));
		} else if (!selected.containsKey(TaxType.ISS)) {
			throw new BusinessRuleException("No ISS rate configured for service " + serviceCode + " in municipality "
					+ issMunicipalityIbgeCode + "; inform a rate override with its justification");
		}

		final List<TaxRateRule> rates = new ArrayList<>();
		for (final ServiceTaxRule rule : selected.values()) {
			// The engine's rate-rule shape is goods-oriented (NCM/UF); the service code stands in for the
			// classification and the provider's UF for both states, none of which affects the arithmetic.
			rates.add(new TaxRateRule(serviceCode, providerState, providerState,
					regime, OPERATION_TYPE, rule.taxType(), rule.ratePercentage(), BigDecimal.ZERO, BigDecimal.ZERO));
		}
		final ItemTaxBreakdown breakdown = taxEngine.calculate(
				new ItemTaxInput(0, serviceCode, BigDecimal.ONE, serviceAmount, rates), List.of());

		BigDecimal issRate = null;
		BigDecimal issAmount = null;
		final List<NfseWithholding> withholdings = new ArrayList<>();
		for (final TaxLineBreakdown line : breakdown.taxLines()) {
			if (line.taxType() == TaxType.ISS) {
				issRate = line.ratePercentage();
				issAmount = line.finalAmount();
			}
			if (selected.get(line.taxType()).isWithheldFor(tomador)) {
				withholdings.add(new NfseWithholding(line.taxType(), line.base(), line.ratePercentage(),
						line.finalAmount()));
			}
		}
		return new Result(issRate, issAmount, withholdings);
	}

	private Map<TaxType, ServiceTaxRule> selectMostSpecific(final List<ServiceTaxRule> candidates, final String ibgeCode,
			final TaxRegime regime) {
		final Map<TaxType, ServiceTaxRule> selected = new EnumMap<>(TaxType.class);
		for (final ServiceTaxRule rule : candidates) {
			if (!SERVICE_TAXES.contains(rule.taxType()) || !rule.appliesTo(ibgeCode, regime)) {
				continue;
			}
			final ServiceTaxRule current = selected.get(rule.taxType());
			if (current == null || rule.specificity() > current.specificity()) {
				selected.put(rule.taxType(), rule);
			}
		}
		return selected;
	}
}
