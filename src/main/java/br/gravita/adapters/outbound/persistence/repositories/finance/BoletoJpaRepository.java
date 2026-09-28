package br.gravita.adapters.outbound.persistence.repositories.finance;

import br.gravita.adapters.outbound.persistence.entities.finance.BoletoJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoletoJpaRepository extends JpaRepository<BoletoJpaEntity, UUID> {

	List<BoletoJpaEntity> findByReceivableIdOrderByCreatedAt(UUID receivableId);
}
