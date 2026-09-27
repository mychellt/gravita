package br.gravita.adapters.outbound.persistence.adapters.sales;

import br.gravita.adapters.outbound.persistence.entities.sales.SalesInvoiceJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.sales.SalesInvoicePersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.sales.SalesInvoiceJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.sales.SalesInvoice;
import br.gravita.core.domain.sales.SalesInvoiceId;
import br.gravita.core.domain.sales.SalesOrderId;
import br.gravita.core.ports.outbound.persistence.sales.SalesInvoiceRepositoryPort;
import java.util.Optional;

@PersistenceAdapter
class SalesInvoiceRepositoryAdapter implements SalesInvoiceRepositoryPort {

	private final SalesInvoiceJpaRepository jpaRepository;
	private final SalesInvoicePersistenceMapper mapper;

	SalesInvoiceRepositoryAdapter(SalesInvoiceJpaRepository jpaRepository, SalesInvoicePersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public SalesInvoice save(SalesInvoice invoice) {
		SalesInvoiceJpaEntity entity = mapper.toEntity(invoice);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		SalesInvoiceJpaEntity saved = jpaRepository.save(entity);
		return mapper.toDomain(saved);
	}

	@Override
	public Optional<SalesInvoice> findById(SalesInvoiceId id) {
		return jpaRepository.findById(id.value()).map(mapper::toDomain);
	}

	@Override
	public Optional<SalesInvoice> findByOrderId(SalesOrderId orderId) {
		return jpaRepository.findBySalesOrderId(orderId.value()).map(mapper::toDomain);
	}
}
