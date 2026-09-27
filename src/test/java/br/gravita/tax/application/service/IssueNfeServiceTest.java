package br.gravita.tax.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.IeIndicator;
import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.DocumentNumber;
import br.gravita.core.domain.masterdata.FiscalDocumentType;
import br.gravita.core.domain.masterdata.MaxDiscountBehavior;
import br.gravita.core.domain.masterdata.PriceFormation;
import br.gravita.core.domain.masterdata.PriceTable;
import br.gravita.core.domain.masterdata.PriceTableId;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.domain.masterdata.TaxRegime;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.shared.PersonType;
import br.gravita.core.domain.tax.ItemTaxBreakdown;
import br.gravita.core.domain.tax.NfeDocument;
import br.gravita.core.domain.tax.NfeDocumentId;
import br.gravita.core.domain.tax.NfeDocumentStatus;
import br.gravita.core.domain.tax.TaxCalculationTotals;
import br.gravita.core.domain.tax.TaxType;
import br.gravita.core.ports.inbound.masterdata.AllocateDocumentNumberCommand;
import br.gravita.core.ports.inbound.masterdata.AllocateDocumentNumberUseCase;
import br.gravita.core.ports.inbound.tax.CalculateTaxCommand;
import br.gravita.core.ports.inbound.tax.CalculateTaxUseCase;
import br.gravita.core.ports.inbound.tax.IssueNfeCommand;
import br.gravita.core.ports.inbound.tax.NfeItemInput;
import br.gravita.core.ports.inbound.tax.NfeItemTaxOverrideInput;
import br.gravita.core.ports.inbound.tax.NfeRecipientInput;
import br.gravita.core.ports.inbound.tax.TaxCalculationResult;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.PriceTableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.NfeRepositoryPort;
import br.gravita.core.ports.outbound.tax.TransmissionQueuePort;
import br.gravita.core.usercases.tax.IssueNfeService;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IssueNfeServiceTest {

	private static final String NATUREZA_OPERACAO = "VENDA";

	@Mock
	private NfeRepositoryPort nfeRepositoryPort;

	@Mock
	private CompanyRepositoryPort companyRepositoryPort;

	@Mock
	private ProductRepositoryPort productRepositoryPort;

	@Mock
	private PriceTableRepositoryPort priceTableRepositoryPort;

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
		service = new IssueNfeService(nfeRepositoryPort, companyRepositoryPort, productRepositoryPort,
				priceTableRepositoryPort, calculateTaxUseCase, allocateDocumentNumberUseCase, transmissionQueuePort);

		companyId = CompanyId.of(UUID.randomUUID());
		productId = UUID.randomUUID();

		lenient().when(companyRepositoryPort.findById(companyId)).thenReturn(Optional.of(company()));
		lenient().when(productRepositoryPort.get(productId))
				.thenReturn(Optional.of(product(productId, "5102")));
		lenient().when(calculateTaxUseCase.execute(any())).thenAnswer(invocation -> {
			CalculateTaxCommand cmd = invocation.getArgument(0);
			return taxResultFor(cmd.items().size());
		});
		lenient().when(allocateDocumentNumberUseCase.execute(any())).thenReturn(new DocumentNumber("1", 42L));
		lenient().when(nfeRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void ac1_acceptsManualEntryWithNoOriginSalesOrder() {
		NfeDocument result = service.execute(command(null, NATUREZA_OPERACAO, null, List.of(item(productId, 0, null))));

		assertThat(result.getOriginSalesOrderId()).isNull();
	}

	@Test
	void ac1_acceptsAnOriginSalesOrderDrivenCallPreservingItsData() {
		UUID originSalesOrderId = UUID.randomUUID();

		NfeDocument result = service.execute(
				command(originSalesOrderId, NATUREZA_OPERACAO, null, List.of(item(productId, 0, null))));

		assertThat(result.getOriginSalesOrderId()).isEqualTo(originSalesOrderId);
	}

	@Test
	void ac2_cfopIsResolvedPerItemFromTheProductRegistryNotHardcoded() {
		UUID secondProductId = UUID.randomUUID();
		when(productRepositoryPort.get(secondProductId))
				.thenReturn(Optional.of(product(secondProductId, "5405")));

		NfeDocument result = service.execute(command(null, NATUREZA_OPERACAO, null,
				List.of(item(productId, 0, null), item(secondProductId, 0, null))));

		assertThat(result.getItems().get(0).cfop()).isEqualTo("5102");
		assertThat(result.getItems().get(1).cfop()).isEqualTo("5405");
	}

	@Test
	void ac2_aProductWithNoRegisteredCfopForTheOperationIsRejected() {
		when(productRepositoryPort.get(productId))
				.thenReturn(Optional.of(product(productId, null)));

		assertThatThrownBy(
				() -> service.execute(command(null, NATUREZA_OPERACAO, null, List.of(item(productId, 0, null)))))
				.isInstanceOf(br.gravita.core.domain.exceptions.BusinessRuleException.class);
	}

	@Test
	void ac3_anInvalidRecipientCnpjChecksDigitIsRejectedBeforeCreatingTheDocument() {
		NfeRecipientInput invalidRecipient = new NfeRecipientInput(null, "11222333000199", PersonType.COMPANY,
				"Cliente Exemplo LTDA", IeIndicator.TAXPAYER, "123456789", "SP");

		assertThatThrownBy(() -> service.execute(new IssueNfeCommand(companyId, null, NATUREZA_OPERACAO,
				invalidRecipient, List.of(item(productId, 0, null)), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
				null, null, null, null))).isInstanceOf(br.gravita.core.domain.shared.BusinessRuleException.class);

		verify(nfeRepositoryPort, never()).save(any());
	}

	@Test
	void ac3_aTaxpayerRecipientWithNoIeIsRejected() {
		NfeRecipientInput noIeRecipient = new NfeRecipientInput(null, "11222333000181", PersonType.COMPANY,
				"Cliente Exemplo LTDA", IeIndicator.TAXPAYER, null, "SP");

		assertThatThrownBy(() -> service.execute(new IssueNfeCommand(companyId, null, NATUREZA_OPERACAO, noIeRecipient,
				List.of(item(productId, 0, null)), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null,
				null))).isInstanceOf(br.gravita.core.domain.exceptions.BusinessRuleException.class);

		verify(nfeRepositoryPort, never()).save(any());
	}

	@Test
	void ac4_taxOverridesAreForwardedToTheSharedCalculateTaxUseCaseWithTheirJustification() {
		NfeItemTaxOverrideInput override = new NfeItemTaxOverrideInput(TaxType.ICMS, new BigDecimal("5.00"), "Convenio X");

		service.execute(command(null, NATUREZA_OPERACAO, null, List.of(item(productId, 0, null, List.of(override)))));

		ArgumentCaptor<CalculateTaxCommand> captor = ArgumentCaptor.forClass(CalculateTaxCommand.class);
		verify(calculateTaxUseCase).execute(captor.capture());
		assertThat(captor.getValue().overrides()).hasSize(1);
		assertThat(captor.getValue().overrides().get(0).itemIndex()).isZero();
		assertThat(captor.getValue().overrides().get(0).tax()).isEqualTo(TaxType.ICMS);
		assertThat(captor.getValue().overrides().get(0).justification()).isEqualTo("Convenio X");
	}

	@Test
	void ac5_aDiscountExceedingTheLinkedPriceTablesMaxIsRejectedWithoutAnOverrideJustification() {
		UUID priceTableId = UUID.randomUUID();
		when(priceTableRepositoryPort.findById(PriceTableId.of(priceTableId)))
				.thenReturn(Optional.of(blockingPriceTable(priceTableId, new BigDecimal("10"))));

		assertThatThrownBy(() -> service.execute(
				command(null, NATUREZA_OPERACAO, priceTableId, List.of(item(productId, 20, null)))))
				.isInstanceOf(br.gravita.core.domain.shared.BusinessRuleException.class);

		verify(nfeRepositoryPort, never()).save(any());
	}

	@Test
	void ac5_aDiscountExceedingTheMaxSucceedsWhenExplicitlyJustified() {
		UUID priceTableId = UUID.randomUUID();
		when(priceTableRepositoryPort.findById(PriceTableId.of(priceTableId)))
				.thenReturn(Optional.of(blockingPriceTable(priceTableId, new BigDecimal("10"))));

		NfeDocument result = service.execute(
				command(null, NATUREZA_OPERACAO, priceTableId, List.of(item(productId, 20, "Gerente aprovou"))));

		assertThat(result.getStatus()).isEqualTo(NfeDocumentStatus.QUEUED);
	}

	@Test
	void ac6_aReturnNatureWithNoReferencedAccessKeyIsRejected() {
		IssueNfeCommand command = new IssueNfeCommand(companyId, null, "Devolucao de compra", recipient(),
				List.of(item(productId, 0, null)), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null,
				null);
		lenient().when(productRepositoryPort.get(productId))
				.thenReturn(Optional.of(product(productId, "1202")));

		assertThatThrownBy(() -> service.execute(command))
				.isInstanceOf(br.gravita.core.domain.exceptions.BusinessRuleException.class)
				.hasMessageContaining("referencedAccessKey");
	}

	@Test
	void ac6_aReturnNatureWithAReferencedAccessKeySucceeds() {
		when(productRepositoryPort.get(productId)).thenReturn(Optional.of(product(productId, "1202")));
		IssueNfeCommand command = new IssueNfeCommand(companyId, null, "Devolucao de compra", recipient(),
				List.of(item(productId, 0, null)), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null,
				"35240111222333000181550010000012345123456789", null, null);

		NfeDocument result = service.execute(command);

		assertThat(result.getReferencedAccessKey()).isEqualTo("35240111222333000181550010000012345123456789");
	}

	@Test
	void ac7_onSuccessTheDocumentIsQueuedAndATransmissionQueueEntryIsCreated() {
		NfeDocument result = service.execute(command(null, NATUREZA_OPERACAO, null, List.of(item(productId, 0, null))));

		assertThat(result.getStatus()).isEqualTo(NfeDocumentStatus.QUEUED);
		assertThat(result.getSeries()).isEqualTo("1");
		assertThat(result.getNumber()).isEqualTo(42L);
		assertThat(result.getAccessKey()).hasSize(44);

		ArgumentCaptor<NfeDocumentId> idCaptor = ArgumentCaptor.forClass(NfeDocumentId.class);
		verify(transmissionQueuePort).enqueue(idCaptor.capture());
		assertThat(idCaptor.getValue()).isEqualTo(result.getId());
		verify(nfeRepositoryPort).save(any());
	}

	@Test
	void aNonExistentIssuerCompanyIsRejected() {
		when(companyRepositoryPort.findById(companyId)).thenReturn(Optional.empty());

		assertThatThrownBy(
				() -> service.execute(command(null, NATUREZA_OPERACAO, null, List.of(item(productId, 0, null)))))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	private IssueNfeCommand command(UUID originSalesOrderId, String naturezaOperacao, UUID priceTableId,
			List<NfeItemInput> items) {
		return new IssueNfeCommand(companyId, originSalesOrderId, naturezaOperacao, recipient(), items,
				BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null, priceTableId);
	}

	private NfeRecipientInput recipient() {
		return new NfeRecipientInput(null, "11222333000181", PersonType.COMPANY, "Cliente Exemplo LTDA",
				IeIndicator.TAXPAYER, "123456789", "SP");
	}

	private NfeItemInput item(UUID itemProductId, int discountPercent, String discountJustification) {
		return item(itemProductId, discountPercent, discountJustification, List.of());
	}

	private NfeItemInput item(UUID itemProductId, int discountPercent, String discountJustification,
			List<NfeItemTaxOverrideInput> overrides) {
		return new NfeItemInput(itemProductId, BigDecimal.TEN, new BigDecimal("15.00"),
				BigDecimal.valueOf(discountPercent), discountJustification, overrides);
	}

	private Company company() {
		return Company.of(companyId, Document.cnpj("11.222.333/0001-81"), "123456789", "987654", "6201500",
				TaxRegime.SIMPLES_NACIONAL, true, SefazEnvironment.HOMOLOGATION, "Rua Teste, 100", "SP",
				"nfe@example.com", "11999999999", null, null);
	}

	private ProductDomain product(UUID id, String cfop) {
		return ProductDomain.builder().id(id)
				.defaultCfopByOperation(cfop == null ? Map.of() : Map.of(NATUREZA_OPERACAO, cfop, "Devolucao de compra", cfop))
				.build();
	}

	private PriceTable blockingPriceTable(UUID id, BigDecimal maxDiscountPercent) {
		return PriceTable.of(PriceTableId.of(id), PriceFormation.FIXED, java.time.LocalDate.now().minusDays(1), null,
				maxDiscountPercent, MaxDiscountBehavior.BLOCK, List.of());
	}

	private TaxCalculationResult taxResultFor(int itemCount) {
		List<ItemTaxBreakdown> breakdowns = new java.util.ArrayList<>();
		for (int i = 0; i < itemCount; i++) {
			breakdowns.add(new ItemTaxBreakdown(i, "product-" + i, List.of()));
		}
		return new TaxCalculationResult(breakdowns, new TaxCalculationTotals(new EnumMap<>(TaxType.class), BigDecimal.ZERO));
	}
}
