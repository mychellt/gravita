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

	PosSessionRepositoryAdapter(final PosSessionJpaRepository jpaRepository, final PosSessionPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public PosSession save(final PosSession posSession) {
		final PosSessionJpaEntity entity = mapper.map(posSession);
		entity.setNew(!jpaRepository.existsById(entity.getId()));
		final PosSessionJpaEntity saved = jpaRepository.save(entity);
		return mapper.map(saved);
	}

	@Override
	public Optional<PosSession> findById(final PosSessionId id) {
		return jpaRepository.findById(id.value()).map(mapper::map);
	}

	@Override
	public boolean existsByRegisterIdAndStatus(final UUID registerId, final PosSessionStatus status) {
		return jpaRepository.existsByRegisterIdAndStatus(registerId, status);
	}
}
