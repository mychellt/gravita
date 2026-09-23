package br.gravita.adapters.outbound.persistence.adapters.purchasing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import br.gravita.adapters.outbound.persistence.mappers.purchasing.PurchaseOrderPersistenceMapperImpl;
import br.gravita.adapters.outbound.persistence.mappers.purchasing.PurchaseRequestPersistenceMapperImpl;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.purchasing.PurchaseOrder;
import br.gravita.core.domain.purchasing.PurchaseOrderItem;
import br.gravita.core.domain.purchasing.PurchaseOrderStatus;
import br.gravita.core.domain.purchasing.PurchaseRequest;
import br.gravita.core.domain.purchasing.PurchaseRequestId;
import br.gravita.core.domain.purchasing.PurchaseRequestItem;
import br.gravita.core.domain.purchasing.PurchaseRequestOrigin;
import br.gravita.core.domain.purchasing.PurchaseRequestStatus;
import br.gravita.core.domain.system.ApprovalAlcada;
import br.gravita.core.domain.system.ApprovalModule;
import br.gravita.core.ports.inbound.purchasing.CreatePurchaseOrderCommand;
import br.gravita.core.ports.outbound.persistence.system.ApprovalAlcadaRepositoryPort;
import br.gravita.core.usercases.purchasing.CreatePurchaseOrderService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

/**
 * Drives CreatePurchaseOrderUseCase (GRA-59) against a real H2-backed
 * repository (@DataJpaTest), proving the order and the originating request's
 * CONVERTED transition both round-trip through persistence correctly -
 * rather than just what the mocked CreatePurchaseOrderServiceTest can prove.
 */
@DataJpaTest
@ExtendWith(MockitoExtension.class)
@Import({PurchaseRequestRepositoryAdapter.class, PurchaseRequestPersistenceMapperImpl.class,
		PurchaseOrderRepositoryAdapter.class, PurchaseOrderPersistenceMapperImpl.class})
class CreatePurchaseOrderIntegrationTest {

	@Autowired
	private PurchaseRequestRepositoryAdapter purchaseRequestRepositoryAdapter;

	@Autowired
	private PurchaseOrderRepositoryAdapter purchaseOrderRepositoryAdapter;

	@Autowired
	private TestEntityManager entityManager;

	@Mock
	private ApprovalAlcadaRepositoryPort approvalAlcadaRepositoryPort;

	private CreatePurchaseOrderService service;

	@BeforeEach
	void setUp() {
		service = new CreatePurchaseOrderService(purchaseRequestRepositoryAdapter, purchaseOrderRepositoryAdapter,
				approvalAlcadaRepositoryPort);
	}

	@Test
	void creatingAnOrderPersistsItAndConvertsTheOriginatingRequest() {
		when(approvalAlcadaRepositoryPort.findByModule(ApprovalModule.PURCHASING)).thenReturn(Optional.empty());
		var requestId = persistOpenRequest();
		UUID productId = UUID.randomUUID();
		UUID supplierId = UUID.randomUUID();

		var orderId = service.execute(new CreatePurchaseOrderCommand(requestId, null, SupplierId.of(supplierId),
				List.of(new PurchaseOrderItem(productId, BigDecimal.TEN, new BigDecimal("2.50")))));
		flushAndClear();

		PurchaseOrder persistedOrder = purchaseOrderRepositoryAdapter.findById(orderId).orElseThrow();
		assertThat(persistedOrder.getRequestId()).isEqualTo(requestId);
		assertThat(persistedOrder.getSupplierId().value()).isEqualTo(supplierId);
		assertThat(persistedOrder.getStatus()).isEqualTo(PurchaseOrderStatus.OPEN);
		assertThat(persistedOrder.isApprovalRequired()).isFalse();
		assertThat(persistedOrder.getItems()).hasSize(1);
		assertThat(persistedOrder.getItems().get(0).productId()).isEqualTo(productId);
		assertThat(persistedOrder.getItems().get(0).unitPrice()).isEqualByComparingTo("2.50");

		PurchaseRequest persistedRequest = purchaseRequestRepositoryAdapter.findById(requestId).orElseThrow();
		assertThat(persistedRequest.getStatus()).isEqualTo(PurchaseRequestStatus.CONVERTED);
	}

	@Test
	void resolvesApprovalRequiredFromTheConfiguredAlcadaThreshold() {
		when(approvalAlcadaRepositoryPort.findByModule(ApprovalModule.PURCHASING)).thenReturn(Optional.of(
				ApprovalAlcada.builder().thresholdValue(new BigDecimal("10.00")).build()));
		var requestId = persistOpenRequest();

		var orderId = service.execute(new CreatePurchaseOrderCommand(requestId, null,
				SupplierId.of(UUID.randomUUID()),
				List.of(new PurchaseOrderItem(UUID.randomUUID(), BigDecimal.TEN, BigDecimal.ONE))));
		flushAndClear();

		PurchaseOrder persistedOrder = purchaseOrderRepositoryAdapter.findById(orderId).orElseThrow();
		assertThat(persistedOrder.isApprovalRequired()).isTrue();
	}

	private PurchaseRequestId persistOpenRequest() {
		var request = PurchaseRequest.open(
				PurchaseRequestId.of(UUID.randomUUID()),
				PurchaseRequestOrigin.USER,
				List.of(new PurchaseRequestItem(UUID.randomUUID(), BigDecimal.ONE)),
				UUID.randomUUID());
		return purchaseRequestRepositoryAdapter.save(request).getId();
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
