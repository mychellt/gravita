package br.gravita.adapters.outbound.persistence.repositories.purchasing;

import br.gravita.adapters.outbound.persistence.entities.purchasing.PurchaseOrderJpaEntity;
import br.gravita.core.domain.purchasing.PurchaseOrderStatus;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseOrderJpaRepository extends JpaRepository<PurchaseOrderJpaEntity, UUID> {

	/** The orders created in {@code [from, to)} that are not waiting for approval and are not in {@code excluded}. */
	@Query("select o from PurchaseOrderJpaEntity o where o.createdAt >= :from and o.createdAt < :to"
			+ " and o.approvalRequired = false and o.status <> :excluded")
	List<PurchaseOrderJpaEntity> findApprovedCreatedBetween(@Param("from") Date from, @Param("to") Date to,
			@Param("excluded") PurchaseOrderStatus excluded);
}
