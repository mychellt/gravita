package br.gravita.core.ports.outbound.persistence;

import br.gravita.core.domain.ChartOfAccountsDomain;
import br.gravita.core.ports.outbound.persistence.commons.BaseRepositoryPort;

import java.util.UUID;

public interface ChartOfAccountsRepositoryPort extends BaseRepositoryPort<ChartOfAccountsDomain> {
	void deleteById(final UUID id);
	boolean existsByParentId(final UUID parentId);
}
