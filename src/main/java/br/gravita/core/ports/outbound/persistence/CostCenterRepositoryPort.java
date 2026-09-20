package br.gravita.core.ports.outbound.persistence;

import br.gravita.core.domain.CostCenterDomain;
import br.gravita.core.ports.outbound.persistence.commons.BaseRepositoryPort;

import java.util.UUID;

public interface CostCenterRepositoryPort extends BaseRepositoryPort<CostCenterDomain> {
	void deleteById(final UUID id);
	boolean existsByParentId(final UUID parentId);
}
