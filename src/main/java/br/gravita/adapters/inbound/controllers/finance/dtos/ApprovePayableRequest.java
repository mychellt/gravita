package br.gravita.adapters.inbound.controllers.finance.dtos;

import br.gravita.core.ports.inbound.finance.ApprovePayableCommand;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ApprovePayableRequest(@NotNull UUID approvedBy) {

	public ApprovePayableCommand toCommand(final UUID payableId) {
		return new ApprovePayableCommand(payableId, approvedBy);
	}
}
