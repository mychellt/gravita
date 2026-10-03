package br.gravita.adapters.outbound.persistence.repositories.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.ActivationTokenJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActivationTokenJpaRepository extends JpaRepository<ActivationTokenJpaEntity, UUID> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<ActivationTokenJpaEntity> findByTokenHash(String tokenHash);

	Optional<ActivationTokenJpaEntity> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);

	List<ActivationTokenJpaEntity> findByUserIdAndUsedAtIsNull(UUID userId);
}
