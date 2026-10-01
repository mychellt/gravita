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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

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
	@DisplayName("Responds 204 when approving an order pending approval")
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
	@DisplayName("Responds 204 when rejecting an order pending approval")
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
	@DisplayName("Responds 400 when approving an order that does not require approval")
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
	@DisplayName("Responds 404 when approving an unknown order")
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
