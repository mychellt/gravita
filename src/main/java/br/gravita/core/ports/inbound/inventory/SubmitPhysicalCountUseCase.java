package br.gravita.core.ports.inbound.inventory;

import br.gravita.core.domain.inventory.PhysicalCount;

public interface SubmitPhysicalCountUseCase {
	PhysicalCount execute(SubmitPhysicalCountCommand command);
}
