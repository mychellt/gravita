package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.InboundNfeJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.tax.InboundNfePersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.tax.InboundNfeJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.ports.outbound.persistence.tax.InboundNfeRepositoryPort;

@PersistenceAdapter
class InboundNfeRepositoryAdapter implements InboundNfeRepositoryPort {

	private final InboundNfeJpaRepository jpaRepository;
	private final InboundNfePersistenceMapper mapper;

	InboundNfeRepositoryAdapter(InboundNfeJpaRepository jpaRepository, InboundNfePersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public InboundNfe save(InboundNfe inboundNfe) {
		InboundNfeJpaEntity entity = mapper.toEntity(inboundNfe);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		InboundNfeJpaEntity saved = jpaRepository.save(entity);
		return mapper.toDomain(saved);
	}
}
