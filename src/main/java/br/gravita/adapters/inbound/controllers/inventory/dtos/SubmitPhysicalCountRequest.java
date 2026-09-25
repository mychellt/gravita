package br.gravita.adapters.inbound.controllers.inventory.dtos;

import br.gravita.core.domain.inventory.PhysicalCountId;
import br.gravita.core.ports.inbound.inventory.SubmitPhysicalCountCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public record SubmitPhysicalCountRequest(@NotEmpty List<@Valid CountedLine> countedLines,
		@NotNull UUID submittedBy) {

	public record CountedLine(@NotNull UUID productId, @NotNull @PositiveOrZero BigDecimal countedQuantity) {
	}

	public SubmitPhysicalCountCommand toCommand(UUID physicalCountId) {
		Map<UUID, BigDecimal> countedQuantities = countedLines.stream()
				.collect(Collectors.toMap(CountedLine::productId, CountedLine::countedQuantity));
		return new SubmitPhysicalCountCommand(PhysicalCountId.of(physicalCountId), countedQuantities, submittedBy);
	}
}
