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

/**
 * PhysicalCount aggregate (UC-M5-08/UC-M5-09). {@link #start} snapshots the
 * system on-hand quantities for the scoped products at count-start time, per
 * AC3, so later stock movements during counting don't retroactively change
 * what's being compared. A count is only ever created {@code IN_PROGRESS}
 * (AC4); reaching {@code PENDING_APPROVAL} and {@code APPROVED} is driven by
 * later use cases (UC-M5-09) not implemented here.
 */
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

	private PhysicalCount(PhysicalCountId id, PhysicalCountScope scope, String productGroupId, UUID warehouseId,
			PhysicalCountStatus status, UUID startedBy, Instant startedAt, List<PhysicalCountLine> lines) {
		this.id = Objects.requireNonNull(id, "id is required");
		this.scope = Objects.requireNonNull(scope, "scope is required");
		this.productGroupId = requireProductGroupMatchesScope(scope, productGroupId);
		this.warehouseId = Objects.requireNonNull(warehouseId, "warehouseId is required");
		this.status = Objects.requireNonNull(status, "status is required");
		this.startedBy = Objects.requireNonNull(startedBy, "startedBy is required");
		this.startedAt = Objects.requireNonNull(startedAt, "startedAt is required");
		this.lines = lines == null ? List.of() : List.copyOf(lines);
	}

	/**
	 * Opens a new count; per AC4 it always starts {@code IN_PROGRESS}.
	 */
	public static PhysicalCount start(PhysicalCountId id, PhysicalCountScope scope, String productGroupId,
			UUID warehouseId, UUID startedBy, Instant startedAt, List<PhysicalCountLine> lines) {
		return new PhysicalCount(id, scope, productGroupId, warehouseId, PhysicalCountStatus.IN_PROGRESS, startedBy,
				startedAt, lines);
	}

	/**
	 * Reconstructs a count from persistence, at any status in its lifecycle.
	 */
	public static PhysicalCount of(PhysicalCountId id, PhysicalCountScope scope, String productGroupId,
			UUID warehouseId, PhysicalCountStatus status, UUID startedBy, Instant startedAt,
			List<PhysicalCountLine> lines) {
		return new PhysicalCount(id, scope, productGroupId, warehouseId, status, startedBy, startedAt, lines);
	}

	private static String requireProductGroupMatchesScope(PhysicalCountScope scope, String productGroupId) {
		if (scope == PhysicalCountScope.PARTIAL_BY_GROUP && (productGroupId == null || productGroupId.isBlank())) {
			throw new BusinessRuleException("productGroupId is required when scope is PARTIAL_BY_GROUP");
		}
		return productGroupId;
	}

	/**
	 * Records counted quantities against an {@code IN_PROGRESS} count
	 * (UC-M5-08b). Submitting is cumulative: a line's previously recorded
	 * count survives a call that doesn't mention its product, so counting can
	 * be split across several submissions. Only once every line has a
	 * counted quantity does the count reach {@code PENDING_APPROVAL} and
	 * become eligible for UC-M5-09.
	 */
	public PhysicalCount submitCounts(Map<UUID, BigDecimal> countedQuantities) {
		if (status != PhysicalCountStatus.IN_PROGRESS) {
			throw new BusinessRuleException(
					"Only a count in IN_PROGRESS can have counts submitted; current status is " + status);
		}
		if (countedQuantities == null || countedQuantities.isEmpty()) {
			throw new BusinessRuleException("countedQuantities is required");
		}

		Set<UUID> knownProductIds = lines.stream().map(PhysicalCountLine::productId).collect(Collectors.toSet());
		for (Map.Entry<UUID, BigDecimal> entry : countedQuantities.entrySet()) {
			if (!knownProductIds.contains(entry.getKey())) {
				throw new BusinessRuleException("Product " + entry.getKey() + " is not part of this physical count");
			}
			if (entry.getValue() == null || entry.getValue().compareTo(BigDecimal.ZERO) < 0) {
				throw new BusinessRuleException(
						"Counted quantity for product " + entry.getKey() + " must not be negative");
			}
		}

		List<PhysicalCountLine> updatedLines = lines.stream()
				.map(line -> new PhysicalCountLine(line.productId(), line.systemQuantity(),
						countedQuantities.getOrDefault(line.productId(), line.countedQuantity())))
				.toList();

		boolean allCounted = updatedLines.stream().allMatch(line -> line.countedQuantity() != null);
		PhysicalCountStatus newStatus = allCounted ? PhysicalCountStatus.PENDING_APPROVAL
				: PhysicalCountStatus.IN_PROGRESS;

		return new PhysicalCount(id, scope, productGroupId, warehouseId, newStatus, startedBy, startedAt,
				updatedLines);
	}

	/**
	 * Transitions to {@code APPROVED} (UC-M5-09, AC1). Only a count that has
	 * had its counted quantities submitted, reaching {@code PENDING_APPROVAL},
	 * may be approved.
	 */
	public PhysicalCount approve() {
		if (status != PhysicalCountStatus.PENDING_APPROVAL) {
			throw new BusinessRuleException(
					"Only a count in PENDING_APPROVAL can be approved; current status is " + status);
		}
		return new PhysicalCount(id, scope, productGroupId, warehouseId, PhysicalCountStatus.APPROVED, startedBy,
				startedAt, lines);
	}
}
