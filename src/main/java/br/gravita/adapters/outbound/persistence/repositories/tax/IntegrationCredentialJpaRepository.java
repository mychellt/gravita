package br.gravita.adapters.outbound.persistence.repositories.tax;

import br.gravita.adapters.outbound.persistence.entities.tax.IntegrationCredentialJpaEntity;
import br.gravita.core.domain.system.IntegrationEnvironment;
import br.gravita.core.domain.system.IntegrationName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface IntegrationCredentialJpaRepository extends JpaRepository<IntegrationCredentialJpaEntity, UUID> {
	Optional<IntegrationCredentialJpaEntity> findByIntegrationNameAndEnvironment(IntegrationName integrationName,
			IntegrationEnvironment environment);
}
