package br.gravita.tax.application.port.out;

import br.gravita.tax.domain.model.TaxRateRule;

import java.util.List;

/**
 * Reads the parameterized {@code NCM x UF x Regime x Operacao} rate table
 * (doc §13). Not named in the module spec's outbound-ports table; inferred
 * here from UC-M2-02. Backed by M1's rate-table data (M1-14); until that
 * lands this port has no production adapter — see the PR notes on GRA-32.
 */
public interface TaxRuleTableRepositoryPort {

	List<TaxRateRule> findApplicableRates(TaxRateQuery query);
}
