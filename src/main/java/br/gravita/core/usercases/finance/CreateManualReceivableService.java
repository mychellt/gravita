package br.gravita.core.usercases.finance;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.ports.inbound.finance.CreateManualReceivableCommand;
import br.gravita.core.ports.inbound.finance.CreateManualReceivableUseCase;
import br.gravita.core.ports.outbound.persistence.CustomerRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.ReceivableRepositoryPort;
import java.util.UUID;

@UseCase
public class CreateManualReceivableService implements CreateManualReceivableUseCase {

	private final ReceivableRepositoryPort receivableRepositoryPort;
	private final CustomerRepositoryPort customerRepositoryPort;

	public CreateManualReceivableService(ReceivableRepositoryPort receivableRepositoryPort,
			CustomerRepositoryPort customerRepositoryPort) {
		this.receivableRepositoryPort = receivableRepositoryPort;
		this.customerRepositoryPort = customerRepositoryPort;
	}

	@Override
	public Receivable execute(CreateManualReceivableCommand command) {
		customerRepositoryPort.get(command.customerId())
				.orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + command.customerId()));

		ReceivableId id = ReceivableId.of(UUID.randomUUID());
		Receivable receivable = Receivable.createManual(id, command.customerId(), command.amount(),
				command.dueDate(), command.installments());

		return receivableRepositoryPort.save(receivable);
	}
}
