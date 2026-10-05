package br.gravita.core.usercases.finance;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.finance.CostCenterShare;
import br.gravita.core.domain.finance.Payable;
import br.gravita.core.domain.finance.PayableId;
import br.gravita.core.ports.inbound.finance.SplitPayableByCostCenterCommand;
import br.gravita.core.ports.inbound.finance.SplitPayableByCostCenterUseCase;
import br.gravita.core.ports.outbound.persistence.CostCenterRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.PayableRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class SplitPayableByCostCenterService implements SplitPayableByCostCenterUseCase {

	private final PayableRepositoryPort payableRepositoryPort;
	private final CostCenterRepositoryPort costCenterRepositoryPort;

	public SplitPayableByCostCenterService(final PayableRepositoryPort payableRepositoryPort,
			final CostCenterRepositoryPort costCenterRepositoryPort) {
		this.payableRepositoryPort = payableRepositoryPort;
		this.costCenterRepositoryPort = costCenterRepositoryPort;
	}

	@Override
	@Transactional
	public Payable execute(final SplitPayableByCostCenterCommand command) {
		final Payable payable = payableRepositoryPort.findById(PayableId.of(command.payableId()))
				.orElseThrow(() -> new ResourceNotFoundException("Payable not found: " + command.payableId()));

		final Payable split = payable.withCostCenterSplit(command.split());

		for (final CostCenterShare share : split.getCostCenterSplit()) {
			costCenterRepositoryPort.get(share.costCenterId()).orElseThrow(
					() -> new ResourceNotFoundException("Cost center not found: " + share.costCenterId()));
		}

		return payableRepositoryPort.save(split);
	}
}
