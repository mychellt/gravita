package br.gravita.adapters.outbound.persistence.adapters.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.Context;
import br.gravita.core.domain.CustomerDomain;
import br.gravita.core.domain.CustomerStatus;
import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.ReceivableOrigin;
import br.gravita.core.domain.finance.ReceivableStatus;
import br.gravita.core.domain.finance.Settlement;
import br.gravita.core.domain.finance.SettlementId;
import br.gravita.core.ports.business.SetCustomerCreditStatusPort;
import br.gravita.core.ports.outbound.persistence.CustomerRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.ReceivableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.SettlementRepositoryPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateCustomerCreditStatusAdapterTest {

	@Mock
	private ReceivableRepositoryPort receivableRepositoryPort;

	@Mock
	private SettlementRepositoryPort settlementRepositoryPort;

	@Mock
	private CustomerRepositoryPort customerRepositoryPort;

	@Mock
	private SetCustomerCreditStatusPort setCustomerCreditStatusPort;

	@InjectMocks
	private UpdateCustomerCreditStatusAdapter adapter;

	private final UUID customerId = UUID.randomUUID();

	private void customerWithLimit(String creditLimit) {
		CustomerDomain customer = CustomerDomain.builder().id(customerId)
				.creditLimit(creditLimit == null ? null : new BigDecimal(creditLimit)).build();
		when(customerRepositoryPort.get(customerId)).thenReturn(Optional.of(customer));
	}

	private Receivable unsettled(String amount, LocalDate dueDate, String alreadyCredited) {
		Receivable receivable = Receivable.of(ReceivableId.of(UUID.randomUUID()), customerId, ReceivableOrigin.MANUAL,
				new BigDecimal(amount), dueDate, null, ReceivableStatus.OPEN, null, null);
		List<Settlement> settlements = alreadyCredited == null ? List.of()
				: List.of(Settlement.manual(SettlementId.of(UUID.randomUUID()), receivable.getId(),
						new BigDecimal(alreadyCredited), null, null, null, null, Instant.now()));
		lenient().when(settlementRepositoryPort.findByReceivableId(receivable.getId())).thenReturn(settlements);
		return receivable;
	}

	private void unsettledTitles(Receivable... receivables) {
		when(receivableRepositoryPort.findUnsettledByCustomerId(customerId)).thenReturn(List.of(receivables));
	}

	private CustomerDomain pushed() {
		ArgumentCaptor<Context> captured = ArgumentCaptor.forClass(Context.class);
		verify(setCustomerCreditStatusPort).execute(captured.capture());
		return captured.getValue().getData(CustomerDomain.class);
	}

	@Test
	void theBalanceIsWhatIsStillOwedOnTheUnsettledTitlesAndTheStatusIsRegularWhenNothingIsOverdue() {
		customerWithLimit("1000.00");
		unsettledTitles(unsettled("100.00", LocalDate.now().plusDays(5), "40.00"),
				unsettled("50.00", LocalDate.now(), null));

		adapter.update(customerId);

		CustomerDomain position = pushed();
		assertThat(position.getId()).isEqualTo(customerId);
		assertThat(position.getCurrentBalance()).isEqualByComparingTo("110.00");
		assertThat(position.getStatus()).isEqualTo(CustomerStatus.REGULAR);
	}

	@Test
	void aCustomerWithNothingLeftToPayIsRegularWithAZeroBalance() {
		customerWithLimit("1000.00");
		unsettledTitles();

		adapter.update(customerId);

		CustomerDomain position = pushed();
		assertThat(position.getCurrentBalance()).isEqualByComparingTo("0");
		assertThat(position.getStatus()).isEqualTo(CustomerStatus.REGULAR);
	}

	@Test
	void anOverdueTitleMakesTheCustomerDelinquent() {
		customerWithLimit("1000.00");
		unsettledTitles(unsettled("100.00", LocalDate.now().minusDays(1), null));

		adapter.update(customerId);

		assertThat(pushed().getStatus()).isEqualTo(CustomerStatus.DELINQUENT);
	}

	@Test
	void exceedingAPositiveCreditLimitBlocksTheCustomerEvenWhenNothingIsOverdue() {
		customerWithLimit("100.00");
		unsettledTitles(unsettled("100.01", LocalDate.now().plusDays(5), null));

		adapter.update(customerId);

		assertThat(pushed().getStatus()).isEqualTo(CustomerStatus.BLOCKED);
	}

	@Test
	void aMissingOrZeroCreditLimitIsNotEnforced() {
		for (String limit : new String[] { null, "0.00" }) {
			org.mockito.Mockito.reset(setCustomerCreditStatusPort);
			customerWithLimit(limit);
			unsettledTitles(unsettled("500.00", LocalDate.now().plusDays(5), null));

			adapter.update(customerId);

			assertThat(pushed().getStatus()).isEqualTo(CustomerStatus.REGULAR);
		}
	}

	@Test
	void anUnknownCustomerIsSkipped() {
		when(customerRepositoryPort.get(customerId)).thenReturn(Optional.empty());

		assertThatCode(() -> adapter.update(customerId)).doesNotThrowAnyException();
		verify(setCustomerCreditStatusPort, never()).execute(any());
	}

	@Test
	void aFailureNeverPropagatesToTheSettlementFlow() {
		when(customerRepositoryPort.get(customerId)).thenThrow(new IllegalStateException("db down"));

		assertThatCode(() -> adapter.update(customerId)).doesNotThrowAnyException();
	}
}
