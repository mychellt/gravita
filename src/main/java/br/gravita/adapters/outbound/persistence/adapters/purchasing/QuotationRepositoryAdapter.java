package br.gravita.adapters.outbound.persistence.adapters.purchasing;

import br.gravita.adapters.outbound.persistence.entities.purchasing.QuotationJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.purchasing.QuotationPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.purchasing.QuotationJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.purchasing.Quotation;
import br.gravita.core.domain.purchasing.QuotationId;
import br.gravita.core.ports.outbound.persistence.purchasing.QuotationRepositoryPort;
import java.util.Optional;

@PersistenceAdapter
class QuotationRepositoryAdapter implements QuotationRepositoryPort {

	private final QuotationJpaRepository jpaRepository;
	private final QuotationPersistenceMapper mapper;

	QuotationRepositoryAdapter(QuotationJpaRepository jpaRepository, QuotationPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public Quotation save(Quotation quotation) {
		QuotationJpaEntity entity = mapper.toEntity(quotation);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		QuotationJpaEntity saved = jpaRepository.save(entity);
		return mapper.toDomain(saved);
	}

	@Override
	public Optional<Quotation> findById(QuotationId id) {
		return jpaRepository.findById(id.value()).map(mapper::toDomain);
	}
}
