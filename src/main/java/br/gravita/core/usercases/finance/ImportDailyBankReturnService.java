package br.gravita.core.usercases.finance;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.finance.BankIntegration;
import br.gravita.core.ports.inbound.finance.BankReturnImportResult;
import br.gravita.core.ports.inbound.finance.ImportBankReturnCommand;
import br.gravita.core.ports.inbound.finance.ImportBankReturnUseCase;
import br.gravita.core.ports.inbound.finance.ImportDailyBankReturnUseCase;
import br.gravita.core.ports.outbound.finance.BankIntegrationPort;
import java.util.Objects;
import java.util.Optional;

@UseCase
public class ImportDailyBankReturnService implements ImportDailyBankReturnUseCase {

	private final BankIntegrationPort bankIntegrationPort;
	private final ImportBankReturnUseCase importBankReturnUseCase;

	public ImportDailyBankReturnService(BankIntegrationPort bankIntegrationPort,
			ImportBankReturnUseCase importBankReturnUseCase) {
		this.bankIntegrationPort = bankIntegrationPort;
		this.importBankReturnUseCase = importBankReturnUseCase;
	}

	@Override
	public Optional<BankReturnImportResult> execute(BankIntegration bankIntegration) {
		Objects.requireNonNull(bankIntegration, "bankIntegration is required");
		return bankIntegrationPort.fetchReturnFile(bankIntegration).filter(content -> !content.isBlank())
				.map(content -> importBankReturnUseCase.execute(new ImportBankReturnCommand(bankIntegration, content)));
	}
}
