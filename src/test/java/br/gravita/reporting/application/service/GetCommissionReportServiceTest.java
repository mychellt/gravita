package br.gravita.reporting.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.exceptions.ForbiddenException;
import br.gravita.core.domain.system.UserId;
import br.gravita.core.ports.inbound.reporting.CommissionReportEntry;
import br.gravita.core.ports.inbound.reporting.CommissionReportQuery;
import br.gravita.core.ports.outbound.reporting.PermissionCheckPort;
import br.gravita.core.ports.outbound.reporting.SalesReadModelPort;
import br.gravita.core.ports.outbound.reporting.SalesReadModelPort.CommissionRecord;
import br.gravita.core.usercases.reporting.GetCommissionReportService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GetCommissionReportServiceTest {

	private static final YearMonth PERIOD = YearMonth.of(2028, 2);
	private static final LocalDate FROM = LocalDate.of(2028, 2, 1);
	private static final LocalDate TO = LocalDate.of(2028, 2, 29);

	private static final UUID ANA = UUID.fromString("00000000-0000-0000-0000-00000000000a");
	private static final UUID BRUNO = UUID.fromString("00000000-0000-0000-0000-00000000000b");
	private static final UUID RICE = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
	private static final UUID BEANS = UUID.fromString("00000000-0000-0000-0000-0000000000a2");
	private static final UUID ORDER_1 = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
	private static final UUID ORDER_2 = UUID.fromString("00000000-0000-0000-0000-0000000000b2");

	private final SalesReadModelPort sales = mock(SalesReadModelPort.class);
	private final PermissionCheckPort permissions = mock(PermissionCheckPort.class);

	private final UserId user = UserId.generate();
	private GetCommissionReportService service;

	@BeforeEach
	void setUp() {
		service = new GetCommissionReportService(sales, permissions);
		when(permissions.canView(user, "commissions")).thenReturn(true);
		when(sales.commissions(any(), any(), any())).thenReturn(List.of());
	}

	@DisplayName("Refuses a user whose profile cannot view the report, without reading any data")
	@Test
	void refusesAUserWhoseProfileCannotViewTheReportWithoutReadingAnything() {
		UserId stranger = UserId.generate();
		when(permissions.canView(stranger, "commissions")).thenReturn(false);

		assertThatThrownBy(() -> service.execute(new CommissionReportQuery(stranger, null, PERIOD)))
				.isInstanceOf(ForbiddenException.class);

		verifyNoInteractions(sales);
	}

	@DisplayName("Projects the commissions of the whole requested month")
	@Test
	void projectsTheCommissionsOfTheWholeRequestedMonth() {
		when(sales.commissions(FROM, TO, null)).thenReturn(List.of(record(ANA, RICE, ORDER_1, "0.0500", "45.0000")));

		List<CommissionReportEntry> report = service.execute(new CommissionReportQuery(user, null, PERIOD));

		assertThat(report).singleElement().satisfies(entry -> {
			assertThat(entry.salespersonId()).isEqualTo(ANA);
			assertThat(entry.productId()).isEqualTo(RICE);
			assertThat(entry.orderId()).isEqualTo(ORDER_1);
			assertThat(entry.rate()).isEqualByComparingTo("0.05");
			assertThat(entry.amount()).isEqualByComparingTo("45");
		});
	}

	@DisplayName("Passes the salesperson filter to the read model")
	@Test
	void passesTheSalespersonFilterToTheReadModel() {
		service.execute(new CommissionReportQuery(user, ANA, PERIOD));

		verify(sales).commissions(FROM, TO, ANA);
	}

	@DisplayName("Groups the lines by salesperson, then product, then order")
	@Test
	void groupsTheLinesBySalespersonThenProductThenOrder() {
		when(sales.commissions(any(), any(), any())).thenReturn(List.of(record(BRUNO, RICE, ORDER_1, "0.1", "10"),
				record(ANA, BEANS, ORDER_2, "0.1", "20"), record(ANA, RICE, ORDER_2, "0.1", "30"),
				record(ANA, BEANS, ORDER_1, "0.1", "40")));

		List<CommissionReportEntry> report = service.execute(new CommissionReportQuery(user, null, PERIOD));

		assertThat(report).extracting(CommissionReportEntry::salespersonId, CommissionReportEntry::productId,
				CommissionReportEntry::orderId).containsExactly(
						tuple(ANA, RICE, ORDER_2),
						tuple(ANA, BEANS, ORDER_1),
						tuple(ANA, BEANS, ORDER_2),
						tuple(BRUNO, RICE, ORDER_1));
	}

	@DisplayName("Returns an empty report when nothing was commissioned in the period")
	@Test
	void answersAnEmptyReportWhenNothingWasCommissionedInThePeriod() {
		assertThat(service.execute(new CommissionReportQuery(user, null, PERIOD))).isEmpty();
	}

	private CommissionRecord record(UUID salesperson, UUID product, UUID order, String rate, String amount) {
		return new CommissionRecord(salesperson, product, order, new BigDecimal(rate), new BigDecimal(amount));
	}
}
