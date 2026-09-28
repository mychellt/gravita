package br.gravita.core.ports.inbound.finance;

import br.gravita.core.domain.finance.BankIntegration;
import java.util.Objects;

/** {@code fileContent} is the bank's CNAB 240/400 return payload, as received. */
public record ImportBankReturnCommand(BankIntegration bankIntegration, String fileContent) {

	public ImportBankReturnCommand {
		Objects.requireNonNull(bankIntegration, "bankIntegration is required");
		if (fileContent == null || fileContent.isBlank()) {
			throw new IllegalArgumentException("fileContent is required");
		}
	}
}
