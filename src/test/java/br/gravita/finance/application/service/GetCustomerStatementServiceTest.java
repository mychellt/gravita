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
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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

	private Receivable receivable(String amount, String dueDate, ReceivableStatus status) {
		return Receivable.of(ReceivableId.of(UUID.randomUUID()), customerId, ReceivableOrigin.MANUAL,
				new BigDecimal(amount), LocalDate.parse(dueDate), null, status, null, null);
	}

	private Settlement settlement(Receivable receivable, String amount, String discount, String at) {
		return Settlement.manual(SettlementId.of(UUID.randomUUID()), receivable.getId(), new BigDecimal(amount), null,
				null, discount == null ? null : new BigDecimal(discount), null, Instant.parse(at));
	}

	private Renegotiation renegotiation(Receivable original, Receivable replacement, String at) {
		return Renegotiation.create(RenegotiationId.of(UUID.randomUUID()), customerId, List.of(original.getId()),
				List.of(replacement.getId()), Instant.parse(at));
	}

	private void given(List<Receivable> receivables, List<Settlement> settlements,
			List<Renegotiation> renegotiations) {
		when(receivableRepositoryPort.findByCustomerId(customerId)).thenReturn(receivables);
		when(settlementRepositoryPort.findByReceivableIds(receivables.stream().map(Receivable::getId).toList()))
				.thenReturn(settlements);
		when(renegotiationRepositoryPort.findByCustomerId(customerId)).thenReturn(renegotiations);
	}

	private CustomerStatement statement(LocalDate from, LocalDate to) {
		return service.execute(new GetCustomerStatementQuery(customerId, from, to));
	}

	@Test
	void listsTitlesSettlementsAndRenegotiationsInChronologicalOrder() {
		Receivable late = receivable("100.00", "2026-03-10", ReceivableStatus.OPEN);
		Receivable early = receivable("50.00", "2026-01-10", ReceivableStatus.SETTLED);
		Receivable replacement = receivable("60.00", "2026-05-10", ReceivableStatus.OPEN);
		Settlement second = settlement(early, "20.00", null, "2026-02-05T10:00:00Z");
		Settlement first = settlement(early, "30.00", null, "2026-01-08T10:00:00Z");
		Renegotiation later = renegotiation(late, replacement, "2026-04-01T10:00:00Z");
		Renegotiation earlier = renegotiation(early, replacement, "2026-03-01T10:00:00Z");
		given(List.of(late, early, replacement), List.of(second, first), List.of(later, earlier));

		CustomerStatement statement = statement(null, null);

		assertThat(statement.titles()).containsExactly(early, late, replacement);
		assertThat(statement.settlements()).containsExactly(first, second);
		assertThat(statement.renegotiations()).containsExactly(earlier, later);
	}

	@Test
	void theOpenBalanceIsWhatIsStillOwedOnTheOutstandingTitlesOnly() {
		Receivable open = receivable("100.00", "2026-03-10", ReceivableStatus.OPEN);
		Receivable partial = receivable("200.00", "2026-03-11", ReceivableStatus.PARTIALLY_SETTLED);
		Receivable settled = receivable("300.00", "2026-03-12", ReceivableStatus.SETTLED);
		Receivable renegotiated = receivable("400.00", "2026-03-13", ReceivableStatus.RENEGOTIATED);
		Receivable cancelled = receivable("500.00", "2026-03-14", ReceivableStatus.CANCELLED);
		given(List.of(open, partial, settled, renegotiated, cancelled),
				List.of(settlement(partial, "50.00", "10.00", "2026-03-20T10:00:00Z"),
						settlement(settled, "300.00", null, "2026-03-20T10:00:00Z")),
				List.of());

		assertThat(statement(null, null).openBalance()).isEqualByComparingTo("240.00");
	}

	@Test
	void aCustomerWithoutTitlesHasAnEmptyStatementAndZeroBalance() {
		given(List.of(), List.of(), List.of());

		CustomerStatement statement = statement(null, null);

		assertThat(statement.titles()).isEmpty();
		assertThat(statement.settlements()).isEmpty();
		assertThat(statement.renegotiations()).isEmpty();
		assertThat(statement.openBalance()).isEqualByComparingTo("0");
	}

	@Test
	void thePeriodLimitsTheListsInclusivelyButNotTheOpenBalance() {
		Receivable before = receivable("10.00", "2026-01-31", ReceivableStatus.OPEN);
		Receivable first = receivable("20.00", "2026-02-01", ReceivableStatus.OPEN);
		Receivable last = receivable("40.00", "2026-02-28", ReceivableStatus.OPEN);
		Receivable after = receivable("80.00", "2026-03-01", ReceivableStatus.OPEN);
		Settlement outside = settlement(before, "1.00", null, "2026-01-31T23:00:00Z");
		Settlement inside = settlement(first, "2.00", null, "2026-02-28T23:00:00Z");
		Renegotiation inPeriod = renegotiation(first, after, "2026-02-15T10:00:00Z");
		Renegotiation outOfPeriod = renegotiation(before, after, "2026-03-01T00:00:00Z");
		given(List.of(before, first, last, after), List.of(outside, inside), List.of(inPeriod, outOfPeriod));

		CustomerStatement statement = statement(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));

		assertThat(statement.titles()).containsExactly(first, last);
		assertThat(statement.settlements()).containsExactly(inside);
		assertThat(statement.renegotiations()).containsExactly(inPeriod);
		assertThat(statement.openBalance()).isEqualByComparingTo("147.00");
		assertThat(statement.from()).isEqualTo(LocalDate.of(2026, 2, 1));
		assertThat(statement.to()).isEqualTo(LocalDate.of(2026, 2, 28));
	}

	@Test
	void aPeriodThatEndsBeforeItStartsIsRejected() {
		assertThatThrownBy(() -> statement(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 2, 1)))
				.isInstanceOf(BusinessRuleException.class);
	}
}
