package br.gravita.purchasing.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.purchasing.PurchaseOrder;
import br.gravita.core.domain.purchasing.PurchaseOrderId;
import br.gravita.core.domain.purchasing.PurchaseOrderItem;
import br.gravita.core.domain.purchasing.PurchaseRequestId;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseOrderRepositoryPort;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class ReceivePurchaseOrderEndToEndTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PurchaseOrderRepositoryPort purchaseOrderRepositoryPort;

	private final UUID productId = UUID.randomUUID();

	@Test
	void receivingTheFullOrderedQuantitySucceedsWith201() throws Exception {
		UUID orderId = seedOpenOrder(BigDecimal.TEN, false).value();

		String body = """
				{
				  "receivedItems": [{"productId": "%s", "receivedQty": 10}]
				}
				""".formatted(productId);

		mockMvc.perform(post("/api/purchasing/orders/" + orderId + "/receipts")
						.contentType("application/json").content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").exists());
	}

	@Test
	void receivingAgainstAnUnknownOrderIsRejectedWith404() throws Exception {
		String body = """
				{
				  "receivedItems": [{"productId": "%s", "receivedQty": 10}]
				}
				""".formatted(productId);

		mockMvc.perform(post("/api/purchasing/orders/" + UUID.randomUUID() + "/receipts")
						.contentType("application/json").content(body))
				.andExpect(status().isNotFound());
	}

	@Test
	void receivingAgainstAnOrderPendingApprovalIsRejectedWith400() throws Exception {
		UUID orderId = seedOpenOrder(BigDecimal.TEN, true).value();

		String body = """
				{
				  "receivedItems": [{"productId": "%s", "receivedQty": 10}]
				}
				""".formatted(productId);

		mockMvc.perform(post("/api/purchasing/orders/" + orderId + "/receipts")
						.contentType("application/json").content(body))
				.andExpect(status().isBadRequest());
	}

	@Test
	void anEmptyReceivedItemListIsRejectedWith400() throws Exception {
		UUID orderId = seedOpenOrder(BigDecimal.TEN, false).value();

		String body = """
				{
				  "receivedItems": []
				}
				""";

		mockMvc.perform(post("/api/purchasing/orders/" + orderId + "/receipts")
						.contentType("application/json").content(body))
				.andExpect(status().isBadRequest());
	}

	private PurchaseOrderId seedOpenOrder(BigDecimal orderedQty, boolean approvalRequired) {
		var order = PurchaseOrder.create(PurchaseOrderId.of(UUID.randomUUID()),
				PurchaseRequestId.of(UUID.randomUUID()), null, SupplierId.of(UUID.randomUUID()),
				List.of(new PurchaseOrderItem(productId, orderedQty, new BigDecimal("5.00"))), approvalRequired);
		return purchaseOrderRepositoryPort.save(order).getId();
	}
}
