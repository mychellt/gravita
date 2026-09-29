package br.gravita.core.ports.inbound.finance;

import java.util.Objects;
import java.util.UUID;

/** {@code bankAccount} is the account the statement belongs to; {@code fileContent} is the OFX/CSV payload, as received. */
public record ReconcileBankStatementCommand(UUID bankAccount, String fileContent) {

	public ReconcileBankStatementCommand {
		Objects.requireNonNull(bankAccount, "bankAccount is required");
		if (fileContent == null || fileContent.isBlank()) {
			throw new IllegalArgumentException("fileContent is required");
		}
	}
}
