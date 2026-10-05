package br.gravita.purchasing.application.service;

import br.gravita.core.domain.masterdata.CompanyId;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.purchasing.ConferenceResult;
import br.gravita.core.domain.purchasing.PurchaseOrder;
import br.gravita.core.domain.purchasing.PurchaseOrderId;
import br.gravita.core.domain.purchasing.PurchaseOrderItem;
import br.gravita.core.domain.purchasing.PurchaseOrderNotFoundException;
import br.gravita.core.domain.purchasing.PurchaseReceipt;
import br.gravita.core.domain.purchasing.PurchaseReceiptId;
import br.gravita.core.domain.purchasing.PurchaseReceiptItem;
import br.gravita.core.domain.purchasing.PurchaseReceiptNotFoundException;
import br.gravita.core.domain.purchasing.PurchaseReceiptStatus;
import br.gravita.core.domain.purchasing.PurchaseRequestId;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.shared.Document;
import br.gravita.core.domain.tax.InboundNfe;
import br.gravita.core.domain.tax.InboundNfeId;
import br.gravita.core.domain.tax.InboundNfeItem;
import br.gravita.core.domain.tax.InboundNfeTotals;
import br.gravita.core.ports.inbound.purchasing.ImportSupplierNfeAtReceivingCommand;
import br.gravita.core.ports.inbound.tax.ImportSupplierNfeXmlUseCase;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseOrderRepositoryPort;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseReceiptRepositoryPort;
import br.gravita.core.usercases.purchasing.ImportSupplierNfeAtReceivingService;
import java.math.BigDecimal;
import java.time.Instant;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportSupplierNfeAtReceivingServiceTest {

	@Mock
	private PurchaseOrderRepositoryPort purchaseOrderRepositoryPort;

	@Mock
	private PurchaseReceiptRepositoryPort purchaseReceiptRepositoryPort;

	@Mock
	private ImportSupplierNfeXmlUseCase importSupplierNfeXmlUseCase;

	private ImportSupplierNfeAtReceivingService service;

	private final UUID productId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		service = new ImportSupplierNfeAtReceivingService(purchaseOrderRepositoryPort, purchaseReceiptRepositoryPort,
				importSupplierNfeXmlUseCase);
	}

	@Test
	@DisplayName("Reconciles the NF-e against the order and receipt and completes the conference")
	void reconcilesAgainstTheOrderAndReceiptAndCompletesConference() {
		final PurchaseOrder order = order(new BigDecimal("10"), new BigDecimal("5.00"));
		final PurchaseReceipt receipt = pendingReceiptFor(order.getId(), new BigDecimal("10"), new BigDecimal("10"));
		final InboundNfe inboundNfe = inboundNfe(new BigDecimal("50.00"));
		when(purchaseOrderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));
		when(purchaseReceiptRepositoryPort.findById(receipt.getId())).thenReturn(Optional.of(receipt));
		when(importSupplierNfeXmlUseCase.execute(any())).thenReturn(inboundNfe);
		when(purchaseReceiptRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final ConferenceResult result = service.execute(command(order, receipt));

		assertThat(result.hasDivergences()).isFalse();
		assertThat(result.lines()).hasSize(1);
		assertThat(result.orderedValue()).isEqualByComparingTo("50.00");
		assertThat(result.invoicedValue()).isEqualByComparingTo("50.00");

		final ArgumentCaptor<PurchaseReceipt> savedCaptor = ArgumentCaptor.forClass(PurchaseReceipt.class);
		verify(purchaseReceiptRepositoryPort).save(savedCaptor.capture());
		final PurchaseReceipt saved = savedCaptor.getValue();
		assertThat(saved.getStatus()).isEqualTo(PurchaseReceiptStatus.CONFERENCE_COMPLETED);
		assertThat(saved.getInstallmentTerms()).hasSize(1);
		assertThat(saved.getInstallmentTerms().get(0).amount()).isEqualByComparingTo("50.00");
		assertThat(saved.getInstallmentTerms().get(0).dueDate()).isEqualTo(LocalDate.of(2026, 1, 15));
	}

	@Test
	@DisplayName("Flags a quantity divergence between ordered and received quantities")
	void flagsAQuantityDivergenceBetweenOrderedAndReceived() {
		final PurchaseOrder order = order(new BigDecimal("10"), new BigDecimal("5.00"));
		final PurchaseReceipt receipt = pendingReceiptFor(order.getId(), new BigDecimal("10"), new BigDecimal("8"));
		final InboundNfe inboundNfe = inboundNfe(new BigDecimal("50.00"));
		when(purchaseOrderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));
		when(purchaseReceiptRepositoryPort.findById(receipt.getId())).thenReturn(Optional.of(receipt));
		when(importSupplierNfeXmlUseCase.execute(any())).thenReturn(inboundNfe);
		when(purchaseReceiptRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final ConferenceResult result = service.execute(command(order, receipt));

		assertThat(result.hasDivergences()).isTrue();
		assertThat(result.lines().get(0).isDivergent()).isTrue();
	}

	@Test
	@DisplayName("Flags a value divergence between ordered and invoiced values")
	void flagsAValueDivergenceBetweenOrderedAndInvoiced() {
		final PurchaseOrder order = order(new BigDecimal("10"), new BigDecimal("5.00"));
		final PurchaseReceipt receipt = pendingReceiptFor(order.getId(), new BigDecimal("10"), new BigDecimal("10"));
		final InboundNfe inboundNfe = inboundNfe(new BigDecimal("45.00"));
		when(purchaseOrderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));
		when(purchaseReceiptRepositoryPort.findById(receipt.getId())).thenReturn(Optional.of(receipt));
		when(importSupplierNfeXmlUseCase.execute(any())).thenReturn(inboundNfe);
		when(purchaseReceiptRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final ConferenceResult result = service.execute(command(order, receipt));

		assertThat(result.hasDivergences()).isTrue();
		assertThat(result.lines().get(0).isDivergent()).isFalse();
	}

	@Test
	@DisplayName("Rejects the import when the order does not exist")
	void rejectsAnUnknownOrder() {
		final PurchaseOrderId orderId = PurchaseOrderId.of(UUID.randomUUID());
		when(purchaseOrderRepositoryPort.findById(orderId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new ImportSupplierNfeAtReceivingCommand(orderId,
				PurchaseReceiptId.of(UUID.randomUUID()), CompanyId.of(UUID.randomUUID()), new byte[] {1 })))
				.isInstanceOf(PurchaseOrderNotFoundException.class);
	}

	@Test
	@DisplayName("Rejects the import when the receipt does not exist")
	void rejectsAnUnknownReceipt() {
		final PurchaseOrder order = order(new BigDecimal("10"), new BigDecimal("5.00"));
		final PurchaseReceiptId receiptId = PurchaseReceiptId.of(UUID.randomUUID());
		when(purchaseOrderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));
		when(purchaseReceiptRepositoryPort.findById(receiptId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new ImportSupplierNfeAtReceivingCommand(order.getId(), receiptId,
				CompanyId.of(UUID.randomUUID()), new byte[] {1 })))
				.isInstanceOf(PurchaseReceiptNotFoundException.class);
	}

	@Test
	@DisplayName("Rejects the import when the receipt does not belong to the given order")
	void rejectsAReceiptThatDoesNotBelongToTheGivenOrder() {
		final PurchaseOrder order = order(new BigDecimal("10"), new BigDecimal("5.00"));
		final PurchaseReceipt receipt = PurchaseReceipt.pending(PurchaseReceiptId.of(UUID.randomUUID()),
				PurchaseOrderId.of(UUID.randomUUID()),
				List.of(new PurchaseReceiptItem(productId, new BigDecimal("10"), new BigDecimal("10"))));
		when(purchaseOrderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));
		when(purchaseReceiptRepositoryPort.findById(receipt.getId())).thenReturn(Optional.of(receipt));

		assertThatThrownBy(() -> service.execute(command(order, receipt))).isInstanceOf(BusinessRuleException.class);
	}

	private PurchaseOrder order(final BigDecimal quantity, final BigDecimal unitPrice) {
		return PurchaseOrder.create(PurchaseOrderId.of(UUID.randomUUID()), PurchaseRequestId.of(UUID.randomUUID()),
				null, SupplierId.of(UUID.randomUUID()), List.of(new PurchaseOrderItem(productId, quantity, unitPrice)),
				false);
	}

	private PurchaseReceipt pendingReceiptFor(final PurchaseOrderId orderId, final BigDecimal orderedQty, final BigDecimal receivedQty) {
		return PurchaseReceipt.pending(PurchaseReceiptId.of(UUID.randomUUID()), orderId,
				List.of(new PurchaseReceiptItem(productId, orderedQty, receivedQty)));
	}

	private ImportSupplierNfeAtReceivingCommand command(final PurchaseOrder order, final PurchaseReceipt receipt) {
		return new ImportSupplierNfeAtReceivingCommand(order.getId(), receipt.getId(), CompanyId.of(UUID.randomUUID()),
				new byte[] {1 });
	}

	private InboundNfe inboundNfe(final BigDecimal totalValue) {
		return InboundNfe.importedFromXml()
				.id(InboundNfeId.of(UUID.randomUUID()))
				.companyId(CompanyId.of(UUID.randomUUID()))
				.accessKey("35240111222333000181550010000012345123456789")
				.series("1")
				.number("12345")
				.supplierDocument(Document.cnpj("11222333000181"))
				.supplierName("Fornecedor Exemplo LTDA")
				.issuedAt(Instant.parse("2026-01-15T13:00:00Z"))
				.items(List.of(new InboundNfeItem("SKU-001", "Parafuso Sextavado M8", "73181500", "5102", "UN",
						BigDecimal.TEN, new BigDecimal("5.00"), totalValue, BigDecimal.ZERO, BigDecimal.ZERO,
						BigDecimal.ZERO, BigDecimal.ZERO)))
				.totals(new InboundNfeTotals(totalValue, null, null, null, null, null, null, null, null, totalValue))
				.xmlStorageRef("xml-object-ref-1")
				.build();
	}
}
