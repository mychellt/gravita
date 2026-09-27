package br.gravita.tax.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.shared.PersonRef;
import br.gravita.core.domain.shared.PersonType;
import br.gravita.core.domain.tax.Cfop;
import br.gravita.core.domain.tax.ItemTaxBreakdown;
import br.gravita.core.domain.tax.NaturezaOperacao;
import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.domain.tax.NfeDocumentId;
import br.gravita.core.domain.tax.NfeDocumentStatus;
import br.gravita.core.domain.tax.NfeItem;
import br.gravita.core.domain.tax.NfeRecipient;
import br.gravita.core.domain.tax.TaxCalculationTotals;
import br.gravita.core.domain.tax.TaxLineBreakdown;
import br.gravita.core.domain.tax.TaxType;
import br.gravita.core.ports.inbound.tax.ResendNfeEmailCommand;
import br.gravita.core.ports.messaging.FiscalDocumentEmailRequest;
import br.gravita.core.ports.messaging.SendFiscalDocumentByEmailPort;
import br.gravita.core.ports.outbound.persistence.CustomerRepositoryPort;
import br.gravita.core.ports.outbound.persistence.XmlObjectStoragePort;
import br.gravita.core.ports.outbound.persistence.tax.NfeRepositoryPort;
import br.gravita.core.usercases.tax.ResendNfeEmailService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResendNfeEmailServiceTest {

	private static final String VALID_CNPJ = "11.222.333/0001-81";

	@Mock
	private NfeRepositoryPort nfeRepositoryPort;

	@Mock
	private CustomerRepositoryPort customerRepositoryPort;

	@Mock
	private XmlObjectStoragePort xmlObjectStoragePort;

	@Mock
	private SendFiscalDocumentByEmailPort sendFiscalDocumentByEmailPort;

	private ResendNfeEmailService service;

	private NfeDocumentId documentId;
	private PersonRef recipientRef;

	@BeforeEach
	void setUp() {
		service = new ResendNfeEmailService(nfeRepositoryPort, customerRepositoryPort, xmlObjectStoragePort,
				sendFiscalDocumentByEmailPort);
		documentId = NfeDocumentId.of(UUID.randomUUID());
		recipientRef = PersonRef.of(UUID.randomUUID());
	}

	@Test
	void ac5_resendsTheAlreadyStoredXmlAndDanfeToTheRecipientRatherThanRegeneratingThem() {
		NfeDocument document = authorizedDocument();
		when(nfeRepositoryPort.findById(documentId)).thenReturn(Optional.of(document));
		when(customerRepositoryPort.get(recipientRef.id()))
				.thenReturn(Optional.of(CustomerDomain.builder().email("cliente@example.com").build()));
		when(xmlObjectStoragePort.retrieve("xml-ref")).thenReturn("xml-bytes".getBytes());
		when(xmlObjectStoragePort.retrieve("danfe-ref")).thenReturn("danfe-bytes".getBytes());

		service.execute(new ResendNfeEmailCommand(documentId.value()));

		ArgumentCaptor<FiscalDocumentEmailRequest> captor = ArgumentCaptor.forClass(FiscalDocumentEmailRequest.class);
		verify(sendFiscalDocumentByEmailPort).send(captor.capture());
		assertThat(captor.getValue().to()).isEqualTo("cliente@example.com");
		assertThat(captor.getValue().xmlContent()).isEqualTo("xml-bytes".getBytes());
		assertThat(captor.getValue().danfeContent()).isEqualTo("danfe-bytes".getBytes());
	}

	@ParameterizedTest
	@EnumSource(value = NfeDocumentStatus.class, names = {"DRAFT", "QUEUED", "SENT", "REJECTED", "CANCELLED", "VOIDED"})
	void onlyAnAuthorizedDocumentCanHaveItsEmailResent(NfeDocumentStatus status) {
		NfeDocument document = documentWithStatus(status);
		when(nfeRepositoryPort.findById(documentId)).thenReturn(Optional.of(document));

		assertThatThrownBy(() -> service.execute(new ResendNfeEmailCommand(documentId.value())))
				.isInstanceOf(BusinessRuleException.class);

		verify(sendFiscalDocumentByEmailPort, never()).send(any());
	}

	@Test
	void aRecipientWithNoEmailOnFileIsRejectedRatherThanSilentlySkipped() {
		NfeDocument document = authorizedDocument();
		when(nfeRepositoryPort.findById(documentId)).thenReturn(Optional.of(document));
		when(customerRepositoryPort.get(recipientRef.id())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new ResendNfeEmailCommand(documentId.value())))
				.isInstanceOf(BusinessRuleException.class);

		verify(sendFiscalDocumentByEmailPort, never()).send(any());
	}

	@Test
	void anNfeDocumentThatDoesNotExistIsRejected() {
		when(nfeRepositoryPort.findById(documentId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new ResendNfeEmailCommand(documentId.value())))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	private NfeDocument authorizedDocument() {
		return documentWithStatus(NfeDocumentStatus.AUTHORIZED);
	}

	private NfeDocument documentWithStatus(NfeDocumentStatus status) {
		UUID productId = UUID.randomUUID();
		TaxLineBreakdown line = new TaxLineBreakdown(TaxType.ICMS, new BigDecimal("100.00"), new BigDecimal("18"),
				new BigDecimal("18.00"), new BigDecimal("18.00"), false, null);
		ItemTaxBreakdown breakdown = new ItemTaxBreakdown(0, productId.toString(), List.of(line));
		NfeItem item = new NfeItem(productId, "Produto Teste", BigDecimal.ONE, new BigDecimal("100.00"),
				BigDecimal.ZERO, breakdown);
		NfeRecipient recipient = NfeRecipient.of(recipientRef, VALID_CNPJ, PersonType.COMPANY, "Cliente PJ Teste",
				"123456789", "RJ");
		TaxCalculationTotals totals = TaxCalculationTotals.from(List.of(item.taxBreakdown()));

		String xmlRef = status == NfeDocumentStatus.AUTHORIZED ? "xml-ref" : null;
		String danfeRef = status == NfeDocumentStatus.AUTHORIZED ? "danfe-ref" : null;
		String sefazProtocol = status == NfeDocumentStatus.DRAFT || status == NfeDocumentStatus.QUEUED ? null
				: "PROTOCOL-1";

		return NfeDocument.of(documentId, CompanyId.of(UUID.randomUUID()), null, NaturezaOperacao.VENDA,
				new Cfop("5102"), recipient, List.of(item), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null,
				null, null, totals, status, Instant.now(), "001", 42L, "3".repeat(44), sefazProtocol, false, null,
				xmlRef, danfeRef, List.of());
	}
}
