package br.gravita.adapters.outbound.persistence.adapters.sales;

import br.gravita.adapters.outbound.persistence.mappers.sales.CommissionRatePersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.sales.CommissionRateJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.sales.CommissionRate;
import br.gravita.core.ports.outbound.persistence.sales.CommissionRateRepositoryPort;
import java.util.Optional;
import java.util.UUID;

@PersistenceAdapter
class CommissionRateRepositoryAdapter implements CommissionRateRepositoryPort {

	private final CommissionRateJpaRepository jpaRepository;
	private final CommissionRatePersistenceMapper mapper;

	CommissionRateRepositoryAdapter(final CommissionRateJpaRepository jpaRepository,
			final CommissionRatePersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public Optional<CommissionRate> findBySalespersonAndProduct(final UUID salespersonId, final UUID productId) {
		return jpaRepository.findBySalespersonIdAndProductId(salespersonId, productId).map(mapper::map);
	}
}
