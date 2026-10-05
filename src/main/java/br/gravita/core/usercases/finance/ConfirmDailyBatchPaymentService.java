package br.gravita.core.usercases.finance;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.finance.BankIntegration;
import br.gravita.core.ports.inbound.finance.BankReturnImportResult;
import br.gravita.core.ports.inbound.finance.ConfirmBatchPaymentCommand;
import br.gravita.core.ports.inbound.finance.ConfirmBatchPaymentUseCase;
import br.gravita.core.ports.inbound.finance.ConfirmDailyBatchPaymentUseCase;
import br.gravita.core.ports.outbound.finance.BankIntegrationPort;
import java.util.Objects;
import java.util.Optional;

@UseCase
public class ConfirmDailyBatchPaymentService implements ConfirmDailyBatchPaymentUseCase {

	private final BankIntegrationPort bankIntegrationPort;
	private final ConfirmBatchPaymentUseCase confirmBatchPaymentUseCase;

	public ConfirmDailyBatchPaymentService(final BankIntegrationPort bankIntegrationPort,
			final ConfirmBatchPaymentUseCase confirmBatchPaymentUseCase) {
		this.bankIntegrationPort = bankIntegrationPort;
		this.confirmBatchPaymentUseCase = confirmBatchPaymentUseCase;
	}

	@Override
	public Optional<BankReturnImportResult> execute(final BankIntegration bankIntegration) {
		Objects.requireNonNull(bankIntegration, "bankIntegration is required");
		return bankIntegrationPort.fetchPaymentReturnFile(bankIntegration).filter(content -> !content.isBlank())
				.map(content -> confirmBatchPaymentUseCase
						.execute(new ConfirmBatchPaymentCommand(bankIntegration, content)));
	}
}
