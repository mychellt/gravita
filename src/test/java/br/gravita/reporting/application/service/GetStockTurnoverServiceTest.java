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
import br.gravita.core.ports.inbound.reporting.StockTurnoverEntry;
import br.gravita.core.ports.inbound.reporting.StockTurnoverQuery;
import br.gravita.core.ports.outbound.reporting.InventoryReadModelPort;
import br.gravita.core.ports.outbound.reporting.InventoryReadModelPort.StockFlow;
import br.gravita.core.ports.outbound.reporting.PermissionCheckPort;
import br.gravita.core.usercases.reporting.GetStockTurnoverService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetStockTurnoverServiceTest {

	private static final YearMonth PERIOD = YearMonth.of(2028, 2);
	private static final LocalDate FROM = LocalDate.of(2028, 2, 1);
	private static final LocalDate TO = LocalDate.of(2028, 2, 29);

	private final InventoryReadModelPort inventory = mock(InventoryReadModelPort.class);
	private final PermissionCheckPort permissions = mock(PermissionCheckPort.class);

	private final UserId user = UserId.generate();
	private final UUID company = UUID.randomUUID();
	private GetStockTurnoverService service;

	@BeforeEach
	void setUp() {
		service = new GetStockTurnoverService(inventory, permissions);
		when(permissions.canView(user, "stock-turnover")).thenReturn(true);
		when(inventory.stockFlows(any(), any(), any())).thenReturn(List.of());
	}

	@Test
	void refusesAUserWhoseProfileCannotViewTheReportWithoutReadingAnything() {
		UserId stranger = UserId.generate();
		when(permissions.canView(stranger, "stock-turnover")).thenReturn(false);

		assertThatThrownBy(() -> service.execute(new StockTurnoverQuery(stranger, PERIOD, company)))
				.isInstanceOf(ForbiddenException.class);

		verifyNoInteractions(inventory);
	}

	@Test
	void dividesWhatWasIssuedByTheAverageOfTheOpeningAndClosingStock() {
		UUID product = UUID.randomUUID();
		when(inventory.stockFlows(FROM, TO, company)).thenReturn(List.of(flow(product, "30", "10", "20")));

		List<StockTurnoverEntry> report = service.execute(new StockTurnoverQuery(user, PERIOD, company));

		assertThat(report).singleElement().satisfies(entry -> {
			assertThat(entry.product()).isEqualTo(product);
			assertThat(entry.turnoverRate()).isEqualByComparingTo("2");
			assertThat(entry.stalledFlag()).isFalse();
		});
	}

	@Test
	void flagsAProductThatHeldStockButIssuedNoneAsStalled() {
		UUID product = UUID.randomUUID();
		when(inventory.stockFlows(any(), any(), any())).thenReturn(List.of(flow(product, "0", "40", "40")));

		List<StockTurnoverEntry> report = service.execute(new StockTurnoverQuery(user, PERIOD, null));

		assertThat(report).singleElement().satisfies(entry -> {
			assertThat(entry.turnoverRate()).isEqualByComparingTo("0");
			assertThat(entry.stalledFlag()).isTrue();
		});
	}

	@Test
	void leavesTheRateEmptyWhenNoStockWasHeldAtEitherEndOfThePeriodButSomethingWasIssued() {
		UUID product = UUID.randomUUID();
		when(inventory.stockFlows(any(), any(), any())).thenReturn(List.of(flow(product, "5", "0", "0")));

		List<StockTurnoverEntry> report = service.execute(new StockTurnoverQuery(user, PERIOD, null));

		assertThat(report).singleElement().satisfies(entry -> {
			assertThat(entry.turnoverRate()).isNull();
			assertThat(entry.stalledFlag()).isFalse();
		});
	}

	@Test
	void leavesOutProductsThatNeitherHeldStockNorIssuedAny() {
		when(inventory.stockFlows(any(), any(), any())).thenReturn(
				List.of(flow(UUID.randomUUID(), "0", "0", "0"), flow(UUID.randomUUID(), "0", "-3", "-3")));

		assertThat(service.execute(new StockTurnoverQuery(user, PERIOD, null))).isEmpty();
	}

	@Test
	void ranksTheFastestMoversFirstWithStalledAndUnratedProductsLast() {
		UUID slow = UUID.fromString("00000000-0000-0000-0000-000000000001");
		UUID fast = UUID.fromString("00000000-0000-0000-0000-000000000002");
		UUID stalled = UUID.fromString("00000000-0000-0000-0000-000000000003");
		UUID unrated = UUID.fromString("00000000-0000-0000-0000-000000000004");
		when(inventory.stockFlows(any(), any(), any())).thenReturn(List.of(flow(unrated, "5", "0", "0"),
				flow(stalled, "0", "10", "10"), flow(slow, "1", "10", "10"), flow(fast, "30", "10", "10")));

		List<StockTurnoverEntry> report = service.execute(new StockTurnoverQuery(user, PERIOD, null));

		assertThat(report).extracting(StockTurnoverEntry::product).containsExactly(fast, slow, stalled, unrated);
		verify(inventory).stockFlows(FROM, TO, null);
	}

	private StockFlow flow(UUID product, String issued, String opening, String closing) {
		return new StockFlow(product, new BigDecimal(issued), new BigDecimal(opening), new BigDecimal(closing));
	}
}
