package br.gravita.core.usercases.finance;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.ports.inbound.finance.AdjustReceivableForReturnCommand;
import br.gravita.core.ports.inbound.finance.AdjustReceivableForReturnUseCase;
import br.gravita.core.ports.outbound.persistence.finance.ReceivableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.SettlementRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class AdjustReceivableForReturnService implements AdjustReceivableForReturnUseCase {

	private final ReceivableRepositoryPort receivableRepositoryPort;
	private final SettlementRepositoryPort settlementRepositoryPort;

	public AdjustReceivableForReturnService(final ReceivableRepositoryPort receivableRepositoryPort,
			final SettlementRepositoryPort settlementRepositoryPort) {
		this.receivableRepositoryPort = receivableRepositoryPort;
		this.settlementRepositoryPort = settlementRepositoryPort;
	}

	/**
	 * The settlements are read to tell how much of the title is still unsettled:
	 * that is the only part a return can adjust (see
	 * {@link Receivable#adjustForReturn}).
	 */
	@Override
	@Transactional
	public Receivable execute(final AdjustReceivableForReturnCommand command) {
		final Receivable receivable = receivableRepositoryPort.findById(ReceivableId.of(command.receivableId()))
				.orElseThrow(() -> new ResourceNotFoundException("Receivable not found: " + command.receivableId()));
		final Receivable adjusted = receivable.adjustForReturn(command.returnedAmount(),
				settlementRepositoryPort.findByReceivableId(receivable.getId()));
		return receivableRepositoryPort.save(adjusted);
	}
}
