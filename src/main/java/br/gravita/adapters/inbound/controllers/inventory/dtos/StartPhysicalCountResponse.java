package br.gravita.adapters.inbound.controllers.inventory.dtos;

import br.gravita.core.domain.inventory.PhysicalCount;
import br.gravita.core.domain.inventory.PhysicalCountLine;
import br.gravita.core.domain.inventory.PhysicalCountScope;
import br.gravita.core.domain.inventory.PhysicalCountStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StartPhysicalCountResponse(UUID id, PhysicalCountScope scope, String productGroupId,
		UUID warehouseId, PhysicalCountStatus status, UUID startedBy, Instant startedAt, List<LineResponse> lines) {

	public static StartPhysicalCountResponse from(final PhysicalCount physicalCount) {
		return new StartPhysicalCountResponse(
				physicalCount.getId().value(),
				physicalCount.getScope(),
				physicalCount.getProductGroupId(),
				physicalCount.getWarehouseId(),
				physicalCount.getStatus(),
				physicalCount.getStartedBy(),
				physicalCount.getStartedAt(),
				physicalCount.getLines().stream().map(LineResponse::from).toList());
	}

	public record LineResponse(UUID productId, BigDecimal systemQuantity) {

		static LineResponse from(final PhysicalCountLine line) {
			return new LineResponse(line.productId(), line.systemQuantity());
		}
	}
}
