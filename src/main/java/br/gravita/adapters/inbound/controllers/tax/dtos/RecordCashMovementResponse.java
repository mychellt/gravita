package br.gravita.adapters.inbound.controllers.tax.dtos;

import br.gravita.core.domain.tax.CashMovementId;
import java.util.UUID;

public record RecordCashMovementResponse(UUID id) {

	public static RecordCashMovementResponse from(CashMovementId id) {
		return new RecordCashMovementResponse(id.value());
	}
}
