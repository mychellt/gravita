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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PurchaseReceiptTest {

	@Test
	@DisplayName("A newly recorded receipt starts pending conference")
	void aNewlyRecordedReceiptStartsPendingConference() {
		PurchaseReceipt receipt = pending();

		assertThat(receipt.getStatus()).isEqualTo(PurchaseReceiptStatus.PENDING_CONFERENCE);
	}

	@Test
	@DisplayName("Completing the conference attaches the installment terms and moves the receipt to CONFERENCE_COMPLETED")
	void completingConferenceAttachesTheInstallmentTermsAndMovesToConferenceCompleted() {
		PurchaseReceipt receipt = pending();

		PurchaseReceipt conferenced = receipt.completeConference(List.of(installment("100.00")));

		assertThat(conferenced.getStatus()).isEqualTo(PurchaseReceiptStatus.CONFERENCE_COMPLETED);
		assertThat(conferenced.getInstallmentTerms()).hasSize(1);
	}

	@Test
	@DisplayName("Rejects completing the conference without any installment term")
	void completingConferenceWithoutAnyInstallmentTermIsRejected() {
		PurchaseReceipt receipt = pending();

		assertThatThrownBy(() -> receipt.completeConference(List.of()))
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("installment term");
	}

	@Test
	@DisplayName("Rejects confirming a receipt still pending conference")
	void confirmingAReceiptStillPendingConferenceIsRejected() {
		PurchaseReceipt receipt = pending();

		assertThatThrownBy(receipt::confirm)
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("completed physical conference");
	}

	@Test
	@DisplayName("Confirming a conferenced receipt moves it to CONFIRMED")
	void confirmingAConferencedReceiptMovesToConfirmed() {
		PurchaseReceipt receipt = pending().completeConference(List.of(installment("100.00")));

		PurchaseReceipt confirmed = receipt.confirm();

		assertThat(confirmed.getStatus()).isEqualTo(PurchaseReceiptStatus.CONFIRMED);
	}

	@Test
	@DisplayName("Rejects confirming an already confirmed receipt instead of silently ignoring it")
	void confirmingAnAlreadyConfirmedReceiptIsRejectedRatherThanNoOp() {
		PurchaseReceipt confirmed = pending().completeConference(List.of(installment("100.00"))).confirm();

		assertThatThrownBy(confirmed::confirm)
				.isInstanceOf(BusinessRuleException.class)
				.hasMessageContaining("already confirmed");
	}

	@Test
	@DisplayName("A receipt with every item fully received is TOTAL")
	void aReceiptWithEveryItemFullyReceivedIsTotal() {
		PurchaseReceipt receipt = PurchaseReceipt.pending(PurchaseReceiptId.of(UUID.randomUUID()),
				PurchaseOrderId.of(UUID.randomUUID()),
				List.of(new PurchaseReceiptItem(UUID.randomUUID(), BigDecimal.TEN, BigDecimal.TEN)));

		assertThat(receipt.isTotal()).isTrue();
	}

	@Test
	@DisplayName("A receipt with an under-received item is PARTIAL")
	void aReceiptWithAnUnderReceivedItemIsPartial() {
		PurchaseReceipt receipt = PurchaseReceipt.pending(PurchaseReceiptId.of(UUID.randomUUID()),
				PurchaseOrderId.of(UUID.randomUUID()),
				List.of(new PurchaseReceiptItem(UUID.randomUUID(), BigDecimal.TEN, new BigDecimal("4"))));

		assertThat(receipt.isTotal()).isFalse();
	}

	@Test
	@DisplayName("A receipt is TOTAL only when every one of its items is fully received")
	void aReceiptIsOnlyTotalWhenEveryOneOfItsItemsIsFullyReceived() {
		UUID fullyReceivedProduct = UUID.randomUUID();
		UUID underReceivedProduct = UUID.randomUUID();
		PurchaseReceipt receipt = PurchaseReceipt.pending(PurchaseReceiptId.of(UUID.randomUUID()),
				PurchaseOrderId.of(UUID.randomUUID()),
				List.of(new PurchaseReceiptItem(fullyReceivedProduct, BigDecimal.TEN, BigDecimal.TEN),
						new PurchaseReceiptItem(underReceivedProduct, BigDecimal.TEN, new BigDecimal("9"))));

		assertThat(receipt.isTotal()).isFalse();
	}

	@Test
	@DisplayName("Rejects a receipt with an empty received item list")
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
