package br.gravita.reporting.adapter.in.web;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gravita.core.domain.finance.LedgerScope;
import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.ReceivableOrigin;
import br.gravita.core.domain.finance.ReceivableStatus;
import br.gravita.core.domain.inventory.Lot;
import br.gravita.core.domain.inventory.LotId;
import br.gravita.core.domain.inventory.StockBalance;
import br.gravita.core.domain.inventory.StockBalanceId;
import br.gravita.core.domain.masterdata.FiscalDocumentType;
import br.gravita.core.domain.sales.FiscalDocumentRef;
import br.gravita.core.domain.sales.QuoteId;
import br.gravita.core.domain.sales.SalesInvoice;
import br.gravita.core.domain.sales.SalesInvoiceId;
import br.gravita.core.domain.sales.SalesOrder;
import br.gravita.core.domain.sales.SalesOrderId;
import br.gravita.core.domain.sales.SalesOrderItem;
import br.gravita.core.domain.sales.SalesOrderStatus;
import br.gravita.core.domain.sales.SalespersonTarget;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.outbound.persistence.finance.ReceivableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.LotRepositoryPort;
import br.gravita.core.ports.outbound.persistence.inventory.StockBalanceRepositoryPort;
import br.gravita.core.ports.outbound.persistence.sales.SalesInvoiceRepositoryPort;
import br.gravita.core.ports.outbound.persistence.sales.SalesOrderRepositoryPort;
import br.gravita.core.ports.outbound.persistence.sales.SalespersonTargetRepositoryPort;
import br.gravita.core.ports.outbound.security.SessionStorePort;
import br.gravita.core.usercases.system.CheckPermissionUseCase;
import br.gravita.core.ports.inbound.reporting.GetExecutiveDashboardUseCase;
import br.gravita.core.ports.outbound.reporting.FinanceReadModelPort;
import br.gravita.core.ports.outbound.reporting.InventoryReadModelPort;
import br.gravita.core.ports.outbound.reporting.PermissionCheckPort;
import br.gravita.core.ports.outbound.reporting.SalesReadModelPort;
import br.gravita.core.ports.outbound.reporting.TaxReadModelPort;
import br.gravita.core.usercases.reporting.GetExecutiveDashboardService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs the real controller and read-model adapters over the real repositories; only the session and permission
 * lookups are stubbed. The service is rebuilt with a same-thread executor because its production fan-out runs on
 * other threads, which cannot see this test's uncommitted (rolled-back) data.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class GetExecutiveDashboardEndToEndTest {

	@TestConfiguration
	static class SameThreadFanOut {

		@Bean
		@Primary
		GetExecutiveDashboardUseCase sameThreadDashboard(SalesReadModelPort sales, InventoryReadModelPort inventory,
				FinanceReadModelPort finance, TaxReadModelPort tax, PermissionCheckPort permissions) {
			return new GetExecutiveDashboardService(sales, inventory, finance, tax, permissions,
					Clock.systemDefaultZone(), Runnable::run);
		}
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private SalesOrderRepositoryPort salesOrderRepositoryPort;

	@Autowired
	private SalesInvoiceRepositoryPort salesInvoiceRepositoryPort;

	@Autowired
	private SalespersonTargetRepositoryPort salespersonTargetRepositoryPort;

	@Autowired
	private ReceivableRepositoryPort receivableRepositoryPort;

	@Autowired
	private StockBalanceRepositoryPort stockBalanceRepositoryPort;

	@Autowired
	private LotRepositoryPort lotRepositoryPort;

	@MockitoBean
	private SessionStorePort sessionStorePort;

	@MockitoBean
	private CheckPermissionUseCase checkPermissionUseCase;

	private final LocalDate today = LocalDate.now();
	private final UUID companyA = UUID.randomUUID();
	private final UUID companyB = UUID.randomUUID();
	private final UUID salesperson = UUID.randomUUID();
	private final UUID product = UUID.randomUUID();

	@BeforeEach
	void seed() {
		UserId caller = UserId.generate();
		when(sessionStorePort.resolve("valid-token")).thenReturn(Optional.of(caller));
		when(checkPermissionUseCase.execute(argThat(
				query -> query != null && query.userId().equals(caller) && query.module().equals("reporting")
						&& query.screen().equals("dashboard")))).thenReturn(true);

		// Today: 2 x 100 with an issued fiscal document, and one 50 order still waiting for its document.
		invoicedOrder(new BigDecimal("2"), new BigDecimal("50.00"), today, true);
		invoicedOrder(BigDecimal.ONE, new BigDecimal("50.00"), today, false);
		salespersonTargetRepositoryPort.save(new SalespersonTarget(salesperson, YearMonth.from(today),
				new BigDecimal("500.00"), 5));
		stockBalanceRepositoryPort.save(StockBalance.of(StockBalanceId.of(UUID.randomUUID()), product,
				UUID.randomUUID(), new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("20.00")));
		lotRepositoryPort.save(Lot.of(LotId.of(UUID.randomUUID()), UUID.randomUUID(), UUID.randomUUID(), "L-EXP",
				today.plusDays(5), new BigDecimal("7")));

		// Receivables: A owes 100 (10 days late) and 40 (70 days late); B owes 999; one is not yet due.
		receivable("100.00", today.minusDays(10), companyA);
		receivable("40.00", today.minusDays(70), companyA);
		receivable("999.00", today.minusDays(10), companyB);
		receivable("5.00", today.plusDays(10), companyA);
	}

	@Test
	void composesTheDashboardFromTheSalesInventoryFinanceAndTaxReadModels() throws Exception {
		mockMvc.perform(get("/api/reports/dashboard").param("period", "DAY").param("companyId", companyA.toString())
				.header("Authorization", "Bearer valid-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.period").value("DAY"))
				.andExpect(jsonPath("$.revenue.day.current").value(150.0))
				.andExpect(jsonPath("$.revenue.invoicedTotal").value(100.0))
				.andExpect(jsonPath("$.revenue.reconciliationDifference").value(50.0))
				.andExpect(jsonPath("$.margin.cmv").value(60.0))
				.andExpect(jsonPath("$.margin.grossMargin").value(90.0))
				.andExpect(jsonPath("$.margin.grossMarginPercent").value(60.0))
				.andExpect(jsonPath("$.delinquency.totalOverdue").value(140.0))
				.andExpect(jsonPath("$.delinquency.aging.upTo30Days").value(100.0))
				.andExpect(jsonPath("$.delinquency.aging.over60Days").value(40.0))
				.andExpect(jsonPath("$.criticalStock[?(@.lotCode == 'L-EXP')].reason").value("NEAR_EXPIRY"))
				.andExpect(jsonPath("$.topProducts.byQuantity[0].quantity").value(3.0))
				.andExpect(jsonPath("$.targets.company.valueTarget").value(500.0))
				.andExpect(jsonPath("$.targets.company.valueAchieved").value(150.0))
				.andExpect(jsonPath("$.targets.company.percentComplete").value(30.0))
				.andExpect(jsonPath("$.targets.salespeople[0].salespersonId").value(salesperson.toString()));
	}

	@Test
	void defaultsToTheMonthPeriod() throws Exception {
		mockMvc.perform(get("/api/reports/dashboard").header("Authorization", "Bearer valid-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.period").value("MONTH"));
	}

	@Test
	void answersForbiddenWhenTheProfileCannotViewTheDashboard() throws Exception {
		when(sessionStorePort.resolve("other-token")).thenReturn(Optional.of(UserId.generate()));

		mockMvc.perform(get("/api/reports/dashboard").header("Authorization", "Bearer other-token"))
				.andExpect(status().isForbidden());
	}

	@Test
	void answersUnauthorizedWithoutASession() throws Exception {
		mockMvc.perform(get("/api/reports/dashboard")).andExpect(status().isUnauthorized());
	}

	private void invoicedOrder(BigDecimal quantity, BigDecimal unitPrice, LocalDate invoicedAt, boolean withDocument) {
		SalesOrder order = salesOrderRepositoryPort.save(SalesOrder.of(SalesOrderId.of(UUID.randomUUID()),
				QuoteId.of(UUID.randomUUID()), UUID.randomUUID(), salesperson,
				List.of(new SalesOrderItem(product, quantity, unitPrice, BigDecimal.ZERO)),
				SalesOrderStatus.INVOICED, UUID.randomUUID(), null, null, invoicedAt));
		if (withDocument) {
			salesInvoiceRepositoryPort.save(SalesInvoice.issue(SalesInvoiceId.of(UUID.randomUUID()), order.getId(),
					List.of(new FiscalDocumentRef(FiscalDocumentType.NFE, UUID.randomUUID()))));
		}
	}

	private void receivable(String amount, LocalDate dueDate, UUID company) {
		receivableRepositoryPort.save(Receivable.of(ReceivableId.of(UUID.randomUUID()), UUID.randomUUID(),
				ReceivableOrigin.MANUAL, new BigDecimal(amount), dueDate, null, ReceivableStatus.OPEN, null, null,
				new LedgerScope(company, null, null)));
	}
}
