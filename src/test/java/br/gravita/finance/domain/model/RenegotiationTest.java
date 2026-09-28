package br.gravita.finance.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.ReceivableOrigin;
import br.gravita.core.domain.finance.ReceivableStatus;
import br.gravita.core.domain.finance.Renegotiation;
import br.gravita.core.domain.finance.RenegotiationId;
import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RenegotiationTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 9, 28);

	private static ReceivableId id() {
		return ReceivableId.of(UUID.randomUUID());
	}

	private static Receivable receivable(ReceivableStatus status, LocalDate dueDate) {
		return Receivable.of(id(), UUID.randomUUID(), ReceivableOrigin.MANUAL, BigDecimal.TEN, dueDate, null,
				status, null, null);
	}

	@Test
	void anOverdueOpenOrPartiallySettledReceivableCanBeRenegotiated() {
		for (ReceivableStatus status : new ReceivableStatus[] { ReceivableStatus.OPEN,
				ReceivableStatus.PARTIALLY_SETTLED }) {
			Receivable original = receivable(status, TODAY.minusDays(1));

			Receivable renegotiated = original.renegotiate(TODAY);

			assertThat(renegotiated.getStatus()).isEqualTo(ReceivableStatus.RENEGOTIATED);
			assertThat(renegotiated.getId()).isEqualTo(original.getId());
			assertThat(renegotiated.getAmount()).isEqualByComparingTo(original.getAmount());
			assertThat(renegotiated.getDueDate()).isEqualTo(original.getDueDate());
		}
	}

	@Test
	void aReceivableDueTodayOrLaterIsNotOverdue() {
		assertThatThrownBy(() -> receivable(ReceivableStatus.OPEN, TODAY).renegotiate(TODAY))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("not overdue");
		assertThatThrownBy(() -> receivable(ReceivableStatus.OPEN, TODAY.plusDays(5)).renegotiate(TODAY))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("not overdue");
	}

	@Test
	void aTitleThatIsNotOutstandingCannotBeRenegotiatedEvenIfItsDueDateHasPassed() {
		for (ReceivableStatus status : new ReceivableStatus[] { ReceivableStatus.SETTLED,
				ReceivableStatus.RENEGOTIATED, ReceivableStatus.CANCELLED }) {
			assertThatThrownBy(() -> receivable(status, TODAY.minusDays(30)).renegotiate(TODAY))
					.isInstanceOf(BusinessRuleException.class).hasMessageContaining("not overdue");
		}
	}

	@Test
	void anInstallmentOfARenegotiationPlanIsAnOpenRenegotiationOriginTitle() {
		UUID customerId = UUID.randomUUID();

		Receivable installment = Receivable.createFromRenegotiation(id(), customerId, new BigDecimal("50.00"),
				TODAY.plusDays(30), 2, 3);

		assertThat(installment.getOrigin()).isEqualTo(ReceivableOrigin.RENEGOTIATION);
		assertThat(installment.getStatus()).isEqualTo(ReceivableStatus.OPEN);
		assertThat(installment.getCustomerId()).isEqualTo(customerId);
		assertThat(installment.getInstallmentNumber()).isEqualTo(2);
		assertThat(installment.getInstallments()).isEqualTo(3);
	}

	@Test
	void anInstallmentNumberOutsideThePlanIsRejected() {
		assertThatThrownBy(() -> Receivable.createFromRenegotiation(id(), UUID.randomUUID(), BigDecimal.TEN,
				TODAY.plusDays(1), 4, 3)).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void keepsTheLinkFromTheOriginalTitlesToTheNewOnes() {
		List<ReceivableId> originals = List.of(id(), id());
		List<ReceivableId> created = List.of(id(), id(), id());

		Renegotiation renegotiation = Renegotiation.create(RenegotiationId.of(UUID.randomUUID()), UUID.randomUUID(),
				originals, created, Instant.now());

		assertThat(renegotiation.getOriginalReceivableIds()).containsExactlyElementsOf(originals);
		assertThat(renegotiation.getNewReceivableIds()).containsExactlyElementsOf(created);
	}

	@Test
	void requiresOriginalsAndNewTitles() {
		assertThatThrownBy(() -> Renegotiation.create(RenegotiationId.of(UUID.randomUUID()), UUID.randomUUID(),
				List.of(), List.of(id()), Instant.now())).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> Renegotiation.create(RenegotiationId.of(UUID.randomUUID()), UUID.randomUUID(),
				List.of(id()), List.of(), Instant.now())).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void aTitleCannotBeBothReplacedAndCreated() {
		ReceivableId shared = id();

		assertThatThrownBy(() -> Renegotiation.create(RenegotiationId.of(UUID.randomUUID()), UUID.randomUUID(),
				List.of(shared), List.of(shared), Instant.now())).isInstanceOf(BusinessRuleException.class);
	}
}
