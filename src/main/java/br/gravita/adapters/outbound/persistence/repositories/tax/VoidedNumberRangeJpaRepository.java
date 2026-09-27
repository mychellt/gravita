package br.gravita.adapters.outbound.persistence.repositories.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.VoidedNumberRangeJpaEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoidedNumberRangeJpaRepository extends JpaRepository<VoidedNumberRangeJpaEntity, UUID> {

	List<VoidedNumberRangeJpaEntity> findByCompanyId(UUID companyId);

	List<VoidedNumberRangeJpaEntity> findByCompanyIdAndSeriesAndVoidedAtBetween(UUID companyId, String series,
			Instant voidedFrom, Instant voidedTo);
}
