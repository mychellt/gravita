package br.gravita.system.application.service;

import br.gravita.shared.UseCase;
import br.gravita.system.application.port.in.ConfigureIntegrationCredentialCommand;
import br.gravita.system.application.port.in.ConfigureIntegrationCredentialUseCase;
import br.gravita.system.application.port.out.IntegrationCredentialRepositoryPort;
import br.gravita.system.domain.model.IntegrationCredential;
import br.gravita.system.domain.model.IntegrationName;

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
