package br.gravita.core.ports.outbound.persistence.inventory;

import br.gravita.core.domain.inventory.SerialUnit;

import java.util.List;

public interface SerialUnitRepositoryPort {
	List<SerialUnit> saveAll(List<SerialUnit> serialUnits);
}
