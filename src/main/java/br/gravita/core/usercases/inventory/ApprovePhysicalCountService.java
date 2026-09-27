package br.gravita.core.usercases.inventory;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.inventory.PhysicalCount;
import br.gravita.core.domain.inventory.PhysicalCountLine;
import br.gravita.core.ports.inbound.inventory.AdjustInventoryCommand;
import br.gravita.core.ports.inbound.inventory.AdjustInventoryUseCase;
import br.gravita.core.ports.inbound.inventory.ApprovePhysicalCountCommand;
import br.gravita.core.ports.inbound.inventory.ApprovePhysicalCountUseCase;
import br.gravita.core.ports.outbound.persistence.inventory.PhysicalCountRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class ApprovePhysicalCountService implements ApprovePhysicalCountUseCase {

	private final PhysicalCountRepositoryPort physicalCountRepositoryPort;
	private final AdjustInventoryUseCase adjustInventoryUseCase;

	public ApprovePhysicalCountService(PhysicalCountRepositoryPort physicalCountRepositoryPort,
			AdjustInventoryUseCase adjustInventoryUseCase) {
		this.physicalCountRepositoryPort = physicalCountRepositoryPort;
		this.adjustInventoryUseCase = adjustInventoryUseCase;
	}

	@Override
	@Transactional
	public PhysicalCount execute(ApprovePhysicalCountCommand command) {
		PhysicalCount physicalCount = physicalCountRepositoryPort.findById(command.physicalCountId())
				.orElseThrow(() -> new ResourceNotFoundException(
						"PhysicalCount not found: " + command.physicalCountId().value()));

		PhysicalCount approved = physicalCount.approve();

		String justification = "Physical count " + command.physicalCountId().value() + " approval";
		for (PhysicalCountLine line : physicalCount.getLines()) {
			if (line.hasDivergence()) {
				adjustInventoryUseCase.execute(new AdjustInventoryCommand(line.productId(),
						physicalCount.getWarehouseId(), line.divergence(), justification, command.approvedBy()));
			}
		}

		return physicalCountRepositoryPort.save(approved);
	}
}
