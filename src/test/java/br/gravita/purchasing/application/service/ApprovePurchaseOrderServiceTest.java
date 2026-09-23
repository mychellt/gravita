package br.gravita.purchasing.application.service;

import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.purchasing.ApprovalDecision;
import br.gravita.core.domain.purchasing.PurchaseOrder;
import br.gravita.core.domain.purchasing.PurchaseOrderId;
import br.gravita.core.domain.purchasing.PurchaseOrderItem;
import br.gravita.core.domain.purchasing.PurchaseOrderNotFoundException;
import br.gravita.core.domain.purchasing.PurchaseOrderStatus;
import br.gravita.core.domain.purchasing.PurchaseRequestId;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.purchasing.ApprovePurchaseOrderCommand;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseOrderRepositoryPort;
import br.gravita.core.ports.outbound.purchasing.NotifyApprovalWorkflowPort;
import br.gravita.core.usercases.purchasing.ApprovePurchaseOrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovePurchaseOrderServiceTest {

	@Mock
	private PurchaseOrderRepositoryPort purchaseOrderRepositoryPort;

	@Mock
	private NotifyApprovalWorkflowPort notifyApprovalWorkflowPort;

	@InjectMocks
	private ApprovePurchaseOrderService service;

	@Test
	void approvingAnOrderPendingApprovalClearsTheFlagAndNotifies() {
		PurchaseOrderId orderId = PurchaseOrderId.of(UUID.randomUUID());
		UUID approvedBy = UUID.randomUUID();
		when(purchaseOrderRepositoryPort.findById(orderId)).thenReturn(Optional.of(pendingApprovalOrder(orderId)));
		when(purchaseOrderRepositoryPort.save(any(PurchaseOrder.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(new ApprovePurchaseOrderCommand(orderId, approvedBy, ApprovalDecision.APPROVE));

		ArgumentCaptor<PurchaseOrder> saved = ArgumentCaptor.forClass(PurchaseOrder.class);
		verify(purchaseOrderRepositoryPort).save(saved.capture());
		assertThat(saved.getValue().isApprovalRequired()).isFalse();
		assertThat(saved.getValue().getApprovedBy()).isEqualTo(approvedBy);
		assertThat(saved.getValue().getStatus()).isEqualTo(PurchaseOrderStatus.OPEN);
		verify(notifyApprovalWorkflowPort).notifyDecision(saved.getValue(), ApprovalDecision.APPROVE);
	}

	@Test
	void rejectingAnOrderPendingApprovalCancelsItAndNotifies() {
		PurchaseOrderId orderId = PurchaseOrderId.of(UUID.randomUUID());
		when(purchaseOrderRepositoryPort.findById(orderId)).thenReturn(Optional.of(pendingApprovalOrder(orderId)));
		when(purchaseOrderRepositoryPort.save(any(PurchaseOrder.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		service.execute(new ApprovePurchaseOrderCommand(orderId, UUID.randomUUID(), ApprovalDecision.REJECT));

		ArgumentCaptor<PurchaseOrder> saved = ArgumentCaptor.forClass(PurchaseOrder.class);
		verify(purchaseOrderRepositoryPort).save(saved.capture());
		assertThat(saved.getValue().getStatus()).isEqualTo(PurchaseOrderStatus.CANCELLED);
		verify(notifyApprovalWorkflowPort).notifyDecision(saved.getValue(), ApprovalDecision.REJECT);
	}

	@Test
	void rejectsAnOrderThatDoesNotExist() {
		PurchaseOrderId orderId = PurchaseOrderId.of(UUID.randomUUID());
		when(purchaseOrderRepositoryPort.findById(orderId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(
				new ApprovePurchaseOrderCommand(orderId, UUID.randomUUID(), ApprovalDecision.APPROVE)))
				.isInstanceOf(PurchaseOrderNotFoundException.class);
		verify(purchaseOrderRepositoryPort, never()).save(any());
	}

	@Test
	void rejectsApprovingAnOrderThatDoesNotRequireApproval() {
		PurchaseOrderId orderId = PurchaseOrderId.of(UUID.randomUUID());
		when(purchaseOrderRepositoryPort.findById(orderId)).thenReturn(Optional.of(create(orderId, false)));

		assertThatThrownBy(() -> service.execute(
				new ApprovePurchaseOrderCommand(orderId, UUID.randomUUID(), ApprovalDecision.APPROVE)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("does not require approval");
		verify(purchaseOrderRepositoryPort, never()).save(any());
	}

	private PurchaseOrder pendingApprovalOrder(PurchaseOrderId id) {
		return create(id, true);
	}

	private PurchaseOrder create(PurchaseOrderId id, boolean approvalRequired) {
		return PurchaseOrder.create(id, PurchaseRequestId.of(UUID.randomUUID()), null,
				SupplierId.of(UUID.randomUUID()),
				List.of(new PurchaseOrderItem(UUID.randomUUID(), BigDecimal.TEN, new BigDecimal("5.00"))),
				approvalRequired);
	}
}
