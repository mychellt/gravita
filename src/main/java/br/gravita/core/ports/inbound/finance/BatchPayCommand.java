package br.gravita.core.ports.inbound.finance;

import br.gravita.core.domain.finance.BankIntegration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record BatchPayCommand(List<UUID> payableIds, BankIntegration bankIntegration) {

	public BatchPayCommand {
		Objects.requireNonNull(payableIds, "payableIds is required");
		payableIds = List.copyOf(payableIds);
		Objects.requireNonNull(bankIntegration, "bankIntegration is required");
	}
}
