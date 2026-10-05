package br.gravita.adapters.outbound.persistence.adapters.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.CommissionJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.sales.CommissionPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.sales.CommissionJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.sales.Commission;
import br.gravita.core.domain.sales.SalesOrderId;
import br.gravita.core.ports.outbound.persistence.sales.CommissionRepositoryPort;
import java.util.Collection;
import java.util.List;

@PersistenceAdapter
class CommissionRepositoryAdapter implements CommissionRepositoryPort {

	private final CommissionJpaRepository jpaRepository;
	private final CommissionPersistenceMapper mapper;

	CommissionRepositoryAdapter(final CommissionJpaRepository jpaRepository, final CommissionPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public Commission save(final Commission commission) {
		final CommissionJpaEntity entity = mapper.map(commission);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		final CommissionJpaEntity saved = jpaRepository.save(entity);
		return mapper.map(saved);
	}

	@Override
	public List<Commission> findByOrderIds(final Collection<SalesOrderId> orderIds) {
		if (orderIds.isEmpty()) {
			return List.of();
		}
		return jpaRepository.findBySalesOrderIdIn(orderIds.stream().map(SalesOrderId::value).toList()).stream()
				.map(mapper::map).toList();
	}
}
