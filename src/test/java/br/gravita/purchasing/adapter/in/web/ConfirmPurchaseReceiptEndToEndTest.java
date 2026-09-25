package br.gravita.purchasing.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.core.domain.ProductDomain;
import br.gravita.core.domain.ProductStatus;
import br.gravita.core.domain.ProductType;
import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.purchasing.InstallmentTerm;
import br.gravita.core.domain.purchasing.PurchaseOrder;
import br.gravita.core.domain.purchasing.PurchaseOrderId;
import br.gravita.core.domain.purchasing.PurchaseOrderItem;
import br.gravita.core.domain.purchasing.PurchaseReceipt;
import br.gravita.core.domain.purchasing.PurchaseReceiptId;
import br.gravita.core.domain.purchasing.PurchaseReceiptItem;
import br.gravita.core.domain.purchasing.PurchaseRequestId;
import br.gravita.core.ports.outbound.persistence.ProductRepositoryPort;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseOrderRepositoryPort;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseReceiptRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * GRA-63: end-to-end verification of ConfirmPurchaseReceiptUseCase
 * (POST /api/purchasing/receipts/{id}/confirm) through the real HTTP stack -
 * real controller, real use case, real H2-backed repositories. The receipt is
 * seeded directly through its repository port (already past physical
 * conference) rather than through ReceivePurchaseOrderUseCase's endpoint,
 * since this test only cares about the confirm step.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class ConfirmPurchaseReceiptEndToEndTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PurchaseOrderRepositoryPort purchaseOrderRepositoryPort;

	@Autowired
	private PurchaseReceiptRepositoryPort purchaseReceiptRepositoryPort;

	@Autowired
	private ProductRepositoryPort productRepositoryPort;

	private final UUID productId = UUID.randomUUID();

	/**
	 * Confirming now calls M5's real RegisterStockEntryUseCase (GRA-82), which
	 * requires the product to exist in masterdata - every test that confirms a
	 * receipt for {@link #productId} needs it seeded first.
	 */
	@BeforeEach
	void seedProduct() {
		productRepositoryPort.save(ProductDomain.builder()
				.id(productId)
				.internalCode("SKU-" + productId)
				.type(ProductType.SIMPLE)
				.status(ProductStatus.ACTIVE)
				.lotControl(false)
				.serialControl(false)
				.build());
	}

	@Test
	void confirmingAConferencedReceiptSucceedsWith204() throws Exception {
		PurchaseOrderId orderId = seedOpenOrder();
		PurchaseReceiptId receiptId = seedConferencedReceipt(orderId, BigDecimal.TEN);

		mockMvc.perform(post("/api/purchasing/receipts/" + receiptId.value() + "/confirm"))
				.andExpect(status().isNoContent());
	}

	/**
	 * GRA-82: confirming now calls M5's real RegisterStockEntryUseCase (no
	 * longer the GRA-63 no-op stub), so the receipt's product balance is
	 * actually updated in inventory.
	 */
	@Test
	void confirmingAReceiptRegistersARealStockEntryInInventory() throws Exception {
		PurchaseOrderId orderId = seedOpenOrder();
		PurchaseReceiptId receiptId = seedConferencedReceipt(orderId, BigDecimal.TEN);

		mockMvc.perform(post("/api/purchasing/receipts/" + receiptId.value() + "/confirm"))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/inventory/products/" + productId + "/balance"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.onHand").value(10))
				.andExpect(jsonPath("$.averageCost").value(5.00));
	}

	@Test
	void confirmingAnUnknownReceiptIsRejectedWith404() throws Exception {
		mockMvc.perform(post("/api/purchasing/receipts/" + UUID.randomUUID() + "/confirm"))
				.andExpect(status().isNotFound());
	}

	@Test
	void confirmingTheSameReceiptTwiceIsRejectedWith400OnTheSecondAttempt() throws Exception {
		PurchaseOrderId orderId = seedOpenOrder();
		PurchaseReceiptId receiptId = seedConferencedReceipt(orderId, BigDecimal.TEN);

		mockMvc.perform(post("/api/purchasing/receipts/" + receiptId.value() + "/confirm"))
				.andExpect(status().isNoContent());
		mockMvc.perform(post("/api/purchasing/receipts/" + receiptId.value() + "/confirm"))
				.andExpect(status().isBadRequest());
	}

	private PurchaseOrderId seedOpenOrder() {
		var order = PurchaseOrder.create(PurchaseOrderId.of(UUID.randomUUID()),
				PurchaseRequestId.of(UUID.randomUUID()), null, SupplierId.of(UUID.randomUUID()),
				List.of(new PurchaseOrderItem(productId, BigDecimal.TEN, new BigDecimal("5.00"))), false);
		return purchaseOrderRepositoryPort.save(order).getId();
	}

	private PurchaseReceiptId seedConferencedReceipt(PurchaseOrderId orderId, BigDecimal receivedQty) {
		var receipt = PurchaseReceipt
				.pending(PurchaseReceiptId.of(UUID.randomUUID()), orderId,
						List.of(new PurchaseReceiptItem(productId, receivedQty, receivedQty)))
				.completeConference(List.of(new InstallmentTerm(new BigDecimal("50.00"), LocalDate.now().plusDays(30))));
		return purchaseReceiptRepositoryPort.save(receipt).getId();
	}
}
