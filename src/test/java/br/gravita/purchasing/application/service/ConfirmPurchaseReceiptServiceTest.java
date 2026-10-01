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
import br.gravita.core.domain.purchasing.PurchaseOrderStatus;
import br.gravita.core.domain.purchasing.PurchaseReceipt;
import br.gravita.core.domain.purchasing.PurchaseReceiptId;
import br.gravita.core.domain.purchasing.PurchaseReceiptItem;
import br.gravita.core.domain.purchasing.PurchaseReceiptNotFoundException;
import br.gravita.core.domain.purchasing.PurchaseRequestId;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.purchasing.ConfirmPurchaseReceiptCommand;
import br.gravita.core.ports.outbound.persistence.purchasing.GeneratePayableFromReceiptPort;
import br.gravita.core.ports.outbound.persistence.purchasing.GeneratePayableFromReceiptPort.GeneratePayableFromReceiptCommand;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseOrderRepositoryPort;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseReceiptRepositoryPort;
import br.gravita.core.ports.outbound.persistence.purchasing.RegisterStockEntryPort;
import br.gravita.core.ports.outbound.persistence.purchasing.RegisterStockEntryPort.RegisterStockEntryCommand;
import br.gravita.core.usercases.purchasing.ConfirmPurchaseReceiptService;
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
class ConfirmPurchaseReceiptServiceTest {

	@Mock
	private PurchaseReceiptRepositoryPort purchaseReceiptRepositoryPort;

	@Mock
	private PurchaseOrderRepositoryPort purchaseOrderRepositoryPort;

	@Mock
	private RegisterStockEntryPort registerStockEntryPort;

	@Mock
	private GeneratePayableFromReceiptPort generatePayableFromReceiptPort;

	private ConfirmPurchaseReceiptService service;

	private final UUID productId = UUID.randomUUID();
	private final SupplierId supplierId = SupplierId.of(UUID.randomUUID());

	@BeforeEach
	void setUp() {
		service = new ConfirmPurchaseReceiptService(purchaseReceiptRepositoryPort, purchaseOrderRepositoryPort,
				registerStockEntryPort, generatePayableFromReceiptPort);
	}

	@Test
	@DisplayName("Confirming a receipt that fully covers the order registers stock, generates payables and closes the order")
	void confirmingARequestFullyCoveringTheOrderRegistersStockGeneratesPayablesAndClosesTheOrder() {
		PurchaseOrder order = openOrder(BigDecimal.TEN);
		PurchaseReceiptId receiptId = conferencedReceipt(order.getId(), BigDecimal.TEN, "100.00");
		when(purchaseOrderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));
		when(purchaseReceiptRepositoryPort.findByOrderId(order.getId())).thenReturn(List.of());
		when(purchaseOrderRepositoryPort.save(any(PurchaseOrder.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(new ConfirmPurchaseReceiptCommand(receiptId));

		ArgumentCaptor<RegisterStockEntryCommand> stockEntry = ArgumentCaptor.forClass(RegisterStockEntryCommand.class);
		verify(registerStockEntryPort, times(1)).registerEntry(stockEntry.capture());
		assertThat(stockEntry.getValue().productId()).isEqualTo(productId);
		assertThat(stockEntry.getValue().quantity()).isEqualByComparingTo(BigDecimal.TEN);
		assertThat(stockEntry.getValue().unitCost()).isEqualByComparingTo("5.00");

		ArgumentCaptor<GeneratePayableFromReceiptCommand> payables =
				ArgumentCaptor.forClass(GeneratePayableFromReceiptCommand.class);
		verify(generatePayableFromReceiptPort).generatePayables(payables.capture());
		assertThat(payables.getValue().supplierId()).isEqualTo(supplierId.value());
		assertThat(payables.getValue().installments()).hasSize(1);
		assertThat(payables.getValue().installments().get(0).amount()).isEqualByComparingTo("100.00");

		ArgumentCaptor<PurchaseReceipt> savedReceipt = ArgumentCaptor.forClass(PurchaseReceipt.class);
		verify(purchaseReceiptRepositoryPort).save(savedReceipt.capture());
		assertThat(savedReceipt.getValue().getId()).isEqualTo(receiptId);

		ArgumentCaptor<PurchaseOrder> savedOrder = ArgumentCaptor.forClass(PurchaseOrder.class);
		verify(purchaseOrderRepositoryPort).save(savedOrder.capture());
		assertThat(savedOrder.getValue().getStatus()).isEqualTo(PurchaseOrderStatus.CLOSED);
	}

	@Test
	@DisplayName("Confirming a partial receipt leaves the order partially received")
	void confirmingAPartialReceiptLeavesTheOrderPartiallyReceived() {
		PurchaseOrder order = openOrder(BigDecimal.TEN);
		PurchaseReceiptId receiptId = conferencedReceipt(order.getId(), new BigDecimal("4"), "40.00");
		when(purchaseOrderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));
		when(purchaseReceiptRepositoryPort.findByOrderId(order.getId())).thenReturn(List.of());
		when(purchaseOrderRepositoryPort.save(any(PurchaseOrder.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(new ConfirmPurchaseReceiptCommand(receiptId));

		ArgumentCaptor<PurchaseOrder> savedOrder = ArgumentCaptor.forClass(PurchaseOrder.class);
		verify(purchaseOrderRepositoryPort).save(savedOrder.capture());
		assertThat(savedOrder.getValue().getStatus()).isEqualTo(PurchaseOrderStatus.PARTIALLY_RECEIVED);
	}

	@Test
	@DisplayName("Closes the order only when all its confirmed receipts together cover the ordered quantity")
	void closesTheOrderOnlyWhenAllOfItsConfirmedReceiptsTogetherCoverTheOrderedQuantity() {
		PurchaseOrder order = openOrder(BigDecimal.TEN);
		PurchaseReceiptId receiptId = conferencedReceipt(order.getId(), new BigDecimal("4"), "40.00");
		PurchaseReceipt earlierConfirmedReceipt = PurchaseReceipt
				.pending(PurchaseReceiptId.of(UUID.randomUUID()), order.getId(),
						List.of(new PurchaseReceiptItem(productId, BigDecimal.TEN, new BigDecimal("6"))))
				.completeConference(List.of(new InstallmentTerm(new BigDecimal("60.00"), LocalDate.now())))
				.confirm();
		when(purchaseOrderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));
		when(purchaseReceiptRepositoryPort.findByOrderId(order.getId())).thenReturn(List.of(earlierConfirmedReceipt));
		when(purchaseOrderRepositoryPort.save(any(PurchaseOrder.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(new ConfirmPurchaseReceiptCommand(receiptId));

		ArgumentCaptor<PurchaseOrder> savedOrder = ArgumentCaptor.forClass(PurchaseOrder.class);
		verify(purchaseOrderRepositoryPort).save(savedOrder.capture());
		assertThat(savedOrder.getValue().getStatus()).isEqualTo(PurchaseOrderStatus.CLOSED);
	}

	@Test
	@DisplayName("Rejects confirming a receipt still pending conference without any side effect")
	void confirmingAReceiptStillPendingConferenceIsRejectedWithoutAnySideEffect() {
		PurchaseOrder order = openOrder(BigDecimal.TEN);
		PurchaseReceipt pending = PurchaseReceipt.pending(PurchaseReceiptId.of(UUID.randomUUID()), order.getId(),
				List.of(new PurchaseReceiptItem(productId, BigDecimal.TEN, BigDecimal.TEN)));
		when(purchaseReceiptRepositoryPort.findById(pending.getId())).thenReturn(Optional.of(pending));
		when(purchaseOrderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));

		assertThatThrownBy(() -> service.execute(new ConfirmPurchaseReceiptCommand(pending.getId())))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("completed physical conference");

		verify(registerStockEntryPort, never()).registerEntry(any());
		verify(generatePayableFromReceiptPort, never()).generatePayables(any());
		verify(purchaseReceiptRepositoryPort, never()).save(any());
		verify(purchaseOrderRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Rejects confirming an already confirmed receipt without duplicating side effects")
	void confirmingAnAlreadyConfirmedReceiptIsRejectedWithoutDuplicatingSideEffects() {
		PurchaseOrder order = openOrder(BigDecimal.TEN);
		PurchaseReceipt confirmed = PurchaseReceipt
				.pending(PurchaseReceiptId.of(UUID.randomUUID()), order.getId(),
						List.of(new PurchaseReceiptItem(productId, BigDecimal.TEN, BigDecimal.TEN)))
				.completeConference(List.of(new InstallmentTerm(new BigDecimal("100.00"), LocalDate.now())))
				.confirm();
		when(purchaseReceiptRepositoryPort.findById(confirmed.getId())).thenReturn(Optional.of(confirmed));
		when(purchaseOrderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));

		assertThatThrownBy(() -> service.execute(new ConfirmPurchaseReceiptCommand(confirmed.getId())))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("already confirmed");

		verify(registerStockEntryPort, never()).registerEntry(any());
		verify(generatePayableFromReceiptPort, never()).generatePayables(any());
		verify(purchaseReceiptRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Rejects confirming a receipt that does not exist")
	void rejectsConfirmingAReceiptThatDoesNotExist() {
		PurchaseReceiptId receiptId = PurchaseReceiptId.of(UUID.randomUUID());
		when(purchaseReceiptRepositoryPort.findById(receiptId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new ConfirmPurchaseReceiptCommand(receiptId)))
				.isInstanceOf(PurchaseReceiptNotFoundException.class);
	}

	@Test
	@DisplayName("Rejects confirming a receipt whose order cannot be found")
	void rejectsConfirmingAReceiptWhoseOrderCannotBeFound() {
		PurchaseOrderId orderId = PurchaseOrderId.of(UUID.randomUUID());
		PurchaseReceipt receipt = PurchaseReceipt
				.pending(PurchaseReceiptId.of(UUID.randomUUID()), orderId,
						List.of(new PurchaseReceiptItem(productId, BigDecimal.TEN, BigDecimal.TEN)))
				.completeConference(List.of(new InstallmentTerm(new BigDecimal("100.00"), LocalDate.now())));
		when(purchaseReceiptRepositoryPort.findById(receipt.getId())).thenReturn(Optional.of(receipt));
		when(purchaseOrderRepositoryPort.findById(orderId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(new ConfirmPurchaseReceiptCommand(receipt.getId())))
				.isInstanceOf(PurchaseOrderNotFoundException.class);
	}

	private PurchaseOrder openOrder(BigDecimal orderedQty) {
		return PurchaseOrder.create(PurchaseOrderId.of(UUID.randomUUID()), PurchaseRequestId.of(UUID.randomUUID()),
				null, supplierId, List.of(new PurchaseOrderItem(productId, orderedQty, new BigDecimal("5.00"))), false);
	}

	private PurchaseReceiptId conferencedReceipt(PurchaseOrderId orderId, BigDecimal receivedQty, String amount) {
		PurchaseReceipt conferenced = PurchaseReceipt
				.pending(PurchaseReceiptId.of(UUID.randomUUID()), orderId,
						List.of(new PurchaseReceiptItem(productId, receivedQty, receivedQty)))
				.completeConference(List.of(new InstallmentTerm(new BigDecimal(amount), LocalDate.now().plusDays(30))));
		when(purchaseReceiptRepositoryPort.findById(conferenced.getId())).thenReturn(Optional.of(conferenced));
		return conferenced.getId();
	}
}
