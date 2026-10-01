package br.gravita.adapters.outbound.persistence.adapters.sales;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.AddressDomain;
import br.gravita.core.domain.AddressType;
import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.CustomerStatus;
import br.gravita.core.domain.masterdata.FiscalDocumentType;
import br.gravita.core.domain.sales.FiscalDocumentRef;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.tax.NaturezaOperacao;
import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.domain.tax.NfeDocumentId;
import br.gravita.core.ports.inbound.tax.IssueNfeCommand;
import br.gravita.core.ports.inbound.tax.IssueNfeUseCase;
import br.gravita.core.ports.outbound.persistence.CustomerRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfeRepositoryPort;
import br.gravita.core.ports.outbound.sales.IssueFiscalDocumentPort.IssueFiscalDocumentCommand;
import br.gravita.core.ports.outbound.sales.IssueFiscalDocumentPort.IssueFiscalDocumentCommand.Item;
import br.gravita.core.ports.outbound.sales.IssueFiscalDocumentPort.IssueReturnFiscalDocumentCommand;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IssueFiscalDocumentAdapterTest {

	@Mock
	private IssueNfeUseCase issueNfeUseCase;

	@Mock
	private CustomerRepositoryPort customerRepositoryPort;

	@Mock
	private NfeRepositoryPort nfeRepositoryPort;

	private static IssueFiscalDocumentAdapter newAdapter(IssueNfeUseCase useCase,
			CustomerRepositoryPort customerRepositoryPort, NfeRepositoryPort nfeRepositoryPort) {
		return new IssueFiscalDocumentAdapter(useCase, customerRepositoryPort, nfeRepositoryPort);
	}

	@Test
	@DisplayName("Issues an NF-e for product items using the default issuer company and the customer's address")
	void issuesAnNfeForProductItemsUsingTheDefaultIssuerCompanyAndTheCustomersAddress() {
		IssueFiscalDocumentAdapter adapter = newAdapter(issueNfeUseCase, customerRepositoryPort, nfeRepositoryPort);
		UUID customerId = UUID.randomUUID();
		when(customerRepositoryPort.get(customerId)).thenReturn(Optional.of(individualCustomer(customerId)));
		NfeDocumentId nfeId = NfeDocumentId.of(UUID.randomUUID());
		NfeDocument issuedDocument = nfeDocumentWithId(nfeId);
		when(issueNfeUseCase.execute(any())).thenReturn(issuedDocument);

		UUID orderId = UUID.randomUUID();
		UUID productId = UUID.randomUUID();
		IssueFiscalDocumentCommand command = new IssueFiscalDocumentCommand(orderId, customerId,
				List.of(new Item(productId, "SKU-1", BigDecimal.ONE, new BigDecimal("10.00"), BigDecimal.ZERO)));

		FiscalDocumentRef ref = adapter.issueForProducts(command);

		assertThat(ref.type()).isEqualTo(FiscalDocumentType.NFE);
		assertThat(ref.documentId()).isEqualTo(nfeId.value());

		ArgumentCaptor<IssueNfeCommand> captor = ArgumentCaptor.forClass(IssueNfeCommand.class);
		verify(issueNfeUseCase).execute(captor.capture());
		IssueNfeCommand nfeCommand = captor.getValue();
		assertThat(nfeCommand.issuerCompanyId()).isEqualTo(IssueFiscalDocumentAdapter.DEFAULT_ISSUER_COMPANY_ID);
		assertThat(nfeCommand.originSalesOrderId()).isEqualTo(orderId);
		assertThat(nfeCommand.naturezaOperacao()).isEqualTo(NaturezaOperacao.VENDA);
		assertThat(nfeCommand.recipient().personId()).isEqualTo(customerId);
		assertThat(nfeCommand.recipient().state()).isEqualTo("SP");
		assertThat(nfeCommand.items()).hasSize(1);
		assertThat(nfeCommand.items().get(0).productId()).isEqualTo(productId);
	}

	@Test
	@DisplayName("Reports that NFS-e issuance is not yet available")
	void nfseIssuanceIsNotYetAvailable() {
		IssueFiscalDocumentAdapter adapter = newAdapter(issueNfeUseCase, customerRepositoryPort, nfeRepositoryPort);
		IssueFiscalDocumentCommand command = new IssueFiscalDocumentCommand(UUID.randomUUID(), UUID.randomUUID(),
				List.of(new Item(UUID.randomUUID(), "Service", BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO)));

		assertThatThrownBy(() -> adapter.issueForServices(command)).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("NFSe issuance is not yet available");
	}

	@Test
	@DisplayName("Issues a return NF-e referencing the original document's access key")
	void issuesAReturnNfeReferencingTheOriginalDocumentsAccessKey() {
		IssueFiscalDocumentAdapter adapter = newAdapter(issueNfeUseCase, customerRepositoryPort, nfeRepositoryPort);
		UUID customerId = UUID.randomUUID();
		when(customerRepositoryPort.get(customerId)).thenReturn(Optional.of(individualCustomer(customerId)));

		UUID originalDocumentId = UUID.randomUUID();
		NfeDocument originalDocument = mock(NfeDocument.class);
		when(originalDocument.getAccessKey()).thenReturn("35250000000000000000000000000000000000000000");
		when(nfeRepositoryPort.findById(NfeDocumentId.of(originalDocumentId)))
				.thenReturn(Optional.of(originalDocument));

		NfeDocumentId returnNfeId = NfeDocumentId.of(UUID.randomUUID());
		NfeDocument returnDocument = nfeDocumentWithId(returnNfeId);
		when(issueNfeUseCase.execute(any())).thenReturn(returnDocument);

		UUID orderId = UUID.randomUUID();
		UUID productId = UUID.randomUUID();
		IssueReturnFiscalDocumentCommand command = new IssueReturnFiscalDocumentCommand(orderId, customerId,
				new FiscalDocumentRef(FiscalDocumentType.NFE, originalDocumentId),
				List.of(new Item(productId, "SKU-1", BigDecimal.ONE, new BigDecimal("10.00"), BigDecimal.ZERO)));

		FiscalDocumentRef ref = adapter.issueForReturn(command);

		assertThat(ref.type()).isEqualTo(FiscalDocumentType.NFE);
		assertThat(ref.documentId()).isEqualTo(returnNfeId.value());

		ArgumentCaptor<IssueNfeCommand> captor = ArgumentCaptor.forClass(IssueNfeCommand.class);
		verify(issueNfeUseCase).execute(captor.capture());
		IssueNfeCommand nfeCommand = captor.getValue();
		assertThat(nfeCommand.naturezaOperacao()).isEqualTo(NaturezaOperacao.DEVOLUCAO);
		assertThat(nfeCommand.referencedAccessKey()).isEqualTo("35250000000000000000000000000000000000000000");
		assertThat(nfeCommand.originSalesOrderId()).isEqualTo(orderId);
	}

	private static CustomerDomain individualCustomer(UUID customerId) {
		return CustomerDomain.builder().id(customerId).name("Cliente Teste")
				.documentDomain(Document.cpf("111.444.777-35")).status(CustomerStatus.REGULAR)
				.addresses(List.of(AddressDomain.builder().type(AddressType.BILLING).state("SP").isDefault(true).build()))
				.build();
	}

	private static NfeDocument nfeDocumentWithId(NfeDocumentId id) {
		NfeDocument document = mock(NfeDocument.class);
		when(document.getId()).thenReturn(id);
		return document;
	}
}
