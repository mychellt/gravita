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
import org.junit.jupiter.api.DisplayName;
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
	@DisplayName("Allows renegotiating an overdue receivable that is open or partially settled")
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
	@DisplayName("Does not treat a receivable due today or later as overdue")
	void aReceivableDueTodayOrLaterIsNotOverdue() {
		assertThatThrownBy(() -> receivable(ReceivableStatus.OPEN, TODAY).renegotiate(TODAY))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("not overdue");
		assertThatThrownBy(() -> receivable(ReceivableStatus.OPEN, TODAY.plusDays(5)).renegotiate(TODAY))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("not overdue");
	}

	@Test
	@DisplayName("Rejects renegotiating a title that is not outstanding even if its due date has passed")
	void aTitleThatIsNotOutstandingCannotBeRenegotiatedEvenIfItsDueDateHasPassed() {
		for (ReceivableStatus status : new ReceivableStatus[] { ReceivableStatus.SETTLED,
				ReceivableStatus.RENEGOTIATED, ReceivableStatus.CANCELLED }) {
			assertThatThrownBy(() -> receivable(status, TODAY.minusDays(30)).renegotiate(TODAY))
					.isInstanceOf(BusinessRuleException.class).hasMessageContaining("not overdue");
		}
	}

	@Test
	@DisplayName("Creates each installment of a renegotiation plan as an open receivable with the renegotiation origin")
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
	@DisplayName("Rejects an installment number outside the plan")
	void anInstallmentNumberOutsideThePlanIsRejected() {
		assertThatThrownBy(() -> Receivable.createFromRenegotiation(id(), UUID.randomUUID(), BigDecimal.TEN,
				TODAY.plusDays(1), 4, 3)).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Keeps the link from the original titles to the new ones")
	void keepsTheLinkFromTheOriginalTitlesToTheNewOnes() {
		List<ReceivableId> originals = List.of(id(), id());
		List<ReceivableId> created = List.of(id(), id(), id());

		Renegotiation renegotiation = Renegotiation.create(RenegotiationId.of(UUID.randomUUID()), UUID.randomUUID(),
				originals, created, Instant.now());

		assertThat(renegotiation.getOriginalReceivableIds()).containsExactlyElementsOf(originals);
		assertThat(renegotiation.getNewReceivableIds()).containsExactlyElementsOf(created);
	}

	@Test
	@DisplayName("Requires both original and new titles")
	void requiresOriginalsAndNewTitles() {
		assertThatThrownBy(() -> Renegotiation.create(RenegotiationId.of(UUID.randomUUID()), UUID.randomUUID(),
				List.of(), List.of(id()), Instant.now())).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> Renegotiation.create(RenegotiationId.of(UUID.randomUUID()), UUID.randomUUID(),
				List.of(id()), List.of(), Instant.now())).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Rejects a title that is both replaced and created")
	void aTitleCannotBeBothReplacedAndCreated() {
		ReceivableId shared = id();

		assertThatThrownBy(() -> Renegotiation.create(RenegotiationId.of(UUID.randomUUID()), UUID.randomUUID(),
				List.of(shared), List.of(shared), Instant.now())).isInstanceOf(BusinessRuleException.class);
	}
}
