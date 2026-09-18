package br.gravita.core.ports.persistence;

import br.gravita.core.domain.ChartOfAccountsDomain;
import br.gravita.core.ports.persistence.commons.BaseRepositoryPort;

import java.util.UUID;

public interface ChartOfAccountsRepositoryPort extends BaseRepositoryPort<ChartOfAccountsDomain> {
	void deleteById(final UUID id);
	boolean existsByParentId(final UUID parentId);
}
