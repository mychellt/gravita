package br.gravita.tax.application.service;

import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.shared.PersonRef;
import br.gravita.core.domain.shared.PersonType;
import br.gravita.core.domain.tax.*;
import br.gravita.core.ports.inbound.tax.TransmissionResult;
import br.gravita.core.ports.inbound.tax.TransmitNfeCommand;
import br.gravita.core.ports.messaging.SendFiscalDocumentByEmailPort;
import br.gravita.core.ports.messaging.records.FiscalDocumentEmailRequest;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.CustomerRepositoryPort;
import br.gravita.core.ports.outbound.persistence.XmlObjectStoragePort;
import br.gravita.core.ports.outbound.persistence.tax.NfeRepositoryPort;
import br.gravita.core.ports.outbound.tax.*;
import br.gravita.core.usercases.tax.TransmitNfeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransmitNfeServiceTest {

    private static final String VALID_CNPJ = "11.222.333/0001-81";

    @Mock
    private NfeRepositoryPort nfeRepositoryPort;

    @Mock
    private CompanyRepositoryPort companyRepositoryPort;

    @Mock
    private CustomerRepositoryPort customerRepositoryPort;

    @Mock
    private SubmitToSefazPort submitToSefazPort;

    @Mock
    private GenerateDanfePort generateDanfePort;

    @Mock
    private SendFiscalDocumentByEmailPort sendFiscalDocumentByEmailPort;

    @Mock
    private XmlObjectStoragePort xmlObjectStoragePort;

    @Mock
    private TransmissionQueuePort transmissionQueuePort;

    private TransmitNfeService service;

    private CompanyId companyId;
    private NfeDocumentId documentId;
    private PersonRef recipientRef;

    @BeforeEach
    void setUp() {
        service = new TransmitNfeService(nfeRepositoryPort, companyRepositoryPort, customerRepositoryPort,
                submitToSefazPort, generateDanfePort, sendFiscalDocumentByEmailPort, xmlObjectStoragePort,
                transmissionQueuePort);

        companyId = CompanyId.of(UUID.randomUUID());
        documentId = NfeDocumentId.of(UUID.randomUUID());
        recipientRef = PersonRef.of(UUID.randomUUID());

        lenient().when(companyRepositoryPort.findById(companyId)).thenReturn(Optional.of(company()));
        lenient().when(nfeRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(generateDanfePort.generate(any(), any(), any())).thenReturn("danfe-bytes".getBytes());
        lenient().when(xmlObjectStoragePort.store(any(), any())).thenReturn(UUID.randomUUID().toString());
        lenient().when(customerRepositoryPort.get(recipientRef.id())).thenReturn(Optional.of(customerWithEmail()));
    }

    @Test
    @DisplayName("Never handles signing itself, leaving it entirely to the SubmitToSefaz port adapter")
    void ac1SigningIsNeverHandledHereItIsLeftEntirelyToTheSubmitToSefazPortAdapter() {
        final NfeDocument document = queuedDocument(false);
        when(nfeRepositoryPort.findById(documentId)).thenReturn(Optional.of(document));
        when(submitToSefazPort.submit(any())).thenReturn(new SefazSubmissionResult("PROT-1"));

        final TransmissionResult result = service.execute(new TransmitNfeCommand(documentId.value()));

        assertThat(result.status()).isEqualTo(NfeDocumentStatus.AUTHORIZED);
        assertThat(result.protocol()).isEqualTo("PROT-1");
    }

    @Test
    @DisplayName("Lets a SEFAZ timeout propagate instead of failing the document outright")
    void ac2ASefazTimeoutIsLeftToPropagateInsteadOfFailingTheDocumentOutright() {
        final NfeDocument document = queuedDocument(false);
        when(nfeRepositoryPort.findById(documentId)).thenReturn(Optional.of(document));
        when(submitToSefazPort.submit(any())).thenThrow(new SefazUnavailableException("timeout", null));

        assertThatThrownBy(() -> service.execute(new TransmitNfeCommand(documentId.value())))
                .isInstanceOf(SefazUnavailableException.class);

        verify(nfeRepositoryPort, never()).save(argThatStatusIs(NfeDocumentStatus.REJECTED));
        verify(transmissionQueuePort, never()).remove(any());
    }

    @Test
    @DisplayName("Resubmits a document already sent in a prior timed-out attempt without transitioning it again")
    void ac2ADocumentAlreadySentFromAPriorTimedOutAttemptIsResubmittedWithoutTransitioningAgain() {
        final NfeDocument document = sentDocument(false);
        when(nfeRepositoryPort.findById(documentId)).thenReturn(Optional.of(document));
        when(submitToSefazPort.submit(any())).thenReturn(new SefazSubmissionResult("PROT-RETRY"));

        final TransmissionResult result = service.execute(new TransmitNfeCommand(documentId.value()));

        assertThat(result.status()).isEqualTo(NfeDocumentStatus.AUTHORIZED);
        assertThat(result.protocol()).isEqualTo("PROT-RETRY");
    }

    @Test
    @DisplayName("Submits a document in contingency mode with the contingency flag set on the SEFAZ request")
    void ac3ADocumentInContingencyModeIsSubmittedWithTheContingencyFlagSetOnTheSefazRequest() {
        final NfeDocument document = sentDocument(true);
        when(nfeRepositoryPort.findById(documentId)).thenReturn(Optional.of(document));
        when(submitToSefazPort.submit(any())).thenReturn(new SefazSubmissionResult("SVC-PROT"));

        service.execute(new TransmitNfeCommand(documentId.value()));

        final ArgumentCaptor<SefazSubmissionRequest> captor = ArgumentCaptor.forClass(SefazSubmissionRequest.class);
        verify(submitToSefazPort).submit(captor.capture());
        assertThat(captor.getValue().contingency()).isTrue();
    }

    @Test
    @DisplayName("Renders the DANFE in portrait orientation by default on authorization")
    void ac4OnAuthorizationTheDanfeIsRenderedInPortraitOrientationByDefault() {
        final NfeDocument document = queuedDocument(false);
        when(nfeRepositoryPort.findById(documentId)).thenReturn(Optional.of(document));
        when(submitToSefazPort.submit(any())).thenReturn(new SefazSubmissionResult("PROT-1"));

        service.execute(new TransmitNfeCommand(documentId.value()));

        verify(generateDanfePort).generate(any(), any(), org.mockito.ArgumentMatchers.eq(DanfeOrientation.PORTRAIT));
    }

    @Test
    @DisplayName("Emails the XML and DANFE to the recipient automatically on authorization")
    void ac5OnAuthorizationXmlAndDanfeAreEmailedToTheRecipientAutomatically() {
        final NfeDocument document = queuedDocument(false);
        when(nfeRepositoryPort.findById(documentId)).thenReturn(Optional.of(document));
        when(submitToSefazPort.submit(any())).thenReturn(new SefazSubmissionResult("PROT-1"));

        service.execute(new TransmitNfeCommand(documentId.value()));

        final ArgumentCaptor<FiscalDocumentEmailRequest> captor = ArgumentCaptor.forClass(FiscalDocumentEmailRequest.class);
        verify(sendFiscalDocumentByEmailPort).send(captor.capture());
        assertThat(captor.getValue().to()).isEqualTo("cliente@example.com");
        assertThat(captor.getValue().xmlContent()).isNotEmpty();
        assertThat(captor.getValue().danfeContent()).isNotEmpty();
    }

    @Test
    @DisplayName("Keeps the transmission successful when the recipient has no email, so a manual resend can cover it later")
    void ac5ARecipientWithNoEmailOnFileStillLeavesTheTransmissionSuccessfulSoAManualResendCanCoverItLater() {
        final NfeDocument document = queuedDocument(false);
        when(nfeRepositoryPort.findById(documentId)).thenReturn(Optional.of(document));
        when(submitToSefazPort.submit(any())).thenReturn(new SefazSubmissionResult("PROT-1"));
        when(customerRepositoryPort.get(recipientRef.id())).thenReturn(Optional.empty());

        final TransmissionResult result = service.execute(new TransmitNfeCommand(documentId.value()));

        assertThat(result.status()).isEqualTo(NfeDocumentStatus.AUTHORIZED);
        verify(sendFiscalDocumentByEmailPort, never()).send(any());
    }

    @Test
    @DisplayName("Stores the rendered XML and DANFE and saves the authorized document with both references")
    void ac6TheRenderedXmlAndDanfeAreStoredAndTheAuthorizedDocumentIsSavedWithBothReferences() {
        final NfeDocument document = queuedDocument(false);
        when(nfeRepositoryPort.findById(documentId)).thenReturn(Optional.of(document));
        when(submitToSefazPort.submit(any())).thenReturn(new SefazSubmissionResult("PROT-1"));
        when(xmlObjectStoragePort.store(any(), any())).thenReturn("xml-ref", "danfe-ref");

        service.execute(new TransmitNfeCommand(documentId.value()));

        final ArgumentCaptor<NfeDocument> savedCaptor = ArgumentCaptor.forClass(NfeDocument.class);
        verify(nfeRepositoryPort, org.mockito.Mockito.atLeastOnce()).save(savedCaptor.capture());
        final NfeDocument saved = savedCaptor.getAllValues().get(savedCaptor.getAllValues().size() - 1);
        assertThat(saved.getXmlStorageRef()).isEqualTo("xml-ref");
        assertThat(saved.getDanfeStorageRef()).isEqualTo("danfe-ref");
        verify(transmissionQueuePort).remove(documentId.value());
    }

    @Test
    @DisplayName("Surfaces a SEFAZ rejection's reason and removes the document from the queue instead of retrying")
    void sefazRejectionSurfacesTheReasonAndRemovesTheDocumentFromTheQueueInsteadOfRetrying() {
        final NfeDocument document = queuedDocument(false);
        when(nfeRepositoryPort.findById(documentId)).thenReturn(Optional.of(document));
        when(submitToSefazPort.submit(any())).thenReturn(new SefazSubmissionResult(null, "CFOP inválido"));

        final TransmissionResult result = service.execute(new TransmitNfeCommand(documentId.value()));

        assertThat(result.status()).isEqualTo(NfeDocumentStatus.REJECTED);
        assertThat(result.rejectionReason()).isEqualTo("CFOP inválido");
        verify(transmissionQueuePort).remove(documentId.value());
        verify(generateDanfePort, never()).generate(any(), any(), any());
    }

    @Test
    @DisplayName("Rejects transmitting a document that is not queued or sent")
    void documentThatIsNotQueuedOrSentIsRejectedForTransmission() {
        final NfeDocument document = documentWithStatus(NfeDocumentStatus.AUTHORIZED, false);
        when(nfeRepositoryPort.findById(documentId)).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> service.execute(new TransmitNfeCommand(documentId.value())))
                .isInstanceOf(BusinessRuleException.class);

        verify(submitToSefazPort, never()).submit(any());
    }

    @Test
    @DisplayName("Rejects transmitting an NF-e document that does not exist")
    void anNfeDocumentThatDoesNotExistIsRejected() {
        when(nfeRepositoryPort.findById(documentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(new TransmitNfeCommand(documentId.value())))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Rejects transmitting when the issuing company does not exist")
    void anIssuingCompanyThatDoesNotExistIsRejected() {
        final NfeDocument document = queuedDocument(false);
        when(nfeRepositoryPort.findById(documentId)).thenReturn(Optional.of(document));
        when(companyRepositoryPort.findById(companyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(new TransmitNfeCommand(documentId.value())))
                .isInstanceOf(BusinessRuleException.class);

        verify(submitToSefazPort, never()).submit(any());
    }

    private NfeDocument argThatStatusIs(final NfeDocumentStatus status) {
        return org.mockito.ArgumentMatchers.argThat(document -> document.getStatus() == status);
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

    private CustomerDomain customerWithEmail() {
        return CustomerDomain.builder().email("cliente@example.com").build();
    }

    private NfeItem item() {
        final UUID productId = UUID.randomUUID();
        final TaxLineBreakdown line = new TaxLineBreakdown(TaxType.ICMS, new BigDecimal("100.00"), new BigDecimal("18"),
                new BigDecimal("18.00"), new BigDecimal("18.00"), false, null);
        final ItemTaxBreakdown breakdown = new ItemTaxBreakdown(0, productId.toString(), List.of(line));
        return new NfeItem(productId, "Produto Teste", BigDecimal.ONE, new BigDecimal("100.00"), BigDecimal.ZERO,
                breakdown);
    }

    private NfeDocument queuedDocument(final boolean contingencyMode) {
        return documentWithStatus(NfeDocumentStatus.QUEUED, contingencyMode);
    }

    private NfeDocument sentDocument(final boolean contingencyMode) {
        return documentWithStatus(NfeDocumentStatus.SENT, contingencyMode);
    }

    private NfeDocument documentWithStatus(final NfeDocumentStatus status, final boolean contingencyMode) {
        final NfeItem item = item();
        final NfeRecipient recipient = NfeRecipient.of(recipientRef, VALID_CNPJ, PersonType.COMPANY, "Cliente PJ Teste",
                "123456789", "RJ");
        final TaxCalculationTotals totals = TaxCalculationTotals.from(List.of(item.taxBreakdown()));

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
        		.documentSeries("001")
        		.documentNumber(42L)
        		.accessKey("3".repeat(44))
        		.sefazProtocol(null)
        		.contingencyMode(contingencyMode)
        		.rejectionReason(null)
        		.xmlStorageRef(null)
        		.danfeStorageRef(null)
        		.correctionLetters(List.of())
        		.authorizedAt(null)
        		.cancellationJustification(null)
        		.cancelledAt(null)
        		.build();
    }
}
