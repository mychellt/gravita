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
import org.junit.jupiter.api.Test;

class PayableTest {

	private static final LocalDate DUE = LocalDate.now().plusDays(10);

	private static PayableId id() {
		return PayableId.of(UUID.randomUUID());
	}

	private static CostCenterShare share(UUID costCenterId, String percent) {
		return new CostCenterShare(costCenterId, new BigDecimal(percent));
	}

	@Test
	void aPurchaseReceiptPayableStartsOpenReferencingTheReceiptAndItsInstallment() {
		UUID supplierId = UUID.randomUUID();
		UUID receiptId = UUID.randomUUID();

		Payable payable = Payable.createFromPurchaseReceipt(id(), supplierId, receiptId, new BigDecimal("450.00"),
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
	void aPurchaseReceiptPayableRequiresSupplierAndReceipt() {
		assertThatThrownBy(() -> Payable.createFromPurchaseReceipt(id(), null, UUID.randomUUID(), BigDecimal.TEN,
				DUE, 1, 1)).isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> Payable.createFromPurchaseReceipt(id(), UUID.randomUUID(), null, BigDecimal.TEN,
				DUE, 1, 1)).isInstanceOf(NullPointerException.class);
	}

	@Test
	void aPurchaseReceiptPayableRejectsAnInstallmentNumberOutsideTheTotal() {
		assertThatThrownBy(() -> Payable.createFromPurchaseReceipt(id(), UUID.randomUUID(), UUID.randomUUID(),
				BigDecimal.TEN, DUE, 0, 2)).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> Payable.createFromPurchaseReceipt(id(), UUID.randomUUID(), UUID.randomUUID(),
				BigDecimal.TEN, DUE, 3, 2)).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void aPurchaseReceiptPayableRejectsANonPositiveAmount() {
		assertThatThrownBy(() -> Payable.createFromPurchaseReceipt(id(), UUID.randomUUID(), UUID.randomUUID(),
				BigDecimal.ZERO, DUE, 1, 1)).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void aManualPayableHasNoReceiptReference() {
		Payable payable = Payable.createManual(id(), null, BigDecimal.TEN, DUE, null);

		assertThat(payable.getPurchaseReceiptRef()).isNull();
		assertThat(payable.getInstallmentNumber()).isNull();
		assertThat(payable.getInstallments()).isNull();
	}

	@Test
	void aManualPayableStartsOpenWithManualOrigin() {
		UUID supplierId = UUID.randomUUID();

		Payable payable = Payable.createManual(id(), supplierId, new BigDecimal("1200.00"), DUE, null);

		assertThat(payable.getOrigin()).isEqualTo(PayableOrigin.MANUAL);
		assertThat(payable.getStatus()).isEqualTo(PayableStatus.OPEN);
		assertThat(payable.getSupplierId()).isEqualTo(supplierId);
		assertThat(payable.getAmount()).isEqualByComparingTo("1200.00");
		assertThat(payable.getDueDate()).isEqualTo(DUE);
		assertThat(payable.getCostCenterSplit()).isEmpty();
	}

	@Test
	void theSupplierIsOptionalForPureExpenses() {
		Payable payable = Payable.createManual(id(), null, BigDecimal.TEN, DUE, List.of());

		assertThat(payable.getSupplierId()).isNull();
	}

	@Test
	void amountIsRequired() {
		assertThatThrownBy(() -> Payable.createManual(id(), null, null, DUE, null))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void amountMustBePositive() {
		assertThatThrownBy(() -> Payable.createManual(id(), null, BigDecimal.ZERO, DUE, null))
				.isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> Payable.createManual(id(), null, new BigDecimal("-1"), DUE, null))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void dueDateIsRequired() {
		assertThatThrownBy(() -> Payable.createManual(id(), null, BigDecimal.TEN, null, null))
				.isInstanceOf(NullPointerException.class).hasMessageContaining("dueDate");
	}

	@Test
	void aCostCenterSplitSummingTo100IsKept() {
		UUID a = UUID.randomUUID();
		UUID b = UUID.randomUUID();

		Payable payable = Payable.createManual(id(), null, BigDecimal.TEN, DUE,
				List.of(share(a, "60.00"), share(b, "40.00")));

		assertThat(payable.getCostCenterSplit()).extracting(CostCenterShare::costCenterId).containsExactly(a, b);
	}

	@Test
	void aCostCenterSplitNotSummingTo100IsRejected() {
		assertThatThrownBy(() -> Payable.createManual(id(), null, BigDecimal.TEN, DUE,
				List.of(share(UUID.randomUUID(), "60"), share(UUID.randomUUID(), "30"))))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("sum to 100");
	}

	@Test
	void aCostCenterCannotAppearTwiceInTheSplit() {
		UUID a = UUID.randomUUID();

		assertThatThrownBy(() -> Payable.createManual(id(), null, BigDecimal.TEN, DUE,
				List.of(share(a, "50"), share(a, "50")))).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void aSharePercentMustBeBetweenZeroExclusiveAndOneHundred() {
		UUID a = UUID.randomUUID();

		assertThatThrownBy(() -> share(a, "0")).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> share(a, "100.01")).isInstanceOf(BusinessRuleException.class);
		assertThat(share(a, "100").percent()).isEqualByComparingTo("100");
	}

	@Test
	void withCostCenterSplitReplacesThePreviousSplitKeepingEverythingElse() {
		UUID a = UUID.randomUUID();
		UUID b = UUID.randomUUID();
		UUID c = UUID.randomUUID();
		Payable payable = Payable.createManual(id(), UUID.randomUUID(), BigDecimal.TEN, DUE,
				List.of(share(a, "100")));

		Payable split = payable.withCostCenterSplit(List.of(share(b, "60"), share(c, "40")));

		assertThat(split.getCostCenterSplit()).extracting(CostCenterShare::costCenterId).containsExactly(b, c);
		assertThat(split.getId()).isEqualTo(payable.getId());
		assertThat(split.getSupplierId()).isEqualTo(payable.getSupplierId());
		assertThat(split.getAmount()).isEqualByComparingTo(payable.getAmount());
		assertThat(split.getDueDate()).isEqualTo(payable.getDueDate());
		assertThat(split.getStatus()).isEqualTo(payable.getStatus());
		assertThat(payable.getCostCenterSplit()).extracting(CostCenterShare::costCenterId).containsExactly(a);
	}

	@Test
	void withCostCenterSplitKeepsTheLedgerScope() {
		LedgerScope scope = new LedgerScope(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
		Payable payable = Payable.createManual(id(), null, BigDecimal.TEN, DUE, null).inScope(scope);

		Payable split = payable.withCostCenterSplit(List.of(share(UUID.randomUUID(), "100")));

		assertThat(split.getScope()).isEqualTo(scope);
	}

	@Test
	void withCostCenterSplitRejectsPercentagesNotSummingTo100() {
		Payable payable = Payable.createManual(id(), null, BigDecimal.TEN, DUE, null);

		assertThatThrownBy(() -> payable.withCostCenterSplit(
				List.of(share(UUID.randomUUID(), "60"), share(UUID.randomUUID(), "30"))))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("sum to 100");
	}

	@Test
	void withCostCenterSplitRejectsARepeatedCostCenter() {
		UUID a = UUID.randomUUID();
		Payable payable = Payable.createManual(id(), null, BigDecimal.TEN, DUE, null);

		assertThatThrownBy(() -> payable.withCostCenterSplit(List.of(share(a, "50"), share(a, "50"))))
				.isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void withCostCenterSplitRequiresAtLeastOneShare() {
		Payable payable = Payable.createManual(id(), null, BigDecimal.TEN, DUE, List.of(share(UUID.randomUUID(), "100")));

		assertThatThrownBy(() -> payable.withCostCenterSplit(null)).isInstanceOf(BusinessRuleException.class);
		assertThatThrownBy(() -> payable.withCostCenterSplit(List.of())).isInstanceOf(BusinessRuleException.class);
	}

	@Test
	void aCancelledPayableCannotBeSplit() {
		Payable cancelled = Payable.of(id(), null, PayableOrigin.MANUAL, BigDecimal.TEN, DUE, null,
				PayableStatus.CANCELLED, null, null, null);

		assertThatThrownBy(() -> cancelled.withCostCenterSplit(List.of(share(UUID.randomUUID(), "100"))))
				.isInstanceOf(BusinessRuleException.class).hasMessageContaining("CANCELLED");
	}

	@Test
	void aPaidPayableCanStillBeReclassified() {
		Payable paid = Payable.of(id(), null, PayableOrigin.MANUAL, BigDecimal.TEN, DUE, null, PayableStatus.PAID,
				null, null, null);

		Payable split = paid.withCostCenterSplit(List.of(share(UUID.randomUUID(), "100")));

		assertThat(split.getStatus()).isEqualTo(PayableStatus.PAID);
		assertThat(split.getCostCenterSplit()).hasSize(1);
	}

	@Test
	void shareOfAppliesTheCostCentersPercentageAndIsZeroForACostCenterNotCharged() {
		UUID a = UUID.randomUUID();
		UUID b = UUID.randomUUID();
		Payable payable = Payable.createManual(id(), null, new BigDecimal("100.00"), DUE,
				List.of(share(a, "33.33"), share(b, "66.67")));

		assertThat(payable.shareOf(new BigDecimal("100.00"), a)).isEqualByComparingTo("33.33");
		assertThat(payable.shareOf(new BigDecimal("50.00"), b)).isEqualByComparingTo("33.34");
		assertThat(payable.shareOf(new BigDecimal("100.00"), UUID.randomUUID())).isEqualByComparingTo("0");
		assertThat(payable.shareOf(new BigDecimal("100.00"), null)).isEqualByComparingTo("100.00");
	}

	@Test
	void isOutstandingWhileOpenOrApproved() {
		for (PayableStatus status : PayableStatus.values()) {
			Payable payable = Payable.of(id(), null, PayableOrigin.MANUAL, BigDecimal.TEN, DUE, List.of(), status,
					null, null, null);

			assertThat(payable.isOutstanding())
					.isEqualTo(status == PayableStatus.OPEN || status == PayableStatus.APPROVED);
		}
	}

	@Test
	void aPayableIsInNoScopeUntilPlacedInOne() {
		Payable payable = Payable.createManual(id(), null, BigDecimal.TEN, DUE, null);
		LedgerScope scope = new LedgerScope(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

		assertThat(payable.getScope()).isEqualTo(LedgerScope.NONE);
		assertThat(payable.inScope(scope).getScope()).isEqualTo(scope);
	}

	@Test
	void approvingAnOpenPayableMovesItToApprovedAndRecordsTheApprover() {
		UUID approver = UUID.randomUUID();
		Payable open = Payable.createManual(id(), UUID.randomUUID(), new BigDecimal("80.00"), DUE,
				List.of(share(UUID.randomUUID(), "100")));

		Payable approved = open.approve(approver);

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
	void onlyOpenPayablesCanBeApproved() {
		for (PayableStatus status : PayableStatus.values()) {
			if (status == PayableStatus.OPEN) {
				continue;
			}
			Payable payable = Payable.of(id(), null, PayableOrigin.MANUAL, BigDecimal.TEN, DUE, List.of(), status,
					null, null, null);

			assertThatThrownBy(() -> payable.approve(UUID.randomUUID())).isInstanceOf(BusinessRuleException.class)
					.hasMessageContaining(status.name());
		}
	}

	@Test
	void approvingRequiresAnApprover() {
		Payable open = Payable.createManual(id(), null, BigDecimal.TEN, DUE, null);

		assertThatThrownBy(() -> open.approve(null)).isInstanceOf(NullPointerException.class);
	}

	@Test
	void theApproverSurvivesSplittingAndPlacingInAScope() {
		UUID approver = UUID.randomUUID();
		Payable approved = Payable.createManual(id(), null, BigDecimal.TEN, DUE, null).approve(approver);

		assertThat(approved.withCostCenterSplit(List.of(share(UUID.randomUUID(), "100"))).getApprovedBy())
				.isEqualTo(approver);
		assertThat(approved.inScope(new LedgerScope(UUID.randomUUID(), null, null)).getApprovedBy())
				.isEqualTo(approver);
	}

	@Test
	void payingAnApprovedPayableMovesItToPaidAndAppendsTheReceipt() {
		Payable approved = Payable.createManual(id(), null, BigDecimal.TEN, DUE, null).approve(UUID.randomUUID());

		Payable paid = approved.pay("https://files.example.com/receipt.pdf");

		assertThat(paid.getStatus()).isEqualTo(PayableStatus.PAID);
		assertThat(paid.getAttachments()).containsExactly("https://files.example.com/receipt.pdf");
		assertThat(paid.getApprovedBy()).isEqualTo(approved.getApprovedBy());
		assertThat(approved.getStatus()).isEqualTo(PayableStatus.APPROVED);
		assertThat(approved.getAttachments()).isEmpty();
	}

	@Test
	void payingKeepsTheAttachmentsAlreadyLinkedAndAllowsMissingReceipt() {
		Payable approved = Payable.of(id(), null, PayableOrigin.MANUAL, BigDecimal.TEN, DUE, null,
				PayableStatus.APPROVED, null, null, null, LedgerScope.NONE, UUID.randomUUID(), List.of("boleto.pdf"));

		assertThat(approved.pay("receipt.pdf").getAttachments()).containsExactly("boleto.pdf", "receipt.pdf");
		assertThat(approved.pay(null).getAttachments()).containsExactly("boleto.pdf");
		assertThat(approved.pay(null).getStatus()).isEqualTo(PayableStatus.PAID);
	}

	@Test
	void onlyApprovedPayablesCanBePaid() {
		for (PayableStatus status : List.of(PayableStatus.OPEN, PayableStatus.PAID, PayableStatus.CANCELLED)) {
			Payable payable = Payable.of(id(), null, PayableOrigin.MANUAL, BigDecimal.TEN, DUE, null, status, null,
					null, null);

			assertThatThrownBy(() -> payable.pay("receipt.pdf")).isInstanceOf(BusinessRuleException.class)
					.hasMessageContaining(status.name());
		}
	}

	@Test
	void attachmentsSurviveApprovingSplittingAndPlacingInAScope() {
		Payable payable = Payable.of(id(), null, PayableOrigin.MANUAL, BigDecimal.TEN, DUE, null,
				PayableStatus.OPEN, null, null, null, LedgerScope.NONE, null, List.of("boleto.pdf"));

		assertThat(payable.approve(UUID.randomUUID()).getAttachments()).containsExactly("boleto.pdf");
		assertThat(payable.withCostCenterSplit(List.of(share(UUID.randomUUID(), "100"))).getAttachments())
				.containsExactly("boleto.pdf");
		assertThat(payable.inScope(new LedgerScope(UUID.randomUUID(), null, null)).getAttachments())
				.containsExactly("boleto.pdf");
	}
}
