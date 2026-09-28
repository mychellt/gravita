package br.gravita.adapters.outbound.persistence.repositories.finance;

import br.gravita.adapters.outbound.persistence.entities.finance.SettlementJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementJpaRepository extends JpaRepository<SettlementJpaEntity, UUID> {

	List<SettlementJpaEntity> findByReceivableIdOrderByTimestampAscCreatedAtAsc(UUID receivableId);
}
