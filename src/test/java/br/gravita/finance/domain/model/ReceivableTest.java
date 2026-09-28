package br.gravita.finance.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import org.junit.jupiter.api.Test;

class ReceivableTest {

	@Test
	void aManuallyCreatedReceivableStartsOpenWithManualOrigin() {
		UUID customerId = UUID.randomUUID();
		LocalDate dueDate = LocalDate.now().plusDays(30);

		Receivable receivable = Receivable.createManual(ReceivableId.of(UUID.randomUUID()), customerId,
				BigDecimal.TEN, dueDate, null);

		assertThat(receivable.getOrigin()).isEqualTo(ReceivableOrigin.MANUAL);
		assertThat(receivable.getStatus()).isEqualTo(ReceivableStatus.OPEN);
		assertThat(receivable.getCustomerId()).isEqualTo(customerId);
		assertThat(receivable.getAmount()).isEqualByComparingTo(BigDecimal.TEN);
		assertThat(receivable.getDueDate()).isEqualTo(dueDate);
		assertThat(receivable.getInstallments()).isNull();
	}

	@Test
	void rejectsAMissingAmount() {
		assertThatThrownBy(() -> Receivable.createManual(ReceivableId.of(UUID.randomUUID()), UUID.randomUUID(), null,
				LocalDate.now().plusDays(1), null))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("amount is required");
	}

	@Test
	void rejectsAZeroOrNegativeAmount() {
		assertThatThrownBy(() -> Receivable.createManual(ReceivableId.of(UUID.randomUUID()), UUID.randomUUID(),
				BigDecimal.ZERO, LocalDate.now().plusDays(1), null))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("must be positive");
	}

	@Test
	void rejectsAMissingDueDate() {
		assertThatThrownBy(() -> Receivable.createManual(ReceivableId.of(UUID.randomUUID()), UUID.randomUUID(),
				BigDecimal.TEN, null, null))
				.isInstanceOf(NullPointerException.class);
	}

	@Test
	void rejectsFewerThanOneInstallment() {
		assertThatThrownBy(() -> Receivable.createManual(ReceivableId.of(UUID.randomUUID()), UUID.randomUUID(),
				BigDecimal.TEN, LocalDate.now().plusDays(1), 0))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("installments must be at least 1");
	}

	@Test
	void requireOpenPassesForAnOpenReceivableAndFailsOtherwise() {
		ReceivableId id = ReceivableId.of(UUID.randomUUID());
		Receivable open = Receivable.createManual(id, UUID.randomUUID(), BigDecimal.TEN,
				LocalDate.now().plusDays(1), null);
		Receivable cancelled = Receivable.of(id, UUID.randomUUID(), ReceivableOrigin.MANUAL, BigDecimal.TEN,
				LocalDate.now().plusDays(1), null, ReceivableStatus.CANCELLED, null, null);

		open.requireOpen();
		assertThatThrownBy(cancelled::requireOpen).isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("not OPEN");
	}

	@Test
	void settleMovesAnOpenOrPartiallySettledReceivableToSettled() {
		for (ReceivableStatus status : new ReceivableStatus[] { ReceivableStatus.OPEN,
				ReceivableStatus.PARTIALLY_SETTLED }) {
			Receivable receivable = Receivable.of(ReceivableId.of(UUID.randomUUID()), UUID.randomUUID(),
					ReceivableOrigin.MANUAL, BigDecimal.TEN, LocalDate.now().plusDays(1), null, status, null, null);

			Receivable settled = receivable.settle();

			assertThat(settled.getStatus()).isEqualTo(ReceivableStatus.SETTLED);
			assertThat(settled.getId()).isEqualTo(receivable.getId());
			assertThat(settled.getAmount()).isEqualByComparingTo(BigDecimal.TEN);
		}
	}

	@Test
	void settleRejectsAReceivableThatCannotBeSettled() {
		for (ReceivableStatus status : new ReceivableStatus[] { ReceivableStatus.SETTLED,
				ReceivableStatus.CANCELLED, ReceivableStatus.RENEGOTIATED }) {
			Receivable receivable = Receivable.of(ReceivableId.of(UUID.randomUUID()), UUID.randomUUID(),
					ReceivableOrigin.MANUAL, BigDecimal.TEN, LocalDate.now().plusDays(1), null, status, null, null);

			assertThatThrownBy(receivable::settle).isInstanceOf(BusinessRuleException.class)
					.hasMessageContaining("cannot be settled");
		}
	}

	private Receivable receivable(ReceivableStatus status) {
		return Receivable.of(ReceivableId.of(UUID.randomUUID()), UUID.randomUUID(), ReceivableOrigin.MANUAL,
				new BigDecimal("100.00"), LocalDate.now().plusDays(1), null, status, null, null);
	}

	@Test
	void applyCreditedTotalSettlesWhenTheTitleIsFullyCovered() {
		assertThat(receivable(ReceivableStatus.OPEN).applyCreditedTotal(new BigDecimal("100.00")).getStatus())
				.isEqualTo(ReceivableStatus.SETTLED);
		assertThat(receivable(ReceivableStatus.PARTIALLY_SETTLED).applyCreditedTotal(new BigDecimal("120.00"))
				.getStatus()).isEqualTo(ReceivableStatus.SETTLED);
	}

	@Test
	void applyCreditedTotalPartiallySettlesWhenTheTitleIsNotCovered() {
		for (ReceivableStatus status : new ReceivableStatus[] { ReceivableStatus.OPEN,
				ReceivableStatus.PARTIALLY_SETTLED }) {
			assertThat(receivable(status).applyCreditedTotal(new BigDecimal("40.00")).getStatus())
					.isEqualTo(ReceivableStatus.PARTIALLY_SETTLED);
		}
	}

	@Test
	void applyCreditedTotalRejectsAReceivableThatCannotTakeAPayment() {
		for (ReceivableStatus status : new ReceivableStatus[] { ReceivableStatus.SETTLED,
				ReceivableStatus.CANCELLED, ReceivableStatus.RENEGOTIATED }) {
			Receivable receivable = receivable(status);

			assertThatThrownBy(() -> receivable.applyCreditedTotal(new BigDecimal("40.00")))
					.isInstanceOf(BusinessRuleException.class);
			assertThatThrownBy(() -> receivable.applyCreditedTotal(new BigDecimal("100.00")))
					.isInstanceOf(BusinessRuleException.class);
		}
	}

	@Test
	void applyCreditedTotalRejectsANonPositiveTotal() {
		assertThatThrownBy(() -> receivable(ReceivableStatus.OPEN).applyCreditedTotal(BigDecimal.ZERO))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void remainingBalanceIsTheAmountLessWhatTheSettlementsCredited() {
		Receivable receivable = receivable(ReceivableStatus.PARTIALLY_SETTLED);
		Settlement first = Settlement.manual(SettlementId.of(UUID.randomUUID()), receivable.getId(),
				new BigDecimal("30.00"), new BigDecimal("5.00"), null, new BigDecimal("10.00"), null, Instant.now());
		Settlement second = Settlement.manual(SettlementId.of(UUID.randomUUID()), receivable.getId(),
				new BigDecimal("20.00"), null, null, null, null, Instant.now());

		assertThat(receivable.remainingBalance(List.of())).isEqualByComparingTo("100.00");
		assertThat(receivable.remainingBalance(List.of(first, second))).isEqualByComparingTo("40.00");
	}
}
