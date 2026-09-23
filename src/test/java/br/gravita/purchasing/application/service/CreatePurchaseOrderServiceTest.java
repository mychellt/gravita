package br.gravita.purchasing.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.purchasing.PurchaseOrder;
import br.gravita.core.domain.purchasing.PurchaseOrderItem;
import br.gravita.core.domain.purchasing.PurchaseOrderStatus;
import br.gravita.core.domain.purchasing.PurchaseRequest;
import br.gravita.core.domain.purchasing.PurchaseRequestId;
import br.gravita.core.domain.purchasing.PurchaseRequestItem;
import br.gravita.core.domain.purchasing.PurchaseRequestNotFoundException;
import br.gravita.core.domain.purchasing.PurchaseRequestOrigin;
import br.gravita.core.domain.purchasing.PurchaseRequestStatus;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.domain.system.ApprovalAlcada;
import br.gravita.core.domain.system.ApprovalModule;
import br.gravita.core.ports.inbound.purchasing.CreatePurchaseOrderCommand;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseOrderRepositoryPort;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseRequestRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.ApprovalAlcadaRepositoryPort;
import br.gravita.core.usercases.purchasing.CreatePurchaseOrderService;
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
class CreatePurchaseOrderServiceTest {

	@Mock
	private PurchaseRequestRepositoryPort purchaseRequestRepositoryPort;

	@Mock
	private PurchaseOrderRepositoryPort purchaseOrderRepositoryPort;

	@Mock
	private ApprovalAlcadaRepositoryPort approvalAlcadaRepositoryPort;

	private CreatePurchaseOrderService service;

	@BeforeEach
	void setUp() {
		service = new CreatePurchaseOrderService(purchaseRequestRepositoryPort, purchaseOrderRepositoryPort,
				approvalAlcadaRepositoryPort);
	}

	@Test
	void createsAnOrderFromAnOpenRequestAndConvertsIt() {
		PurchaseRequestId requestId = openRequest();
		when(purchaseOrderRepositoryPort.save(any(PurchaseOrder.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		when(approvalAlcadaRepositoryPort.findByModule(ApprovalModule.PURCHASING)).thenReturn(Optional.empty());
		SupplierId supplierId = SupplierId.of(UUID.randomUUID());
		List<PurchaseOrderItem> items = List.of(new PurchaseOrderItem(UUID.randomUUID(), BigDecimal.TEN,
				new BigDecimal("5.00")));

		var id = service.execute(new CreatePurchaseOrderCommand(requestId, null, supplierId, items));

		assertThat(id).isNotNull();
		ArgumentCaptor<PurchaseOrder> savedOrder = ArgumentCaptor.forClass(PurchaseOrder.class);
		verify(purchaseOrderRepositoryPort).save(savedOrder.capture());
		assertThat(savedOrder.getValue().getRequestId()).isEqualTo(requestId);
		assertThat(savedOrder.getValue().getSupplierId()).isEqualTo(supplierId);
		assertThat(savedOrder.getValue().getStatus()).isEqualTo(PurchaseOrderStatus.OPEN);
		assertThat(savedOrder.getValue().isApprovalRequired()).isFalse();

		ArgumentCaptor<PurchaseRequest> savedRequest = ArgumentCaptor.forClass(PurchaseRequest.class);
		verify(purchaseRequestRepositoryPort).save(savedRequest.capture());
		assertThat(savedRequest.getValue().getStatus()).isEqualTo(PurchaseRequestStatus.CONVERTED);
	}

	@Test
	void createsAnOrderFromAQuotedRequestCarryingTheQuotationId() {
		PurchaseRequestId requestId = PurchaseRequestId.of(UUID.randomUUID());
		when(purchaseRequestRepositoryPort.findById(requestId)).thenReturn(Optional.of(
				PurchaseRequest.of(requestId, PurchaseRequestOrigin.USER,
						List.of(new PurchaseRequestItem(UUID.randomUUID(), BigDecimal.ONE)), UUID.randomUUID(),
						PurchaseRequestStatus.QUOTED)));
		when(purchaseOrderRepositoryPort.save(any(PurchaseOrder.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		when(approvalAlcadaRepositoryPort.findByModule(ApprovalModule.PURCHASING)).thenReturn(Optional.empty());
		UUID quotationId = UUID.randomUUID();
		List<PurchaseOrderItem> items = List.of(new PurchaseOrderItem(UUID.randomUUID(), BigDecimal.ONE,
				BigDecimal.TEN));

		service.execute(new CreatePurchaseOrderCommand(requestId, quotationId, SupplierId.of(UUID.randomUUID()),
				items));

		ArgumentCaptor<PurchaseOrder> savedOrder = ArgumentCaptor.forClass(PurchaseOrder.class);
		verify(purchaseOrderRepositoryPort).save(savedOrder.capture());
		assertThat(savedOrder.getValue().getQuotationId()).isEqualTo(quotationId);
	}

	@Test
	void rejectsARequestThatDoesNotExist() {
		PurchaseRequestId requestId = PurchaseRequestId.of(UUID.randomUUID());
		when(purchaseRequestRepositoryPort.findById(requestId)).thenReturn(Optional.empty());
		List<PurchaseOrderItem> items = List.of(new PurchaseOrderItem(UUID.randomUUID(), BigDecimal.ONE, BigDecimal.TEN));

		assertThatThrownBy(() -> service.execute(
				new CreatePurchaseOrderCommand(requestId, null, SupplierId.of(UUID.randomUUID()), items)))
				.isInstanceOf(PurchaseRequestNotFoundException.class);
	}

	@Test
	void rejectsARequestThatIsAlreadyConverted() {
		PurchaseRequestId requestId = PurchaseRequestId.of(UUID.randomUUID());
		when(purchaseRequestRepositoryPort.findById(requestId)).thenReturn(Optional.of(
				PurchaseRequest.of(requestId, PurchaseRequestOrigin.USER,
						List.of(new PurchaseRequestItem(UUID.randomUUID(), BigDecimal.ONE)), UUID.randomUUID(),
						PurchaseRequestStatus.CONVERTED)));
		List<PurchaseOrderItem> items = List.of(new PurchaseOrderItem(UUID.randomUUID(), BigDecimal.ONE, BigDecimal.TEN));

		assertThatThrownBy(() -> service.execute(
				new CreatePurchaseOrderCommand(requestId, null, SupplierId.of(UUID.randomUUID()), items)))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("OPEN or QUOTED");
		verify(purchaseOrderRepositoryPort, never()).save(any());
	}

	@Test
	void requiresApprovalWhenTheOrderTotalMeetsTheConfiguredThreshold() {
		PurchaseRequestId requestId = openRequest();
		when(purchaseOrderRepositoryPort.save(any(PurchaseOrder.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		when(approvalAlcadaRepositoryPort.findByModule(ApprovalModule.PURCHASING)).thenReturn(Optional.of(
				ApprovalAlcada.builder().thresholdValue(new BigDecimal("100.00")).build()));
		List<PurchaseOrderItem> items = List.of(new PurchaseOrderItem(UUID.randomUUID(), BigDecimal.TEN,
				new BigDecimal("10.00")));

		service.execute(new CreatePurchaseOrderCommand(requestId, null, SupplierId.of(UUID.randomUUID()), items));

		ArgumentCaptor<PurchaseOrder> savedOrder = ArgumentCaptor.forClass(PurchaseOrder.class);
		verify(purchaseOrderRepositoryPort).save(savedOrder.capture());
		assertThat(savedOrder.getValue().isApprovalRequired()).isTrue();
	}

	@Test
	void skipsApprovalWhenTheOrderTotalIsBelowTheConfiguredThreshold() {
		PurchaseRequestId requestId = openRequest();
		when(purchaseOrderRepositoryPort.save(any(PurchaseOrder.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		when(approvalAlcadaRepositoryPort.findByModule(ApprovalModule.PURCHASING)).thenReturn(Optional.of(
				ApprovalAlcada.builder().thresholdValue(new BigDecimal("1000.00")).build()));
		List<PurchaseOrderItem> items = List.of(new PurchaseOrderItem(UUID.randomUUID(), BigDecimal.ONE,
				new BigDecimal("10.00")));

		service.execute(new CreatePurchaseOrderCommand(requestId, null, SupplierId.of(UUID.randomUUID()), items));

		ArgumentCaptor<PurchaseOrder> savedOrder = ArgumentCaptor.forClass(PurchaseOrder.class);
		verify(purchaseOrderRepositoryPort).save(savedOrder.capture());
		assertThat(savedOrder.getValue().isApprovalRequired()).isFalse();
	}

	private PurchaseRequestId openRequest() {
		PurchaseRequestId requestId = PurchaseRequestId.of(UUID.randomUUID());
		when(purchaseRequestRepositoryPort.findById(requestId)).thenReturn(Optional.of(
				PurchaseRequest.of(requestId, PurchaseRequestOrigin.USER,
						List.of(new PurchaseRequestItem(UUID.randomUUID(), BigDecimal.ONE)), UUID.randomUUID(),
						PurchaseRequestStatus.OPEN)));
		return requestId;
	}
}
