package br.gravita.purchasing.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.assertj.core.api.Assertions.assertThat;

import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.purchasing.PurchaseOrder;
import br.gravita.core.domain.purchasing.PurchaseOrderId;
import br.gravita.core.domain.purchasing.PurchaseOrderItem;
import br.gravita.core.domain.purchasing.PurchaseOrderStatus;
import br.gravita.core.domain.purchasing.PurchaseRequestId;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseOrderRepositoryPort;
import br.gravita.core.ports.outbound.purchasing.NotifyApprovalWorkflowPort;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * GRA-60: end-to-end verification of ApprovePurchaseOrderUseCase
 * (POST /api/purchasing/orders/{id}/approve) through the real HTTP stack - real
 * controller, real use case, real H2-backed repositories. The order is seeded directly
 * through its repository port since it only needs to exist, not be created through its
 * own endpoint's full request/quotation flow. {@link NotifyApprovalWorkflowPort} is mocked
 * since its only adapter delivers over real SQS/e-mail infra, out of scope for this HTTP-level
 * test - the wiring itself is covered by {@code ApprovePurchaseOrderServiceTest}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class ApprovePurchaseOrderEndToEndTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PurchaseOrderRepositoryPort purchaseOrderRepositoryPort;

	@MockitoBean
	private NotifyApprovalWorkflowPort notifyApprovalWorkflowPort;

	private final UUID productId = UUID.randomUUID();

	@Test
	void approvingAnOrderPendingApprovalSucceedsWith204() throws Exception {
		UUID orderId = seedOrder(true).value();
		UUID approvedBy = UUID.randomUUID();

		String body = """
				{
				  "approvedBy": "%s",
				  "decision": "APPROVE"
				}
				""".formatted(approvedBy);

		mockMvc.perform(post("/api/purchasing/orders/" + orderId + "/approve")
						.contentType("application/json").content(body))
				.andExpect(status().isNoContent());

		PurchaseOrder updated = purchaseOrderRepositoryPort.findById(PurchaseOrderId.of(orderId)).orElseThrow();
		assertThat(updated.isApprovalRequired()).isFalse();
		assertThat(updated.getApprovedBy()).isEqualTo(approvedBy);
		assertThat(updated.getStatus()).isEqualTo(PurchaseOrderStatus.OPEN);
	}

	@Test
	void rejectingAnOrderPendingApprovalSucceedsWith204() throws Exception {
		UUID orderId = seedOrder(true).value();

		String body = """
				{
				  "approvedBy": "%s",
				  "decision": "REJECT"
				}
				""".formatted(UUID.randomUUID());

		mockMvc.perform(post("/api/purchasing/orders/" + orderId + "/approve")
						.contentType("application/json").content(body))
				.andExpect(status().isNoContent());

		PurchaseOrder updated = purchaseOrderRepositoryPort.findById(PurchaseOrderId.of(orderId)).orElseThrow();
		assertThat(updated.getStatus()).isEqualTo(PurchaseOrderStatus.CANCELLED);
	}

	@Test
	void approvingAnOrderThatDoesNotRequireApprovalIsRejectedWith400() throws Exception {
		UUID orderId = seedOrder(false).value();

		String body = """
				{
				  "approvedBy": "%s",
				  "decision": "APPROVE"
				}
				""".formatted(UUID.randomUUID());

		mockMvc.perform(post("/api/purchasing/orders/" + orderId + "/approve")
						.contentType("application/json").content(body))
				.andExpect(status().isBadRequest());
	}

	@Test
	void approvingAnUnknownOrderIsRejectedWith404() throws Exception {
		String body = """
				{
				  "approvedBy": "%s",
				  "decision": "APPROVE"
				}
				""".formatted(UUID.randomUUID());

		mockMvc.perform(post("/api/purchasing/orders/" + UUID.randomUUID() + "/approve")
						.contentType("application/json").content(body))
				.andExpect(status().isNotFound());
	}

	private PurchaseOrderId seedOrder(boolean approvalRequired) {
		var order = PurchaseOrder.create(PurchaseOrderId.of(UUID.randomUUID()),
				PurchaseRequestId.of(UUID.randomUUID()), null, SupplierId.of(UUID.randomUUID()),
				List.of(new PurchaseOrderItem(productId, BigDecimal.TEN, new BigDecimal("5.00"))), approvalRequired);
		return purchaseOrderRepositoryPort.save(order).getId();
	}
}
