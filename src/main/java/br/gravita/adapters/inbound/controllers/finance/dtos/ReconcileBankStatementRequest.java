package br.gravita.adapters.inbound.controllers.finance.dtos;

import br.gravita.core.ports.inbound.finance.ReconcileBankStatementCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** {@code fileContent} is the OFX or CSV statement, as exported by the bank. */
public record ReconcileBankStatementRequest(@NotNull UUID bankAccount, @NotBlank String fileContent) {

	public ReconcileBankStatementCommand toCommand() {
		return new ReconcileBankStatementCommand(bankAccount, fileContent);
	}
}
