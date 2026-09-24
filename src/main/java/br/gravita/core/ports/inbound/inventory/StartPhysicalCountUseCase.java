package br.gravita.core.ports.inbound.inventory;

import br.gravita.core.domain.inventory.PhysicalCount;

public interface StartPhysicalCountUseCase {
	PhysicalCount execute(StartPhysicalCountCommand command);
}
