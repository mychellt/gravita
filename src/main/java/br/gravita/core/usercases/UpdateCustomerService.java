package br.gravita.core.usercases;

import br.gravita.core.annotations.UseCase;
import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.ports.inbound.masterdata.UpdateCustomerCommand;
import br.gravita.core.ports.inbound.masterdata.UpdateCustomerUseCase;
import br.gravita.core.ports.outbound.persistence.CustomerRepositoryPort;

@UseCase
public class UpdateCustomerService implements UpdateCustomerUseCase {

	private final CustomerRepositoryPort customerRepositoryPort;

	public UpdateCustomerService(final CustomerRepositoryPort customerRepositoryPort) {
		this.customerRepositoryPort = customerRepositoryPort;
	}

	@Override
	public void execute(final UpdateCustomerCommand command) {
		final CustomerDomain existing = customerRepositoryPort.get(command.customerId())
				.orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + command.customerId()));

		existing.setDocumentDomain(coalesce(command.document(), existing.getDocumentDomain()));
		existing.setName(coalesce(command.name(), existing.getName()));
		existing.setEmail(coalesce(command.email(), existing.getEmail()));
		existing.setIeIndicator(coalesce(command.ieIndicator(), existing.getIeIndicator()));
		existing.setFinalConsumer(coalesce(command.finalConsumer(), existing.getFinalConsumer()));
		existing.setCreditLimit(coalesce(command.creditLimit(), existing.getCreditLimit()));
		existing.setAddresses(coalesce(command.addresses(), existing.getAddresses()));
		existing.setContacts(coalesce(command.contacts(), existing.getContacts()));
		existing.setStatus(coalesce(command.status(), existing.getStatus()));
		existing.setCurrentBalance(coalesce(command.currentBalance(), existing.getCurrentBalance()));
		existing.setPriceTables(coalesce(command.priceTables(), existing.getPriceTables()));

		existing.validateForRegistration();

		customerRepositoryPort.save(existing);
	}

	private static <T> T coalesce(final T newValue, final T currentValue) {
		return newValue != null ? newValue : currentValue;
	}
}
