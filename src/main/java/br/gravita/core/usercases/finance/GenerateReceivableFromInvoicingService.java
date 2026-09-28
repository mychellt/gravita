package br.gravita.core.usercases.finance;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.finance.GenerateReceivableFromInvoicingCommand;
import br.gravita.core.ports.inbound.finance.GenerateReceivableFromInvoicingCommand.Installment;
import br.gravita.core.ports.inbound.finance.GenerateReceivableFromInvoicingUseCase;
import br.gravita.core.ports.outbound.persistence.finance.ReceivableRepositoryPort;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@UseCase
public class GenerateReceivableFromInvoicingService implements GenerateReceivableFromInvoicingUseCase {

	private final ReceivableRepositoryPort receivableRepositoryPort;

	public GenerateReceivableFromInvoicingService(ReceivableRepositoryPort receivableRepositoryPort) {
		this.receivableRepositoryPort = receivableRepositoryPort;
	}

	/**
	 * Runs in its own transaction so that a failure here rolls back only the
	 * receivables, never the already-authorized fiscal document / invoice of the
	 * caller's transaction; all installments are created together or not at all.
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public List<Receivable> execute(GenerateReceivableFromInvoicingCommand command) {
		if (command.installments().isEmpty()) {
			throw new BusinessRuleException("At least one installment is required");
		}

		List<Receivable> existing = receivableRepositoryPort.findByOriginDocumentRef(command.originDocumentRef());
		if (!existing.isEmpty()) {
			return existing;
		}

		int total = command.installments().size();
		List<Receivable> created = new ArrayList<>(total);
		for (int i = 0; i < total; i++) {
			Installment installment = command.installments().get(i);
			Receivable receivable = Receivable.createFromInvoicing(ReceivableId.of(UUID.randomUUID()),
					command.customerId(), command.originDocumentRef(), installment.amount(), installment.dueDate(),
					i + 1, total);
			created.add(receivableRepositoryPort.save(receivable));
		}
		return created;
	}
}
