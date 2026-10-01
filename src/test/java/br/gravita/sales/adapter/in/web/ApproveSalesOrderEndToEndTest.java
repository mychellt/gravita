package br.gravita.sales.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.adapters.outbound.persistence.entities.inventory.StockBalanceJpaEntity;
import br.gravita.adapters.outbound.persistence.repositories.inventory.StockBalanceJpaRepository;
import br.gravita.core.domain.sales.Quote;
import br.gravita.core.domain.sales.QuoteId;
import br.gravita.core.domain.sales.QuoteItem;
import br.gravita.core.domain.sales.SalesOrder;
import br.gravita.core.domain.sales.SalesOrderId;
import br.gravita.core.domain.sales.SalesOrderItem;
import br.gravita.core.domain.sales.SalesOrderStatus;
import br.gravita.core.domain.system.ApprovalAlcada;
import br.gravita.core.domain.system.ApprovalModule;
import br.gravita.core.domain.system.ProfileReference;
import br.gravita.core.ports.outbound.persistence.sales.QuoteRepositoryPort;
import br.gravita.core.ports.outbound.persistence.sales.SalesOrderRepositoryPort;
import br.gravita.core.ports.outbound.persistence.system.ApprovalAlcadaRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class ApproveSalesOrderEndToEndTest {

	private static final UUID DEFAULT_WAREHOUSE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private QuoteRepositoryPort quoteRepositoryPort;

	@Autowired
	private SalesOrderRepositoryPort salesOrderRepositoryPort;

	@Autowired
	private StockBalanceJpaRepository stockBalanceJpaRepository;

	@Autowired
	private ApprovalAlcadaRepositoryPort approvalAlcadaRepositoryPort;

	@Test
	@DisplayName("Approving a draft order marks it approved and reserves stock for each item")
	void approvesADraftOrderAndReservesStockForEachItem() throws Exception {
		UUID productId = UUID.randomUUID();
		SalesOrder saved = persistDraftOrder(productId, BigDecimal.ONE, new BigDecimal("10.00"), BigDecimal.ZERO);
		seedStockBalance(productId, "50", "0");

		UUID approvedBy = UUID.randomUUID();
		mockMvc.perform(post("/api/sales/orders/" + saved.getId().value() + "/approve")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"approvedBy": "%s"}
								""".formatted(approvedBy)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("APPROVED"))
				.andExpect(jsonPath("$.approvedBy").value(approvedBy.toString()));

		SalesOrder reloaded = salesOrderRepositoryPort.findById(saved.getId()).orElseThrow();
		assertThat(reloaded.getStatus()).isEqualTo(SalesOrderStatus.APPROVED);
		assertThat(reloaded.getApprovedBy()).isEqualTo(approvedBy);

		StockBalanceJpaEntity balance = stockBalanceJpaRepository
				.findByProductIdAndWarehouseId(productId, DEFAULT_WAREHOUSE_ID).orElseThrow();
		assertThat(balance.getReserved()).isEqualByComparingTo("1");
	}

	@Test
	@DisplayName("Approving an order that is not in DRAFT is rejected")
	void rejectsApprovingAnOrderThatIsNotDraft() throws Exception {
		UUID productId = UUID.randomUUID();
		SalesOrder saved = persistDraftOrder(productId, BigDecimal.ONE, new BigDecimal("10.00"), BigDecimal.ZERO);
		seedStockBalance(productId, "50", "0");
		SalesOrder approved = saved.approve(UUID.randomUUID(), null);
		salesOrderRepositoryPort.save(approved);

		mockMvc.perform(post("/api/sales/orders/" + saved.getId().value() + "/approve")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"approvedBy": "%s"}
								""".formatted(UUID.randomUUID())))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("Approving an order above the approval limit is rejected when the approver lacks the elevated profile")
	void rejectsApprovingAnOrderExceedingTheAlcadaWhenTheApproverLacksTheElevatedProfile() throws Exception {
		UUID productId = UUID.randomUUID();
		SalesOrder saved = persistDraftOrder(productId, BigDecimal.TEN, new BigDecimal("50.00"), BigDecimal.ZERO);
		seedStockBalance(productId, "50", "0");
		approvalAlcadaRepositoryPort.save(ApprovalAlcada.configure(ApprovalModule.SALES, new BigDecimal("100.00"),
				null, new ProfileReference(UUID.randomUUID(), "Sales Manager")));

		mockMvc.perform(post("/api/sales/orders/" + saved.getId().value() + "/approve")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"approvedBy": "%s"}
								""".formatted(UUID.randomUUID())))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("Approving an order that does not exist returns 404")
	void anOrderThatDoesNotExistIsRejectedWith404() throws Exception {
		mockMvc.perform(post("/api/sales/orders/" + UUID.randomUUID() + "/approve")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"approvedBy": "%s"}
								""".formatted(UUID.randomUUID())))
				.andExpect(status().isNotFound());
	}

	private SalesOrder persistDraftOrder(UUID productId, BigDecimal quantity, BigDecimal unitPrice,
			BigDecimal discount) {
		UUID customerId = UUID.randomUUID();
		UUID salespersonId = UUID.randomUUID();
		Quote quote = Quote.create(QuoteId.of(UUID.randomUUID()), customerId, salespersonId,
				List.of(new QuoteItem(productId, quantity, unitPrice, discount)), LocalDate.now().plusDays(5),
				LocalDate.now());
		quoteRepositoryPort.save(quote);

		SalesOrder order = SalesOrder.createFromQuote(SalesOrderId.of(UUID.randomUUID()), quote.getId(), customerId,
				salespersonId, List.of(new SalesOrderItem(productId, quantity, unitPrice, discount)));
		return salesOrderRepositoryPort.save(order);
	}

	private void seedStockBalance(UUID productId, String onHand, String reserved) {
		stockBalanceJpaRepository.save(StockBalanceJpaEntity.builder()
				.id(UUID.randomUUID())
				.productId(productId)
				.warehouseId(DEFAULT_WAREHOUSE_ID)
				.onHand(new BigDecimal(onHand))
				.reserved(new BigDecimal(reserved))
				.inTransit(BigDecimal.ZERO)
				.averageCost(new BigDecimal("9.00"))
				.build());
	}
}
