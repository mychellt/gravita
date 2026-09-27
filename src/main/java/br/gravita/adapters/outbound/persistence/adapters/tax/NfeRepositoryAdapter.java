package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.NfeDocumentJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.tax.NfePersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.tax.NfeDocumentJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.ports.outbound.persistence.tax.NfeRepositoryPort;

@PersistenceAdapter
class NfeRepositoryAdapter implements NfeRepositoryPort {

	private final NfeDocumentJpaRepository jpaRepository;
	private final NfePersistenceMapper mapper;

	NfeRepositoryAdapter(NfeDocumentJpaRepository jpaRepository, NfePersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public NfeDocument save(NfeDocument nfeDocument) {
		NfeDocumentJpaEntity entity = mapper.toEntity(nfeDocument);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		NfeDocumentJpaEntity saved = jpaRepository.saveAndFlush(entity);
		return mapper.toDomain(saved);
	}
}
