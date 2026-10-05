package br.gravita.core.domain.inventory;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
public final class PhysicalCount {

	private final PhysicalCountId id;
	private final PhysicalCountScope scope;
	private final String productGroupId;
	private final UUID warehouseId;
	private final PhysicalCountStatus status;
	private final UUID startedBy;
	private final Instant startedAt;
	private final List<PhysicalCountLine> lines;

	public PhysicalCount(final PhysicalCountId id, final PhysicalCountScope scope, final String productGroupId, final UUID warehouseId,
			final PhysicalCountStatus status, final UUID startedBy, final Instant startedAt, final List<PhysicalCountLine> lines) {
		this.id = Objects.requireNonNull(id, "id is required");
		this.scope = Objects.requireNonNull(scope, "scope is required");
		this.productGroupId = requireProductGroupMatchesScope(scope, productGroupId);
		this.warehouseId = Objects.requireNonNull(warehouseId, "warehouseId is required");
		this.status = Objects.requireNonNull(status, "status is required");
		this.startedBy = Objects.requireNonNull(startedBy, "startedBy is required");
		this.startedAt = Objects.requireNonNull(startedAt, "startedAt is required");
		this.lines = lines == null ? List.of() : List.copyOf(lines);
	}

	public static PhysicalCount start(final PhysicalCountId id, final PhysicalCountScope scope, final String productGroupId,
			final UUID warehouseId, final UUID startedBy, final Instant startedAt, final List<PhysicalCountLine> lines) {
		return new PhysicalCount(id, scope, productGroupId, warehouseId, PhysicalCountStatus.IN_PROGRESS, startedBy,
				startedAt, lines);
	}

	public static PhysicalCount of(final PhysicalCountId id, final PhysicalCountScope scope, final String productGroupId,
			final UUID warehouseId, final PhysicalCountStatus status, final UUID startedBy, final Instant startedAt,
			final List<PhysicalCountLine> lines) {
		return new PhysicalCount(id, scope, productGroupId, warehouseId, status, startedBy, startedAt, lines);
	}

	private static String requireProductGroupMatchesScope(final PhysicalCountScope scope, final String productGroupId) {
		if (scope == PhysicalCountScope.PARTIAL_BY_GROUP && (productGroupId == null || productGroupId.isBlank())) {
			throw new BusinessRuleException("productGroupId is required when scope is PARTIAL_BY_GROUP");
		}
		return productGroupId;
	}

	public PhysicalCount submitCounts(final Map<UUID, BigDecimal> countedQuantities) {
		if (status != PhysicalCountStatus.IN_PROGRESS) {
			throw new BusinessRuleException(
					"Only a count in IN_PROGRESS can have counts submitted; current status is " + status);
		}
		if (countedQuantities == null || countedQuantities.isEmpty()) {
			throw new BusinessRuleException("countedQuantities is required");
		}

		final Set<UUID> knownProductIds = lines.stream().map(PhysicalCountLine::productId).collect(Collectors.toSet());
		for (final Map.Entry<UUID, BigDecimal> entry : countedQuantities.entrySet()) {
			if (!knownProductIds.contains(entry.getKey())) {
				throw new BusinessRuleException("Product " + entry.getKey() + " is not part of this physical count");
			}
			if (entry.getValue() == null || entry.getValue().compareTo(BigDecimal.ZERO) < 0) {
				throw new BusinessRuleException(
						"Counted quantity for product " + entry.getKey() + " must not be negative");
			}
		}

		final List<PhysicalCountLine> updatedLines = lines.stream()
				.map(line -> new PhysicalCountLine(line.productId(), line.systemQuantity(),
						countedQuantities.getOrDefault(line.productId(), line.countedQuantity())))
				.toList();

		final boolean allCounted = updatedLines.stream().allMatch(line -> line.countedQuantity() != null);
		final PhysicalCountStatus newStatus = allCounted ? PhysicalCountStatus.PENDING_APPROVAL
				: PhysicalCountStatus.IN_PROGRESS;

		return new PhysicalCount(id, scope, productGroupId, warehouseId, newStatus, startedBy, startedAt,
				updatedLines);
	}

	public PhysicalCount approve() {
		if (status != PhysicalCountStatus.PENDING_APPROVAL) {
			throw new BusinessRuleException(
					"Only a count in PENDING_APPROVAL can be approved; current status is " + status);
		}
		return new PhysicalCount(id, scope, productGroupId, warehouseId, PhysicalCountStatus.APPROVED, startedBy,
				startedAt, lines);
	}
}
