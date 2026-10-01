package br.gravita.tax.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.domain.masterdata.TaxRegime;
import br.gravita.core.domain.shared.Document;
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
import br.gravita.core.ports.inbound.tax.CancelNfeCommand;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfeRepositoryPort;
import br.gravita.core.ports.outbound.tax.SefazCancellationRequest;
import br.gravita.core.ports.outbound.tax.SefazSubmissionResult;
import br.gravita.core.ports.outbound.tax.SubmitToSefazPort;
import br.gravita.core.usercases.tax.CancelNfeService;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CancelNfeServiceTest {

	private static final String VALID_CNPJ = "11.222.333/0001-81";

	@Mock
	private NfeRepositoryPort nfeRepositoryPort;

	@Mock
	private CompanyRepositoryPort companyRepositoryPort;

	@Mock
	private SubmitToSefazPort submitToSefazPort;

	private CancelNfeService service;

	private CompanyId companyId;
	private NfeDocumentId documentId;

	@BeforeEach
	void setUp() {
		service = new CancelNfeService(nfeRepositoryPort, companyRepositoryPort, submitToSefazPort);

		companyId = CompanyId.of(UUID.randomUUID());
		documentId = NfeDocumentId.of(UUID.randomUUID());

		lenient().when(companyRepositoryPort.findById(companyId)).thenReturn(Optional.of(company("SP")));
		lenient().when(submitToSefazPort.cancel(any())).thenReturn(new SefazSubmissionResult("cancel-protocol-1"));
		lenient().when(nfeRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	@DisplayName("Refuses to cancel a document that is not authorized")
	void aDocumentThatIsNotAuthorizedCannotBeCancelled() {
		NfeDocument document = documentWithStatus(NfeDocumentStatus.QUEUED, null, "SP");
		when(nfeRepositoryPort.findById(documentId)).thenReturn(Optional.of(document));

		assertThatThrownBy(() -> service.execute(command())).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("not AUTHORIZED");

		verify(submitToSefazPort, never()).cancel(any());
		verify(nfeRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Rejects cancelling a document past the default twenty-four-hour window")
	void aDocumentPastTheTwentyFourHourDefaultWindowIsRejected() {
		Instant twentyFiveHoursAgo = Instant.now().minus(Duration.ofHours(25));
		NfeDocument document = documentWithStatus(NfeDocumentStatus.AUTHORIZED, twentyFiveHoursAgo, "SP");
		when(nfeRepositoryPort.findById(documentId)).thenReturn(Optional.of(document));

		assertThatThrownBy(() -> service.execute(command())).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Cancellation window has expired");

		verify(submitToSefazPort, never()).cancel(any());
		verify(nfeRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Cancels a document within the default twenty-four-hour window")
	void aDocumentWithinTheTwentyFourHourDefaultWindowIsCancelled() {
		Instant twentyHoursAgo = Instant.now().minus(Duration.ofHours(20));
		NfeDocument document = documentWithStatus(NfeDocumentStatus.AUTHORIZED, twentyHoursAgo, "SP");
		when(nfeRepositoryPort.findById(documentId)).thenReturn(Optional.of(document));

		NfeDocument result = service.execute(command());

		assertThat(result.getStatus()).isEqualTo(NfeDocumentStatus.CANCELLED);
	}

	@Test
	@DisplayName("Honors a state's longer legal window past the default twenty-four hours")
	void aStateWithALongerLegalWindowIsHonoredPastTheDefaultTwentyFourHours() {
		lenient().when(companyRepositoryPort.findById(companyId)).thenReturn(Optional.of(company("AM")));
		Instant thirtyHoursAgo = Instant.now().minus(Duration.ofHours(30));
		NfeDocument document = documentWithStatus(NfeDocumentStatus.AUTHORIZED, thirtyHoursAgo, "AM");
		when(nfeRepositoryPort.findById(documentId)).thenReturn(Optional.of(document));

		NfeDocument result = service.execute(command());

		assertThat(result.getStatus()).isEqualTo(NfeDocumentStatus.CANCELLED);
	}

	@Test
	@DisplayName("Rejects cancellation once a state's longer legal window has also expired")
	void aStateWithALongerLegalWindowStillExpiresPastItsOwnLimit() {
		lenient().when(companyRepositoryPort.findById(companyId)).thenReturn(Optional.of(company("AM")));
		Instant fortyNineHoursAgo = Instant.now().minus(Duration.ofHours(49));
		NfeDocument document = documentWithStatus(NfeDocumentStatus.AUTHORIZED, fortyNineHoursAgo, "AM");
		when(nfeRepositoryPort.findById(documentId)).thenReturn(Optional.of(document));

		assertThatThrownBy(() -> service.execute(command())).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("Cancellation window has expired");
	}

	@Test
	@DisplayName("Requires a justification to cancel")
	void justificationIsMandatory() {
		assertThatThrownBy(() -> service.execute(new CancelNfeCommand(documentId.value(), " ")))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("justification is required");

		verify(nfeRepositoryPort, never()).findById(any());
	}

	@Test
	@DisplayName("Transmits the cancellation to SEFAZ with the justification and persists it")
	void successfulCancellationTransmitsToSefazWithTheJustificationAndPersistsIt() {
		Instant fiveHoursAgo = Instant.now().minus(Duration.ofHours(5));
		NfeDocument document = documentWithStatus(NfeDocumentStatus.AUTHORIZED, fiveHoursAgo, "SP");
		when(nfeRepositoryPort.findById(documentId)).thenReturn(Optional.of(document));

		NfeDocument result = service.execute(command());

		ArgumentCaptor<SefazCancellationRequest> requestCaptor = ArgumentCaptor.forClass(SefazCancellationRequest.class);
		verify(submitToSefazPort).cancel(requestCaptor.capture());
		assertThat(requestCaptor.getValue().accessKey()).isEqualTo(document.getAccessKey());
		assertThat(requestCaptor.getValue().protocol()).isEqualTo(document.getSefazProtocol());
		assertThat(requestCaptor.getValue().reason()).isEqualTo("customer changed their mind");

		assertThat(result.getStatus()).isEqualTo(NfeDocumentStatus.CANCELLED);
		assertThat(result.getCancellationJustification()).isEqualTo("customer changed their mind");
		assertThat(result.getCancelledAt()).isNotNull();
		verify(nfeRepositoryPort).save(any());
	}

	@Test
	@DisplayName("Rejects cancelling a document that does not exist")
	void aNonExistentDocumentIsRejected() {
		when(nfeRepositoryPort.findById(documentId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(command())).isInstanceOf(ResourceNotFoundException.class);

		verify(submitToSefazPort, never()).cancel(any());
	}

	@Test
	@DisplayName("Rejects cancelling when the issuing company does not exist")
	void anIssuingCompanyThatDoesNotExistIsRejected() {
		NfeDocument document = documentWithStatus(NfeDocumentStatus.AUTHORIZED, Instant.now(), "SP");
		when(nfeRepositoryPort.findById(documentId)).thenReturn(Optional.of(document));
		when(companyRepositoryPort.findById(companyId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(command())).isInstanceOf(BusinessRuleException.class);

		verify(submitToSefazPort, never()).cancel(any());
	}

	private CancelNfeCommand command() {
		return new CancelNfeCommand(documentId.value(), "customer changed their mind");
	}

	private Company company(String state) {
		return Company.of(companyId, Document.cnpj(VALID_CNPJ), "123456789", "987654", "6201500",
				TaxRegime.SIMPLES_NACIONAL, true, SefazEnvironment.HOMOLOGATION, "Rua Teste, 100", state,
				"nfe@example.com", "11999999999", null, null);
	}

	private NfeItem item() {
		UUID productId = UUID.randomUUID();
		TaxLineBreakdown line = new TaxLineBreakdown(TaxType.ICMS, new BigDecimal("100.00"), new BigDecimal("18"),
				new BigDecimal("18.00"), new BigDecimal("18.00"), false, null);
		ItemTaxBreakdown breakdown = new ItemTaxBreakdown(0, productId.toString(), List.of(line));
		return new NfeItem(productId, "Produto Teste", BigDecimal.ONE, new BigDecimal("100.00"), BigDecimal.ZERO,
				breakdown);
	}

	private NfeDocument documentWithStatus(NfeDocumentStatus status, Instant authorizedAt, String recipientState) {
		NfeItem item = item();
		NfeRecipient recipient = NfeRecipient.of(null, VALID_CNPJ, PersonType.COMPANY, "Cliente PJ Teste", "123456789",
				recipientState);
		TaxCalculationTotals totals = TaxCalculationTotals.from(List.of(item.taxBreakdown()));

		return NfeDocument.of(documentId, companyId, null, NaturezaOperacao.VENDA, new Cfop("5102"), recipient,
				List.of(item), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null, totals, status,
				Instant.now(), "001", 42L, "3".repeat(44), "PROTOCOL-1", false, null, "xml-ref", "danfe-ref",
				List.of(), authorizedAt, null, null);
	}
}
