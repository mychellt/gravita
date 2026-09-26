package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.TaxRateRuleJpaEntity;
import br.gravita.adapters.outbound.persistence.repositories.tax.TaxRateRuleJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.tax.TaxRateRule;
import br.gravita.core.ports.outbound.persistence.tax.TaxRateQuery;
import br.gravita.core.ports.outbound.persistence.tax.TaxRuleTableRepositoryPort;
import java.util.List;

@PersistenceAdapter
class TaxRuleTableRepositoryAdapter implements TaxRuleTableRepositoryPort {

	private final TaxRateRuleJpaRepository jpaRepository;

	TaxRuleTableRepositoryAdapter(TaxRateRuleJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public List<TaxRateRule> findApplicableRates(TaxRateQuery query) {
		return jpaRepository
				.findByNcmAndOriginStateAndDestinationStateAndRegimeAndOperationType(query.ncm(), query.originState(),
						query.destinationState(), query.regime(), query.operationType())
				.stream()
				.map(this::toDomain)
				.toList();
	}

	private TaxRateRule toDomain(TaxRateRuleJpaEntity entity) {
		return new TaxRateRule(entity.getNcm(), entity.getOriginState(), entity.getDestinationState(),
				entity.getRegime(), entity.getOperationType(), entity.getTaxType(), entity.getRatePercentage(),
				entity.getBaseReductionPercentage(), entity.getMvaPercentage());
	}
}
