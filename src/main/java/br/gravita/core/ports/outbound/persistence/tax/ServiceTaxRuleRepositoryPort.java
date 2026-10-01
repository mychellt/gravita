package br.gravita.core.ports.outbound.persistence.tax;

import br.gravita.core.domain.tax.ServiceTaxRule;
import java.util.List;

public interface ServiceTaxRuleRepositoryPort {

	/**
	 * Candidate rules for a service code in a municipality: the rows that name that municipality plus the ones that
	 * apply to every municipality. Narrowing by the provider's tax regime is left to the caller.
	 */
	List<ServiceTaxRule> findCandidates(String serviceCode, String municipalityIbgeCode);
}
