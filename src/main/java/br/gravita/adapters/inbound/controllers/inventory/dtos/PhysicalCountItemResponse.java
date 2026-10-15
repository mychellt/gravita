package br.gravita.adapters.inbound.controllers.inventory.dtos;

import br.gravita.core.domain.inventory.PhysicalCount;
import br.gravita.core.domain.inventory.PhysicalCountLine;
import br.gravita.core.domain.inventory.PhysicalCountScope;
import br.gravita.core.domain.inventory.PhysicalCountStatus;
import br.gravita.core.ports.inbound.inventory.PhysicalCountItemView;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PhysicalCountItemResponse(UUID id, PhysicalCountScope scope, String productGroupId, UUID warehouseId,
		PhysicalCountStatus status, UUID startedBy, Instant startedAt, List<Line> lines) {

	/** {@code countedQuantity} and {@code divergence} stay null until the line has been counted. */
	public record Line(UUID productId, String productCode, BigDecimal systemQuantity, BigDecimal countedQuantity,
			BigDecimal divergence) {

		static Line from(final PhysicalCountLine line, final Map<UUID, String> productCodes) {
			return new Line(line.productId(), productCodes.get(line.productId()), line.systemQuantity(),
					line.countedQuantity(), line.countedQuantity() == null ? null : line.divergence());
		}
	}

	public static PhysicalCountItemResponse from(final PhysicalCountItemView view) {
		final PhysicalCount count = view.count();
		return new PhysicalCountItemResponse(count.getId().value(), count.getScope(), count.getProductGroupId(),
				count.getWarehouseId(), count.getStatus(), count.getStartedBy(), count.getStartedAt(),
				count.getLines().stream().map(line -> Line.from(line, view.productCodes())).toList());
	}
}
