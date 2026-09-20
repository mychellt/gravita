package br.gravita.system.application.service;

import br.gravita.shared.UseCase;
import br.gravita.system.application.port.in.ConfigureApprovalAlcadaCommand;
import br.gravita.system.application.port.in.ConfigureApprovalAlcadaUseCase;
import br.gravita.system.application.port.out.ApprovalAlcadaRepositoryPort;
import br.gravita.system.application.port.out.ProfileRepositoryPort;
import br.gravita.system.domain.model.ApprovalAlcada;
import br.gravita.system.domain.model.ApprovalModule;
import br.gravita.system.domain.model.ProfileReference;
import br.gravita.system.domain.model.UnknownProfileException;

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
