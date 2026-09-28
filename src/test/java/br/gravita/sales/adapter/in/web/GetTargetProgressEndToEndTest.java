package br.gravita.sales.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.adapters.outbound.persistence.entities.sales.SalespersonTargetJpaEntity;
import br.gravita.adapters.outbound.persistence.repositories.sales.SalespersonTargetJpaRepository;
import br.gravita.core.domain.sales.QuoteId;
import br.gravita.core.domain.sales.SalesOrder;
import br.gravita.core.domain.sales.SalesOrderId;
import br.gravita.core.domain.sales.SalesOrderItem;
import br.gravita.core.domain.sales.SalesOrderStatus;
import br.gravita.core.ports.outbound.persistence.sales.SalesOrderRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
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
class GetTargetProgressEndToEndTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private SalesOrderRepositoryPort salesOrderRepositoryPort;

	@Autowired
	private SalespersonTargetJpaRepository salespersonTargetJpaRepository;

	@Test
	void reportsProgressAgainstTheConfiguredTarget() throws Exception {
		UUID salespersonId = UUID.randomUUID();
		YearMonth month = YearMonth.now();
		seedTarget(salespersonId, month, new BigDecimal("1000.00"), 2);
		persistInvoicedOrder(salespersonId, new BigDecimal("600.00"), month.atDay(1));

		mockMvc.perform(get("/api/crm/targets/" + salespersonId + "/" + month))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.targetConfigured").value(true))
				.andExpect(jsonPath("$.valueAchieved").value(600.0))
				.andExpect(jsonPath("$.orderCountAchieved").value(1))
				.andExpect(jsonPath("$.valueTarget").value(1000.0))
				.andExpect(jsonPath("$.orderCountTarget").value(2))
				.andExpect(jsonPath("$.percentComplete.value").value(60.0))
				.andExpect(jsonPath("$.percentComplete.orderCount").value(50.0));
	}

	@Test
	void returnsANoTargetConfiguredResultWhenNoTargetWasSet() throws Exception {
		UUID salespersonId = UUID.randomUUID();
		YearMonth month = YearMonth.now();
		persistInvoicedOrder(salespersonId, new BigDecimal("100.00"), month.atDay(1));

		mockMvc.perform(get("/api/crm/targets/" + salespersonId + "/" + month))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.targetConfigured").value(false))
				.andExpect(jsonPath("$.valueAchieved").value(100.0))
				.andExpect(jsonPath("$.percentComplete").doesNotExist());
	}

	private void seedTarget(UUID salespersonId, YearMonth month, BigDecimal valueTarget, long orderCountTarget) {
		salespersonTargetJpaRepository.save(SalespersonTargetJpaEntity.builder()
				.id(UUID.randomUUID())
				.salespersonId(salespersonId)
				.referenceMonth(month.atDay(1))
				.valueTarget(valueTarget)
				.orderCountTarget(orderCountTarget)
				.build());
	}

	private void persistInvoicedOrder(UUID salespersonId, BigDecimal itemValue, LocalDate invoicedAt) {
		SalesOrder order = SalesOrder.of(SalesOrderId.of(UUID.randomUUID()), QuoteId.of(UUID.randomUUID()),
				UUID.randomUUID(), salespersonId,
				List.of(new SalesOrderItem(UUID.randomUUID(), BigDecimal.ONE, itemValue, BigDecimal.ZERO)),
				SalesOrderStatus.INVOICED, UUID.randomUUID(), null, null, invoicedAt);
		salesOrderRepositoryPort.save(order);
	}
}
