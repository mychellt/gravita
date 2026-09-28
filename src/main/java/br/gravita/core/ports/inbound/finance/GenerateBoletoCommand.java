package br.gravita.core.ports.inbound.finance;

import br.gravita.core.domain.finance.BankIntegration;
import java.util.Objects;
import java.util.UUID;

public record GenerateBoletoCommand(UUID receivableId, BankIntegration bankIntegration) {

	public GenerateBoletoCommand {
		Objects.requireNonNull(receivableId, "receivableId is required");
		Objects.requireNonNull(bankIntegration, "bankIntegration is required");
	}
}
