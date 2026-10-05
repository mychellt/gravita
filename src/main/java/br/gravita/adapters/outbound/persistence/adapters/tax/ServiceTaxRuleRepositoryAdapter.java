package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.repositories.tax.ServiceTaxRuleJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.tax.ServiceTaxRule;
import br.gravita.core.ports.outbound.persistence.tax.ServiceTaxRuleRepositoryPort;
import java.util.List;

@PersistenceAdapter
class ServiceTaxRuleRepositoryAdapter implements ServiceTaxRuleRepositoryPort {

	private final ServiceTaxRuleJpaRepository jpaRepository;

	ServiceTaxRuleRepositoryAdapter(final ServiceTaxRuleJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public List<ServiceTaxRule> findCandidates(final String serviceCode, final String municipalityIbgeCode) {
		return jpaRepository.findCandidates(serviceCode, municipalityIbgeCode).stream()
				.map(e -> new ServiceTaxRule(e.getServiceCode(), e.getMunicipalityIbge(), e.getRegime(),
						e.getTaxType(), e.getRatePercentage(), e.getWithholding()))
				.toList();
	}
}
