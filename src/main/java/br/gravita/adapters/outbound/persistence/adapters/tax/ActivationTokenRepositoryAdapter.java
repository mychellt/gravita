package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.mappers.tax.ActivationTokenPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.tax.ActivationTokenJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.system.ActivationToken;
import br.gravita.core.ports.outbound.persistence.system.ActivationTokenRepositoryPort;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@PersistenceAdapter
class ActivationTokenRepositoryAdapter implements ActivationTokenRepositoryPort {

    private final ActivationTokenJpaRepository jpaRepository;
    private final ActivationTokenPersistenceMapper mapper;

    @Override
    public ActivationToken save(final ActivationToken token) {
        final var entity = jpaRepository.findById(token.getId())
                .map(existing -> {
                    existing.setModifiedAt(token.getModifiedAt());
                    existing.setExpiresAt(token.getExpiresAt());
                    return existing;
                })
                .orElseGet(() -> mapper.map(token));
        return mapper.map(jpaRepository.save(entity));
    }

    @Override
    public Optional<ActivationToken> findByTokenHashForUpdate(final String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash).map(mapper::map);
    }

    @Override
    public Optional<ActivationToken> findLatestByUserId(final UUID userId) {
        return jpaRepository.findFirstByUserIdOrderByCreatedAtDesc(userId).map(mapper::map);
    }

    @Override
    public List<ActivationToken> findUnusedByUserId(final UUID userId) {
        return jpaRepository.findByUserIdAndModifiedAtIsNull(userId).stream().map(mapper::map).toList();
    }
}
