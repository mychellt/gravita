package br.gravita.tax.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.masterdata.Company;
import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.SefazEnvironment;
import br.gravita.core.domain.masterdata.Supplier;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.masterdata.SupplierProductCatalogLink;
import br.gravita.core.domain.masterdata.Address;
import br.gravita.core.domain.masterdata.TaxRegime;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.domain.tax.InboundNfeId;
import br.gravita.core.domain.tax.InboundNfeItem;
import br.gravita.core.domain.tax.InboundNfeNotFoundException;
import br.gravita.core.domain.tax.InboundNfeTotals;
import br.gravita.core.ports.inbound.tax.CalculateTaxCommand;
import br.gravita.core.ports.inbound.tax.CalculateTaxUseCase;
import br.gravita.core.ports.inbound.tax.ConfirmInboundNfeReceiptCommand;
import br.gravita.core.ports.inbound.tax.ConfirmInboundNfeReceiptCommand.ConferenceItem;
import br.gravita.core.ports.inbound.tax.TaxCalculationResult;
import br.gravita.core.ports.outbound.persistence.CompanyRepositoryPort;
import br.gravita.core.ports.outbound.persistence.SupplierProductCatalogLinkRepositoryPort;
import br.gravita.core.ports.outbound.persistence.SupplierRepositoryPort;
import br.gravita.core.ports.outbound.persistence.tax.InboundNfeRepositoryPort;
import br.gravita.core.ports.outbound.tax.NotifyPayableGeneratedPort;
import br.gravita.core.ports.outbound.tax.NotifyPayableGeneratedPort.NotifyPayableGeneratedCommand;
import br.gravita.core.ports.outbound.tax.NotifyStockEntryPort;
import br.gravita.core.ports.outbound.tax.NotifyStockEntryPort.NotifyStockEntryCommand;
import br.gravita.core.usercases.tax.ConfirmInboundNfeReceiptService;
import br.gravita.core.usercases.tax.InboundNfeResolver;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
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
class ConfirmInboundNfeReceiptServiceTest {

	@Mock
	private InboundNfeRepositoryPort inboundNfeRepositoryPort;

	@Mock
	private CompanyRepositoryPort companyRepositoryPort;

	@Mock
	private CalculateTaxUseCase calculateTaxUseCase;

	@Mock
	private NotifyStockEntryPort notifyStockEntryPort;

	@Mock
	private NotifyPayableGeneratedPort notifyPayableGeneratedPort;

	@Mock
	private SupplierRepositoryPort supplierRepositoryPort;

	@Mock
	private SupplierProductCatalogLinkRepositoryPort linkRepositoryPort;

	private ConfirmInboundNfeReceiptService service;

	private final CompanyId companyId = CompanyId.of(UUID.randomUUID());
	private final UUID productId = UUID.randomUUID();
	private final SupplierId supplierId = SupplierId.of(UUID.randomUUID());
	private final Instant previousPurchaseAt = Instant.parse("2025-12-01T12:00:00Z");
	private final Instant issuedAt = Instant.parse("2026-01-10T12:00:00Z");

	@BeforeEach
	void setUp() {
		service = new ConfirmInboundNfeReceiptService(inboundNfeRepositoryPort, companyRepositoryPort,
				calculateTaxUseCase, notifyStockEntryPort, notifyPayableGeneratedPort,
				new InboundNfeResolver(supplierRepositoryPort, linkRepositoryPort), linkRepositoryPort);
	}

	@Test
	@DisplayName("Records the three-way comparison before confirming the receipt")
	void confirmingRecordsTheThreeWayComparisonBeforeConfirmingTheReceipt() {
		final InboundNfe pending = pendingInboundNfe();
		mockLookups(pending);
		when(inboundNfeRepositoryPort.save(any(InboundNfe.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final InboundNfe result = service.execute(command(pending.getId().value(), new BigDecimal("10"), new BigDecimal("8")));

		assertThat(result.getConferenceResult()).hasSize(1);
		assertThat(result.getConferenceResult().get(0).itemRef()).isEqualTo(productId);
		assertThat(result.getConferenceResult().get(0).orderedQty()).isEqualByComparingTo("10");
		assertThat(result.getConferenceResult().get(0).receivedQty()).isEqualByComparingTo("8");
		assertThat(result.getItems().get(0).quantity()).isEqualByComparingTo("10");
	}

	@Test
	@DisplayName("Registers the stock entry immediately for what was physically received")
	void confirmingRegistersTheStockEntryImmediatelyForWhatWasPhysicallyReceived() {
		final InboundNfe pending = pendingInboundNfe();
		mockLookups(pending);
		when(inboundNfeRepositoryPort.save(any(InboundNfe.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(command(pending.getId().value(), new BigDecimal("10"), new BigDecimal("8")));

		final ArgumentCaptor<NotifyStockEntryCommand> captor = ArgumentCaptor.forClass(NotifyStockEntryCommand.class);
		verify(notifyStockEntryPort).notifyEntry(captor.capture());
		assertThat(captor.getValue().productId()).isEqualTo(productId);
		assertThat(captor.getValue().quantity()).isEqualByComparingTo("8");
		assertThat(captor.getValue().unitCost()).isEqualByComparingTo("1.5000");
		assertThat(captor.getValue().sourceInboundNfeId()).isEqualTo(pending.getId().value());
	}

	@Test
	@DisplayName("Generates a payable installment matching the NF-e's own total value")
	void confirmingGeneratesAPayableInstallmentMatchingTheNfsOwnTotalValue() {
		final InboundNfe pending = pendingInboundNfe();
		mockLookups(pending);
		when(inboundNfeRepositoryPort.save(any(InboundNfe.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(command(pending.getId().value(), new BigDecimal("10"), new BigDecimal("8")));

		final ArgumentCaptor<NotifyPayableGeneratedCommand> captor =
				ArgumentCaptor.forClass(NotifyPayableGeneratedCommand.class);
		verify(notifyPayableGeneratedPort).notifyGenerated(captor.capture());
		assertThat(captor.getValue().sourceInboundNfeId()).isEqualTo(pending.getId().value());
		assertThat(captor.getValue().supplierDocument()).isEqualTo("11222333000181");
		assertThat(captor.getValue().installments()).hasSize(1);
		assertThat(captor.getValue().installments().get(0).amount()).isEqualByComparingTo("165.00");
		assertThat(captor.getValue().installments().get(0).dueDate())
				.isEqualTo(issuedAt.atZone(ZoneOffset.UTC).toLocalDate().plusDays(30));
	}

	@Test
	@DisplayName("Computes the ICMS, PIS and COFINS credit under the company's tax regime")
	void confirmingComputesTheIcmsPisCofinsCreditUnderTheCompanysTaxRegime() {
		final InboundNfe pending = pendingInboundNfe();
		mockLookups(pending);
		when(inboundNfeRepositoryPort.save(any(InboundNfe.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(command(pending.getId().value(), new BigDecimal("10"), new BigDecimal("8")));

		final ArgumentCaptor<CalculateTaxCommand> captor = ArgumentCaptor.forClass(CalculateTaxCommand.class);
		verify(calculateTaxUseCase).execute(captor.capture());
		assertThat(captor.getValue().taxRegime()).isEqualTo(br.gravita.core.domain.tax.TaxRegime.LUCRO_PRESUMIDO);
		assertThat(captor.getValue().originState()).isEqualTo("SP");
		assertThat(captor.getValue().destinationState()).isEqualTo("SP");
		assertThat(captor.getValue().items()).hasSize(1);
		assertThat(captor.getValue().items().get(0).productRef()).isEqualTo(productId.toString());
		assertThat(captor.getValue().items().get(0).quantity()).isEqualByComparingTo("10");
	}

	@Test
	@DisplayName("Rejects confirming an inbound NF-e that does not exist")
	void rejectsConfirmingAnInboundNfeThatDoesNotExist() {
		final UUID inboundNfeId = UUID.randomUUID();
		when(inboundNfeRepositoryPort.findById(InboundNfeId.of(inboundNfeId))).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(command(inboundNfeId, BigDecimal.TEN, BigDecimal.TEN)))
				.isInstanceOf(InboundNfeNotFoundException.class);

		verify(notifyStockEntryPort, never()).notifyEntry(any());
		verify(notifyPayableGeneratedPort, never()).notifyGenerated(any());
		verify(calculateTaxUseCase, never()).execute(any());
	}

	@Test
	@DisplayName("Rejects confirming an already confirmed inbound NF-e without duplicating side effects")
	void rejectsConfirmingAnAlreadyConfirmedInboundNfeWithoutDuplicatingSideEffects() {
		final InboundNfe confirmed = pendingInboundNfe()
				.confirm(List.of(new br.gravita.core.domain.tax.InboundNfeConferenceItem(productId, BigDecimal.TEN,
						BigDecimal.TEN)));
		when(inboundNfeRepositoryPort.findById(confirmed.getId())).thenReturn(Optional.of(confirmed));

		assertThatThrownBy(() -> service.execute(command(confirmed.getId().value(), BigDecimal.TEN, BigDecimal.TEN)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("already confirmed");

		verify(notifyStockEntryPort, never()).notifyEntry(any());
		verify(notifyPayableGeneratedPort, never()).notifyGenerated(any());
		verify(calculateTaxUseCase, never()).execute(any());
		verify(inboundNfeRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("AC2: rejects the confirmation when no registered supplier matches the NF-e's CNPJ")
	void rejectsConfirmationWhenTheSupplierIsUnresolved() {
		final InboundNfe pending = pendingInboundNfe();
		when(inboundNfeRepositoryPort.findById(pending.getId())).thenReturn(Optional.of(pending));
		when(supplierRepositoryPort.findByDocument(pending.getSupplierDocument())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(command(pending.getId().value(), BigDecimal.TEN, BigDecimal.TEN)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("No registered supplier");

		verifyNothingWasConfirmed();
	}

	@Test
	@DisplayName("AC3: rejects the confirmation while any item has no resolved product")
	void rejectsConfirmationWhenAnItemHasNoResolvedProduct() {
		final InboundNfe pending = pendingInboundNfe();
		when(inboundNfeRepositoryPort.findById(pending.getId())).thenReturn(Optional.of(pending));
		when(supplierRepositoryPort.findByDocument(pending.getSupplierDocument())).thenReturn(Optional.of(supplier()));
		when(linkRepositoryPort.findBySupplierAndCodes(eq(companyId), eq(supplierId), anyCollection())).thenReturn(List.of());

		assertThatThrownBy(() -> service.execute(command(pending.getId().value(), BigDecimal.TEN, BigDecimal.TEN)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("item 1 (SKU-001)");

		verifyNothingWasConfirmed();
	}

	@Test
	@DisplayName("Still rejects a conference whose line count differs from the NF-e's item count")
	void rejectsConferenceWithTheWrongNumberOfLines() {
		final InboundNfe pending = pendingInboundNfe();
		when(inboundNfeRepositoryPort.findById(pending.getId())).thenReturn(Optional.of(pending));

		assertThatThrownBy(() -> service.execute(new ConfirmInboundNfeReceiptCommand(pending.getId().value(), List.of())))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("exactly one entry per NFe item");

		verifyNothingWasConfirmed();
	}

	@Test
	@DisplayName("Rejects a conference line received as a product other than the one linked to the item")
	void rejectsConferenceLineThatDoesNotMatchTheLinkedProduct() {
		final InboundNfe pending = pendingInboundNfe();
		when(inboundNfeRepositoryPort.findById(pending.getId())).thenReturn(Optional.of(pending));
		when(supplierRepositoryPort.findByDocument(pending.getSupplierDocument())).thenReturn(Optional.of(supplier()));
		when(linkRepositoryPort.findBySupplierAndCodes(eq(companyId), eq(supplierId), anyCollection()))
				.thenReturn(List.of(link(UUID.randomUUID(), new BigDecimal("1.2000"))));

		assertThatThrownBy(() -> service.execute(command(pending.getId().value(), BigDecimal.TEN, BigDecimal.TEN)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("product linked to it");

		verifyNothingWasConfirmed();
	}

	@Test
	@DisplayName("AC9: resolves the links inside the NF-e's own company only")
	void resolvesLinksWithinTheInboundNfesCompany() {
		final InboundNfe pending = pendingInboundNfe();
		mockLookups(pending);
		when(inboundNfeRepositoryPort.save(any(InboundNfe.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(command(pending.getId().value(), new BigDecimal("10"), new BigDecimal("8")));

		verify(linkRepositoryPort).findBySupplierAndCodes(eq(companyId), eq(supplierId), eq(List.of("SKU-001")));
	}

	@Test
	@DisplayName("AC8: confirming updates the link to this NF-e's unit value and issuance date")
	void confirmingUpdatesTheLinkToThisNfesLastPurchase() {
		final InboundNfe pending = pendingInboundNfe();
		mockLookups(pending);
		when(inboundNfeRepositoryPort.save(any(InboundNfe.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(command(pending.getId().value(), new BigDecimal("10"), new BigDecimal("8")));

		final ArgumentCaptor<SupplierProductCatalogLink> captor = ArgumentCaptor.forClass(SupplierProductCatalogLink.class);
		verify(linkRepositoryPort).save(captor.capture());
		assertThat(captor.getValue().getProductId()).isEqualTo(productId);
		assertThat(captor.getValue().getLastPurchaseUnitValue()).isEqualByComparingTo("1.5000");
		assertThat(captor.getValue().getLastPurchaseAt()).isEqualTo(issuedAt);
	}

	@Test
	@DisplayName("AC10: confirming never mutates the originally imported item values")
	void confirmingNeverMutatesTheImportedItems() {
		final InboundNfe pending = pendingInboundNfe();
		final List<InboundNfeItem> importedItems = List.copyOf(pending.getItems());
		mockLookups(pending);
		when(inboundNfeRepositoryPort.save(any(InboundNfe.class))).thenAnswer(invocation -> invocation.getArgument(0));

		final InboundNfe result = service.execute(command(pending.getId().value(), new BigDecimal("10"), new BigDecimal("8")));

		assertThat(result.getItems()).isEqualTo(importedItems);
		assertThat(pending.getItems()).isEqualTo(importedItems);
	}

	private void verifyNothingWasConfirmed() {
		verify(notifyStockEntryPort, never()).notifyEntry(any());
		verify(notifyPayableGeneratedPort, never()).notifyGenerated(any());
		verify(calculateTaxUseCase, never()).execute(any());
		verify(linkRepositoryPort, never()).save(any());
		verify(inboundNfeRepositoryPort, never()).save(any());
	}

	private void mockLookups(final InboundNfe pending) {
		when(inboundNfeRepositoryPort.findById(pending.getId())).thenReturn(Optional.of(pending));
		when(companyRepositoryPort.findById(companyId)).thenReturn(Optional.of(company()));
		when(calculateTaxUseCase.execute(any())).thenReturn(new TaxCalculationResult(List.of(), null));
		mockResolvedSupplierAndLink(pending);
	}

	private void mockResolvedSupplierAndLink(final InboundNfe pending) {
		when(supplierRepositoryPort.findByDocument(pending.getSupplierDocument())).thenReturn(Optional.of(supplier()));
		when(linkRepositoryPort.findBySupplierAndCodes(eq(companyId), eq(supplierId), anyCollection()))
				.thenReturn(List.of(link(productId, new BigDecimal("1.2000"))));
		when(linkRepositoryPort.save(any(SupplierProductCatalogLink.class))).thenAnswer(invocation -> invocation.getArgument(0));
	}

	private SupplierProductCatalogLink link(final UUID linkedProductId, final BigDecimal lastPurchaseUnitValue) {
		return SupplierProductCatalogLink.create(companyId, supplierId, "SKU-001", linkedProductId, lastPurchaseUnitValue,
				previousPurchaseAt);
	}

	private Supplier supplier() {
		return Supplier.of(supplierId, Document.cnpj("11222333000181"), "Fornecedor Exemplo LTDA",
				List.of(new Address("Rua Teste", "100", null, "Centro", "Sao Paulo", "SP", "01000-000")), List.of(), null,
				null, null, null);
	}

	private ConfirmInboundNfeReceiptCommand command(final UUID inboundNfeId, final BigDecimal orderedQty, final BigDecimal receivedQty) {
		return new ConfirmInboundNfeReceiptCommand(inboundNfeId,
				List.of(new ConferenceItem(productId, orderedQty, receivedQty)));
	}

	private InboundNfe pendingInboundNfe() {
		return InboundNfe.importedFromXml()
				.id(InboundNfeId.of(UUID.randomUUID()))
				.companyId(companyId)
				.accessKey("35240111222333000181550010000012345123456789")
				.series("1")
				.number("12345")
				.supplierDocument(Document.cnpj("11222333000181"))
				.supplierName("Fornecedor Exemplo LTDA")
				.issuedAt(issuedAt)
				.items(List.of(item()))
				.totals(totals())
				.xmlStorageRef("xml-ref-1")
				.build();
	}

	private InboundNfeItem item() {
		return new InboundNfeItem("SKU-001", "Parafuso Sextavado M8", "73181500", "5102", "UN",
				new BigDecimal("10"), new BigDecimal("1.5000"), new BigDecimal("150.00"), new BigDecimal("27.00"),
				BigDecimal.ZERO, new BigDecimal("2.48"), new BigDecimal("11.40"));
	}

	private InboundNfeTotals totals() {
		return new InboundNfeTotals(new BigDecimal("150.00"), new BigDecimal("15.00"), BigDecimal.ZERO,
				BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("27.00"), BigDecimal.ZERO, new BigDecimal("2.48"),
				new BigDecimal("11.40"), new BigDecimal("165.00"));
	}

	private Company company() {
		return Company.builder()
				.id(companyId)
				.name("Acme Ltda")
				.cnpj(Document.cnpj("11444777000161"))
				.ie("123456789")
				.im("12345")
				.cnae("4711301")
				.taxRegime(TaxRegime.LUCRO_PRESUMIDO)
				.simplesOptante(false)
				.sefazEnvironment(SefazEnvironment.HOMOLOGATION)
				.address("Rua Exemplo, 100")
				.state("SP")
				.issuingEmail("fiscal@exemplo.com")
				.phone("11999999999")
				.logoUrl(null)
				.parentCompanyId(null)
				.build();
	}
}
