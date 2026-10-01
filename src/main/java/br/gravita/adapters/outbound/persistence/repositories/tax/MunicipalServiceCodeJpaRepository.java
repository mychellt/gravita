package br.gravita.adapters.outbound.persistence.repositories.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.MunicipalServiceCodeJpaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MunicipalServiceCodeJpaRepository extends JpaRepository<MunicipalServiceCodeJpaEntity, UUID> {

	boolean existsByMunicipalityIbgeAndActiveTrue(String municipalityIbge);

	boolean existsByMunicipalityIbgeAndServiceCodeAndActiveTrue(String municipalityIbge, String serviceCode);
}
