package br.gravita.reporting.adapter.in.web;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.core.domain.sales.Commission;
import br.gravita.core.domain.sales.CommissionId;
import br.gravita.core.domain.sales.QuoteId;
import br.gravita.core.domain.sales.SalesOrder;
import br.gravita.core.domain.sales.SalesOrderId;
import br.gravita.core.domain.sales.SalesOrderItem;
import br.gravita.core.domain.sales.SalesOrderStatus;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.outbound.persistence.sales.CommissionRepositoryPort;
import br.gravita.core.ports.outbound.persistence.sales.SalesOrderRepositoryPort;
import br.gravita.core.ports.outbound.security.SessionStorePort;
import br.gravita.core.usercases.system.CheckPermissionUseCase;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
class GetCommissionReportEndToEndTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private SalesOrderRepositoryPort salesOrderRepositoryPort;

	@Autowired
	private CommissionRepositoryPort commissionRepositoryPort;

	@MockitoBean
	private SessionStorePort sessionStorePort;

	@MockitoBean
	private CheckPermissionUseCase checkPermissionUseCase;

	private final UUID ana = UUID.fromString("00000000-0000-0000-0000-00000000000a");
	private final UUID bruno = UUID.fromString("00000000-0000-0000-0000-00000000000b");
	private final UUID rice = UUID.randomUUID();
	private final UUID beans = UUID.randomUUID();
	private final SalesOrderId anaOrder = SalesOrderId.of(UUID.randomUUID());
	private final SalesOrderId brunoOrder = SalesOrderId.of(UUID.randomUUID());

	@BeforeEach
	void seed() {
		UserId caller = UserId.generate();
		when(sessionStorePort.resolve("valid-token")).thenReturn(Optional.of(caller));
		when(checkPermissionUseCase.execute(argThat(
				query -> query != null && query.userId().equals(caller) && query.module().equals("reporting")
						&& query.screen().equals("commissions")))).thenReturn(true);

		// Inside the period (a month long past, so other tests' orders cannot collide): Ana sells rice and beans, Bruno rice.
		invoicedOrder(anaOrder, ana, LocalDate.of(2018, 3, 10));
		commission(ana, rice, anaOrder, "0.0500", "45.0000");
		commission(ana, beans, anaOrder, "0.1000", "10.0000");
		invoicedOrder(brunoOrder, bruno, LocalDate.of(2018, 3, 31));
		commission(bruno, rice, brunoOrder, "0.0200", "18.0000");
		// Outside the period, which must not count.
		SalesOrderId lateOrder = SalesOrderId.of(UUID.randomUUID());
		invoicedOrder(lateOrder, ana, LocalDate.of(2018, 4, 1));
		commission(ana, rice, lateOrder, "0.0500", "99.0000");
	}

	@DisplayName("Lists the period's commissions per salesperson and product")
	@Test
	void listsTheCommissionsOfThePeriodPerSalespersonAndProduct() throws Exception {
		mockMvc.perform(get("/api/reports/commissions").param("period", "2018-03").header("Authorization",
				"Bearer valid-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(3))
				.andExpect(jsonPath("$[0].salespersonId").value(ana.toString()))
				.andExpect(jsonPath("$[1].salespersonId").value(ana.toString()))
				.andExpect(jsonPath("$[2].salespersonId").value(bruno.toString()))
				.andExpect(jsonPath("$[2].productId").value(rice.toString()))
				.andExpect(jsonPath("$[2].orderId").value(brunoOrder.value().toString()))
				.andExpect(jsonPath("$[2].rate").value(0.02))
				.andExpect(jsonPath("$[2].amount").value(18.0));
	}

	@DisplayName("Narrows the report to the requested salesperson")
	@Test
	void narrowsTheReportToTheRequestedSalesperson() throws Exception {
		mockMvc.perform(get("/api/reports/commissions").param("salesperson", ana.toString())
				.param("period", "2018-03").header("Authorization", "Bearer valid-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].salespersonId").value(ana.toString()))
				.andExpect(jsonPath("$[1].salespersonId").value(ana.toString()));
	}

	@DisplayName("Returns an empty report for a period without commissions")
	@Test
	void answersAnEmptyReportForAPeriodWithoutCommissions() throws Exception {
		mockMvc.perform(get("/api/reports/commissions").param("period", "2018-02").header("Authorization",
				"Bearer valid-token")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
	}

	@DisplayName("Returns 400 when the period is missing or malformed")
	@Test
	void answersBadRequestWhenThePeriodIsMissingOrMalformed() throws Exception {
		mockMvc.perform(get("/api/reports/commissions").header("Authorization", "Bearer valid-token"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/reports/commissions").param("period", "March").header("Authorization",
				"Bearer valid-token")).andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/reports/commissions").param("salesperson", "ana").param("period", "2018-03")
				.header("Authorization", "Bearer valid-token")).andExpect(status().isBadRequest());
	}

	@DisplayName("Returns 403 when the profile cannot view the report")
	@Test
	void answersForbiddenWhenTheProfileCannotViewTheReport() throws Exception {
		when(sessionStorePort.resolve("other-token")).thenReturn(Optional.of(UserId.generate()));

		mockMvc.perform(get("/api/reports/commissions").param("period", "2018-03").header("Authorization",
				"Bearer other-token")).andExpect(status().isForbidden());
	}

	@DisplayName("Returns 401 when there is no session")
	@Test
	void answersUnauthorizedWithoutASession() throws Exception {
		mockMvc.perform(get("/api/reports/commissions").param("period", "2018-03"))
				.andExpect(status().isUnauthorized());
	}

	private void invoicedOrder(SalesOrderId id, UUID salesperson, LocalDate invoicedAt) {
		salesOrderRepositoryPort.save(SalesOrder.of(id, QuoteId.of(UUID.randomUUID()), UUID.randomUUID(), salesperson,
				List.of(new SalesOrderItem(rice, BigDecimal.ONE, new BigDecimal("100.00"), BigDecimal.ZERO)),
				SalesOrderStatus.INVOICED, UUID.randomUUID(), null, null, invoicedAt));
	}

	private void commission(UUID salesperson, UUID product, SalesOrderId order, String rate, String amount) {
		commissionRepositoryPort.save(new Commission(CommissionId.of(UUID.randomUUID()), salesperson, product, order,
				new BigDecimal(rate), new BigDecimal(amount)));
	}
}
