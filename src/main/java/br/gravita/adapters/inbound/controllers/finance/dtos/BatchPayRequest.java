package br.gravita.adapters.inbound.controllers.finance.dtos;

import br.gravita.core.domain.finance.BankIntegration;
import br.gravita.core.ports.inbound.finance.BatchPayCommand;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record BatchPayRequest(@NotEmpty List<@NotNull UUID> payableIds, @NotNull BankIntegration bankIntegration) {

	public BatchPayCommand toCommand() {
		return new BatchPayCommand(payableIds, bankIntegration);
	}
}
