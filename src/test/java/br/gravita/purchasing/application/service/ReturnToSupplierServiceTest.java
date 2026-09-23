package br.gravita.purchasing.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.purchasing.InstallmentTerm;
import br.gravita.core.domain.purchasing.PurchaseOrder;
import br.gravita.core.domain.purchasing.PurchaseOrderId;
import br.gravita.core.domain.purchasing.PurchaseOrderItem;
import br.gravita.core.domain.purchasing.PurchaseOrderNotFoundException;
import br.gravita.core.domain.purchasing.PurchaseReceipt;
import br.gravita.core.domain.purchasing.PurchaseReceiptId;
import br.gravita.core.domain.purchasing.PurchaseReceiptItem;
import br.gravita.core.domain.purchasing.PurchaseReceiptNotFoundException;
import br.gravita.core.domain.purchasing.PurchaseRequestId;
import br.gravita.core.domain.purchasing.PurchaseReturn;
import br.gravita.core.domain.purchasing.PurchaseReturnId;
import br.gravita.core.domain.purchasing.PurchaseReturnItem;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.purchasing.ReturnToSupplierCommand;
import br.gravita.core.ports.inbound.purchasing.ReturnToSupplierCommand.ReturnedItem;
import br.gravita.core.ports.outbound.persistence.purchasing.IssuePurchaseReturnNfePort;
import br.gravita.core.ports.outbound.persistence.purchasing.IssuePurchaseReturnNfePort.IssuePurchaseReturnNfeCommand;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseOrderRepositoryPort;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseReceiptRepositoryPort;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseReturnRepositoryPort;
import br.gravita.core.ports.outbound.persistence.purchasing.ReversePayableFromReturnPort;
import br.gravita.core.ports.outbound.persistence.purchasing.ReversePayableFromReturnPort.ReversePayableFromReturnCommand;
import br.gravita.core.ports.outbound.persistence.purchasing.ReverseStockEntryPort;
import br.gravita.core.ports.outbound.persistence.purchasing.ReverseStockEntryPort.ReverseStockEntryCommand;
import br.gravita.core.usercases.purchasing.ReturnToSupplierService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReturnToSupplierServiceTest {

	@Mock
	private PurchaseReceiptRepositoryPort purchaseReceiptRepositoryPort;

	@Mock
	private PurchaseOrderRepositoryPort purchaseOrderRepositoryPort;

	@Mock
	private PurchaseReturnRepositoryPort purchaseReturnRepositoryPort;

	@Mock
	private ReverseStockEntryPort reverseStockEntryPort;

	@Mock
	private ReversePayableFromReturnPort reversePayableFromReturnPort;

	@Mock
	private IssuePurchaseReturnNfePort issuePurchaseReturnNfePort;

	private ReturnToSupplierService service;

	private final UUID productId = UUID.randomUUID();
	private final SupplierId supplierId = SupplierId.of(UUID.randomUUID());

	@BeforeEach
	void setUp() {
		service = new ReturnToSupplierService(purchaseReceiptRepositoryPort, purchaseOrderRepositoryPort,
				purchaseReturnRepositoryPort, reverseStockEntryPort, reversePayableFromReturnPort,
				issuePurchaseReturnNfePort);
	}

	@Test
	void returningEveryReceivedItemInFullReversesStockAndPayableAndIssuesTheReturnNfe() {
		PurchaseOrder order = openOrder(BigDecimal.TEN, "5.00");
		PurchaseReceipt receipt = confirmedReceipt(order.getId(), BigDecimal.TEN);
		when(purchaseOrderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));
		when(purchaseReturnRepositoryPort.findByReceiptId(receipt.getId())).thenReturn(List.of());
		when(purchaseReturnRepositoryPort.save(any(PurchaseReturn.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		when(issuePurchaseReturnNfePort.issueReturnNfe(any())).thenReturn("35250000000000000000000000000000000000000000");

		PurchaseReturnId returnId = service.execute(
				new ReturnToSupplierCommand(receipt.getId(), List.of(new ReturnedItem(productId, BigDecimal.TEN))));

		assertThat(returnId).isNotNull();

		ArgumentCaptor<ReverseStockEntryCommand> stockReversal = ArgumentCaptor.forClass(ReverseStockEntryCommand.class);
		verify(reverseStockEntryPort, times(1)).reverseEntry(stockReversal.capture());
		assertThat(stockReversal.getValue().productId()).isEqualTo(productId);
		assertThat(stockReversal.getValue().quantity()).isEqualByComparingTo(BigDecimal.TEN);
		assertThat(stockReversal.getValue().unitCost()).isEqualByComparingTo("5.00");

		ArgumentCaptor<ReversePayableFromReturnCommand> payableReversal =
				ArgumentCaptor.forClass(ReversePayableFromReturnCommand.class);
		verify(reversePayableFromReturnPort).reversePayable(payableReversal.capture());
		assertThat(payableReversal.getValue().supplierId()).isEqualTo(supplierId.value());
		assertThat(payableReversal.getValue().amount()).isEqualByComparingTo("50.00");

		ArgumentCaptor<IssuePurchaseReturnNfeCommand> nfeCommand =
				ArgumentCaptor.forClass(IssuePurchaseReturnNfeCommand.class);
		verify(issuePurchaseReturnNfePort).issueReturnNfe(nfeCommand.capture());
		assertThat(nfeCommand.getValue().supplierId()).isEqualTo(supplierId.value());
		assertThat(nfeCommand.getValue().items()).hasSize(1);

		ArgumentCaptor<PurchaseReturn> savedReturn = ArgumentCaptor.forClass(PurchaseReturn.class);
		verify(purchaseReturnRepositoryPort).save(savedReturn.capture());
		assertThat(savedReturn.getValue().isTotal()).isTrue();
		assertThat(savedReturn.getValue().getReturnNfeRef()).isEqualTo("35250000000000000000000000000000000000000000");
	}

	@Test
	void returningFewerThanTheReceivedQuantityIsRecordedAsPartial() {
		PurchaseOrder order = openOrder(BigDecimal.TEN, "5.00");
		PurchaseReceipt receipt = confirmedReceipt(order.getId(), BigDecimal.TEN);
		when(purchaseOrderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));
		when(purchaseReturnRepositoryPort.findByReceiptId(receipt.getId())).thenReturn(List.of());
		when(purchaseReturnRepositoryPort.save(any(PurchaseReturn.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(new ReturnToSupplierCommand(receipt.getId(),
				List.of(new ReturnedItem(productId, new BigDecimal("4")))));

		ArgumentCaptor<PurchaseReturn> savedReturn = ArgumentCaptor.forClass(PurchaseReturn.class);
		verify(purchaseReturnRepositoryPort).save(savedReturn.capture());
		assertThat(savedReturn.getValue().isTotal()).isFalse();

		ArgumentCaptor<ReversePayableFromReturnCommand> payableReversal =
				ArgumentCaptor.forClass(ReversePayableFromReturnCommand.class);
		verify(reversePayableFromReturnPort).reversePayable(payableReversal.capture());
		assertThat(payableReversal.getValue().amount()).isEqualByComparingTo("20.00");
	}

	@Test
	void aSecondPartialReturnThatCompletesTheReceivedQuantityIsRecordedAsTotal() {
		PurchaseOrder order = openOrder(BigDecimal.TEN, "5.00");
		PurchaseReceipt receipt = confirmedReceipt(order.getId(), BigDecimal.TEN);
		PurchaseReturn firstReturn = PurchaseReturn.forReceipt(PurchaseReturnId.of(UUID.randomUUID()), receipt,
				List.of(new PurchaseReturnItem(productId, new BigDecimal("6"))), List.of());
		when(purchaseOrderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));
		when(purchaseReturnRepositoryPort.findByReceiptId(receipt.getId())).thenReturn(List.of(firstReturn));
		when(purchaseReturnRepositoryPort.save(any(PurchaseReturn.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(new ReturnToSupplierCommand(receipt.getId(),
				List.of(new ReturnedItem(productId, new BigDecimal("4")))));

		ArgumentCaptor<PurchaseReturn> savedReturn = ArgumentCaptor.forClass(PurchaseReturn.class);
		verify(purchaseReturnRepositoryPort).save(savedReturn.capture());
		assertThat(savedReturn.getValue().isTotal()).isTrue();
	}

	@Test
	void rejectsAReturnAgainstAReceiptThatIsNotConfirmed() {
		PurchaseOrder order = openOrder(BigDecimal.TEN, "5.00");
		PurchaseReceipt pending = PurchaseReceipt.pending(PurchaseReceiptId.of(UUID.randomUUID()), order.getId(),
				List.of(new PurchaseReceiptItem(productId, BigDecimal.TEN, BigDecimal.TEN)));
		when(purchaseReceiptRepositoryPort.findById(pending.getId())).thenReturn(Optional.of(pending));
		when(purchaseOrderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));

		assertThatThrownBy(() -> service.execute(
				new ReturnToSupplierCommand(pending.getId(), List.of(new ReturnedItem(productId, BigDecimal.TEN)))))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("confirmed receipt");

		verify(reverseStockEntryPort, never()).reverseEntry(any());
		verify(reversePayableFromReturnPort, never()).reversePayable(any());
		verify(issuePurchaseReturnNfePort, never()).issueReturnNfe(any());
		verify(purchaseReturnRepositoryPort, never()).save(any());
	}

	@Test
	void rejectsAReturnQuantityExceedingWhatWasOriginallyReceivedWithoutAnySideEffect() {
		PurchaseOrder order = openOrder(BigDecimal.TEN, "5.00");
		PurchaseReceipt receipt = confirmedReceipt(order.getId(), BigDecimal.TEN);
		when(purchaseOrderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));
		when(purchaseReturnRepositoryPort.findByReceiptId(receipt.getId())).thenReturn(List.of());

		assertThatThrownBy(() -> service.execute(new ReturnToSupplierCommand(receipt.getId(),
				List.of(new ReturnedItem(productId, new BigDecimal("11"))))))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("exceeds the quantity originally received");

		verify(reverseStockEntryPort, never()).reverseEntry(any());
		verify(reversePayableFromReturnPort, never()).reversePayable(any());
		verify(purchaseReturnRepositoryPort, never()).save(any());
	}

	@Test
	void rejectsReturningMoreThanWhatRemainsAfterAPriorReturn() {
		PurchaseOrder order = openOrder(BigDecimal.TEN, "5.00");
		PurchaseReceipt receipt = confirmedReceipt(order.getId(), BigDecimal.TEN);
		PurchaseReturn firstReturn = PurchaseReturn.forReceipt(PurchaseReturnId.of(UUID.randomUUID()), receipt,
				List.of(new PurchaseReturnItem(productId, new BigDecimal("6"))), List.of());
		when(purchaseOrderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));
		when(purchaseReturnRepositoryPort.findByReceiptId(receipt.getId())).thenReturn(List.of(firstReturn));

		assertThatThrownBy(() -> service.execute(new ReturnToSupplierCommand(receipt.getId(),
				List.of(new ReturnedItem(productId, new BigDecimal("5"))))))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("exceeds the quantity originally received");

		verify(purchaseReturnRepositoryPort, never()).save(any());
	}

	@Test
	void rejectsAReturnForAReceiptThatDoesNotExist() {
		PurchaseReceiptId receiptId = PurchaseReceiptId.of(UUID.randomUUID());
		when(purchaseReceiptRepositoryPort.findById(receiptId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(
				new ReturnToSupplierCommand(receiptId, List.of(new ReturnedItem(productId, BigDecimal.ONE)))))
				.isInstanceOf(PurchaseReceiptNotFoundException.class);
	}

	@Test
	void rejectsAReturnWhoseOrderCannotBeFound() {
		PurchaseOrderId orderId = PurchaseOrderId.of(UUID.randomUUID());
		PurchaseReceipt receipt = PurchaseReceipt
				.pending(PurchaseReceiptId.of(UUID.randomUUID()), orderId,
						List.of(new PurchaseReceiptItem(productId, BigDecimal.TEN, BigDecimal.TEN)))
				.completeConference(List.of(new InstallmentTerm(new BigDecimal("100.00"), LocalDate.now())))
				.confirm();
		when(purchaseReceiptRepositoryPort.findById(receipt.getId())).thenReturn(Optional.of(receipt));
		when(purchaseOrderRepositoryPort.findById(orderId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(
				new ReturnToSupplierCommand(receipt.getId(), List.of(new ReturnedItem(productId, BigDecimal.TEN)))))
				.isInstanceOf(PurchaseOrderNotFoundException.class);
	}

	private PurchaseOrder openOrder(BigDecimal orderedQty, String unitPrice) {
		return PurchaseOrder.create(PurchaseOrderId.of(UUID.randomUUID()), PurchaseRequestId.of(UUID.randomUUID()),
				null, supplierId, List.of(new PurchaseOrderItem(productId, orderedQty, new BigDecimal(unitPrice))),
				false);
	}

	private PurchaseReceipt confirmedReceipt(PurchaseOrderId orderId, BigDecimal receivedQty) {
		PurchaseReceipt confirmed = PurchaseReceipt
				.pending(PurchaseReceiptId.of(UUID.randomUUID()), orderId,
						List.of(new PurchaseReceiptItem(productId, receivedQty, receivedQty)))
				.completeConference(List.of(new InstallmentTerm(new BigDecimal("50.00"), LocalDate.now().plusDays(30))))
				.confirm();
		when(purchaseReceiptRepositoryPort.findById(confirmed.getId())).thenReturn(Optional.of(confirmed));
		return confirmed;
	}
}
