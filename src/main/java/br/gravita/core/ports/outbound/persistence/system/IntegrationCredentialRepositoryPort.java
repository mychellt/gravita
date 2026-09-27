package br.gravita.core.ports.outbound.persistence.system;

import br.gravita.core.domain.system.IntegrationCredential;
import br.gravita.core.domain.system.IntegrationEnvironment;
import br.gravita.core.domain.system.IntegrationName;

import java.util.Optional;

public interface IntegrationCredentialRepositoryPort {

	IntegrationCredential save(IntegrationCredential credential);

	Optional<IntegrationCredential> findByIntegrationNameAndEnvironment(IntegrationName integrationName,
			IntegrationEnvironment environment);
}
