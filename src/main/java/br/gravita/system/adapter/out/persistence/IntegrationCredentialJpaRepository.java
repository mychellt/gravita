package br.gravita.system.adapter.out.persistence;

import br.gravita.system.domain.model.IntegrationEnvironment;
import br.gravita.system.domain.model.IntegrationName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface IntegrationCredentialJpaRepository extends JpaRepository<IntegrationCredentialJpaEntity, UUID> {
	Optional<IntegrationCredentialJpaEntity> findByIntegrationNameAndEnvironment(IntegrationName integrationName,
			IntegrationEnvironment environment);
}
