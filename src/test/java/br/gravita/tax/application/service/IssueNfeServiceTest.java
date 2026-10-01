package br.gravita.tax.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.exceptions.BusinessRuleException;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.DiscountCheckResult;
import br.gravita.core.domain.masterdata.DocumentNumber;
import br.gravita.core.domain.masterdata.FiscalDocumentType;
import br.gravita.core.domain.masterdata.MaxDiscountBehavior;
import br.gravita.core.domain.masterdata.PriceFormation;
import br.gravita.core.domain.masterdata.PriceTable;
import br.gravita.core.domain.masterdata.PriceTableId;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.shared.PersonType;
import br.gravita.core.domain.tax.Cfop;
import br.gravita.core.domain.tax.ItemTaxBreakdown;
import br.gravita.core.domain.tax.NaturezaOperacao;
import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.domain.tax.NfeDocumentStatus;
import br.gravita.core.domain.tax.TaxLineBreakdown;
import br.gravita.core.domain.tax.TaxType;
import br.gravita.core.ports.inbound.masterdata.AllocateDocumentNumberCommand;
import br.gravita.core.ports.inbound.masterdata.AllocateDocumentNumberUseCase;
import br.gravita.core.ports.inbound.tax.CalculateTaxCommand;
import br.gravita.core.ports.inbound.tax.CalculateTaxUseCase;
import br.gravita.core.ports.inbound.tax.IssueNfeCommand;
import br.gravita.core.ports.inbound.tax.IssueNfeCommand.ItemCommand;
import br.gravita.core.ports.inbound.tax.IssueNfeCommand.RecipientCommand;
import br.gravita.core.ports.inbound.tax.TaxCalculationResult;
import br.gravita.core.ports.inbound.tax.TaxOverrideCommand;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.PriceTableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfeRepositoryPort;
import br.gravita.core.ports.outbound.tax.CfopRegistryPort;
import br.gravita.core.ports.outbound.tax.TransmissionQueuePort;
import br.gravita.core.usercases.tax.IssueNfeService;
import java.math.BigDecimal;
import java.time.LocalDate;
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
class IssueNfeServiceTest {

	private static final String VALID_CNPJ = "11.222.333/0001-81";
	private static final String VALID_CPF = "529.982.247-25";

	@Mock
	private NfeRepositoryPort nfeRepositoryPort;

	@Mock
	private CompanyRepositoryPort companyRepositoryPort;

	@Mock
	private PriceTableRepositoryPort priceTableRepositoryPort;

	@Mock
	private CfopRegistryPort cfopRegistryPort;

	@Mock
	private CalculateTaxUseCase calculateTaxUseCase;

	@Mock
	private AllocateDocumentNumberUseCase allocateDocumentNumberUseCase;

	@Mock
	private TransmissionQueuePort transmissionQueuePort;

	private IssueNfeService service;

	private CompanyId companyId;
	private UUID productId;

	@BeforeEach
	void setUp() {
		service = new IssueNfeService(nfeRepositoryPort, companyRepositoryPort, priceTableRepositoryPort,
				cfopRegistryPort, calculateTaxUseCase, allocateDocumentNumberUseCase, transmissionQueuePort);

		companyId = CompanyId.of(UUID.randomUUID());
		productId = UUID.randomUUID();

		lenient().when(companyRepositoryPort.findById(companyId)).thenReturn(Optional.of(company()));
		lenient().when(cfopRegistryPort.resolve(any())).thenReturn(new Cfop("5102"));
		lenient().when(calculateTaxUseCase.execute(any())).thenReturn(taxResult());
		lenient().when(allocateDocumentNumberUseCase.execute(any())).thenReturn(new DocumentNumber("001", 10L));
		lenient().when(nfeRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
	}

	private Company company() {
		return Company.of(companyId, Document.cnpj(VALID_CNPJ), "123456789", "987654", "6201500",
				br.gravita.core.domain.masterdata.TaxRegime.SIMPLES_NACIONAL, true, SefazEnvironment.HOMOLOGATION,
				"Rua Teste, 100", "SP", "nfe@example.com", "11999999999", null, null);
	}

	private TaxCalculationResult taxResult() {
		TaxLineBreakdown line = new TaxLineBreakdown(TaxType.ICMS, new BigDecimal("100.00"), new BigDecimal("18"),
				new BigDecimal("18.00"), new BigDecimal("18.00"), false, null);
		List<ItemTaxBreakdown> items = List.of(new ItemTaxBreakdown(0, productId.toString(), List.of(line)));
		return new TaxCalculationResult(items, br.gravita.core.domain.tax.TaxCalculationTotals.from(items));
	}

	private RecipientCommand companyRecipient() {
		return new RecipientCommand(null, VALID_CNPJ, PersonType.COMPANY, "Cliente PJ Teste", "123456789", "RJ");
	}

	private ItemCommand item(BigDecimal discount) {
		return new ItemCommand(productId, "Produto Teste", BigDecimal.ONE, new BigDecimal("100.00"), discount);
	}

	private IssueNfeCommand command(UUID originSalesOrderId, NaturezaOperacao naturezaOperacao,
			RecipientCommand recipient, UUID priceTableId, String discountOverrideJustification,
			List<TaxOverrideCommand> taxOverrides, String referencedAccessKey) {
		return new IssueNfeCommand(companyId.value(), originSalesOrderId, naturezaOperacao, recipient,
				List.of(item(BigDecimal.ZERO)), priceTableId, discountOverrideJustification, taxOverrides,
				BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, referencedAccessKey, null);
	}

	private IssueNfeCommand defaultCommand() {
		return command(null, NaturezaOperacao.VENDA, companyRecipient(), null, null, null, null);
	}

	@Test
	@DisplayName("Preserves the origin sales order id when provided")
	void ac1_originSalesOrderIdIsPreservedWhenProvided() {
		UUID orderId = UUID.randomUUID();

		NfeDocument document = service.execute(command(orderId, NaturezaOperacao.VENDA, companyRecipient(), null,
				null, null, null));

		assertThat(document.getOriginSalesOrderId()).isEqualTo(orderId);
	}

	@Test
	@DisplayName("Leaves the origin sales order id null for a manual entry")
	void ac1_manualEntryLeavesOriginSalesOrderIdNull() {
		NfeDocument document = service.execute(defaultCommand());

		assertThat(document.getOriginSalesOrderId()).isNull();
	}

	@Test
	@DisplayName("Resolves the CFOP from the registry rather than hardcoding it")
	void ac2_cfopIsResolvedFromTheRegistryNotHardcoded() {
		when(cfopRegistryPort.resolve(NaturezaOperacao.VENDA)).thenReturn(new Cfop("6102"));

		NfeDocument document = service.execute(defaultCommand());

		assertThat(document.getCfop()).isEqualTo(new Cfop("6102"));
		verify(cfopRegistryPort).resolve(NaturezaOperacao.VENDA);
	}

	@Test
	@DisplayName("Validates the recipient's document and IE before the document is created")
	void ac3_recipientDocumentAndIeAreValidatedBeforeTheDocumentIsCreated() {
		NfeDocument document = service.execute(defaultCommand());

		assertThat(document.getRecipient().document().number()).isEqualTo(Document.digitsOnly(VALID_CNPJ));
		verify(nfeRepositoryPort).save(any());
	}

	@Test
	@DisplayName("Rejects an invalid recipient CPF/CNPJ before anything else happens")
	void ac3_anInvalidRecipientCpfCnpjIsRejectedBeforeAnythingElseHappens() {
		RecipientCommand invalidRecipient = new RecipientCommand(null, "111.111.111-11", PersonType.INDIVIDUAL,
				"Cliente Invalido", null, "SP");

		assertThatThrownBy(() -> service.execute(command(null, NaturezaOperacao.VENDA, invalidRecipient, null, null,
				null, null))).isInstanceOf(br.gravita.core.domain.shared.BusinessRuleException.class);

		verify(allocateDocumentNumberUseCase, never()).execute(any());
	}

	@Test
	@DisplayName("Rejects a company recipient without an IE")
	void ac3_aCompanyRecipientWithoutAnIeIsRejected() {
		RecipientCommand recipientWithoutIe = new RecipientCommand(null, VALID_CNPJ, PersonType.COMPANY,
				"Cliente PJ Teste", null, "RJ");

		assertThatThrownBy(() -> service.execute(command(null, NaturezaOperacao.VENDA, recipientWithoutIe, null, null,
				null, null))).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Takes the item tax totals from the shared CalculateTax use case")
	void ac4_itemTaxTotalsComeFromTheSharedCalculateTaxUseCase() {
		NfeDocument document = service.execute(defaultCommand());

		assertThat(document.getTaxTotals().grandTotal()).isEqualByComparingTo("18.00");
		verify(calculateTaxUseCase).execute(any());
	}

	@Test
	@DisplayName("Rejects a manual tax override without justification")
	void ac4_aManualTaxOverrideWithoutJustificationIsRejected() {
		TaxOverrideCommand unjustifiedOverride = new TaxOverrideCommand(0, TaxType.ICMS, new BigDecimal("5.00"), null);

		assertThatThrownBy(() -> service.execute(
				command(null, NaturezaOperacao.VENDA, companyRecipient(), null, null, List.of(unjustifiedOverride),
						null))).isInstanceOf(BusinessRuleException.class);

		verify(calculateTaxUseCase, never()).execute(any());
	}

	@Test
	@DisplayName("Forwards a justified manual tax override to the tax engine")
	void ac4_aJustifiedManualTaxOverrideIsForwardedToTheTaxEngine() {
		TaxOverrideCommand justifiedOverride = new TaxOverrideCommand(0, TaxType.ICMS, new BigDecimal("5.00"),
				"Isenção aprovada pelo fiscal");

		service.execute(command(null, NaturezaOperacao.VENDA, companyRecipient(), null, null,
				List.of(justifiedOverride), null));

		ArgumentCaptor<CalculateTaxCommand> captor = ArgumentCaptor.forClass(CalculateTaxCommand.class);
		verify(calculateTaxUseCase).execute(captor.capture());
		assertThat(captor.getValue().overrides()).containsExactly(justifiedOverride);
	}

	@Test
	@DisplayName("Rejects a discount that exceeds the price table's maximum")
	void ac5_discountExceedingThePriceTableMaxIsRejected() {
		UUID priceTableId = UUID.randomUUID();
		when(priceTableRepositoryPort.findById(PriceTableId.of(priceTableId)))
				.thenReturn(Optional.of(blockingPriceTable()));
		IssueNfeCommand withExcessiveDiscount = new IssueNfeCommand(companyId.value(), null, NaturezaOperacao.VENDA,
				companyRecipient(), List.of(item(new BigDecimal("50.00"))), priceTableId, null, null, BigDecimal.ZERO,
				BigDecimal.ZERO, BigDecimal.ZERO, null, null, null);

		assertThatThrownBy(() -> service.execute(withExcessiveDiscount))
				.isInstanceOf(br.gravita.core.domain.shared.BusinessRuleException.class);

		verify(allocateDocumentNumberUseCase, never()).execute(any());
	}

	@Test
	@DisplayName("Lets an explicitly justified override bypass the price table's maximum discount")
	void ac5_anExplicitlyJustifiedOverrideBypassesThePriceTableMaxDiscount() {
		UUID priceTableId = UUID.randomUUID();
		when(priceTableRepositoryPort.findById(PriceTableId.of(priceTableId)))
				.thenReturn(Optional.of(blockingPriceTable()));
		IssueNfeCommand withJustifiedDiscount = new IssueNfeCommand(companyId.value(), null, NaturezaOperacao.VENDA,
				companyRecipient(), List.of(item(new BigDecimal("50.00"))), priceTableId,
				"Desconto aprovado pela gerência", null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null,
				null, null);

		NfeDocument document = service.execute(withJustifiedDiscount);

		assertThat(document.getStatus()).isEqualTo(NfeDocumentStatus.QUEUED);
	}

	@Test
	@DisplayName("Rejects a return without a referenced access key")
	void ac6_aReturnWithoutAReferencedAccessKeyIsRejected() {
		assertThatThrownBy(() -> service.execute(
				command(null, NaturezaOperacao.DEVOLUCAO, companyRecipient(), null, null, null, null)))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Accepts a return with a referenced access key")
	void ac6_aReturnWithAReferencedAccessKeyIsAccepted() {
		String referencedAccessKey = "3".repeat(44);

		NfeDocument document = service.execute(
				command(null, NaturezaOperacao.DEVOLUCAO, companyRecipient(), null, null, null, referencedAccessKey));

		assertThat(document.getReferencedAccessKey()).isEqualTo(referencedAccessKey);
	}

	@Test
	@DisplayName("Queues the document and enqueues it for transmission on success")
	void ac7_onSuccessTheDocumentIsQueuedAndEnqueuedForTransmission() {
		NfeDocument document = service.execute(defaultCommand());

		assertThat(document.getStatus()).isEqualTo(NfeDocumentStatus.QUEUED);
		assertThat(document.getAccessKey()).hasSize(44);
		assertThat(document.getDocumentNumber()).isEqualTo(10L);

		verify(transmissionQueuePort, times(1)).enqueue(document.getId());

		ArgumentCaptor<AllocateDocumentNumberCommand> captor = ArgumentCaptor
				.forClass(AllocateDocumentNumberCommand.class);
		verify(allocateDocumentNumberUseCase).execute(captor.capture());
		assertThat(captor.getValue().companyId()).isEqualTo(companyId);
		assertThat(captor.getValue().documentType()).isEqualTo(FiscalDocumentType.NFE);
	}

	@Test
	@DisplayName("Rejects issuing when the issuing company does not exist")
	void anIssuingCompanyThatDoesNotExistIsRejected() {
		when(companyRepositoryPort.findById(companyId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(defaultCommand())).isInstanceOf(ResourceNotFoundException.class);

		verify(allocateDocumentNumberUseCase, never()).execute(any());
	}

	private PriceTable blockingPriceTable() {
		return PriceTable.of(PriceTableId.of(UUID.randomUUID()), PriceFormation.FIXED, LocalDate.now().minusDays(1),
				null, new BigDecimal("10"), MaxDiscountBehavior.BLOCK, List.of());
	}
}
