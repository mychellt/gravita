package br.gravita.adapters.inbound.controllers.finance.dtos;

import br.gravita.core.domain.finance.BankIntegration;
import br.gravita.core.ports.inbound.finance.GenerateBoletoCommand;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record GenerateBoletoRequest(@NotNull BankIntegration bankIntegration) {

	public GenerateBoletoCommand toCommand(final UUID receivableId) {
		return new GenerateBoletoCommand(receivableId, bankIntegration);
	}
}
