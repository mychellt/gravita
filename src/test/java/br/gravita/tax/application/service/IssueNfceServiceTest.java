package br.gravita.tax.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.PaymentMethodType;
import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.DocumentNumber;
import br.gravita.core.domain.masterdata.FiscalDocumentType;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.domain.masterdata.TaxRegime;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.tax.NfceSale;
import br.gravita.core.domain.tax.NfceSaleId;
import br.gravita.core.domain.tax.NfceSaleStatus;
import br.gravita.core.domain.tax.Payment;
import br.gravita.core.domain.tax.PosSession;
import br.gravita.core.domain.tax.PosSessionId;
import br.gravita.core.domain.tax.SaleItem;
import br.gravita.core.domain.tax.SefazUnavailableException;
import br.gravita.core.ports.inbound.masterdata.AllocateDocumentNumberCommand;
import br.gravita.core.ports.inbound.masterdata.AllocateDocumentNumberUseCase;
import br.gravita.core.ports.inbound.tax.CalculateTaxCommand;
import br.gravita.core.ports.inbound.tax.CalculateTaxUseCase;
import br.gravita.core.ports.inbound.tax.IssueNfceCommand;
import br.gravita.core.ports.inbound.tax.NfceIssuanceResult;
import br.gravita.core.ports.inbound.tax.TaxCalculationResult;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfceRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.PosSessionRepositoryPort;
import br.gravita.core.ports.outbound.tax.SefazSubmissionResult;
import br.gravita.core.ports.outbound.tax.SubmitToSefazPort;
import br.gravita.core.ports.outbound.tax.TransmissionQueuePort;
import br.gravita.core.usercases.tax.IssueNfceService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumMap;
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
class IssueNfceServiceTest {

	@Mock
	private NfceRepositoryPort nfceRepositoryPort;

	@Mock
	private PosSessionRepositoryPort posSessionRepositoryPort;

	@Mock
	private CompanyRepositoryPort companyRepositoryPort;

	@Mock
	private CalculateTaxUseCase calculateTaxUseCase;

	@Mock
	private AllocateDocumentNumberUseCase allocateDocumentNumberUseCase;

	@Mock
	private SubmitToSefazPort submitToSefazPort;

	@Mock
	private TransmissionQueuePort transmissionQueuePort;

	private IssueNfceService service;

	private UUID saleId;
	private PosSessionId sessionId;
	private CompanyId companyId;

	@BeforeEach
	void setUp() {
		service = new IssueNfceService(nfceRepositoryPort, posSessionRepositoryPort, companyRepositoryPort,
				calculateTaxUseCase, allocateDocumentNumberUseCase, submitToSefazPort, transmissionQueuePort);

		saleId = UUID.randomUUID();
		sessionId = PosSessionId.of(UUID.randomUUID());
		companyId = CompanyId.of(UUID.randomUUID());

		lenient().when(nfceRepositoryPort.findById(NfceSaleId.of(saleId))).thenReturn(Optional.of(draftSale()));
		lenient().when(posSessionRepositoryPort.findById(sessionId)).thenReturn(Optional.of(session()));
		lenient().when(companyRepositoryPort.findById(companyId)).thenReturn(Optional.of(company()));
		lenient().when(calculateTaxUseCase.execute(any())).thenReturn(emptyTaxResult());
		lenient().when(allocateDocumentNumberUseCase.execute(any())).thenReturn(new DocumentNumber("001", 10L));
		lenient().when(nfceRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
	}

	private NfceSale draftSale() {
		SaleItem item = new SaleItem(UUID.randomUUID(), BigDecimal.ONE, new BigDecimal("10.00"), null);
		return NfceSale.register(NfceSaleId.of(saleId), sessionId, List.of(item), null,
				List.of(new Payment(PaymentMethodType.CASH, new BigDecimal("10.00"))), null, Instant.now());
	}

	private PosSession session() {
		return PosSession.open(sessionId, UUID.randomUUID(), UUID.randomUUID(), companyId,
				new BigDecimal("100.00"), Instant.now());
	}

	private Company company() {
		return Company.of(companyId, "Acme Ltda", Document.cnpj("11.222.333/0001-81"), "123456789", "987654", "6201500",
				TaxRegime.SIMPLES_NACIONAL, true, SefazEnvironment.HOMOLOGATION, "Rua Teste, 100", "SP",
				"nfce@example.com", "11999999999", null, null);
	}

	private TaxCalculationResult emptyTaxResult() {
		return new TaxCalculationResult(List.of(),
				new br.gravita.core.domain.tax.TaxCalculationTotals(new EnumMap<>(br.gravita.core.domain.tax.TaxType.class),
						BigDecimal.ZERO));
	}

	@Test
	@DisplayName("Moves the sale to authorized with the SEFAZ protocol on online authorization")
	void ac1_onlineAuthorizationMovesTheSaleToAuthorizedWithTheSefazProtocol() {
		when(submitToSefazPort.submit(any())).thenReturn(new SefazSubmissionResult("protocol-123"));

		NfceIssuanceResult result = service.execute(new IssueNfceCommand(saleId));

		assertThat(result.status()).isEqualTo(NfceSaleStatus.AUTHORIZED);
		assertThat(result.protocol()).isEqualTo("protocol-123");
		assertThat(result.accessKey()).hasSize(44);
		verify(transmissionQueuePort, never()).enqueue(any());

		ArgumentCaptor<NfceSale> captor = ArgumentCaptor.forClass(NfceSale.class);
		verify(nfceRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo(NfceSaleStatus.AUTHORIZED);
		assertThat(captor.getValue().isContingencyMode()).isFalse();
	}

	@Test
	@DisplayName("Queues the sale for contingency instead of blocking when SEFAZ is unavailable")
	void ac2_sefazUnavailableQueuesTheSaleForContingencyInsteadOfBlocking() {
		when(submitToSefazPort.submit(any())).thenThrow(new SefazUnavailableException("timeout", null));

		NfceIssuanceResult result = service.execute(new IssueNfceCommand(saleId));

		assertThat(result.status()).isEqualTo(NfceSaleStatus.PENDING_SYNC);
		assertThat(result.protocol()).isNull();
		verify(transmissionQueuePort).enqueue(NfceSaleId.of(saleId));

		ArgumentCaptor<NfceSale> captor = ArgumentCaptor.forClass(NfceSale.class);
		verify(nfceRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo(NfceSaleStatus.PENDING_SYNC);
		assertThat(captor.getValue().isContingencyMode()).isTrue();
	}

	@Test
	@DisplayName("Always takes the tax totals from the shared CalculateTax use case")
	void ac3_theTaxTotalsAlwaysComeFromTheSharedCalculateTaxUseCase() {
		when(submitToSefazPort.submit(any())).thenReturn(new SefazSubmissionResult("protocol-123"));

		service.execute(new IssueNfceCommand(saleId));

		ArgumentCaptor<CalculateTaxCommand> captor = ArgumentCaptor.forClass(CalculateTaxCommand.class);
		verify(calculateTaxUseCase).execute(captor.capture());
		assertThat(captor.getValue().items()).hasSize(1);
		assertThat(captor.getValue().operationType()).isEqualTo("VENDA_PDV");
		assertThat(captor.getValue().taxRegime())
				.isEqualTo(br.gravita.core.domain.tax.TaxRegime.SIMPLES_NACIONAL);
	}

	@Test
	@DisplayName("Allocates document numbering exactly once per sale")
	void ac4_documentNumberingIsAllocatedExactlyOncePerSale() {
		when(submitToSefazPort.submit(any())).thenReturn(new SefazSubmissionResult("protocol-123"));

		service.execute(new IssueNfceCommand(saleId));

		ArgumentCaptor<AllocateDocumentNumberCommand> captor = ArgumentCaptor.forClass(AllocateDocumentNumberCommand.class);
		verify(allocateDocumentNumberUseCase, org.mockito.Mockito.times(1)).execute(captor.capture());
		assertThat(captor.getValue().companyId()).isEqualTo(companyId);
		assertThat(captor.getValue().documentType()).isEqualTo(FiscalDocumentType.NFCE);
	}

	@Test
	@DisplayName("Rejects issuing a sale that does not exist")
	void aNonExistentSaleIsRejected() {
		when(nfceRepositoryPort.findById(NfceSaleId.of(saleId))).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new IssueNfceCommand(saleId)))
				.isInstanceOf(ResourceNotFoundException.class);

		verify(allocateDocumentNumberUseCase, never()).execute(any());
	}

	@Test
	@DisplayName("Rejects issuing a sale that was already issued")
	void anAlreadyIssuedSaleCannotBeIssuedAgain() {
		NfceSale alreadyAuthorized = draftSale().authorize("001", 1L, "3".repeat(44), "protocol-1");
		when(nfceRepositoryPort.findById(NfceSaleId.of(saleId))).thenReturn(Optional.of(alreadyAuthorized));

		assertThatThrownBy(() -> service.execute(new IssueNfceCommand(saleId)))
				.isInstanceOf(BusinessRuleException.class);

		verify(allocateDocumentNumberUseCase, never()).execute(any());
	}
}
