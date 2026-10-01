package br.gravita.adapters.outbound.persistence.repositories.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.InboundNfeJpaEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InboundNfeJpaRepository extends JpaRepository<InboundNfeJpaEntity, UUID> {

	Optional<InboundNfeJpaEntity> findByAccessKey(String accessKey);

	List<InboundNfeJpaEntity> findByIssuedAtGreaterThanEqualAndIssuedAtLessThanOrderByIssuedAt(Instant from, Instant to);

	List<InboundNfeJpaEntity> findByCompanyIdAndIssuedAtGreaterThanEqualAndIssuedAtLessThanOrderByIssuedAt(
			UUID companyId, Instant from, Instant to);
}
