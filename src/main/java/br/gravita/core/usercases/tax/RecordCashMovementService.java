package br.gravita.core.usercases.tax;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.tax.CashMovement;
import br.gravita.core.domain.tax.CashMovementId;
import br.gravita.core.domain.tax.PosSession;
import br.gravita.core.domain.tax.PosSessionId;
import br.gravita.core.domain.tax.PosSessionStatus;
import br.gravita.core.ports.inbound.tax.RecordCashMovementCommand;
import br.gravita.core.ports.inbound.tax.RecordCashMovementUseCase;
import br.gravita.core.ports.outbound.persistence.tax.CashMovementRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.PosSessionRepositoryPort;

import java.time.Instant;
import java.util.UUID;

@UseCase
public class RecordCashMovementService implements RecordCashMovementUseCase {

	private final PosSessionRepositoryPort posSessionRepositoryPort;
	private final CashMovementRepositoryPort cashMovementRepositoryPort;

	public RecordCashMovementService(PosSessionRepositoryPort posSessionRepositoryPort,
			CashMovementRepositoryPort cashMovementRepositoryPort) {
		this.posSessionRepositoryPort = posSessionRepositoryPort;
		this.cashMovementRepositoryPort = cashMovementRepositoryPort;
	}

	@Override
	public CashMovementId execute(RecordCashMovementCommand command) {
		if (command.justification() == null || command.justification().isBlank()) {
			throw new BusinessRuleException("Cash movement justification is required");
		}

		PosSessionId sessionId = PosSessionId.of(command.sessionId());
		PosSession session = posSessionRepositoryPort.findById(sessionId)
				.orElseThrow(() -> new ResourceNotFoundException("PosSession not found: " + command.sessionId()));
		if (session.getStatus() != PosSessionStatus.OPEN) {
			throw new BusinessRuleException("PosSession " + command.sessionId() + " is not open");
		}

		CashMovement movement = CashMovement.of(CashMovementId.of(UUID.randomUUID()), sessionId, command.type(),
				command.amount(), command.justification(), Instant.now());

		return cashMovementRepositoryPort.save(movement).getId();
	}
}
