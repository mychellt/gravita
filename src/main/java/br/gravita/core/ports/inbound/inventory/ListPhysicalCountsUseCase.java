package br.gravita.core.ports.inbound.inventory;

import br.gravita.core.domain.shared.Page;

public interface ListPhysicalCountsUseCase {
	/** Newest first. */
	Page<PhysicalCountItemView> execute(ListPhysicalCountsQuery query);
}
