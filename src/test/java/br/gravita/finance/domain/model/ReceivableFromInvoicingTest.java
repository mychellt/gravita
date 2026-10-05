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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReceivableFromInvoicingTest {

	@Test
	@DisplayName("Starts an invoicing receivable as open, referencing its fiscal document")
	void anInvoicingReceivableStartsOpenAndReferencesItsFiscalDocument() {
		final UUID documentId = UUID.randomUUID();

		final Receivable receivable = Receivable.createFromInvoicing(ReceivableId.of(UUID.randomUUID()),
				UUID.randomUUID(), documentId, BigDecimal.TEN, LocalDate.now().plusDays(30), 2, 3);

		assertThat(receivable.getOrigin()).isEqualTo(ReceivableOrigin.INVOICING);
		assertThat(receivable.getStatus()).isEqualTo(ReceivableStatus.OPEN);
		assertThat(receivable.getOriginDocumentRef()).isEqualTo(documentId);
		assertThat(receivable.getInstallmentNumber()).isEqualTo(2);
		assertThat(receivable.getInstallments()).isEqualTo(3);
	}

	@Test
	@DisplayName("Rejects an installment number outside the installment count")
	void rejectsAnInstallmentNumberOutsideTheInstallmentCount() {
		assertThatThrownBy(() -> Receivable.createFromInvoicing(ReceivableId.of(UUID.randomUUID()),
				UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, LocalDate.now(), 4, 3))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Requires the origin document")
	void requiresTheOriginDocument() {
		assertThatThrownBy(() -> Receivable.createFromInvoicing(ReceivableId.of(UUID.randomUUID()),
				UUID.randomUUID(), null, BigDecimal.TEN, LocalDate.now(), 1, 1))
				.isInstanceOf(NullPointerException.class);
	}
}
