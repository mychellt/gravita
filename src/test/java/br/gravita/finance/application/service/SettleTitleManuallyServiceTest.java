package br.gravita.finance.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.gravita.core.domain.exceptions.ResourceNotFoundException;
import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.ReceivableOrigin;
import br.gravita.core.domain.finance.ReceivableStatus;
import br.gravita.core.domain.finance.Settlement;
import br.gravita.core.domain.finance.SettlementId;
import br.gravita.core.domain.finance.SettlementMethod;
import br.gravita.core.domain.shared.BusinessRuleException;
import br.gravita.core.ports.inbound.finance.SettleTitleCommand;
import br.gravita.core.ports.outbound.finance.UpdateCustomerCreditStatusPort;
import br.gravita.core.ports.outbound.persistence.finance.ReceivableRepositoryPort;
import br.gravita.core.ports.outbound.persistence.finance.SettlementRepositoryPort;
import br.gravita.core.usercases.finance.SettleTitleManuallyService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SettleTitleManuallyServiceTest {

	@Mock
	private ReceivableRepositoryPort receivableRepositoryPort;

	@Mock
	private SettlementRepositoryPort settlementRepositoryPort;

	@Mock
	private UpdateCustomerCreditStatusPort updateCustomerCreditStatusPort;

	@InjectMocks
	private SettleTitleManuallyService service;

	private final UUID customerId = UUID.randomUUID();

	private Receivable receivable(final ReceivableStatus status) {
		return Receivable.of(ReceivableId.of(UUID.randomUUID()), customerId, ReceivableOrigin.MANUAL,
				new BigDecimal("100.00"), LocalDate.now().plusDays(30), null, status, null, null);
	}

	private void found(final Receivable receivable, final Settlement... previous) {
		when(receivableRepositoryPort.findById(receivable.getId())).thenReturn(Optional.of(receivable));
		when(settlementRepositoryPort.findByReceivableId(receivable.getId())).thenReturn(List.of(previous));
	}

	private void savesEcho() {
		when(settlementRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
	}

	private SettleTitleCommand command(final Receivable receivable, final String amount, final String discount, final boolean partial) {
		return new SettleTitleCommand(receivable.getId().value(), new BigDecimal(amount), new BigDecimal("2.00"),
				new BigDecimal("1.00"), discount == null ? null : new BigDecimal(discount), new BigDecimal("0.50"),
				partial);
	}

	private Settlement previousManual(final Receivable receivable, final String amount) {
		return Settlement.manual(SettlementId.of(UUID.randomUUID()), receivable.getId(), new BigDecimal(amount), null,
				null, null, null, Instant.now());
	}

	@Test
	@DisplayName("Creates a manual settlement with the entered adjustments and settles the receivable on a full payment")
	void fullSettlementCreatesAManualSettlementWithTheEnteredAdjustmentsAndSettlesTheReceivable() {
		final Receivable receivable = receivable(ReceivableStatus.OPEN);
		found(receivable);
		savesEcho();

		final Settlement settlement = service.execute(command(receivable, "90.00", "10.00", false));

		assertThat(settlement.getMethod()).isEqualTo(SettlementMethod.MANUAL);
		assertThat(settlement.getReceivableId()).isEqualTo(receivable.getId());
		assertThat(settlement.getAmount()).isEqualByComparingTo("90.00");
		assertThat(settlement.getInterest()).isEqualByComparingTo("2.00");
		assertThat(settlement.getFine()).isEqualByComparingTo("1.00");
		assertThat(settlement.getDiscount()).isEqualByComparingTo("10.00");
		assertThat(settlement.getSurcharge()).isEqualByComparingTo("0.50");
		final ArgumentCaptor<Receivable> saved = ArgumentCaptor.forClass(Receivable.class);
		verify(receivableRepositoryPort).save(saved.capture());
		assertThat(saved.getValue().getStatus()).isEqualTo(ReceivableStatus.SETTLED);
		verify(updateCustomerCreditStatusPort).update(customerId);
	}

	@Test
	@DisplayName("Leaves the receivable partially settled after a partial payment")
	void partialSettlementLeavesTheReceivablePartiallySettled() {
		final Receivable receivable = receivable(ReceivableStatus.OPEN);
		found(receivable);
		savesEcho();

		service.execute(command(receivable, "40.00", null, true));

		final ArgumentCaptor<Receivable> saved = ArgumentCaptor.forClass(Receivable.class);
		verify(receivableRepositoryPort).save(saved.capture());
		assertThat(saved.getValue().getStatus()).isEqualTo(ReceivableStatus.PARTIALLY_SETTLED);
		verify(updateCustomerCreditStatusPort).update(customerId);
	}

	@Test
	@DisplayName("Settles a partially settled receivable once the payment covers only what is left")
	void fullSettlementOfAPartiallySettledReceivableOnlyHasToCoverWhatIsLeft() {
		final Receivable receivable = receivable(ReceivableStatus.PARTIALLY_SETTLED);
		found(receivable, previousManual(receivable, "40.00"));
		savesEcho();

		service.execute(command(receivable, "60.00", null, false));

		final ArgumentCaptor<Receivable> saved = ArgumentCaptor.forClass(Receivable.class);
		verify(receivableRepositoryPort).save(saved.capture());
		assertThat(saved.getValue().getStatus()).isEqualTo(ReceivableStatus.SETTLED);
	}

	@Test
	@DisplayName("Keeps the receivable partially settled after a second partial payment that does not clear it")
	void secondPartialSettlementCanStillLeaveTheReceivablePartiallySettled() {
		final Receivable receivable = receivable(ReceivableStatus.PARTIALLY_SETTLED);
		found(receivable, previousManual(receivable, "40.00"));
		savesEcho();

		service.execute(command(receivable, "30.00", null, true));

		final ArgumentCaptor<Receivable> saved = ArgumentCaptor.forClass(Receivable.class);
		verify(receivableRepositoryPort).save(saved.capture());
		assertThat(saved.getValue().getStatus()).isEqualTo(ReceivableStatus.PARTIALLY_SETTLED);
	}

	@Test
	@DisplayName("Rejects a settlement for a receivable that does not exist")
	void rejectsAnUnknownReceivable() {
		final UUID id = UUID.randomUUID();
		when(receivableRepositoryPort.findById(ReceivableId.of(id))).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.execute(
				new SettleTitleCommand(id, BigDecimal.TEN, null, null, null, null, true)))
				.isInstanceOf(ResourceNotFoundException.class);
		verifyNoInteractions(settlementRepositoryPort, updateCustomerCreditStatusPort);
	}

	@Test
	@DisplayName("Rejects a settlement for a receivable that is not open")
	void rejectsAReceivableThatIsNotOpen() {
		for (final ReceivableStatus status : new ReceivableStatus[] {ReceivableStatus.SETTLED, ReceivableStatus.CANCELLED,
				ReceivableStatus.RENEGOTIATED }) {
			final Receivable receivable = receivable(status);
			when(receivableRepositoryPort.findById(receivable.getId())).thenReturn(Optional.of(receivable));

			assertThatThrownBy(() -> service.execute(command(receivable, "40.00", null, true)))
					.isInstanceOf(BusinessRuleException.class).hasMessageContaining("cannot be settled");
		}
		verify(settlementRepositoryPort, never()).save(any());
		verifyNoInteractions(updateCustomerCreditStatusPort);
	}

	@Test
	@DisplayName("Rejects a settlement that would clear more than what is still owed")
	void rejectsASettlementThatWouldClearMoreThanIsLeft() {
		final Receivable receivable = receivable(ReceivableStatus.PARTIALLY_SETTLED);
		found(receivable, previousManual(receivable, "40.00"));

		assertThatThrownBy(() -> service.execute(command(receivable, "61.00", null, false)))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("exceeds the remaining balance");
		assertThatThrownBy(() -> service.execute(command(receivable, "55.00", "6.00", true)))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("exceeds the remaining balance");
		verify(settlementRepositoryPort, never()).save(any());
	}

	@Test
	@DisplayName("Rejects a partial settlement that covers the whole balance and a full one that does not")
	void rejectsAPartialSettlementThatCoversTheWholeBalanceAndAFullOneThatDoesNot() {
		final Receivable receivable = receivable(ReceivableStatus.OPEN);
		found(receivable);

		assertThatThrownBy(() -> service.execute(command(receivable, "100.00", null, true)))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("Partial settlement");
		assertThatThrownBy(() -> service.execute(command(receivable, "99.99", null, false)))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("Full settlement");
		verify(settlementRepositoryPort, never()).save(any());
		verifyNoInteractions(updateCustomerCreditStatusPort);
	}

	@Test
	@DisplayName("Rejects negative adjustments on a settlement")
	void rejectsNegativeAdjustments() {
		final Receivable receivable = receivable(ReceivableStatus.OPEN);
		when(receivableRepositoryPort.findById(receivable.getId())).thenReturn(Optional.of(receivable));

		assertThatThrownBy(() -> service.execute(new SettleTitleCommand(receivable.getId().value(),
				new BigDecimal("40.00"), new BigDecimal("-1.00"), null, null, null, true)))
				.isInstanceOf(BusinessRuleException.class);
		verifyNoInteractions(updateCustomerCreditStatusPort);
	}
}
