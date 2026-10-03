package br.gravita.adapters.outbound.persistence.repositories.tax;

import br.gravita.adapters.outbound.persistence.entities.ProfileJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProfileLookupJpaRepository extends JpaRepository<ProfileJpaEntity, UUID> {

	Optional<ProfileJpaEntity> findByName(String name);
}
