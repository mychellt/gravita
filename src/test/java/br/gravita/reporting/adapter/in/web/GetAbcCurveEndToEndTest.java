package br.gravita.reporting.adapter.in.web;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.core.domain.sales.QuoteId;
import br.gravita.core.domain.sales.SalesOrder;
import br.gravita.core.domain.sales.SalesOrderId;
import br.gravita.core.domain.sales.SalesOrderItem;
import br.gravita.core.domain.sales.SalesOrderStatus;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.outbound.persistence.sales.SalesOrderRepositoryPort;
import br.gravita.core.ports.outbound.security.SessionStorePort;
import br.gravita.core.usercases.system.CheckPermissionUseCase;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
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
class GetAbcCurveEndToEndTest {

	// A month long past, so the data cannot collide with other tests' invoiced orders.
	private static final YearMonth PERIOD = YearMonth.of(2019, 3);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private SalesOrderRepositoryPort salesOrderRepositoryPort;

	@MockitoBean
	private SessionStorePort sessionStorePort;

	@MockitoBean
	private CheckPermissionUseCase checkPermissionUseCase;

	private final UUID salesperson = UUID.randomUUID();
	private final UUID rice = UUID.randomUUID();
	private final UUID beans = UUID.randomUUID();
	private final UUID bigCustomer = UUID.randomUUID();
	private final UUID smallCustomer = UUID.randomUUID();

	@BeforeEach
	void seed() {
		UserId caller = UserId.generate();
		when(sessionStorePort.resolve("valid-token")).thenReturn(Optional.of(caller));
		when(checkPermissionUseCase.execute(argThat(
				query -> query != null && query.userId().equals(caller) && query.module().equals("reporting")
						&& query.screen().equals("abc-curve")))).thenReturn(true);

		// Inside the period: rice 900 + beans 100, bought 900 by the big customer and 100 by the small one.
		invoicedOrder(bigCustomer, rice, "9", "100.00", LocalDate.of(2019, 3, 1));
		invoicedOrder(smallCustomer, beans, "1", "100.00", LocalDate.of(2019, 3, 31));
		// Outside the period, which must not count.
		invoicedOrder(smallCustomer, beans, "50", "100.00", LocalDate.of(2019, 4, 1));
		invoicedOrder(smallCustomer, beans, "50", "100.00", LocalDate.of(2019, 2, 28));
	}

	@Test
	void classifiesProductsOfThePeriodByRevenueShare() throws Exception {
		mockMvc.perform(get("/api/reports/abc-curve").param("type", "product").param("period", "2019-03")
				.header("Authorization", "Bearer valid-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].entityId").value(rice.toString()))
				.andExpect(jsonPath("$[0].revenue").value(900.0))
				.andExpect(jsonPath("$[0].revenueShare").value(90.0))
				.andExpect(jsonPath("$[0].cumulativeShare").value(90.0))
				.andExpect(jsonPath("$[0].abcClass").value("A"))
				.andExpect(jsonPath("$[1].entityId").value(beans.toString()))
				.andExpect(jsonPath("$[1].revenueShare").value(10.0))
				.andExpect(jsonPath("$[1].abcClass").value("B"));
	}

	@Test
	void classifiesCustomersOfThePeriodAndAcceptsTheTypeInAnyCase() throws Exception {
		mockMvc.perform(get("/api/reports/abc-curve").param("type", "CUSTOMER").param("period", "2019-03")
				.header("Authorization", "Bearer valid-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].entityId").value(bigCustomer.toString()))
				.andExpect(jsonPath("$[0].abcClass").value("A"))
				.andExpect(jsonPath("$[1].entityId").value(smallCustomer.toString()))
				.andExpect(jsonPath("$[1].revenueShare").value(10.0));
	}

	@Test
	void answersBadRequestForAnUnknownType() throws Exception {
		mockMvc.perform(get("/api/reports/abc-curve").param("type", "supplier").param("period", "2019-03")
				.header("Authorization", "Bearer valid-token")).andExpect(status().isBadRequest());
	}

	@Test
	void answersBadRequestWhenThePeriodIsMissingOrMalformed() throws Exception {
		mockMvc.perform(get("/api/reports/abc-curve").param("type", "product")
				.header("Authorization", "Bearer valid-token")).andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/reports/abc-curve").param("type", "product").param("period", "March")
				.header("Authorization", "Bearer valid-token")).andExpect(status().isBadRequest());
	}

	@Test
	void answersForbiddenWhenTheProfileCannotViewTheReport() throws Exception {
		when(sessionStorePort.resolve("other-token")).thenReturn(Optional.of(UserId.generate()));

		mockMvc.perform(get("/api/reports/abc-curve").param("type", "product").param("period", "2019-03")
				.header("Authorization", "Bearer other-token")).andExpect(status().isForbidden());
	}

	@Test
	void answersUnauthorizedWithoutASession() throws Exception {
		mockMvc.perform(get("/api/reports/abc-curve").param("type", "product").param("period", "2019-03"))
				.andExpect(status().isUnauthorized());
	}

	private void invoicedOrder(UUID customer, UUID product, String quantity, String unitPrice, LocalDate invoicedAt) {
		salesOrderRepositoryPort.save(SalesOrder.of(SalesOrderId.of(UUID.randomUUID()), QuoteId.of(UUID.randomUUID()),
				customer, salesperson,
				List.of(new SalesOrderItem(product, new BigDecimal(quantity), new BigDecimal(unitPrice), BigDecimal.ZERO)),
				SalesOrderStatus.INVOICED, UUID.randomUUID(), null, null, invoicedAt));
	}
}
