package br.gravita.core.domain.sales;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

/**
 * Append-only CRM interaction log entry: feeds the customer timeline (M1) and
 * follow-up rule evaluation (UC-M7-12), so rows are only ever inserted, never edited.
 */
@Getter
public final class Interaction {

	private final InteractionId id;
	private final OpportunityId opportunityId;
	private final UUID customerId;
	private final InteractionChannel channel;
	private final String summary;
	private final Instant timestamp;

	public Interaction(final InteractionId id, final OpportunityId opportunityId, final UUID customerId, final InteractionChannel channel,
			final String summary, final Instant timestamp) {
		this.id = Objects.requireNonNull(id, "id is required");
		if (opportunityId == null && customerId == null) {
			throw new BusinessRuleException("An interaction must link to an opportunity, a customer, or both");
		}
		this.opportunityId = opportunityId;
		this.customerId = customerId;
		this.channel = Objects.requireNonNull(channel, "channel is required");
		this.summary = requireNonBlank(summary);
		this.timestamp = Objects.requireNonNull(timestamp, "timestamp is required");
	}

	public static Interaction log(final OpportunityId opportunityId, final UUID customerId, final InteractionChannel channel,
			final String summary, final Instant timestamp) {
		return new Interaction(InteractionId.of(UUID.randomUUID()), opportunityId, customerId, channel, summary,
				timestamp);
	}

	public static Interaction of(final InteractionId id, final OpportunityId opportunityId, final UUID customerId,
			final InteractionChannel channel, final String summary, final Instant timestamp) {
		return new Interaction(id, opportunityId, customerId, channel, summary, timestamp);
	}

	private static String requireNonBlank(final String summary) {
		if (summary == null || summary.isBlank()) {
			throw new BusinessRuleException("summary is required");
		}
		return summary;
	}
}
