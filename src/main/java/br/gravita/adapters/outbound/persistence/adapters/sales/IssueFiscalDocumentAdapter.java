package br.gravita.adapters.outbound.persistence.adapters.sales;

import br.gravita.core.domain.AddressDomain;
import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.IeIndicator;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.masterdata.FiscalDocumentType;
import br.gravita.core.domain.sales.FiscalDocumentRef;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.shared.PersonType;
import br.gravita.core.domain.tax.NaturezaOperacao;
import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.ports.inbound.tax.IssueNfeCommand;
import br.gravita.core.ports.inbound.tax.IssueNfeCommand.ItemCommand;
import br.gravita.core.ports.inbound.tax.IssueNfeCommand.RecipientCommand;
import br.gravita.core.ports.inbound.tax.IssueNfeUseCase;
import br.gravita.core.ports.outbound.persistence.CustomerRepositoryPort;
import br.gravita.core.ports.outbound.sales.IssueFiscalDocumentPort;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * The sales flow has no company-selection concept yet, so, like
 * {@code ReserveStockAdapter.DEFAULT_WAREHOUSE_ID}, a single issuer company
 * is assumed until multi-company invoicing is required.
 */
@Component
class IssueFiscalDocumentAdapter implements IssueFiscalDocumentPort {

	static final UUID DEFAULT_ISSUER_COMPANY_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

	private final IssueNfeUseCase issueNfeUseCase;
	private final CustomerRepositoryPort customerRepositoryPort;

	IssueFiscalDocumentAdapter(IssueNfeUseCase issueNfeUseCase, CustomerRepositoryPort customerRepositoryPort) {
		this.issueNfeUseCase = issueNfeUseCase;
		this.customerRepositoryPort = customerRepositoryPort;
	}

	@Override
	public FiscalDocumentRef issueForProducts(IssueFiscalDocumentCommand command) {
		RecipientCommand recipient = buildRecipient(command.customerId());
		List<ItemCommand> items = command.items().stream()
				.map(item -> new ItemCommand(item.productOrServiceId(), item.description(), item.quantity(),
						item.unitPrice(), item.discount()))
				.toList();

		IssueNfeCommand nfeCommand = new IssueNfeCommand(DEFAULT_ISSUER_COMPANY_ID, command.orderId(),
				NaturezaOperacao.VENDA, recipient, items, null, null, List.of(), BigDecimal.ZERO, BigDecimal.ZERO,
				BigDecimal.ZERO, null, null, null);

		NfeDocument issued = issueNfeUseCase.execute(nfeCommand);
		return new FiscalDocumentRef(FiscalDocumentType.NFE, issued.getId().value());
	}

	@Override
	public FiscalDocumentRef issueForServices(IssueFiscalDocumentCommand command) {
		throw new BusinessRuleException(
				"NFSe issuance is not yet available: M4 (Fiscal NFSe) has not been implemented");
	}

	private RecipientCommand buildRecipient(UUID customerId) {
		CustomerDomain customer = customerRepositoryPort.get(customerId)
				.orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));
		return new RecipientCommand(customerId, customer.getDocumentDomain().number(),
				customer.getDocumentDomain().personType(), customer.getName(), stateRegistrationOf(customer),
				stateOf(customer));
	}

	private String stateRegistrationOf(CustomerDomain customer) {
		if (customer.getDocumentDomain().personType() != PersonType.COMPANY) {
			return null;
		}
		return customer.getIeIndicator() == IeIndicator.EXEMPT ? "ISENTO" : null;
	}

	private String stateOf(CustomerDomain customer) {
		List<AddressDomain> addresses = customer.getAddresses() == null ? List.of() : customer.getAddresses();
		return addresses.stream().filter(AddressDomain::isDefault).findFirst().or(() -> addresses.stream().findFirst())
				.map(AddressDomain::getState)
				.orElseThrow(() -> new BusinessRuleException("Customer has no address to resolve the recipient state"));
	}
}
