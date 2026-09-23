package br.gravita.purchasing.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.purchasing.PurchaseOrder;
import br.gravita.core.domain.purchasing.PurchaseOrderId;
import br.gravita.core.domain.purchasing.PurchaseOrderItem;
import br.gravita.core.domain.purchasing.PurchaseOrderNotFoundException;
import br.gravita.core.domain.purchasing.PurchaseReceipt;
import br.gravita.core.domain.purchasing.PurchaseReceiptItem;
import br.gravita.core.domain.purchasing.PurchaseReceiptStatus;
import br.gravita.core.domain.purchasing.PurchaseRequestId;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.purchasing.ReceivePurchaseOrderCommand;
import br.gravita.core.ports.inbound.purchasing.ReceivePurchaseOrderCommand.ReceivedItem;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseOrderRepositoryPort;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseReceiptRepositoryPort;
import br.gravita.core.usercases.purchasing.ReceivePurchaseOrderService;
import java.math.BigDecimal;
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
class ReceivePurchaseOrderServiceTest {

	@Mock
	private PurchaseOrderRepositoryPort purchaseOrderRepositoryPort;

	@Mock
	private PurchaseReceiptRepositoryPort purchaseReceiptRepositoryPort;

	private ReceivePurchaseOrderService service;

	private final UUID productId = UUID.randomUUID();
	private final SupplierId supplierId = SupplierId.of(UUID.randomUUID());

	@BeforeEach
	void setUp() {
		service = new ReceivePurchaseOrderService(purchaseOrderRepositoryPort, purchaseReceiptRepositoryPort);
	}

	@Test
	void receivingTheFullOrderedQuantityRecordsATotalReceiptPendingConference() {
		PurchaseOrder order = openOrder(BigDecimal.TEN);
		when(purchaseOrderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));
		when(purchaseReceiptRepositoryPort.save(any(PurchaseReceipt.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(new ReceivePurchaseOrderCommand(order.getId(), List.of(new ReceivedItem(productId, BigDecimal.TEN))));

		ArgumentCaptor<PurchaseReceipt> saved = ArgumentCaptor.forClass(PurchaseReceipt.class);
		verify(purchaseReceiptRepositoryPort).save(saved.capture());
		assertThat(saved.getValue().getOrderId()).isEqualTo(order.getId());
		assertThat(saved.getValue().getStatus()).isEqualTo(PurchaseReceiptStatus.PENDING_CONFERENCE);
		assertThat(saved.getValue().isTotal()).isTrue();
		assertThat(saved.getValue().getReceivedItems()).containsExactly(
				new PurchaseReceiptItem(productId, BigDecimal.TEN, BigDecimal.TEN));
	}

	@Test
	void receivingLessThanTheOrderedQuantityRecordsAPartialReceipt() {
		PurchaseOrder order = openOrder(BigDecimal.TEN);
		when(purchaseOrderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));
		when(purchaseReceiptRepositoryPort.save(any(PurchaseReceipt.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(
				new ReceivePurchaseOrderCommand(order.getId(), List.of(new ReceivedItem(productId, new BigDecimal("4")))));

		ArgumentCaptor<PurchaseReceipt> saved = ArgumentCaptor.forClass(PurchaseReceipt.class);
		verify(purchaseReceiptRepositoryPort).save(saved.capture());
		assertThat(saved.getValue().isTotal()).isFalse();
	}

	@Test
	void rejectsReceivingAgainstAnOrderThatDoesNotExist() {
		PurchaseOrderId orderId = PurchaseOrderId.of(UUID.randomUUID());
		when(purchaseOrderRepositoryPort.findById(orderId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(
				new ReceivePurchaseOrderCommand(orderId, List.of(new ReceivedItem(productId, BigDecimal.TEN)))))
				.isInstanceOf(PurchaseOrderNotFoundException.class);

		verify(purchaseReceiptRepositoryPort, never()).save(any());
	}

	@Test
	void rejectsReceivingAgainstAClosedOrder() {
		PurchaseOrder order = openOrder(BigDecimal.TEN).afterReceiptConfirmed(true);
		when(purchaseOrderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));

		assertThatThrownBy(() -> service.execute(
				new ReceivePurchaseOrderCommand(order.getId(), List.of(new ReceivedItem(productId, BigDecimal.TEN)))))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("CLOSED");

		verify(purchaseReceiptRepositoryPort, never()).save(any());
	}

	@Test
	void rejectsReceivingAgainstAnOrderPendingApproval() {
		PurchaseOrder order = PurchaseOrder.create(PurchaseOrderId.of(UUID.randomUUID()),
				PurchaseRequestId.of(UUID.randomUUID()), null, supplierId,
				List.of(new PurchaseOrderItem(productId, BigDecimal.TEN, new BigDecimal("5.00"))), true);
		when(purchaseOrderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));

		assertThatThrownBy(() -> service.execute(
				new ReceivePurchaseOrderCommand(order.getId(), List.of(new ReceivedItem(productId, BigDecimal.TEN)))))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("pending approval");

		verify(purchaseReceiptRepositoryPort, never()).save(any());
	}

	@Test
	void rejectsAReceivedItemForAProductNotPartOfTheOrder() {
		PurchaseOrder order = openOrder(BigDecimal.TEN);
		when(purchaseOrderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));
		UUID unknownProductId = UUID.randomUUID();

		assertThatThrownBy(() -> service.execute(new ReceivePurchaseOrderCommand(order.getId(),
				List.of(new ReceivedItem(unknownProductId, BigDecimal.TEN)))))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("not part of order");

		verify(purchaseReceiptRepositoryPort, never()).save(any());
	}

	private PurchaseOrder openOrder(BigDecimal orderedQty) {
		return PurchaseOrder.create(PurchaseOrderId.of(UUID.randomUUID()), PurchaseRequestId.of(UUID.randomUUID()),
				null, supplierId, List.of(new PurchaseOrderItem(productId, orderedQty, new BigDecimal("5.00"))), false);
	}
}
