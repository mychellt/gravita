package br.gravita.core.usercases.inventory;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.inventory.PhysicalCount;
import br.gravita.core.ports.inbound.inventory.SubmitPhysicalCountCommand;
import br.gravita.core.ports.inbound.inventory.SubmitPhysicalCountUseCase;
import br.gravita.core.ports.outbound.persistence.inventory.PhysicalCountRepositoryPort;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class SubmitPhysicalCountService implements SubmitPhysicalCountUseCase {

	private final PhysicalCountRepositoryPort physicalCountRepositoryPort;

	public SubmitPhysicalCountService(PhysicalCountRepositoryPort physicalCountRepositoryPort) {
		this.physicalCountRepositoryPort = physicalCountRepositoryPort;
	}

	@Override
	@Transactional
	public PhysicalCount execute(SubmitPhysicalCountCommand command) {
		PhysicalCount physicalCount = physicalCountRepositoryPort.findById(command.physicalCountId())
				.orElseThrow(() -> new ResourceNotFoundException(
						"PhysicalCount not found: " + command.physicalCountId().value()));

		PhysicalCount updated = physicalCount.submitCounts(command.countedQuantities());

		return physicalCountRepositoryPort.save(updated);
	}
}
