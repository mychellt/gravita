package br.gravita.adapters.inbound.controllers.inventory.dtos;

import br.gravita.core.domain.inventory.PhysicalCount;
import br.gravita.core.domain.inventory.PhysicalCountLine;
import br.gravita.core.domain.inventory.PhysicalCountScope;
import br.gravita.core.domain.inventory.PhysicalCountStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ApprovePhysicalCountResponse(UUID id, PhysicalCountScope scope, String productGroupId,
		UUID warehouseId, PhysicalCountStatus status, UUID startedBy, Instant startedAt, List<LineResponse> lines) {

	public static ApprovePhysicalCountResponse from(final PhysicalCount physicalCount) {
		return new ApprovePhysicalCountResponse(
				physicalCount.getId().value(),
				physicalCount.getScope(),
				physicalCount.getProductGroupId(),
				physicalCount.getWarehouseId(),
				physicalCount.getStatus(),
				physicalCount.getStartedBy(),
				physicalCount.getStartedAt(),
				physicalCount.getLines().stream().map(LineResponse::from).toList());
	}

	public record LineResponse(UUID productId, BigDecimal systemQuantity, BigDecimal countedQuantity,
			boolean adjustmentGenerated) {

		static LineResponse from(final PhysicalCountLine line) {
			return new LineResponse(line.productId(), line.systemQuantity(), line.countedQuantity(),
					line.hasDivergence());
		}
	}
}
