package br.gravita.core.usercases.finance;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.finance.CashMovement;
import br.gravita.core.domain.finance.CashMovementId;
import br.gravita.core.domain.finance.InternalCashBox;
import br.gravita.core.domain.finance.InternalCashBoxId;
import br.gravita.core.ports.inbound.finance.RecordInternalCashMovementCommand;
import br.gravita.core.ports.inbound.finance.RecordInternalCashMovementUseCase;
import br.gravita.core.ports.outbound.persistence.finance.InternalCashBoxRepositoryPort;
import java.time.Instant;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class RecordInternalCashMovementService implements RecordInternalCashMovementUseCase {

	private final InternalCashBoxRepositoryPort internalCashBoxRepositoryPort;

	public RecordInternalCashMovementService(final InternalCashBoxRepositoryPort internalCashBoxRepositoryPort) {
		this.internalCashBoxRepositoryPort = internalCashBoxRepositoryPort;
	}

	@Override
	@Transactional
	public CashMovement execute(final RecordInternalCashMovementCommand command) {
		final InternalCashBox cashBox = internalCashBoxRepositoryPort.findByIdForUpdate(InternalCashBoxId.MAIN)
				.orElseThrow(() -> new ResourceNotFoundException("Internal cash box not found"));

		final CashMovement movement = CashMovement.of(CashMovementId.of(UUID.randomUUID()), cashBox.getId(),
				command.direction(), command.amount(), command.justification(), Instant.now());

		internalCashBoxRepositoryPort.save(cashBox.apply(movement));
		return internalCashBoxRepositoryPort.saveMovement(movement);
	}
}
