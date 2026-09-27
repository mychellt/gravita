package br.gravita.core.ports.outbound.persistence.sales;

import br.gravita.core.domain.sales.FollowUpTask;
import java.time.LocalDate;
import java.util.UUID;

public interface FollowUpTaskRepositoryPort {
	FollowUpTask save(FollowUpTask task);

	/**
	 * Query shape UC-M7-12 (Evaluate Follow-up Rules) uses to avoid sending a
	 * duplicate alert for a breach already notified on the given date. Exactly
	 * one of opportunityId/customerId is expected to be non-null.
	 */
	boolean existsForTargetOnDate(UUID opportunityId, UUID customerId, LocalDate dueDate);
}
