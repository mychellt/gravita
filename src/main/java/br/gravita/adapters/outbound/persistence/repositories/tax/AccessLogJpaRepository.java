package br.gravita.adapters.outbound.persistence.repositories.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.AccessLogJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface AccessLogJpaRepository extends JpaRepository<AccessLogJpaEntity, UUID> {

	@Query("""
			select a from AccessLogJpaEntity a
			where a.timestamp >= :dateFrom
			and (:dateTo is null or a.timestamp <= :dateTo)
			and (:userId is null or a.userId = :userId)
			and (:ip is null or a.ip = :ip)
			and (:device is null or a.device = :device)
			""")
	Page<AccessLogJpaEntity> search(
			@Param("userId") UUID userId,
			@Param("dateFrom") Instant dateFrom,
			@Param("dateTo") Instant dateTo,
			@Param("ip") String ip,
			@Param("device") String device,
			Pageable pageable);
}
