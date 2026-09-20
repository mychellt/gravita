package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.usercases.system.ConfigureIntegrationCredentialCommand;
import br.gravita.core.usercases.system.ConfigureIntegrationCredentialUseCase;
import br.gravita.core.ports.outbound.persistence.system.IntegrationCredentialRepositoryPort;
import br.gravita.core.domain.system.IntegrationCredential;
import br.gravita.core.domain.system.IntegrationName;

@UseCase
public class ConfigureIntegrationCredentialService implements ConfigureIntegrationCredentialUseCase {

	private final IntegrationCredentialRepositoryPort repositoryPort;

	public ConfigureIntegrationCredentialService(IntegrationCredentialRepositoryPort repositoryPort) {
		this.repositoryPort = repositoryPort;
	}

	@Override
	public void execute(ConfigureIntegrationCredentialCommand command) {
		IntegrationName integrationName = IntegrationName.fromCode(command.integrationName());

		IntegrationCredential credential = repositoryPort
				.findByIntegrationNameAndEnvironment(integrationName, command.environment())
				.map(existing -> {
					existing.rotate(command.endpoint(), command.credentialPayload());
					return existing;
				})
				.orElseGet(() -> IntegrationCredential.register(integrationName, command.environment(),
						command.endpoint(), command.credentialPayload()));

		repositoryPort.save(credential);
	}
}
