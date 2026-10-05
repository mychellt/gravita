package br.gravita.finance.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.finance.CustomerStatement;
import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.ReceivableOrigin;
import br.gravita.core.domain.finance.ReceivableStatus;
import br.gravita.core.domain.finance.Renegotiation;
import br.gravita.core.domain.finance.RenegotiationId;
import br.gravita.core.domain.finance.Settlement;
import br.gravita.core.domain.finance.SettlementId;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.finance.GetCustomerStatementQuery;
import br.gravita.core.ports.outbound.persistence.finance.ReceivableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.RenegotiationRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.SettlementRepositoryPort;
import br.gravita.core.usercases.finance.GetCustomerStatementService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetCustomerStatementServiceTest {

	@Mock
	private ReceivableRepositoryPort receivableRepositoryPort;

	@Mock
	private SettlementRepositoryPort settlementRepositoryPort;

	@Mock
	private RenegotiationRepositoryPort renegotiationRepositoryPort;

	private GetCustomerStatementService service;

	private final UUID customerId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		service = new GetCustomerStatementService(receivableRepositoryPort, settlementRepositoryPort,
				renegotiationRepositoryPort, Clock.systemUTC());
	}

	private Receivable receivable(final String amount, final String dueDate, final ReceivableStatus status) {
		return Receivable.of(ReceivableId.of(UUID.randomUUID()), customerId, ReceivableOrigin.MANUAL,
				new BigDecimal(amount), LocalDate.parse(dueDate), null, status, null, null);
	}

	private Settlement settlement(final Receivable receivable, final String amount, final String discount, final String at) {
		return Settlement.manual(SettlementId.of(UUID.randomUUID()), receivable.getId(), new BigDecimal(amount), null,
				null, discount == null ? null : new BigDecimal(discount), null, Instant.parse(at));
	}

	private Renegotiation renegotiation(final Receivable original, final Receivable replacement, final String at) {
		return Renegotiation.create(RenegotiationId.of(UUID.randomUUID()), customerId, List.of(original.getId()),
				List.of(replacement.getId()), Instant.parse(at));
	}

	private void given(final List<Receivable> receivables, final List<Settlement> settlements,
			final List<Renegotiation> renegotiations) {
		when(receivableRepositoryPort.findByCustomerId(customerId)).thenReturn(receivables);
		when(settlementRepositoryPort.findByReceivableIds(receivables.stream().map(Receivable::getId).toList()))
				.thenReturn(settlements);
		when(renegotiationRepositoryPort.findByCustomerId(customerId)).thenReturn(renegotiations);
	}

	private CustomerStatement statement(final LocalDate from, final LocalDate to) {
		return service.execute(new GetCustomerStatementQuery(customerId, from, to));
	}

	@Test
	@DisplayName("Lists titles, settlements and renegotiations in chronological order")
	void listsTitlesSettlementsAndRenegotiationsInChronologicalOrder() {
		final Receivable late = receivable("100.00", "2026-03-10", ReceivableStatus.OPEN);
		final Receivable early = receivable("50.00", "2026-01-10", ReceivableStatus.SETTLED);
		final Receivable replacement = receivable("60.00", "2026-05-10", ReceivableStatus.OPEN);
		final Settlement second = settlement(early, "20.00", null, "2026-02-05T10:00:00Z");
		final Settlement first = settlement(early, "30.00", null, "2026-01-08T10:00:00Z");
		final Renegotiation later = renegotiation(late, replacement, "2026-04-01T10:00:00Z");
		final Renegotiation earlier = renegotiation(early, replacement, "2026-03-01T10:00:00Z");
		given(List.of(late, early, replacement), List.of(second, first), List.of(later, earlier));

		final CustomerStatement statement = statement(null, null);

		assertThat(statement.titles()).containsExactly(early, late, replacement);
		assertThat(statement.settlements()).containsExactly(first, second);
		assertThat(statement.renegotiations()).containsExactly(earlier, later);
	}

	@Test
	@DisplayName("Computes the open balance from what is still owed on outstanding titles only")
	void theOpenBalanceIsWhatIsStillOwedOnTheOutstandingTitlesOnly() {
		final Receivable open = receivable("100.00", "2026-03-10", ReceivableStatus.OPEN);
		final Receivable partial = receivable("200.00", "2026-03-11", ReceivableStatus.PARTIALLY_SETTLED);
		final Receivable settled = receivable("300.00", "2026-03-12", ReceivableStatus.SETTLED);
		final Receivable renegotiated = receivable("400.00", "2026-03-13", ReceivableStatus.RENEGOTIATED);
		final Receivable cancelled = receivable("500.00", "2026-03-14", ReceivableStatus.CANCELLED);
		given(List.of(open, partial, settled, renegotiated, cancelled),
				List.of(settlement(partial, "50.00", "10.00", "2026-03-20T10:00:00Z"),
						settlement(settled, "300.00", null, "2026-03-20T10:00:00Z")),
				List.of());

		assertThat(statement(null, null).openBalance()).isEqualByComparingTo("240.00");
	}

	@Test
	@DisplayName("Gives a customer without titles an empty statement and a zero balance")
	void customerWithoutTitlesHasAnEmptyStatementAndZeroBalance() {
		given(List.of(), List.of(), List.of());

		final CustomerStatement statement = statement(null, null);

		assertThat(statement.titles()).isEmpty();
		assertThat(statement.settlements()).isEmpty();
		assertThat(statement.renegotiations()).isEmpty();
		assertThat(statement.openBalance()).isEqualByComparingTo("0");
	}

	@Test
	@DisplayName("Limits the lists to the period, inclusive of its ends, without affecting the open balance")
	void thePeriodLimitsTheListsInclusivelyButNotTheOpenBalance() {
		final Receivable before = receivable("10.00", "2026-01-31", ReceivableStatus.OPEN);
		final Receivable first = receivable("20.00", "2026-02-01", ReceivableStatus.OPEN);
		final Receivable last = receivable("40.00", "2026-02-28", ReceivableStatus.OPEN);
		final Receivable after = receivable("80.00", "2026-03-01", ReceivableStatus.OPEN);
		final Settlement outside = settlement(before, "1.00", null, "2026-01-31T23:00:00Z");
		final Settlement inside = settlement(first, "2.00", null, "2026-02-28T23:00:00Z");
		final Renegotiation inPeriod = renegotiation(first, after, "2026-02-15T10:00:00Z");
		final Renegotiation outOfPeriod = renegotiation(before, after, "2026-03-01T00:00:00Z");
		given(List.of(before, first, last, after), List.of(outside, inside), List.of(inPeriod, outOfPeriod));

		final CustomerStatement statement = statement(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));

		assertThat(statement.titles()).containsExactly(first, last);
		assertThat(statement.settlements()).containsExactly(inside);
		assertThat(statement.renegotiations()).containsExactly(inPeriod);
		assertThat(statement.openBalance()).isEqualByComparingTo("147.00");
		assertThat(statement.from()).isEqualTo(LocalDate.of(2026, 2, 1));
		assertThat(statement.to()).isEqualTo(LocalDate.of(2026, 2, 28));
	}

	@Test
	@DisplayName("Rejects a period that ends before it starts")
	void periodThatEndsBeforeItStartsIsRejected() {
		assertThatThrownBy(() -> statement(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 2, 1)))
				.isInstanceOf(BusinessRuleException.class);
	}
}
