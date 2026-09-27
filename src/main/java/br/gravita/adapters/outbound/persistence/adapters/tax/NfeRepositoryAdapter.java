package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.NfeJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.tax.NfePersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.tax.NfeJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.domain.tax.NfeDocumentId;
import br.gravita.core.ports.outbound.persistence.tax.NfeRepositoryPort;
import java.util.Optional;

@PersistenceAdapter
class NfeRepositoryAdapter implements NfeRepositoryPort {

	private final NfeJpaRepository jpaRepository;
	private final NfePersistenceMapper mapper;

	NfeRepositoryAdapter(NfeJpaRepository jpaRepository, NfePersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public NfeDocument save(NfeDocument document) {
		NfeJpaEntity entity = mapper.toEntity(document);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		NfeJpaEntity saved = jpaRepository.save(entity);
		return mapper.toDomain(saved);
	}

	@Override
	public Optional<NfeDocument> findById(NfeDocumentId id) {
		return jpaRepository.findById(id.value()).map(mapper::toDomain);
	}
}
