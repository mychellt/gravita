package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.tax.PosSession;
import br.gravita.core.domain.tax.PosSessionId;
import br.gravita.core.domain.tax.PosSessionStatus;
import br.gravita.core.ports.inbound.tax.OpenPosSessionCommand;
import br.gravita.core.ports.inbound.tax.OpenPosSessionUseCase;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.PosSessionRepositoryPort;

import java.time.Instant;
import java.util.UUID;

@UseCase
public class OpenPosSessionService implements OpenPosSessionUseCase {

	private final PosSessionRepositoryPort posSessionRepositoryPort;
	private final CompanyRepositoryPort companyRepositoryPort;

	public OpenPosSessionService(PosSessionRepositoryPort posSessionRepositoryPort,
			CompanyRepositoryPort companyRepositoryPort) {
		this.posSessionRepositoryPort = posSessionRepositoryPort;
		this.companyRepositoryPort = companyRepositoryPort;
	}

	@Override
	public PosSessionId execute(OpenPosSessionCommand command) {
		if (posSessionRepositoryPort.existsByRegisterIdAndStatus(command.registerId(), PosSessionStatus.OPEN)) {
			throw new BusinessRuleException("Register " + command.registerId() + " already has an open session");
		}
		companyRepositoryPort.findById(command.companyId())
				.orElseThrow(() -> new BusinessRuleException("Company not found: " + command.companyId().value()));

		PosSession session = PosSession.open(PosSessionId.of(UUID.randomUUID()), command.registerId(),
				command.operatorId(), command.companyId(), command.openingChangeAmount(), Instant.now());

		return posSessionRepositoryPort.save(session).getId();
	}
}
