package br.gravita.core.usercases.tax;

import br.gravita.core.domain.shared.UseCase;
import br.gravita.core.usercases.system.ConfigureApprovalAlcadaCommand;
import br.gravita.core.usercases.system.ConfigureApprovalAlcadaUseCase;
import br.gravita.core.ports.outbound.persistence.system.ApprovalAlcadaRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.ProfileRepositoryPort;
import br.gravita.core.domain.system.ApprovalAlcada;
import br.gravita.core.domain.system.ApprovalModule;
import br.gravita.core.domain.system.ProfileReference;
import br.gravita.core.domain.system.UnknownProfileException;

@UseCase
public class ConfigureApprovalAlcadaService implements ConfigureApprovalAlcadaUseCase {

	private final ApprovalAlcadaRepositoryPort approvalAlcadaRepositoryPort;
	private final ProfileRepositoryPort profileRepositoryPort;

	public ConfigureApprovalAlcadaService(ApprovalAlcadaRepositoryPort approvalAlcadaRepositoryPort,
			ProfileRepositoryPort profileRepositoryPort) {
		this.approvalAlcadaRepositoryPort = approvalAlcadaRepositoryPort;
		this.profileRepositoryPort = profileRepositoryPort;
	}

	@Override
	public void execute(ConfigureApprovalAlcadaCommand command) {
		ApprovalModule module = ApprovalModule.fromCode(command.module());
		ProfileReference approverProfile = profileRepositoryPort.findById(command.approverProfileId())
				.orElseThrow(() -> new UnknownProfileException(command.approverProfileId()));

		ApprovalAlcada alcada = approvalAlcadaRepositoryPort.findByModule(module)
				.map(existing -> {
					existing.reconfigure(command.thresholdValue(), command.thresholdDiscountPercent(), approverProfile);
					return existing;
				})
				.orElseGet(() -> ApprovalAlcada.configure(module, command.thresholdValue(),
						command.thresholdDiscountPercent(), approverProfile));

		approvalAlcadaRepositoryPort.save(alcada);
	}
}
