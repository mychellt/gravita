package br.gravita.core.usercases.finance;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.finance.Boleto;
import br.gravita.core.domain.finance.BoletoId;
import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.ports.inbound.finance.GenerateBoletoCommand;
import br.gravita.core.ports.inbound.finance.GenerateBoletoUseCase;
import br.gravita.core.ports.messaging.EmailNotificationPort;
import br.gravita.core.ports.outbound.finance.BankIntegrationPort;
import br.gravita.core.ports.outbound.finance.BankIntegrationPort.BoletoIssueRequest;
import br.gravita.core.ports.outbound.finance.BankIntegrationPort.IssuedBoleto;
import br.gravita.core.ports.outbound.persistence.CustomerRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.BoletoRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.ReceivableRepositoryPort;
import java.util.UUID;

@UseCase
public class GenerateBoletoService implements GenerateBoletoUseCase {

	private final ReceivableRepositoryPort receivableRepositoryPort;
	private final BoletoRepositoryPort boletoRepositoryPort;
	private final BankIntegrationPort bankIntegrationPort;
	private final CustomerRepositoryPort customerRepositoryPort;
	private final EmailNotificationPort emailNotificationPort;

	public GenerateBoletoService(ReceivableRepositoryPort receivableRepositoryPort,
			BoletoRepositoryPort boletoRepositoryPort, BankIntegrationPort bankIntegrationPort,
			CustomerRepositoryPort customerRepositoryPort, EmailNotificationPort emailNotificationPort) {
		this.receivableRepositoryPort = receivableRepositoryPort;
		this.boletoRepositoryPort = boletoRepositoryPort;
		this.bankIntegrationPort = bankIntegrationPort;
		this.customerRepositoryPort = customerRepositoryPort;
		this.emailNotificationPort = emailNotificationPort;
	}

	@Override
	public Boleto execute(GenerateBoletoCommand command) {
		Receivable receivable = receivableRepositoryPort.findById(ReceivableId.of(command.receivableId()))
				.orElseThrow(() -> new ResourceNotFoundException("Receivable not found: " + command.receivableId()));
		// Checked before the bank is contacted so a rejected request never issues a boleto there.
		receivable.requireOpen();

		IssuedBoleto issued = bankIntegrationPort.issueBoleto(new BoletoIssueRequest(command.bankIntegration(),
				receivable.getId().value(), receivable.getCustomerId(), receivable.getAmount(),
				receivable.getDueDate()));

		Boleto boleto = boletoRepositoryPort.save(Boleto.issue(BoletoId.of(UUID.randomUUID()), receivable.getId(),
				command.bankIntegration(), issued.barcodeLine()));

		sendByEmail(receivable, boleto);
		return boleto;
	}

	/**
	 * Automatic delivery on generation. A customer with no e-mail on file still
	 * leaves the boleto generated (it is already issued at the bank); the
	 * receivable's boleto can be delivered once an address is available.
	 */
	private void sendByEmail(Receivable receivable, Boleto boleto) {
		customerRepositoryPort.get(receivable.getCustomerId()).map(CustomerDomain::getEmail)
				.filter(email -> !email.isBlank()).ifPresent(email -> emailNotificationPort.send(email,
						"Boleto - vencimento " + receivable.getDueDate(), buildBody(receivable, boleto)));
	}

	private static String buildBody(Receivable receivable, Boleto boleto) {
		return "Seu boleto no valor de R$ " + receivable.getAmount().toPlainString() + " com vencimento em "
				+ receivable.getDueDate() + " foi gerado.\n\nLinha digitável: " + boleto.getBarcodeLine();
	}
}
