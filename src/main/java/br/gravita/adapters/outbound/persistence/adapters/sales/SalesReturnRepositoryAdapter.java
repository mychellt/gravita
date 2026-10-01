package br.gravita.adapters.outbound.persistence.adapters.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.SalesReturnJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.sales.SalesReturnPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.sales.SalesReturnJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.sales.SalesOrderId;
import br.gravita.core.domain.sales.SalesReturn;
import br.gravita.core.domain.sales.SalesReturnId;
import br.gravita.core.ports.outbound.persistence.sales.SalesReturnRepositoryPort;
import java.util.List;
import java.util.Optional;

@PersistenceAdapter
class SalesReturnRepositoryAdapter implements SalesReturnRepositoryPort {

	private final SalesReturnJpaRepository jpaRepository;
	private final SalesReturnPersistenceMapper mapper;

	SalesReturnRepositoryAdapter(SalesReturnJpaRepository jpaRepository, SalesReturnPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public SalesReturn save(SalesReturn salesReturn) {
		SalesReturnJpaEntity entity = mapper.map(salesReturn);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		SalesReturnJpaEntity saved = jpaRepository.save(entity);
		return mapper.map(saved);
	}

	@Override
	public Optional<SalesReturn> findById(SalesReturnId id) {
		return jpaRepository.findById(id.value()).map(mapper::map);
	}

	@Override
	public List<SalesReturn> findByOrderId(SalesOrderId orderId) {
		return jpaRepository.findBySalesOrderId(orderId.value()).stream().map(mapper::map).toList();
	}
}
