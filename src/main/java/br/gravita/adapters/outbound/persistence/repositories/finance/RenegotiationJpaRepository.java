package br.gravita.adapters.outbound.persistence.repositories.finance;

import br.gravita.adapters.outbound.persistence.entities.finance.RenegotiationJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RenegotiationJpaRepository extends JpaRepository<RenegotiationJpaEntity, UUID> {

	@Query("select r from RenegotiationJpaEntity r join r.originalReceivableIds o where o = :receivableId")
	Optional<RenegotiationJpaEntity> findByOriginalReceivableId(@Param("receivableId") UUID receivableId);

	List<RenegotiationJpaEntity> findByCustomerIdOrderByRenegotiatedAtAsc(UUID customerId);
}
