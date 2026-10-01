package br.gravita.sales.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.adapters.outbound.persistence.entities.sales.CommissionRateJpaEntity;
import br.gravita.adapters.outbound.persistence.repositories.sales.CommissionRateJpaRepository;
import br.gravita.core.domain.sales.QuoteId;
import br.gravita.core.domain.sales.SalesOrder;
import br.gravita.core.domain.sales.SalesOrderId;
import br.gravita.core.domain.sales.SalesOrderItem;
import br.gravita.core.domain.sales.SalesOrderStatus;
import br.gravita.core.ports.outbound.persistence.sales.SalesOrderRepositoryPort;
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
class CalculateCommissionEndToEndTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private SalesOrderRepositoryPort salesOrderRepositoryPort;

	@Autowired
	private CommissionRateJpaRepository commissionRateJpaRepository;

	@Test
	@DisplayName("Calculates commissions for the orders invoiced within the period")
	void calculatesCommissionsForInvoicedOrdersInThePeriod() throws Exception {
		UUID salespersonId = UUID.randomUUID();
		UUID productId = UUID.randomUUID();
		seedRate(salespersonId, productId, new BigDecimal("0.10"));
		SalesOrder order = persistInvoicedOrder(salespersonId,
				List.of(new SalesOrderItem(productId, new BigDecimal("2"), new BigDecimal("50.00"),
						BigDecimal.ZERO)),
				LocalDate.now());

		mockMvc.perform(get("/api/sales/commissions")
						.param("salesperson", salespersonId.toString())
						.param("period", LocalDate.now().toString().substring(0, 7)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].salespersonId").value(salespersonId.toString()))
				.andExpect(jsonPath("$[0].productId").value(productId.toString()))
				.andExpect(jsonPath("$[0].orderId").value(order.getId().value().toString()))
				.andExpect(jsonPath("$[0].rate").value(0.10))
				.andExpect(jsonPath("$[0].amount").value(10.0));
	}

	@Test
	@DisplayName("Orders invoiced outside the period are not included in the commission")
	void ordersOutsideThePeriodAreNotIncluded() throws Exception {
		UUID salespersonId = UUID.randomUUID();
		UUID productId = UUID.randomUUID();
		seedRate(salespersonId, productId, new BigDecimal("0.10"));
		persistInvoicedOrder(salespersonId,
				List.of(new SalesOrderItem(productId, BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO)),
				LocalDate.now().minusMonths(2));

		mockMvc.perform(get("/api/sales/commissions")
						.param("period", LocalDate.now().toString().substring(0, 7)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	@DisplayName("Calculating a commission without a configured rate returns 404")
	void aMissingCommissionRateIsRejectedWith404() throws Exception {
		UUID salespersonId = UUID.randomUUID();
		UUID productId = UUID.randomUUID();
		persistInvoicedOrder(salespersonId,
				List.of(new SalesOrderItem(productId, BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO)),
				LocalDate.now());

		mockMvc.perform(get("/api/sales/commissions")
						.param("period", LocalDate.now().toString().substring(0, 7)))
				.andExpect(status().isNotFound());
	}

	private void seedRate(UUID salespersonId, UUID productId, BigDecimal rate) {
		commissionRateJpaRepository.save(CommissionRateJpaEntity.builder()
				.id(UUID.randomUUID())
				.salespersonId(salespersonId)
				.productId(productId)
				.rate(rate)
				.build());
	}

	private SalesOrder persistInvoicedOrder(UUID salespersonId, List<SalesOrderItem> items, LocalDate invoicedAt) {
		SalesOrder order = SalesOrder.of(SalesOrderId.of(UUID.randomUUID()), QuoteId.of(UUID.randomUUID()),
				UUID.randomUUID(), salespersonId, items, SalesOrderStatus.INVOICED, UUID.randomUUID(), null, null,
				invoicedAt);
		SalesOrder saved = salesOrderRepositoryPort.save(order);
		assertThat(saved.getStatus()).isEqualTo(SalesOrderStatus.INVOICED);
		return saved;
	}
}
