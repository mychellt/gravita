package br.gravita.core.ports.outbound.persistence.tax;

import br.gravita.core.domain.tax.TaxRateRule;

import java.util.List;

public interface TaxRuleTableRepositoryPort {

	List<TaxRateRule> findApplicableRates(TaxRateQuery query);
}
