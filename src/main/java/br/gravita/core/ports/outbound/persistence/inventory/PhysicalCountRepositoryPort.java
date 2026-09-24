package br.gravita.core.ports.outbound.persistence.inventory;

import br.gravita.core.domain.inventory.PhysicalCount;
import br.gravita.core.domain.inventory.PhysicalCountId;

import java.util.Optional;

public interface PhysicalCountRepositoryPort {

	PhysicalCount save(PhysicalCount physicalCount);

	Optional<PhysicalCount> findById(PhysicalCountId id);
}
