package br.gravita.core.ports.outbound.persistence.system;

import br.gravita.core.domain.system.IntegrationCredential;
import br.gravita.core.domain.system.IntegrationEnvironment;
import br.gravita.core.domain.system.IntegrationName;

import java.util.Optional;

/**
 * What every adapter implementation (SEFAZ client, Receita Federal client, bank clients,
 * WhatsApp Business API client, e-commerce webhooks, accounting export) calls at call time to
 * read the credential currently configured for it - so rotating a credential never requires
 * redeploying the adapter that consumes it.
 */
public interface IntegrationCredentialRepositoryPort {

	IntegrationCredential save(IntegrationCredential credential);

	Optional<IntegrationCredential> findByIntegrationNameAndEnvironment(IntegrationName integrationName,
			IntegrationEnvironment environment);
}
