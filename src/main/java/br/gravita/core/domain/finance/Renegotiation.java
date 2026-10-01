package br.gravita.core.domain.finance;

import br.gravita.core.domain.shared.BusinessRuleException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

/**
 * The agreement that replaced one or more overdue {@link Receivable}s of a
 * customer by a new installment plan. It keeps both sides of the link - the
 * original titles and the titles created from the plan - for audit and for the
 * customer statement. Immutable.
 */
@Getter
public final class Renegotiation {

	private final RenegotiationId id;
	private final UUID customerId;
	private final List<ReceivableId> originalReceivableIds;
	private final List<ReceivableId> newReceivableIds;
	private final Instant createdAt;

	public Renegotiation(RenegotiationId id, UUID customerId, List<ReceivableId> originalReceivableIds,
			List<ReceivableId> newReceivableIds, Instant createdAt) {
		this.id = Objects.requireNonNull(id, "id is required");
		this.customerId = Objects.requireNonNull(customerId, "customerId is required");
		this.originalReceivableIds = requireDistinctAndNotEmpty("originalReceivableIds", originalReceivableIds);
		this.newReceivableIds = requireDistinctAndNotEmpty("newReceivableIds", newReceivableIds);
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
		if (this.originalReceivableIds.stream().anyMatch(this.newReceivableIds::contains)) {
			throw new BusinessRuleException("A receivable cannot be both renegotiated and created by the renegotiation");
		}
	}

	public static Renegotiation create(RenegotiationId id, UUID customerId, List<ReceivableId> originalReceivableIds,
			List<ReceivableId> newReceivableIds, Instant createdAt) {
		return new Renegotiation(id, customerId, originalReceivableIds, newReceivableIds, createdAt);
	}

	public static Renegotiation of(RenegotiationId id, UUID customerId, List<ReceivableId> originalReceivableIds,
			List<ReceivableId> newReceivableIds, Instant createdAt) {
		return new Renegotiation(id, customerId, originalReceivableIds, newReceivableIds, createdAt);
	}

	private static List<ReceivableId> requireDistinctAndNotEmpty(String field, List<ReceivableId> ids) {
		Objects.requireNonNull(ids, field + " is required");
		if (ids.isEmpty()) {
			throw new BusinessRuleException(field + " must not be empty");
		}
		if (new HashSet<>(ids).size() != ids.size()) {
			throw new BusinessRuleException(field + " must not contain duplicates");
		}
		return List.copyOf(ids);
	}
}
