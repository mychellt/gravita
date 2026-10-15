package br.gravita.core.ports.outbound.persistence.inventory;

import br.gravita.core.domain.inventory.PhysicalCount;
import br.gravita.core.domain.inventory.PhysicalCountId;
import br.gravita.core.domain.shared.Page;
import br.gravita.core.ports.inbound.inventory.ListPhysicalCountsQuery;

import java.util.Optional;
import java.util.UUID;

public interface PhysicalCountRepositoryPort {

	PhysicalCount save(PhysicalCount physicalCount);

	Optional<PhysicalCount> findById(PhysicalCountId id);

	/** The warehouse's counts, optionally of one status, newest first. */
	Page<PhysicalCount> search(UUID warehouseId, ListPhysicalCountsQuery query);
}
