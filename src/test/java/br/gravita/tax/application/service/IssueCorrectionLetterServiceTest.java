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
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.shared.PersonRef;
import br.gravita.core.domain.shared.PersonType;
import br.gravita.core.domain.tax.Cfop;
import br.gravita.core.domain.tax.CorrectionLetter;
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
import br.gravita.core.ports.inbound.tax.IssueCorrectionLetterCommand;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfeRepositoryPort;
import br.gravita.core.ports.outbound.tax.SefazCorrectionRequest;
import br.gravita.core.ports.outbound.tax.SefazSubmissionResult;
import br.gravita.core.ports.outbound.tax.SubmitToSefazPort;
import br.gravita.core.usercases.tax.IssueCorrectionLetterService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IssueCorrectionLetterServiceTest {

	private static final String VALID_CNPJ = "11.222.333/0001-81";

	@Mock
	private NfeRepositoryPort nfeRepositoryPort;

	@Mock
	private CompanyRepositoryPort companyRepositoryPort;

	@Mock
	private SubmitToSefazPort submitToSefazPort;

	private IssueCorrectionLetterService service;

	private CompanyId companyId;
	private NfeDocumentId documentId;

	@BeforeEach
	void setUp() {
		service = new IssueCorrectionLetterService(nfeRepositoryPort, companyRepositoryPort, submitToSefazPort);

		companyId = CompanyId.of(UUID.randomUUID());
		documentId = NfeDocumentId.of(UUID.randomUUID());

		lenient().when(companyRepositoryPort.findById(companyId)).thenReturn(Optional.of(company()));
		lenient().when(nfeRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	@DisplayName("Rejects a new correction letter once twenty correction letter events already exist")
	void rejectsOnceTwentyCorrectionLetterEventsAlreadyExist() {
		final List<CorrectionLetter> maxedOut = new ArrayList<>();
		for (int i = 1; i <= NfeDocument.MAX_CORRECTION_LETTERS; i++) {
			maxedOut.add(new CorrectionLetter(i, "Correção " + i, "PROT" + i, Instant.now()));
		}
		final NfeDocument document = authorizedDocument(maxedOut);
		when(nfeRepositoryPort.findById(documentId)).thenReturn(Optional.of(document));

		assertThatThrownBy(() -> service.execute(new IssueCorrectionLetterCommand(documentId.value(), "Nova correção")))
				.isInstanceOf(BusinessRuleException.class);

		verify(submitToSefazPort, never()).correct(any());
		verify(nfeRepositoryPort, never()).save(any());
	}

	@ParameterizedTest
	@EnumSource(value = NfeDocumentStatus.class, names = {"DRAFT", "CANCELLED", "VOIDED"})
	@DisplayName("Requires the document to be authorized")
	void requiresTheDocumentToBeAuthorized(final NfeDocumentStatus status) {
		final NfeDocument document = documentWithStatus(status, List.of());
		when(nfeRepositoryPort.findById(documentId)).thenReturn(Optional.of(document));

		assertThatThrownBy(
				() -> service.execute(new IssueCorrectionLetterCommand(documentId.value(), "Nova correção")))
				.isInstanceOf(BusinessRuleException.class);

		verify(submitToSefazPort, never()).correct(any());
	}

	@Test
	@DisplayName("Assigns each event a sequence number and a SEFAZ protocol and persists both")
	void eachEventIsAssignedASequenceNumberAndASefazProtocolAndBothArePersisted() {
		final NfeDocument document = authorizedDocument(List.of(new CorrectionLetter(1, "Primeira correção", "PROT1",
				Instant.now())));
		when(nfeRepositoryPort.findById(documentId)).thenReturn(Optional.of(document));
		when(submitToSefazPort.correct(any())).thenReturn(new SefazSubmissionResult("PROT2"));

		final CorrectionLetter result = service
				.execute(new IssueCorrectionLetterCommand(documentId.value(), "Segunda correção"));

		assertThat(result.sequenceNumber()).isEqualTo(2);
		assertThat(result.protocol()).isEqualTo("PROT2");
		assertThat(result.text()).isEqualTo("Segunda correção");

		final ArgumentCaptor<SefazCorrectionRequest> requestCaptor = ArgumentCaptor.forClass(SefazCorrectionRequest.class);
		verify(submitToSefazPort).correct(requestCaptor.capture());
		assertThat(requestCaptor.getValue().sequenceNumber()).isEqualTo(2);

		final ArgumentCaptor<NfeDocument> savedCaptor = ArgumentCaptor.forClass(NfeDocument.class);
		verify(nfeRepositoryPort).save(savedCaptor.capture());
		assertThat(savedCaptor.getValue().getCorrectionLetters()).hasSize(2);
		assertThat(savedCaptor.getValue().getCorrectionLetters().get(1).protocol()).isEqualTo("PROT2");
	}

	@Test
	@DisplayName("Rejects a correction letter for an NF-e document that does not exist")
	void anNfeDocumentThatDoesNotExistIsRejected() {
		when(nfeRepositoryPort.findById(documentId)).thenReturn(Optional.empty());

		assertThatThrownBy(
				() -> service.execute(new IssueCorrectionLetterCommand(documentId.value(), "Nova correção")))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	private Company company() {
		return Company.builder()
				.id(companyId)
				.name("Acme Ltda")
				.cnpj(Document.cnpj(VALID_CNPJ))
				.ie("123456789")
				.im("987654")
				.cnae("6201500")
				.taxRegime(br.gravita.core.domain.masterdata.TaxRegime.SIMPLES_NACIONAL)
				.simplesOptante(true)
				.sefazEnvironment(SefazEnvironment.HOMOLOGATION)
				.address("Rua Teste, 100")
				.state("SP")
				.issuingEmail("nfe@example.com")
				.phone("11999999999")
				.logoUrl(null)
				.parentCompanyId(null)
				.build();
	}

	private NfeItem item() {
		final UUID productId = UUID.randomUUID();
		final TaxLineBreakdown line = new TaxLineBreakdown(TaxType.ICMS, new BigDecimal("100.00"), new BigDecimal("18"),
				new BigDecimal("18.00"), new BigDecimal("18.00"), false, null);
		final ItemTaxBreakdown breakdown = new ItemTaxBreakdown(0, productId.toString(), List.of(line));
		return new NfeItem(productId, "Produto Teste", BigDecimal.ONE, new BigDecimal("100.00"), BigDecimal.ZERO,
				breakdown);
	}

	private NfeDocument authorizedDocument(final List<CorrectionLetter> correctionLetters) {
		return documentWithStatus(NfeDocumentStatus.AUTHORIZED, correctionLetters);
	}

	private NfeDocument documentWithStatus(final NfeDocumentStatus status, final List<CorrectionLetter> correctionLetters) {
		final NfeItem item = item();
		final NfeRecipient recipient = NfeRecipient.of(PersonRef.of(UUID.randomUUID()), VALID_CNPJ, PersonType.COMPANY,
				"Cliente PJ Teste", "123456789", "RJ");
		final TaxCalculationTotals totals = TaxCalculationTotals.from(List.of(item.taxBreakdown()));

		final String documentSeries = status == NfeDocumentStatus.DRAFT ? null : "001";
		final Long documentNumber = status == NfeDocumentStatus.DRAFT ? null : 42L;
		final String accessKey = status == NfeDocumentStatus.DRAFT ? null : "3".repeat(44);
		final String sefazProtocol = status == NfeDocumentStatus.DRAFT ? null : "PROTOCOL-ORIGINAL";

		return NfeDocument.builder()
				.id(documentId)
				.issuerCompanyId(companyId)
				.originSalesOrderId(null)
				.naturezaOperacao(NaturezaOperacao.VENDA)
				.cfop(new Cfop("5102"))
				.recipient(recipient)
				.items(List.of(item))
				.freight(BigDecimal.ZERO)
				.insurance(BigDecimal.ZERO)
				.otherExpenses(BigDecimal.ZERO)
				.transport(null)
				.referencedAccessKey(null)
				.additionalInfo(null)
				.taxTotals(totals)
				.status(status)
				.createdAt(Instant.now())
				.documentSeries(documentSeries)
				.documentNumber(documentNumber)
				.accessKey(accessKey)
				.sefazProtocol(sefazProtocol)
				.contingencyMode(false)
				.rejectionReason(null)
				.xmlStorageRef(null)
				.danfeStorageRef(null)
				.correctionLetters(correctionLetters)
				.authorizedAt(null)
				.cancellationJustification(null)
				.cancelledAt(null)
				.build();
	}
}
