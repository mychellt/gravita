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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * GRA-64: end-to-end verification of ReturnToSupplierUseCase
 * (POST /api/purchasing/receipts/{id}/return) through the real HTTP stack -
 * real controller, real use case, real H2-backed repositories. M2's
 * IssueNfeUseCase doesn't exist yet (GRA-8), so the return NF-e ref is
 * expected to be whatever the stub {@code IssuePurchaseReturnNfePort} adapter
 * returns (currently {@code null}), not an actual access key.
 */
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
	void returningAConfirmedReceiptInFullSucceedsWith201() throws Exception {
		PurchaseOrderId orderId = seedOpenOrder();
		PurchaseReceiptId receiptId = seedConfirmedReceipt(orderId, BigDecimal.TEN);

		mockMvc.perform(post("/api/purchasing/receipts/" + receiptId.value() + "/return")
						.contentType("application/json").content(returnBody("10")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").exists());
	}

	@Test
	void returningMoreThanReceivedIsRejectedWith400() throws Exception {
		PurchaseOrderId orderId = seedOpenOrder();
		PurchaseReceiptId receiptId = seedConfirmedReceipt(orderId, BigDecimal.TEN);

		mockMvc.perform(post("/api/purchasing/receipts/" + receiptId.value() + "/return")
						.contentType("application/json").content(returnBody("11")))
				.andExpect(status().isBadRequest());
	}

	@Test
	void returningAgainstAnUnknownReceiptIsRejectedWith404() throws Exception {
		mockMvc.perform(post("/api/purchasing/receipts/" + UUID.randomUUID() + "/return")
						.contentType("application/json").content(returnBody("1")))
				.andExpect(status().isNotFound());
	}

	@Test
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
