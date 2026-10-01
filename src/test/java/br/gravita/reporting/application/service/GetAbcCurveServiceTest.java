package br.gravita.reporting.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.exceptions.ForbiddenException;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.inbound.reporting.AbcClass;
import br.gravita.core.ports.inbound.reporting.AbcCurveEntry;
import br.gravita.core.ports.inbound.reporting.AbcCurveQuery;
import br.gravita.core.ports.inbound.reporting.AbcCurveType;
import br.gravita.core.ports.outbound.reporting.PermissionCheckPort;
import br.gravita.core.ports.outbound.reporting.SalesReadModelPort;
import br.gravita.core.ports.outbound.reporting.SalesReadModelPort.CustomerSales;
import br.gravita.core.ports.outbound.reporting.SalesReadModelPort.ProductSales;
import br.gravita.core.usercases.reporting.GetAbcCurveService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetAbcCurveServiceTest {

	private static final YearMonth PERIOD = YearMonth.of(2026, 2);
	private static final LocalDate FROM = LocalDate.of(2026, 2, 1);
	private static final LocalDate TO = LocalDate.of(2026, 2, 28);

	private final SalesReadModelPort sales = mock(SalesReadModelPort.class);
	private final PermissionCheckPort permissions = mock(PermissionCheckPort.class);

	private final UserId user = UserId.generate();
	private final UUID company = UUID.randomUUID();
	private GetAbcCurveService service;

	@BeforeEach
	void setUp() {
		service = new GetAbcCurveService(sales, permissions);
		when(permissions.canView(user, "abc-curve")).thenReturn(true);
		when(sales.productSales(any(), any(), any())).thenReturn(List.of());
		when(sales.customerSales(any(), any(), any())).thenReturn(List.of());
	}

	@Test
	void refusesAUserWhoseProfileCannotViewTheReportWithoutReadingAnything() {
		UserId stranger = UserId.generate();
		when(permissions.canView(stranger, "abc-curve")).thenReturn(false);

		assertThatThrownBy(() -> service.execute(query(stranger, AbcCurveType.PRODUCT)))
				.isInstanceOf(ForbiddenException.class);

		verifyNoInteractions(sales);
	}

	@Test
	void ranksProductsByRevenueAndClassesThemByTheCumulativeShareBeforeEach() {
		UUID p1 = UUID.randomUUID();
		UUID p2 = UUID.randomUUID();
		UUID p3 = UUID.randomUUID();
		UUID p4 = UUID.randomUUID();
		UUID p5 = UUID.randomUUID();
		when(sales.productSales(FROM, TO, company)).thenReturn(List.of(productSale(p4, "4"), productSale(p1, "50"),
				productSale(p5, "1"), productSale(p3, "15"), productSale(p2, "30")));

		List<AbcCurveEntry> curve = service.execute(query(user, AbcCurveType.PRODUCT));

		assertThat(curve).extracting(AbcCurveEntry::entityId).containsExactly(p1, p2, p3, p4, p5);
		// Shares of 100: 50, 30 | 15 | 4, 1. Before p2 the curve is at 50 (<80); before p3 at 80; before p4 at 95.
		assertThat(curve).extracting(AbcCurveEntry::abcClass)
				.containsExactly(AbcClass.A, AbcClass.A, AbcClass.B, AbcClass.C, AbcClass.C);
		assertThat(curve).extracting(AbcCurveEntry::revenueShare).usingComparatorForType(BigDecimal::compareTo,
				BigDecimal.class).containsExactly(new BigDecimal("50"), new BigDecimal("30"), new BigDecimal("15"),
						new BigDecimal("4"), new BigDecimal("1"));
		assertThat(curve).extracting(AbcCurveEntry::cumulativeShare).usingComparatorForType(BigDecimal::compareTo,
				BigDecimal.class).containsExactly(new BigDecimal("50"), new BigDecimal("80"), new BigDecimal("95"),
						new BigDecimal("99"), new BigDecimal("100"));
		assertThat(curve.get(0).revenue()).isEqualByComparingTo("50");
	}

	@Test
	void classifiesCustomersWhenTheTypeIsCustomer() {
		UUID big = UUID.randomUUID();
		UUID small = UUID.randomUUID();
		when(sales.customerSales(FROM, TO, company))
				.thenReturn(List.of(new CustomerSales(small, new BigDecimal("10")), new CustomerSales(big, new BigDecimal("990"))));

		List<AbcCurveEntry> curve = service.execute(query(user, AbcCurveType.CUSTOMER));

		assertThat(curve).extracting(AbcCurveEntry::entityId).containsExactly(big, small);
		assertThat(curve).extracting(AbcCurveEntry::abcClass).containsExactly(AbcClass.A, AbcClass.C);
		verify(sales).customerSales(FROM, TO, company);
		verify(sales, never()).productSales(any(), any(), any());
	}

	@Test
	void keepsTheTopEntryInClassAEvenWhenItAloneIsAboveTheThreshold() {
		UUID top = UUID.randomUUID();
		when(sales.productSales(any(), any(), any())).thenReturn(List.of(productSale(top, "100")));

		List<AbcCurveEntry> curve = service.execute(query(user, AbcCurveType.PRODUCT));

		assertThat(curve).singleElement().satisfies(entry -> {
			assertThat(entry.abcClass()).isEqualTo(AbcClass.A);
			assertThat(entry.revenueShare()).isEqualByComparingTo("100");
		});
	}

	@Test
	void scopesTheReadToTheWholeRequestedMonth() {
		service.execute(new AbcCurveQuery(user, AbcCurveType.PRODUCT, YearMonth.of(2028, 2), null));

		verify(sales).productSales(eq(LocalDate.of(2028, 2, 1)), eq(LocalDate.of(2028, 2, 29)), eq(null));
	}

	@Test
	void leavesOutEntriesWithoutPositiveRevenueAndBreaksTiesByEntityId() {
		UUID low = UUID.fromString("00000000-0000-0000-0000-000000000001");
		UUID high = UUID.fromString("00000000-0000-0000-0000-000000000002");
		when(sales.productSales(any(), any(), any())).thenReturn(List.of(productSale(high, "10"),
				productSale(UUID.randomUUID(), "0"), productSale(UUID.randomUUID(), "-5"), productSale(low, "10")));

		List<AbcCurveEntry> curve = service.execute(query(user, AbcCurveType.PRODUCT));

		assertThat(curve).extracting(AbcCurveEntry::entityId).containsExactly(low, high);
		assertThat(curve.get(0).revenueShare()).isEqualByComparingTo("50");
	}

	@Test
	void decidesTheClassOnExactAmountsNotRoundedShares() {
		UUID a = UUID.randomUUID();
		UUID b = UUID.randomUUID();
		UUID c = UUID.randomUUID();
		// The curve reaches 79.996% before c: it displays as 80.00 but is still below the threshold, so c stays A.
		when(sales.productSales(any(), any(), any())).thenReturn(List.of(productSale(a, "79996"),
				productSale(b, "10000"), productSale(c, "10004")));

		List<AbcCurveEntry> curve = service.execute(query(user, AbcCurveType.PRODUCT));

		assertThat(curve).extracting(AbcCurveEntry::entityId).containsExactly(a, c, b);
		assertThat(curve.get(0).cumulativeShare()).isEqualByComparingTo("80.00");
		assertThat(curve).extracting(AbcCurveEntry::abcClass).containsExactly(AbcClass.A, AbcClass.A, AbcClass.B);
	}

	@Test
	void answersAnEmptyCurveWhenNothingWasInvoicedInThePeriod() {
		assertThat(service.execute(query(user, AbcCurveType.PRODUCT))).isEmpty();
	}

	private AbcCurveQuery query(UserId requester, AbcCurveType type) {
		return new AbcCurveQuery(requester, type, PERIOD, company);
	}

	private ProductSales productSale(UUID productId, String value) {
		return new ProductSales(productId, BigDecimal.ONE, new BigDecimal(value));
	}
}
