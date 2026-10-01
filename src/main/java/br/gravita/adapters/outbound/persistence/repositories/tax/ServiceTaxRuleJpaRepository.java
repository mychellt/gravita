package br.gravita.adapters.outbound.persistence.repositories.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.ServiceTaxRuleJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServiceTaxRuleJpaRepository extends JpaRepository<ServiceTaxRuleJpaEntity, UUID> {

	@Query("select r from ServiceTaxRuleJpaEntity r where r.serviceCode = :serviceCode and r.active = true "
			+ "and (r.municipalityIbge is null or r.municipalityIbge = :municipalityIbge)")
	List<ServiceTaxRuleJpaEntity> findCandidates(@Param("serviceCode") String serviceCode,
			@Param("municipalityIbge") String municipalityIbge);
}
