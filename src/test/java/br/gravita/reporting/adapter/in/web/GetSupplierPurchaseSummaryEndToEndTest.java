package br.gravita.reporting.adapter.in.web;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.core.domain.masterdata.SupplierId;
import br.gravita.core.domain.purchasing.InstallmentTerm;
import br.gravita.core.domain.purchasing.PurchaseOrder;
import br.gravita.core.domain.purchasing.PurchaseOrderId;
import br.gravita.core.domain.purchasing.PurchaseOrderItem;
import br.gravita.core.domain.purchasing.PurchaseOrderStatus;
import br.gravita.core.domain.purchasing.PurchaseReceipt;
import br.gravita.core.domain.purchasing.PurchaseReceiptId;
import br.gravita.core.domain.purchasing.PurchaseReceiptItem;
import br.gravita.core.domain.purchasing.PurchaseRequestId;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseOrderRepositoryPort;
import br.gravita.core.ports.outbound.persistence.purchasing.PurchaseReceiptRepositoryPort;
import br.gravita.core.ports.outbound.security.SessionStorePort;
import br.gravita.core.usercases.system.CheckPermissionUseCase;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Runs the real controller, service and read-model adapter over the real repositories; only session and permission are stubbed. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class GetSupplierPurchaseSummaryEndToEndTest {

	private static final UUID PRODUCT = UUID.randomUUID();

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PurchaseOrderRepositoryPort purchaseOrderRepositoryPort;

	@Autowired
	private PurchaseReceiptRepositoryPort purchaseReceiptRepositoryPort;

	@Autowired
	private EntityManager entityManager;

	@MockitoBean
	private SessionStorePort sessionStorePort;

	@MockitoBean
	private CheckPermissionUseCase checkPermissionUseCase;

	private final UUID acme = UUID.fromString("00000000-0000-0000-0000-00000000000a");
	private final UUID bolt = UUID.fromString("00000000-0000-0000-0000-00000000000b");

	@BeforeEach
	void seed() {
		UserId caller = UserId.generate();
		when(sessionStorePort.resolve("valid-token")).thenReturn(Optional.of(caller));
		when(checkPermissionUseCase.execute(argThat(
				query -> query != null && query.userId().equals(caller) && query.module().equals("reporting")
						&& query.screen().equals("purchases-by-supplier")))).thenReturn(true);

		// Acme: two March orders, 10 x 5.00 delivered 7 days later and 4 x 2.50 delivered 4 days later.
		PurchaseOrderId first = order(acme, "10", "5.00", false, PurchaseOrderStatus.OPEN, "2019-03-05T10:00:00");
		confirmedReceipt(first, "2019-03-12T10:00:00");
		PurchaseOrderId second = order(acme, "4", "2.50", false, PurchaseOrderStatus.OPEN, "2019-03-10T10:00:00");
		confirmedReceipt(second, "2019-03-14T10:00:00");
		// A receipt still waiting for its conference is not a delivery.
		pendingReceipt(second, "2019-03-30T10:00:00");
		// Bolt: one March order, nothing received yet.
		order(bolt, "1", "100.00", false, PurchaseOrderStatus.OPEN, "2019-03-20T10:00:00");
		// Not committed purchases: cancelled, awaiting approval, and placed in another month.
		order(acme, "1", "1000.00", false, PurchaseOrderStatus.CANCELLED, "2019-03-21T10:00:00");
		order(acme, "1", "1000.00", true, PurchaseOrderStatus.OPEN, "2019-03-22T10:00:00");
		order(acme, "1", "1000.00", false, PurchaseOrderStatus.OPEN, "2019-04-01T10:00:00");
		entityManager.clear();
	}

	@Test
	void summarisesTheMonthPerSupplierRankedByValue() throws Exception {
		mockMvc.perform(get("/api/reports/purchases-by-supplier").param("period", "2019-03")
				.header("Authorization", "Bearer valid-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].supplier").value(bolt.toString()))
				.andExpect(jsonPath("$[0].volume").value(1.0))
				.andExpect(jsonPath("$[0].value").value(100.0))
				.andExpect(jsonPath("$[0].averageLeadTimeDays").value(nullValue()))
				.andExpect(jsonPath("$[1].supplier").value(acme.toString()))
				.andExpect(jsonPath("$[1].volume").value(14.0))
				.andExpect(jsonPath("$[1].value").value(60.0))
				.andExpect(jsonPath("$[1].averageLeadTimeDays").value(5.5));
	}

	@Test
	void onlyReportsTheOrdersPlacedInTheRequestedMonth() throws Exception {
		// The 2019-04-01 order is the only one placed in April, and nothing was received for it.
		mockMvc.perform(get("/api/reports/purchases-by-supplier").param("period", "2019-04")
				.header("Authorization", "Bearer valid-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].supplier").value(acme.toString()))
				.andExpect(jsonPath("$[0].value").value(1000.0))
				.andExpect(jsonPath("$[0].averageLeadTimeDays").value(nullValue()));
	}

	@Test
	void answersAnEmptyListForAMonthWithoutPurchases() throws Exception {
		mockMvc.perform(get("/api/reports/purchases-by-supplier").param("period", "2019-02")
				.header("Authorization", "Bearer valid-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void answersBadRequestWhenThePeriodIsMissingOrMalformed() throws Exception {
		mockMvc.perform(get("/api/reports/purchases-by-supplier").header("Authorization", "Bearer valid-token"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/reports/purchases-by-supplier").param("period", "March")
				.header("Authorization", "Bearer valid-token")).andExpect(status().isBadRequest());
	}

	@Test
	void answersForbiddenWhenTheProfileCannotViewTheReport() throws Exception {
		when(sessionStorePort.resolve("other-token")).thenReturn(Optional.of(UserId.generate()));

		mockMvc.perform(get("/api/reports/purchases-by-supplier").param("period", "2019-03")
				.header("Authorization", "Bearer other-token")).andExpect(status().isForbidden());
	}

	@Test
	void answersUnauthorizedWithoutASession() throws Exception {
		mockMvc.perform(get("/api/reports/purchases-by-supplier").param("period", "2019-03"))
				.andExpect(status().isUnauthorized());
	}

	private PurchaseOrderId order(UUID supplier, String quantity, String unitPrice, boolean approvalRequired,
			PurchaseOrderStatus status, String createdAt) {
		PurchaseOrder order = PurchaseOrder.of(PurchaseOrderId.of(UUID.randomUUID()),
				PurchaseRequestId.of(UUID.randomUUID()), null, SupplierId.of(supplier),
				List.of(new PurchaseOrderItem(PRODUCT, new BigDecimal(quantity), new BigDecimal(unitPrice))),
				approvalRequired, status);
		PurchaseOrderId id = purchaseOrderRepositoryPort.save(order).getId();
		backdate("purchase_orders", "created_at", id.value(), createdAt);
		return id;
	}

	private void confirmedReceipt(PurchaseOrderId orderId, String confirmedAt) {
		PurchaseReceipt receipt = pending(orderId).completeConference(
				List.of(new InstallmentTerm(new BigDecimal("50.00"), LocalDate.now().plusDays(30)))).confirm();
		backdate("purchase_receipts", "modified_at", purchaseReceiptRepositoryPort.save(receipt).getId().value(),
				confirmedAt);
	}

	private void pendingReceipt(PurchaseOrderId orderId, String modifiedAt) {
		backdate("purchase_receipts", "modified_at", purchaseReceiptRepositoryPort.save(pending(orderId)).getId().value(),
				modifiedAt);
	}

	private PurchaseReceipt pending(PurchaseOrderId orderId) {
		return PurchaseReceipt.pending(PurchaseReceiptId.of(UUID.randomUUID()), orderId,
				List.of(new PurchaseReceiptItem(PRODUCT, BigDecimal.TEN, BigDecimal.TEN)));
	}

	/** The timestamps are set by the persistence layer when saving, so the test moves them to the day it wants. */
	private void backdate(String table, String column, UUID id, String at) {
		entityManager.flush();
		entityManager.createNativeQuery("update " + table + " set " + column + " = ?1 where id = ?2")
				.setParameter(1, Timestamp.valueOf(LocalDateTime.parse(at))).setParameter(2, id).executeUpdate();
	}
}
