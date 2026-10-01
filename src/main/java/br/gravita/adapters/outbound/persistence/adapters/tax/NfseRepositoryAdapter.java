package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.NfseJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.tax.NfsePersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.tax.NfseJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.tax.NfseDocument;
import br.gravita.core.domain.tax.NfseId;
import br.gravita.core.ports.outbound.persistence.tax.NfseRepositoryPort;
import java.util.Optional;

@PersistenceAdapter
class NfseRepositoryAdapter implements NfseRepositoryPort {

	private final NfseJpaRepository jpaRepository;
	private final NfsePersistenceMapper mapper;

	NfseRepositoryAdapter(NfseJpaRepository jpaRepository, NfsePersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public NfseDocument save(NfseDocument document) {
		NfseJpaEntity entity = mapper.toEntity(document);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		return mapper.toDomain(jpaRepository.save(entity));
	}

	@Override
	public Optional<NfseDocument> findById(NfseId id) {
		return jpaRepository.findById(id.value()).map(mapper::toDomain);
	}
}
