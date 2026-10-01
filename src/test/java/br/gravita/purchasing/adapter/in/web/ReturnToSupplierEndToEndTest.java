package br.gravita.purchasing.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.purchasing.InstallmentTerm;
import br.gravita.core.domain.purchasing.PurchaseOrder;
import br.gravita.core.domain.purchasing.PurchaseOrderId;
import br.gravita.core.domain.purchasing.PurchaseOrderItem;
import br.gravita.core.domain.purchasing.PurchaseReceipt;
import br.gravita.core.domain.purchasing.PurchaseReceiptId;
import br.gravita.core.domain.purchasing.PurchaseReceiptItem;
import br.gravita.core.domain.purchasing.PurchaseRequestId;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseOrderRepositoryPort;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseReceiptRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class ReturnToSupplierEndToEndTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PurchaseOrderRepositoryPort purchaseOrderRepositoryPort;

	@Autowired
	private PurchaseReceiptRepositoryPort purchaseReceiptRepositoryPort;

	private final UUID productId = UUID.randomUUID();

	@Test
	@DisplayName("Responds 201 when a confirmed receipt is returned in full")
	void returningAConfirmedReceiptInFullSucceedsWith201() throws Exception {
		PurchaseOrderId orderId = seedOpenOrder();
		PurchaseReceiptId receiptId = seedConfirmedReceipt(orderId, BigDecimal.TEN);

		mockMvc.perform(post("/api/purchasing/receipts/" + receiptId.value() + "/return")
						.contentType("application/json").content(returnBody("10")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").exists());
	}

	@Test
	@DisplayName("Responds 400 when returning more than was received")
	void returningMoreThanReceivedIsRejectedWith400() throws Exception {
		PurchaseOrderId orderId = seedOpenOrder();
		PurchaseReceiptId receiptId = seedConfirmedReceipt(orderId, BigDecimal.TEN);

		mockMvc.perform(post("/api/purchasing/receipts/" + receiptId.value() + "/return")
						.contentType("application/json").content(returnBody("11")))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("Responds 404 when returning against an unknown receipt")
	void returningAgainstAnUnknownReceiptIsRejectedWith404() throws Exception {
		mockMvc.perform(post("/api/purchasing/receipts/" + UUID.randomUUID() + "/return")
						.contentType("application/json").content(returnBody("1")))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("Responds 400 when returning a receipt still pending conference")
	void returningAReceiptStillPendingConferenceIsRejectedWith400() throws Exception {
		PurchaseOrderId orderId = seedOpenOrder();
		var pending = PurchaseReceipt.pending(PurchaseReceiptId.of(UUID.randomUUID()), orderId,
				List.of(new PurchaseReceiptItem(productId, BigDecimal.TEN, BigDecimal.TEN)));
		PurchaseReceiptId receiptId = purchaseReceiptRepositoryPort.save(pending).getId();

		mockMvc.perform(post("/api/purchasing/receipts/" + receiptId.value() + "/return")
						.contentType("application/json").content(returnBody("10")))
				.andExpect(status().isBadRequest());
	}

	private String returnBody(String quantity) {
		return """
				{
				  "items": [
				    {"productId": "%s", "quantity": %s}
				  ]
				}
				""".formatted(productId, quantity);
	}

	private PurchaseOrderId seedOpenOrder() {
		var order = PurchaseOrder.create(PurchaseOrderId.of(UUID.randomUUID()),
				PurchaseRequestId.of(UUID.randomUUID()), null, SupplierId.of(UUID.randomUUID()),
				List.of(new PurchaseOrderItem(productId, BigDecimal.TEN, new BigDecimal("5.00"))), false);
		return purchaseOrderRepositoryPort.save(order).getId();
	}

	private PurchaseReceiptId seedConfirmedReceipt(PurchaseOrderId orderId, BigDecimal receivedQty) {
		var receipt = PurchaseReceipt
				.pending(PurchaseReceiptId.of(UUID.randomUUID()), orderId,
						List.of(new PurchaseReceiptItem(productId, receivedQty, receivedQty)))
				.completeConference(List.of(new InstallmentTerm(new BigDecimal("50.00"), LocalDate.now().plusDays(30))))
				.confirm();
		return purchaseReceiptRepositoryPort.save(receipt).getId();
	}
}
