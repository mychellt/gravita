package br.gravita.core.ports.inbound.tax;

import java.util.Objects;
import java.util.UUID;

/**
 * {@code justification} is intentionally not null-checked here: UC-M2-06's
 * AC1 requires it to fail as a {@code BusinessRuleException} (mandatory but
 * blank vs. missing are the same rejection), so the service validates it the
 * same way {@code RecordCashMovementService} validates a cash movement's own
 * mandatory justification.
 */
public record VoidNumberRangeCommand(UUID companyId, String series, Long startNumber, Long endNumber,
		String justification) {

	public VoidNumberRangeCommand {
		Objects.requireNonNull(companyId, "companyId is required");
		Objects.requireNonNull(series, "series is required");
		Objects.requireNonNull(startNumber, "startNumber is required");
		Objects.requireNonNull(endNumber, "endNumber is required");
	}
}
