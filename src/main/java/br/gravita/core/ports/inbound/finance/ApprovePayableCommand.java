package br.gravita.core.ports.inbound.finance;

import java.util.Objects;
import java.util.UUID;

public record ApprovePayableCommand(UUID payableId, UUID approvedBy) {

	public ApprovePayableCommand {
		Objects.requireNonNull(payableId, "payableId is required");
		Objects.requireNonNull(approvedBy, "approvedBy is required");
	}
}
