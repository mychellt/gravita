package br.gravita.reporting.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.exceptions.ForbiddenException;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.inbound.reporting.DreQuery;
import br.gravita.core.ports.inbound.reporting.ManagerialDre;
import br.gravita.core.ports.outbound.reporting.FinanceReadModelPort;
import br.gravita.core.ports.outbound.reporting.FinanceReadModelPort.CostCenterExpense;
import br.gravita.core.ports.outbound.reporting.InventoryReadModelPort;
import br.gravita.core.ports.outbound.reporting.InventoryReadModelPort.SoldQuantity;
import br.gravita.core.ports.outbound.reporting.PermissionCheckPort;
import br.gravita.core.ports.outbound.reporting.SalesReadModelPort;
import br.gravita.core.ports.outbound.reporting.SalesReadModelPort.ProductSales;
import br.gravita.core.ports.outbound.reporting.TaxReadModelPort;
import br.gravita.core.ports.outbound.reporting.TaxReadModelPort.DocumentTaxRecord;
import br.gravita.core.usercases.reporting.GetManagerialDreService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GetManagerialDreServiceTest {

	private static final YearMonth PERIOD = YearMonth.of(2028, 2);
	private static final LocalDate FROM = LocalDate.of(2028, 2, 1);
	private static final LocalDate TO = LocalDate.of(2028, 2, 29);

	private final SalesReadModelPort sales = mock(SalesReadModelPort.class);
	private final TaxReadModelPort tax = mock(TaxReadModelPort.class);
	private final InventoryReadModelPort inventory = mock(InventoryReadModelPort.class);
	private final FinanceReadModelPort finance = mock(FinanceReadModelPort.class);
	private final PermissionCheckPort permissions = mock(PermissionCheckPort.class);

	private final UserId user = UserId.generate();
	private final UUID company = UUID.randomUUID();
	private final UUID rent = UUID.randomUUID();
	private final UUID marketing = UUID.randomUUID();
	private GetManagerialDreService service;

	@BeforeEach
	void setUp() {
		service = new GetManagerialDreService(sales, tax, inventory, finance, permissions);
		when(permissions.canView(user, "dre")).thenReturn(true);
		when(sales.dailyRevenue(any(), any(), any())).thenReturn(Map.of());
		when(sales.productSales(any(), any(), any())).thenReturn(List.of());
		when(tax.authorizedDocumentTaxes(any(), any())).thenReturn(List.of());
		when(inventory.costOfGoodsSold(any())).thenReturn(BigDecimal.ZERO);
		when(finance.expensesByCostCenter(any(), any(), any(), any())).thenReturn(List.of());
	}

	@DisplayName("Refuses a user whose profile cannot view the report, without reading any data")
	@Test
	void refusesAUserWhoseProfileCannotViewTheReportWithoutReadingAnything() {
		UserId stranger = UserId.generate();
		when(permissions.canView(stranger, "dre")).thenReturn(false);

		assertThatThrownBy(() -> service.execute(new DreQuery(stranger, PERIOD, null, company)))
				.isInstanceOf(ForbiddenException.class);

		verifyNoInteractions(sales, tax, inventory, finance);
	}

	@DisplayName("Composes the month's DRE and reconciles the net result")
	@Test
	void composesTheDreOfTheMonthAndReconcilesTheNetResult() {
		UUID product = UUID.randomUUID();
		when(sales.dailyRevenue(FROM, TO, company)).thenReturn(
				Map.of(LocalDate.of(2028, 2, 3), new BigDecimal("600.00"), LocalDate.of(2028, 2, 20), new BigDecimal("400.00")));
		when(sales.productSales(FROM, TO, company))
				.thenReturn(List.of(new ProductSales(product, new BigDecimal("5"), new BigDecimal("1000.00"))));
		when(inventory.costOfGoodsSold(List.of(new SoldQuantity(product, new BigDecimal("5")))))
				.thenReturn(new BigDecimal("350.00"));
		when(tax.authorizedDocumentTaxes(FROM, TO)).thenReturn(List.of(
				new DocumentTaxRecord("NFE", LocalDate.of(2028, 2, 3), new BigDecimal("100.00"), new BigDecimal("50.00"),
						new BigDecimal("16.50"), new BigDecimal("76.00"), BigDecimal.ZERO),
				new DocumentTaxRecord("NFSE", LocalDate.of(2028, 2, 9), BigDecimal.ZERO, BigDecimal.ZERO,
						BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("12.50"))));
		when(finance.expensesByCostCenter(FROM, TO, null, company)).thenReturn(
				List.of(new CostCenterExpense(rent, new BigDecimal("120.00")), new CostCenterExpense(marketing, new BigDecimal("80.00"))));

		ManagerialDre dre = service.execute(new DreQuery(user, PERIOD, null, company));

		assertThat(dre.period()).isEqualTo(PERIOD);
		assertThat(dre.costCenter()).isNull();
		assertThat(dre.grossRevenue()).isEqualByComparingTo("1000.00");
		// ICMS + PIS + COFINS + ISS; the IPI is charged on top of the price, so it is not deducted.
		assertThat(dre.deductions()).isEqualByComparingTo("205.00");
		assertThat(dre.cmv()).isEqualByComparingTo("350.00");
		assertThat(dre.totalExpenses()).isEqualByComparingTo("200.00");
		assertThat(dre.netResult()).isEqualByComparingTo("245.00");
		assertThat(dre.netResult()).isEqualByComparingTo(dre.grossRevenue().subtract(dre.deductions())
				.subtract(dre.cmv()).subtract(dre.totalExpenses()));
	}

	@DisplayName("Lists expenses biggest first, with those without a cost center last")
	@Test
	void listsTheExpensesBiggestFirstWithThoseOfNoCostCenterLast() {
		when(finance.expensesByCostCenter(any(), any(), any(), any())).thenReturn(List.of(
				new CostCenterExpense(null, new BigDecimal("900.00")), new CostCenterExpense(marketing, new BigDecimal("80.00")),
				new CostCenterExpense(rent, new BigDecimal("120.00"))));

		ManagerialDre dre = service.execute(new DreQuery(user, PERIOD, null, null));

		assertThat(dre.expensesByCostCenter()).extracting(expense -> expense.costCenterId()).containsExactly(rent,
				marketing, null);
		assertThat(dre.totalExpenses()).isEqualByComparingTo("1100.00");
	}

	@DisplayName("Narrows the expenses to the requested cost center and leaves the other figures untouched")
	@Test
	void narrowsTheExpensesToTheRequestedCostCenterAndLeavesTheRestUntouched() {
		when(sales.dailyRevenue(any(), any(), any())).thenReturn(Map.of(FROM, new BigDecimal("1000.00")));
		when(finance.expensesByCostCenter(FROM, TO, rent, company))
				.thenReturn(List.of(new CostCenterExpense(rent, new BigDecimal("120.00"))));

		ManagerialDre dre = service.execute(new DreQuery(user, PERIOD, rent, company));

		verify(finance).expensesByCostCenter(FROM, TO, rent, company);
		assertThat(dre.costCenter()).isEqualTo(rent);
		assertThat(dre.expensesByCostCenter()).singleElement()
				.satisfies(expense -> assertThat(expense.costCenterId()).isEqualTo(rent));
		assertThat(dre.grossRevenue()).isEqualByComparingTo("1000.00");
		assertThat(dre.netResult()).isEqualByComparingTo("880.00");
	}

	@DisplayName("Reports a zero DRE for a period without activity")
	@Test
	void reportsAZeroDreForAPeriodWithoutActivity() {
		ManagerialDre dre = service.execute(new DreQuery(user, PERIOD, null, null));

		assertThat(dre.grossRevenue()).isEqualByComparingTo("0");
		assertThat(dre.deductions()).isEqualByComparingTo("0");
		assertThat(dre.cmv()).isEqualByComparingTo("0");
		assertThat(dre.expensesByCostCenter()).isEmpty();
		assertThat(dre.netResult()).isEqualByComparingTo("0");
	}

	@DisplayName("Reports a loss when costs exceed revenue")
	@Test
	void reportsALossWhenTheCostsExceedTheRevenue() {
		when(sales.dailyRevenue(any(), any(), any())).thenReturn(Map.of(FROM, new BigDecimal("100.00")));
		when(finance.expensesByCostCenter(any(), any(), any(), any()))
				.thenReturn(List.of(new CostCenterExpense(rent, new BigDecimal("250.00"))));

		ManagerialDre dre = service.execute(new DreQuery(user, PERIOD, null, null));

		assertThat(dre.netResult()).isEqualByComparingTo("-150.00");
	}
}
