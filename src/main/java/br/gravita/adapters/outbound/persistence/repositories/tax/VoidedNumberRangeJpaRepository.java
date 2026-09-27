package br.gravita.adapters.outbound.persistence.repositories.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.VoidedNumberRangeJpaEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VoidedNumberRangeJpaRepository extends JpaRepository<VoidedNumberRangeJpaEntity, UUID> {

	@Query("""
			select v from VoidedNumberRangeJpaEntity v
			where v.companyId = :companyId
			and (:series is null or v.series = :series)
			and (:dateFrom is null or v.voidedAt >= :dateFrom)
			and (:dateTo is null or v.voidedAt <= :dateTo)
			""")
	List<VoidedNumberRangeJpaEntity> search(
			@Param("companyId") UUID companyId,
			@Param("series") String series,
			@Param("dateFrom") Instant dateFrom,
			@Param("dateTo") Instant dateTo);
}
