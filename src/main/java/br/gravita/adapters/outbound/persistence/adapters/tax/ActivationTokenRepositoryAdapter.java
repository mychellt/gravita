package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.ActivationTokenJpaEntity;
import br.gravita.adapters.outbound.persistence.mappers.tax.ActivationTokenPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.tax.ActivationTokenJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.system.ActivationToken;
import br.gravita.core.ports.outbound.persistence.system.ActivationTokenRepositoryPort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@PersistenceAdapter
class ActivationTokenRepositoryAdapter implements ActivationTokenRepositoryPort {

	private final ActivationTokenJpaRepository jpaRepository;
	private final ActivationTokenPersistenceMapper mapper;

	ActivationTokenRepositoryAdapter(ActivationTokenJpaRepository jpaRepository,
			ActivationTokenPersistenceMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public ActivationToken save(ActivationToken token) {
		ActivationTokenJpaEntity entity = jpaRepository.findById(token.getId())
				.map(existing -> {
					existing.setUsedAt(token.getUsedAt());
					existing.setExpiresAt(token.getExpiresAt());
					return existing;
				})
				.orElseGet(() -> mapper.map(token));
		return mapper.map(jpaRepository.save(entity));
	}

	@Override
	public Optional<ActivationToken> findByTokenHashForUpdate(String tokenHash) {
		return jpaRepository.findByTokenHash(tokenHash).map(mapper::map);
	}

	@Override
	public Optional<ActivationToken> findLatestByUserId(UUID userId) {
		return jpaRepository.findFirstByUserIdOrderByCreatedAtDesc(userId).map(mapper::map);
	}

	@Override
	public List<ActivationToken> findUnusedByUserId(UUID userId) {
		return jpaRepository.findByUserIdAndUsedAtIsNull(userId).stream().map(mapper::map).toList();
	}
}
