package br.gravita.finance.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gravita.core.domain.finance.LedgerScope;
import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.ReceivableOrigin;
import br.gravita.core.domain.finance.ReceivableStatus;
import br.gravita.core.domain.finance.Settlement;
import br.gravita.core.domain.finance.SettlementId;
import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReceivableTest {

	@Test
	@DisplayName("Starts a manually created receivable as open with the manual origin")
	void manuallyCreatedReceivableStartsOpenWithManualOrigin() {
		final UUID customerId = UUID.randomUUID();
		final LocalDate dueDate = LocalDate.now().plusDays(30);

		final Receivable receivable = Receivable.createManual(ReceivableId.of(UUID.randomUUID()), customerId,
				BigDecimal.TEN, dueDate, null);

		assertThat(receivable.getOrigin()).isEqualTo(ReceivableOrigin.MANUAL);
		assertThat(receivable.getStatus()).isEqualTo(ReceivableStatus.OPEN);
		assertThat(receivable.getCustomerId()).isEqualTo(customerId);
		assertThat(receivable.getAmount()).isEqualByComparingTo(BigDecimal.TEN);
		assertThat(receivable.getDueDate()).isEqualTo(dueDate);
		assertThat(receivable.getInstallments()).isNull();
	}

	@Test
	@DisplayName("Rejects a missing amount")
	void rejectsAMissingAmount() {
		assertThatThrownBy(() -> Receivable.createManual(ReceivableId.of(UUID.randomUUID()), UUID.randomUUID(), null,
				LocalDate.now().plusDays(1), null))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("amount is required");
	}

	@Test
	@DisplayName("Rejects an amount that is zero or negative")
	void rejectsAZeroOrNegativeAmount() {
		assertThatThrownBy(() -> Receivable.createManual(ReceivableId.of(UUID.randomUUID()), UUID.randomUUID(),
				BigDecimal.ZERO, LocalDate.now().plusDays(1), null))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("must be positive");
	}

	@Test
	@DisplayName("Rejects a missing due date")
	void rejectsAMissingDueDate() {
		assertThatThrownBy(() -> Receivable.createManual(ReceivableId.of(UUID.randomUUID()), UUID.randomUUID(),
				BigDecimal.TEN, null, null))
				.isInstanceOf(NullPointerException.class);
	}

	@Test
	@DisplayName("Rejects fewer than one installment")
	void rejectsFewerThanOneInstallment() {
		assertThatThrownBy(() -> Receivable.createManual(ReceivableId.of(UUID.randomUUID()), UUID.randomUUID(),
				BigDecimal.TEN, LocalDate.now().plusDays(1), 0))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("installments must be at least 1");
	}

	@Test
	@DisplayName("Passes the open check for an open receivable and fails for any other status")
	void requireOpenPassesForAnOpenReceivableAndFailsOtherwise() {
		final ReceivableId id = ReceivableId.of(UUID.randomUUID());
		final Receivable open = Receivable.createManual(id, UUID.randomUUID(), BigDecimal.TEN,
				LocalDate.now().plusDays(1), null);
		final Receivable cancelled = Receivable.of(id, UUID.randomUUID(), ReceivableOrigin.MANUAL, BigDecimal.TEN,
				LocalDate.now().plusDays(1), null, ReceivableStatus.CANCELLED, null, null);

		open.requireOpen();
		assertThatThrownBy(cancelled::requireOpen).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("not OPEN");
	}

	@Test
	@DisplayName("Moves an open or partially settled receivable to settled")
	void settleMovesAnOpenOrPartiallySettledReceivableToSettled() {
		for (final ReceivableStatus status : new ReceivableStatus[] {ReceivableStatus.OPEN,
				ReceivableStatus.PARTIALLY_SETTLED }) {
			final Receivable receivable = Receivable.of(ReceivableId.of(UUID.randomUUID()), UUID.randomUUID(),
					ReceivableOrigin.MANUAL, BigDecimal.TEN, LocalDate.now().plusDays(1), null, status, null, null);

			final Receivable settled = receivable.settle();

			assertThat(settled.getStatus()).isEqualTo(ReceivableStatus.SETTLED);
			assertThat(settled.getId()).isEqualTo(receivable.getId());
			assertThat(settled.getAmount()).isEqualByComparingTo(BigDecimal.TEN);
		}
	}

	@Test
	@DisplayName("Rejects settling a receivable that cannot be settled")
	void settleRejectsAReceivableThatCannotBeSettled() {
		for (final ReceivableStatus status : new ReceivableStatus[] {ReceivableStatus.SETTLED,
				ReceivableStatus.CANCELLED, ReceivableStatus.RENEGOTIATED }) {
			final Receivable receivable = Receivable.of(ReceivableId.of(UUID.randomUUID()), UUID.randomUUID(),
					ReceivableOrigin.MANUAL, BigDecimal.TEN, LocalDate.now().plusDays(1), null, status, null, null);

			assertThatThrownBy(receivable::settle).isInstanceOf(BusinessRuleException.class)
					.hasMessageContaining("cannot be settled");
		}
	}

	private Receivable receivable(final ReceivableStatus status) {
		return Receivable.of(ReceivableId.of(UUID.randomUUID()), UUID.randomUUID(), ReceivableOrigin.MANUAL,
				new BigDecimal("100.00"), LocalDate.now().plusDays(1), null, status, null, null);
	}

	@Test
	@DisplayName("Settles the receivable when the credited total fully covers it")
	void applyCreditedTotalSettlesWhenTheTitleIsFullyCovered() {
		assertThat(receivable(ReceivableStatus.OPEN).applyCreditedTotal(new BigDecimal("100.00")).getStatus())
				.isEqualTo(ReceivableStatus.SETTLED);
		assertThat(receivable(ReceivableStatus.PARTIALLY_SETTLED).applyCreditedTotal(new BigDecimal("120.00"))
				.getStatus()).isEqualTo(ReceivableStatus.SETTLED);
	}

	@Test
	@DisplayName("Partially settles the receivable when the credited total does not cover it")
	void applyCreditedTotalPartiallySettlesWhenTheTitleIsNotCovered() {
		for (final ReceivableStatus status : new ReceivableStatus[] {ReceivableStatus.OPEN,
				ReceivableStatus.PARTIALLY_SETTLED }) {
			assertThat(receivable(status).applyCreditedTotal(new BigDecimal("40.00")).getStatus())
					.isEqualTo(ReceivableStatus.PARTIALLY_SETTLED);
		}
	}

	@Test
	@DisplayName("Rejects a credited total on a receivable that cannot take a payment")
	void applyCreditedTotalRejectsAReceivableThatCannotTakeAPayment() {
		for (final ReceivableStatus status : new ReceivableStatus[] {ReceivableStatus.SETTLED,
				ReceivableStatus.CANCELLED, ReceivableStatus.RENEGOTIATED }) {
			final Receivable receivable = receivable(status);

			assertThatThrownBy(() -> receivable.applyCreditedTotal(new BigDecimal("40.00")))
					.isInstanceOf(BusinessRuleException.class);
			assertThatThrownBy(() -> receivable.applyCreditedTotal(new BigDecimal("100.00")))
					.isInstanceOf(BusinessRuleException.class);
		}
	}

	@Test
	@DisplayName("Rejects a credited total that is zero or negative")
	void applyCreditedTotalRejectsANonPositiveTotal() {
		assertThatThrownBy(() -> receivable(ReceivableStatus.OPEN).applyCreditedTotal(BigDecimal.ZERO))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Computes the remaining balance as the amount minus what the settlements credited")
	void remainingBalanceIsTheAmountLessWhatTheSettlementsCredited() {
		final Receivable receivable = receivable(ReceivableStatus.PARTIALLY_SETTLED);
		final Settlement first = Settlement.manual(SettlementId.of(UUID.randomUUID()), receivable.getId(),
				new BigDecimal("30.00"), new BigDecimal("5.00"), null, new BigDecimal("10.00"), null, Instant.now());
		final Settlement second = Settlement.manual(SettlementId.of(UUID.randomUUID()), receivable.getId(),
				new BigDecimal("20.00"), null, null, null, null, Instant.now());

		assertThat(receivable.remainingBalance(List.of())).isEqualByComparingTo("100.00");
		assertThat(receivable.remainingBalance(List.of(first, second))).isEqualByComparingTo("40.00");
	}

	@Test
	@DisplayName("Keeps the scope through every status transition")
	void theScopeSurvivesEveryStatusTransition() {
		final LedgerScope scope = new LedgerScope(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
		final Receivable open = Receivable.createManual(ReceivableId.of(UUID.randomUUID()), UUID.randomUUID(),
				new BigDecimal("100.00"), LocalDate.now().minusDays(5), null);

		assertThat(open.getScope()).isEqualTo(LedgerScope.NONE);
		final Receivable scoped = open.inScope(scope);
		assertThat(scoped.settle().getScope()).isEqualTo(scope);
		assertThat(scoped.applyCreditedTotal(new BigDecimal("40.00")).getScope()).isEqualTo(scope);
		assertThat(scoped.renegotiate(LocalDate.now()).getScope()).isEqualTo(scope);
	}
}
