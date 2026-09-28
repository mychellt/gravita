package br.gravita.core.usercases.finance;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.finance.CostCenterShare;
import br.gravita.core.domain.finance.Payable;
import br.gravita.core.domain.finance.PayableId;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.ports.inbound.finance.CreateManualPayableCommand;
import br.gravita.core.ports.inbound.finance.CreateManualPayableUseCase;
import br.gravita.core.ports.outbound.persistence.CostCenterRepositoryPort;
import br.gravita.core.ports.outbound.persistence.SupplierRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.PayableRepositoryPort;
import java.util.List;
import java.util.UUID;

@UseCase
public class CreateManualPayableService implements CreateManualPayableUseCase {

	private final PayableRepositoryPort payableRepositoryPort;
	private final SupplierRepositoryPort supplierRepositoryPort;
	private final CostCenterRepositoryPort costCenterRepositoryPort;

	public CreateManualPayableService(PayableRepositoryPort payableRepositoryPort,
			SupplierRepositoryPort supplierRepositoryPort, CostCenterRepositoryPort costCenterRepositoryPort) {
		this.payableRepositoryPort = payableRepositoryPort;
		this.supplierRepositoryPort = supplierRepositoryPort;
		this.costCenterRepositoryPort = costCenterRepositoryPort;
	}

	@Override
	public Payable execute(CreateManualPayableCommand command) {
		if (command.supplierId() != null) {
			supplierRepositoryPort.findById(SupplierId.of(command.supplierId()))
					.orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + command.supplierId()));
		}

		PayableId id = PayableId.of(UUID.randomUUID());
		Payable payable = Payable.createManual(id, command.supplierId(), command.amount(), command.dueDate(),
				command.costCenterSplit());

		requireCostCentersExist(payable.getCostCenterSplit());

		return payableRepositoryPort.save(payable);
	}

	private void requireCostCentersExist(List<CostCenterShare> split) {
		for (CostCenterShare share : split) {
			costCenterRepositoryPort.get(share.costCenterId()).orElseThrow(
					() -> new ResourceNotFoundException("Cost center not found: " + share.costCenterId()));
		}
	}
}
