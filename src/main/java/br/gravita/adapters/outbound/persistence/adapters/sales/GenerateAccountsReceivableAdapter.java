package br.gravita.adapters.outbound.persistence.adapters.sales;

import br.gravita.core.ports.inbound.finance.GenerateReceivableFromInvoicingCommand;
import br.gravita.core.ports.inbound.finance.GenerateReceivableFromInvoicingCommand.Installment;
import br.gravita.core.ports.inbound.finance.GenerateReceivableFromInvoicingUseCase;
import br.gravita.core.ports.outbound.sales.GenerateAccountsReceivablePort;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Bridges M7's invoicing to {@code finance}'s
 * {@link GenerateReceivableFromInvoicingUseCase} (UC-M8-01). The fiscal
 * document is already authorized by the time this runs, so a failure to create
 * the receivable must never propagate and block issuance: it is logged at ERROR
 * (the hook for M10 alerting) and can be retried safely, because the use case is
 * idempotent per fiscal document.
 * <p>
 * M7 currently issues a single-installment payment term (total, due today); the
 * M4 NFSe-authorization flow will call the use case directly once it exists.
 */
@Component
class GenerateAccountsReceivableAdapter implements GenerateAccountsReceivablePort {

	private static final Logger log = LoggerFactory.getLogger(GenerateAccountsReceivableAdapter.class);

	private final GenerateReceivableFromInvoicingUseCase generateReceivableFromInvoicingUseCase;

	GenerateAccountsReceivableAdapter(GenerateReceivableFromInvoicingUseCase generateReceivableFromInvoicingUseCase) {
		this.generateReceivableFromInvoicingUseCase = generateReceivableFromInvoicingUseCase;
	}

	@Override
	public void generate(GenerateAccountsReceivableCommand command) {
		try {
			generateReceivableFromInvoicingUseCase.execute(new GenerateReceivableFromInvoicingCommand(
					command.customerId(), command.originDocument().documentId(),
					List.of(new Installment(command.dueDate(), command.amount()))));
		} catch (RuntimeException e) {
			log.error("Failed to generate receivable for {} {} (customer {}); needs retry",
					command.originDocument().type(), command.originDocument().documentId(), command.customerId(), e);
		}
	}
}
