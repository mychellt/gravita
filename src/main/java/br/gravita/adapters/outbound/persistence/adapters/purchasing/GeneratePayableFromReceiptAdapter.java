package br.gravita.adapters.outbound.persistence.adapters.purchasing;

import br.gravita.core.ports.inbound.finance.GeneratePayableFromReceiptCommand.Installment;
import br.gravita.core.ports.inbound.finance.GeneratePayableFromReceiptUseCase;
import br.gravita.core.ports.outbound.persistence.purchasing.GeneratePayableFromReceiptPort;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Bridges M6's receipt confirmation to {@code finance}'s
 * {@link GeneratePayableFromReceiptUseCase} (UC-M8-11). Failures propagate:
 * {@code ConfirmPurchaseReceiptService} generates the payables before saving the
 * confirmed receipt, so a failure here leaves the receipt unconfirmed and
 * confirming it again is safe, because the use case is idempotent per receipt.
 */
@Component
class GeneratePayableFromReceiptAdapter implements GeneratePayableFromReceiptPort {

	private final GeneratePayableFromReceiptUseCase generatePayableFromReceiptUseCase;

	GeneratePayableFromReceiptAdapter(final GeneratePayableFromReceiptUseCase generatePayableFromReceiptUseCase) {
		this.generatePayableFromReceiptUseCase = generatePayableFromReceiptUseCase;
	}

	@Override
	public void generatePayables(final GeneratePayableFromReceiptCommand command) {
		final List<Installment> installments = command.installments().stream()
				.map(installment -> new Installment(installment.dueDate(), installment.amount())).toList();
		generatePayableFromReceiptUseCase.execute(
				new br.gravita.core.ports.inbound.finance.GeneratePayableFromReceiptCommand(command.supplierId(),
						command.sourcePurchaseReceiptId(), installments));
	}
}
