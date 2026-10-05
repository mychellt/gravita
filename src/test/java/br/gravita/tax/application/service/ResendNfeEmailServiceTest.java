package br.gravita.tax.application.service;

import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.shared.PersonRef;
import br.gravita.core.domain.shared.PersonType;
import br.gravita.core.domain.tax.*;
import br.gravita.core.ports.inbound.tax.ResendNfeEmailCommand;
import br.gravita.core.ports.messaging.SendFiscalDocumentByEmailPort;
import br.gravita.core.ports.messaging.records.FiscalDocumentEmailRequest;
import br.gravita.core.ports.outbound.persistence.CustomerRepositoryPort;
import br.gravita.core.ports.outbound.persistence.XmlObjectStoragePort;
import br.gravita.core.ports.outbound.persistence.tax.NfeRepositoryPort;
import br.gravita.core.usercases.tax.ResendNfeEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
    @DisplayName("Resends the already stored XML and DANFE to the recipient rather than regenerating them")
    void ac5ResendsTheAlreadyStoredXmlAndDanfeToTheRecipientRatherThanRegeneratingThem() {
        final NfeDocument document = authorizedDocument();
        when(nfeRepositoryPort.findById(documentId)).thenReturn(Optional.of(document));
        when(customerRepositoryPort.get(recipientRef.id()))
                .thenReturn(Optional.of(CustomerDomain.builder().email("cliente@example.com").build()));
        when(xmlObjectStoragePort.retrieve("xml-ref")).thenReturn("xml-bytes".getBytes());
        when(xmlObjectStoragePort.retrieve("danfe-ref")).thenReturn("danfe-bytes".getBytes());

        service.execute(new ResendNfeEmailCommand(documentId.value()));

        final ArgumentCaptor<FiscalDocumentEmailRequest> captor = ArgumentCaptor.forClass(FiscalDocumentEmailRequest.class);
        verify(sendFiscalDocumentByEmailPort).send(captor.capture());
        assertThat(captor.getValue().to()).isEqualTo("cliente@example.com");
        assertThat(captor.getValue().xmlContent()).isEqualTo("xml-bytes".getBytes());
        assertThat(captor.getValue().danfeContent()).isEqualTo("danfe-bytes".getBytes());
    }

    @ParameterizedTest
    @EnumSource(value = NfeDocumentStatus.class, names = {"DRAFT", "QUEUED", "SENT", "REJECTED", "CANCELLED", "VOIDED"})
    @DisplayName("Only an authorized document can have its email resent")
    void onlyAnAuthorizedDocumentCanHaveItsEmailResent(final NfeDocumentStatus status) {
        final NfeDocument document = documentWithStatus(status);
        when(nfeRepositoryPort.findById(documentId)).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> service.execute(new ResendNfeEmailCommand(documentId.value())))
                .isInstanceOf(BusinessRuleException.class);

        verify(sendFiscalDocumentByEmailPort, never()).send(any());
    }

    @Test
    @DisplayName("Rejects a recipient with no email on file rather than silently skipping")
    void recipientWithNoEmailOnFileIsRejectedRatherThanSilentlySkipped() {
        final NfeDocument document = authorizedDocument();
        when(nfeRepositoryPort.findById(documentId)).thenReturn(Optional.of(document));
        when(customerRepositoryPort.get(recipientRef.id())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(new ResendNfeEmailCommand(documentId.value())))
                .isInstanceOf(BusinessRuleException.class);

        verify(sendFiscalDocumentByEmailPort, never()).send(any());
    }

    @Test
    @DisplayName("Rejects resending for an NF-e document that does not exist")
    void anNfeDocumentThatDoesNotExistIsRejected() {
        when(nfeRepositoryPort.findById(documentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(new ResendNfeEmailCommand(documentId.value())))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private NfeDocument authorizedDocument() {
        return documentWithStatus(NfeDocumentStatus.AUTHORIZED);
    }

    private NfeDocument documentWithStatus(final NfeDocumentStatus status) {
        final UUID productId = UUID.randomUUID();
        final TaxLineBreakdown line = new TaxLineBreakdown(TaxType.ICMS, new BigDecimal("100.00"), new BigDecimal("18"),
                new BigDecimal("18.00"), new BigDecimal("18.00"), false, null);
        final ItemTaxBreakdown breakdown = new ItemTaxBreakdown(0, productId.toString(), List.of(line));
        final NfeItem item = new NfeItem(productId, "Produto Teste", BigDecimal.ONE, new BigDecimal("100.00"),
                BigDecimal.ZERO, breakdown);
        final NfeRecipient recipient = NfeRecipient.of(recipientRef, VALID_CNPJ, PersonType.COMPANY, "Cliente PJ Teste",
                "123456789", "RJ");
        final TaxCalculationTotals totals = TaxCalculationTotals.from(List.of(item.taxBreakdown()));

        final String xmlRef = status == NfeDocumentStatus.AUTHORIZED ? "xml-ref" : null;
        final String danfeRef = status == NfeDocumentStatus.AUTHORIZED ? "danfe-ref" : null;
        final String sefazProtocol = status == NfeDocumentStatus.DRAFT || status == NfeDocumentStatus.QUEUED ? null
                : "PROTOCOL-1";

        return NfeDocument.builder()
        		.id(documentId)
        		.issuerCompanyId(CompanyId.of(UUID.randomUUID()))
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
        		.sefazProtocol(sefazProtocol)
        		.contingencyMode(false)
        		.rejectionReason(null)
        		.xmlStorageRef(xmlRef)
        		.danfeStorageRef(danfeRef)
        		.correctionLetters(List.of())
        		.authorizedAt(null)
        		.cancellationJustification(null)
        		.cancelledAt(null)
        		.build();
    }
}
