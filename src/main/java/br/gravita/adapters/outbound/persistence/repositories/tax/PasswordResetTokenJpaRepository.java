package br.gravita.adapters.outbound.persistence.repositories.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.PasswordResetTokenJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenJpaRepository extends JpaRepository<PasswordResetTokenJpaEntity, UUID> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<PasswordResetTokenJpaEntity> findByTokenHash(String tokenHash);

	Optional<PasswordResetTokenJpaEntity> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);

	List<PasswordResetTokenJpaEntity> findByUserIdAndModifiedAtIsNull(UUID userId);
}
