package br.gravita.system.application.port.out;

import br.gravita.system.domain.model.IntegrationCredential;
import br.gravita.system.domain.model.IntegrationEnvironment;
import br.gravita.system.domain.model.IntegrationName;

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
