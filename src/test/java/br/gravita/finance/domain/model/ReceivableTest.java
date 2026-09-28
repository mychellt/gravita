package br.gravita.finance.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gravita.core.domain.finance.Receivable;
import br.gravita.core.domain.finance.ReceivableId;
import br.gravita.core.domain.finance.ReceivableOrigin;
import br.gravita.core.domain.finance.ReceivableStatus;
import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.time.LocalDate;
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
}
