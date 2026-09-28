package br.gravita.adapters.outbound.persistence.repositories.finance;

import br.gravita.adapters.outbound.persistence.entities.finance.ReceivableJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceivableJpaRepository extends JpaRepository<ReceivableJpaEntity, UUID> {

	List<ReceivableJpaEntity> findByOriginDocumentRefOrderByInstallmentNumber(UUID originDocumentRef);
}
