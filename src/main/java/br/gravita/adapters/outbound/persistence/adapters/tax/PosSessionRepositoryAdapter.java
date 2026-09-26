package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.PosSessionJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.tax.PosSessionPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.tax.PosSessionJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.tax.PosSession;
import br.gravita.core.domain.tax.PosSessionId;
import br.gravita.core.domain.tax.PosSessionStatus;
import br.gravita.core.ports.outbound.persistence.tax.PosSessionRepositoryPort;
import java.util.Optional;
import java.util.UUID;

@PersistenceAdapter
class PosSessionRepositoryAdapter implements PosSessionRepositoryPort {

	private final PosSessionJpaRepository jpaRepository;
	private final PosSessionPersistenceMapper mapper;

	PosSessionRepositoryAdapter(PosSessionJpaRepository jpaRepository, PosSessionPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public PosSession save(PosSession posSession) {
		PosSessionJpaEntity entity = mapper.toEntity(posSession);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		PosSessionJpaEntity saved = jpaRepository.save(entity);
		return mapper.toDomain(saved);
	}

	@Override
	public Optional<PosSession> findById(PosSessionId id) {
		return jpaRepository.findById(id.value()).map(mapper::toDomain);
	}

	@Override
	public boolean existsByRegisterIdAndStatus(UUID registerId, PosSessionStatus status) {
		return jpaRepository.existsByRegisterIdAndStatus(registerId, status);
	}
}
