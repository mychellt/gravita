package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.tax.MunicipalityIntegration;
import br.gravita.core.domain.tax.MunicipalityIntegrationId;
import br.gravita.core.ports.inbound.tax.RegisterMunicipalityIntegrationCommand;
import br.gravita.core.ports.inbound.tax.RegisterMunicipalityIntegrationUseCase;
import br.gravita.core.ports.outbound.persistence.tax.MunicipalityIntegrationRepositoryPort;
import java.util.UUID;

@UseCase
public class RegisterMunicipalityIntegrationService implements RegisterMunicipalityIntegrationUseCase {

	private final MunicipalityIntegrationRepositoryPort repositoryPort;

	public RegisterMunicipalityIntegrationService(final MunicipalityIntegrationRepositoryPort repositoryPort) {
		this.repositoryPort = repositoryPort;
	}

	@Override
	public MunicipalityIntegrationId execute(final RegisterMunicipalityIntegrationCommand command) {
		final MunicipalityIntegration integration = repositoryPort.findByIbgeCode(command.ibgeCode())
				.map(existing -> {
					existing.update(command.standard(), command.version(), command.webserviceUrl(),
							command.requiredCertificateType(), command.requiredFields(), command.homologated());
					return existing;
				})
				.orElseGet(() -> MunicipalityIntegration.of(MunicipalityIntegrationId.of(UUID.randomUUID()),
						command.ibgeCode(), command.standard(), command.version(), command.webserviceUrl(),
						command.requiredCertificateType(), command.requiredFields(), command.homologated()));

		// No IssueNfsePort lookup here: a standard without a deployed adapter only surfaces at transmission time.
		return repositoryPort.save(integration).getId();
	}
}
