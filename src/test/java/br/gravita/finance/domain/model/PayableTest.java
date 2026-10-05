package br.gravita.finance.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gravita.core.domain.finance.CostCenterShare;
import br.gravita.core.domain.finance.LedgerScope;
import br.gravita.core.domain.finance.Payable;
import br.gravita.core.domain.finance.PayableId;
import br.gravita.core.domain.finance.PayableOrigin;
import br.gravita.core.domain.finance.PayableStatus;
import br.gravita.core.domain.shared.BusinessRuleException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PayableTest {

	private static final LocalDate DUE = LocalDate.now().plusDays(10);

	private static PayableId id() {
		return PayableId.of(UUID.randomUUID());
	}

	private static CostCenterShare share(final UUID costCenterId, final String percent) {
		return new CostCenterShare(costCenterId, new BigDecimal(percent));
	}

	@Test
	@DisplayName("Starts a purchase receipt payable as open, referencing the receipt and its installment")
	void purchaseReceiptPayableStartsOpenReferencingTheReceiptAndItsInstallment() {
		final UUID supplierId = UUID.randomUUID();
		final UUID receiptId = UUID.randomUUID();

		final Payable payable = Payable.createFromPurchaseReceipt(id(), supplierId, receiptId, new BigDecimal("450.00"),
				DUE, 2, 3);

		assertThat(payable.getOrigin()).isEqualTo(PayableOrigin.PURCHASE_RECEIPT);
		assertThat(payable.getStatus()).isEqualTo(PayableStatus.OPEN);
		assertThat(payable.getSupplierId()).isEqualTo(supplierId);
		assertThat(payable.getPurchaseReceiptRef()).isEqualTo(receiptId);
		assertThat(payable.getInstallmentNumber()).isEqualTo(2);
		assertThat(payable.getInstallments()).isEqualTo(3);
		assertThat(payable.getAmount()).isEqualByComparingTo("450.00");
		assertThat(payable.getDueDate()).isEqualTo(DUE);
		assertThat(payable.getCostCenterSplit()).isEmpty();
	}

	@Test
	@DisplayName("Requires a supplier and a receipt on a purchase receipt payable")
	void purchaseReceiptPayableRequiresSupplierAndReceipt() {
		assertThatThrownBy(() -> Payable.createFromPurchaseReceipt(id(), null, UUID.randomUUID(), BigDecimal.TEN,
				DUE, 1, 1)).isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> Payable.createFromPurchaseReceipt(id(), UUID.randomUUID(), null, BigDecimal.TEN,
				DUE, 1, 1)).isInstanceOf(NullPointerException.class);
	}

	@Test
	@DisplayName("Rejects an installment number outside the installment total")
	void purchaseReceiptPayableRejectsAnInstallmentNumberOutsideTheTotal() {
		assertThatThrownBy(() -> Payable.createFromPurchaseReceipt(id(), UUID.randomUUID(), UUID.randomUUID(),
				BigDecimal.TEN, DUE, 0, 2)).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> Payable.createFromPurchaseReceipt(id(), UUID.randomUUID(), UUID.randomUUID(),
				BigDecimal.TEN, DUE, 3, 2)).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Rejects a purchase receipt payable with a zero or negative amount")
	void purchaseReceiptPayableRejectsANonPositiveAmount() {
		assertThatThrownBy(() -> Payable.createFromPurchaseReceipt(id(), UUID.randomUUID(), UUID.randomUUID(),
				BigDecimal.ZERO, DUE, 1, 1)).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Gives a manual payable no receipt reference")
	void manualPayableHasNoReceiptReference() {
		final Payable payable = Payable.createManual(id(), null, BigDecimal.TEN, DUE, null);

		assertThat(payable.getPurchaseReceiptRef()).isNull();
		assertThat(payable.getInstallmentNumber()).isNull();
		assertThat(payable.getInstallments()).isNull();
	}

	@Test
	@DisplayName("Starts a manual payable as open with the manual origin")
	void manualPayableStartsOpenWithManualOrigin() {
		final UUID supplierId = UUID.randomUUID();

		final Payable payable = Payable.createManual(id(), supplierId, new BigDecimal("1200.00"), DUE, null);

		assertThat(payable.getOrigin()).isEqualTo(PayableOrigin.MANUAL);
		assertThat(payable.getStatus()).isEqualTo(PayableStatus.OPEN);
		assertThat(payable.getSupplierId()).isEqualTo(supplierId);
		assertThat(payable.getAmount()).isEqualByComparingTo("1200.00");
		assertThat(payable.getDueDate()).isEqualTo(DUE);
		assertThat(payable.getCostCenterSplit()).isEmpty();
	}

	@Test
	@DisplayName("Makes the supplier optional for pure expenses")
	void theSupplierIsOptionalForPureExpenses() {
		final Payable payable = Payable.createManual(id(), null, BigDecimal.TEN, DUE, List.of());

		assertThat(payable.getSupplierId()).isNull();
	}

	@Test
	@DisplayName("Requires an amount")
	void amountIsRequired() {
		assertThatThrownBy(() -> Payable.createManual(id(), null, null, DUE, null))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Requires the amount to be positive")
	void amountMustBePositive() {
		assertThatThrownBy(() -> Payable.createManual(id(), null, BigDecimal.ZERO, DUE, null))
				.isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> Payable.createManual(id(), null, new BigDecimal("-1"), DUE, null))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Requires a due date")
	void dueDateIsRequired() {
		assertThatThrownBy(() -> Payable.createManual(id(), null, BigDecimal.TEN, null, null))
				.isInstanceOf(NullPointerException.class).hasMessageContaining("dueDate");
	}

	@Test
	@DisplayName("Keeps a cost center split that sums to 100%")
	void costCenterSplitSummingTo100IsKept() {
		final UUID a = UUID.randomUUID();
		final UUID b = UUID.randomUUID();

		final Payable payable = Payable.createManual(id(), null, BigDecimal.TEN, DUE,
				List.of(share(a, "60.00"), share(b, "40.00")));

		assertThat(payable.getCostCenterSplit()).extracting(CostCenterShare::costCenterId).containsExactly(a, b);
	}

	@Test
	@DisplayName("Rejects a cost center split that does not sum to 100%")
	void costCenterSplitNotSummingTo100IsRejected() {
		assertThatThrownBy(() -> Payable.createManual(id(), null, BigDecimal.TEN, DUE,
				List.of(share(UUID.randomUUID(), "60"), share(UUID.randomUUID(), "30"))))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("sum to 100");
	}

	@Test
	@DisplayName("Rejects a split that repeats a cost center")
	void costCenterCannotAppearTwiceInTheSplit() {
		final UUID a = UUID.randomUUID();

		assertThatThrownBy(() -> Payable.createManual(id(), null, BigDecimal.TEN, DUE,
				List.of(share(a, "50"), share(a, "50")))).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Requires each share percentage to be above 0 and at most 100")
	void sharePercentMustBeBetweenZeroExclusiveAndOneHundred() {
		final UUID a = UUID.randomUUID();

		assertThatThrownBy(() -> share(a, "0")).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> share(a, "100.01")).isInstanceOf(BusinessRuleException.class);
		assertThat(share(a, "100").percent()).isEqualByComparingTo("100");
	}

	@Test
	@DisplayName("Replaces the previous split and keeps every other field when changing the cost center split")
	void withCostCenterSplitReplacesThePreviousSplitKeepingEverythingElse() {
		final UUID a = UUID.randomUUID();
		final UUID b = UUID.randomUUID();
		final UUID c = UUID.randomUUID();
		final Payable payable = Payable.createManual(id(), UUID.randomUUID(), BigDecimal.TEN, DUE,
				List.of(share(a, "100")));

		final Payable split = payable.withCostCenterSplit(List.of(share(b, "60"), share(c, "40")));

		assertThat(split.getCostCenterSplit()).extracting(CostCenterShare::costCenterId).containsExactly(b, c);
		assertThat(split.getId()).isEqualTo(payable.getId());
		assertThat(split.getSupplierId()).isEqualTo(payable.getSupplierId());
		assertThat(split.getAmount()).isEqualByComparingTo(payable.getAmount());
		assertThat(split.getDueDate()).isEqualTo(payable.getDueDate());
		assertThat(split.getStatus()).isEqualTo(payable.getStatus());
		assertThat(payable.getCostCenterSplit()).extracting(CostCenterShare::costCenterId).containsExactly(a);
	}

	@Test
	@DisplayName("Keeps the ledger scope when changing the cost center split")
	void withCostCenterSplitKeepsTheLedgerScope() {
		final LedgerScope scope = new LedgerScope(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
		final Payable payable = Payable.createManual(id(), null, BigDecimal.TEN, DUE, null).inScope(scope);

		final Payable split = payable.withCostCenterSplit(List.of(share(UUID.randomUUID(), "100")));

		assertThat(split.getScope()).isEqualTo(scope);
	}

	@Test
	@DisplayName("Rejects a new split whose percentages do not sum to 100%")
	void withCostCenterSplitRejectsPercentagesNotSummingTo100() {
		final Payable payable = Payable.createManual(id(), null, BigDecimal.TEN, DUE, null);

		assertThatThrownBy(() -> payable.withCostCenterSplit(
				List.of(share(UUID.randomUUID(), "60"), share(UUID.randomUUID(), "30"))))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("sum to 100");
	}

	@Test
	@DisplayName("Rejects a new split that repeats a cost center")
	void withCostCenterSplitRejectsARepeatedCostCenter() {
		final UUID a = UUID.randomUUID();
		final Payable payable = Payable.createManual(id(), null, BigDecimal.TEN, DUE, null);

		assertThatThrownBy(() -> payable.withCostCenterSplit(List.of(share(a, "50"), share(a, "50"))))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Requires a new split to have at least one share")
	void withCostCenterSplitRequiresAtLeastOneShare() {
		final Payable payable = Payable.createManual(id(), null, BigDecimal.TEN, DUE, List.of(share(UUID.randomUUID(), "100")));

		assertThatThrownBy(() -> payable.withCostCenterSplit(null)).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> payable.withCostCenterSplit(List.of())).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	@DisplayName("Rejects splitting a cancelled payable")
	void cancelledPayableCannotBeSplit() {
		final Payable cancelled = Payable.builder()
				.id(id())
				.supplierId(null)
				.origin(PayableOrigin.MANUAL)
				.amount(BigDecimal.TEN)
				.dueDate(DUE)
				.costCenterSplit(null)
				.status(PayableStatus.CANCELLED)
				.purchaseReceiptRef(null)
				.installmentNumber(null)
				.installments(null)
				.build();

		assertThatThrownBy(() -> cancelled.withCostCenterSplit(List.of(share(UUID.randomUUID(), "100"))))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("CANCELLED");
	}

	@Test
	@DisplayName("Allows reclassifying a paid payable by cost center")
	void paidPayableCanStillBeReclassified() {
		final Payable paid = Payable.builder()
				.id(id())
				.supplierId(null)
				.origin(PayableOrigin.MANUAL)
				.amount(BigDecimal.TEN)
				.dueDate(DUE)
				.costCenterSplit(null)
				.status(PayableStatus.PAID)
				.purchaseReceiptRef(null)
				.installmentNumber(null)
				.installments(null)
				.build();

		final Payable split = paid.withCostCenterSplit(List.of(share(UUID.randomUUID(), "100")));

		assertThat(split.getStatus()).isEqualTo(PayableStatus.PAID);
		assertThat(split.getCostCenterSplit()).hasSize(1);
	}

	@Test
	@DisplayName("Applies the cost center's percentage to the amount and gives zero for a cost center not charged")
	void shareOfAppliesTheCostCentersPercentageAndIsZeroForACostCenterNotCharged() {
		final UUID a = UUID.randomUUID();
		final UUID b = UUID.randomUUID();
		final Payable payable = Payable.createManual(id(), null, new BigDecimal("100.00"), DUE,
				List.of(share(a, "33.33"), share(b, "66.67")));

		assertThat(payable.shareOf(new BigDecimal("100.00"), a)).isEqualByComparingTo("33.33");
		assertThat(payable.shareOf(new BigDecimal("50.00"), b)).isEqualByComparingTo("33.34");
		assertThat(payable.shareOf(new BigDecimal("100.00"), UUID.randomUUID())).isEqualByComparingTo("0");
		assertThat(payable.shareOf(new BigDecimal("100.00"), null)).isEqualByComparingTo("100.00");
	}

	@Test
	@DisplayName("Treats a payable as outstanding while it is open or approved")
	void isOutstandingWhileOpenOrApproved() {
		for (final PayableStatus status : PayableStatus.values()) {
			final Payable payable = Payable.builder()
					.id(id())
					.supplierId(null)
					.origin(PayableOrigin.MANUAL)
					.amount(BigDecimal.TEN)
					.dueDate(DUE)
					.costCenterSplit(List.of())
					.status(status)
					.purchaseReceiptRef(null)
					.installmentNumber(null)
					.installments(null)
					.build();

			assertThat(payable.isOutstanding())
					.isEqualTo(status == PayableStatus.OPEN || status == PayableStatus.APPROVED);
		}
	}

	@Test
	@DisplayName("Leaves a payable without a scope until it is placed in one")
	void payableIsInNoScopeUntilPlacedInOne() {
		final Payable payable = Payable.createManual(id(), null, BigDecimal.TEN, DUE, null);
		final LedgerScope scope = new LedgerScope(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

		assertThat(payable.getScope()).isEqualTo(LedgerScope.NONE);
		assertThat(payable.inScope(scope).getScope()).isEqualTo(scope);
	}

	@Test
	@DisplayName("Moves an open payable to approved and records the approver")
	void approvingAnOpenPayableMovesItToApprovedAndRecordsTheApprover() {
		final UUID approver = UUID.randomUUID();
		final Payable open = Payable.createManual(id(), UUID.randomUUID(), new BigDecimal("80.00"), DUE,
				List.of(share(UUID.randomUUID(), "100")));

		final Payable approved = open.approve(approver);

		assertThat(open.getStatus()).isEqualTo(PayableStatus.OPEN);
		assertThat(open.getApprovedBy()).isNull();
		assertThat(approved.getStatus()).isEqualTo(PayableStatus.APPROVED);
		assertThat(approved.getApprovedBy()).isEqualTo(approver);
		assertThat(approved.getId()).isEqualTo(open.getId());
		assertThat(approved.getAmount()).isEqualByComparingTo("80.00");
		assertThat(approved.getCostCenterSplit()).isEqualTo(open.getCostCenterSplit());
		assertThat(approved.isOutstanding()).isTrue();
	}

	@Test
	@DisplayName("Allows approving only open payables")
	void onlyOpenPayablesCanBeApproved() {
		for (final PayableStatus status : PayableStatus.values()) {
			if (status == PayableStatus.OPEN) {
				continue;
			}
			final Payable payable = Payable.builder()
					.id(id())
					.supplierId(null)
					.origin(PayableOrigin.MANUAL)
					.amount(BigDecimal.TEN)
					.dueDate(DUE)
					.costCenterSplit(List.of())
					.status(status)
					.purchaseReceiptRef(null)
					.installmentNumber(null)
					.installments(null)
					.build();

			assertThatThrownBy(() -> payable.approve(UUID.randomUUID())).isInstanceOf(BusinessRuleException.class)
					.hasMessageContaining(status.name());
		}
	}

	@Test
	@DisplayName("Requires an approver to approve a payable")
	void approvingRequiresAnApprover() {
		final Payable open = Payable.createManual(id(), null, BigDecimal.TEN, DUE, null);

		assertThatThrownBy(() -> open.approve(null)).isInstanceOf(NullPointerException.class);
	}

	@Test
	@DisplayName("Keeps the approver after splitting and placing in a scope")
	void theApproverSurvivesSplittingAndPlacingInAScope() {
		final UUID approver = UUID.randomUUID();
		final Payable approved = Payable.createManual(id(), null, BigDecimal.TEN, DUE, null).approve(approver);

		assertThat(approved.withCostCenterSplit(List.of(share(UUID.randomUUID(), "100"))).getApprovedBy())
				.isEqualTo(approver);
		assertThat(approved.inScope(new LedgerScope(UUID.randomUUID(), null, null)).getApprovedBy())
				.isEqualTo(approver);
	}

	@Test
	@DisplayName("Moves an approved payable to paid and appends the receipt")
	void payingAnApprovedPayableMovesItToPaidAndAppendsTheReceipt() {
		final Payable approved = Payable.createManual(id(), null, BigDecimal.TEN, DUE, null).approve(UUID.randomUUID());

		final Payable paid = approved.pay("https://files.example.com/receipt.pdf");

		assertThat(paid.getStatus()).isEqualTo(PayableStatus.PAID);
		assertThat(paid.getAttachments()).containsExactly("https://files.example.com/receipt.pdf");
		assertThat(paid.getApprovedBy()).isEqualTo(approved.getApprovedBy());
		assertThat(approved.getStatus()).isEqualTo(PayableStatus.APPROVED);
		assertThat(approved.getAttachments()).isEmpty();
	}

	@Test
	@DisplayName("Keeps the attachments already linked when paying and allows a missing receipt")
	void payingKeepsTheAttachmentsAlreadyLinkedAndAllowsMissingReceipt() {
		final Payable approved = Payable.builder()
				.id(id())
				.supplierId(null)
				.origin(PayableOrigin.MANUAL)
				.amount(BigDecimal.TEN)
				.dueDate(DUE)
				.costCenterSplit(null)
				.status(PayableStatus.APPROVED)
				.purchaseReceiptRef(null)
				.installmentNumber(null)
				.installments(null)
				.scope(LedgerScope.NONE)
				.approvedBy(UUID.randomUUID())
				.attachments(List.of("boleto.pdf"))
				.build();

		assertThat(approved.pay("receipt.pdf").getAttachments()).containsExactly("boleto.pdf", "receipt.pdf");
		assertThat(approved.pay(null).getAttachments()).containsExactly("boleto.pdf");
		assertThat(approved.pay(null).getStatus()).isEqualTo(PayableStatus.PAID);
	}

	@Test
	@DisplayName("Allows paying only approved payables")
	void onlyApprovedPayablesCanBePaid() {
		for (final PayableStatus status : List.of(PayableStatus.OPEN, PayableStatus.PAID, PayableStatus.CANCELLED)) {
			final Payable payable = Payable.builder()
					.id(id())
					.supplierId(null)
					.origin(PayableOrigin.MANUAL)
					.amount(BigDecimal.TEN)
					.dueDate(DUE)
					.costCenterSplit(null)
					.status(status)
					.purchaseReceiptRef(null)
					.installmentNumber(null)
					.installments(null)
					.build();

			assertThatThrownBy(() -> payable.pay("receipt.pdf")).isInstanceOf(BusinessRuleException.class)
					.hasMessageContaining(status.name());
		}
	}

	@Test
	@DisplayName("Keeps the attachments after approving, splitting and placing in a scope")
	void attachmentsSurviveApprovingSplittingAndPlacingInAScope() {
		final Payable payable = Payable.builder()
				.id(id())
				.supplierId(null)
				.origin(PayableOrigin.MANUAL)
				.amount(BigDecimal.TEN)
				.dueDate(DUE)
				.costCenterSplit(null)
				.status(PayableStatus.OPEN)
				.purchaseReceiptRef(null)
				.installmentNumber(null)
				.installments(null)
				.scope(LedgerScope.NONE)
				.approvedBy(null)
				.attachments(List.of("boleto.pdf"))
				.build();

		assertThat(payable.approve(UUID.randomUUID()).getAttachments()).containsExactly("boleto.pdf");
		assertThat(payable.withCostCenterSplit(List.of(share(UUID.randomUUID(), "100"))).getAttachments())
				.containsExactly("boleto.pdf");
		assertThat(payable.inScope(new LedgerScope(UUID.randomUUID(), null, null)).getAttachments())
				.containsExactly("boleto.pdf");
	}

	@Test
	@DisplayName("Appends an attached document to those already linked, whatever the status")
	void attachingAppendsTheDocumentToThoseAlreadyLinkedInAnyStatus() {
		for (final PayableStatus status : PayableStatus.values()) {
			final Payable payable = Payable.builder()
					.id(id())
					.supplierId(null)
					.origin(PayableOrigin.MANUAL)
					.amount(BigDecimal.TEN)
					.dueDate(DUE)
					.costCenterSplit(null)
					.status(status)
					.purchaseReceiptRef(null)
					.installmentNumber(null)
					.installments(null)
					.scope(LedgerScope.NONE)
					.approvedBy(null)
					.attachments(List.of("boleto.pdf"))
					.build();

			final Payable attached = payable.attach("nf.pdf");

			assertThat(attached.getAttachments()).containsExactly("boleto.pdf", "nf.pdf");
			assertThat(attached.getStatus()).isEqualTo(status);
			assertThat(payable.getAttachments()).containsExactly("boleto.pdf");
		}
	}

	@Test
	@DisplayName("Requires a document reference to attach a document")
	void attachingRequiresADocumentReference() {
		final Payable payable = Payable.createManual(id(), null, BigDecimal.TEN, DUE, null);

		assertThatThrownBy(() -> payable.attach(null)).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> payable.attach("  ")).isInstanceOf(BusinessRuleException.class);
	}
}
