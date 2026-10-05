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
import br.gravita.core.domain.tax.NfeDocumentId;
import br.gravita.core.ports.inbound.tax.IssueNfeCommand;
import br.gravita.core.ports.inbound.tax.IssueNfeCommand.ItemCommand;
import br.gravita.core.ports.inbound.tax.IssueNfeCommand.RecipientCommand;
import br.gravita.core.ports.inbound.tax.IssueNfeUseCase;
import br.gravita.core.ports.outbound.persistence.CustomerRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfeRepositoryPort;
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
	private final NfeRepositoryPort nfeRepositoryPort;

	IssueFiscalDocumentAdapter(final IssueNfeUseCase issueNfeUseCase, final CustomerRepositoryPort customerRepositoryPort,
			final NfeRepositoryPort nfeRepositoryPort) {
		this.issueNfeUseCase = issueNfeUseCase;
		this.customerRepositoryPort = customerRepositoryPort;
		this.nfeRepositoryPort = nfeRepositoryPort;
	}

	@Override
	public FiscalDocumentRef issueForProducts(final IssueFiscalDocumentCommand command) {
		final RecipientCommand recipient = buildRecipient(command.customerId());
		final List<ItemCommand> items = command.items().stream()
				.map(item -> new ItemCommand(item.productOrServiceId(), item.description(), item.quantity(),
						item.unitPrice(), item.discount()))
				.toList();

		final IssueNfeCommand nfeCommand = new IssueNfeCommand(DEFAULT_ISSUER_COMPANY_ID, command.orderId(),
				NaturezaOperacao.VENDA, recipient, items, null, null, List.of(), BigDecimal.ZERO, BigDecimal.ZERO,
				BigDecimal.ZERO, null, null, null);

		final NfeDocument issued = issueNfeUseCase.execute(nfeCommand);
		return new FiscalDocumentRef(FiscalDocumentType.NFE, issued.getId().value());
	}

	@Override
	public FiscalDocumentRef issueForServices(final IssueFiscalDocumentCommand command) {
		throw new BusinessRuleException(
				"NFSe issuance is not yet available: M4 (Fiscal NFSe) has not been implemented");
	}

	@Override
	public FiscalDocumentRef issueForReturn(final IssueReturnFiscalDocumentCommand command) {
		if (command.originalDocument().type() != FiscalDocumentType.NFE) {
			throw new BusinessRuleException("Return NFe issuance is only supported for orders invoiced through NFe");
		}
		final NfeDocument original = nfeRepositoryPort.findById(NfeDocumentId.of(command.originalDocument().documentId()))
				.orElseThrow(() -> new ResourceNotFoundException(
						"Original fiscal document not found: " + command.originalDocument().documentId()));

		final RecipientCommand recipient = buildRecipient(command.customerId());
		final List<ItemCommand> items = command.items().stream()
				.map(item -> new ItemCommand(item.productOrServiceId(), item.description(), item.quantity(),
						item.unitPrice(), item.discount()))
				.toList();

		final IssueNfeCommand nfeCommand = new IssueNfeCommand(DEFAULT_ISSUER_COMPANY_ID, command.orderId(),
				NaturezaOperacao.DEVOLUCAO, recipient, items, null, null, List.of(), BigDecimal.ZERO, BigDecimal.ZERO,
				BigDecimal.ZERO, null, original.getAccessKey(), null);

		final NfeDocument issued = issueNfeUseCase.execute(nfeCommand);
		return new FiscalDocumentRef(FiscalDocumentType.NFE, issued.getId().value());
	}

	private RecipientCommand buildRecipient(final UUID customerId) {
		final CustomerDomain customer = customerRepositoryPort.get(customerId)
				.orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));
		return new RecipientCommand(customerId, customer.getDocumentDomain().number(),
				customer.getDocumentDomain().personType(), customer.getName(), stateRegistrationOf(customer),
				stateOf(customer));
	}

	private String stateRegistrationOf(final CustomerDomain customer) {
		if (customer.getDocumentDomain().personType() != PersonType.COMPANY) {
			return null;
		}
		return customer.getIeIndicator() == IeIndicator.EXEMPT ? "ISENTO" : null;
	}

	private String stateOf(final CustomerDomain customer) {
		final List<AddressDomain> addresses = customer.getAddresses() == null ? List.of() : customer.getAddresses();
		return addresses.stream().filter(AddressDomain::isDefault).findFirst().or(() -> addresses.stream().findFirst())
				.map(AddressDomain::getState)
				.orElseThrow(() -> new BusinessRuleException("Customer has no address to resolve the recipient state"));
	}
}
