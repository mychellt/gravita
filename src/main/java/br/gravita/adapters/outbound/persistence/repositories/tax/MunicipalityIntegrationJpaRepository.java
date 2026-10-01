package br.gravita.adapters.outbound.persistence.repositories.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.MunicipalityIntegrationJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MunicipalityIntegrationJpaRepository extends JpaRepository<MunicipalityIntegrationJpaEntity, UUID> {

	Optional<MunicipalityIntegrationJpaEntity> findByIbgeCode(String ibgeCode);
}
