package br.gravita.adapters.outbound.persistence.adapters.tax;

import br.gravita.adapters.outbound.persistence.mappers.tax.PasswordResetTokenPersistenceMapper;
import br.gravita.adapters.outbound.persistence.repositories.tax.PasswordResetTokenJpaRepository;
import br.gravita.core.annotations.PersistenceAdapter;
import br.gravita.core.domain.system.PasswordResetToken;
import br.gravita.core.ports.outbound.persistence.system.PasswordResetTokenRepositoryPort;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@PersistenceAdapter
class PasswordResetTokenRepositoryAdapter implements PasswordResetTokenRepositoryPort {

    private final PasswordResetTokenJpaRepository jpaRepository;
    private final PasswordResetTokenPersistenceMapper mapper;

    @Override
    public PasswordResetToken save(final PasswordResetToken token) {
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
    public Optional<PasswordResetToken> findByTokenHashForUpdate(final String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash).map(mapper::map);
    }

    @Override
    public Optional<PasswordResetToken> findLatestByUserId(final UUID userId) {
        return jpaRepository.findFirstByUserIdOrderByCreatedAtDesc(userId).map(mapper::map);
    }

    @Override
    public List<PasswordResetToken> findUnusedByUserId(final UUID userId) {
        return jpaRepository.findByUserIdAndModifiedAtIsNull(userId).stream().map(mapper::map).toList();
    }
}
