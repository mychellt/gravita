package br.gravita.core.usercases.finance;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.finance.Payable;
import br.gravita.core.domain.finance.PayableId;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.finance.GeneratePayableFromReceiptCommand;
import br.gravita.core.ports.inbound.finance.GeneratePayableFromReceiptCommand.Installment;
import br.gravita.core.ports.inbound.finance.GeneratePayableFromReceiptUseCase;
import br.gravita.core.ports.outbound.persistence.finance.PayableRepositoryPort;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class GeneratePayableFromReceiptService implements GeneratePayableFromReceiptUseCase {

	private final PayableRepositoryPort payableRepositoryPort;

	public GeneratePayableFromReceiptService(PayableRepositoryPort payableRepositoryPort) {
		this.payableRepositoryPort = payableRepositoryPort;
	}

	/**
	 * Joins the caller's transaction, so that a failure rolls back the receipt
	 * confirmation together with the payables; all installments are created
	 * together or not at all.
	 */
	@Override
	@Transactional
	public List<Payable> execute(GeneratePayableFromReceiptCommand command) {
		if (command.installments().isEmpty()) {
			throw new BusinessRuleException("At least one installment is required");
		}

		List<Payable> existing = payableRepositoryPort.findByPurchaseReceiptRef(command.purchaseReceiptRef());
		if (!existing.isEmpty()) {
			return existing;
		}

		int total = command.installments().size();
		List<Payable> created = new ArrayList<>(total);
		for (int i = 0; i < total; i++) {
			Installment installment = command.installments().get(i);
			Payable payable = Payable.createFromPurchaseReceipt(PayableId.of(UUID.randomUUID()),
					command.supplierId(), command.purchaseReceiptRef(), installment.amount(),
					installment.dueDate(), i + 1, total);
			created.add(payableRepositoryPort.save(payable));
		}
		return created;
	}
}
