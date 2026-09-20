package br.gravita.adapters.outbound.persistence.repositories.tax;

import br.gravita.adapters.outbound.persistence.entities.ProfileJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * A second, read-only repository over the same {@code profiles} table already owned by
 * {@code AssignProfileUseCase} (M10-03, legacy {@code br.gravita.core}/{@code adapters} layout -
 * see docs/ARCHITECTURE.md). {@code RegisterUserUseCase} only needs existence + name, so it reads
 * through the existing {@link ProfileJpaEntity} rather than duplicating the profile aggregate
 * under this context.
 */
public interface ProfileLookupJpaRepository extends JpaRepository<ProfileJpaEntity, UUID> {
}
