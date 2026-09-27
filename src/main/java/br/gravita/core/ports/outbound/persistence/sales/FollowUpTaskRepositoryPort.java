package br.gravita.core.ports.outbound.persistence.sales;

import br.gravita.core.domain.sales.FollowUpTask;

public interface FollowUpTaskRepositoryPort {
	FollowUpTask save(FollowUpTask task);
}
