package br.gravita.adapters.outbound.persistence.repositories.finance;

import br.gravita.adapters.outbound.persistence.entities.finance.InternalCashBoxJpaEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InternalCashBoxJpaRepository extends JpaRepository<InternalCashBoxJpaEntity, UUID> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select b from InternalCashBoxJpaEntity b where b.id = :id")
	Optional<InternalCashBoxJpaEntity> findByIdForUpdate(@Param("id") UUID id);
}
