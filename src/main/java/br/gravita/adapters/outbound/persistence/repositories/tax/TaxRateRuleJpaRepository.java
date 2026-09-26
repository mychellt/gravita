package br.gravita.adapters.outbound.persistence.repositories.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.TaxRateRuleJpaEntity;
import br.gravita.core.domain.tax.TaxRegime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxRateRuleJpaRepository extends JpaRepository<TaxRateRuleJpaEntity, UUID> {

	List<TaxRateRuleJpaEntity> findByNcmAndOriginStateAndDestinationStateAndRegimeAndOperationType(String ncm,
			String originState, String destinationState, TaxRegime regime, String operationType);
}
