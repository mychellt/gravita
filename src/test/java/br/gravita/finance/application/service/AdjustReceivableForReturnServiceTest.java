package br.gravita.finance.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.ReceivableOrigin;
import br.gravita.core.domain.finance.ReceivableStatus;
import br.gravita.core.domain.finance.Settlement;
import br.gravita.core.domain.finance.SettlementId;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.finance.AdjustReceivableForReturnCommand;
import br.gravita.core.ports.outbound.persistence.finance.ReceivableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.SettlementRepositoryPort;
import br.gravita.core.usercases.finance.AdjustReceivableForReturnService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdjustReceivableForReturnServiceTest {

	@Mock
	private ReceivableRepositoryPort receivableRepositoryPort;

	@Mock
	private SettlementRepositoryPort settlementRepositoryPort;

	@InjectMocks
	private AdjustReceivableForReturnService service;

	private final UUID salesReturnRef = UUID.randomUUID();

	private Receivable receivable(final ReceivableStatus status) {
		return Receivable.of(ReceivableId.of(UUID.randomUUID()), UUID.randomUUID(), ReceivableOrigin.MANUAL,
				new BigDecimal("100.00"), LocalDate.now().plusDays(30), null, status, null, null);
	}

	private void found(final Receivable receivable, final Settlement... settlements) {
		when(receivableRepositoryPort.findById(receivable.getId())).thenReturn(Optional.of(receivable));
		when(settlementRepositoryPort.findByReceivableId(receivable.getId())).thenReturn(List.of(settlements));
	}

	private void savesEcho() {
		when(receivableRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
	}

	private Settlement settled(final Receivable receivable, final String amount) {
		return Settlement.manual(SettlementId.of(UUID.randomUUID()), receivable.getId(), new BigDecimal(amount), null,
				null, null, null, Instant.now());
	}

	private AdjustReceivableForReturnCommand command(final Receivable receivable, final String returned) {
		return new AdjustReceivableForReturnCommand(receivable.getId().value(), new BigDecimal(returned),
				salesReturnRef);
	}

	@Test
	@DisplayName("Cancels the receivable when the return covers the whole open balance")
	void returnCoveringTheWholeOpenBalanceCancelsTheReceivable() {
		final Receivable receivable = receivable(ReceivableStatus.OPEN);
		found(receivable);
		savesEcho();

		final Receivable result = service.execute(command(receivable, "100.00"));

		assertThat(result.getStatus()).isEqualTo(ReceivableStatus.CANCELLED);
		verify(receivableRepositoryPort).save(result);
	}

	@Test
	@DisplayName("Lowers the amount and keeps the receivable open on a partial return")
	void partialReturnLowersTheAmountAndKeepsTheReceivableOpen() {
		final Receivable receivable = receivable(ReceivableStatus.OPEN);
		found(receivable);
		savesEcho();

		final Receivable result = service.execute(command(receivable, "30.00"));

		assertThat(result.getStatus()).isEqualTo(ReceivableStatus.OPEN);
		assertThat(result.getAmount()).isEqualByComparingTo("70.00");
	}

	@Test
	@DisplayName("Keeps a partially settled receivable partially settled after a partial return")
	void partialReturnKeepsAPartiallySettledReceivablePartiallySettled() {
		final Receivable receivable = receivable(ReceivableStatus.PARTIALLY_SETTLED);
		found(receivable, settled(receivable, "40.00"));
		savesEcho();

		final Receivable result = service.execute(command(receivable, "20.00"));

		assertThat(result.getStatus()).isEqualTo(ReceivableStatus.PARTIALLY_SETTLED);
		assertThat(result.getAmount()).isEqualByComparingTo("80.00");
	}

	@Test
	@DisplayName("Allows returning only the unsettled portion of the receivable")
	void onlyTheUnsettledPortionCanBeReturned() {
		final Receivable receivable = receivable(ReceivableStatus.PARTIALLY_SETTLED);
		found(receivable, settled(receivable, "40.00"));

		assertThatThrownBy(() -> service.execute(command(receivable, "60.01")))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("exceeds the open balance");
		verify(receivableRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Cancels the receivable on a return of the whole unsettled balance without altering what was settled")
	void returnOfTheWholeUnsettledBalanceCancelsWithoutTouchingWhatWasSettled() {
		final Receivable receivable = receivable(ReceivableStatus.PARTIALLY_SETTLED);
		found(receivable, settled(receivable, "40.00"));
		savesEcho();

		final Receivable result = service.execute(command(receivable, "60.00"));

		assertThat(result.getStatus()).isEqualTo(ReceivableStatus.CANCELLED);
		assertThat(result.getAmount()).isEqualByComparingTo("100.00");
	}

	@Test
	@DisplayName("Rejects a return above the open balance")
	void rejectsAReturnAboveTheOpenBalance() {
		final Receivable receivable = receivable(ReceivableStatus.OPEN);
		found(receivable);

		assertThatThrownBy(() -> service.execute(command(receivable, "100.01")))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("exceeds the open balance");
		verify(receivableRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Rejects a returned amount that is zero or negative")
	void rejectsANonPositiveReturnedAmount() {
		final Receivable receivable = receivable(ReceivableStatus.OPEN);
		found(receivable);

		assertThatThrownBy(() -> service.execute(command(receivable, "0.00")))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Rejects a return for a receivable that is no longer outstanding")
	void rejectsAReceivableThatIsNoLongerOutstanding() {
		for (final ReceivableStatus status : List.of(ReceivableStatus.SETTLED, ReceivableStatus.CANCELLED,
				ReceivableStatus.RENEGOTIATED)) {
			final Receivable receivable = receivable(status);
			found(receivable);

			assertThatThrownBy(() -> service.execute(command(receivable, "10.00")))
					.isInstanceOf(BusinessRuleException.class).hasMessageContaining("cannot be adjusted");
		}
		verify(receivableRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Fails when the receivable does not exist")
	void failsWhenTheReceivableDoesNotExist() {
		final UUID id = UUID.randomUUID();
		when(receivableRepositoryPort.findById(ReceivableId.of(id))).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service
				.execute(new AdjustReceivableForReturnCommand(id, new BigDecimal("10.00"), salesReturnRef)))
				.isInstanceOf(ResourceNotFoundException.class);
	}
}
