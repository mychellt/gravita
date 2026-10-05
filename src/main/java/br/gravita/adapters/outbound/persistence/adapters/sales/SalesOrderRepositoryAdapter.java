package br.gravita.adapters.outbound.persistence.adapters.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.SalesOrderJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.sales.SalesOrderPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.sales.SalesOrderJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.sales.SalesOrder;
import br.gravita.core.domain.sales.SalesOrderId;
import br.gravita.core.domain.sales.SalesOrderStatus;
import br.gravita.core.ports.outbound.persistence.sales.SalesOrderRepositoryPort;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@PersistenceAdapter
class SalesOrderRepositoryAdapter implements SalesOrderRepositoryPort {

	private final SalesOrderJpaRepository jpaRepository;
	private final SalesOrderPersistenceMapper mapper;

	SalesOrderRepositoryAdapter(final SalesOrderJpaRepository jpaRepository, final SalesOrderPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public SalesOrder save(final SalesOrder order) {
		final SalesOrderJpaEntity entity = mapper.map(order);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		final SalesOrderJpaEntity saved = jpaRepository.save(entity);
		return mapper.map(saved);
	}

	@Override
	public Optional<SalesOrder> findById(final SalesOrderId id) {
		return jpaRepository.findById(id.value()).map(mapper::map);
	}

	@Override
	public List<SalesOrder> findInvoicedByPeriod(final LocalDate periodStart, final LocalDate periodEnd) {
		return jpaRepository.findByStatusAndInvoicedAtBetween(SalesOrderStatus.INVOICED, periodStart, periodEnd)
				.stream().map(mapper::map).toList();
	}

	@Override
	public List<SalesOrder> findInvoicedByPeriodAndSalesperson(final LocalDate periodStart, final LocalDate periodEnd,
			final UUID salespersonId) {
		return jpaRepository.findByStatusAndInvoicedAtBetweenAndSalespersonId(SalesOrderStatus.INVOICED, periodStart,
				periodEnd, salespersonId).stream().map(mapper::map).toList();
	}
}
