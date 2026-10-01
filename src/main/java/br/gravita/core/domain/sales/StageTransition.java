package br.gravita.core.domain.sales;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

/**
 * Append-only log entry recorded on every {@code changeStage} call. Never edited or
 * backfilled — it is the only data source UC-M7-15 (funnel conversion analytics) has
 * to compute cycle time between stages.
 */
@Getter
public final class StageTransition {

	private final StageTransitionId id;
	private final OpportunityId opportunityId;
	private final OpportunityStage fromStage;
	private final OpportunityStage toStage;
	private final Instant timestamp;

	public StageTransition(StageTransitionId id, OpportunityId opportunityId, OpportunityStage fromStage,
			OpportunityStage toStage, Instant timestamp) {
		this.id = Objects.requireNonNull(id, "id is required");
		this.opportunityId = Objects.requireNonNull(opportunityId, "opportunityId is required");
		this.fromStage = Objects.requireNonNull(fromStage, "fromStage is required");
		this.toStage = Objects.requireNonNull(toStage, "toStage is required");
		if (fromStage == toStage) {
			throw new BusinessRuleException("A stage transition must change the stage: " + fromStage);
		}
		this.timestamp = Objects.requireNonNull(timestamp, "timestamp is required");
	}

	public static StageTransition append(OpportunityId opportunityId, OpportunityStage fromStage,
			OpportunityStage toStage) {
		return new StageTransition(StageTransitionId.of(UUID.randomUUID()), opportunityId, fromStage, toStage,
				Instant.now());
	}

	public static StageTransition of(StageTransitionId id, OpportunityId opportunityId, OpportunityStage fromStage,
			OpportunityStage toStage, Instant timestamp) {
		return new StageTransition(id, opportunityId, fromStage, toStage, timestamp);
	}
}
