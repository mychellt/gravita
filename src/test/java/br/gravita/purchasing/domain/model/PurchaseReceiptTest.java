package br.gravita.purchasing.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gravita.core.domain.purchasing.InstallmentTerm;
import br.gravita.core.domain.purchasing.PurchaseOrderId;
import br.gravita.core.domain.purchasing.PurchaseReceipt;
import br.gravita.core.domain.purchasing.PurchaseReceiptId;
import br.gravita.core.domain.purchasing.PurchaseReceiptItem;
import br.gravita.core.domain.purchasing.PurchaseReceiptStatus;
import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PurchaseReceiptTest {

	@Test
	void aNewlyRecordedReceiptStartsPendingConference() {
		PurchaseReceipt receipt = pending();

		assertThat(receipt.getStatus()).isEqualTo(PurchaseReceiptStatus.PENDING_CONFERENCE);
	}

	@Test
	void completingConferenceAttachesTheInstallmentTermsAndMovesToConferenceCompleted() {
		PurchaseReceipt receipt = pending();

		PurchaseReceipt conferenced = receipt.completeConference(List.of(installment("100.00")));

		assertThat(conferenced.getStatus()).isEqualTo(PurchaseReceiptStatus.CONFERENCE_COMPLETED);
		assertThat(conferenced.getInstallmentTerms()).hasSize(1);
	}

	@Test
	void completingConferenceWithoutAnyInstallmentTermIsRejected() {
		PurchaseReceipt receipt = pending();

		assertThatThrownBy(() -> receipt.completeConference(List.of()))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("installment term");
	}

	@Test
	void confirmingAReceiptStillPendingConferenceIsRejected() {
		PurchaseReceipt receipt = pending();

		assertThatThrownBy(receipt::confirm)
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("completed physical conference");
	}

	@Test
	void confirmingAConferencedReceiptMovesToConfirmed() {
		PurchaseReceipt receipt = pending().completeConference(List.of(installment("100.00")));

		PurchaseReceipt confirmed = receipt.confirm();

		assertThat(confirmed.getStatus()).isEqualTo(PurchaseReceiptStatus.CONFIRMED);
	}

	@Test
	void confirmingAnAlreadyConfirmedReceiptIsRejectedRatherThanNoOp() {
		PurchaseReceipt confirmed = pending().completeConference(List.of(installment("100.00"))).confirm();

		assertThatThrownBy(confirmed::confirm)
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("already confirmed");
	}

	@Test
	void rejectsAnEmptyReceivedItemList() {
		assertThatThrownBy(() -> PurchaseReceipt.pending(PurchaseReceiptId.of(UUID.randomUUID()),
				PurchaseOrderId.of(UUID.randomUUID()), List.of()))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("at least one received item");
	}

	private PurchaseReceipt pending() {
		return PurchaseReceipt.pending(PurchaseReceiptId.of(UUID.randomUUID()), PurchaseOrderId.of(UUID.randomUUID()),
				List.of(new PurchaseReceiptItem(UUID.randomUUID(), BigDecimal.TEN, BigDecimal.TEN)));
	}

	private InstallmentTerm installment(String amount) {
		return new InstallmentTerm(new BigDecimal(amount), LocalDate.now().plusDays(30));
	}
}
